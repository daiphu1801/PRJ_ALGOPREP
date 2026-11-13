package com.algoprep.harness.domain.codegen;

import com.algoprep.common.functionspec.TypeNode;

/** Source fragments for a validated type. Implementations only emit trusted source text. */
public interface TypeCodeEmitter {
  String type(TypeNode node);

  String read(TypeNode node);

  String write(TypeNode node, String value);
}
