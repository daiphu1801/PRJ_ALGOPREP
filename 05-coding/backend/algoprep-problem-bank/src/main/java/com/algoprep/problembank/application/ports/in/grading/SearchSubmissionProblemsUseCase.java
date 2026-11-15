package com.algoprep.problembank.application.ports.in;

import java.util.List;
import java.util.UUID;

public interface SearchSubmissionProblemsUseCase {
  List<UUID> search(String keyword, int limit);
}
