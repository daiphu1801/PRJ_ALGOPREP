// A-6/A-7: one matrix row. The PowerShell runner repeats identical rows and drains between them.
// ui = STOMP hints + authoritative REST snapshots + 3s silence fallback (up to 10 polls).
// poll = historical 0.5s detail polling. Both modes use the SAME fixture/languages/think time.
import http from "k6/http";
import ws from "k6/ws";
import exec from "k6/execution";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE = (__ENV.BASE_URL || "http://localhost:8081/api/v1").replace(
  /\/$/,
  "",
);
const WS =
  __ENV.WS_URL || BASE.replace(/^http/, "ws").replace(/\/api\/v1$/, "/ws");
const ORIGIN = __ENV.WS_ORIGIN || "http://localhost:3000";
const MODE = __ENV.MODE || "ui";
const VUS = Number(__ENV.VUS || 40);
const REPEAT = Number(__ENV.REPEAT || 1);
const HOLD = __ENV.HOLD || "180s";
const MAX_WAIT_S = Number(__ENV.MAX_WAIT || 120);
const MIN_ITERATION_S = Number(__ENV.MIN_ITERATION || 3.1);
const fixture = __ENV.FIXTURE ? JSON.parse(open(__ENV.FIXTURE)) : null;
const LANGUAGES = ["JAVA", "CPP", "PYTHON"];
const SOURCES = {
  JAVA: "class Solution { public int echo(int value) { return value; } }\n",
  CPP: "class Solution { public: int echo(int value) { return value; } };\n",
  PYTHON:
    "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n",
};
const TERMINAL = [
  "ACCEPTED",
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
  "COMPILE_ERROR",
  "SYSTEM_ERROR",
];
if (
  !["ui", "poll"].includes(MODE) ||
  !Number.isInteger(VUS) ||
  VUS < 1 ||
  !Number.isInteger(REPEAT) ||
  REPEAT < 1
) {
  throw new Error("MODE must be ui/poll and VUS/REPEAT positive integers");
}

const accepted = new Rate("admission_accepted");
const reached = new Rate("verdict_reached");
const correct = new Rate("accepted_result_correct");
const latency = new Trend("time_to_verdict_ms", true);
const submitStatus = new Counter("submit_http_status");
const verdicts = new Counter("verdicts");
const progressReads = new Counter("progress_reads");
const resultReads = new Counter("result_reads");
const socketAttempts = new Counter("stomp_connections");
const socketConnected = new Rate("stomp_connected");
const socketHints = new Counter("stomp_hints");
const socketErrors = new Counter("stomp_errors");
const reconnects = new Counter("stomp_reconnects");
const fallbackExhausted = new Counter("fallback_exhausted");
const networkErrors = new Counter("network_errors");
const authErrors = new Counter("auth_errors");
const tags = { mode: MODE, vus: String(VUS), repeat: String(REPEAT) };
const thresholds = {
  checks: ["rate==1"],
  auth_errors: ["count==0"],
  network_errors: [
    { threshold: "count<20", abortOnFail: true, delayAbortEval: "10s" },
  ],
  verdict_reached: ["rate==1"],
  accepted_result_correct: ["rate==1"],
  "http_req_duration{name:submit}": [],
  "progress_reads{reason:initial-recovery}": [],
  "progress_reads{reason:connected-recovery}": [],
  "progress_reads{reason:stomp-hint}": [],
  "progress_reads{reason:fallback-3s}": [],
  "result_reads{reason:poll-0.5s}": [],
};
for (const status of [0, 201, 400, 401, 403, 404, 409, 422, 429, 500, 503]) {
  thresholds["submit_http_status{status:" + status + "}"] = [];
}
for (const status of TERMINAL)
  thresholds["verdicts{status:" + status + "}"] = [];
for (const language of LANGUAGES) {
  thresholds["time_to_verdict_ms{language:" + language + "}"] = [];
  thresholds["verdict_reached{language:" + language + "}"] = [];
  thresholds["verdicts{language:" + language + "}"] = [];
}
export const options = {
  scenarios: {
    row: {
      executor: "constant-vus",
      vus: VUS,
      duration: HOLD,
      gracefulStop: MAX_WAIT_S + "s",
      tags,
    },
  },
  thresholds,
  // Authenticate serially before the timed workload, including the largest 120-actor row.
  setupTimeout: "180s",
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  systemTags: [
    "status",
    "method",
    "name",
    "scenario",
    "group",
    "check",
    "error_code",
    "proto",
  ],
};

function json(response) {
  if (response.status === 0) networkErrors.add(1);
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}
function params(token, name) {
  return {
    headers: {
      Authorization: "Bearer " + token,
      "Content-Type": "application/json",
    },
    tags: { name },
    timeout: "10s",
  };
}
function login(actor, actorIndex) {
  const response = http.post(
    BASE + "/identity/auth/login",
    JSON.stringify({ email: actor.email, password: fixture.password }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "login" },
      timeout: "10s",
    },
  );
  const body = json(response);
  if (response.status !== 200 || !body || !body.accessToken) {
    authErrors.add(1);
    const candidateCode = body && (body.errorCode || body.code);
    const safeCode =
      typeof candidateCode === "string" && /^[A-Z0-9_-]{1,48}$/.test(candidateCode)
        ? candidateCode
        : "none";
    exec.test.abort(
      "Synthetic load actor login failed in setup: actorIndex=" + actorIndex +
        " http=" + response.status + " transportCode=" + (response.error_code || 0) +
        " appCode=" + safeCode + "; verify isolated fixture/backend (no token logged)",
    );
  }
  return body.accessToken;
}
export function setup() {
  if (
    !fixture ||
    fixture.dbName !== "algoprep_load" ||
    fixture.testcaseCount < 2
  ) {
    throw new Error(
      "FIXTURE must name seed-load-matrix output for isolated algoprep_load with multiple testcases",
    );
  }
  const row = MODE + "-r" + REPEAT + "-v" + VUS;
  if (!fixture.accountSets[row] || fixture.accountSets[row].length !== VUS)
    throw new Error("No dedicated actors for " + row);
  // Setup data stays in k6 memory; do not persist or print bearer tokens.
  // Dedicated actors also make per-student submit quota independent between rows.
  const tokens = fixture.accountSets[row].map((actor, index) => login(actor, index));
  const checkProblem = http.get(
    BASE + "/problem-bank/problems/" + fixture.problemId + "/workspace",
    params(tokens[0], "setup-fixture"),
  );
  if (checkProblem.status !== 200)
    throw new Error(
      "Published fixture not available on target backend; stop before load",
    );
  return { row, tokens };
}

let token = null;
function result(id, state, reason, language) {
  resultReads.add(1, { reason, language });
  const response = http.get(
    BASE + "/judge/submissions/" + id,
    params(token, "result"),
  );
  const body = json(response);
  if (response.status === 401) token = null;
  if (response.status === 200 && body && TERMINAL.includes(body.status)) {
    state.done = true;
    state.status = body.status;
    state.correct =
      body.status === "ACCEPTED" &&
      body.totalCount === fixture.testcaseCount &&
      body.passedCount === fixture.testcaseCount;
  }
}
function snapshot(id, state, reason, language) {
  if (state.done) return;
  progressReads.add(1, { reason, language });
  const response = http.get(
    BASE + "/judge/submissions/" + id + "/progress",
    params(token, "progress"),
  );
  const body = json(response);
  if (response.status === 401) token = null;
  if (response.status !== 200 || !body || body.submissionId !== id) return;
  state.seq = Math.max(state.seq, typeof body.seq === "number" ? body.seq : -1);
  if (body.isFinal) result(id, state, reason, language);
}
function fallback(id, state, language) {
  if (state.done || Date.now() - state.lastMessage < 3000) return;
  if (state.polls >= 10) {
    if (!state.slow) fallbackExhausted.add(1, { language });
    state.slow = true;
    return;
  }
  state.polls++;
  snapshot(id, state, "fallback-3s", language);
}
function uiProgress(id, state, language, deadline) {
  snapshot(id, state, "initial-recovery", language);
  while (!state.done && token && Date.now() < deadline) {
    socketAttempts.add(1, { language });
    let connected = false;
    let pending = "";
    const response = ws.connect(
      WS,
      {
        headers: { Origin: ORIGIN, "Sec-WebSocket-Protocol": "v12.stomp" },
        tags: { name: "submission-stomp", language },
      },
      (socket) => {
        const closeIfFinal = () => {
          if (state.done) socket.close();
        };
        socket.on("open", () =>
          socket.send(
            "CONNECT\naccept-version:1.2\nhost:localhost\nheart-beat:10000,10000\nAuthorization:Bearer " +
              token +
              "\n\n\0",
          ),
        );
        socket.on("message", (message) => {
          pending += message;
          let end;
          while ((end = pending.indexOf("\0")) >= 0) {
            const frame = pending.slice(0, end).replace(/^\s+/, "");
            pending = pending.slice(end + 1);
            if (frame.startsWith("CONNECTED\n")) {
              connected = true;
              state.polls = 0;
              state.lastMessage = Date.now();
              socket.send(
                "SUBSCRIBE\nid:load-submission\ndestination:/user/queue/submissions\nack:auto\n\n\0",
              );
              snapshot(id, state, "connected-recovery", language);
            } else if (frame.startsWith("MESSAGE\n")) {
              let event;
              try {
                event = JSON.parse(frame.slice(frame.indexOf("\n\n") + 2));
              } catch (_) {
                continue;
              }
              if (
                event.submissionId !== id ||
                typeof event.seq !== "number" ||
                (!event.isFinal && event.seq <= state.seq)
              )
                continue;
              state.lastMessage = Date.now();
              socketHints.add(1, {
                language,
                final: String(Boolean(event.isFinal)),
              });
              snapshot(id, state, "stomp-hint", language);
            } else if (frame.startsWith("ERROR\n")) {
              socketErrors.add(1, { language });
              socket.close();
            }
            closeIfFinal();
          }
        });
        socket.on("error", () => socketErrors.add(1, { language }));
        socket.on("close", () => {
          state.lastMessage = 0;
        });
        socket.setInterval(() => {
          fallback(id, state, language);
          closeIfFinal();
        }, 3000);
        socket.setInterval(() => {
          if (connected && !state.done) socket.send("\n");
        }, 10000);
        socket.setTimeout(
          () => socket.close(),
          Math.max(1, deadline - Date.now()),
        );
      },
    );
    socketConnected.add(connected, { language });
    if (!response || response.status === 0) networkErrors.add(1);
    if (state.done || !token || Date.now() >= deadline) break;
    // Like the client reconnectDelay: snapshots stay authoritative, retries never synthesize a verdict.
    reconnects.add(1, { language });
    sleep(Math.min(3, Math.max(0, (deadline - Date.now()) / 1000)));
    fallback(id, state, language);
  }
}

export default function (data) {
  const started = Date.now();
  if (!data.tokens || !data.tokens[__VU - 1])
    exec.test.abort("VU has no preauthenticated dedicated synthetic actor");
  token = data.tokens[__VU - 1];
  const language = LANGUAGES[(__VU + __ITER - 1) % LANGUAGES.length];
  const submit = http.post(
    BASE + "/judge/problems/" + fixture.problemId + "/submissions",
    JSON.stringify({
      language,
      submissionModel: "FUNCTION_WRAPPER",
      sourceCode: SOURCES[language],
      sourceInputMethod: "EDITOR",
    }),
    params(token, "submit"),
  );
  const admission = json(submit);
  submitStatus.add(1, {
    status: String(submit.status),
    code: (admission && admission.errorCode) || "none",
    language,
  });
  accepted.add(submit.status === 201, { language });
  if (submit.status === 401) {
    authErrors.add(1);
    exec.test.abort("Preauthenticated load actor rejected during timed row: HTTP401 (no token logged)");
  }
  if (submit.status === 201 && admission && admission.submissionId) {
    const state = {
      done: false,
      correct: false,
      status: admission.status,
      seq: -1,
      polls: 0,
      lastMessage: 0,
      slow: false,
    };
    const admittedAt = Date.now();
    const deadline = admittedAt + MAX_WAIT_S * 1000;
    if (MODE === "ui")
      uiProgress(admission.submissionId, state, language, deadline);
    else
      while (!state.done && token && Date.now() < deadline) {
        sleep(0.5);
        result(admission.submissionId, state, "poll-0.5s", language);
      }
    reached.add(state.done, { language });
    correct.add(state.done && state.correct, { language });
    check(state, {
      "accepted submission reaches correct multi-testcase verdict": (value) =>
        value.done && value.correct,
    });
    if (state.done) {
      latency.add(Date.now() - admittedAt, { language });
      verdicts.add(1, { status: state.status, language });
    }
  }
  // A dedicated actor does not exceed the unchanged 60/hour/problem quota during a 180s row.
  sleep(Math.max(0.1, MIN_ITERATION_S - (Date.now() - started) / 1000));
}
