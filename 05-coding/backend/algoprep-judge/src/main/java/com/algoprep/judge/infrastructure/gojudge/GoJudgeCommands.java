package com.algoprep.judge.infrastructure.gojudge;

import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adds testcase data and limits at execution time, never as shell text. */
final class GoJudgeCommands {
  // Zero in our contract disables the 60-process policy. go-judge writes zero literally
  // to pids.max, so use Linux's global PID ceiling; host/parent cgroup limits still apply.
  private static final int LINUX_PID_CEILING = 4_194_304;

  private GoJudgeCommands() {}

  static Map<String, Object> compile(Program program, Limits limits) {
    var command = template(program, "compileCmd");
    applyLimits(command, limits, "", limits.outputBytes());
    if (program.artifact() != null) {
      command.put("copyOutCached", List.of(program.artifact()));
    }
    return command;
  }

  static Map<String, Object> run(
      Program program, Map<String, String> ids, String stdin, Limits limits) {
    var command = template(program, "runCmd");
    long cap = limits.outputBytes();
    if (program.model() == Model.FUNCTION_WRAPPER) {
      // The result budget excludes framing; debug allowance is a separate bounded budget.
      cap = Math.addExact(cap, 16 + 4096);
      var env = new ArrayList<Object>((List<?>) command.get("env"));
      env.removeIf(value -> value.toString().startsWith("APH_MAX_RESULT_BYTES="));
      env.add("APH_MAX_RESULT_BYTES=" + limits.outputBytes());
      command.put("env", env);
    }
    applyLimits(command, limits, stdin, cap);
    var copyIn = new LinkedHashMap<String, Object>((Map<String, Object>) command.get("copyIn"));
    ids.forEach((name, id) -> copyIn.put(name, Map.of("fileId", id)));
    command.put("copyIn", copyIn);
    return command;
  }

  private static Map<String, Object> template(Program program, String key) {
    return new LinkedHashMap<>((Map<String, Object>) program.body().get(key));
  }

  private static void applyLimits(
      Map<String, Object> command, Limits limits, String stdin, long cap) {
    command.put(
        "files",
        List.of(
            Map.of("content", stdin),
            Map.of("name", "stdout", "max", cap),
            Map.of("name", "stderr", "max", 65536)));
    command.put("cpuLimit", Math.multiplyExact(limits.timeMs(), 1000000));
    command.put("clockLimit", Math.multiplyExact(limits.timeMs(), 1000000));
    command.put("memoryLimit", limits.memoryBytes());
    command.put("procLimit", limits.processes() == 0 ? LINUX_PID_CEILING : limits.processes());
  }
}
