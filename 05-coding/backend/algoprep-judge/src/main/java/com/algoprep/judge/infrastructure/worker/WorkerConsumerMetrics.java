package com.algoprep.judge.infrastructure.worker;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/** Worker-owned totals; never substitutes engine process counts for broker consumers. */
final class WorkerConsumerMetrics implements MeterBinder {
  private final WorkerConsumptionControl control;

  WorkerConsumerMetrics(WorkerConsumptionControl control) {
    this.control = control;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder(
            "judge.grading.consumers.target", control, WorkerConsumptionControl::targetConsumers)
        .description("Configured grading consumer target, retained while paused")
        .register(registry);
    Gauge.builder(
            "judge.grading.consumers.active", control, WorkerConsumptionControl::activeConsumers)
        .description("Active RabbitMQ grading consumers in the elected worker")
        .register(registry);
  }
}
