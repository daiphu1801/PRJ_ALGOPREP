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
    assertThat(InterviewLlmCall.TOTAL_TIMEOUT).isEqualTo(Duration.ofSeconds(20));
  }
}
