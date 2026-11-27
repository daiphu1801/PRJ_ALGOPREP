// Seeds one published Python "echo" problem (8 testcases) through the real API plus the same SQL
// the e2e specs use, and prints its id. Run once per load database:
//   ALGOPREP_DB_NAME=algoprep_load node seed-fixture.mjs
import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";

const BASE = process.env.BASE_URL ?? "http://localhost:8080/api/v1";
const MAIL = process.env.MAILPIT_URL ?? "http://localhost:8025/api/v1";
const PG_CONTAINER = process.env.E2E_PG_CONTAINER ?? "algoprep-postgres";
const PG_USER = process.env.E2E_PG_USER ?? "algoprep";
const PG_DB = process.env.ALGOPREP_DB_NAME ?? "algoprep_load";
const PASSWORD = "Matkhau123";
const SOURCE =
  "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n";

function sql(statement) {
  return execFileSync(
    "docker",
    ["exec", PG_CONTAINER, "psql", "-U", PG_USER, "-d", PG_DB, "-At", "-c", statement],
    { encoding: "utf8" },
  ).trim();
}

async function call(method, path, body, token) {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null, text };
}

async function otpFor(email) {
  for (let i = 0; i < 30; i++) {
    const found = await (
      await fetch(`${MAIL}/search?query=${encodeURIComponent("to:" + email)}&limit=1`)
    ).json();
    const hit = found.messages?.[0];
    if (hit) {
      const message = await (await fetch(`${MAIL}/message/${hit.ID}`)).json();
      const code = /\b(\d{6})\b/.exec(message.Text ?? "")?.[1];
      if (code) return code;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`no OTP mailed to ${email}`);
}

const email = `k6-author-${Date.now()}@load.test`;
const registered = await call("POST", "/identity/auth/register", {
  email,
  password: PASSWORD,
  displayName: "k6 author",
});
if (registered.status !== 201) throw new Error(`register ${registered.status} ${registered.text}`);
const verified = await call("POST", "/identity/auth/email/verify", {
  email,
  otp: await otpFor(email),
});
if (verified.status >= 300) throw new Error(`verify ${verified.status} ${verified.text}`);
sql(
  `update identity.users set role_id=(select id from identity.roles where code='INSTRUCTOR') where email='${email}'`,
);
const authorId = sql(`select id from identity.users where email='${email}'`);
const problemId = randomUUID();
const referenceSql = "'" + SOURCE.split("\n").join("'||chr(10)||'") + "'";
sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
  values('${problemId}','K6-${problemId}','k6 load echo','Return the integer input.','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true);
  insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
  values(gen_random_uuid(),'${problemId}','integer','integer','EXACT',1,'echo','{"kind":"INT"}'::jsonb,'[{"name":"value","type":{"kind":"INT"}}]'::jsonb);
  insert into problem.worked_examples(id,problem_id,position,input,output) values(gen_random_uuid(),'${problemId}',1,'1','1'),(gen_random_uuid(),'${problemId}',2,'2','2');
  insert into problem.problem_reference_solutions(id,problem_id,language,source_code) values(gen_random_uuid(),'${problemId}','PYTHON',${referenceSql});
  insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
  select gen_random_uuid(),'${problemId}',case when i<=2 then 'SAMPLE' else 'HIDDEN' end,i,true,'INLINE',i::text||chr(10),'INLINE',i::text||chr(10),'MANUAL' from generate_series(1,8) i;`);
const login = await call("POST", "/identity/auth/login", { email, password: PASSWORD });
if (login.status !== 200) throw new Error(`login ${login.status} ${login.text}`);
const stamp = sql(
  `select to_char(updated_at at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') from problem.problems where id='${problemId}'`,
);
const published = await call(
  "PUT",
  `/problem-bank/authoring/problems/${problemId}/status`,
  { status: "PUBLISHED", expectedUpdatedAt: stamp },
  login.body.accessToken,
);
if (published.status !== 200) throw new Error(`publish ${published.status} ${published.text}`);
console.log(problemId);
