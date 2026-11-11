package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.application.ports.out.AiCostGatePort;
import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.GateOutcome;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * BR-AI-04 steps 3, 4, 8 and 9 as two atomic Lua scripts. Keys follow 02-bd/database/F5 section 3:
 * {@code ai:cooldown:<userId>} holds the request id of the holder, {@code
 * ai:ratelimit:<userId>:<feature>:<epochHour>} is a fixed one-hour counter
 * (DEC-2026-1010-ai-rate-limit-fixed-window). {@code ai:refund:<requestId>} is the once-only marker
 * that makes a second refund a no-op. Redis down lets the call through (fail-open): the Postgres
 * budget still blocks, and a dead Redis must not kill the AI path as well.
 */
@Component
class RedisAiCostGateAdapter implements AiCostGatePort {
  private static final Logger LOG = Logger.getLogger(RedisAiCostGateAdapter.class.getName());
  private static final long HOUR_SECONDS = 3600;
  private static final long REFUND_MARKER_SECONDS = 2 * HOUR_SECONDS;

  // Reply: {0} granted, {1, ttl} cooldown held, {2} quota exceeded (already undone). A cooldown of
  // 0 seconds skips the cooldown key entirely: PRACTICE_FEEDBACK has a quota but no cooldown.
  private static final DefaultRedisScript<List> RESERVE =
      new DefaultRedisScript<>(
          """
          local cooldown=tonumber(ARGV[2])>0
          if cooldown and not redis.call('SET',KEYS[1],ARGV[1],'NX','EX',ARGV[2]) then
            local ttl=redis.call('TTL',KEYS[1])
            if ttl<0 then ttl=tonumber(ARGV[2]) end
            return {1,ttl}
          end
          local n=redis.call('INCR',KEYS[2])
          if n==1 then redis.call('EXPIRE',KEYS[2],3600) end
          if n>tonumber(ARGV[3]) then
            redis.call('DECR',KEYS[2])
            if cooldown then redis.call('DEL',KEYS[1]) end
            return {2,0}
          end
          return {0,0}
          """,
          List.class);

  // Reply: 1 when this call refunded, 0 when the reservation was already refunded.
  private static final DefaultRedisScript<Long> REFUND =
      new DefaultRedisScript<>(
          """
          if not redis.call('SET',KEYS[3],'1','NX','EX',ARGV[3]) then
            return 0
          end
          if redis.call('GET',KEYS[1])==ARGV[1] then
            redis.call('DEL',KEYS[1])
          end
          if ARGV[2]=='1' then
            local v=tonumber(redis.call('GET',KEYS[2]))
            if v and v>0 then redis.call('DECR',KEYS[2]) end
          end
          return 1
          """,
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;
  private final ClockPort clock;

  RedisAiCostGateAdapter(ObjectProvider<StringRedisTemplate> redis, ClockPort clock) {
    this.redis = redis;
    this.clock = clock;
  }

  @Override
  public CostReservation reserve(
      UUID userId, FeatureCode featureCode, int hourlyLimit, int cooldownSeconds) {
    if (!featureCode.hasHourlyQuota()) {
      throw new IllegalArgumentException("No hourly quota for " + featureCode);
    }
    long nowSeconds = clock.now().getEpochSecond();
    long epochHour = nowSeconds / HOUR_SECONDS;
    String requestId = UUID.randomUUID().toString();
    try {
      List<?> reply =
          redis
              .getObject()
              .execute(
                  RESERVE,
                  List.of(cooldownKey(userId), quotaKey(userId, featureCode, epochHour)),
                  requestId,
                  String.valueOf(cooldownSeconds),
                  String.valueOf(hourlyLimit));
      CostReservation base = bypass(userId, featureCode, requestId, epochHour);
      if (reply == null || reply.isEmpty()) {
        return base;
      }
      long code = ((Number) reply.get(0)).longValue();
      if (code == 1) {
        long ttl = ((Number) reply.get(1)).longValue();
        return outcome(GateOutcome.COOLDOWN, base, ttl);
      }
      if (code == 2) {
        long untilNextHour = (epochHour + 1) * HOUR_SECONDS - nowSeconds;
        return outcome(GateOutcome.QUOTA_EXCEEDED, base, untilNextHour);
      }
      return new CostReservation(
          GateOutcome.GRANTED, userId, featureCode, requestId, epochHour, true, 0);
    } catch (RuntimeException unavailable) {
      LOG.log(Level.WARNING, "AI cost gate unavailable; cooldown and quota checks skipped");
      return bypass(userId, featureCode, requestId, epochHour);
    }
  }

  @Override
  public void refund(CostReservation reservation) {
    if (!reservation.granted()) {
      return;
    }
    try {
      redis
          .getObject()
          .execute(
              REFUND,
              List.of(
                  cooldownKey(reservation.userId()),
                  quotaKey(
                      reservation.userId(), reservation.featureCode(), reservation.epochHour()),
                  "ai:refund:" + reservation.requestId()),
              reservation.requestId(),
              reservation.quotaIncremented() ? "1" : "0",
              String.valueOf(REFUND_MARKER_SECONDS));
    } catch (RuntimeException unavailable) {
      LOG.log(Level.WARNING, "AI cost gate unavailable; refund skipped");
    }
  }

  private static CostReservation outcome(GateOutcome outcome, CostReservation base, long retry) {
    return new CostReservation(
        outcome,
        base.userId(),
        base.featureCode(),
        base.requestId(),
        base.epochHour(),
        false,
        retry);
  }

  private static CostReservation bypass(
      UUID userId, FeatureCode feature, String requestId, long epochHour) {
    return new CostReservation(
        GateOutcome.BYPASSED, userId, feature, requestId, epochHour, false, 0);
  }

  private static String cooldownKey(UUID userId) {
    return "ai:cooldown:" + userId;
  }

  private static String quotaKey(UUID userId, FeatureCode feature, long epochHour) {
    return "ai:ratelimit:" + userId + ":" + feature + ":" + epochHour;
  }
}
