package com.algoprep.identity.application.dto;

import java.util.Iterator;
import java.util.List;

/**
 * A validated export, ready to stream: the header cells and a lazy iterator of rows. Cells are
 * plain text; quoting for CSV is the caller's job. The iterator reads the log page by page, so the
 * whole result never sits in memory.
 *
 * @param headers header cells in the requested column order
 * @param rows one list of cells per log row, same order as {@code headers}
 */
public record AuditExport(List<String> headers, Iterator<List<String>> rows) {}
