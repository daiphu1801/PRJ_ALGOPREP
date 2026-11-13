package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.model.MyClass;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Student-side reads and the invite-code join over {@code identity.class_*} tables. */
public interface StudentClassRepository {

  /**
   * Looks up an unexpired invite code and, when found, enrols the student: a new row, or the old
   * row re-activated with {@code joined_at} kept. Already being a member changes nothing.
   *
   * @return the class, or empty when the code does not exist or has expired
   */
  Optional<ClassRef> joinByCode(UUID studentId, String code);

  /** Active enrolments only, newest join first, then class id ascending. */
  List<MyClass> listActive(UUID studentId);

  /** Membership is part of the query: a class the caller is not in is simply absent. */
  Optional<MyClass> findActive(UUID studentId, UUID classId);
}
