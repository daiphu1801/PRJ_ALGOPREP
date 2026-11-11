package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptSnapshotTest {
  private static PromptSnapshot of(String instruction, ResponseGuards guards) {
    return new PromptSnapshot(
        UUID.randomUUID(), FeatureCode.MOCK_INTERVIEW, instruction, guards, "default", "v1");
  }

  @Test
  void nullGuardsBecomeNoneAndBlankInstructionIsRejected() {
    assertThat(of("x", null).guards()).isEqualTo(ResponseGuards.NONE);
    assertThatThrownBy(() -> of(" ", null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> of(null, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void theSnapshotIsAnImmutableValueWithFinalFieldsAndNoSetters() {
    assertThat(PromptSnapshot.class.isRecord()).isTrue();
    assertThat(
            Arrays.stream(PromptSnapshot.class.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .allMatch(f -> Modifier.isFinal(f.getModifiers())))
        .isTrue();
    assertThat(
            Arrays.stream(PromptSnapshot.class.getMethods())
                .filter(m -> m.getName().startsWith("set")))
        .isEmpty();
    assertThat(ResponseGuards.class.isRecord()).isTrue();
  }

  @Test
  void equalSnapshotsCompareEqual() {
    UUID id = UUID.randomUUID();
    PromptSnapshot a = new PromptSnapshot(id, FeatureCode.SOLUTION_REVIEW, "x", null, "d", "v1");
    PromptSnapshot b =
        new PromptSnapshot(id, FeatureCode.SOLUTION_REVIEW, "x", ResponseGuards.NONE, "d", "v1");
    assertThat(a).isEqualTo(b);
  }
}
