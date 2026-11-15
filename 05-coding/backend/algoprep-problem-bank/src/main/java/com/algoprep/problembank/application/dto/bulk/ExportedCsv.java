package com.algoprep.problembank.application.dto;

/** A finished export: the download name and the UTF-8 bytes, BOM included. */
public record ExportedCsv(String filename, byte[] content) {
  @Override
  public boolean equals(Object other) {
    return other instanceof ExportedCsv that
        && java.util.Objects.equals(filename, that.filename)
        && java.util.Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return 31 * java.util.Objects.hashCode(filename) + java.util.Arrays.hashCode(content);
  }

  @Override
  public String toString() {
    return "ExportedCsv[filename=" + filename + ", bytes=" + content.length + "]";
  }
}
