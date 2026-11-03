package com.algoprep.judge.infrastructure.ratelimit;

import com.algoprep.judge.application.dto.SampleRun.Reservation;
import com.algoprep.judge.application.ports.out.SampleQuotaPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisSampleQuotaAdapter implements SampleQuotaPort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisSampleQuotaAdapter.class);
  private static final DefaultRedisScript<Long> RESERVE =
      new DefaultRedisScript<>(
          """
      local t=redis.call('TIME')
      local now=tonumber(t[1])*1000+math.floor(tonumber(t[2])/1000)
      redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',now-3600000)
      if redis.call('ZCARD',KEYS[1])>=60 then
        local oldest=redis.call('ZRANGE',KEYS[1],0,0,'WITHSCORES')
        return math.max(1,math.ceil((tonumber(oldest[2])+3600000-now)/1000))
      end
      redis.call('ZADD',KEYS[1],now,ARGV[1])
      redis.call('PEXPIRE',KEYS[1],3600000)
      return 0
      """,
          Long.class);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisSampleQuotaAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public Reservation reserve(UUID userId, UUID requestId) {
    try {
      Long retry = redis.getObject().execute(RESERVE, List.of(key(userId)), requestId.toString());
      if (retry == null) {
        throw new IllegalStateException("Missing quota reply");
      }
      return new Reservation(retry == 0, retry);
    } catch (RuntimeException unavailable) {
      LOG.warn("Run sample quota unavailable; concurrency limit remains active");
      return new Reservation(false, 0);
    }
  }

  @Override
  public void refund(UUID userId, UUID requestId) {
    try {
      redis.getObject().opsForZSet().remove(key(userId), requestId.toString());
    } catch (RuntimeException unavailable) {
      LOG.warn("Run sample quota refund unavailable");
    }
  }

  private static String key(UUID userId) {
    return "judge:run-sample:rate:" + userId;
  }
}
