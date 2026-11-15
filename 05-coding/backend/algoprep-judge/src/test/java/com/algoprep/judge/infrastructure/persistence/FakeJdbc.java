package com.algoprep.judge.infrastructure.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * In-memory JDBC double: records every statement and parameter, and replays queued result sets and
 * update counts. It exercises the repositories' real SQL strings, parameter binding and row mappers
 * without a database (no Testcontainers dependency in this module).
 */
final class FakeJdbc {
  /** One executed statement with 1-based positional parameters. */
  record Call(String sql, Map<Integer, Object> params) {}

  final List<Call> calls = new ArrayList<>();
  private final Deque<List<Map<String, Object>>> results = new ArrayDeque<>();
  private final Deque<Object> updates = new ArrayDeque<>();

  /** Queues the rows returned by the next query. */
  FakeJdbc rows(List<Map<String, Object>> rows) {
    results.add(rows);
    return this;
  }

  FakeJdbc row(Object... kv) {
    return rows(List.of(row0(kv)));
  }

  FakeJdbc none() {
    return rows(List.of());
  }

  /** Queues an update count, or a RuntimeException to throw. */
  FakeJdbc update(Object countOrFailure) {
    updates.add(countOrFailure);
    return this;
  }

  static Map<String, Object> row0(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  JdbcClient client() {
    return JdbcClient.create(dataSource());
  }

  ObjectProvider<JdbcClient> provider() {
    var client = client();
    return new ObjectProvider<>() {
      @Override
      public JdbcClient getObject() {
        return client;
      }
    };
  }

  String lastSql() {
    return calls.get(calls.size() - 1).sql();
  }

  Map<Integer, Object> lastParams() {
    return calls.get(calls.size() - 1).params();
  }

  DataSource dataSourceForTemplate() {
    return dataSource();
  }

  private DataSource dataSource() {
    Connection connection =
        proxy(
            Connection.class,
            (p, m, a) -> {
              if (m.getName().equals("prepareStatement")) {
                return statement((String) a[0]);
              }
              if (m.getName().equals("createStatement")) {
                return statement(null);
              }
              return defaults(m.getReturnType());
            });
    return proxy(
        DataSource.class,
        (p, m, a) ->
            m.getName().equals("getConnection") ? connection : defaults(m.getReturnType()));
  }

  private PreparedStatement statement(String sql) {
    Map<Integer, Object> params = new LinkedHashMap<>();
    return proxy(
        PreparedStatement.class,
        (p, m, a) -> {
          String name = m.getName();
          if (name.equals("setNull")) {
            params.put((Integer) a[0], null);
          } else if (name.startsWith("set")
              && a != null
              && a.length >= 2
              && a[0] instanceof Integer) {
            params.put((Integer) a[0], a[1]);
          } else if (name.equals("executeQuery")) {
            calls.add(new Call(a != null && a.length > 0 ? (String) a[0] : sql, params));
            return resultSet(results.isEmpty() ? List.of() : results.poll());
          } else if (name.equals("executeUpdate")) {
            calls.add(new Call(sql, params));
            Object next = updates.isEmpty() ? (Object) 1 : updates.poll();
            if (next instanceof RuntimeException failure) {
              throw failure;
            }
            return next;
          }
          return defaults(m.getReturnType());
        });
  }

  private static ResultSet resultSet(List<Map<String, Object>> rows) {
    int[] cursor = {-1};
    boolean[] wasNull = {false};
    ResultSetMetaData meta =
        proxy(
            ResultSetMetaData.class,
            (p, m, a) -> {
              Map<String, Object> current = rows.isEmpty() ? Map.of() : rows.get(0);
              return switch (m.getName()) {
                case "getColumnCount" -> current.size();
                case "getColumnLabel", "getColumnName" ->
                    new ArrayList<>(current.keySet()).get((Integer) a[0] - 1);
                default -> defaults(m.getReturnType());
              };
            });
    return proxy(
        ResultSet.class,
        (p, m, a) -> {
          String name = m.getName();
          if (name.equals("next")) {
            cursor[0]++;
            return cursor[0] < rows.size();
          }
          if (name.equals("wasNull")) {
            return wasNull[0];
          }
          if (name.equals("getMetaData")) {
            return meta;
          }
          if (name.startsWith("get") && a != null && a.length >= 1) {
            Map<String, Object> current = rows.get(cursor[0]);
            Object key = a[0];
            Object value =
                key instanceof Integer i
                    ? current.values().toArray()[i - 1]
                    : current.get((String) key);
            wasNull[0] = value == null;
            return convert(name, value, a.length > 1 ? (Class<?>) a[1] : null, m.getReturnType());
          }
          return defaults(m.getReturnType());
        });
  }

  private static Object convert(String method, Object v, Class<?> required, Class<?> returnType)
      throws SQLException {
    if (v == null) {
      return returnType.isPrimitive() ? defaults(returnType) : null;
    }
    if (required != null) {
      return cast(v, required);
    }
    return switch (method) {
      case "getString" -> v.toString();
      case "getInt" -> ((Number) v).intValue();
      case "getLong" -> ((Number) v).longValue();
      case "getShort" -> ((Number) v).shortValue();
      case "getDouble" -> ((Number) v).doubleValue();
      case "getBoolean" -> v;
      case "getBigDecimal" -> new BigDecimal(v.toString());
      case "getTimestamp" -> v instanceof OffsetDateTime o ? Timestamp.from(o.toInstant()) : v;
      default -> v;
    };
  }

  private static Object cast(Object v, Class<?> t) throws SQLException {
    if (t.isInstance(v)) {
      return v;
    }
    if (v instanceof Number n) {
      if (t == Integer.class) {
        return n.intValue();
      }
      if (t == Long.class) {
        return n.longValue();
      }
      if (t == Double.class) {
        return n.doubleValue();
      }
      if (t == BigDecimal.class) {
        return new BigDecimal(n.toString());
      }
    }
    if (t == Instant.class && v instanceof OffsetDateTime o) {
      return o.toInstant();
    }
    throw new SQLException("Cannot convert " + v.getClass() + " to " + t);
  }

  private static Object defaults(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T)
        Proxy.newProxyInstance(FakeJdbc.class.getClassLoader(), new Class<?>[] {type}, handler);
  }
}
