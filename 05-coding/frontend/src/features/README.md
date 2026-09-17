# features/

Each slice is **one user action**, named **verb-object**: `run-code`,
`submit-solution`, `request-solution-review`, `conduct-mock-interview`,
`answer-interview-question`, `auth-by-credentials`, `author-problem`.

Do not name by generic noun (`auth`, `problem`) — a generic noun sucks everything related into
one slice and balloons into the very monolith FSD exists to avoid.

No feature exists yet at this base stage — build one once `03-dd/api/<module>.md` exists for
that action.
Source: `01-rd/system/frontend_architecture.md` section 2.
