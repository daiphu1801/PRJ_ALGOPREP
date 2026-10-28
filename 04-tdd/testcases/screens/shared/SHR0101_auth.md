# Testcase màn SHR0101 — Xác thực

- **Scope / TDD:** `04-tdd/screens/shared/SHR0101_auth.md`, AUTH-AC-01–04.
- **DD sources:** `03-dd/screens/shared/SHR0101_auth.md` §§2–7 (V0.1, 2026-10-25); `03-dd/api/F1_identity.md` §§1, #1–3, #5–8, #53–54, #60–61; `03-dd/validation/F1_identity.md` §§1–2; `03-dd/security/F1_identity.md`.
- **Fixture catalog:** Separate FE fixture tests and API/E2E tests. FE fixtures: `student-A` active STUDENT (`a@prep.test`), `teacher-A` active INSTRUCTOR, `pending-A`, `self-deactivated-A` (deactivated 2 days ago, not anonymized), `admin-locked-A`; deterministic API client, router and clock. Integration: F1 service, PostgreSQL, Redis, test mail sink with OTP capture, OAuth provider stub; valid password `PrepPass2026!` (8–128 chars, contains letters and digits), OTP `123456`, clock fixed UTC. Secrets/tickets must not be placed in URL, local storage or logs. Each test resets user/OTP/session/cookie state. All cases are **Chưa thực hiện**.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Branch inventory and coverage

| AC | Branches | Case IDs | Review |
|---|---|---|---|
| AUTH-AC-01 | Valid signup→verify; client validation boundaries; login before verification | TC-SHR0101-AUTHAC01-01, TC-SHR0101-AUTHAC01-02, TC-SHR0101-AUTHAC01-03 |
| AUTH-AC-02 | Credential/OAuth success and role-aware safe redirect; unsafe/forbidden redirect; callback failure; refresh concurrency/failure | TC-SHR0101-AUTHAC02-01..04 |
| AUTH-AC-03 | Email enumeration; invalid/expired OTP and resend cooldown; verify-before-reset; reset success without session; retry after delayed verify response | TC-SHR0101-AUTHAC03-01..04 |
| AUTH-AC-04 | One-use restore ticket; invalid/expired ticket; admin-deactivated account has no self-restore ticket | TC-SHR0101-AUTHAC04-01..03 |

## Cases

### TC-SHR0101-AUTHAC01-01 — Đăng ký hợp lệ chuyển sang xác thực email, chưa có phiên

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-01; screen DD BR-SC-02, SH-01; F1 API #1, #53 |
| Kind / priority | Normal, P0 — establishes account before authentication |
| Environment | FE mock: Vitest state-machine; integration/E2E: F1 + mail sink |
| Preconditions | Email `new.student@prep.test` unused; signup route `/register`; no access token or refresh cookie. |
| Test data | `displayName="New Student"`, email above, password `PrepPass2026!`, terms accepted in UI (not sent in #1 request). |
| Steps | Submit signup; inspect route/form state, API call, storage/cookies and mail sink; verify email using delivered OTP; inspect state after verify. |
| Expected | #1 returns 201, UI enters `verify_email`, keeps email in screen memory and does not show authenticated overlay or set access/refresh token. #53 success returns UI to login with email prefilled and success toast; still no session. Mail delivery observed only via test sink. |
| Cleanup | Delete test account and OTP/session state; clear browser memory, cookies and mail sink. |
| Evidence | FE state assertions; request/response; mail sink event; token/cookie/storage snapshot before/after. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC01-02 — Client validation reports exact signup field and does not call F1

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-01; screen DD §§3,5,7 SH-03; F1 validation §1 |
| Kind / priority | Boundary, P1 — client-side constraints and feedback channel |
| Environment | FE mock: Vitest; no backend |
| Preconditions | Signup form loaded; API client spy installed. |
| Test data | Cases: displayName length 1, 2, 100, 101; malformed email; password `lettersOnly` (no digit), `short1` (<8), and otherwise valid password. |
| Steps | Submit each case independently; inspect aria-invalid, focus/toast, and API call count. |
| Expected | 2- and 100-char names and a compliant email/password may pass client schema; 1/101 name, malformed email, short/no-digit password are blocked on their respective fields. Error uses the screen's single-toast pattern plus `aria-invalid`; no F1 request for client-invalid cases. Server remains authoritative for password/email cross-field rules. |
| Cleanup | Reset form between inputs. |
| Evidence | Per-input UI/ARIA snapshot and API spy calls. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC01-03 — Login tài khoản PENDING_VERIFICATION quay về bước OTP

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-01; screen DD §§2–3, BR-SC-02, SH-02; F1 API #2 error `IDT-113` |
| Kind / priority | Abnormal, P0 — avoids creating a false authenticated session |
| Environment | FE mock and API integration |
| Preconditions | `pending-A` exists with `PENDING_VERIFICATION`, no active session. |
| Test data | Valid credentials for `pending-A`. |
| Steps | Submit login; inspect route/state/token/cookie and user guidance. |
| Expected | F1 returns `403 IDT-113`; UI enters email verification flow and offers email verification guidance; no access token, refresh cookie, authenticated navigation or session state. |
| Cleanup | Reset pending account and browser auth state. |
| Evidence | Response, UI state, token/cookie/storage snapshot, session DB query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC02-01 — Login và OAuth thành công áp dụng redirect nội bộ có quyền

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-02; screen DD BR-SC-01/05, SH-04; F1 API #2, #5–6 |
| Kind / priority | Normal, P0 — role-aware authentication navigation |
| Environment | E2E with F1 and OAuth provider stub; FE mock separately for deterministic route-state assertions |
| Preconditions | Active `student-A` and `teacher-A`; OAuth provider returns verified identity; each has a valid same-role return path. |
| Test data | Student redirect `/problems`; teacher redirect `/instructor/problems`; provider states minted by F1 #5, callback code controlled by stub. |
| Steps | Login each account with allowed returnTo; run OAuth start→provider→callback with valid state for each role; inspect final URL and cookies. |
| Expected | Credentials/OAuth set access token in memory and refresh cookie HTTP-only; redirect to authorized internal path. If no valid returnTo, student defaults `/dashboard`, instructor `/instructor/overview`. Overlay only follows successful authentication. |
| Cleanup | Revoke test refresh families; clear browser storage/cookies and OAuth stub state. |
| Evidence | Navigation trace; provider redirect/callback requests; cookie flags; token memory state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC02-02 — Bỏ qua redirect không an toàn hoặc sai vai trò

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-02; screen DD BR-SC-01, SH-05 |
| Kind / priority | Security, P0 — open redirect and cross-role prevention |
| Environment | FE mock: Vitest; E2E route validation |
| Preconditions | Auth form with student and instructor identities. |
| Test data | redirect values `//evil.example`, `/\\evil.example`, `https://evil.example`, `/admin/users` for student, `/instructor/users` for student. |
| Steps | Complete valid login/OAuth with each returnTo value; inspect resulting URL and any external navigation. |
| Expected | Unsafe/non-internal and unauthorized role targets are discarded; route falls back to role default. No navigation to external host or forbidden area. |
| Cleanup | Clear browser session/cookies. |
| Evidence | Input URL; final `window.location`; navigation log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC02-03 — OAuth callback sai state hoặc lỗi không giữ phiên giả

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-02; screen DD §§3–4 BR-SC-05, SH-09; F1 API #5–6 (`IDT-109`, `IDT-110`) |
| Kind / priority | Security, P1 — callback CSRF/state validation |
| Environment | E2E/API integration with OAuth stub |
| Preconditions | Callback route loaded from a real F1 start but receives altered state; separate case receives provider-unverified email. |
| Test data | Valid code + mismatching state; valid state + provider `email_verified=false`. |
| Steps | Submit each callback scenario; inspect callback route, auth state, cookies, address bar and logs. |
| Expected | F1 rejects mismatch `401 IDT-109` and unverified identity `422 IDT-110`; UI returns to form with one callback-level toast, no auth session/cookie; removes code/state from address bar and does not log them. |
| Cleanup | Clear OAuth state, cookies, logs and browser auth state. |
| Evidence | Callback responses; URL after callback; cookie/token snapshot; sanitized logs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC02-04 — Refresh đồng thời chỉ chạy một lần; refresh lỗi xoá phiên

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-02; screen DD §§1,4 BR-SC-05; F1 API #3, refresh reuse `IDT-105/106` |
| Kind / priority | Concurrency/Recovery, P1 — shared auth-provider behavior |
| Environment | FE mock tests plus E2E with F1 refresh endpoint |
| Preconditions | Access token expired; multiple parallel app requests receive 401; valid refresh cookie for success phase, revoked cookie for failure phase. |
| Test data | Three parallel protected requests; one valid refresh cookie then one revoked refresh family. |
| Steps | Trigger parallel 401s; observe refresh/replay counts; repeat with revoked cookie. |
| Expected | First burst causes one #3 refresh and each original request is replayed at most once. On refresh failure, access token cleared, user routed to `/login` with internal returnTo; no false authenticated state. |
| Cleanup | Revoke test token family; clear browser cookies and auth memory. |
| Evidence | Network trace with counts; router state; cookies/access-token memory before/after. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC03-01 — Forgot password không tiết lộ email có tài khoản hay không

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-03; screen DD §§2–3 BR-SC-03, SH-06; F1 API #7 |
| Kind / priority | Security, P0 — account enumeration protection |
| Environment | FE mock and API integration with mail sink |
| Preconditions | `a@prep.test` is active; `nobody@prep.test` does not exist. |
| Test data | Both valid-format addresses; identical password recovery screen state. |
| Steps | Submit forgot request for each address; compare UI message/state and inspect API/mail sink. |
| Expected | Both requests return #7's generic 200 response; UI shows same translated message and enters OTP step. No UI/network detail reveals account existence. Mail send may occur only for existing address per backend contract. |
| Cleanup | Clear OTP/mail records and form state. |
| Evidence | Both UI snapshots/responses; network payloads; test mail sink. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC03-02 — OTP sai/hết hạn không mở bước reset; gửi lại theo cooldown

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-03; screen DD §§3, BR-SC-03, SH-07; F1 API #60, #7 and validation |
| Kind / priority | Abnormal/Boundary, P0 — OTP gate and replay protection |
| Environment | FE mock and F1 integration with controllable clock/mail sink |
| Preconditions | Existing account has one OTP issued; OTP TTL 10 minutes, wrong-attempt limit 5, resend cooldown 60 seconds per F1 contract. |
| Test data | Wrong OTP `000000`, correct OTP captured by mail sink, expired fixture at 10m+1s, resend at 59s then 60s. |
| Steps | Enter wrong code; enter expired code in isolated reset; attempt resend before and at cooldown boundary; enter correct code via #60. |
| Expected | Wrong/expired/over-limit code remains forgot_otp with generic error and no reset form; #60 neither consumes nor extends OTP. Before 60s UI resend is disabled; a forged #7 resend still returns generic 200 with no mail, OTP replacement or TTL extension. At 60s authorized resend follows F1 limits. Correct verification alone opens reset; no auth session. |
| Cleanup | Delete OTP/reset tokens; restore fake clock and account state. |
| Evidence | Screen state, API responses/error codes, timestamps, mail sink events; no OTP in URL/storage/log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC03-03 — Reset kiểm OTP lần cuối rồi về login, không tự đăng nhập

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-03; screen DD BR-SC-03, SH-08; F1 API #8 |
| Kind / priority | Normal/Recovery, P1 |
| Environment | E2E with F1 and mail sink |
| Preconditions | Active password account requested recovery; valid OTP still within original 10-minute TTL; OTP previously verified by #60. |
| Test data | `newPassword=NewPrepPass2026!`, confirmation identical; OTP from mail sink. |
| Steps | Submit matching password and OTP to reset; inspect route, form, tokens/cookies; attempt new login. |
| Expected | #8 success consumes OTP; screen clears OTP memory, returns to login with email retained; no access token or refresh cookie/session is created. New password login succeeds; old password no longer authenticates. |
| Cleanup | Restore test password/account; clear refresh families and OTP data. |
| Evidence | Reset/login responses; route and browser token state; OTP consumed-state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC04-01 — Restore ticket khôi phục SELF account đúng một lần rồi yêu cầu login

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-04; screen DD §§2,4 BR-SC-04, SH-10; F1 API #2/#6 and #61; `DEC-2026-1025-dd-auth-recovery-contracts` |
| Kind / priority | Recovery/Security, P0 — account recovery and one-use credential |
| Environment | E2E with F1, Redis and PostgreSQL; FE mock for banner lifecycle |
| Preconditions | `self-deactivated-A` is SELF-deactivated 2 days ago, not anonymized; valid password; restore ticket TTL 300 sec. |
| Test data | Correct credentials; opaque ticket returned only in `IDT-104.details.restoreTicket`. |
| Steps | Login; verify banner appears while no session token is set; click restore once; inspect account; retry same ticket; then login normally. |
| Expected | Ticket is not in URL/storage/log; #61 first call returns restored true and account becomes ACTIVE; UI removes ticket/banner and asks for login; no access/refresh token is emitted. Reuse returns `401 IDT-114`; fresh login succeeds. |
| Cleanup | Restore original deactivated status or remove account; clear Redis ticket/cookie/session. |
| Evidence | Login/restore responses; account status audit; ticket storage scan; cookie/token state; second-use response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC04-02 — Ticket sai hoặc hết hạn bị xoá khỏi màn, cần xác minh mới

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-04; screen DD BR-SC-04; F1 API #61, `IDT-114` |
| Kind / priority | Abnormal/Boundary, P1 |
| Environment | FE mock and integration with fixed clock |
| Preconditions | Banner exists after valid credential verification; ticket separately altered or clock advanced beyond 300 sec. |
| Test data | Altered opaque ticket; expired ticket at 301 seconds. |
| Steps | Submit each ticket through restore CTA; inspect banner, stored data, and calls. |
| Expected | #61 returns generic `401 IDT-114`; UI removes banner and ticket and requires fresh login verification; it does not auto-replay retained password or issue another restore call. |
| Cleanup | Clear ticket/account/browser state. |
| Evidence | API response; banner/state assertions; storage and URL scan. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC04-03 — Tài khoản Admin khóa không nhận vé tự khôi phục

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-04; screen DD BR-SC-04; F1 API #2/#6 deactivation rules |
| Kind / priority | Security, P1 — no self-restore for admin-imposed lock |
| Environment | API integration/E2E |
| Preconditions | `admin-locked-A` is deactivated by Admin, not SELF; valid credentials. |
| Test data | Valid login credentials |
| Steps | Login, inspect error details and restore banner; verify no #61 ticket is available. |
| Expected | Login is denied according to F1 account status but `restoreTicket` is absent; screen does not show self-restore banner or call #61. No session token/cookie is created. |
| Cleanup | Restore admin fixture state and clear auth data. |
| Evidence | F1 response; UI state; #61 call count; token/cookie snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0101-AUTHAC03-04 - Retry OTP verification after the successful response is delayed

| Field | Value |
| :--- | :--- |
| AC / DD source | AUTH-AC-03; screen DD section 3, BR-SC-03, SH-07/08; F1 API #60 and `DEC-2026-1025-dd-auth-recovery-contracts` |
| Kind / priority | Recovery, Concurrency; P1 - prevents a lost response from stranding a valid recovery flow |
| Environment | Browser + F1 integration, mail sink, controllable network proxy and clock |
| Preconditions | `a@prep.test` has a live recovery OTP with at least 2 minutes remaining; screen is `forgot_otp`; no reset token or authenticated session exists. |
| Test data | Correct six-digit OTP captured by the mail sink; proxy lets #60 complete but delays/drops its first response; retry sends identical email and OTP. |
| Steps | Submit the valid OTP; allow F1 to return `200 {verified:true}` while the browser times out; inspect screen and state; retry with the same OTP before its original expiry; inspect #60, screen and session state. |
| Expected | After the timeout, the screen remains at `forgot_otp`, retains email/code for retry, and exposes a retryable error without opening `forgot_reset`. Retry succeeds because #60 validates without consuming or extending the OTP; only then does the screen advance. Original OTP expiry is unchanged. No password-reset write, access token, refresh cookie, or session is created by #60. |
| Cleanup | Delete OTP/reset state; restore clock/proxy; clear form, cookies and test account state. |
| Evidence | Proxy timeline, both #60 requests/responses, OTP TTL before/after, rendered step, token/cookie/session snapshots. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
