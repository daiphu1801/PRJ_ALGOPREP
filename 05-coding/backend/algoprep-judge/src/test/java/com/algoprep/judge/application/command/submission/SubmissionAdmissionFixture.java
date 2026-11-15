package com.algoprep.judge.application.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;

class SubmissionAdmissionFixture {
  final SubmissionAdmissionRepository repository = mock(SubmissionAdmissionRepository.class);
  final ProblemGradingSpecPort problems = mock(ProblemGradingSpecPort.class);
  final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
  final Owner owner = new Owner(UUID.randomUUID(), UUID.randomUUID());
  final UUID submissionId = UUID.randomUUID();
  final Instant at = Instant.parse("2026-11-03T03:00:00Z");

  SubmissionAdmissionFixture() {
    spec(true, true, true, 1, null);
    when(repository.languageEnabled("PYTHON")).thenReturn(true);
    when(repository.databaseNow()).thenReturn(at);
    when(ids.newId()).thenReturn(submissionId);
  }

  RecordPendingSubmissionCommand command() {
    return new RecordPendingSubmissionCommand(
        owner, new Program("PYTHON", "STANDARD_IO", " print('đáp án')\r\n", "EDITOR"), null);
  }

  RecordPendingSubmissionCommand command(
      String source, String language, String model, String method, String file) {
    return new RecordPendingSubmissionCommand(
        owner, new Program(language, model, source, method), file);
  }

  void spec(boolean published, boolean ready, boolean wrapper, int count, Integer quota) {
    var cases =
        java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new GradingSpecification.Testcase(UUID.randomUUID(), "HIDDEN", i + 1))
            .toList();
    when(problems.loadGradingSpec(owner.problemId()))
        .thenReturn(
            Optional.of(
                new GradingSpecification(
                    new GradingSpecification.Problem(owner.problemId(), published, wrapper, ready),
                    new GradingSpecification.Limits(1000, 256, 64, quota),
                    new GradingSpecification.Versions(17, 4),
                    new GradingSpecification.Matching("EXACT", null),
                    List.copyOf(cases))));
  }

  RecordPendingSubmissionCommandHandler handler() {
    return new RecordPendingSubmissionCommandHandler(repository, problems, ids, manager, 60);
  }
}
