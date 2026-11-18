package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.domain.model.classes.ClassInviteCode;
import java.util.List;
import java.util.UUID;

/** Lists the unexpired invite codes of an owned class (#24). */
public interface ListClassInviteCodesUseCase {
  List<ClassInviteCode> list(UUID instructorId, UUID classId);
}
