// Synthetic fixture only. Refuses every DB except algoprep_load; no load is generated here.
// CONFIRM_LOAD_STACK=yes BASE_URL=http://localhost:8081/api/v1 ALGOPREP_DB_NAME=algoprep_load
// node seed-load-matrix.mjs <output-fixture.json>
import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const BASE = (process.env.BASE_URL ?? "http://localhost:8081/api/v1").replace(
  /\/$/,
  "",
);
const MAIL = process.env.MAILPIT_URL ?? "http://localhost:8025/api/v1";
const PG_CONTAINER = process.env.E2E_PG_CONTAINER ?? "algoprep-postgres";
const PG_USER = process.env.E2E_PG_USER ?? "algoprep";
const PG_DB = process.env.ALGOPREP_DB_NAME ?? "algoprep_load";
const CASES = Number(process.env.CASE_COUNT ?? 16);
const LEVELS = (process.env.LEVELS ?? "40,80,120").split(",").map(Number);
const REPEATS = Number(process.env.REPEATS ?? 2);
const MODES = ["poll", "ui"];
const PASSWORD = "Matkhau123";
const SOURCE =
  "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n";
const out = resolve(process.argv[2] ?? "load-matrix-fixture.json");
if (PG_DB !== "algoprep_load" || process.env.CONFIRM_LOAD_STACK !== "yes") {
  throw new Error(
    "Verify isolated backend first, then set CONFIRM_LOAD_STACK=yes and ALGOPREP_DB_NAME=algoprep_load",
  );
}
if (existsSync(out))
  throw new Error("Fixture file already exists; use a new evidence path");
if (
  !Number.isInteger(CASES) ||
  CASES < 8 ||
  CASES > 100 ||
  !Number.isInteger(REPEATS) ||
  REPEATS < 1 ||
  LEVELS.some((v) => !Number.isInteger(v) || v < 1 || v > 500)
) {
  throw new Error(
    "CASE_COUNT must be8..100, REPEATS positive and LEVELS positive integers<=500",
  );
}
const quote = (value) => "'" + String(value).replaceAll("'", "''") + "'";
function sql(statement) {
  return execFileSync(
    "docker",
    [
      "exec",
      PG_CONTAINER,
      "psql",
      "-v",
      "ON_ERROR_STOP=1",
      "-U",
      PG_USER,
      "-d",
      PG_DB,
      "-At",
      "-c",
      statement,
    ],
    { encoding: "utf8" },
  ).trim();
}
async function call(method, path, body, token) {
  const response = await fetch(BASE + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(120000),
  });
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (_) {
    /* Report only HTTP/code, not body with secrets. */
  }
  if (response.status >= 300)
    throw new Error(
      method +
        " " +
        path +
        " failed HTTP" +
        response.status +
        " code=" +
        (data?.errorCode ?? "unknown"),
    );
  return data;
}
async function otpFor(email) {
  for (let i = 0; i < 30; i++) {
    const found = await (
      await fetch(
        MAIL +
          "/search?query=" +
          encodeURIComponent("to:" + email) +
          "&limit=1",
      )
    ).json();
    const hit = found.messages?.[0];
    if (hit) {
      const mail = await (await fetch(MAIL + "/message/" + hit.ID)).json();
      const match = /\b(\d{6})\b/.exec(mail.Text ?? "");
      if (match) return match[1];
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error("Synthetic author OTP was not received");
}

const runId = randomUUID();
const email = "load-author-" + runId + "@load.test";
sql("SELECT id FROM identity.roles WHERE code='STUDENT'");
await call("POST", "/identity/auth/register", {
  email,
  password: PASSWORD,
  displayName: "Isolated load author",
});
await call("POST", "/identity/auth/email/verify", {
  email,
  otp: await otpFor(email),
});
const authorId = sql(
  "SELECT id FROM identity.users WHERE email=" + quote(email),
);
if (!/^[0-9a-f-]{36}$/.test(authorId))
  throw new Error(
    "API actor absent from algoprep_load; target API and SQL DB differ, stop",
  );
sql(
  "UPDATE identity.users SET role_id=(SELECT id FROM identity.roles WHERE code='INSTRUCTOR') WHERE id=" +
    quote(authorId),
);
const problemId = randomUUID();
sql(`BEGIN;
  INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
  VALUES('${problemId}','LOAD-${problemId}','Isolated matrix echo','Return the input integer.','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true);
  INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
  VALUES(gen_random_uuid(),'${problemId}','integer','integer','EXACT',1,'echo','{"kind":"INT"}','[{"name":"value","type":{"kind":"INT"}}]');
  INSERT INTO problem.worked_examples(id,problem_id,position,input,output)
  VALUES(gen_random_uuid(),'${problemId}',1,'1','1'),(gen_random_uuid(),'${problemId}',2,'2','2');
  INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code)
  VALUES(gen_random_uuid(),'${problemId}','PYTHON',${quote(SOURCE)});
  INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
  SELECT gen_random_uuid(),'${problemId}',CASE WHEN i<=2 THEN 'SAMPLE' ELSE 'HIDDEN' END,i,true,'INLINE',i::text||chr(10),'INLINE',i::text||chr(10),'MANUAL' FROM generate_series(1,${CASES}) i;
  COMMIT;`);
const auth = await call("POST", "/identity/auth/login", {
  email,
  password: PASSWORD,
});
const stamp = sql(
  `SELECT to_char(updated_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') FROM problem.problems WHERE id='${problemId}'`,
);
await call(
  "PUT",
  "/problem-bank/authoring/problems/" + problemId + "/status",
  { status: "PUBLISHED", expectedUpdatedAt: stamp },
  auth.accessToken,
);

const accountSets = {};
const actors = [];
for (let repeat = 1; repeat <= REPEATS; repeat++)
  for (const vus of LEVELS)
    for (const mode of MODES) {
      const key = mode + "-r" + repeat + "-v" + vus;
      accountSets[key] = [];
      for (let i = 1; i <= vus; i++) {
        const actor = {
          userId: randomUUID(),
          email: "load-" + runId + "-" + key + "-" + i + "@load.test",
        };
        actors.push(actor);
        accountSets[key].push(actor);
      }
    }
// Copy the synthetic author's BCrypt hash INSIDE PostgreSQL; never print or export a hash/token.
for (let from = 0; from < actors.length; from += 50) {
  const batch = JSON.stringify(actors.slice(from, from + 50));
  sql(`INSERT INTO identity.users(id,email,display_name,password_hash,role_id,status)
    SELECT x."userId",x.email,'Synthetic load actor',a.password_hash,r.id,'ACTIVE'
    FROM jsonb_to_recordset(${quote(batch)}::jsonb) AS x("userId" uuid,email text)
    CROSS JOIN identity.users a CROSS JOIN identity.roles r
    WHERE a.id='${authorId}' AND r.code='STUDENT';`);
}
mkdirSync(dirname(out), { recursive: true });
writeFileSync(
  out,
  JSON.stringify(
    {
      version: 1,
      runId,
      createdAt: new Date().toISOString(),
      dbName: PG_DB,
      baseUrl: BASE,
      problemId,
      testcaseCount: CASES,
      password: PASSWORD,
      levels: LEVELS,
      repeats: REPEATS,
      languages: ["JAVA", "CPP", "PYTHON"],
      accountSets,
    },
    null,
    2,
  ) + "\n",
);
console.log(
  JSON.stringify({
    fixture: out,
    problemId,
    testcaseCount: CASES,
    syntheticActors: actors.length,
    dbName: PG_DB,
  }),
);
