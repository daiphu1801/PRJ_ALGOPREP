package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.StudentClassModels.MyAssignment;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Active assignments ({@code removed_at IS NULL}) of exactly the requested classes. */
public interface StudentClassAssignmentReadRepository {
  /**
   * {@code publishedOnly} drops problems that are unpublished or deleted instead of flagging them.
   */
  List<MyAssignment> active(Set<UUID> classIds, boolean publishedOnly);
}
