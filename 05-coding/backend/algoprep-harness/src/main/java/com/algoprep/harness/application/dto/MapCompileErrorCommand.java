package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.LineMap;

/** P3 inputs. Raw compiler output is for restricted internal logging only. */
public record MapCompileErrorCommand(
    Language language,
    SubmissionModel model,
    String rawCompilerOutput,
    LineMap lineMap,
    String functionSignatureText,
    String correlationId) {}
