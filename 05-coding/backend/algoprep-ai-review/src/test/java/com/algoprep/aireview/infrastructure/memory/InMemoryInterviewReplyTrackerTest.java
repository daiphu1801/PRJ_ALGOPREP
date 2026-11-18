package com.algoprep.aireview.infrastructure.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryInterviewReplyTrackerTest {
  @Test
  void beginAndEndToggleGeneratingPerSession() {
    var tracker = new InMemoryInterviewReplyTracker();
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();

    tracker.begin(a);

    assertThat(tracker.isGenerating(a)).isTrue();
    assertThat(tracker.isGenerating(b)).isFalse();
    tracker.end(a);
    assertThat(tracker.isGenerating(a)).isFalse();
  }
}
