// Closed-model load on the grading path: each virtual user logs in, submits a correct Python
// solution, polls until a verdict, then submits again. Levels run one after another with a gap so
// the queue drains between them. Run from 05-coding/k6:
//   k6 run -e PROBLEM_ID=<id> -e LEVELS=2,5,10,20 -e HOLD=45s submit-load.js
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE = __ENV.BASE_URL || "http://localhost:8080/api/v1";
const MAIL = __ENV.MAILPIT_URL || "http://localhost:8025/api/v1";
const PROBLEM_ID = __ENV.PROBLEM_ID;
const LEVELS = (__ENV.LEVELS || "2,5,10,20").split(",").map(Number);
const HOLD = __ENV.HOLD || "45s";
const GAP_S = Number(__ENV.GAP || 20);
const MAX_WAIT_S = Number(__ENV.MAX_WAIT || 120);
const POLL_S = Number(__ENV.POLL || 0.5);
const PASSWORD = "Matkhau123";
const SOURCE =
  "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n";
const TERMINAL = [
  "ACCEPTED",
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
  "COMPILE_ERROR",
  "SYSTEM_ERROR",
];

const toVerdict = new Trend("time_to_verdict_ms", true);
const verdicts = new Counter("verdicts");
const submitCodes = new Counter("submit_http_status");
const verdictReached = new Rate("verdict_reached");

function holdSeconds() {
  const match = /^(\d+)(s|m)$/.exec(HOLD);
  return Number(match[1]) * (match[2] === "m" ? 60 : 1);
}

const scenarios = {};
const thresholds = { verdict_reached: ["rate>0"] };
let offset = 0;
for (const level of LEVELS) {
  scenarios["level_" + level] = {
    executor: "constant-vus",
    vus: level,
    duration: HOLD,
    startTime: offset + "s",
    gracefulStop: MAX_WAIT_S + "s",
    tags: { level: String(level) },
  };
  // An empty threshold only makes k6 print that level's own sub-metrics in the summary.
  thresholds["time_to_verdict_ms{level:" + level + "}"] = [];
  thresholds["verdict_reached{level:" + level + "}"] = [];
  thresholds["http_req_duration{name:submit,level:" + level + "}"] = [];
  offset += holdSeconds() + GAP_S;
}

export const options = {
  scenarios,
  thresholds,
  setupTimeout: "600s",
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

function otpFor(email) {
  for (let i = 0; i < 30; i++) {
    const found = http.get(
      MAIL + "/search?query=" + encodeURIComponent("to:" + email) + "&limit=1",
      { tags: { name: "setup-mail" } },
    );
    const hit = (found.json("messages") || [])[0];
    if (hit) {
      const message = http.get(MAIL + "/message/" + hit.ID, {
        tags: { name: "setup-mail" },
      });
      const code = /\b(\d{6})\b/.exec(message.json("Text") || "");
      if (code) return code[1];
    }
    sleep(0.5);
  }
  throw new Error("no OTP mailed to " + email);
}

export function setup() {
  if (!PROBLEM_ID) throw new Error("pass -e PROBLEM_ID=<published problem id>");
  const total = LEVELS.reduce((sum, level) => sum + level, 0);
  const run = Date.now();
  const emails = [];
  const json = { headers: { "Content-Type": "application/json" }, tags: { name: "setup" } };
  for (let i = 0; i < total; i++) {
    const email = "k6-load-" + run + "-" + i + "@load.test";
    const registered = http.post(
      BASE + "/identity/auth/register",
      JSON.stringify({ email, password: PASSWORD, displayName: "k6 " + i }),
      json,
    );
    if (registered.status !== 201) {
      throw new Error("register " + registered.status + " " + registered.body);
    }
    const verified = http.post(
      BASE + "/identity/auth/email/verify",
      JSON.stringify({ email, otp: otpFor(email) }),
      json,
    );
    if (verified.status >= 300) {
      throw new Error("verify " + verified.status + " " + verified.body);
    }
    emails.push(email);
  }
  return { emails };
}

let token = null;
let tokenFor = null;

function login(email) {
  const response = http.post(
    BASE + "/identity/auth/login",
    JSON.stringify({ email, password: PASSWORD }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "login" } },
  );
  check(response, { "login 200": (r) => r.status === 200 });
  token = response.status === 200 ? response.json("accessToken") : null;
  tokenFor = email;
}

export default function (data) {
  const email = data.emails[(__VU - 1) % data.emails.length];
  if (token === null || tokenFor !== email) login(email);
  if (token === null) {
    sleep(1);
    return;
  }
  const auth = { Authorization: "Bearer " + token, "Content-Type": "application/json" };
  const submit = http.post(
    BASE + "/judge/problems/" + PROBLEM_ID + "/submissions",
    JSON.stringify({
      language: "PYTHON",
      submissionModel: "FUNCTION_WRAPPER",
      sourceCode: SOURCE,
      sourceInputMethod: "EDITOR",
    }),
    { headers: auth, tags: { name: "submit" } },
  );
  submitCodes.add(1, { status: String(submit.status) });
  if (submit.status === 401) {
    token = null;
    return;
  }
  if (submit.status !== 201) {
    verdictReached.add(false);
    sleep(1);
    return;
  }
  const id = submit.json("submissionId");
  const started = Date.now();
  let status = submit.json("status");
  while (!TERMINAL.includes(status) && (Date.now() - started) / 1000 < MAX_WAIT_S) {
    sleep(POLL_S);
    const poll = http.get(BASE + "/judge/submissions/" + id, {
      headers: auth,
      tags: { name: "poll" },
    });
    if (poll.status === 200) {
      status = poll.json("status");
    } else if (poll.status === 401) {
      token = null;
      break;
    }
  }
  const reached = TERMINAL.includes(status);
  verdictReached.add(reached);
  if (reached) {
    toVerdict.add(Date.now() - started);
    verdicts.add(1, { status });
  }
  sleep(1 + Math.random());
}
