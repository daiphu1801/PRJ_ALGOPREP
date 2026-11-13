package com.algoprep.judge.application.command;

import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class WorkerTransactions {
  private final TransactionTemplate transaction;

  WorkerTransactions(@Lazy PlatformTransactionManager manager) {
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  <T> T execute(Supplier<T> operation) {
    try {
      return Objects.requireNonNull(transaction.execute(status -> operation.get()));
    } catch (RuntimeException error) {
      throw new WorkerPersistenceException();
    }
  }
}
