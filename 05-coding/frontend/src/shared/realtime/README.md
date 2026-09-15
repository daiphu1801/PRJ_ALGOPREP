# shared/realtime

STOMP (WebSocket) client and SSE client — pure messaging infrastructure, no business knowledge.
Source: `01-rd/system/frontend_architecture.md` section 2, 2.B.

Not built yet at this base stage: no `03-dd/api/` defines the real topic/endpoint to wrap a
client around. Build it when working on `features/submit-solution` (WebSocket) or
`features/conduct-mock-interview` (SSE) — do not build ahead of time, to avoid guessing the
message shape and having to rewrite it.
