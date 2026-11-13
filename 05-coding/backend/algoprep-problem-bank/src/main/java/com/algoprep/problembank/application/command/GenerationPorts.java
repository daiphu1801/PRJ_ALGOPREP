package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.ports.out.GeneratorSandboxPort;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.TestcaseGenerationLockPort;
import com.algoprep.problembank.application.ports.out.TestcaseScriptGenerationPort;
import org.springframework.stereotype.Component;

/** The three outbound ports of the generation pipeline, grouped to keep its constructor short. */
@Component
record GenerationPorts(
    TestcaseScriptGenerationPort ai,
    GeneratorSandboxPort sandbox,
    TestcaseGenerationLockPort lock,
    JudgeConfigReadPort config) {}
