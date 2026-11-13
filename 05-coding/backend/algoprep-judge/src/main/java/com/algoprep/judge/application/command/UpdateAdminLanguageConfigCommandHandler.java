package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.AdminConfigFields;
import com.algoprep.judge.application.ports.in.UpdateAdminLanguageConfigUseCase;
import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.JudgeAuditEntry;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import com.algoprep.judge.domain.ports.out.JudgeAuditPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
class UpdateAdminLanguageConfigCommandHandler implements UpdateAdminLanguageConfigUseCase {
  private final AdminLanguageConfigRepository repository;
  private final JudgeAuditPort audit;
  private final JsonMapper json = JsonMapper.builder().build();

  UpdateAdminLanguageConfigCommandHandler(
      AdminLanguageConfigRepository repository, JudgeAuditPort audit) {
    this.repository = repository;
    this.audit = audit;
  }

  @Override
  @Transactional
  public AdminLanguageConfig update(UUID actor, AdminLanguageConfig requested) {
    Objects.requireNonNull(actor);
    validateRules(requested);
    var before = repository.lock();
    checkVersions(before, requested);
    Map<String, Object> oldFields = AdminConfigFields.values(before);
    Map<String, Object> newFields = AdminConfigFields.values(requested);
    Map<String, Object> oldDiff = new LinkedHashMap<>();
    Map<String, Object> newDiff = new LinkedHashMap<>();
    newFields.forEach(
        (key, value) -> {
          if (!value.equals(oldFields.get(key))) {
            oldDiff.put(key, oldFields.get(key));
            newDiff.put(key, value);
          }
        });
    if (newDiff.isEmpty()) {
      return before;
    }
    repository.saveChanges(before, requested, actor);
    audit.record(
        new JudgeAuditEntry(
            actor,
            "LANGUAGE_CONFIG_CHANGED",
            "LANGUAGE_CONFIG",
            "ALL",
            json.writeValueAsString(oldDiff),
            json.writeValueAsString(newDiff)));
    return repository.read();
  }

  private static void validateRules(AdminLanguageConfig config) {
    if (config.languages().stream().noneMatch(AdminLanguageConfig.Language::enabled)) {
      throw new AdminConfigException(422, "JDG-203");
    }
    for (var language : config.languages()) {
      checkEffective(
          config.gradingDefaults().timeLimitMs(),
          language.timeLimitMultiplier(),
          100,
          30000,
          "JDG-204");
      checkEffective(
          config.gradingDefaults().memoryLimitMb(),
          language.memoryLimitMultiplier(),
          64,
          1024,
          "JDG-205");
    }
  }

  private static void checkEffective(
      int base, BigDecimal multiplier, int min, int max, String code) {
    BigDecimal value =
        multiplier.multiply(BigDecimal.valueOf(base)).setScale(0, RoundingMode.HALF_UP);
    if (value.compareTo(BigDecimal.valueOf(min)) < 0
        || value.compareTo(BigDecimal.valueOf(max)) > 0) {
      throw new AdminConfigException(422, code);
    }
  }

  private static void checkVersions(AdminLanguageConfig before, AdminLanguageConfig requested) {
    for (var language : before.languages()) {
      var sent =
          requested.languages().stream()
              .filter(row -> row.language().equals(language.language()))
              .findFirst()
              .orElseThrow();
      if (!language.updatedAt().equals(sent.updatedAt())) {
        throw new AdminConfigException(409, "JDG-202");
      }
    }
    if (!before.gradingDefaults().updatedAt().equals(requested.gradingDefaults().updatedAt())
        || !before.sandbox().updatedAt().equals(requested.sandbox().updatedAt())
        || !before
            .generatorSandbox()
            .updatedAt()
            .equals(requested.generatorSandbox().updatedAt())) {
      throw new AdminConfigException(409, "JDG-202");
    }
  }
}
