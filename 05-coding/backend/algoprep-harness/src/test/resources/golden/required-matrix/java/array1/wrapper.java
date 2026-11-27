import java.util.*;
import java.io.*;
class Solution {
  public int[] echo(int[] value) { return value; }
}
record ApHType(String kind, int dimensions, ApHType of) {}
class ApHInput {
    final String[] lines;
    int index;
    ApHInput() throws Exception {
        String text = new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
        String[] split = text.split("\n", -1);
        lines = java.util.Arrays.copyOf(split, split.length - (text.endsWith("\n") || text.isEmpty() ? 1 : 0));
    }
    String line() {
        if (index == lines.length) ApHCodec.failInput();
        return lines[index++];
    }
    void finish() {
        while (index < lines.length) if (!lines[index++].isBlank()) ApHCodec.failInput();
    }
}
class ApHOutput {
    final StringBuilder text = new StringBuilder();
    final long limit;
    long bytes;
    ApHOutput() {
        long value = 0;
        try {
            String raw = System.getenv("APH_MAX_RESULT_BYTES");
            if (raw == null || !raw.matches("[0-9]+")) fail();
            value = Long.parseLong(raw);
        }
        catch (RuntimeException ignored) { fail(); }
        if (value <= 0) fail();
        limit = value;
    }
    static void fail() {
        System.err.println("@@APH-HARNESS-OUTPUT-LIMIT@@");
        System.exit(91);
    }
    void add(String value) {
        long size = 0;
        for (int i = 0; i < value.length();) {
            int c = value.codePointAt(i);
            size += c <= 127 ? 1 : c <= 2047 ? 2 : c <= 65535 ? (Character.isSurrogate((char)c) ? 1 : 3) : 4;
            if (size > limit - bytes) fail();
            i += Character.charCount(c);
        }
        bytes += size;
        text.append(value);
    }
    void publish() {
        add("\n");
        byte[] output = ("\n@@APH-RESULT@@\n" + text).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.out.write(output, 0, output.length);
    }
}
class ApHCodec {
    static void failInput() {
        System.err.println("@@APH-HARNESS-INPUT-ERROR@@");
        System.exit(90);
        throw new IllegalArgumentException();
    }
    static String[] tokens(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("[ \\t\\r\\f\\u000B]+");
    }
    static int size(String value) {
        Object parsed = scalar("INT", value);
        int n = (int)parsed;
        if (n < 0) failInput();
        return n;
    }
    static Object scalar(String kind, String raw) {
        if (kind.equals("STRING")) return raw;
        String value = raw.trim();
        if (kind.equals("INT") || kind.equals("LONG")) {
            if (!value.matches("[+-]?[0-9]+")) throw new IllegalArgumentException();
            if (kind.equals("INT")) return Integer.parseInt(value);
            return Long.parseLong(value);
        }
        if (kind.equals("DOUBLE")) {
            if (!value.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) throw new IllegalArgumentException();
            double d = Double.parseDouble(value);
            if (!Double.isFinite(d)) throw new IllegalArgumentException();
            return d;
        }
        if (kind.equals("BOOLEAN")) {
            if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException();
            return value.equals("true");
        }
        if (raw.length() != 1 || raw.charAt(0) < 33 || raw.charAt(0) > 126) throw new IllegalArgumentException();
        return raw.charAt(0);
    }
    static Class<?> leaf(String kind) {
        return switch (kind) {
            case "INT" -> int.class; case "LONG" -> long.class; case "DOUBLE" -> double.class;
            case "BOOLEAN" -> boolean.class; case "CHAR" -> char.class; default -> String.class;
        };
    }
    static Object read(ApHInput in, ApHType type) {
        try {
            return switch (type.kind()) {
                case "ARRAY" -> readArray(in, type);
                case "LIST" -> readList(in, type.of());
                case "LINKED_LIST" -> readLink(in, type.of());
                case "BINARY_TREE" -> readTree(in, type.of());
                default -> scalar(type.kind(), in.line());
            };
        } catch (Exception ignored) { failInput(); return null; }
    }
    static Object readArray(ApHInput in, ApHType type) {
        String[] header = tokens(in.line());
        if (header.length != type.dimensions()) failInput();
        int[] shape = new int[header.length];
        for (int i = 0; i < shape.length; i++) shape[i] = size(header[i]);
        Object array = java.lang.reflect.Array.newInstance(leaf(type.of().kind()), shape);
        fillArray(in, array, shape, 0, type.of());
        return array;
    }
    static void fillArray(ApHInput in, Object array, int[] shape, int level, ApHType item) {
        int n = shape[level];
        if (level + 1 < shape.length) {
            for (int i = 0; i < n; i++) fillArray(in, java.lang.reflect.Array.get(array, i), shape, level + 1, item);
        } else {
            String[] row = item.kind().equals("STRING") ? null : tokens(in.line());
            if (row != null && row.length != n) failInput();
            for (int i = 0; i < n; i++) java.lang.reflect.Array.set(array, i, scalar(item.kind(), row == null ? in.line() : row[i]));
        }
    }
    static java.util.List<Object> readList(ApHInput in, ApHType item) {
        int n = size(in.line());
        java.util.List<Object> result = new java.util.ArrayList<>();
        if (item.kind().equals("LIST") || item.kind().equals("STRING")) {
            for (int i = 0; i < n; i++) result.add(read(in, item));
        } else {
            String[] row = tokens(in.line());
            if (row.length != n) failInput();
            for (String token : row) result.add(scalar(item.kind(), token));
        }
        return result;
    }
    static Object node(String name, Object value) throws Exception {
        Object result = Class.forName(name).getConstructor().newInstance();
        result.getClass().getField("val").set(result, value);
        return result;
    }
    static void field(Object node, String name, Object value) throws Exception {
        node.getClass().getField(name).set(node, value);
    }
    static Object field(Object node, String name) throws Exception {
        return node.getClass().getField(name).get(node);
    }
    static Object readLink(ApHInput in, ApHType item) throws Exception {
        java.util.List<Object> values = readList(in, item);
        Object head = null, tail = null;
        for (Object value : values) {
            Object next = node("ListNode", value);
            if (tail == null) head = next; else field(tail, "next", next);
            tail = next;
        }
        return head;
    }
    static Object readTree(ApHInput in, ApHType item) throws Exception {
        String[] row = tokens(in.line());
        if (row.length == 0 || (row[0].equals("#") && row.length != 1)) failInput();
        if (row[0].equals("#")) return null;
        Object root = node("TreeNode", scalar(item.kind(), row[0]));
        java.util.ArrayDeque<Object> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        int index = 1;
        while (index < row.length) {
            if (queue.isEmpty()) failInput();
            Object parent = queue.remove();
            for (String side : new String[]{"left", "right"}) {
                if (index == row.length) break;
                String token = row[index++];
                if (!token.equals("#")) {
                    Object child = node("TreeNode", scalar(item.kind(), token));
                    field(parent, side, child); queue.add(child);
                }
            }
        }
        return root;
    }
    static String text(Object value, String kind) {
        java.util.Objects.requireNonNull(value);
        if (kind.equals("DOUBLE")) {
            double number = (double)value;
            if (Double.isNaN(number)) return "NaN";
            if (Double.isInfinite(number)) return number < 0 ? "-Infinity" : "Infinity";
            String formatted = String.format(java.util.Locale.ROOT, "%.12f", number);
            return formatted.equals("-0.000000000000") ? "0.000000000000" : formatted;
        }
        if (kind.equals("CHAR") && ((char)value < 33 || (char)value > 126)) throw new IllegalArgumentException();
        return value.toString();
    }
    static void quote(ApHOutput out, String value) {
        out.add("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                out.add(value.substring(i, i + 2)); i++; continue;
            }
            out.add(switch (c) {
                case '\\' -> "\\\\"; case '"' -> "\\\""; case '\n' -> "\\n";
                case '\t' -> "\\t"; case '\r' -> "\\r"; case '\b' -> "\\b"; case '\f' -> "\\f";
                default -> c < 32 ? String.format(java.util.Locale.ROOT, "\\u%04x", (int)c) : String.valueOf(c);
            });
        }
        out.add("\"");
    }
    static void write(ApHOutput out, Object value, ApHType type) throws Exception {
        switch (type.kind()) {
            case "LIST" -> writeList(out, (java.util.List<?>)value, type.of());
            case "ARRAY" -> writeArray(out, value, type.dimensions(), type.of());
            case "LINKED_LIST" -> writeLink(out, value, type.of());
            case "BINARY_TREE" -> writeTree(out, value, type.of());
            default -> out.add(text(value, type.kind()));
        }
    }
    static void writeList(ApHOutput out, java.util.List<?> values, ApHType item) throws Exception {
        out.add("[");
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) out.add(",");
            Object value = values.get(i);
            if (item.kind().equals("STRING") || item.kind().equals("CHAR")) quote(out, text(value, item.kind()));
            else write(out, value, item);
        }
        out.add("]");
    }
    static void writeArray(ApHOutput out, Object array, int depth, ApHType item) throws Exception {
        int[] shape = new int[depth]; java.util.Arrays.fill(shape, -1);
        checkArray(array, depth, 0, shape);
        int n = java.lang.reflect.Array.getLength(array);
        boolean quoted = item.kind().equals("STRING");
        if (quoted) out.add("[");
        for (int i = 0; i < n; i++) {
            if (i != 0) out.add(quoted ? "," : depth == 1 ? " " : depth == 2 ? "\n" : "\n\n");
            Object value = java.lang.reflect.Array.get(array, i);
            if (depth > 1) writeArray(out, value, depth - 1, item);
            else if (quoted) quote(out, text(value, "STRING"));
            else out.add(text(value, item.kind()));
        }
        if (quoted) out.add("]");
    }
    static void checkArray(Object array, int depth, int level, int[] shape) {
        int n = java.lang.reflect.Array.getLength(array);
        if (shape[level] != -1 && shape[level] != n) throw new IllegalArgumentException("Invalid result");
        shape[level] = n;
        if (level + 1 < depth) for (int i = 0; i < n; i++) checkArray(java.lang.reflect.Array.get(array, i), depth, level + 1, shape);
    }
    static void writeLink(ApHOutput out, Object head, ApHType item) throws Exception {
        ApHOutput values = new ApHOutput();
        int count = 0;
        for (Object node = head; node != null; node = field(node, "next")) {
            if (++count > 1000000) ApHOutput.fail();
            if (count > 1) values.add(" ");
            values.add(text(field(node, "val"), item.kind()));
        }
        out.add(Integer.toString(count)); out.add("\n"); out.add(values.text.toString());
    }
    static void writeTree(ApHOutput out, Object root, ApHType item) throws Exception {
        if (root == null) { out.add("#"); return; }
        java.util.LinkedList<Object> queue = new java.util.LinkedList<>();
        queue.add(root);
        int count = 0, pending = 0;
        boolean first = true;
        while (!queue.isEmpty()) {
            Object node = queue.remove();
            if (node == null) { pending++; continue; }
            if (++count > 1000000) ApHOutput.fail();
            while (pending-- > 0) out.add(" #");
            pending = 0;
            if (!first) out.add(" "); first = false;
            out.add(text(field(node, "val"), item.kind()));
            queue.add(field(node, "left")); queue.add(field(node, "right"));
        }
    }
}
class ApHRunner {
  public static void main(String[] apH_args) throws Exception {
    ApHInput apH_in = new ApHInput();
    int[] apH_p0 = (int[]) ApHCodec.read(apH_in, new ApHType("ARRAY", 1, new ApHType("INT", 0, null)));
    apH_in.finish();
    int[] apH_r = new Solution().echo(apH_p0);
    ApHOutput apH_out = new ApHOutput();
    ApHCodec.write(apH_out, apH_r, new ApHType("ARRAY", 1, new ApHType("INT", 0, null)));
    apH_out.publish();
  }
}
