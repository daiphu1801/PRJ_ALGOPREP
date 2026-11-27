package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.ReferenceRunQuotaPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Sorted set {@code problem:reference-run:rate:<userId>}, one atomic Lua step per reservation. */
@Component
class RedisReferenceRunQuotaAdapter implements ReferenceRunQuotaPort {
  static final int MAX_RUNS = 10;
  static final int WINDOW_SECONDS = 60;
  private static final Logger LOG = LoggerFactory.getLogger(RedisReferenceRunQuotaAdapter.class);
  private static final DefaultRedisScript<Long> RESERVE =
      new DefaultRedisScript<>(
          """
          local t=redis.call('TIME')
          local now=tonumber(t[1])*1000+math.floor(tonumber(t[2])/1000)
          local window=tonumber(ARGV[2])*1000
          redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',now-window)
          if redis.call('ZCARD',KEYS[1])>=tonumber(ARGV[3]) then
            local oldest=redis.call('ZRANGE',KEYS[1],0,0,'WITHSCORES')
            return math.max(1,math.ceil((tonumber(oldest[2])+window-now)/1000))
          end
          redis.call('ZADD',KEYS[1],now,ARGV[1])
          redis.call('PEXPIRE',KEYS[1],window)
          return 0
          """,
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisReferenceRunQuotaAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public long reserve(UUID userId, UUID requestId) {
    try {
      Long retry =
          redis
              .getObject()
              .execute(
                  RESERVE,
                  List.of(key(userId)),
                  requestId.toString(),
                  String.valueOf(WINDOW_SECONDS),
                  String.valueOf(MAX_RUNS));
      if (retry == null) {
        throw new IllegalStateException("Missing quota reply");
      }
      return retry;
    } catch (RuntimeException unavailable) {
      LOG.warn("Reference run quota unavailable; the run goes ahead");
      return 0;
    }
  }

  @Override
  public void refund(UUID userId, UUID requestId) {
    try {
      redis.getObject().opsForZSet().remove(key(userId), requestId.toString());
    } catch (RuntimeException unavailable) {
      LOG.warn("Reference run quota refund unavailable");
    }
  }

  private static String key(UUID userId) {
    return "problem:reference-run:rate:" + userId;
  }
}
