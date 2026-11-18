package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import java.util.UUID;

/** Identity fields are supplied by the trusted inbound adapter. */
public record GetStarterCodeQuery(
    UUID problemId,
    Language language,
    SubmissionModel submissionModel,
    UUID callerUserId,
    String baseCategory) {}
