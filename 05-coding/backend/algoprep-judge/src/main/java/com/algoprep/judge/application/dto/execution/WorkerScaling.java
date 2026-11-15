package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.WorkerControl;

/** Stateful counters are private to the one elected worker process. */
public final class WorkerScaling {
  private final int maximum;
  private final int highPolls;
  private final int idlePolls;
  private int target = 1;
  private int high;
  private int empty;
  private int threshold = -1;

  public WorkerScaling() {
    this(8, 2, 4);
  }

  public WorkerScaling(int maximum, int highPolls, int idlePolls) {
    if (maximum < 1 || highPolls < 1 || idlePolls < 1) {
      throw new IllegalArgumentException("Worker scaling values must be positive");
    }
    this.maximum = maximum;
    this.highPolls = highPolls;
    this.idlePolls = idlePolls;
  }

  public int target() {
    return target;
  }

  public WorkerScaling copy() {
    var copy = new WorkerScaling(maximum, highPolls, idlePolls);
    copy.target = target;
    copy.high = high;
    copy.empty = empty;
    copy.threshold = threshold;
    return copy;
  }

  public int poll(WorkerControl control) {
    if (control.paused()) {
      reset();
      return target;
    }
    if (threshold != control.threshold()) {
      threshold = control.threshold();
      reset();
    }
    if (!control.autoscale()) {
      target = 1;
      reset();
      return target;
    }
    high = control.pending() > threshold ? Math.min(high, highPolls - 1) + 1 : 0;
    empty = control.pending() == 0 ? Math.min(empty, idlePolls - 1) + 1 : 0;
    if (high >= highPolls && target < maximum) {
      target++;
      reset();
    } else if (empty >= idlePolls && target > 1) {
      target--;
      reset();
    }
    return target;
  }

  private void reset() {
    high = 0;
    empty = 0;
  }
}
