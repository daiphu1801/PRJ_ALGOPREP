package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WrapperPythonGuardFixturesTest {
  @ParameterizedTest
  @CsvSource({
    "missing,92",
    "arity,92",
    "constructor,1",
    "descriptor,1",
    "default,0",
    "varargs,0",
    "bool-result,1"
  })
  void exportsGuardAndRuntimeClassificationFixtures(String scenario, int exit) throws IOException {
    Map<String, String> users =
        Map.of(
            "missing", "class Solution:\n def other(self, x): return x\n",
            "arity", "class Solution:\n def solve(self): return 7\n",
            "constructor",
                "class Solution:\n def __init__(self): raise TypeError('constructor')\n def solve(self, x): return x\n",
            "descriptor",
                "class Solution:\n @property\n def solve(self): raise AttributeError('descriptor')\n",
            "default", "class Solution:\n def solve(self, x, y=8): return x\n",
            "varargs", "class Solution:\n def solve(self, *args): return args[0]\n",
            "bool-result", "class Solution:\n def solve(self, x): return 1\n");
    TypeNode type =
        new TypeNode(scenario.equals("bool-result") ? TypeKind.BOOLEAN : TypeKind.INT, null, null);
    var draft =
        new FunctionSpecDraft(
            "solve",
            type,
            List.of(new FunctionParameter("x", type)),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    var result = new FunctionWrapperRenderer().render(Language.PYTHON, draft, users.get(scenario));
    assertThat(result.text())
        .contains("apH_resolve_call(\"solve\", [apH_p0])", "apH_r = apH_fn(apH_p0)");
    Path directory = Path.of("target", "f3-wrapper-probe", "python-guard-" + scenario, "PYTHON");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("main.py"), result.text(), StandardCharsets.UTF_8);
    Files.writeString(
        directory.resolve("input.txt"),
        scenario.equals("bool-result") ? "true\n" : "7\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        directory.resolve("expected.txt"), exit == 0 ? "7\n" : "", StandardCharsets.UTF_8);
    Files.writeString(
        directory.resolve("exit.txt"), Integer.toString(exit), StandardCharsets.UTF_8);
  }
}
