package com.algoprep.judge.domain.model;

import java.util.List;
import java.util.UUID;

public record InstructorReadFilter(
    List<InstructorReadWindow> windows,
    UUID classId,
    UUID submissionId,
    String result,
    List<UUID> keywordStudents,
    List<UUID> keywordProblems) {}
