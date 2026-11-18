package com.algoprep.problembank.infrastructure.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Scriptable in-memory stand-in for {@link JdbcClient}: records every statement with its bound
 * parameters, replays scripted rows through the REAL row mappers of the repository under test, and
 * fails when a named parameter that appears in the SQL was never bound (the real failure mode).
 * Rows are plain maps keyed by column label. The module has no database driver on its test
 * classpath, so SQL text itself is not validated here; the bootstrap module's Testcontainers flows
 * cover that.
 */
final class FakeJdbc {
  private static final Pattern NAMED = Pattern.compile("(?<!:):([A-Za-z_][A-Za-z0-9_]*)");

  /** One executed statement. */
  record Call(String sql, Map<String, Object> params, List<Object> positional) {
    Object param(String name) {
      return params.get(name);
    }
  }

  private record Script(String fragment, Function<Call, List<?>> rows, int updateCount) {}

  private final List<Call> calls = new ArrayList<>();
  private final List<Script> scripts = new ArrayList<>();
  private final JdbcClient client;

  FakeJdbc() {
    client = Mockito.mock(JdbcClient.class);
    Mockito.when(client.sql(Mockito.anyString()))
        .thenAnswer(inv -> statement(normalize(inv.getArgument(0))));
  }

  static Map<String, Object> row(Object... kv) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  /** Rows returned by every query whose normalised SQL contains {@code fragment}. */
  FakeJdbc onQuery(String fragment, Object... rows) {
    List<Object> list = List.of(rows);
    scripts.add(new Script(fragment, c -> list, 0));
    return this;
  }

  FakeJdbc onQuery(String fragment, Function<Call, List<?>> rows) {
    scripts.add(new Script(fragment, rows, 0));
    return this;
  }

  FakeJdbc onUpdate(String fragment, int count) {
    scripts.add(new Script(fragment, c -> List.of(), count));
    return this;
  }

  /** Makes the statement throw when it is executed. */
  FakeJdbc onFail(String fragment, RuntimeException failure) {
    scripts.add(
        new Script(
            fragment,
            c -> {
              throw failure;
            },
            -1));
    return this;
  }

  JdbcClient client() {
    return client;
  }

  @SuppressWarnings("unchecked")
  ObjectProvider<JdbcClient> provider() {
    ObjectProvider<JdbcClient> provider = Mockito.mock(ObjectProvider.class);
    Mockito.when(provider.getObject()).thenReturn(client);
    return provider;
  }

  List<Call> calls() {
    return calls;
  }

  /** Last executed statement whose SQL contains the fragment. */
  Call last(String fragment) {
    for (int i = calls.size() - 1; i >= 0; i--) {
      if (calls.get(i).sql().contains(fragment)) {
        return calls.get(i);
      }
    }
    throw new AssertionError("no statement containing: " + fragment + " in " + sqls());
  }

  long count(String fragment) {
    return calls.stream().filter(c -> c.sql().contains(fragment)).count();
  }

  List<String> sqls() {
    return calls.stream().map(Call::sql).toList();
  }

  private static String normalize(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }

  private Script find(String sql) {
    for (Script s : scripts) {
      if (sql.contains(s.fragment())) {
        return s;
      }
    }
    return null;
  }

  private static void bindParam(Call call, Object[] args) {
    if (args[0] instanceof String key) {
      call.params().put(key, args[1]);
    } else if (args.length == 1) {
      call.positional().add(args[0]);
    } else {
      call.positional().add(args[1]);
    }
  }

  private static void bindParams(Call call, Object[] args) {
    if (args[0] instanceof Map<?, ?> map) {
      map.forEach((k, v) -> call.params().put((String) k, v));
    } else if (args[0] instanceof List<?> list) {
      call.positional().addAll(list);
    } else if (args[0] instanceof Object[] arr) {
      call.positional().addAll(List.of(arr));
    }
  }

  private Object statement(String sql) {
    var params = new LinkedHashMap<String, Object>();
    var positional = new ArrayList<Object>();
    var call = new Call(sql, params, positional);
    InvocationHandler handler =
        (proxy, method, args) -> {
          String name = method.getName();
          switch (name) {
            case "param" -> {
              bindParam(call, args);
              return proxy;
            }
            case "params" -> {
              bindParams(call, args);
              return proxy;
            }
            case "withFetchSize", "withMaxRows", "withQueryTimeout" -> {
              return proxy;
            }
            case "update" -> {
              execute(call);
              Script s = find(sql);
              if (s != null && s.updateCount() < 0) {
                s.rows().apply(call);
              }
              return s == null ? 0 : Math.max(s.updateCount(), 0);
            }
            case "query" -> {
              return query(call, args);
            }
            case "toString" -> {
              return "FakeStatement[" + sql + "]";
            }
            case "hashCode" -> {
              return System.identityHashCode(proxy);
            }
            case "equals" -> {
              return proxy == args[0];
            }
            default -> throw new UnsupportedOperationException(name);
          }
        };
    return Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {JdbcClient.StatementSpec.class}, handler);
  }

  private void execute(Call call) {
    calls.add(call);
    Matcher m = NAMED.matcher(call.sql().replaceAll("'[^']*'", "''"));
    var missing = new LinkedHashSet<String>();
    while (m.find()) {
      if (!call.params().containsKey(m.group(1))) {
        missing.add(m.group(1));
      }
    }
    if (!missing.isEmpty() && call.positional().isEmpty()) {
      throw new AssertionError("unbound named parameters " + missing + " in: " + call.sql());
    }
  }

  private List<?> rowsFor(Call call) {
    execute(call);
    Script s = find(call.sql());
    return s == null ? List.of() : s.rows().apply(call);
  }

  private Object query(Call call, Object[] args) {
    if (args == null || args.length == 0) {
      return resultQuery(call);
    }
    Object arg = args[0];
    if (arg instanceof RowCallbackHandler handler) {
      var rows = rowsFor(call);
      try {
        ResultSet rs = resultSet(new Cursor(rows));
        while (rs.next()) {
          handler.processRow(rs);
        }
      } catch (SQLException e) {
        throw new IllegalStateException(e);
      }
      return null;
    }
    if (arg instanceof ResultSetExtractor<?> extractor) {
      try {
        return extractor.extractData(resultSet(new Cursor(rowsFor(call))));
      } catch (SQLException e) {
        throw new IllegalStateException(e);
      }
    }
    return mapped(call, arg);
  }

  private Object mapped(Call call, Object arg) {
    InvocationHandler handler =
        (proxy, method, a) -> {
          List<Object> items = new ArrayList<>();
          int i = 0;
          for (Object raw : rowsFor(call)) {
            if (arg instanceof RowMapper<?> mapper) {
              var cursor = new Cursor(List.of(raw));
              cursor.next();
              items.add(mapper.mapRow(resultSet(cursor), i++));
            } else if (raw instanceof Map<?, ?> m) {
              items.add(m.values().iterator().next());
            } else {
              items.add(raw);
            }
          }
          return switch (method.getName()) {
            case "list" -> items;
            case "set" -> new LinkedHashSet<>(items);
            case "stream" -> items.stream();
            case "single" -> {
              if (items.size() != 1) {
                throw new EmptyResultDataAccessException(1);
              }
              yield items.get(0);
            }
            case "optional" -> items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
            default -> throw new UnsupportedOperationException(method.getName());
          };
        };
    return Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {JdbcClient.MappedQuerySpec.class}, handler);
  }

  @SuppressWarnings("unchecked")
  private Object resultQuery(Call call) {
    InvocationHandler handler =
        (proxy, method, a) -> {
          var rows = (List<Map<String, Object>>) rowsFor(call);
          return switch (method.getName()) {
            case "listOfRows" -> rows;
            case "singleRow" -> rows.get(0);
            case "singleColumn" -> rows.stream().map(r -> r.values().iterator().next()).toList();
            case "singleValue" -> rows.get(0).values().iterator().next();
            case "optionalValue" ->
                rows.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(rows.get(0).values().iterator().next());
            default -> throw new UnsupportedOperationException(method.getName());
          };
        };
    return Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {JdbcClient.ResultQuerySpec.class}, handler);
  }

  private static final class Cursor {
    private final List<?> rows;
    private int index = -1;
    private boolean wasNull;

    Cursor(List<?> rows) {
      this.rows = rows;
    }

    boolean next() {
      index++;
      return index < rows.size();
    }

    Map<?, ?> current() {
      Object raw = rows.get(index);
      if (raw instanceof Map<?, ?> m) {
        return m;
      }
      return Map.of("value", raw);
    }
  }

  private static ResultSet resultSet(Cursor cursor) {
    InvocationHandler handler =
        (proxy, method, args) -> {
          String name = method.getName();
          switch (name) {
            case "next" -> {
              return cursor.next();
            }
            case "wasNull" -> {
              return cursor.wasNull;
            }
            case "close", "getMetaData" -> {
              return null;
            }
            default -> {}
          }
          Object key = args[0];
          Object value;
          if (key instanceof Integer position) {
            value = new ArrayList<>(cursor.current().values()).get(position - 1);
          } else {
            if (!cursor.current().containsKey(key)) {
              throw new SQLException("column not scripted: " + key);
            }
            value = cursor.current().get(key);
          }
          cursor.wasNull = value == null;
          return convert(name, args, method.getReturnType(), value);
        };
    return (ResultSet)
        Proxy.newProxyInstance(
            FakeJdbc.class.getClassLoader(), new Class<?>[] {ResultSet.class}, handler);
  }

  private static Object convert(String name, Object[] args, Class<?> type, Object value) {
    if (value == null) {
      if (type == boolean.class) {
        return false;
      }
      if (type == int.class) {
        return 0;
      }
      if (type == long.class) {
        return 0L;
      }
      if (type == double.class) {
        return 0d;
      }
      return null;
    }
    if (name.equals("getArray")) {
      Object[] raw = value instanceof List<?> l ? l.toArray() : (Object[]) value;
      boolean strings = java.util.Arrays.stream(raw).allMatch(String.class::isInstance);
      Object[] items = strings ? java.util.Arrays.copyOf(raw, raw.length, String[].class) : raw;
      return Proxy.newProxyInstance(
          FakeJdbc.class.getClassLoader(),
          new Class<?>[] {Array.class},
          (p, m, a) -> m.getName().equals("getArray") ? items : null);
    }
    if (name.equals("getObject") && args.length == 2 && args[1] instanceof Class<?> wanted) {
      return adapt(value, wanted);
    }
    return switch (name) {
      case "getString" -> value.toString();
      case "getInt" -> ((Number) value).intValue();
      case "getLong" -> ((Number) value).longValue();
      case "getDouble" -> ((Number) value).doubleValue();
      case "getBoolean" -> (Boolean) value;
      case "getBigDecimal" -> value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
      case "getTimestamp" -> value instanceof Timestamp t ? t : Timestamp.from(toInstant(value));
      default -> value;
    };
  }

  private static Instant toInstant(Object value) {
    if (value instanceof Instant i) {
      return i;
    }
    if (value instanceof OffsetDateTime o) {
      return o.toInstant();
    }
    return ((Timestamp) value).toInstant();
  }

  private static Object adapt(Object value, Class<?> wanted) {
    if (wanted.isInstance(value)) {
      return value;
    }
    if (wanted == OffsetDateTime.class) {
      return toInstant(value).atOffset(ZoneOffset.UTC);
    }
    if (wanted == Instant.class) {
      return toInstant(value);
    }
    if (wanted == String.class) {
      return value.toString();
    }
    if (wanted == UUID.class) {
      return UUID.fromString(value.toString());
    }
    if (wanted == Integer.class && value instanceof Number n) {
      return n.intValue();
    }
    if (wanted == Long.class && value instanceof Number n) {
      return n.longValue();
    }
    throw new IllegalArgumentException("cannot adapt " + value.getClass() + " to " + wanted);
  }
}
