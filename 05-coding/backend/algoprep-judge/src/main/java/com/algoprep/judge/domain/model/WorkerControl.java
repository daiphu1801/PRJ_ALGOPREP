package com.algoprep.judge.domain.model;

public record WorkerControl(boolean paused, boolean autoscale, int threshold, long pending) {}
