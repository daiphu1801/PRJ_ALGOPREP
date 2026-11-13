package com.algoprep.judge.domain.model;

import java.util.Set;
import java.util.UUID;

/** Null problems means no keyword filter; empty means no matches. */
public record HistoryWindow(Set<UUID> problems, int offset, int limit) {}
