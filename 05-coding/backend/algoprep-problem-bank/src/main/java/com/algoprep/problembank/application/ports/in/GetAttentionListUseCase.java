package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.domain.model.AttentionCount;
import java.util.List;

/** Attention list #26 (DD F2 section 27): four rule counts, always in the same order. */
public interface GetAttentionListUseCase {
  List<AttentionCount> get(AuthoringScope scope);
}
