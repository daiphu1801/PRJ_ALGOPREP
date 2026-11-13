package com.algoprep.harness.domain.matching;

import com.algoprep.common.functionspec.TypeKind;
import java.util.List;

/** Structural values used only while parsing and matching a single output. */
record OutputValue(String scalar, TypeKind kind, List<OutputValue> children) {
  OutputValue {
    children = List.copyOf(children);
  }

  static OutputValue leaf(String value) {
    return new OutputValue(value, TypeKind.STRING, List.of());
  }

  static OutputValue sequence(List<OutputValue> values) {
    return new OutputValue(null, null, values);
  }
}
