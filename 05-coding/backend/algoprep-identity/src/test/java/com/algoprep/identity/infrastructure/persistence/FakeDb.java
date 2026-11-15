package com.algoprep.identity.infrastructure.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Scripted in-memory JDBC double used to drive the {@code Jdbc*Repository} classes through the real
 * {@link JdbcClient} without a database. Every statement is recorded (SQL text plus bound
 * parameters) so a test can assert what the repository sent, and result rows are scripted per SQL
 * fragment. Columns a test does not script are synthesised from the getter type the row mapper asks
 * for, so mappers run end to end with plausible values.
 */
public final class FakeDb {

  /** One executed statement. */
  public record Call(String sql, Map<Integer, Object> params) {
    public List<Object> values() {
      return new ArrayList<>(params.values());
    }
  }

  private record Rule(
      String fragment, List<Map<String, Object>> rows, Integer updateCount, SQLException failure) {}

  private final List<Call> calls = new ArrayList<>();
  private final List<Rule> rules = new ArrayList<>();
  private final JdbcClient client;

  public FakeDb() {
    this.client = JdbcClient.create(dataSource());
  }

  /** Rows returned by queries whose SQL contains the fragment (later rules win). */
  @SafeVarargs
  public final FakeDb rows(String fragment, Map<String, Object>... rows) {
    rules.add(0, new Rule(fragment, List.of(rows), null, null));
    return this;
  }

  /** Returns {@code n} rows whose columns are all synthesised from the getter type. */
  public final FakeDb generic(String fragment, int n) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      rows.add(new LinkedHashMap<>());
    }
    rules.add(0, new Rule(fragment, rows, null, null));
    return this;
  }

  /** Update count returned for statements whose SQL contains the fragment. */
  public final FakeDb updateCount(String fragment, int count) {
    rules.add(0, new Rule(fragment, null, count, null));
    return this;
  }

  /** Makes statements whose SQL contains the fragment fail with the given SQL state. */
  public final FakeDb fail(String fragment, String sqlState) {
    rules.add(0, new Rule(fragment, null, null, new SQLException("scripted failure", sqlState)));
    return this;
  }

  public JdbcClient client() {
    return client;
  }

  @SuppressWarnings("unchecked")
  public ObjectProvider<JdbcClient> provider() {
    ObjectProvider<JdbcClient> provider = org.mockito.Mockito.mock(ObjectProvider.class);
    org.mockito.Mockito.when(provider.getObject()).thenReturn(client);
    org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(client);
    return provider;
  }

  public List<Call> calls() {
    return calls;
  }

  public Call last() {
    return calls.get(calls.size() - 1);
  }

  /** First recorded call whose SQL contains the fragment. */
  public Call call(String fragment) {
    return calls.stream()
        .filter(c -> c.sql().contains(fragment))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no statement containing: " + fragment));
  }

  // ---------------------------------------------------------------- proxies

  private DataSource dataSource() {
    return (DataSource)
        proxy(
            DataSource.class,
            (p, m, a) -> {
              if (m.getName().equals("getConnection")) {
                return connection();
              }
              return dflt(m);
            });
  }

  private Connection connection() {
    return (Connection)
        proxy(
            Connection.class,
            (p, m, a) -> {
              switch (m.getName()) {
                case "prepareStatement":
                  return statement((String) a[0], (Connection) p);
                case "getMetaData":
                  return proxy(
                      DatabaseMetaData.class,
                      (p2, m2, a2) -> m2.getReturnType() == String.class ? "PostgreSQL" : dflt(m2));
                case "isClosed":
                  return false;
                default:
                  return dflt(m);
              }
            });
  }

  private PreparedStatement statement(String sql, Connection conn) {
    Map<Integer, Object> params = new TreeMap<>();
    Rule rule = rules.stream().filter(r -> sql.contains(r.fragment())).findFirst().orElse(null);
    return (PreparedStatement)
        proxy(
            PreparedStatement.class,
            (p, m, a) -> {
              String n = m.getName();
              if (n.startsWith("set") && a != null && a.length >= 2 && a[0] instanceof Integer i) {
                params.put(i, n.equals("setNull") ? null : a[1]);
                return null;
              }
              switch (n) {
                case "getConnection":
                  return conn;
                case "getParameterMetaData":
                  throw new SQLException("not supported");
                case "executeQuery":
                case "execute":
                  record(sql, params);
                  failIfScripted(rule);
                  if (n.equals("execute")) {
                    return true;
                  }
                  return resultSet(rule == null || rule.rows() == null ? List.of() : rule.rows());
                case "executeUpdate":
                case "executeLargeUpdate":
                  record(sql, params);
                  failIfScripted(rule);
                  int count = rule != null && rule.updateCount() != null ? rule.updateCount() : 1;
                  return n.equals("executeLargeUpdate") ? (Object) (long) count : (Object) count;
                case "getResultSet":
                  return resultSet(List.of());
                case "getUpdateCount":
                  return -1;
                case "getMoreResults":
                  return false;
                default:
                  return dflt(m);
              }
            });
  }

  private void record(String sql, Map<Integer, Object> params) {
    calls.add(new Call(sql, new TreeMap<>(params)));
  }

  private static void failIfScripted(Rule rule) throws SQLException {
    if (rule != null && rule.failure() != null) {
      throw rule.failure();
    }
  }

  private static ResultSet resultSet(List<Map<String, Object>> rows) {
    int[] cursor = {-1};
    boolean[] wasNull = {false};
    return (ResultSet)
        proxy(
            ResultSet.class,
            (p, m, a) -> {
              String n = m.getName();
              switch (n) {
                case "next":
                  cursor[0]++;
                  return cursor[0] < rows.size();
                case "wasNull":
                  return wasNull[0];
                case "getMetaData":
                  return metaData(rows.isEmpty() ? Map.of() : rows.get(0));
                case "close":
                  return null;
                case "isClosed":
                  return false;
                default:
                  break;
              }
              if (n.startsWith("get") && a != null && a.length >= 1) {
                Map<String, Object> row = rows.get(cursor[0]);
                Object key = a[0];
                String col = key instanceof String s ? s : columnAt(row, (Integer) key);
                if (n.equals("getArray")) {
                  return arrayOf(row, col);
                }
                Class<?> type = typeOf(m, a);
                Object value;
                if (col != null && row.containsKey(col)) {
                  value = convert(row.get(col), type);
                } else {
                  value = synth(type);
                }
                wasNull[0] = value == null;
                if (value == null && m.getReturnType().isPrimitive()) {
                  return dflt(m);
                }
                return value;
              }
              return dflt(m);
            });
  }

  private static Object arrayOf(Map<String, Object> row, String col) {
    Object arr = col != null && row.containsKey(col) ? row.get(col) : new Object[] {1};
    Object[] items = arr instanceof java.util.Collection<?> c ? c.toArray() : (Object[]) arr;
    return proxy(
        java.sql.Array.class, (p3, m3, a3) -> m3.getName().equals("getArray") ? items : dflt(m3));
  }

  private static String columnAt(Map<String, Object> row, int index) {
    int i = 1;
    for (String k : row.keySet()) {
      if (i++ == index) {
        return k;
      }
    }
    return null;
  }

  private static ResultSetMetaData metaData(Map<String, Object> row) {
    List<String> cols = new ArrayList<>(row.keySet());
    return (ResultSetMetaData)
        proxy(
            ResultSetMetaData.class,
            (p, m, a) -> {
              switch (m.getName()) {
                case "getColumnCount":
                  return Math.max(1, cols.size());
                case "getColumnLabel":
                case "getColumnName":
                  int i = (Integer) a[0];
                  return i <= cols.size() ? cols.get(i - 1) : "c" + i;
                case "getColumnType":
                  return Types.OTHER;
                default:
                  return dflt(m);
              }
            });
  }

  private static Class<?> typeOf(Method m, Object[] a) {
    if (a.length >= 2 && a[1] instanceof Class<?> c) {
      return c;
    }
    return switch (m.getName()) {
      case "getString", "getNString" -> String.class;
      case "getInt" -> Integer.class;
      case "getLong" -> Long.class;
      case "getShort" -> Short.class;
      case "getBoolean" -> Boolean.class;
      case "getDouble" -> Double.class;
      case "getFloat" -> Float.class;
      case "getBigDecimal" -> BigDecimal.class;
      case "getTimestamp" -> Timestamp.class;
      case "getDate" -> java.sql.Date.class;
      case "getBytes" -> byte[].class;
      default -> Object.class;
    };
  }

  static Object convert(Object v, Class<?> type) {
    if (v == null || type == Object.class || type.isInstance(v)) {
      return v;
    }
    if (type == UUID.class) {
      return UUID.fromString(v.toString());
    }
    if (type == String.class) {
      return v.toString();
    }
    if (type == Timestamp.class) {
      return v instanceof Instant i
          ? Timestamp.from(i)
          : Timestamp.from(((OffsetDateTime) v).toInstant());
    }
    if (type == Instant.class) {
      return v instanceof Timestamp t ? t.toInstant() : ((OffsetDateTime) v).toInstant();
    }
    if (type == OffsetDateTime.class) {
      Instant i = v instanceof Timestamp t ? t.toInstant() : (Instant) v;
      return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }
    if (v instanceof Number num) {
      if (type == Integer.class) {
        return num.intValue();
      }
      if (type == Long.class) {
        return num.longValue();
      }
      if (type == Short.class) {
        return num.shortValue();
      }
      if (type == Double.class) {
        return num.doubleValue();
      }
      if (type == Float.class) {
        return num.floatValue();
      }
      if (type == BigDecimal.class) {
        return new BigDecimal(num.toString());
      }
    }
    return v;
  }

  private static Object synth(Class<?> type) {
    if (type == String.class) {
      return "x";
    }
    if (type == Integer.class) {
      return 1;
    }
    if (type == Long.class) {
      return 1L;
    }
    if (type == Short.class) {
      return (short) 1;
    }
    if (type == Boolean.class) {
      return false;
    }
    if (type == Double.class) {
      return 1.0d;
    }
    if (type == Float.class) {
      return 1.0f;
    }
    if (type == BigDecimal.class) {
      return BigDecimal.ONE;
    }
    if (type == UUID.class) {
      return UUID.randomUUID();
    }
    if (type == Timestamp.class) {
      return Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
    }
    if (type == Instant.class) {
      return Instant.parse("2026-01-01T00:00:00Z");
    }
    if (type == OffsetDateTime.class) {
      return OffsetDateTime.parse("2026-01-01T00:00:00Z");
    }
    if (type == LocalDate.class) {
      return LocalDate.parse("2026-01-01");
    }
    if (type == java.sql.Date.class) {
      return java.sql.Date.valueOf("2026-01-01");
    }
    if (type == byte[].class) {
      return new byte[0];
    }
    return "x";
  }

  private static Object proxy(Class<?> type, InvocationHandler h) {
    return Proxy.newProxyInstance(FakeDb.class.getClassLoader(), new Class<?>[] {type}, h);
  }

  private static Object dflt(Method m) {
    Class<?> r = m.getReturnType();
    if (r == boolean.class) {
      return false;
    }
    if (r == int.class) {
      return 0;
    }
    if (r == long.class) {
      return 0L;
    }
    if (r == double.class) {
      return 0d;
    }
    if (r == float.class) {
      return 0f;
    }
    if (r == short.class) {
      return (short) 0;
    }
    if (r == byte.class) {
      return (byte) 0;
    }
    return null;
  }
}
