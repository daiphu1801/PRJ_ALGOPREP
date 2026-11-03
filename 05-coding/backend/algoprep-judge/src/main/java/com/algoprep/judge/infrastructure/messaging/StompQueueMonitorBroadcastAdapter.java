package com.algoprep.judge.infrastructure.messaging;

import com.algoprep.judge.application.dto.QueueMonitorBatch;
import com.algoprep.judge.application.ports.out.QueueMonitorBroadcastPort;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Component
class StompQueueMonitorBroadcastAdapter implements QueueMonitorBroadcastPort {
  private final SimpMessagingTemplate messaging;

  StompQueueMonitorBroadcastAdapter(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  @Override
  public void publish(QueueMonitorBatch batch) {
    var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
    headers.setLeaveMutable(true);
    messaging.convertAndSend("/topic/admin/queue", batch, headers.getMessageHeaders());
  }
}
