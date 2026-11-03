package com.algoprep.judge.infrastructure.worker;

import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** JSON is ID-only and acknowledgements are made only after the application boundary returns. */
final class WorkerMessageListener implements ChannelAwareMessageListener {
  private static final Logger LOG = LoggerFactory.getLogger(WorkerMessageListener.class);
  private final ProcessQueuedSubmissionUseCase process;
  private final JsonMapper json =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  WorkerMessageListener(ProcessQueuedSubmissionUseCase process) {
    this.process = process;
  }

  @Override
  public void onMessage(Message message, Channel channel) throws IOException {
    if (channel == null) {
      throw new IllegalStateException("Manual acknowledgement requires a channel");
    }
    long tag = message.getMessageProperties().getDeliveryTag();
    UUID id;
    try {
      id = id(message.getBody());
    } catch (RuntimeException invalid) {
      LOG.error("Invalid grading queue message");
      channel.basicReject(tag, false);
      return;
    }
    try {
      process.process(id);
    } catch (RuntimeException unavailable) {
      LOG.warn("Grading persistence or ownership is unavailable; message will be retried");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      channel.basicNack(tag, false, true);
      return;
    }
    channel.basicAck(tag, false);
  }

  private UUID id(byte[] body) {
    if (body.length > 128) {
      throw new IllegalArgumentException("Invalid queue message");
    }
    var node = json.readTree(body);
    if (node == null
        || !node.isObject()
        || node.size() != 1
        || !node.path("submissionId").isString()) {
      throw new IllegalArgumentException("Invalid queue message");
    }
    String text = node.path("submissionId").asString();
    UUID id = UUID.fromString(text);
    if (!id.toString().equalsIgnoreCase(text)) {
      throw new IllegalArgumentException("Invalid queue message");
    }
    return id;
  }
}
