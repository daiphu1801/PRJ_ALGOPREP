# e2e: real-backend F1 auth specs

Files `auth-real-*.spec.ts` and `admin-users-real.spec.ts` (ADM0201, F1 #35 to #38, #50) run against
the REAL backend. All other specs in this folder assume mock mode. The real specs skip themselves (with a reason) when the stack below is not up or the
frontend runs in mock mode, so a plain `pnpm e2e` stays green without the stack.

## Required services

| Service | Default | Override env |
| :--- | :--- | :--- |
| Frontend (`pnpm dev`) in REAL mode, same-origin proxy | `http://localhost:3000` | `E2E_BASE_URL` |
| Backend (Spring Boot, profile `local`) behind that proxy | `http://localhost:8080` | `API_PROXY_TARGET` (frontend process) |
| Mailpit HTTP API (OTP mails) | `http://localhost:8025/api/v1` | `E2E_MAILPIT_URL` |
| Postgres container (SQL helper via `docker exec psql`) | `algoprep-postgres`, user/db `algoprep` | `E2E_PG_CONTAINER`, `E2E_PG_USER`, `E2E_PG_DB` |

Start order (PowerShell, never Git Bash for the frontend): `docker compose up -d` in `05-coding`,
load `05-coding/.env` into the shell, `.\mvnw.cmd -q install -DskipTests` then
`.\mvnw.cmd -pl algoprep-bootstrap spring-boot:run "-Dspring-boot.run.profiles=local"` in
`05-coding/backend`, then in `05-coding/frontend`:

```powershell
$env:NEXT_PUBLIC_MOCK_DATA = "false"
$env:NEXT_PUBLIC_API_BASE_URL = "/api/v1"
$env:API_PROXY_TARGET = "http://localhost:8080"
pnpm dev
```

## Run

```powershell
cd 05-coding/frontend
pnpm exec playwright test e2e/auth-real
```

Do not run the whole suite against a real-mode frontend: the other specs expect mock data.
`pnpm check` does not run any of this.

## How accounts are created (`support/real-stack.ts`)

`createUser` registers through the public API (`/api/v1/identity/auth/register`), reads the 6 digit
OTP from Mailpit, verifies the e-mail, then, for a role other than STUDENT, runs SQL in the
Postgres container: `update identity.users set role_id = (select id from identity.roles where
code = 'INSTRUCTOR' | 'ADMIN')`. The forced-password account additionally gets
`must_change_password = true` by SQL. E-mails look like `e2e-auth-<tag>-<timestamp>-<n>@test.vn`,
password `Matkhau123`. Accounts are never deleted (foreign keys to tokens and audit), so the local
DB accumulates them; find them with `select email from identity.users where email like 'e2e-auth-%'`.

The skip probe posts an empty login through the proxy, which counts as a failed attempt for the
empty e-mail key; it accepts IDT-102 and IDT-103 for that reason.

## SHR0202 authoring spec (`problem-authoring-real.spec.ts`)

Three cases against the real backend and MinIO: bulk testcase upload (F2-8a), running the reference solution
without leaking hidden data (F2-8b), and publishing a standard-I/O-only problem from the screen (F-C-01). The
two sandbox cases skip themselves unless `E2E_F3F4_GRADING=true` and the backend is wired to a go-judge image
with toolchains. Problems are created by SQL in the Postgres container, so run it against a demo database:

```powershell
$env:E2E_PG_DB = "algoprep_demo"; $env:E2E_F3F4_GRADING = "true"
pnpm exec playwright test problem-authoring-real --workers=1 --reporter=list
```

The problems it creates (`E2E-BULK-*`, `E2E-RUN-*`, `E2E-STDIO-*`) stay in the database, like the accounts.
