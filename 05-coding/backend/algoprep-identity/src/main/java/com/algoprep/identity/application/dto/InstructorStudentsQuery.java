package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Filters of #59 as sent: {@code classId} null means every class of the caller. */
public record InstructorStudentsQuery(
    UUID instructorId, UUID classId, String keyword, Integer page, Integer pageSize) {}
