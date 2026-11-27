package com.algoprep.harness.application.ports.in;

import com.algoprep.harness.application.dto.CompileErrorReport;
import com.algoprep.harness.application.dto.MapCompileErrorCommand;

/**
 * P3: maps compiler diagnostics using the supplied source regions, without reading other modules.
 */
public interface MapCompileErrorUseCase {
  CompileErrorReport map(MapCompileErrorCommand command);
}
