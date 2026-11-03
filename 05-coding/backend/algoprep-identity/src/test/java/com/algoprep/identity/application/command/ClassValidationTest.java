package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.domain.model.ScheduleSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

/** DD validation F1 section 5: IDT-V14 name, IDT-V15 description, IDT-V10 and IDT-V16 schedule. */
class ClassValidationTest {

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private static ScheduleSpec schedule(
      List<Integer> days, String start, String end, String from, String to) {
    return new ScheduleSpec(days, start, end, null, from, to, null);
  }

  @Test
  void nameIsTrimmedAndMustBeOneTo120Characters() {
    assertThat(ClassValidation.name("  Algo K21  ")).isEqualTo("Algo K21");
    assertThat(ClassValidation.name("x".repeat(120))).hasSize(120);
    assertThatThrownBy(() -> ClassValidation.name("   ")).satisfies(e -> assertCode(e, "IDT-V14"));
    assertThatThrownBy(() -> ClassValidation.name(null)).satisfies(e -> assertCode(e, "IDT-V14"));
    assertThatThrownBy(() -> ClassValidation.name("x".repeat(121)))
        .satisfies(e -> assertCode(e, "IDT-V14"));
  }

  @Test
  void descriptionIsOptionalAndAtMost500Characters() {
    assertThat(ClassValidation.description("  ")).isNull();
    assertThat(ClassValidation.description(null)).isNull();
    assertThat(ClassValidation.description(" Intro ")).isEqualTo("Intro");
    assertThat(ClassValidation.description("x".repeat(500))).hasSize(500);
    assertThatThrownBy(() -> ClassValidation.description("x".repeat(501)))
        .satisfies(e -> assertCode(e, "IDT-V15"));
  }

  @Test
  void nothingSetMeansNoSchedule() {
    assertThat(ClassValidation.schedule(null)).isNull();
    assertThat(ClassValidation.schedule(new ScheduleSpec(null, null, null, " ", "", null, null)))
        .isNull();
  }

  @Test
  void aValidScheduleIsNormalised() {
    ScheduleSpec result =
        ClassValidation.schedule(
            new ScheduleSpec(
                List.of(5, 2), "09:00", "10:30", " B2.01 ", "2026-09-01", "2026-12-31", " note "));

    assertThat(result)
        .isEqualTo(
            new ScheduleSpec(
                List.of(2, 5), "09:00", "10:30", "B2.01", "2026-09-01", "2026-12-31", "note"));
  }

  @Test
  void roomAndNoteHaveLengthLimitsWithCodeV10() {
    assertThatThrownBy(
            () ->
                ClassValidation.schedule(
                    new ScheduleSpec(null, null, null, "r".repeat(101), null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V10"));
    assertThatThrownBy(
            () ->
                ClassValidation.schedule(
                    new ScheduleSpec(null, null, null, null, null, null, "n".repeat(201))))
        .satisfies(e -> assertCode(e, "IDT-V10"));
    assertThat(
            ClassValidation.schedule(
                    new ScheduleSpec(
                        null, null, null, "r".repeat(100), null, null, "n".repeat(200)))
                .note())
        .hasSize(200);
  }

  @Test
  void structureDatesAndTimesAreRejectedWithCodeV16() {
    List<ScheduleSpec> bad =
        List.of(
            schedule(List.of(0), null, null, null, null),
            schedule(List.of(8), null, null, null, null),
            schedule(List.of(1, 1), null, null, null, null),
            schedule(java.util.Arrays.asList(1, null), null, null, null, null),
            schedule(List.of(1), "09:00", null, null, null),
            schedule(List.of(1), "9am", "10:00", null, null),
            schedule(List.of(1), "25:00", "26:00", null, null),
            schedule(List.of(1), "10:00", "10:00", null, null),
            schedule(List.of(1), "11:00", "10:00", null, null),
            schedule(List.of(), "09:00", "10:00", null, null),
            schedule(List.of(1), null, null, "2026-13-01", null),
            schedule(List.of(1), null, null, "2026-09-10", "2026-09-09"));
    for (ScheduleSpec s : bad) {
      assertThatThrownBy(() -> ClassValidation.schedule(s))
          .as("schedule %s", s)
          .satisfies(e -> assertCode(e, "IDT-V16"));
    }
  }
}
