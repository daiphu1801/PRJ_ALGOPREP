package com.algoprep.judge.domain.model.read;

import com.algoprep.judge.domain.model.InstructorReadWindow;
import java.util.List;
import java.util.UUID;

public record InstructorReadFilter(
    List<InstructorReadWindow> windows,
    UUID classId,
    UUID submissionId,
    String result,
    List<UUID> keywordStudents,
    List<UUID> keywordProblems) {}
