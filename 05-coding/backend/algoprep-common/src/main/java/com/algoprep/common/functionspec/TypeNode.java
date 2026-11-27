package com.algoprep.common.functionspec;

/** Typed input to P5. JSON shape validation belongs at the adapter boundary. */
public record TypeNode(TypeKind kind, TypeNode of, Integer dimensions) {}
