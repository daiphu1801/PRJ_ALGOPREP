package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.AdminQueueFlag;
import com.algoprep.judge.application.dto.QueuePauseChange;
import com.algoprep.judge.application.dto.QueueSettingsChange;
import com.algoprep.judge.application.ports.in.ChangeQueuePauseUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueueAutoscaleUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueuePausedUseCase;
import com.algoprep.judge.application.ports.in.UpdateAdminQueueSettingsUseCase;
import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.JudgeAuditEntry;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import com.algoprep.judge.domain.ports.out.JudgeAuditPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** The outer transaction deliberately contains the existing resume kernel and identity audit. */
@Service
class ChangeAdminQueueCommandHandler
    implements SetAdminQueuePausedUseCase,
        SetAdminQueueAutoscaleUseCase,
        UpdateAdminQueueSettingsUseCase {
  private final AdminQueueRepository repository;
  private final ChangeQueuePauseUseCase pause;
  private final JudgeAuditPort audit;
  private final JsonMapper json = JsonMapper.builder().build();

  ChangeAdminQueueCommandHandler(
      AdminQueueRepository repository, ChangeQueuePauseUseCase pause, JudgeAuditPort audit) {
    this.repository = repository;
    this.pause = pause;
    this.audit = audit;
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public AdminQueueFlag setPaused(UUID actor, boolean expectedPaused, boolean paused) {
    Objects.requireNonNull(actor);
    var before = repository.lock();
    var result = pause.change(expectedPaused, paused);
    if (result == QueuePauseChange.CONFLICT) {
      throw new AdminConfigException(409, "JDG-201");
    }
    if (result == QueuePauseChange.UNCHANGED) {
      return new AdminQueueFlag(before.paused(), before.settings().updatedAt());
    }
    repository.markPauseActor(actor);
    record(
        actor,
        "QUEUE_PAUSE_CHANGED",
        Map.of("queuePaused", before.paused()),
        Map.of("queuePaused", paused));
    var after = repository.read();
    return new AdminQueueFlag(after.paused(), after.settings().updatedAt());
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public AdminQueueFlag setAutoscale(UUID actor, boolean expectedEnabled, boolean enabled) {
    Objects.requireNonNull(actor);
    var before = repository.lock();
    if (before.autoscaleEnabled() != expectedEnabled) {
      throw new AdminConfigException(409, "JDG-201");
    }
    if (before.autoscaleEnabled() == enabled) {
      return new AdminQueueFlag(enabled, before.settings().updatedAt());
    }
    repository.setAutoscale(enabled, actor);
    record(
        actor,
        "QUEUE_AUTOSCALE_CHANGED",
        Map.of("autoscaleEnabled", before.autoscaleEnabled()),
        Map.of("autoscaleEnabled", enabled));
    var after = repository.read();
    return new AdminQueueFlag(after.autoscaleEnabled(), after.settings().updatedAt());
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public AdminQueueSettings update(UUID actor, QueueSettingsChange requested) {
    Objects.requireNonNull(actor);
    var before = repository.lock().settings();
    var oldFields = before.values();
    Map<String, Integer> oldDiff = new LinkedHashMap<>();
    Map<String, Integer> newDiff = new LinkedHashMap<>();
    requested
        .values()
        .forEach(
            (key, value) -> {
              if (!value.equals(oldFields.get(key))) {
                oldDiff.put(key, oldFields.get(key));
                newDiff.put(key, value);
              }
            });
    if (newDiff.isEmpty()) {
      return before;
    }
    repository.saveSettings(newDiff, actor);
    record(actor, "QUEUE_SETTINGS_CHANGED", oldDiff, newDiff);
    return repository.read().settings();
  }

  private void record(UUID actor, String action, Map<String, ?> before, Map<String, ?> after) {
    audit.record(
        new JudgeAuditEntry(
            actor,
            action,
            "CLUSTER_SETTINGS",
            "1",
            json.writeValueAsString(before),
            json.writeValueAsString(after)));
  }
}
