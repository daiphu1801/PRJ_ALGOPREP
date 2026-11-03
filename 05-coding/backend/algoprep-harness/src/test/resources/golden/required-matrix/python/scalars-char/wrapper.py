from typing import *
class Solution:
    def echo(self, p0: int, p1: int, p2: float, p3: bool, p4: str, p5: str) -> str:
        return p4
import sys as apH_sys
import os as apH_os
import re as apH_re
import math as apH_math
from collections import deque as apH_deque


def apH_input_error():
    apH_sys.stderr.write("@@APH-HARNESS-INPUT-ERROR@@\n")
    raise SystemExit(90)


def apH_output_error():
    apH_sys.stderr.write("@@APH-HARNESS-OUTPUT-LIMIT@@\n")
    raise SystemExit(91)


class ApHInput:
    def __init__(self):
        text = apH_sys.stdin.read().replace("\r\n", "\n")
        self.lines = text.split("\n")
        if text.endswith("\n") or not text:
            self.lines.pop()
        self.index = 0

    def line(self):
        if self.index == len(self.lines):
            apH_input_error()
        value = self.lines[self.index]
        self.index += 1
        return value

    def finish(self):
        if any(line.strip(" \t\r\f\v") for line in self.lines[self.index:]):
            apH_input_error()


class ApHOutput:
    def __init__(self):
        raw = apH_os.environ.get("APH_MAX_RESULT_BYTES", "")
        if not apH_re.fullmatch(r"[0-9]+", raw):
            apH_output_error()
        self.limit = int(raw)
        if not 0 < self.limit <= 9223372036854775807:
            apH_output_error()
        self.parts = []
        self.bytes = 0

    def add(self, value):
        if len(value) > self.limit - self.bytes:
            apH_output_error()
        self.bytes += len(value.encode("utf-8"))
        if self.bytes > self.limit:
            apH_output_error()
        self.parts.append(value)

    def publish(self):
        self.add("\n")
        apH_sys.stdout.write("\n@@APH-RESULT@@\n" + "".join(self.parts))


def apH_tokens(line):
    value = line.strip(" \t\r\f\v")
    return [] if not value else apH_re.split(r"[ \t\r\f\v]+", value)


def apH_scalar(kind, raw):
    if kind == "STRING":
        return raw
    value = raw.strip(" \t\r\f\v")
    if kind in ("INT", "LONG"):
        if not apH_re.fullmatch(r"[+-]?[0-9]+", value):
            raise ValueError()
        number = int(value)
        bits = 31 if kind == "INT" else 63
        if not -(1 << bits) <= number < (1 << bits):
            raise ValueError()
        return number
    if kind == "DOUBLE":
        if not apH_re.fullmatch(r"[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?", value):
            raise ValueError()
        number = float(value)
        if not apH_math.isfinite(number):
            raise ValueError()
        return number
    if kind == "BOOLEAN":
        if value not in ("true", "false"):
            raise ValueError()
        return value == "true"
    if len(raw) != 1 or not 33 <= ord(raw) <= 126:
        raise ValueError()
    return raw


def apH_size(raw):
    value = apH_scalar("INT", raw)
    if value < 0:
        raise ValueError()
    return value


def apH_sequence(source, size, item):
    if item[0] in ("STRING", "LIST"):
        return [apH_read(source, item) for _ in range(size)]
    row = apH_tokens(source.line())
    if len(row) != size:
        raise ValueError()
    return [apH_scalar(item[0], token) for token in row]


def apH_array(source, shape, item):
    if len(shape) == 1:
        return apH_sequence(source, shape[0], item)
    return [apH_array(source, shape[1:], item) for _ in range(shape[0])]


def apH_read(source, schema):
    kind, depth, item = schema
    try:
        if kind == "ARRAY":
            header = apH_tokens(source.line())
            if len(header) != depth:
                raise ValueError()
            return apH_array(source, [apH_size(x) for x in header], item)
        if kind == "LIST":
            return apH_sequence(source, apH_size(source.line()), item)
        if kind == "LINKED_LIST":
            values = apH_sequence(source, apH_size(source.line()), item)
            head = None
            for value in reversed(values):
                head = ListNode(value, head)
            return head
        if kind == "BINARY_TREE":
            row = apH_tokens(source.line())
            if not row or (row[0] == "#" and len(row) != 1):
                raise ValueError()
            if row[0] == "#":
                return None
            root = TreeNode(apH_scalar(item[0], row[0]))
            queue = apH_deque([root])
            index = 1
            while index < len(row):
                if not queue:
                    raise ValueError()
                parent = queue.popleft()
                for side in ("left", "right"):
                    if index == len(row):
                        break
                    token = row[index]
                    index += 1
                    if token != "#":
                        child = TreeNode(apH_scalar(item[0], token))
                        setattr(parent, side, child)
                        queue.append(child)
            return root
        return apH_scalar(kind, source.line())
    except (ValueError, OverflowError, TypeError):
        apH_input_error()


def apH_text(value, kind):
    if kind == "BOOLEAN":
        if type(value) is not bool:
            raise TypeError("Invalid result")
        return "true" if value is True else "false"
    if kind in ("INT", "LONG"):
        bits = 31 if kind == "INT" else 63
        if type(value) is not int or not -(1 << bits) <= value < (1 << bits):
            raise TypeError("Invalid result")
        return str(value)
    if kind == "DOUBLE":
        if type(value) not in (int, float):
            raise TypeError("Invalid result")
        if apH_math.isnan(value):
            return "NaN"
        if apH_math.isinf(value):
            return "-Infinity" if value < 0 else "Infinity"
        formatted = format(value, ".12f")
        return "0.000000000000" if formatted == "-0.000000000000" else formatted
    if not isinstance(value, str) or (kind == "CHAR" and (len(value) != 1 or not 33 <= ord(value) <= 126)):
        raise TypeError("Invalid result")
    return value


def apH_quote(out, value):
    out.add('"')
    for char in value:
        escapes = {'\\': '\\\\', '"': '\\"', '\n': '\\n', '\t': '\\t', '\r': '\\r', '\b': '\\b', '\f': '\\f'}
        out.add(escapes.get(char, ('\\u%04x' % ord(char)) if ord(char) < 32 else char))
    out.add('"')


def apH_check_array(value, depth, shape, level=0):
    if type(value) is not list or (shape[level] is not None and shape[level] != len(value)):
        raise TypeError("Invalid result")
    shape[level] = len(value)
    if level + 1 < depth:
        for child in value:
            apH_check_array(child, depth, shape, level + 1)


def apH_write_array(out, value, depth, item):
    apH_check_array(value, depth, [None] * depth)
    quoted = item[0] == "STRING"
    if quoted:
        out.add("[")
    for index, child in enumerate(value):
        if index:
            out.add("," if quoted else " " if depth == 1 else "\n" if depth == 2 else "\n\n")
        if depth > 1:
            apH_write_array(out, child, depth - 1, item)
        elif quoted:
            apH_quote(out, apH_text(child, "STRING"))
        else:
            out.add(apH_text(child, item[0]))
    if quoted:
        out.add("]")


def apH_write(out, value, schema):
    kind, depth, item = schema
    if kind == "ARRAY":
        if value is None:
            raise TypeError("Invalid result")
        apH_write_array(out, value, depth, item)
    elif kind == "LIST":
        out.add("[")
        for index, child in enumerate(value):
            if index:
                out.add(",")
            if item[0] in ("STRING", "CHAR"):
                apH_quote(out, apH_text(child, item[0]))
            else:
                apH_write(out, child, item)
        out.add("]")
    elif kind == "LINKED_LIST":
        values = ApHOutput()
        count = 0
        while value is not None:
            count += 1
            if count > 1000000:
                apH_output_error()
            if count > 1:
                values.add(" ")
            values.add(apH_text(value.val, item[0]))
            value = value.next
        out.add(str(count) + "\n")
        for part in values.parts:
            out.add(part)
    elif kind == "BINARY_TREE":
        if value is None:
            out.add("#")
            return
        queue = apH_deque([value])
        count, pending = 0, 0
        first = True
        while queue:
            node = queue.popleft()
            if node is None:
                pending += 1
                continue
            count += 1
            if count > 1000000:
                apH_output_error()
            for _ in range(pending):
                out.add(" #")
            pending = 0
            if not first:
                out.add(" ")
            first = False
            out.add(apH_text(node.val, item[0]))
            queue.extend((node.left, node.right))
    else:
        out.add(apH_text(value, kind))
# Trusted epilogue helper. Only executed inside the judge sandbox.
import inspect as apH_inspect
import sys as apH_sys


def apH_signature_error():
    apH_sys.stderr.write("@@APH-HARNESS-SIGNATURE-ERROR@@\n")
    raise SystemExit(92)


def apH_resolve_call(apH_name, apH_args):
    apH_missing = object()
    apH_solution = globals().get("Solution", apH_missing)
    if apH_solution is apH_missing or not callable(apH_solution):
        apH_signature_error()
    # Constructor exceptions retain their runtime classification.
    apH_instance = apH_solution()
    apH_static = apH_inspect.getattr_static(apH_instance, apH_name, apH_missing)
    try:
        apH_callable = getattr(apH_instance, apH_name)
    except AttributeError:
        if apH_static is apH_missing:
            apH_signature_error()
        raise
    if not callable(apH_callable):
        apH_signature_error()
    try:
        apH_signature = apH_inspect.signature(apH_callable)
    except (TypeError, ValueError):
        # Unsupported introspection does not prove that the callable is invalid.
        return apH_callable
    try:
        apH_signature.bind(*apH_args)
    except TypeError:
        apH_signature_error()
    # The actual invocation belongs outside this guard; body exceptions remain runtime errors.
    return apH_callable

apH_in = ApHInput()
apH_p0 = apH_read(apH_in, ("INT", 0, None))
apH_p1 = apH_read(apH_in, ("LONG", 0, None))
apH_p2 = apH_read(apH_in, ("DOUBLE", 0, None))
apH_p3 = apH_read(apH_in, ("BOOLEAN", 0, None))
apH_p4 = apH_read(apH_in, ("CHAR", 0, None))
apH_p5 = apH_read(apH_in, ("STRING", 0, None))
apH_in.finish()
apH_fn = apH_resolve_call("echo", [apH_p0, apH_p1, apH_p2, apH_p3, apH_p4, apH_p5])
apH_r = apH_fn(apH_p0, apH_p1, apH_p2, apH_p3, apH_p4, apH_p5)
apH_out = ApHOutput()
apH_write(apH_out, apH_r, ("CHAR", 0, None))
apH_out.publish()
