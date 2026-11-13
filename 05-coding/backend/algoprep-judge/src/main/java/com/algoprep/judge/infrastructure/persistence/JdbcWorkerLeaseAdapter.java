package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** A dedicated checked connection owns each lease; unlock before returning it to a pool. */
@Component
class JdbcWorkerLeaseAdapter implements WorkerLeasePort {
  private final ObjectProvider<DataSource> data;

  JdbcWorkerLeaseAdapter(ObjectProvider<DataSource> data) {
    this.data = data;
  }

  @Override
  public Optional<Lease> acquire(String key) {
    Connection connection = null;
    try {
      connection = data.getObject().getConnection();
      if (!connection.getAutoCommit()) {
        throw new SQLException("Invalid lease connection");
      }
      var lease = new SessionLease(connection, key);
      if (!lease.query(false)) {
        connection.close();
        return Optional.empty();
      }
      return Optional.of(lease);
    } catch (SQLException | RuntimeException failure) {
      if (connection != null) {
        discard(connection);
      }
      throw new WorkerPersistenceException();
    }
  }

  private static void discard(Connection connection) {
    try {
      connection.abort(Runnable::run);
    } catch (SQLException ignored) {
      /* Physical connection may already be closed. */
    }
    try {
      connection.close();
    } catch (SQLException ignored) {
      /* Never return a live locked connection. */
    }
  }

  private static final class SessionLease implements Lease {
    private final Connection connection;
    private final String key;
    private boolean closed;

    SessionLease(Connection connection, String key) {
      this.connection = connection;
      this.key = key;
    }

    boolean query(boolean release) throws SQLException {
      String sql;
      if (key.equals("F4_GRADING_WORKER_LEADER")) {
        sql =
            release
                ? "SELECT pg_advisory_unlock(hashtext(?))"
                : "SELECT pg_try_advisory_lock(hashtext(?))";
      } else {
        sql =
            release
                ? "SELECT pg_advisory_unlock(hashtextextended(?,0))"
                : "SELECT pg_try_advisory_lock(hashtextextended(?,0))";
      }
      try (var statement = connection.prepareStatement(sql)) {
        statement.setQueryTimeout(2);
        statement.setString(1, key);
        try (var row = statement.executeQuery()) {
          return row.next() && row.getBoolean(1);
        }
      }
    }

    @Override
    public void check() {
      if (closed) {
        throw new WorkerPersistenceException();
      }
      try (var statement = connection.createStatement()) {
        statement.setQueryTimeout(2);
        statement.execute("SELECT 1");
      } catch (SQLException e) {
        throw new WorkerPersistenceException();
      }
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      try {
        if (!query(true)) {
          throw new SQLException("Lease lost");
        }
        connection.close();
      } catch (SQLException e) {
        discard(connection);
        throw new WorkerPersistenceException();
      }
    }
  }
}
