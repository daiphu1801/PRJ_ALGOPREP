package com.algoprep.identity.application.dto.classes;

import com.algoprep.identity.domain.model.classes.ClassInviteCode;
import com.algoprep.identity.domain.model.classes.InstructorClass;

/** Result of #20: the new class and its first invite code, created in one transaction. */
public record CreatedClass(InstructorClass created, ClassInviteCode inviteCode) {}
