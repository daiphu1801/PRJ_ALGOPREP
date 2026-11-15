package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.UpdateMySettingsCommand;
import com.algoprep.identity.application.ports.in.UpdateMySettingsUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MySettings;
import com.algoprep.identity.domain.ports.out.MySettingsRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges the sent values over the stored ones and validates them (DD validation F1 section 8). The
 * account row is locked first: the settings screen sends several PATCH calls at once, and without
 * the lock the later one would write back the stored values it read before the earlier one saved.
 * Whether a language is currently enabled belongs to F4; only the three known codes are checked
 * here.
 */
@Service
class UpdateMySettingsCommandHandler implements UpdateMySettingsUseCase {
  private static final Set<String> LEVELS = Set.of("INTERN", "JUNIOR", "MIDDLE", "SENIOR");
  private static final Set<Integer> MAX_TURNS = Set.of(8, 12, 16);
  private static final Set<String> LANGUAGES = Set.of("JAVA", "CPP", "PYTHON");
  private final MySettingsRepository settings;

  UpdateMySettingsCommandHandler(MySettingsRepository settings) {
    this.settings = settings;
  }

  @Override
  @Transactional
  public MySettings update(UpdateMySettingsCommand command) {
    MySettings current =
        settings
            .findForUpdate(command.userId())
            .orElseThrow(
                () ->
                    new UnauthenticatedException(
                        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
    MySettings next =
        new MySettings(
            pick(command.streakReminderEnabled(), current.streakReminderEnabled()),
            pick(command.weeklyReportEnabled(), current.weeklyReportEnabled()),
            oneOf(
                command.preferredInterviewLevel(),
                current.preferredInterviewLevel(),
                LEVELS,
                "IDT-V18"),
            oneOf(command.preferredMaxTurns(), current.preferredMaxTurns(), MAX_TURNS, "IDT-V18"),
            pick(command.preferredHintAllowed(), current.preferredHintAllowed()),
            oneOf(command.defaultLanguage(), current.defaultLanguage(), LANGUAGES, "IDT-413"));
    settings.save(command.userId(), next);
    return next;
  }

  private static <T> T pick(T sent, T stored) {
    return sent == null ? stored : sent;
  }

  private static <T> T oneOf(T sent, T stored, Set<T> allowed, String code) {
    if (sent == null) {
      return stored;
    }
    if (!allowed.contains(sent)) {
      throw new DomainRuleViolationException(code, "Value is not one of the allowed settings");
    }
    return sent;
  }
}
