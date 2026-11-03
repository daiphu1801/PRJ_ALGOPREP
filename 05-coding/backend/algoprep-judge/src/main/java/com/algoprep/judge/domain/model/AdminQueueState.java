package com.algoprep.judge.domain.model;

/** One cluster_settings singleton row, including the shared update timestamp. */
public record AdminQueueState(
    boolean paused, boolean autoscaleEnabled, AdminQueueSettings settings) {}
