import { randomUUID } from "node:crypto";
import { expect, test, type APIRequestContext } from "@playwright/test";
import { createUser, sql, type TestUser } from "./support/real-stack";

async function login(request: APIRequestContext, user: TestUser) {
  const response = await request.post("/api/v1/identity/auth/login", {
    data: { email: user.email, password: user.password },
  });
  expect(response.status()).toBe(200);
  const { accessToken } = (await response.json()) as { accessToken: string };
  expect(accessToken).toBeTruthy();
  return accessToken;
}

const sources = {
  JAVA: "class Solution { public int echo(int value) { return value; } }",
  CPP: "class Solution { public: int echo(int value) { return value; } };",
  PYTHON:
    "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n",
};

test("all toolchains deliver a final STOMP event through Caddy", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const problemId = process.env.E2E_RELEASE_PROBLEM_ID;
  expect(problemId, "published grading fixture ID").toMatch(/^[0-9a-f-]{36}$/);
  const token = await login(
    request,
    await createUser(request, { tag: "release-judge" }),
  );
  await page.goto("/login");

  for (const [language, sourceCode] of Object.entries(sources)) {
    const submissionId = await page.evaluate(
      ({ problemId, token, language, sourceCode }) =>
        new Promise<string>((resolve, reject) => {
          const socket = new WebSocket("/ws", "v12.stomp");
          let receivedId: string | undefined;
          const finalIds = new Set<string>();
          let pending = "";
          const timer = window.setTimeout(
            () => failed("No final STOMP event"),
            30_000,
          );
          function failed(message: string) {
            clearTimeout(timer);
            socket.close();
            reject(new Error(message));
          }
          function finish() {
            if (receivedId && finalIds.has(receivedId)) {
              clearTimeout(timer);
              socket.close();
              resolve(receivedId);
            }
          }
          socket.addEventListener("error", () =>
            failed("WebSocket connection failed"),
          );
          socket.addEventListener("open", () => {
            socket.send(
              `CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer ${token}\n\n\0`,
            );
          });
          socket.addEventListener("message", ({ data }) => {
            pending += String(data);
            const frames = pending.split("\0");
            pending = frames.pop() ?? "";
            for (const raw of frames) {
              const frame = raw.trimStart();
              if (frame.startsWith("ERROR\n")) {
                failed("STOMP broker returned ERROR");
              } else if (frame.startsWith("CONNECTED\n")) {
                socket.send(
                  "SUBSCRIBE\nid:release\ndestination:/user/queue/submissions\nack:auto\n\n\0",
                );
                void fetch(`/api/v1/judge/problems/${problemId}/submissions`, {
                  method: "POST",
                  headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                  },
                  body: JSON.stringify({
                    language,
                    sourceCode,
                    submissionModel: "FUNCTION_WRAPPER",
                    sourceInputMethod: "EDITOR",
                  }),
                })
                  .then(async (response) => {
                    if (response.status !== 201) {
                      failed(`${language} submit returned ${response.status}`);
                      return;
                    }
                    receivedId = (
                      (await response.json()) as { submissionId: string }
                    ).submissionId;
                    finish();
                  })
                  .catch(() => failed("Submission request failed"));
              } else if (frame.startsWith("MESSAGE\n")) {
                const event = JSON.parse(
                  frame.slice(frame.indexOf("\n\n") + 2),
                ) as {
                  submissionId: string;
                  isFinal: boolean;
                };
                if (event.isFinal) finalIds.add(event.submissionId);
                finish();
              }
            }
          });
        }),
      { problemId, token, language, sourceCode },
    );
    const result = await request.get(
      `/api/v1/judge/submissions/${submissionId}`,
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    expect(result.ok(), language).toBe(true);
    expect(await result.json(), language).toMatchObject({ status: "ACCEPTED" });
  }
});

test("private MinIO testcase round trips through signed HTTPS URLs", async ({
  request,
}) => {
  const author = await createUser(request, {
    tag: "release-storage",
    role: "INSTRUCTOR",
  });
  const headers = { Authorization: `Bearer ${await login(request, author)}` };
  const id = randomUUID();
  const authorId = sql(
    `select id from identity.users where email='${author.email}'`,
  );
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
    values('${id}','RELEASE-${id}','Release storage check','Fixture','${authorId}','20000000-0000-4000-8000-000000000001','UNPUBLISHED',1000,256,64,false)`);
  const path = `/api/v1/problem-bank/authoring/problems/${id}`;
  const input = "x".repeat(9000) + "\n";
  const expected = "9000\n";
  const plan = await request.post(`${path}/testcases/bulk-upload-url`, {
    headers,
    data: {
      items: [
        {
          clientKey: "release",
          inputSizeBytes: input.length,
          expectedOutputSizeBytes: expected.length,
          visibility: "HIDDEN",
        },
      ],
    },
  });
  expect(plan.ok(), "sign upload").toBe(true);
  const { uploadId, items } = (await plan.json()) as {
    uploadId: string;
    items: { input: { putUrl: string }; expectedOutput: { putUrl: string } }[];
  };
  expect(items).toHaveLength(1);
  for (const [url, value] of [
    [items[0]!.input.putUrl, input],
    [items[0]!.expectedOutput.putUrl, expected],
  ] as const) {
    const signed = new URL(url);
    expect(signed.origin).toBe(process.env.E2E_BASE_URL);
    expect(
      (
        await request.put(url, {
          data: value,
          headers: { "Content-Type": "text/plain" },
        })
      ).ok(),
    ).toBe(true);
  }
  const confirm = await request.post(`${path}/testcases/bulk-upload/confirm`, {
    headers,
    data: { uploadId },
  });
  expect(confirm.ok(), "confirm upload").toBe(true);
  const detail = await request.get(path, { headers });
  expect(detail.ok(), "read author detail").toBe(true);
  const { testcases } = (await detail.json()) as {
    testcases: { inputDownloadUrl: string }[];
  };
  expect(testcases).toHaveLength(1);
  const download = new URL(testcases[0]!.inputDownloadUrl);
  expect(download.origin).toBe(process.env.E2E_BASE_URL);
  const content = await request.get(download.href);
  expect(content.ok()).toBe(true);
  expect(await content.text()).toBe(input);
  download.search = "";
  expect((await request.get(download.href)).status()).toBe(403);
});
