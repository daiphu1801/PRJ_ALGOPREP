package com.algoprep.harness.infrastructure.gojudge;

import com.algoprep.harness.application.dto.ExecutionPlan;
import com.algoprep.harness.application.dto.JudgePayload;
import com.algoprep.harness.application.ports.out.JudgePayloadAssembler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Source remains JSON data; command arguments are fixed by the toolchain profile. */
@Component
public final class GoJudgePayloadAssembler implements JudgePayloadAssembler {
  @Override
  public JudgePayload assemble(ExecutionPlan plan) {
    var sources =
        plan.files().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    ExecutionPlan.SourceFile::name, file -> Map.of("content", file.content())));
    var compile = plan.compile() == null ? null : template(plan.compile(), sources);
    var run = template(plan.run(), plan.artifact() == null ? sources : Map.of());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("compileCmd", compile);
    body.put("runCmd", run);
    return new JudgePayload("go-judge", body, plan.artifact());
  }

  private Map<String, Object> template(ExecutionPlan.Command command, Map<?, ?> sources) {
    return Map.of("args", command.argv(), "env", command.env(), "copyIn", sources);
  }
}
