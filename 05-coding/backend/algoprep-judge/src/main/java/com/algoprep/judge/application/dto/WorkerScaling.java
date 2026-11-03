package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.WorkerControl;

/** Stateful counters are private to the one elected worker process. */
public final class WorkerScaling {
  private int target = 1;
  private int high;
  private int empty;
  private int threshold = -1;

  public int target() {
    return target;
  }

  public WorkerScaling copy() {
    var copy = new WorkerScaling();
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
    high = control.pending() > threshold ? high + 1 : 0;
    empty = control.pending() == 0 ? empty + 1 : 0;
    if (high >= 2 && target < 8) {
      target++;
      reset();
    } else if (empty >= 4 && target > 1) {
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
