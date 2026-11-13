package com.algoprep.bootstrap.judge;

import com.algoprep.identity.application.ports.in.ReadSubmissionClassScopeUseCase;
import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SubmissionClassScopeBridge implements ClassScopeReadPort {
  private final ReadSubmissionClassScopeUseCase scope;

  SubmissionClassScopeBridge(ReadSubmissionClassScopeUseCase scope) {
    this.scope = scope;
  }

  @Override
  public List<Membership> read(UUID instructorId) {
    try {
      return scope.read(instructorId).stream()
          .map(
              m ->
                  new Membership(
                      m.classId(),
                      m.className(),
                      m.studentId(),
                      m.studentName(),
                      m.joinedAt(),
                      m.leftAt()))
          .toList();
    } catch (RuntimeException failure) {
      throw new IllegalStateException("Submission class scope unavailable");
    }
  }
}
