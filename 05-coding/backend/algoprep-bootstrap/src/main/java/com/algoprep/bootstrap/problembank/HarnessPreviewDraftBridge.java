package com.algoprep.bootstrap.problembank;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.SpecEvaluation.Violation;
import com.algoprep.harness.application.ports.out.PreviewDraftParserPort;
import com.algoprep.harness.application.preview.PreviewSpecInvalidException;
import com.algoprep.problembank.application.command.ProblemSpecJson;
import com.algoprep.problembank.domain.exception.ProblemSpecInvalidException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * F2's raw JSON reader serves the F3 preview, so a draft is read exactly as a save reads it and the
 * preview matches the starter code after saving. F2 errors become HRN-060 here.
 */
@Component
public final class HarnessPreviewDraftBridge implements PreviewDraftParserPort {
  private static final Set<String> FIELDS =
      Set.of(
          "functionName",
          "returnType",
          "parameters",
          "nameOverrides",
          "matchingStrategy",
          "epsilonValue");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Override
  @SuppressWarnings("unchecked")
  public FunctionSpecDraft parse(byte[] utf8Body) {
    try {
      JsonNode root = JSON.readTree(utf8Body);
      if (root == null || !root.isObject()) {
        throw invalid("/");
      }
      for (String name : root.propertyNames()) {
        if (!FIELDS.contains(name)) {
          throw invalid("/" + name);
        }
      }
      FunctionSpecDraft draft = ProblemSpecJson.draft(ProblemSpecJson.data(root, null));
      ProblemSpecJson.checkEpsilon(draft.epsilonValue());
      return draft;
    } catch (ProblemSpecInvalidException hard) {
      throw new PreviewSpecInvalidException(
          (List<Violation>) hard.clientErrorDetails().get("violations"));
    } catch (InvalidRequestException | JacksonException shape) {
      throw invalid("/");
    }
  }

  private static PreviewSpecInvalidException invalid(String path) {
    return new PreviewSpecInvalidException(
        List.of(new Violation("HRN-V10", path, "Invalid function specification")));
  }
}
