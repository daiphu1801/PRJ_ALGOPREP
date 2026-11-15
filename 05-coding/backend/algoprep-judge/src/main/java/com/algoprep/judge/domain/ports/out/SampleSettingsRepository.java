package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.execution.SampleSettings;
import java.util.List;

public interface SampleSettingsRepository {
  List<SampleSettings> read();
}
