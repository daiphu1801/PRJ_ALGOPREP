package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.aireview.application.ports.out.ProblemContextPort;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ProblemContext;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.ProblemGuidance;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * BR-AI-02 steps 8 to 10: fetch the problem context, compose the prompt, call the LLM once (no
 * retry), write the token usage in its own transaction, then validate and normalise the answer. A
 * bad answer is rejected AFTER its tokens were recorded, so the budget never under-counts.
 */
@Component
class ReviewLlmCall {
  // ponytail: constants instead of application.yml keys (DD Bang 2 lists them as yml values); move
  // them to @ConfigurationProperties when the OpenRouter adapter (F5-2) needs tuning per model.
  static final int MAX_TOKENS = CallOptions.DEFAULT_MAX_TOKENS;
  static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(30);
  static final String SCHEMA_NAME = "solution_review";

  private final LlmProviderPort llm;
  private final ProblemContextPort problems;
  private final RecordAiTokenUsageUseCase usage;
  private final IdGeneratorPort ids;
  private final PromptComposer composer = new PromptComposer();

  ReviewLlmCall(
      LlmProviderPort llm,
      ProblemContextPort problems,
      RecordAiTokenUsageUseCase usage,
      IdGeneratorPort ids) {
    this.llm = llm;
    this.problems = problems;
    this.usage = usage;
    this.ids = ids;
  }

  SolutionReviewNormalizer.Normalized generate(
      SubmissionSnapshot source, PromptSnapshot template, UUID reviewId) {
    ProblemContext context = problems.getContext(source.problemId(), source.language());
    ComposedPrompt prompt =
        composer.compose(
            template,
            new ProblemGuidance(context.aiBrief(), context.noFullCode(), context.socraticOnly()),
            null,
            List.of(
                new DataBlock(BlockName.PROBLEM, problemText(context)),
                new DataBlock(BlockName.USER_CODE, source.sourceCode())));
    UUID callId = ids.newId();
    StructuredResult result;
    try {
      result =
          llm.completeStructured(
              prompt.system(),
              prompt.perProblem(),
              prompt.data(),
              new ResponseSchema(
                  SCHEMA_NAME, template.responseSchemaVersion(), SolutionReviewSchema.JSON),
              new CallOptions(template.modelId(), MAX_TOKENS, TOTAL_TIMEOUT, TOTAL_TIMEOUT));
    } catch (AiProviderUnavailableException | AiInvalidResponseException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, failure);
    }
    record(result, template, source, reviewId, callId);
    return SolutionReviewNormalizer.normalize(
        result.json(), template.responseSchemaVersion(), context.knownOptimalComplexity() != null);
  }

  private void record(
      StructuredResult result,
      PromptSnapshot template,
      SubmissionSnapshot source,
      UUID reviewId,
      UUID callId) {
    var tokens = result.usage();
    usage.record(
        new RecordAiTokenUsageCommand(
            callId,
            source.userId(),
            FeatureCode.SOLUTION_REVIEW,
            source.problemId(),
            reviewId,
            tokens == null ? null : tokens.promptTokens(),
            tokens == null ? null : tokens.completionTokens(),
            result.modelName() == null ? template.modelId() : result.modelName()));
  }

  /** Statement data only. A missing optimum adds no line and no comparison (BR-AI-01 step 6). */
  static String problemText(ProblemContext c) {
    List<String> lines = new ArrayList<>();
    lines.add("Title: " + c.title());
    lines.add("Difficulty: " + c.difficulty());
    lines.add("Statement:\n" + c.statementMarkdown());
    if (c.constraintsMarkdown() != null && !c.constraintsMarkdown().isBlank()) {
      lines.add("Constraints:\n" + c.constraintsMarkdown());
    }
    if (!c.topics().isEmpty()) {
      lines.add("Topics: " + String.join(", ", c.topics()));
    }
    if (c.functionSignature() != null) {
      lines.add("Function signature: " + c.functionSignature());
    }
    if (c.knownOptimalComplexity() != null) {
      lines.add("Known optimal complexity: " + c.knownOptimalComplexity());
    }
    return String.join("\n", lines);
  }
}
