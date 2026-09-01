# AlgoPrep — Frontend

Next.js 16.x (App Router) + TypeScript strict + Tailwind CSS v4, kiến trúc Feature-Sliced Design
(FSD). Spec đầy đủ: `01-rd/system/frontend_architecture.md`. Cây thư mục monorepo:
`01-rd/system/codebase_structure.md`.

## Trạng thái: khung base (2026-08-25)

Scaffold — cây thư mục FSD 6 tầng, 29 slice `views/<slug>/` khớp `01-rd/screens/` (25 gốc + 3
màn dùng chung + admin_overview phát sinh từ đợt đối chiếu prototype khu Admin, 2026-08-25), route theo actor,
provider (theme/i18n/query), bộ công cụ chất lượng. **Chưa có nội dung nghiệp vụ thật**:
`02-bd/screens/` và `03-dd/api/` còn trống nên mọi `views/*` là khung rỗng chờ BD/DD.

Bốn quyết định kiến trúc nền của khung base đã ghi thành
**`DEC-2026-0825-frontend-base-architecture`**; ba màn dùng chung A2+A3 ghi thành
**`DEC-2026-0825-shared-content-authoring-screens`** — cả hai trong
`.nexa/control/decision-registry.md` (P1 theo `sot-precedence.md`), đọc ở đó, không phải ở file
này.

## Định tuyến

Route group tách layout theo actor; instructor và admin có **prefix URL thật** để
`middleware.ts` guard được theo `/admin/:path*` và `/instructor/:path*`.

| Khu vực | Route group | URL |
| :--- | :--- | :--- |
| Công khai | `app/(public)/` | `/login` · `/register` |
| Người học | `app/(student)/` | `/problems` · `/submissions` · `/progress` · `/saved` · `/interview-bank` · `/profile` · `/settings` |
| Giảng viên | `app/(instructor)/instructor/` | `/instructor/classes` · `/instructor/grading` · `/instructor/overview` · `/instructor/problems[/[id]]` · `/instructor/interview-questions` |
| Quản trị | `app/(admin)/admin/` | `/admin/overview` · `/admin/users` · `/admin/queue` · `/admin/ai-config` · `/admin/problems[/[id]]` · `/admin/interview-questions` · ... |

Role bắt buộc theo khu vực: `entities/user/model/area.ts` → `ROLE_BY_AREA`. **`middleware.ts`
phải đọc map đó**, không viết lại — hai chỗ lệch nhau là một khu vực hở quyền không thấy trong
diff.

**Ba route dùng chung** (`problems`, `problems/[id]`, `interview-questions`) xuất hiện ở CẢ HAI
khu vực, cùng render một view (`ProblemManagementView`, `ProblemAuthoringView`,
`InterviewQuestionManagementView`) — theo `DEC-2026-0825-shared-content-authoring-screens`. Đây
KHÔNG phải trùng lặp code: `ROLE_BY_AREA` chỉ gác được "khu vực nào cần role gì", còn "A2 chỉ thấy
bài của mình" là phạm vi dữ liệu, việc của tầng API/backend theo ma trận phân quyền
(`PROBLEM_AUTHORING`/`INTERVIEW_BANK_MANAGEMENT`, `01-rd/req/identity.md` — F1-12), chưa hiện thực ở base.

### Ánh xạ slug tài liệu ↔ URL

Slug trong `01-rd/screens/` là `snake_case`, slice FSD là `kebab-case`, URL ngắn hơn cả hai ở vài
màn. Bảng này giữ chuỗi tra ngược `01-rd → 02-bd → 03-dd → code` (luật chống trôi,
`codebase_structure.md` mục 3):

| Slug tài liệu | Slice `views/` | URL |
| :--- | :--- | :--- |
| `problem_list` | `problem-list` | `/problems` |
| `my_progress` | `my-progress` | `/progress` |
| `my_submissions` | `my-submissions` | `/submissions` |
| `saved_problems` | `saved-problems` | `/saved` |
| `interview_bank_list` | `interview-bank-list` | `/interview-bank` |
| `admin_user_management` | `admin-user-management` | `/admin/users` |
| `admin_queue_monitor` | `admin-queue-monitor` | `/admin/queue` |
| `problem_management` | `problem-management` | `/admin/problems`, `/instructor/problems` |
| `problem_authoring` | `problem-authoring` | `/admin/problems/[problemId]`, `/instructor/problems/[problemId]` |
| `interview_question_management` | `interview-question-management` | `/admin/interview-questions`, `/instructor/interview-questions` |

Các màn còn lại: URL trùng tên slice. `problem_management`, `problem_authoring`,
`interview_question_management` đều thuộc `01-rd/screens/shared/` (mục 7.0 của
`system_survey.md`), không thuộc `admin/` hay `teacher/` dù trước 2026-08-25 từng mang tiền tố
`admin_`.

## Màn `auth`

Một view duy nhất (`views/auth`), chuyển chế độ tại chỗ không rời URL theo
`01-rd/screens/shared/auth.md` mục 3 điểm 3. `/login` và `/register` là hai route chỉ để
deep-link, cùng render `AuthView` với `initialMode` khác nhau. `AuthMode` khai đủ 5 chế độ
(`login` · `signup` · `forgot_email` · `forgot_otp` · `forgot_reset` — F1-17); ba chế độ
`forgot_*` chưa dựng UI.

Route `/` redirect về `/login`. Nhánh "đã đăng nhập → đích theo vai trò"
(`HOME_PATH_BY_ROLE`, chốt tại `auth.md:90`) chưa hiện thực vì chưa có session thật.

## Lệnh

```bash
pnpm install                      # ở 05-coding/
pnpm --filter frontend dev
pnpm check                        # typecheck + lint + vitest — PHẢI PASS trước khi báo xong (CLAUDE.md G-CHECK)
pnpm --filter frontend e2e        # Playwright
pnpm --filter frontend build
```

**Lưu ý về `pnpm check`:** `tsconfig.json` include `.next/types/**`, nên sau khi đổi cấu trúc
route mà `.next` còn cũ thì `tsc` báo lỗi module không tồn tại cho route cũ. Xoá `.next` rồi
`pnpm build` lại là hết.

## Điều ESLint chặn thật (không phải quy ước)

| Luật | Cơ chế |
| :--- | :--- |
| Chiều import FSD một hướng | `boundaries/element-types` — đã kiểm bằng 3 chiều sai, đều đỏ |
| Public API Rule (không deep import slice) | `no-restricted-imports` patterns — `@/entities/user/model/types` đỏ, `@/entities/user` xanh |
| Không import `next-intl` ngoài `shared/i18n` | `no-restricted-imports` paths |
| Không import `dompurify` trực tiếp | `no-restricted-imports` paths |
| Key dịch phải có ở cả `vi.json` và `en.json` | `shared/i18n/messages.test.ts` |

`shared/` được miễn luật Public API vì nó chia theo segment kỹ thuật, không có slice —
`shared/i18n/server` là entry point hợp lệ.

## Còn thiếu (không dựng ở bước base, có chủ đích)

- **`middleware.ts`** guard theo role — cần `03-dd/api/identity.md`. Đọc `ROLE_BY_AREA`.
- **Session bootstrap** — `app/providers/auth-provider.tsx` còn TODO; access token in-memory sẽ
  mất sau reload cho tới khi có nó.
- **`shared/realtime`** (STOMP/SSE) — dựng cùng `features/submit-solution` và
  `features/conduct-mock-interview`.
- **`sanitizeHtml` CỐ TÌNH NÉM LỖI.** Thư viện sanitizer chưa chọn. Hàm giả (regex strip
  `<script>`) nguy hiểm hơn không có vì XSS vẫn đi qua `onerror`, `javascript:`, SVG — nên nó
  throw để ai dùng là thấy ngay lúc dev. Thay bằng bản thật trước khi render nội dung người dùng.
- **Thư viện Markdown + LaTeX, biểu đồ** — chưa chọn (`frontend_architecture.md` mục 1).
- **E2E xương sống** — `e2e/smoke.spec.ts` chỉ là smoke (redirect, AppShell, đổi locale). Luồng
  thật ở `environment.md` mục 3.B cần backend.

## Nợ kỹ thuật đã biết

| # | Nợ | Gỡ khi |
| :-: | :--- | :--- |
| 1 | ~~`HOME_PATH_BY_ROLE.ADMIN` trỏ tạm `/admin/queue`~~ — **đã gỡ 2026-08-25**, trỏ `/admin/overview` (`admin_overview` đã có RD) | Đã xong |
| 2 | 27/28 view là placeholder chỉ có tiêu đề + một dòng chờ BD | Có `02-bd/screens/<màn>.md` |
| 3 | `shared/api` chỉ có `get`/`post` | Khi DD cần `put`/`patch`/`delete` |
| 4 | Chưa có mapper/zod nào (Anti-Corruption Layer) | Khi có endpoint thật đầu tiên |
| 5 | Ba route dùng chung (`problems`, `problems/[id]`, `interview-questions`) chưa có cơ chế lọc phạm vi dữ liệu theo role (A2 chỉ thấy bài của mình) — chỉ mount đúng view ở đúng URL, chưa gọi API thật | Khi có `03-dd/api/problem-bank.md` + `interview-bank.md` và middleware đọc ma trận quyền |
