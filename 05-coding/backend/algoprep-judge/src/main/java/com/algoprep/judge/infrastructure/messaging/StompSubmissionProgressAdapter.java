package com.algoprep.judge.infrastructure.messaging;

import com.algoprep.judge.application.dto.SubmissionProgress;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MimeTypeUtils;

@Component
class StompSubmissionProgressAdapter implements SubmissionProgressPort {
  private static final Logger LOG = LoggerFactory.getLogger(StompSubmissionProgressAdapter.class);
  private final SimpMessagingTemplate messaging;

  StompSubmissionProgressAdapter(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  @Override
  public void afterCommit(UUID ownerId, SubmissionProgress progress) {
    Objects.requireNonNull(ownerId);
    Objects.requireNonNull(progress);
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(ownerId, progress);
            }
          });
    } else {
      send(ownerId, progress);
    }
  }

  private void send(UUID ownerId, SubmissionProgress progress) {
    try {
      var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
      headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
      headers.setLeaveMutable(true);
      messaging.convertAndSendToUser(
          ownerId.toString(), "/queue/submissions", progress, headers.getMessageHeaders());
    } catch (RuntimeException error) {
      LOG.warn("Submission progress delivery failed");
    }
  }
}
