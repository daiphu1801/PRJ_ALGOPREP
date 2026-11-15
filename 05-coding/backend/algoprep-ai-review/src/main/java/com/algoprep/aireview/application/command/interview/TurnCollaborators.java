package com.algoprep.aireview.application.command.interview;

import com.algoprep.aireview.application.ports.in.budget.CheckAiBudgetUseCase;
import org.springframework.stereotype.Component;

/** The collaborators a turn needs to build and run its LLM call, kept as one bean. */
@Component
record TurnCollaborators(
    InterviewMemory memory,
    InterviewLlmCall llm,
    InterviewLimits limits,
    CheckAiBudgetUseCase budget) {}
