package com.algoprep.judge.domain.ports.out;

import java.util.Optional;

public interface WorkerLeasePort {
  Optional<Lease> acquire(String key);

  interface Lease extends AutoCloseable {
    void check();

    @Override
    void close();
  }
}
