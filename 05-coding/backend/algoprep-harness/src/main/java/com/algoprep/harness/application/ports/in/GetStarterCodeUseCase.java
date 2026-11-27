package com.algoprep.harness.application.ports.in;

import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.StarterCodeResult;

public interface GetStarterCodeUseCase {
  StarterCodeResult getStarterCode(GetStarterCodeQuery query);
}
