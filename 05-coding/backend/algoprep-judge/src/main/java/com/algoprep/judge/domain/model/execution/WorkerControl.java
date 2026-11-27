package com.algoprep.judge.domain.model.execution;

public record WorkerControl(boolean paused, boolean autoscale, int threshold, long pending) {}
