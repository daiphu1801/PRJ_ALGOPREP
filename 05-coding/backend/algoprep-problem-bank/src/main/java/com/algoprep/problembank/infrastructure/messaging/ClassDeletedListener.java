package com.algoprep.problembank.infrastructure.messaging;

import com.algoprep.problembank.application.ports.in.ApplyClassDeletedUseCase;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Malformed envelopes are rejected; persistence failures escape and are requeued. */
@Component("problemClassDeletedListener")
class ClassDeletedListener {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ApplyClassDeletedUseCase useCase;

  ClassDeletedListener(ApplyClassDeletedUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = ClassDeletedBindingConfig.QUEUE)
  void onMessage(byte[] body) {
    UUID classId;
    try {
      var root = JSON.readTree(body);
      if (!"ClassDeleted".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      UUID.fromString(root.path("eventId").asString(""));
      Instant.parse(root.path("occurredAt").asString(""));
      classId = UUID.fromString(root.path("payload").path("classId").asString(""));
    } catch (RuntimeException invalid) {
      throw new AmqpRejectAndDontRequeueException("invalid ClassDeleted envelope");
    }
    useCase.apply(classId);
  }
}
