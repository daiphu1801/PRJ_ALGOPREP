package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.application.dto.VerifyEmailCommand;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** DD validation boundaries and safe diagnostics, independent of database and transport. */
class RegistrationValidationTest {
  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private final RegistrationValidation validation =
      new RegistrationValidation(FACTORY.getValidator());

  @AfterAll
  static void closeValidator() {
    FACTORY.close();
  }

  @Test
  void normalizationPreservesPasswordAndAcceptsNameBoundaries() {
    RegisterCommand minimum =
        validation.registration(new RegisterCommand(" USER@Example.test ", "Abcd1234", " AB "));
    assertThat(minimum.email()).isEqualTo("user@example.test");
    assertThat(minimum.displayName()).isEqualTo("AB");
    assertThat(minimum.password()).isEqualTo("Abcd1234");
    assertThat(
            validation
                .registration(new RegisterCommand("user@example.test", "Abcd1234", "N".repeat(100)))
                .displayName())
        .hasSize(100);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "no-at-sign", "a b@example.test", "a@@example.test"})
  void invalidEmailsFailWithoutEchoingInput(String email) {
    assertThatThrownBy(() -> validation.email(email))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("A valid email is required");
  }

  @ParameterizedTest
  @CsvSource({"Abc123,IDT-V01", "abcdefgh,IDT-V03", "12345678,IDT-V03", "QAUser123,IDT-V04"})
  void rejectsInvalidPasswordsWithStableCodes(String password, String code) {
    assertThatThrownBy(
            () ->
                validation.registration(
                    new RegisterCommand("qa@example.test", password, "QAUser123")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            error -> assertThat(error.errorCode()).isEqualTo(code))
        .hasMessageNotContaining(password);
  }

  @Test
  void missingPasswordAndPasswordEqualToEmailAreRejected() {
    assertThatThrownBy(
            () -> validation.registration(new RegisterCommand("qa@example.test", null, "QA")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            error -> assertThat(error.errorCode()).isEqualTo("IDT-V01"));
    assertThatThrownBy(
            () ->
                validation.registration(
                    new RegisterCommand("qa1@example.test", "QA1@EXAMPLE.TEST", "QA")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            error -> assertThat(error.errorCode()).isEqualTo("IDT-V04"));
  }

  @Test
  void commandsCannotPrintPasswordsOrOtp() {
    assertThat(new RegisterCommand("qa@example.test", "Sensitive123", "QA").toString())
        .doesNotContain("Sensitive123", "qa@example.test");
    assertThat(new VerifyEmailCommand("qa@example.test", "123456").toString())
        .doesNotContain("123456", "qa@example.test");
  }

  @ParameterizedTest
  @ValueSource(strings = {"A", "é"})
  void passwordMaximumCountsUtf8BytesInsteadOfCharacters(String character) {
    int repetitions = character.equals("A") ? 70 : 35;
    String accepted = character.repeat(repetitions) + "A1";
    assertThat(
            validation
                .registration(new RegisterCommand("qa@example.test", accepted, "QA"))
                .password())
        .isEqualTo(accepted);
    assertThatThrownBy(
            () ->
                validation.registration(
                    new RegisterCommand("qa@example.test", accepted + "x", "QA")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            error -> assertThat(error.errorCode()).isEqualTo("IDT-V02"));
  }
}
