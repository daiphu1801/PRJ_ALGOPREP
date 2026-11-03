package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.InstructorClass;

/** Result of #20: the new class and its first invite code, created in one transaction. */
public record CreatedClass(InstructorClass created, ClassInviteCode inviteCode) {}
