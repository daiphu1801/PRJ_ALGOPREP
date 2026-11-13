package com.algoprep.harness.domain.signature;

import com.algoprep.common.functionspec.FunctionSpecDraft;

/** A declaration-only strategy, without placeholder methods for unimplemented P1/P2 codegen. */
interface DeclarationFormatter {
  String declaration(FunctionSpecDraft draft);
}
