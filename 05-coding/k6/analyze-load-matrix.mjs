// Reads existing evidence only; never starts a server, seed, or load generator.
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
const dir = resolve(process.argv[2] ?? "");
if (!process.argv[2])
  throw new Error(
    "Usage: node analyze-load-matrix.mjs <matrix-output-directory>",
  );
function readJson(path) {
  const bytes = readFileSync(path);
  const text =
    bytes[0] === 255 && bytes[1] === 254
      ? bytes.toString("utf16le")
      : bytes.toString("utf8");
  return JSON.parse(text.replace(/^\uFEFF/, ""));
}
const manifest = readJson(join(dir, "matrix-manifest.json"));
function value(summary, name, key) {
  const metric = summary.metrics?.[name];
  const values = metric?.values ?? metric;
  const result =
    key === "rateValue" ? (values?.rate ?? values?.value) : values?.[key];
  return typeof result === "number" && Number.isFinite(result) ? result : null;
}
function count(summary, name) {
  return value(summary, name, "count");
}
function ratio(n, d) {
  return n !== null && d !== null && d > 0 ? n / d : null;
}
function cpu(path) {
  if (!existsSync(path)) return null;
  const text = readFileSync(path, "utf8").replace(/^\uFEFF/, "");
  const lines = text.trim().split(/\r?\n/);
  const columns = lines.shift()?.split(",") ?? [];
  const data = lines.map((line) => line.split(","));
  if (data.some((row) => row.length !== columns.length))
    return { invalidCsv: true };
  const output = {};
  for (const column of [
    "host_cpu",
    "backend",
    "k6",
    "vmmem",
    "go_judge_docker",
  ]) {
    const index = columns.indexOf(column);
    const samples = data
      .map((row) => row[index])
      .filter((raw) => typeof raw === "string" && raw.trim().length > 0)
      .map(Number)
      .filter(Number.isFinite);
    output[column] = samples.length
      ? {
          samples: samples.length,
          mean: samples.reduce((a, b) => a + b, 0) / samples.length,
          max: Math.max(...samples),
        }
      : null;
  }
  return output;
}
const rows = manifest.rows.map((row) => {
  const summaryPath = join(dir, row.id, "summary.json");
  if (!existsSync(summaryPath))
    return { ...row, evidenceStatus: "MISSING_SUMMARY" };
  const s = readJson(summaryPath);
  const admissions = value(s, "admission_accepted", "passes");
  const restProgress = count(s, "progress_reads") ?? 0;
  const restResult = count(s, "result_reads") ?? 0;
  return {
    ...row,
    evidenceStatus:
      row.k6ExitCode === 0
        ? "PASSED"
        : row.k6ExitCode === null
          ? "RUNNING"
          : "FAILED",
    admissions,
    admissionRate: value(s, "admission_accepted", "rateValue"),
    submitStatus: Object.fromEntries(
      [0, 201, 400, 401, 403, 404, 409, 422, 429, 500, 503].map((status) => [
        status,
        count(s, "submit_http_status{status:" + status + "}") ?? 0,
      ]),
    ),
    verdictCount: count(s, "verdicts"),
    verdictsPerSecond: value(s, "verdicts", "rate"),
    acceptedCount: count(s, "verdicts{status:ACCEPTED}"),
    reachedRate: value(s, "verdict_reached", "rateValue"),
    correctRate: value(s, "accepted_result_correct", "rateValue"),
    p95Ms: value(s, "time_to_verdict_ms", "p(95)"),
    p99Ms: value(s, "time_to_verdict_ms", "p(99)"),
    submitP95Ms: value(s, "http_req_duration{name:submit}", "p(95)"),
    restProgressReads: restProgress,
    restResultReads: restResult,
    restReadsPerAdmission: ratio(restProgress + restResult, admissions),
    fallbackReads: count(s, "progress_reads{reason:fallback-3s}"),
    hints: count(s, "stomp_hints"),
    connections: count(s, "stomp_connections"),
    connectedRate: value(s, "stomp_connected", "rateValue"),
    reconnects: count(s, "stomp_reconnects"),
    stompErrors: count(s, "stomp_errors"),
    fallbackExhausted: count(s, "fallback_exhausted"),
    byLanguage: Object.fromEntries(
      ["JAVA", "CPP", "PYTHON"].map((language) => [
        language,
        {
          verdictCount: count(s, "verdicts{language:" + language + "}"),
          p95Ms: value(
            s,
            "time_to_verdict_ms{language:" + language + "}",
            "p(95)",
          ),
          reachedRate: value(
            s,
            "verdict_reached{language:" + language + "}",
            "rateValue",
          ),
        },
      ]),
    ),
    cpu: cpu(join(dir, row.id, "cpu.csv")),
  };
});
const numeric = (values) =>
  values.filter((v) => typeof v === "number" && Number.isFinite(v));
const band = (values) => {
  const n = numeric(values);
  return n.length
    ? {
        min: Math.min(...n),
        max: Math.max(...n),
        mean: n.reduce((a, b) => a + b, 0) / n.length,
      }
    : null;
};
const comparisons = manifest.levels.map((vus) => {
  const modes = {};
  for (const mode of ["poll", "ui"]) {
    const selected = rows.filter((row) => row.vus === vus && row.mode === mode);
    modes[mode] = {
      rows: selected.length,
      passed: selected.filter((row) => row.evidenceStatus === "PASSED").length,
      verdictsPerSecond: band(selected.map((row) => row.verdictsPerSecond)),
      p95Ms: band(selected.map((row) => row.p95Ms)),
      restReadsPerAdmission: band(
        selected.map((row) => row.restReadsPerAdmission),
      ),
    };
  }
  const poll = modes.poll.verdictsPerSecond?.mean;
  const ui = modes.ui.verdictsPerSecond?.mean;
  return {
    vus,
    ...modes,
    uiThroughputChangePercent:
      typeof poll === "number" && poll > 0 && typeof ui === "number"
        ? 100 * (ui / poll - 1)
        : null,
  };
});
const expectedRows = manifest.levels.length * manifest.repeats * 2;
const analysis = {
  generatedAt: new Date().toISOString(),
  expectedRows,
  actualRows: rows.length,
  completed: Boolean(manifest.completedAt),
  configuration: manifest.configuration,
  loadGeneratorSharesHost: manifest.loadGeneratorSharesHost,
  rows,
  comparisons,
};
const fmt = (n, d = 2) => (typeof n === "number" ? n.toFixed(d) : "N/A");
const fmtBand = (b) => (b ? fmt(b.min) + ".." + fmt(b.max) : "N/A");
const md = [
  "# A-6/A-7 measured matrix",
  "",
  "Generated from existing raw evidence; failed/missing rows remain visible.",
  "",
  "Expected rows: " +
    expectedRows +
    "; observed rows: " +
    rows.length +
    "; matrix completed: " +
    analysis.completed +
    ".",
  "Configuration: " + manifest.configuration,
  "Load generator shares host: " + manifest.loadGeneratorSharesHost + ".",
  "",
  "| Row | Exit/drain | Admissions | AC/final | Verdict/s | p95/p99 ms | REST/admitted | WS connected/hints | Submit503/429 |",
  "| --- | --- | ---: | --- | ---: | --- | ---: | --- | --- |",
];
for (const r of rows)
  md.push(
    "| " +
      r.id +
      " | " +
      r.k6ExitCode +
      " / " +
      r.drain +
      " | " +
      (r.admissions ?? "N/A") +
      " | " +
      (r.acceptedCount ?? "N/A") +
      " / " +
      (r.verdictCount ?? "N/A") +
      " | " +
      fmt(r.verdictsPerSecond) +
      " | " +
      fmt(r.p95Ms) +
      " / " +
      fmt(r.p99Ms) +
      " | " +
      fmt(r.restReadsPerAdmission) +
      " | " +
      fmt(r.connectedRate) +
      " / " +
      (r.hints ?? "N/A") +
      " | " +
      (r.submitStatus?.[503] ?? "N/A") +
      " / " +
      (r.submitStatus?.[429] ?? "N/A") +
      " |",
  );
md.push(
  "",
  "| VUs | Poll rows/pass | UI rows/pass | Poll verdict/s min..max | UI verdict/s min..max | UI change of mean | Poll p95 range | UI p95 range | Poll/UI REST per admission ranges |",
  "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
);
for (const c of comparisons)
  md.push(
    "| " +
      c.vus +
      " | " +
      c.poll.rows +
      "/" +
      c.poll.passed +
      " | " +
      c.ui.rows +
      "/" +
      c.ui.passed +
      " | " +
      fmtBand(c.poll.verdictsPerSecond) +
      " | " +
      fmtBand(c.ui.verdictsPerSecond) +
      " | " +
      fmt(c.uiThroughputChangePercent) +
      "% | " +
      fmtBand(c.poll.p95Ms) +
      " | " +
      fmtBand(c.ui.p95Ms) +
      " | " +
      fmtBand(c.poll.restReadsPerAdmission) +
      " / " +
      fmtBand(c.ui.restReadsPerAdmission) +
      " |",
  );
md.push(
  "",
  "Throughput is the k6 verdict counter rate, including the measured row's completion tail; it is not HTTP req/s. Admission is a201, AC/final counts are verified multi-testcase results. REST ratio excludes submit/login. New rows have the same languages/cases/think time across modes; historical Python-only numbers are not directly comparable. Ranges include failed rows when numeric evidence exists; inspect correctness/admission before interpreting capacity. Missing summaries are not silently zero-filled for primary rates/latency. Two repetitions give an observed range, not a confidence interval or a deployment-wide student capacity claim.",
  "",
  "Per-language details and sampled CPU are in analysis.json; raw evidence remains in each row directory. If WS connected/hints are missing or zero, do not describe the UI row as successful realtime load.",
);
writeFileSync(
  join(dir, "analysis.json"),
  JSON.stringify(analysis, null, 2) + "\n",
);
writeFileSync(join(dir, "analysis.md"), md.join("\n") + "\n");
console.log(
  JSON.stringify({
    report: join(dir, "analysis.md"),
    expectedRows,
    actualRows: rows.length,
    completed: analysis.completed,
  }),
);
