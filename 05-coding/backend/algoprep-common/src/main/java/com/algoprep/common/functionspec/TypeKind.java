package com.algoprep.common.functionspec;

/** Closed wrapper type vocabulary; OTHER is an evaluation marker, never a stored kind. */
public enum TypeKind {
  INT,
  LONG,
  DOUBLE,
  BOOLEAN,
  CHAR,
  STRING,
  ARRAY,
  LIST,
  LINKED_LIST,
  BINARY_TREE,
  OTHER;

  public boolean isScalar() {
    return ordinal() <= STRING.ordinal();
  }

  public boolean isContainer() {
    return this == ARRAY || this == LIST || this == LINKED_LIST || this == BINARY_TREE;
  }
}
