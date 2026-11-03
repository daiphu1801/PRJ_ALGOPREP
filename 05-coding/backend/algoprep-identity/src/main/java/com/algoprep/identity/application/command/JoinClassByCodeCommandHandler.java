package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.dto.JoinClassByCodeCommand;
import com.algoprep.identity.application.ports.in.JoinClassByCodeUseCase;
import com.algoprep.identity.application.ports.out.JoinAttemptLimitPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order per attempt: blocked check first (the code is not even looked at), then validation. Only an
 * unusable code (missing, malformed, expired) adds to the window; success clears it. Every failure
 * answers the same way so a once-valid code cannot be told apart from a wrong one.
 */
@Service
class JoinClassByCodeCommandHandler implements JoinClassByCodeUseCase {
  private static final Pattern CODE = Pattern.compile("^[A-Z0-9]{8}$");
  private final StudentClassRepository classes;
  private final JoinAttemptLimitPort attempts;

  JoinClassByCodeCommandHandler(StudentClassRepository classes, JoinAttemptLimitPort attempts) {
    this.classes = classes;
    this.attempts = attempts;
  }

  @Override
  @Transactional
  public ClassRef join(JoinClassByCodeCommand command) {
    if (attempts.isBlocked(command.userId())) {
      throw new RateLimitExceededException(
          IdentityErrorCodes.INVITE_CODE_RATE_LIMITED, "Too many wrong invite codes");
    }
    String code = command.code() == null ? "" : command.code().strip().toUpperCase(Locale.ROOT);
    Optional<ClassRef> joined =
        CODE.matcher(code).matches()
            ? classes.joinByCode(command.userId(), code)
            : Optional.empty();
    if (joined.isEmpty()) {
      attempts.recordFailure(command.userId());
      throw new DomainRuleViolationException(
          IdentityErrorCodes.INVITE_CODE_INVALID, "Invite code is not valid");
    }
    attempts.clear(command.userId());
    return joined.get();
  }
}
