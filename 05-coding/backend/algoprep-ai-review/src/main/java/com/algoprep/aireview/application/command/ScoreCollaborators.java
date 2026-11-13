package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import org.springframework.stereotype.Component;

/** The helpers {@link InterviewScorer} needs besides its repositories, kept as one bean. */
@Component
record ScoreCollaborators(
    InterviewMemory memory,
    InterviewLlmCall templates,
    InterviewLimits limits,
    RecordAiTokenUsageUseCase usage,
    IdGeneratorPort ids) {}
