package com.algoprep.problembank.application.dto;

import java.util.UUID;

/**
 * The caller of DD F2 section 34. The permission flags come from the token's {@code
 * Function:Action} cells; {@code admin} from its base category.
 */
public record ProblemAuthoringQuery(
    UUID problemId, UUID actorId, boolean admin, boolean canReadTestcases) {}
