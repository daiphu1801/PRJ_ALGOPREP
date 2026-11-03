package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** Metadata only; no object key or testcase content belongs in a grading-spec read. */
public record TestcaseDescriptor(UUID testcaseId, String visibility, int position) {}
