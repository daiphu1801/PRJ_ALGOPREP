# features/

Mỗi slice là **một hành động của người dùng**, đặt tên **verb-object**: `run-code`,
`submit-solution`, `request-solution-review`, `conduct-mock-interview`,
`answer-interview-question`, `auth-by-credentials`, `author-problem`.

Không đặt tên bằng danh từ chung (`auth`, `problem`) — danh từ chung hút mọi thứ liên quan vào
một slice và phình thành chính cái monolith mà FSD tồn tại để tránh.

Chưa có feature nào ở bước base — dựng khi có `03-dd/api/<module>.md` cho hành động đó.
Nguồn: `01-rd/system/frontend_architecture.md` mục 2.
