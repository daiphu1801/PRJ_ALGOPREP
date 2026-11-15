package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.SaveAiContextCommand;

/** Endpoint #38, DD F2 section 39. */
public interface SaveAiAuthoringContextUseCase {
  SaveAiContextCommand.Result save(SaveAiContextCommand command);
}
