package com.algoprep.judge.infrastructure.worker;

import com.algoprep.judge.application.dto.WorkerScaling;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;

/** Only the dedicated worker instance creates this controller and its listener. */
final class WorkerConsumptionControl implements SmartLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(WorkerConsumptionControl.class);
  private final WorkerJobRepository jobs;
  private final WorkerLeasePort leases;
  private final SimpleMessageListenerContainer container;
  private WorkerScaling scaling;
  private Lease leader;
  private volatile boolean running;
  private volatile boolean healthy;
  private volatile int target = 1;
  private boolean leadershipLost;

  WorkerConsumptionControl(
      WorkerJobRepository jobs, WorkerLeasePort leases, SimpleMessageListenerContainer container) {
    this(jobs, leases, container, new WorkerScaling());
  }

  WorkerConsumptionControl(
      WorkerJobRepository jobs,
      WorkerLeasePort leases,
      SimpleMessageListenerContainer container,
      WorkerScaling scaling) {
    this.jobs = jobs;
    this.leases = leases;
    this.container = container;
    this.scaling = scaling;
  }

  @Override
  public synchronized void start() {
    if (leadershipLost) {
      throw new IllegalStateException("A worker that lost leadership must be restarted");
    }
    if (running) {
      return;
    }
    leader =
        leases
            .acquire("F4_GRADING_WORKER_LEADER")
            .orElseThrow(() -> new IllegalStateException("Another grading worker owns leadership"));
    running = true;
    gate();
  }

  @Scheduled(fixedDelayString = "${judge.worker.gate-delay-ms:3000}")
  synchronized void gate() {
    if (!running || !ownsLeader()) {
      return;
    }
    try {
      var control = jobs.control();
      if (control.paused()) {
        drain();
      } else if (!container.isRunning()) {
        container.setConcurrency(Integer.toString(scaling.target()));
        container.start();
      }
      healthy = true;
    } catch (RuntimeException e) {
      healthy = false;
      LOG.warn("Grading consumption settings are unavailable");
    }
  }

  @Scheduled(fixedDelayString = "${judge.worker.scale-delay-ms:15000}")
  synchronized void scale() {
    if (!running || !ownsLeader()) {
      return;
    }
    try {
      var control = jobs.control();
      var proposal = scaling.copy();
      int before = scaling.target();
      int after = proposal.poll(control);
      // Publish the desired target before the container can expose its new active count.
      target = after;
      if (before != after) {
        resize(before, after, control);
      }
      scaling = proposal;
    } catch (RuntimeException e) {
      // A rejected resize must retain the committed counters and target for the next poll.
      target = scaling.target();
      healthy = false;
      LOG.warn("Grading consumer control is unavailable");
    }
  }

  private void resize(
      int before, int after, com.algoprep.judge.domain.model.WorkerControl control) {
    try {
      container.setConcurrency(Integer.toString(after));
    } catch (RuntimeException e) {
      LOG.error("Grading consumer resize failed; target remains {}", before);
      throw e;
    }
    LOG.info(
        "Grading consumer target changed oldTarget={} newTarget={} pendingCount={} threshold={} reason={}",
        before,
        after,
        control.pending(),
        control.threshold(),
        !control.autoscale() ? "DISABLED" : after > before ? "BACKLOG" : "IDLE");
  }

  private boolean ownsLeader() {
    try {
      leader.check();
      return true;
    } catch (RuntimeException e) {
      lostLeadership();
      return false;
    }
  }

  private void lostLeadership() {
    leadershipLost = true;
    stop();
    LOG.error("Grading leadership is unavailable; listener stopped");
  }

  @Override
  public synchronized void stop() {
    healthy = false;
    drain();
    // A lost leader must never restart this listener; the process must be restarted to compete
    // again.
    running = false;
    if (leader != null) {
      try {
        leader.close();
      } catch (RuntimeException ignored) {
        LOG.warn("Grading leadership cleanup failed");
      }
      leader = null;
    }
  }

  private void drain() {
    container.stop();
    boolean interrupted = false;
    while (container.getActiveConsumerCount() > 0) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException failure) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  boolean ready() {
    return running && healthy;
  }

  int targetConsumers() {
    return target;
  }

  int activeConsumers() {
    return container.getActiveConsumerCount();
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }
}
