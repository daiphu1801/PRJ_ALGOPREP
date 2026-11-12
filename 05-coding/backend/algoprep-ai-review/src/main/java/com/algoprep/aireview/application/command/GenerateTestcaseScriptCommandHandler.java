package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.dto.TestcaseScriptGeneration;
import com.algoprep.aireview.application.ports.in.GenerateTestcaseScriptUseCase;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.ports.out.AiPromptSnapshotRepository;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.aireview.domain.prompt.ServerDirective;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * P3 (DD F5 section 32.2, BR-AI-14). One structured LLM call, no retry, 120 s. No budget gate, no
 * cooldown and no hourly quota apply to this feature, so there is no reservation to refund: a
 * provider failure simply leaves nothing behind. Tokens are written to the ledger BEFORE the answer
 * is validated, so a bad answer still counts against the budget. Not transactional on purpose.
 */
@Service
class GenerateTestcaseScriptCommandHandler implements GenerateTestcaseScriptUseCase {
  static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(120);

  /**
   * Fixed server text appended to the ACTIVE instruction: the contract between the scripts and
   * problem-bank (case separator and validator behaviour). Not user data. [SoT: Suy luan] the DD
   * names neither the case format nor the script language; the seeded v1.0 template says only
   * "Python script, SEED from the environment".
   */
  static final String SCRIPT_CONTRACT =
      "Script sinh dữ liệu chạy đúng một lần với SEED cố định và in nhiều ca; mỗi ca mở đầu bằng"
          + " một dòng `###CASE <nhãn>` (nhãn thuộc TYPICAL, BOUNDARY_LOW, BOUNDARY_HIGH, DEGENERATE,"
          + " DUPLICATE, SORTED, EXTREME) rồi tới dữ liệu đầu vào của ca đó. Script kiểm ràng buộc"
          + " cũng bằng Python, đọc đúng một ca từ stdin và thoát với mã 0 khi hợp lệ.";

  private final AiPromptSnapshotRepository templates;
  private final LlmProviderPort llm;
  private final RecordAiTokenUsageUseCase usage;
  private final PromptComposer composer = new PromptComposer();

  GenerateTestcaseScriptCommandHandler(
      AiPromptSnapshotRepository templates, LlmProviderPort llm, RecordAiTokenUsageUseCase usage) {
    this.templates = templates;
    this.llm = llm;
    this.usage = usage;
  }

  @Override
  public TestcaseScriptGeneration.Output generate(TestcaseScriptGeneration.Input input) {
    PromptSnapshot template =
        templates
            .readActive(FeatureCode.GENERATE_TESTCASE)
            .orElseThrow(
                () ->
                    new AiProviderUnavailableException(
                        AiProviderUnavailableException.Reason.UPSTREAM_PORT, null));
    StructuredResult result = call(template, input);
    String model = result.modelName() == null ? template.modelId() : result.modelName();
    var tokens = result.usage();
    usage.record(
        new RecordAiTokenUsageCommand(
            input.callId(),
            input.userId(),
            FeatureCode.GENERATE_TESTCASE,
            input.problemId(),
            input.problemId(),
            tokens == null ? null : tokens.promptTokens(),
            tokens == null ? null : tokens.completionTokens(),
            model));
    var parsed = TestcaseScriptSchema.parse(result.json());
    return new TestcaseScriptGeneration.Output(
        parsed.generatorScript(),
        parsed.validatorScript(),
        parsed.categoryLabels(),
        model,
        template.id().toString(),
        tokens == null ? 0 : tokens.promptTokens() + tokens.completionTokens());
  }

  private StructuredResult call(PromptSnapshot template, TestcaseScriptGeneration.Input input) {
    var prompt =
        composer.compose(
            withContract(template),
            null,
            null,
            List.of(
                new DataBlock(BlockName.PROBLEM, problemText(input)),
                new DataBlock(BlockName.SAMPLES, sampleText(input))),
            ServerDirective.TESTCASE_SCRIPT_JSON);
    try {
      return llm.completeStructured(
          prompt.system(),
          prompt.perProblem(),
          prompt.data(),
          new ResponseSchema(
              TestcaseScriptSchema.NAME,
              template.responseSchemaVersion(),
              TestcaseScriptSchema.JSON),
          new CallOptions(
              template.modelId(), CallOptions.DEFAULT_MAX_TOKENS, TOTAL_TIMEOUT, TOTAL_TIMEOUT));
    } catch (AiProviderUnavailableException | AiInvalidResponseException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, failure);
    }
  }

  private static PromptSnapshot withContract(PromptSnapshot t) {
    return new PromptSnapshot(
        t.id(),
        t.featureCode(),
        t.systemInstruction() + "\n" + SCRIPT_CONTRACT,
        t.guards(),
        t.modelId(),
        t.responseSchemaVersion());
  }

  static String problemText(TestcaseScriptGeneration.Input input) {
    return "Statement:\n"
        + input.statementMarkdown()
        + "\nData constraints:\n"
        + input.constraintsMarkdown();
  }

  static String sampleText(TestcaseScriptGeneration.Input input) {
    StringBuilder out = new StringBuilder();
    int n = 1;
    for (var sample : input.samples()) {
      out.append("Sample ").append(n++).append(" input:\n").append(sample.input());
      out.append("\nSample ").append(n - 1).append(" expected output:\n");
      out.append(sample.expectedOutput()).append('\n');
    }
    return out.toString();
  }
}
