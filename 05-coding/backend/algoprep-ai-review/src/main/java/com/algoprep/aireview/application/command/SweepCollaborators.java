package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.out.InterviewSweepRetryPort;
import com.algoprep.common.domain.ports.out.ClockPort;
import org.springframework.stereotype.Component;

/** Constants, retry counter and clock of JOB-AI-04, kept as one bean. */
@Component
record SweepCollaborators(
    InterviewSweepLimits limits, InterviewSweepRetryPort retries, ClockPort clock) {}
