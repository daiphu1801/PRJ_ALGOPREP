# DD màn hình — Trang đầu Workspace (`USR0105`)

> Hợp đồng giao diện theo `03-dd/screens/README.md` mục 4. API chi tiết chỉ nằm trong DD module.

## 0. Thông tin tài liệu

**Bảng 0.1: Bìa.**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0105`, `workspace_home` |
| Tên màn | Trang đầu Workspace |
| Actor | A1 `STUDENT` |
| Route | `/workspace` |
| Nguồn | `01-rd/screens/users/USR0105_workspace_home.md`; `02-bd/screens/users/USR0105_workspace_home.md` |
| Prototype | `05-coding/frontend/src/views/users/workspace-home/` (mock) |
| Module | F4 `judge-orchestration`; F2 `problem-bank` cho bài ngẫu nhiên |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| TDD | `04-tdd/modules/F4_judge-orchestration.md`; không có TDD riêng cho màn |

**Bảng 0.2: Lịch sử sửa.**

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo DD màn từ BD, F4/F2 và prototype hiện có | 2026-10-24 |

**Bảng 0.3: Ký hiệu nguồn trong tài liệu.**

| Ký hiệu | Nguồn |
| :--- | :--- |
| RD, BD | `01-rd/screens/users/USR0105_workspace_home.md`, `02-bd/screens/users/USR0105_workspace_home.md` |
| F2, F4 | `03-dd/api/F2_problem-bank.md`, `03-dd/api/F4_judge-orchestration.md` |
| V | `05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

**Bảng 1.1: Slice của màn.**

| Lớp | Đường dẫn từ `src/` | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/workspace/page.tsx` | Render trang đầu workspace | Có |
| views | `views/users/workspace-home/ui/workspace-home-view.tsx` | Nhận query state, hiển thị đầu trang và truyền danh sách vào `InProgressList` | Có, đang đọc mock `useProblemListPage` [SoT: V:25-33] |
| entities | `entities/submission/api/queries.ts` | `useMyUnsolvedRecentProblems()` theo F4 mục 11 (danh mục #8) | Chưa có query theo contract |
| entities | `entities/problem/api/queries.ts` | `GetRandomProblem` theo F2 mục 3 | Có mock; phải dùng mutation/query tới server khi nối thật |
| entities | `entities/problem/ui/in-progress-list.tsx` | Nhận `items`, `limit=12`, `layout="grid"`, `emptyText`; dùng chung với `USR0101` | Có [SoT: BD:263-267] |
| shared | `Card`, `Button`, `Skeleton`, `EmptyState`, `ErrorState`, `useT` | Thành phần chung | Tái sử dụng |

Không có feature hoặc widget mới: sự kiện của màn chỉ đọc và điều hướng. `QueryBoundary` của `shared/ui` xử lý khung chờ/lỗi mặc định; view cung cấp nội dung rỗng riêng. Chiều import: `app → views → widgets → features → entities → shared`.

## 2. API tiêu thụ

**Bảng 2.1: Ánh xạ sự kiện sang API.**

| EVT (BD) | ID hợp đồng | Hook/query đích | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 khởi tạo | F4 mục 11 `ListMyUnsolvedRecentProblems` | `useMyUnsolvedRecentProblems({limit:12})` | Mock; F4 mặc định 3 nên màn phải truyền `limit=12` [SoT: F4:645-651; BD:258-267] |
| EVT-2 chọn bài trong danh sách | Không gọi API | Link `/problems/{problemId}` | Client |
| EVT-3 bài ngẫu nhiên | F2 mục 3 `GetRandomProblem` | `getRandomProblem()` | Mock; thành công điều hướng tới overview |
| EVT-4 mở bài đang làm dở | Không gọi API | Link `/workspace/{problemId}` | Client |

Không có realtime. “Đang làm dở” theo F4: bài có lượt nộp gần nhất chưa `ACCEPTED`; không đồng nghĩa với bản nháp mã trong trình duyệt. Không có dữ liệu thì rỗng hợp lệ.

## 3. Trạng thái màn

**Bảng 3.1: Trạng thái tải và thao tác.**

| Vùng | Trạng thái | Hiển thị/xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Danh sách | Loading | `QueryBoundary`/Skeleton; không dựng dữ liệu cũ khi tài khoản đổi | Khoá loading chung của `QueryBoundary` |
| Danh sách | Rỗng | Lời nhắc tới `/problems`, nút xem danh sách | `workspaceHome.empty` |
| Danh sách | Lỗi | Thông báo chung theo `error.code`, nút thử lại | `apiError.*` |
| Mỗi bài | F4 trả `problem=null` do F2 lỗi | Bỏ thẻ theo hợp đồng F4; không dựng liên kết thiếu tên bài | Không áp dụng [SoT: F4:651] |
| Bài ngẫu nhiên | Đang tải | Khoá nút; tránh gửi nhiều yêu cầu | `workspaceHome.random.*` |
| Bài ngẫu nhiên | Không có bài khả dụng/lỗi | Giữ trang, thông báo và cho thử lại | `apiError.*` |

Không đọc `message` từ API; ánh xạ `error.code` sang khoá i18n (`03-dd/api/api.md` mục 1.1).

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Thứ tự và giới hạn danh sách.** Kích hoạt khi vào `/workspace`; tiền điều kiện là phiên `STUDENT` hợp lệ. Gửi `limit=12` cho F4 mục 11, giữ nguyên thứ tự `submittedAt` giảm dần từ máy chủ và truyền mảng vào `InProgressList`. Hậu điều kiện: tối đa 12 thẻ, không tự tính lại “đang làm dở” từ mock hoặc bản nháp trình duyệt.

**BR-SC-02 — Bài ngẫu nhiên.** Kích hoạt khi bấm nút; tiền điều kiện là không có yêu cầu random đang chờ. Gọi F2 mục 3 để chọn ở server, chỉ điều hướng sau khi nhận `problemId` hợp lệ. Hậu điều kiện: mở `/problems/{problemId}`; tập rỗng báo toast theo BD, lỗi không làm mất danh sách.

**BR-SC-03 — Hai ý định điều hướng.** Kích hoạt qua EVT-2 hoặc EVT-4. EVT-2 mở danh mục `/problems`; EVT-4 mở lại trình soạn mã `/workspace/{problemId}` của đúng thẻ. Hậu điều kiện: màn này không ghi nháp; nháp của từng bài thuộc `USR0102` [SoT: BD Sheet 8 EVT-2, EVT-4].

## 5. Kiểm tra phía client

Không có form; không có schema zod cho đầu vào người dùng. `limit=12` là hằng số màn thuộc miền hợp lệ 1..20 của `03-dd/api/F4_judge-orchestration.md` mục 11 và `03-dd/validation/F4_judge-orchestration.md`; không kiểm lại định nghĩa “đang làm dở” ở client. `problemId` trong response là UUID theo API, view chỉ xây href từ DTO đã được tầng API parse.

## 6. i18n và truy cập

Tiền tố `workspaceHome.` và `apiError.`. Bài trong danh sách là liên kết có tên gồm mã/tên bài; nút bài ngẫu nhiên nêu rõ hành động. Trạng thái tải/lỗi dùng vùng thông báo phù hợp; focus không tự nhảy khi danh sách nạp xong. Các nhãn trạng thái không chỉ biểu đạt bằng màu.

## 7. Điểm kiểm thử

**Bảng 7.1: Điểm kiểm thử và tiêu chí.**

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 danh sách hiển thị tối đa 12 bản ghi theo thứ tự response | Vitest | F4 AC-38 |
| SC-02 | Boundary | EVT-1 danh sách rỗng có empty state | Vitest | `workspace_home` AC-SC-02 |
| SC-03 | Abnormal | EVT-1 lỗi query hiển thị retry; retry refetch | Vitest | `workspace_home` AC-SC-02 |
| SC-04 | Normal | EVT-2 mở danh mục, EVT-4 mở workspace của thẻ | Playwright | `workspace_home` AC-SC-01 |
| SC-05 | Normal | EVT-3 gọi F2 và điều hướng tới overview đúng id | Vitest | F2 AC-05 |
| SC-06 | Abnormal | EVT-3 lỗi không điều hướng và không làm mất danh sách | Vitest | F2 AC-05 |

## 8. Nợ prototype và câu hỏi mở

Prototype đang dùng `useProblemListPage` để lấy cả danh sách và random; F4 mục 11 và F2 mục 3 mới là nguồn thật. Nợ màn nằm ở `06-plan/PROTOTYPE_DEBT.md` mục 9 và ghi chú 2026-10-21 khoảng dòng 2095; chỉ gỡ khi code đã thay mock, có loading/lỗi và điều hướng đúng. DD này đóng phần thiết kế, chưa đóng nợ code.

Không có câu hỏi nghiệp vụ mở mới. Hợp đồng đã có tại F4 mục 11 và F2 mục 3: `GetRandomProblem` trả `problemId` UUID hoặc null; route overview dùng UUID nên không cần slug.
