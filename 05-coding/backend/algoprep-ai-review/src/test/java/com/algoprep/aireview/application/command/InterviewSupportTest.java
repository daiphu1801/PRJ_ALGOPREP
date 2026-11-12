package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.ports.out.AiPromptSnapshotRepository;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

class InterviewSupportTest {
  @Test
  void parserAcceptsJsonInsideFencesAndDefaultsStageComplete() {
    var reply = InterviewReplyParser.parse("```json\n{\"message\":\" hi \"}\n```");
    assertThat(reply.message()).isEqualTo("hi");
    assertThat(reply.stageComplete()).isFalse();
    assertThat(
            InterviewReplyParser.parse("{\"message\":\"x\",\"stageComplete\":true}")
                .stageComplete())
        .isTrue();
  }

  @Test
  void parserRejectsBadShapes() {
    for (String bad :
        List.of(
            "plain text",
            "{\"message\":\"\"}",
            "{\"message\":5}",
            "{\"stageComplete\":true}",
            "{\"message\":\"x\",\"stageComplete\":\"yes\"}",
            "{\"message\":")) {
      assertThatThrownBy(() -> InterviewReplyParser.parse(bad))
          .as(bad)
          .isInstanceOf(AiInvalidResponseException.class);
    }
  }

  @Test
  void hintPlainTextUnwrapsJsonAndRejectsEmpty() {
    assertThat(InterviewReplyParser.plainText("  just text ")).isEqualTo("just text");
    assertThat(InterviewReplyParser.plainText("{\"message\":\"wrapped\"}")).isEqualTo("wrapped");
    assertThatThrownBy(() -> InterviewReplyParser.plainText("  "))
        .isInstanceOf(AiInvalidResponseException.class);
    assertThatThrownBy(() -> InterviewReplyParser.plainText(null))
        .isInstanceOf(AiInvalidResponseException.class);
  }

  @Test
  void blocksSplitContextFromTranscript() {
    List<DataBlock> blocks =
        InterviewMemory.blocks(
            List.of(
                new ChatMessage(ChatMessage.Kind.PROBLEM, "p"),
                new ChatMessage(ChatMessage.Kind.USER_CODE, "c"),
                new ChatMessage(ChatMessage.Kind.QUESTION, "q"),
                new ChatMessage(ChatMessage.Kind.TOPICS, "t"),
                new ChatMessage(ChatMessage.Kind.AI, "hello"),
                new ChatMessage(ChatMessage.Kind.USER, "my answer")));
    assertThat(blocks)
        .extracting(DataBlock::name)
        .containsExactly(
            BlockName.PROBLEM,
            BlockName.USER_CODE,
            BlockName.QUESTION,
            BlockName.TOPICS,
            BlockName.TRANSCRIPT);
    assertThat(blocks.get(4).content()).isEqualTo("Interviewer: hello\nCandidate: my answer");
    assertThat(ChatMessage.Kind.USER.isContext()).isFalse();
    assertThat(ChatMessage.Kind.TOPICS.isContext()).isTrue();
  }

  @Test
  void llmCallMapsDeadlineAndNullToAir501() {
    var llm = mock(LlmProviderPort.class);
    var usage = mock(RecordAiTokenUsageUseCase.class);
    var ids = mock(IdGeneratorPort.class);
    when(ids.newId()).thenReturn(UUID.randomUUID());
    var call = new InterviewLlmCall(mock(AiPromptSnapshotRepository.class), llm, usage, ids);
    var template =
        new PromptSnapshot(UUID.randomUUID(), FeatureCode.MOCK_INTERVIEW, "s", null, "m", "v1");
    ComposedPrompt prompt =
        new PromptComposer()
            .compose(template, null, null, List.of(new DataBlock(BlockName.TOPICS, "t")));

    when(llm.streamTurn(any(), any(), any(), any(), any()))
        .thenReturn(
            Flux.error(
                new AiProviderUnavailableException(
                    AiProviderUnavailableException.Reason.TIMEOUT, null)));
    assertThatThrownBy(() -> call.collect(template, prompt, UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOfSatisfying(
            AiProviderUnavailableException.class,
            e -> assertThat(e.reason()).isEqualTo(AiProviderUnavailableException.Reason.TIMEOUT));

    when(llm.streamTurn(any(), any(), any(), any(), any())).thenReturn(Flux.empty());
    assertThat(call.collect(template, prompt, UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    ArgumentCaptor<RecordAiTokenUsageCommand> ledger =
        ArgumentCaptor.forClass(RecordAiTokenUsageCommand.class);
    verify(usage).record(ledger.capture());
    assertThat(ledger.getValue().usageKnown()).isFalse();
  }

  @Test
  void theShortCallTimeoutsDefaultToSixtyAndThirtySecondsAndTheKeysAreBound() throws Exception {
    var total = InterviewLlmCall.class.getDeclaredField("callTotalSec");
    var first = InterviewLlmCall.class.getDeclaredField("firstTokenSec");
    assertThat(
            total.getAnnotation(org.springframework.beans.factory.annotation.Value.class).value())
        .isEqualTo("${algoprep.ai.llm.timeout.interview-call-total-sec:60}");
    assertThat(
            first.getAnnotation(org.springframework.beans.factory.annotation.Value.class).value())
        .isEqualTo("${algoprep.ai.llm.timeout.interview-first-token-sec:30}");
    var options = sentOptions(call -> {});
    assertThat(options.totalTimeout()).isEqualTo(Duration.ofSeconds(60));
    assertThat(options.firstTokenTimeout()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void overriddenShortCallTimeoutsReachTheProviderCall() {
    var options =
        sentOptions(
            call -> {
              call.callTotalSec = 11;
              call.firstTokenSec = 5;
            });
    assertThat(options.totalTimeout()).isEqualTo(Duration.ofSeconds(11));
    assertThat(options.firstTokenTimeout()).isEqualTo(Duration.ofSeconds(5));
  }

  private com.algoprep.aireview.domain.model.CallOptions sentOptions(
      java.util.function.Consumer<InterviewLlmCall> tweak) {
    var llm = mock(LlmProviderPort.class);
    var ids = mock(IdGeneratorPort.class);
    when(ids.newId()).thenReturn(UUID.randomUUID());
    var call =
        new InterviewLlmCall(
            mock(AiPromptSnapshotRepository.class),
            llm,
            mock(RecordAiTokenUsageUseCase.class),
            ids);
    tweak.accept(call);
    when(llm.streamTurn(any(), any(), any(), any(), any())).thenReturn(Flux.empty());
    var template =
        new PromptSnapshot(UUID.randomUUID(), FeatureCode.MOCK_INTERVIEW, "s", null, "m", "v1");
    ComposedPrompt prompt =
        new PromptComposer()
            .compose(template, null, null, List.of(new DataBlock(BlockName.TOPICS, "t")));
    call.collect(template, prompt, UUID.randomUUID(), UUID.randomUUID());
    var captured = ArgumentCaptor.forClass(com.algoprep.aireview.domain.model.CallOptions.class);
    verify(llm).streamTurn(any(), any(), any(), any(), captured.capture());
    return captured.getValue();
  }
}
