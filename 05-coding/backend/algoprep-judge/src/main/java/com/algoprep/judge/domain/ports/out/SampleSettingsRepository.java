package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.SampleSettings;
import java.util.List;

public interface SampleSettingsRepository {
  List<SampleSettings> read();
}
