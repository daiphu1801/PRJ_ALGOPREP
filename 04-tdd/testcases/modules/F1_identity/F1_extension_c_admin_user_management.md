# F1 extension C — Admin user management refusals (task #11)

- **Scope/TDD:** F1 AC-79; `04-tdd/modules/F1_identity.md`.
- **DD:** `03-dd/api/F1_identity.md` #35–38, #50 (khối "Triển khai 2026-11-02"); `03-dd/validation/F1_identity.md` §14; `DEC-2026-1102-admin-user-management-contracts`.
- **Fixture catalog:** A1 ADMIN with user-management permission; U1 ACTIVE account with a password; U2 anonymized account; R_STUDENT and R_INSTRUCTOR system roles; mail sink that can be switched to fail; DB audit rows readable. Exact IDs captured per run.
- **Execution:** All cases `Chưa thực hiện`. Automated JUnit coverage exists (`AdminUserManagementFlowTest`, `AdminUserHandlersTest`, `AdminUserQueryHandlersTest`); these cases are the manual QA pass.

| AC | Branch | Case | Review |
|---|---|---|---|
| 79 | bad list filter or paging | TC-F1-AC79-01 | Chưa audit |
| 79 | refused role or status change, no side effect | TC-F1-AC79-02 | Chưa audit |
| 79 | temporary password for an anonymized account | TC-F1-AC79-03 | Chưa audit |
| 79 | mail failure leaves nothing changed; no-op writes no audit | TC-F1-AC79-04 | Chưa audit |

### TC-F1-AC79-01 — A malformed list request is refused with IDT-V17

| Field | Value |
|---|---|
| AC / DD source | AC-79; API #35 |
| Kind / priority | Validation/Boundary; P2 |
| Environment | API |
| Preconditions | A1 signed in. |
| Test data | `status=UNKNOWN`; `page=-1`; `pageSize=0`; `pageSize=101`. |
| Steps | GET #35 once per data row. |
| Expected | Every call returns `400 IDT-V17`; no stack trace or SQL text in the body. A call with no filters returns `200` with `pageSize` 20. |
| Cleanup | None. |
| Evidence | HTTP status and error body per call. |
| Design review | Chưa audit |

### TC-F1-AC79-02 — A refused role or status change changes nothing

| Field | Value |
|---|---|
| AC / DD source | AC-79; API #36, #37; validation §14 |
| Kind / priority | Negative/Data integrity; P1 — silent change or audit row for a refused call |
| Environment | API + DB + mail sink |
| Preconditions | A1 signed in; U1 ACTIVE with role R_STUDENT; audit row count and mail count noted. |
| Test data | Unknown `userId`; target role id that does not exist; status `FROZEN`; lock with a reason of 501 characters; lock with email notice on and no reason. |
| Steps | 1. PATCH #36 with the unknown user. 2. PATCH #36 with the missing role. 3. PATCH #37 with `FROZEN`. 4. PATCH #37 with the long reason. 5. PATCH #37 with email on and no reason. 6. Compare DB, audit and mail with the noted state. |
| Expected | `404 IDT-450`, `404 IDT-424`, `422 IDT-V26` (steps 3 to 5) respectively; U1 role and status unchanged, no audit rows, no email. Then PATCH #36 to the role U1 already has: `200` and still no audit row. |
| Cleanup | None needed; state unchanged. |
| Evidence | HTTP bodies, `users` row before and after, audit count, mail sink. |
| Design review | Chưa audit |

### TC-F1-AC79-03 — Temporary password for an anonymized account is refused

| Field | Value |
|---|---|
| AC / DD source | AC-79; API #38; `IDT-448` |
| Kind / priority | Negative; P1 |
| Environment | API + DB + mail sink |
| Preconditions | A1 signed in; U2 has `anonymized_at` set. |
| Test data | U2 id. |
| Steps | POST #38 for U2; inspect DB, audit and mail. |
| Expected | `422 IDT-448`; no password change, no session revoked, no audit row, no email. |
| Cleanup | None. |
| Evidence | HTTP body, DB row, mail sink. |
| Design review | Chưa audit |

### TC-F1-AC79-04 — A mail failure leaves the account as it was

| Field | Value |
|---|---|
| AC / DD source | AC-79; API #50, #38 |
| Kind / priority | Recovery/Security; P1 — account created or password changed with no way for the person to know it |
| Environment | API + DB + mail sink set to fail |
| Preconditions | A1 signed in; U1 can log in with password P_OLD; mail sink fails on send. |
| Test data | New account `new.user@example.com`; reset target U1. |
| Steps | 1. POST #50 for the new email. 2. POST #38 for U1. 3. Log in as U1 with P_OLD. 4. Search responses, application log and audit for the temporary password. |
| Expected | Step 1: no account row exists afterwards. Step 2: error response, P_OLD still works, refresh tokens of U1 still valid, no audit row. The temporary password appears nowhere in responses, logs or audit. |
| Cleanup | Restore the mail sink. |
| Evidence | HTTP bodies, `users` query, login result, log and audit grep. |
| Design review | Chưa audit |
