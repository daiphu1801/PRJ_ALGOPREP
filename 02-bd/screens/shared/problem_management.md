# BD — Màn `problem_management` (Quản lý bài tập — trục màn hình, dùng chung A2/A3)

> Trục màn hình. RD: `01-rd/screens/shared/problem_management.md`. Actor: A2 (Giảng viên) và A3 (Quản
> trị viên) — cùng một view, mount ở cả `/instructor/problems` và `/admin/problems`
> (`DEC-2026-0825-shared-content-authoring-screens`). Bounded Context chủ: `problem-bank` (F2) — đọc
> cùng `02-bd/architecture/problem-bank.md`, `02-bd/database/problem-bank.md`,
> `02-bd/security/problem-bank.md` trước khi dùng file này; không lặp lại nội dung ba file đó.
>
> **Anti-drift:** file này không định nghĩa request/response của bất kỳ API nào (đó là việc của
> `03-dd/api/problem-bank.md`, chưa viết), không định nghĩa bảng CSDL, không định nghĩa cơ chế phân
> quyền chi tiết (đã có ở `02-bd/security/problem-bank.md`) — chỉ liệt kê layout, component, trạng
> thái màn, tên API tiêu thụ, điều hướng, và quyền truy cập ở mức màn.

## 1. Bố cục màn (layout regions)

Đối chiếu `09-layoutBase/Admin - Quản lý bài tập.dc.html` (dựng trong shell Admin; khu Giảng viên mount
cùng view, chưa có prototype riêng — `06-plan/PROTOTYPE_DEBT.md` mục 7.3.a
[SoT: 01-rd/screens/shared/problem_management.md:11-12]). Tám vùng, trên xuống dưới:

1. **Thanh điều hướng dọc (sidebar)** — không thuộc phạm vi màn này, dùng chung shell Admin/Giảng
   viên; mục nav "Quản lý bài tập" ở nhóm "Nội dung" active khi ở route này
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:348-349].
2. **Thanh tiêu đề (header bar)** — tên màn "Quản lý bài tập", dòng phụ đếm tổng số bài và số bài đang
   hiển thị công khai, nút "Bài tập mới" (điều hướng, không phải form tại chỗ)
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:150-160]. Nút "Nhập CSV" của prototype **bị cắt
   khỏi phạm vi** (`DEC-2026-0831-problem-management-lifecycle-details`, Q4 RD) — không dựng.
3. **Dải chỉ số tổng (4 thẻ thống kê)** — Tổng bài tập, Đã xuất bản, Chưa xuất bản, Tỉ lệ AC trung bình
   (90 ngày gần nhất) [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:162-173, 477-482]. Nhãn
   "Bản nháp" của prototype đổi thành "Chưa xuất bản" theo `F2-15`.
4. **Thanh lọc/tìm kiếm** — ô tìm theo mã bài hoặc tiêu đề, tab độ khó (Tất cả/Easy/Medium/Hard), tab
   trạng thái (Tất cả/Đã xuất bản/Chưa xuất bản — đúng 3 tab, không có tab "Đã ẩn"), bộ đếm kết quả
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:177-193].
5. **Thanh hành động theo lô** — chỉ hiện khi có dòng được chọn; 5 hành động cộng nút xoá, xem mục 3.
6. **Bảng danh sách bài toán** — 10 cột, phân trang.
7. **Hai khối phụ song song** — "Phân bố theo chủ đề" (thanh tỉ lệ) và "Bài cần chú ý" (4 quy tắc tự
   phát hiện) [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:247-283].
8. **Hộp thoại xác nhận xoá (ẩn mềm)** — dùng chung cho xoá một dòng và xoá theo lô
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:304-316].

## 2. Kho component (component inventory)

| Component | Mô tả | Ghi chú thiết kế |
| :--- | :--- | :--- |
| `PageHeader` | Tiêu đề + dòng phụ đếm + CTA "Bài tập mới" | CTA điều hướng sang `problem_authoring` chế độ tạo mới, không mở modal tại chỗ [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:159] |
| `StatCard` × 4 | Thẻ số liệu tổng | Giá trị tính từ API danh sách/thống kê (mục 4), không phải state tĩnh |
| `SearchInput` | Ô tìm theo mã/tiêu đề | Debounce phía client trước khi gọi API lọc — cụ thể hoá ở DD |
| `SegmentedTabs` (độ khó) | 4 lựa chọn, một chọn | Đổi tab reset về trang 1 [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:96 (RD)] |
| `SegmentedTabs` (trạng thái) | 3 lựa chọn: Tất cả/Đã xuất bản/Chưa xuất bản | Không có lựa chọn thứ tư (`F2-15` chỉ hai trạng thái) |
| `ResultCounter` | "`n` / tổng bài" | |
| `BulkActionBar` | Hiện có điều kiện khi `selectedCount > 0` | 5 nút hành động + 1 nút xoá, xem mục 3 |
| `ProblemTable` | Bảng 10 cột, sort 7 cột | Cột: chọn, Mã, Tiêu đề (liên kết), Chủ đề, Độ khó, Trạng thái, Lượt nộp, AC, "TC · Sửa cuối", nhóm nút Sửa/Xoá [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:208-229] |
| `RowCheckbox` | Chọn dòng, giữ nguyên qua đổi trang/lọc | Xem trạng thái S5 mục 3 dưới — bổ sung so với prototype |
| `DifficultyBadge` | Nhãn màu theo Easy/Medium/Hard | Không định màu cụ thể ở đây (không có design-token SoT theo `.claude/skills/bd-generation/SKILL.md` Layer 3) — chỉ mô tả có 3 trạng thái badge khác nhau về màu |
| `StatusDot` | Chấm màu + nhãn trạng thái xuất bản | 2 giá trị: `Đã xuất bản` / `Chưa xuất bản` |
| `Pagination` | Trước/Sau + số trang + nhãn "Trang x/y" | 8 dòng/trang theo prototype [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:236-245] — số dòng/trang cụ thể hoá lại ở DD nếu cần cấu hình |
| `TopicDistributionPanel` | Thanh tỉ lệ theo chủ đề | Số liệu suy ra từ chính tập dữ liệu đang hiển thị (RD đã cảnh báo dữ liệu mẫu prototype tự mâu thuẫn — không lặp lại số mẫu sai đó) [SoT: 01-rd/screens/shared/problem_management.md:114-118] |
| `AttentionPanel` | 4 dòng cảnh báo tự phát hiện | Chưa có testcase / AC < 30% / Chưa xuất bản (đang ẩn khỏi người học) / Chưa xuất bản quá 7 ngày — ngưỡng chốt ở `DEC-2026-0831-problem-management-lifecycle-details` |
| `ConfirmDeleteDialog` | Xác nhận xoá (ẩn mềm) | Dùng chung 1 dòng / nhiều dòng; nội dung phải liệt kê rõ số lượt nộp bị ảnh hưởng (xem GWT bổ sung ở RD) [SoT: 01-rd/screens/shared/problem_management.md:154-156] |

## 3. Trạng thái màn (screen states)

| Mã | Trạng thái | Điều kiện vào | Hành vi |
| :-: | :--- | :--- | :--- |
| S1 | Mặc định (tab "Tất cả") | Mở màn lần đầu hoặc reset bộ lọc | Bảng hiển thị cả `Đã xuất bản` và `Chưa xuất bản`, phạm vi dữ liệu theo actor (mục 5) |
| S2 | Đang lọc/tìm kiếm | Người dùng nhập từ khoá hoặc đổi tab độ khó/trạng thái | Reset về trang 1, bộ đếm kết quả cập nhật [SoT: 01-rd/screens/shared/problem_management.md:95-96] |
| S3 | Rỗng (không có kết quả) | Bộ lọc/tìm kiếm không khớp bài nào | Bảng hiện thông báo rỗng, ẩn phân trang; hai khối phụ (mục 1.7) vẫn tính trên toàn bộ phạm vi dữ liệu của actor, không theo bộ lọc đang áp — vì hai khối đó là "sức khoẻ kho", không phải "kết quả tìm kiếm" `[SoT: Suy luận]` — RD không nói rõ, suy luận từ mục đích khối (mục 1 RD dòng 111-124) |
| S4 | Đã chọn ≥ 1 dòng | Người dùng tick `RowCheckbox` | Hiện `BulkActionBar`, nhãn "Đã chọn N bài" |
| S5 | Tập chọn có phần không hiển thị | Người dùng đổi trang/lọc sau khi đã chọn, khiến một số mã đã chọn không còn nằm trong kết quả hiện tại | Nhãn `BulkActionBar` phải nêu rõ "Đã chọn N bài (trong đó M bài không nằm trong kết quả hiện tại)" — bổ sung so với prototype, theo GWT của RD [SoT: 01-rd/screens/shared/problem_management.md:148-153] |
| S6 | Xác nhận xoá (một dòng) | Bấm nút Xoá ở một dòng | Mở `ConfirmDeleteDialog`, hiển thị mã + tiêu đề + số lượt nộp bị ảnh hưởng |
| S7 | Xác nhận xoá (theo lô) | Bấm "Xoá" trên `BulkActionBar` khi đang ở S4/S5 | Mở `ConfirmDeleteDialog`, liệt kê số bài đã chọn tường minh (đúng tập đã tick, không áp lên toàn bộ kết quả lọc) [SoT: 01-rd/screens/shared/problem_management.md:144-147] |
| S8 | Chặn xuất bản do thiếu testcase/spec | Người dùng cố đổi trạng thái một bài sang `Đã xuất bản` qua hành động lô "Xuất bản/ẩn" mà bài chưa đạt checklist (F2-15) | Hệ thống chặn cứng, nêu lý do cụ thể — không âm thầm bỏ qua bài đó (chốt 2026-08-30, xem RD mục 4) |
| S9 | Không có quyền xoá | Vai trò hiện tại thiếu `PROBLEM_AUTHORING:DELETE` | Hành động xoá bị chặn ở tầng ứng dụng (API trả lỗi 403), không chỉ ẩn nút trên UI [SoT: 01-rd/screens/shared/problem_management.md:157-159] |
| S10 | Phạm vi dữ liệu theo actor | A2 đăng nhập | Bảng và 2 thẻ thống kê phụ ("Phân bố theo chủ đề", "Bài cần chú ý") chỉ tính trên bài do chính A2 soạn (quyền tác giả); A3 thấy toàn bộ kho (mục 5) |

**Hành động theo lô (`BulkActionBar`, đối chiếu mục 1.5 RD):**

| Hành động | Có mã F2-nn | Ghi chú |
| :--- | :--- | :--- |
| Xuất bản / ẩn | F2-15 (dạng thao tác lô) | Có thể trúng S8 nếu một hoặc nhiều bài trong lô chưa đạt checklist — hành vi khi lô hỗn hợp (một phần đạt, một phần không) là câu hỏi mở, xem mục 8 |
| Đổi độ khó | F2-02 (dạng thao tác lô) | |
| Gán chủ đề | F2-02 (dạng thao tác lô) | |
| Nhân bản | F2-16 | Mỗi bài được chọn tạo một bản sao độc lập ở trạng thái `Chưa xuất bản` |
| Xuất CSV | F2-17 | Chỉ metadata bảng (không đề bài/đặc tả/testcase) |
| Xoá | F2-15 (ẩn mềm, `DEC-2026-0831-problem-management-lifecycle-details`) | Dẫn tới S7 |

## 4. API tiêu thụ (chỉ tên endpoint + Bounded Context sở hữu — KHÔNG viết request/response)

Toàn bộ endpoint dưới đây thuộc `03-dd/api/problem-bank.md` (chưa viết) — file này chỉ liệt kê để màn
biết cần gọi gì, không định nghĩa hợp đồng.

| Nhu cầu màn | Endpoint (tên nghiệp vụ, chưa cố định route) | BC sở hữu |
| :--- | :--- | :--- |
| Danh sách bài toán có phân trang/lọc/sắp xếp, phạm vi theo actor | Danh sách bài toán (quản trị) | `problem-bank` |
| 4 chỉ số tổng của kho | Thống kê tổng quan kho bài toán | `problem-bank` |
| Phân bố theo chủ đề | Thống kê phân bố theo chủ đề | `problem-bank` |
| Bài cần chú ý (4 quy tắc tự phát hiện) | Danh sách bài cần chú ý | `problem-bank` |
| Đổi trạng thái xuất bản (một bài / theo lô) | Xuất bản / rút xuống bài toán | `problem-bank` |
| Đổi độ khó theo lô | Cập nhật độ khó theo lô | `problem-bank` |
| Gán chủ đề theo lô | Gán chủ đề theo lô | `problem-bank` |
| Nhân bản bài toán (một bài / theo lô) | Nhân bản bài toán | `problem-bank` |
| Xuất CSV metadata (một bài / theo lô / toàn kho đã lọc) | Xuất CSV danh sách bài toán | `problem-bank` |
| Xoá (ẩn mềm, một bài / theo lô) | Xoá bài toán (ẩn mềm) | `problem-bank` |

`[SoT: Suy luận]` — tên endpoint và việc gộp/tách thao tác lô thành một hay nhiều API là quyết định
của DD; BD chỉ đảm bảo mỗi nhu cầu nghiệp vụ trên màn có đúng một điểm chạm API tương ứng.

## 5. Điều hướng (navigation)

- **Nút "Bài tập mới"** (thanh tiêu đề) → `problem_authoring` chế độ tạo mới (không có `problem_id`)
  [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:159; 01-rd/screens/shared/problem_management.md:20-25].
- **Tiêu đề bài toán trong bảng** → `problem_authoring` chế độ sửa, kèm `problem_id` của dòng đó
  [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:219].
- **Nút "Sửa" cuối mỗi dòng** → `problem_authoring` chế độ sửa, kèm `problem_id` — cùng đích với tiêu đề
  bài toán, không phải hai luồng khác nhau [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:227-228].
  Màn này **không lặp lại form soạn đề** — mọi chỉnh sửa nội dung/testcase đi qua `problem_authoring`
  (`01-rd/screens/shared/problem_management.md:34-38`).
- Route mount: `/admin/problems` (A3) và `/instructor/problems` (A2), cùng một view
  (`DEC-2026-0825-shared-content-authoring-screens`).
- **Không có** nút "chấm lại hàng loạt"/rejudge trên màn này — đã loại khỏi phạm vi toàn hệ thống
  (`DEC-2026-0828-remove-rejudge-scope`); không dựng lại dù prototype không có nút này để bắt đầu (không
  áp dụng, chỉ để xác nhận rõ ràng theo yêu cầu nhiệm vụ).

## 6. Quyền truy cập (access rights)

- **Actor:** A2 (`INSTRUCTOR`) và A3 (`ADMIN`) — không actor nào khác được mount route này.
- **Function gác màn:** `PROBLEM_AUTHORING` (đọc danh sách, sửa/xuất bản/nhân bản/xuất CSV) và
  `TESTCASE_MANAGEMENT` (đường dẫn tới quản lý testcase qua `problem_authoring`, không thao tác trực
  tiếp trên màn này) — theo ma trận Role × Function × Action của `identity`
  [SoT: 01-rd/req/identity.md — F1-10 → F1-12; 02-bd/security/problem-bank.md mục 1].
- **Action cụ thể theo hành động trên màn:**

  | Hành động màn | Function:Action yêu cầu |
  | :--- | :--- |
  | Xem danh sách, thống kê, khối "Bài cần chú ý" | `PROBLEM_AUTHORING:READ` |
  | Xuất bản/rút xuống, đổi độ khó, gán chủ đề | `PROBLEM_AUTHORING:UPDATE` |
  | Nhân bản | `PROBLEM_AUTHORING:CREATE` (tạo bản sao mới) |
  | Xuất CSV | `PROBLEM_AUTHORING:READ` [SoT: 02-bd/security/problem-bank.md mục 1] |
  | Xoá (ẩn mềm) | `PROBLEM_AUTHORING:DELETE` |

- **Phạm vi dữ liệu theo vai trò** (không phải Function/Action riêng, là quy tắc lọc dữ liệu ở tầng
  application): A2 chỉ thấy/sửa bài do chính mình soạn (quyền tác giả — không theo lớp phụ trách); A3
  thấy toàn bộ kho [SoT: 01-rd/screens/shared/problem_management.md:6, 165 (Q1 đã chốt),
  `DEC-2026-0825-shared-content-authoring-screens`].
- **Mọi kiểm quyền thực thi ở tầng ứng dụng (backend), không chỉ ẩn/hiện nút trên UI** — kể cả khi nút
  đã bị ẩn với vai trò không đủ quyền, API vẫn phải tự chặn nếu bị gọi trực tiếp (đúng nguyên tắc chung
  của dự án, lặp lại vì đây là màn có hành động phá huỷ dữ liệu)
  [SoT: 01-rd/screens/shared/problem_management.md:157-159; 02-bd/security/problem-bank.md mục 1].
- **Không có ngoại lệ nào cho bookmark** (F2-13) trên màn này — màn không hiển thị bookmark của học
  viên dưới bất kỳ hình thức nào, kể cả số tổng hợp ẩn danh [SoT: 02-bd/security/problem-bank.md mục 3].

## 7. Ngoài phạm vi file này

- Form soạn/sửa nội dung bài toán, tab Testcase, wizard xuất bản — `02-bd/screens/shared/problem_authoring.md`
  (chưa viết; RD tương ứng đã có ở `01-rd/screens/shared/problem_authoring.md`).
- Hợp đồng API đầy đủ (request/response, mã lỗi) — `03-dd/api/problem-bank.md` (chưa viết).
- Schema CSDL, chỉ mục, Redis key — `02-bd/database/problem-bank.md`.
- Cơ chế phân quyền chi tiết, chống rò rỉ Hidden testcase — `02-bd/security/problem-bank.md`.
- Rejudge/chấm lại hàng loạt — ngoài phạm vi toàn hệ thống (`DEC-2026-0828-remove-rejudge-scope`).

## 8. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| BD-Q1 | Khi thao tác lô "Xuất bản/ẩn" áp lên một tập hỗn hợp (một số bài đủ điều kiện xuất bản, một số chưa), hệ thống xử lý thế nào — chặn toàn bộ lô, hay xuất bản phần đạt và báo lỗi riêng phần không đạt? | RD chỉ mô tả GWT cho một bài đơn lẻ (mục 4 RD), không nói rõ hành vi khi trộn nhiều bài trong một lô. | Đề xuất: xử lý từng bài độc lập trong lô, trả về danh sách kết quả kèm lý do cho từng bài thất bại — nhất quán với nguyên tắc "chặn cứng kèm lý do" đã chốt cho từng bài, tránh việc một bài lỗi kéo cả lô không xuất bản được. | Chốt ở DD (`03-dd/api/problem-bank.md`) hoặc xin owner xác nhận trước. |
| BD-Q2 | Số dòng/trang (8 theo prototype) có cấu hình được hay cố định? | RD không đề cập, chỉ mô tả đúng số của prototype. | Đề xuất: cố định 8 dòng/trang cho MVP, không cần cấu hình — giữ đơn giản, không phải yêu cầu nào đòi hỏi tuỳ biến. | Chốt ở DD nếu phát sinh nhu cầu. |

## 9. Tham chiếu

- `01-rd/screens/shared/problem_management.md` — RD của màn.
- `01-rd/screens/shared/problem_authoring.md` — RD màn con (nơi nút sửa/tạo mới trỏ tới).
- `02-bd/architecture/problem-bank.md`, `02-bd/database/problem-bank.md`,
  `02-bd/security/problem-bank.md` — BD Bounded Context chủ.
- `01-rd/req/identity.md` — F1-10 → F1-12, ma trận phân quyền.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0830-problem-lifecycle-two-states`,
  `DEC-2026-0831-problem-management-lifecycle-details`.
- `09-layoutBase/Admin - Quản lý bài tập.dc.html` — prototype (SoT layout).
