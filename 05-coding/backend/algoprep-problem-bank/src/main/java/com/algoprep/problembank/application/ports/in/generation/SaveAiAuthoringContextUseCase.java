package com.algoprep.problembank.application.ports.in.generation;

import com.algoprep.problembank.application.dto.generation.SaveAiContextCommand;

/** Endpoint #38, DD F2 section 39. */
public interface SaveAiAuthoringContextUseCase {
  SaveAiContextCommand.Result save(SaveAiContextCommand command);
}
