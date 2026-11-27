// Re-applies the reviewed SonarQube decisions of triage.json and the AlgoPrep quality gate, so that a
// rebuilt server does not lose them. Dry run by default; --apply writes.
//
//   cd 05-coding && node --env-file=.env sonar/apply-triage.mjs [--apply]
//
// An entry matches by rule, file and the hash of the flagged line (how SonarQube itself tracks an issue),
// and only up to its reviewed count: a changed line, or one more identical line, stays open for review.
import { readFileSync } from "node:fs";

const { SONAR_HOST_URL, SONAR_LOGIN, SONAR_PASSWORD } = process.env;
if (!SONAR_HOST_URL || !SONAR_LOGIN || !SONAR_PASSWORD)
  throw new Error(
    "Set SONAR_HOST_URL, SONAR_LOGIN and SONAR_PASSWORD (an administrator).",
  );
const apply = process.argv.includes("--apply");
const authorization = `Basic ${Buffer.from(`${SONAR_LOGIN}:${SONAR_PASSWORD}`).toString("base64")}`;
const triage = JSON.parse(
  readFileSync(new URL("./triage.json", import.meta.url), "utf8"),
);

async function call(path, params, method = "GET") {
  const query = new URLSearchParams(params).toString();
  const url = new URL(
    `/api/${path}${method === "GET" ? `?${query}` : ""}`,
    SONAR_HOST_URL,
  );
  const response = await fetch(url, {
    method,
    headers: {
      authorization,
      ...(method === "POST"
        ? { "content-type": "application/x-www-form-urlencoded" }
        : {}),
    },
    body: method === "POST" ? query : undefined,
  });
  const text = await response.text();
  if (!response.ok)
    throw new Error(
      `${method} ${path}: HTTP ${response.status} ${text.slice(0, 300)}`,
    );
  return text ? JSON.parse(text) : {};
}

async function issues(extra) {
  const found = [];
  for (let page = 1; ; page += 1) {
    const body = await call("issues/search", {
      componentKeys: triage.projects.join(","),
      ps: "500",
      p: String(page),
      ...extra,
    });
    found.push(...body.issues);
    if (found.length >= body.paging.total) return found;
  }
}

const keyOf = (item) => `${item.rule}|${item.component}|${item.hash}`;
const open = await issues({ resolved: "false" });
const resolved = new Set((await issues({ resolved: "true" })).map(keyOf));
const pending = Map.groupBy(open, keyOf);
let marked = 0;
let skipped = 0;
let absent = 0;

for (const entry of triage.entries) {
  const matches = pending.get(keyOf(entry)) ?? [];
  if (matches.length === 0) {
    if (!resolved.has(keyOf(entry))) {
      absent += 1;
      console.log(
        `no issue for ${entry.rule} ${entry.component} (code changed or fixed)`,
      );
    }
    continue;
  }
  if (matches.length > entry.count) {
    skipped += matches.length;
    console.log(
      `review again: ${matches.length} issues but ${entry.count} reviewed, ${entry.rule} ${entry.component}`,
    );
    continue;
  }
  for (const issue of matches) {
    if (apply) {
      await call(
        "issues/do_transition",
        { issue: issue.key, transition: entry.transition },
        "POST",
      );
      await call(
        "issues/add_comment",
        { issue: issue.key, text: entry.reason },
        "POST",
      );
    }
    marked += 1;
  }
}

const gate = triage.qualityGate;
const gates = await call("qualitygates/list", {});
if (!gates.qualitygates.some((item) => item.name === gate.name)) {
  console.log(
    `quality gate ${gate.name}: ${apply ? "creating" : "would create"}`,
  );
  if (apply) await call("qualitygates/create", { name: gate.name }, "POST");
}
if (apply) {
  const defaults =
    (await call("qualitygates/show", { name: "Sonar way" })).conditions ?? [];
  const present =
    (await call("qualitygates/show", { name: gate.name })).conditions ?? [];
  for (const condition of [...defaults, ...gate.conditions]) {
    if (present.some((item) => item.metric === condition.metric)) continue;
    const { metric, op, error } = condition;
    await call(
      "qualitygates/create_condition",
      { gateName: gate.name, metric, op, error },
      "POST",
    );
  }
  for (const projectKey of triage.projects)
    await call(
      "qualitygates/select",
      { gateName: gate.name, projectKey },
      "POST",
    );
}

const left = open.length - (apply ? marked : 0);
console.log(
  `${apply ? "marked" : "would mark"} ${marked}; to review again ${skipped}; entries without issue ${absent}; ` +
    `open issues ${apply ? "left" : "now"} ${left}${apply ? "" : ` (${open.length - marked} not covered)`}`,
);
