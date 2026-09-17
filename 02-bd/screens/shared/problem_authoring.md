# BD — Màn `problem_authoring` (Soạn đề bài)

> Trục: **màn hình** (không phải Bounded Context). Slug khớp `01-rd/screens/shared/problem_authoring.md`
> [SoT: 01-rd/screens/shared/problem_authoring.md:1]. Actor: **A2 và A3 dùng chung** — một view, mount ở
> cả `/instructor/problems/[id]` và `/admin/problems/[id]`
> [SoT: 01-rd/screens/shared/problem_authoring.md:5-8, `DEC-2026-0825-shared-content-authoring-screens`,
> `DEC-2026-0825-frontend-base-architecture`]. Bounded Context sở hữu dữ liệu/API: **`problem-bank`** (nội
> dung đề, testcase, F2) và **`harness`** (lược đồ kiểu dữ liệu đọc bởi tab "Đặc tả",
> `02-bd/architecture/harness.md` mục 4) [SoT: 01-rd/screens/shared/problem_authoring.md:3].
>
> Đọc cùng: `02-bd/architecture/problem-bank.md`, `02-bd/database/problem-bank.md`,
> `02-bd/storage/problem-bank.md`, `02-bd/architecture/harness.md` mục 4/8. File này **không lặp lại**
> nội dung bốn file trên — chỉ mô tả layout, state, và API nào màn hình gọi.
>
> Màn con của `problem_management` (nút quay lại, không phải màn gốc)
> [SoT: 01-rd/screens/shared/problem_authoring.md:22-25, 09-layoutBase/Admin - Soạn đề bài.dc.html:142].
> `testcase_management` **không phải slug riêng** — toàn bộ testcase là một tab bên trong màn này
> (Q2 đã đóng) [SoT: 01-rd/screens/shared/problem_authoring.md:18-20, 187].

## 1. Mục đích và phạm vi

Nơi A2/A3 tạo và sửa trọn vẹn một bài toán trong một màn: đề Markdown+LaTeX, phân loại, ràng buộc/giới
hạn, ví dụ mẫu, đặc tả song song hai mô hình nộp bài, đáp án mẫu, testcase, gợi ý AI theo bài — rồi xuất
bản [SoT: 01-rd/screens/shared/problem_authoring.md:32-35]. Phạm vi dữ liệu theo quyền: A2 chỉ soạn/sửa
bài của mình, A3 quản toàn bộ kho, gác bởi hai `FUNCTION` `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT`
[SoT: 01-rd/req/identity.md — F1-10 tới F1-12].

Ngoài phạm vi (không thiết kế ở đây — xem mục 9):
- Hợp đồng API — `03-dd/api/problem-bank.md`, `03-dd/api/harness.md`, `03-dd/api/ai-review.md` (chưa
  viết) [SoT: 01-rd/screens/shared/problem_authoring.md:201-202].
- Lược đồ kiểu dữ liệu độc lập ngôn ngữ và thuật toán sinh mã khung — thuộc `harness`, không thuộc trục
  màn [SoT: 01-rd/screens/shared/problem_authoring.md:203-204].
- Ma trận phân quyền — thuộc `admin_permission_matrix`
  [SoT: 01-rd/screens/shared/problem_authoring.md:207-208].
- Bảng danh sách bài toán, tìm kiếm, phân trang — thuộc màn cha `problem_management`
  [SoT: 01-rd/screens/shared/problem_authoring.md:209-210].
- Hệ số nhân giới hạn theo ngôn ngữ (F2-10 phần hệ số) — thuộc `admin_language_config`
  [SoT: 01-rd/screens/shared/problem_authoring.md:211-212].

## 2. Layout regions

Đối chiếu `09-layoutBase/Admin - Soạn đề bài.dc.html` (736 dòng). Cấu trúc: thanh đầu trang sticky + vùng
nội dung 5 tab (4 tab cũ + tab "Đặc tả" mới, `DEC-2026-0831-problem-authoring-spec-tab`) bên trái + cột
thuộc tính sticky bên phải, lưới 2 cột (`minmax(0,1fr) 300px`)
[SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:141-159, 159, 344].

1. **Thanh đầu trang (sticky)** — nút quay lại `problem_management`, mã+tiêu đề bài (`#76 · Minimum
   Window Substring`), trạng thái lưu (`saveHint`), nút "Xem như người học", nút "Lưu và xuất bản"
   [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:141-157].
2. **Thanh tab** — 5 tab kèm số đếm: "Nội dung đề" · "Ví dụ mẫu" (N) · "Testcase" (N) · "Đặc tả" ·
   "Gợi ý AI" [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:163-165; tab "Đặc tả" bổ sung theo
   `DEC-2026-0831-problem-authoring-spec-tab`, không có trong prototype gốc].
3. **Vùng nội dung tab** (5 khối `sc-if`, chỉ một hiển thị theo tab đang chọn):
   - Tab 1 — Nội dung đề: Tiêu đề, Nội dung đề Markdown, Ràng buộc và giới hạn (4 trường), Ràng buộc dữ
     liệu, Đáp án mẫu theo ngôn ngữ [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:168-213].
   - Tab 2 — Ví dụ mẫu: danh sách ví dụ (Đầu vào/Kết quả/Giải thích), thêm/xoá
     [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:215-249].
   - Tab 3 — Testcase: thanh hành động (Tải lên hàng loạt, Sinh tự động, Chạy với đáp án mẫu), băng kết
     quả chạy, bảng testcase kéo-thả, panel phiên bản bộ testcase (bổ sung theo `DEC-2026-0831-problem-
     authoring-spec-tab`) [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:251-305].
   - Tab 4 — Đặc tả (mới, không có trong prototype — dựng theo `DEC-2026-0831-problem-authoring-spec-tab`):
     chữ ký hàm theo Java/C++/Python, lược đồ kiểu tham số/trả về, định dạng stdin/stdout Standard I/O,
     chiến lược so khớp `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`
     [SoT: 01-rd/screens/shared/problem_authoring.md:189].
   - Tab 5 — Gợi ý AI: chỉ còn khối "Chỉ dẫn cho trợ lý AI" (khối "Gợi ý theo cấp độ" đã cắt,
     `DEC-2026-0831-problem-authoring-round2`) [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:296-310].
4. **Cột thuộc tính (sticky, bên phải)**: "Thuộc tính" (Chủ đề, Độ khó, Trạng thái, Thẻ), "Sẵn sàng xuất
   bản" (checklist), "Số liệu bài" [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:313-373].

Sidebar/topbar admin+instructor dùng chung khung điều hướng toàn hệ thống — không mô tả lại ở đây (thuộc
BD khung dùng chung, chưa viết — cùng khoảng trống Q1 đã ghi ở `02-bd/screens/admin/admin_queue_monitor.md`
mục 8).

## 3. Component inventory

| Vùng | Component | Mô tả hành vi (không phải hợp đồng API) |
| :--- | :--- | :--- |
| Đầu trang | `AuthoringHeader` | Mã+tiêu đề, `saveHint` ("Đang hiển thị cho người học · lưu nháp tự động Xh trước" khi `PUBLISHED`, "Bản nháp · chưa hiển thị cho người học" khi `UNPUBLISHED`) [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:148, 724, 678]. Auto-save chưa có mã RD — xem mục 8 |
| Đầu trang | Nút "Xem như người học" | Mở preview đề bài đúng như A1 thấy — chưa gắn mã RD, xem mục 8 |
| Đầu trang | Nút "Lưu và xuất bản" | Chuyển `problems.status` từ `UNPUBLISHED` sang `PUBLISHED` (`02-bd/database/problem-bank.md` mục 1.1) — **chỉ bấm được khi checklist "Sẵn sàng xuất bản" đủ điều kiện**, không optimistic-update trước phản hồi backend [SoT: Suy luận — cùng nguyên tắc đã áp cho `admin_queue_monitor` toggle] |
| Tab 1 | `MarkdownEditorField` | Textarea Markdown thô, không có preview/LaTeX render trong prototype — **khoảng trống cần bổ sung khi dựng UI thật**, F2-01 yêu cầu "Markdown kèm LaTeX" [SoT: 01-rd/screens/shared/problem_authoring.md:81-82; 01-rd/req/problem-bank.md — F2-01] |
| Tab 1 | `LimitsGrid` | 4 trường: Giới hạn thời gian (giây), Giới hạn bộ nhớ (MB) — khớp F2-10; Kích thước đầu ra (KB), Số lần nộp/giờ — amendment F2-10 (`DEC-2026-0831-problem-authoring-round2`). Map cột `problems.time_limit_ms`/`memory_limit_mb`/`max_output_size_kb`/`max_submissions_per_hour` [SoT: 02-bd/database/problem-bank.md mục 1.1]. Phụ đề "áp dụng toàn bộ ngôn ngữ, ghi đè ở `admin_language_config`" — đúng ranh giới, hệ số nhân theo ngôn ngữ không nằm ở màn này [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:182] |
| Tab 1 | `ConstraintsField` | Textarea tự do — vừa là văn bản đề bài, vừa là đầu vào bắt buộc cho F2-14 (AI sinh input dựa trên đề bài + ràng buộc dữ liệu) [SoT: 01-rd/req/problem-bank.md — F2-14] |
| Tab 1 | `SampleSolutionEditor` | Chọn ngôn ngữ Python/C++/Java + textarea code, phụ đề "Dùng để sinh kết quả mong đợi cho testcase" — cơ chế an toàn cốt lõi của F2-14 (AI không tự sinh output) [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:199-210] |
| Tab 2 | `ExampleList` / `ExampleCard` | Mỗi ví dụ: Đầu vào/Kết quả/Giải thích, thêm/xoá. Khác testcase Sample — ví dụ là văn bản minh hoạ trong đề, không phải dữ liệu chạy máy [SoT: 01-rd/screens/shared/problem_authoring.md:97-100] |
| Tab 3 | `TestcaseToolbar` | 3 nút: "Tải lên hàng loạt" (F2-07, mở luồng presigned URL theo `02-bd/storage/problem-bank.md` mục 3), "Sinh tự động" (F2-14 — **disabled kèm lý do tại chỗ khi chưa có Đáp án mẫu chạy Pass**, không chờ bấm mới báo lỗi, xem mục 4 GWT), "Chạy với đáp án mẫu" |
| Tab 3 | `RunResultBanner` | Sau khi chạy: "Đúng N/N testcase" hoặc "Thất bại ở testcase #k" kèm nguyên nhân (ví dụ "Vượt giờ") và thời gian tối đa [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:264-269, 711-716] |
| Tab 3 | `TestcaseTable` | Cột: `#`, Đầu vào, Kết quả mong đợi, **Hiển thị** (toggle "Công khai"/"Ẩn" — nhãn VN, giá trị dữ liệu `SAMPLE`/`HIDDEN`, `02-bd/database/problem-bank.md` mục 1.6), Chạy thử, xoá. **Không có cột "Điểm"** (đã xoá, Q3 đóng, `DEC-2026-0831-partial-score-testcase-ratio`) [SoT: 01-rd/screens/shared/problem_authoring.md:113-117] |
| Tab 3 | Kéo-thả sắp thứ tự testcase | Giữ nguyên ở mức trình bày (nhóm testcase theo ca kiểm thử) — không còn ảnh hưởng kết quả chấm vì F4 chạy hết mọi testcase (`DEC-2026-0831-partial-score-testcase-ratio`), khác lý do gốc đã lỗi thời (fail-fast F4-04) [SoT: 01-rd/screens/shared/problem_authoring.md:118-124] |
| Tab 3 | `TestcaseVersionBadge` (mới, `DEC-2026-0831-problem-authoring-spec-tab`) | Hiển thị `problems.current_testcase_set_version` ở đầu tab — panel/drawer trong tab, không tách màn/hộp thoại riêng [SoT: 02-bd/database/problem-bank.md mục 1.6; 01-rd/screens/shared/problem_authoring.md:187 (Q2 đóng)]. Không có trong prototype (grep "phiên bản": không có kết quả) — bổ sung khi dựng UI thật, xem mục 4 GWT thứ tư |
| Tab 3 | Nút "+ Thêm testcase" | Thêm dòng trống vào `TestcaseTable`, mặc định `Ẩn` [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:290, 665] |
| Tab 4 | `FunctionSignatureEditor` × 3 (Java/C++/Python) | Tên hàm, kiểu trả về, danh sách tham số (tên + kiểu) theo lược đồ `TypeKind` của `harness` — component này **là giao diện nhập liệu cho JSONB `return_type`/`parameters`** [SoT: 02-bd/database/problem-bank.md mục 1.5; 02-bd/architecture/harness.md mục 4.2]. Kiểu nhập qua picker cấu trúc (chọn `kind`, lồng `of` khi `ARRAY`/`LIST`/`LINKED_LIST`/`BINARY_TREE`), không phải textarea JSON thô — để tránh A2/A3 gõ sai cấu trúc `[SoT: Suy luận]` — lý do: `harness` mục 4.5 chốt validate structural ngay lúc lưu, một picker có ràng buộc cấu trúc sẵn giảm tỉ lệ request bị từ chối so với textarea tự do; RD/prototype không đặc tả UI cụ thể của tab này (tab mới hoàn toàn) nên đây là đề xuất BD, chưa có xác nhận |
| Tab 4 | `StdioFormatFields` | Hai textarea mô tả tự do `stdin_format_md`/`stdout_format_md` [SoT: 02-bd/database/problem-bank.md mục 1.4]. Placeholder/gợi ý mẫu **phải dùng đúng ký hiệu đã chốt**: null-marker `#` cho `BINARY_TREE`, token `true`/`false` cho `BOOLEAN`, mảng nhiều chiều mỗi dòng một chiều [SoT: 02-bd/architecture/harness.md mục 8, chốt 2026-09-13] |
| Tab 4 | `MatchingStrategySelector` | Chọn `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`; hiện thêm trường `epsilon_value` khi chọn `EPSILON` [SoT: 02-bd/database/problem-bank.md mục 1.4]. **Phải chặn ở tầng UI** tổ hợp không hợp lệ `UNORDERED_SET` + kiểu trả về `BINARY_TREE`/`LINKED_LIST` (validate liên trường đã ghi nhận ở `harness` mục 6, chốt chi tiết ở DD) — báo lỗi tại chỗ, không chờ backend từ chối |
| Tab 5 | `AiBriefField` | Textarea "Chỉ dẫn cho trợ lý AI" — chỉ thị bậc hai, nối vào prompt hệ thống trong một khối có nhãn riêng, không ghi đè ràng buộc cứng F5-17/F5-18 [SoT: `DEC-2026-0831-ai-instruction-injection-guard`]. Chỉ vai trò có `PROBLEM_AUTHORING:UPDATE` mới sửa được |
| Tab 5 | `AiFlagToggles` × 3 | "Không đưa mã hoàn chỉnh", "Chỉ hỏi ngược, không giải hộ", "Cho phép AI mở gợi ý ẩn" [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:302-306] |
| Cột phải | `PropertiesPanel` | Chủ đề (single-select `topics`), Độ khó (segment `EASY`/`MEDIUM`/`HARD`), **Trạng thái chỉ 2 lựa chọn** `Chưa xuất bản`/`Đã xuất bản` — prototype còn vẽ 3 lựa chọn kể cả "Ẩn", **không dựng "Ẩn" khi build UI thật** (F2-15, `DEC-2026-0830-problem-lifecycle-two-states`) [SoT: 01-rd/screens/shared/problem_authoring.md:146-148]. Thẻ tự do (`tags`, thêm mới khi gõ chưa tồn tại) [SoT: 02-bd/database/problem-bank.md mục 1.3] |
| Cột phải | `PublishChecklist` | 4 điều kiện (đã bỏ "tổng trọng số bằng 100" — Q3 đóng): ≥ 8 testcase, ≥ 2 testcase công khai (`SAMPLE`), đáp án mẫu chạy đúng mọi testcase, ≥ 2 ví dụ mẫu [SoT: 02-bd/database/problem-bank.md mục 5, `DEC-2026-0831-problem-management-lifecycle-details`]. **Bổ sung điều kiện thứ 5 khi dựng UI thật: tab "Đặc tả" không được để trống** (`DEC-2026-0831-problem-authoring-spec-tab`) |
| Cột phải | `ProblemMetricsPanel` | Lượt nộp, Tỉ lệ AC, Thời gian giải trung bình, Lượt xin gợi ý — đọc từ `problem_stats` read model [SoT: 02-bd/database/problem-bank.md mục 1.9], cộng dòng "Sửa lần cuối X giờ trước bởi <tên>" đọc `problems.updated_by`/`updated_at` |

## 4. Screen states

| Trạng thái | Điều kiện | Hiển thị |
| :--- | :--- | :--- |
| Đang tải | Vừa mở màn (route có `id`), chưa có dữ liệu bài | Skeleton toàn bộ 3 vùng chính (tab content, cột thuộc tính, header) |
| Soạn mới | Route không có `id` hợp lệ / vào từ nút "Bài tập mới" ở `problem_management` | Toàn bộ trường rỗng, `status = UNPUBLISHED`, checklist "Sẵn sàng xuất bản" hiện đủ 5 mục ở trạng thái chưa đạt, nút "Lưu và xuất bản" chỉ lưu nháp (`UNPUBLISHED`) cho tới khi đủ checklist |
| Soạn sửa — nháp | `status = UNPUBLISHED`, đã có dữ liệu | `saveHint` = "Bản nháp · chưa hiển thị cho người học" |
| Soạn sửa — đã xuất bản | `status = PUBLISHED` | `saveHint` = "Đang hiển thị cho người học · lưu nháp tự động Xh trước" |
| Đang gõ (auto-save) | Người dùng đang chỉnh field | `[SoT: Suy luận]` — ngụ ý lưu nháp tự động từ chuỗi `saveHint`, không có mã `Fx-nn`. Đề xuất debounce ~2 giây/field, không optimistic khoá field khi đang lưu — cần chủ dự án xác nhận, xem mục 8 |
| Đang chạy đáp án mẫu | Người dùng bấm "Chạy với đáp án mẫu" | Nút chuyển "Đang chạy…", disabled; `TestcaseTable` từng dòng hiện "đang chạy" cho tới khi có kết quả [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:509-520, 559] |
| Có kết quả chạy — Pass toàn bộ | `runResult.failIdx < 0` | `RunResultBanner` xanh "Đúng N/N testcase"; điều kiện checklist "Đáp án mẫu chạy đúng mọi testcase" chuyển đạt |
| Có kết quả chạy — Fail | `runResult.failIdx >= 0` | `RunResultBanner` đỏ "Thất bại ở testcase #k" kèm nguyên nhân đầu tiên (ví dụ "Vượt giờ"); dòng testcase #k trong bảng đánh dấu lỗi; checklist "Đáp án mẫu chạy đúng mọi testcase" **không đạt** [SoT: 01-rd/screens/shared/problem_authoring.md:166-169] |
| "Sinh tự động" bị chặn sớm | Chưa có Đáp án mẫu chạy Pass toàn bộ testcase hiện có | Nút ở trạng thái **vô hiệu kèm lý do ngay tại chỗ** (tooltip/inline text), không phải bấm được rồi mới báo lỗi — F2-14 chốt "chưa có đáp án mẫu hợp lệ thì tính năng vô hiệu" [SoT: 01-rd/req/problem-bank.md — F2-14; 01-rd/screens/shared/problem_authoring.md:160-165] |
| Testcase AI sinh — nháp chờ xác nhận | Sau khi "Sinh tự động" chạy xong | Các dòng mới thêm vào `TestcaseTable` đánh dấu trạng thái nháp (map cột `is_ai_generated_draft = true`, `02-bd/database/problem-bank.md` mục 1.6), **chưa gộp vào Hidden khi xuất bản** cho tới khi A2/A3 xác nhận từng dòng [SoT: 01-rd/req/problem-bank.md — F2-14] |
| Kiểu dữ liệu vượt lược đồ (F3-13) | Tab "Đặc tả": chữ ký hàm có `kind` không nằm trong tập hợp lệ, hoặc thiếu `of` bắt buộc | Không chặn cứng lưu spec — hệ thống tự đặt `function_wrapper_supported = false`, hiện cảnh báo tại tab "Đặc tả": "Kiểu dữ liệu này chỉ hỗ trợ Standard I/O, bài sẽ không hiện tuỳ chọn Bọc hàm cho người học" [SoT: 02-bd/architecture/harness.md mục 4.5, 195-199] |
| Sẵn sàng xuất bản — chưa đủ | Bất kỳ điều kiện checklist chưa đạt | Nút "Lưu và xuất bản" disabled hoặc lưu về `UNPUBLISHED` kèm banner liệt kê điều kiện thiếu [SoT: Suy luận — hành vi cụ thể của nút khi checklist chưa đủ không có trong prototype, chốt ở DD] |
| Sửa testcase của bài đã có người nộp | Lưu thay đổi testcase Hidden trên bài `status = PUBLISHED` đã có submission | Tăng `problems.current_testcase_set_version`, hiện thông báo phiên bản mới là bao nhiêu (F2-09) [SoT: 01-rd/screens/shared/problem_authoring.md:177-180, `US-A2-02`] |
| Đổi trạng thái Đã xuất bản → Chưa xuất bản | A2/A3 lưu sau khi đổi `Trạng thái` | Bài ẩn khỏi người học, lượt nộp cũ không bị xoá — nguyên tắc "không phá dữ liệu đã có" [SoT: Suy luận, tiền lệ F1-16 — 01-rd/screens/shared/problem_authoring.md:170-176] |
| Lỗi tải dữ liệu | API lấy bài lỗi (mạng, 404, không có quyền) | Thông báo lỗi toàn màn kèm nút thử lại/quay về `problem_management`, không hiển thị dữ liệu cũ giả định |
| Không có quyền sửa bài này | A2 mở bài không thuộc sở hữu của mình (không có `TESTCASE_MANAGEMENT`/`PROBLEM_AUTHORING` phạm vi tương ứng) | 403 tại tầng route — chuyển hướng hoặc thông báo chặn truy cập, xem mục 7 |

## 5. API tiêu thụ (chỉ liệt kê — hợp đồng đầy đủ ở DD)

| Mục đích | Bounded Context sở hữu |
| :--- | :--- |
| Lấy chi tiết bài toán để soạn/sửa (đề, ràng buộc, đáp án mẫu, ví dụ, thuộc tính) | `problem-bank` |
| Lưu/cập nhật nội dung đề, ràng buộc, đáp án mẫu, ví dụ mẫu | `problem-bank` |
| Đổi trạng thái xuất bản (`UNPUBLISHED`/`PUBLISHED`) | `problem-bank` |
| Nhân bản bài toán (F2-16) | `problem-bank` |
| CRUD testcase (thêm/sửa/xoá/sắp thứ tự/đổi Hiển thị) | `problem-bank` |
| Tải lên hàng loạt testcase (presigned URL, F2-07) | `problem-bank` |
| Lấy/tăng phiên bản bộ testcase (F2-09) | `problem-bank` |
| "Chạy với đáp án mẫu" trên toàn bộ testcase hiện có | `problem-bank` (outbound port `SampleSolutionRunnerPort` gọi go-judge — `02-bd/architecture/problem-bank.md` mục 3.2, 7) |
| "Sinh tự động" testcase bằng AI (F2-14) | `problem-bank` (chỉ sinh input; output chạy qua Đáp án mẫu, không gọi `ai-review` để lấy output) |
| Lưu/đọc đặc tả hàm (chữ ký, lược đồ kiểu), định dạng stdin/stdout, chiến lược so khớp (tab "Đặc tả") | `problem-bank` (lưu trữ) + `harness` (nguồn lược đồ kiểu dùng để validate — qua shared kernel `algoprep-common`, không phải một API riêng của `harness` mà màn này gọi trực tiếp) [SoT: 02-bd/architecture/harness.md mục 4.1] |
| Lưu "Chỉ dẫn cho trợ lý AI" + 3 cờ hành vi theo bài | `problem-bank` lưu trữ; `ai-review` tiêu thụ khi build prompt lúc chạy Solution Review/Mock Interview (`DEC-2026-0831-ai-instruction-injection-guard`) |
| Số liệu bài (Lượt nộp, Tỉ lệ AC, Thời gian giải TB, Lượt xin gợi ý) | `problem-bank` (read model `problem_stats`, `02-bd/database/problem-bank.md` mục 1.9) |
| Danh mục Chủ đề (`topics`) để chọn ở "Thuộc tính" | `problem-bank` |

Không có API nào của `judge-orchestration` hay `identity` được màn này gọi trực tiếp cho luồng nghiệp vụ
chính — "Chạy với đáp án mẫu" đi qua outbound port riêng của `problem-bank` (`SampleSolutionRunnerPort`),
không qua hàng đợi/state machine của `judge-orchestration`, vì đây là một lượt chạy thử biệt lập, không
phải submission cần theo dõi trạng thái [SoT: 02-bd/architecture/problem-bank.md mục 3.2]. `identity` chỉ
tham gia gián tiếp qua kiểm tra quyền `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` ở tầng gateway/middleware,
không phải một API màn này chủ động gọi.

Tên endpoint cụ thể, request/response, mã lỗi: **không viết ở đây** — thuộc `03-dd/api/problem-bank.md`,
`03-dd/api/harness.md`, `03-dd/api/ai-review.md` (chưa viết), theo anti-drift rule của `bd-generation`
Layer 3.

## 6. Navigation

- Vào từ nút "Bài tập mới" hoặc click một dòng bài toán ở màn cha `problem_management`
  [SoT: 01-rd/screens/shared/problem_authoring.md:209-210].
- Nút quay lại (`‹`) ở đầu trang đưa về `problem_management`
  [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:142].
- Nút "Xem như người học" mở preview đề bài — đích đến cụ thể (modal hay tab mới, có render như
  `problem_detail` thật hay không) chưa có mã RD, xem mục 8.
- Không có điều hướng sang `admin_language_config` từ nút bấm trực tiếp trên màn — chỉ có dòng chú thích
  văn bản trỏ khái niệm (hệ số ngôn ngữ nằm ở đó), không phải liên kết click-through
  [SoT: 09-layoutBase/Admin - Soạn đề bài.dc.html:182].
- Route mount ở hai tiền tố `/instructor/problems/[id]` và `/admin/problems/[id]`, cùng component, khác
  chỉ ở khung điều hướng bao ngoài (sidebar Giảng viên vs Admin)
  [SoT: `DEC-2026-0825-shared-content-authoring-screens`, `DEC-2026-0825-frontend-base-architecture`].

## 7. Access rights

- Chỉ actor **A2 (Instructor)** và **A3 (Admin)** truy cập được màn này — A1 (Student) không có quyền
  [SoT: 01-rd/screens/shared/problem_authoring.md:4-8].
- Gác bởi hai `FUNCTION` trong ma trận phân quyền của `identity`: `PROBLEM_AUTHORING` (nội dung đề, ví dụ,
  đặc tả, xuất bản) và `TESTCASE_MANAGEMENT` (CRUD testcase, tải lên hàng loạt, sinh tự động, chạy đáp án
  mẫu) [SoT: 01-rd/req/identity.md — F1-10 tới F1-12].
- **Phạm vi dữ liệu khác nhau theo vai trò**: A2 chỉ thao tác được trên bài toán do chính mình tạo (kiểm
  tra sở hữu ở tầng application, không chỉ ẩn UI); A3 thao tác được trên toàn bộ kho bài toán
  [SoT: 01-rd/screens/shared/problem_authoring.md:4-8]. Đây là kiểm tra **quyền sở hữu bản ghi**, khác với
  RBAC thuần vai trò của `admin_queue_monitor` — màn này cần cả hai lớp (có `FUNCTION` + đúng sở hữu hoặc
  là A3).
- Mọi hành động ghi (lưu nội dung, CRUD testcase, đổi trạng thái xuất bản, chạy đáp án mẫu, sinh testcase
  AI) là API có tác dụng phụ thật — yêu cầu kiểm tra quyền ở tầng API, không chỉ ẩn nút ở tầng FE
  [SoT: Suy luận — nguyên tắc RBAC chuẩn của `identity` áp dụng lại, cùng lý do đã nêu ở
  `02-bd/screens/admin/admin_queue_monitor.md` mục 7].
- Trường "Chỉ dẫn cho trợ lý AI" chỉ vai trò có `PROBLEM_AUTHORING:UPDATE` mới sửa được — chốt riêng vì
  đây là nội dung nối vào prompt AI, rủi ro prompt-injection cao hơn các trường khác
  [SoT: `DEC-2026-0831-ai-instruction-injection-guard`].

## 8. Việc còn mở

| # | Mức | Câu hỏi | Đề xuất | Chủ sở hữu |
| :-: | :-: | :--- | :--- | :--- |
| Q1 | C | Nút "Xem như người học" — đích đến cụ thể (modal/tab mới) và có render đúng `problem_detail` thật không? Chưa có mã RD [SoT: 01-rd/screens/shared/problem_authoring.md:67, Q7 đã đóng nhưng không nêu chi tiết nút này] | Đề xuất mở trong tab mới, render lại component `problem_detail` ở chế độ preview (không lưu tiến trình giải) để tránh trôi (drift) hai bản UI đề bài | DD |
| Q2 | C | Auto-save — debounce theo giây/field, có khoá field khi đang lưu hay không; không có mã `Fx-nn` nào phủ [SoT: 01-rd/screens/shared/problem_authoring.md:66, Q7 đã đóng, không định nghĩa cơ chế] | Debounce ~2 giây/field, không khoá field, hiện `saveHint` cập nhật realtime; chốt ở DD `problem-bank` | DD |
| Q3 | C | Hành vi nút "Lưu và xuất bản" khi checklist chưa đủ 5 điều kiện — chặn cứng hay lưu về nháp kèm banner? [SoT: Suy luận, mục 4 "Sẵn sàng xuất bản — chưa đủ"] | Đề xuất: lưu nội dung bình thường (không mất dữ liệu), nhưng **không đổi `status` sang `PUBLISHED`**, hiện banner liệt kê điều kiện thiếu | DD |
| Q4 | D | Nút "Nhờ AI soạn nháp" (tab 1, cạnh "Gợi ý theo cấp độ" đã cắt) — F2-14 chỉ phủ sinh testcase, không phủ AI soạn gợi ý/đề bài. Còn mở, tách riêng khỏi Q6 gốc, chưa xử lý [SoT: 01-rd/screens/shared/problem_authoring.md:191, "Còn mở, tách riêng khỏi Q6"] | Cần chủ dự án xác nhận: giữ tính năng (thêm mã RD mới cho AI soạn nháp đề bài) hay cắt khỏi phạm vi cùng đợt với "Gợi ý theo cấp độ" | Chủ dự án |
| Q5 | D | `FunctionSignatureEditor` — picker cấu trúc kiểu (chọn `kind`, lồng `of`) là đề xuất BD, chưa có UI cụ thể nào trong prototype cho tab "Đặc tả" (tab hoàn toàn mới) | Giữ đề xuất, thiết kế chi tiết UI ở DD/prototype bổ sung cho tab này trước khi build | DD + prototype bổ sung |
| Q6 | D | Cột "Trạng thái" ở "Thuộc tính" hiện 3 lựa chọn tĩnh trong prototype gốc (kể cả "Ẩn") — đã xác nhận chỉ dựng 2 (F2-15). Cần bản prototype cập nhật hoặc ghi chú rõ khi giao cho FE | Ghi chú rõ trong hồ sơ bàn giao FE, không sửa lại file prototype gốc (read-only evidence) | DD/BUILD |
| Q7 | D | Quy tắc validate liên trường `UNORDERED_SET` không hợp lệ với `BINARY_TREE`/`LINKED_LIST` — thông báo lỗi cụ thể hiển thị ở `MatchingStrategySelector` | Chốt câu chữ lỗi ở DD, cùng lúc với validate backend [SoT: 02-bd/architecture/harness.md mục 6] | DD |

Không có mục nào ở mức A (mâu thuẫn SoT thật) hay B (khoảng trống schema) cho màn này — mọi bảng/cột cần
thiết đã có trong `02-bd/database/problem-bank.md` (kể cả amendment round2/spec-tab); các câu hỏi còn lại
đều thuộc mức C/D theo phân loại của `07-review/bd_screens_admin_open_questions_260913.md` mục 1.

## 9. Ngoài phạm vi file này (nhắc lại từ RD, không thiết kế ở đây)

- Bố cục/bảng màu/spacing/typography chi tiết — chưa có design system SoT chính thức ngoài
  `09-layoutBase/**`; mô tả cấu trúc, không tự đặt giá trị màu/spacing.
- Hợp đồng API đầy đủ — `03-dd/api/problem-bank.md`, `03-dd/api/harness.md`, `03-dd/api/ai-review.md`.
- Lược đồ kiểu dữ liệu và thuật toán sinh mã khung — `02-bd/architecture/harness.md` (đã chốt), DD của
  `harness`.
- Ma trận phân quyền — `02-bd/screens/admin/admin_permission_matrix.md`.
- Bảng danh sách/tìm kiếm/phân trang bài toán — `01-rd/screens/shared/problem_management.md` (BD chưa
  viết ở thời điểm file này).
- Hệ số nhân giới hạn theo ngôn ngữ và cấu hình ngôn ngữ — `02-bd/screens/admin/admin_language_config.md`.

## 10. Tham chiếu

- `01-rd/screens/shared/problem_authoring.md` — RD của màn (Q1-Q7 đã đóng, trừ mục nhỏ tách khỏi Q6).
- `09-layoutBase/Admin - Soạn đề bài.dc.html` — prototype (736 dòng), thiếu tab "Đặc tả" và panel phiên
  bản testcase (bổ sung sau prototype theo `DEC-2026-0831-problem-authoring-spec-tab`).
- `02-bd/architecture/problem-bank.md` — kiến trúc module, ranh giới Bọc hàm/Standard I/O (mục 4), ngoại
  lệ F3-13 (mục 5).
- `02-bd/database/problem-bank.md` — toàn bộ bảng: `problems`, `problem_specs`, `function_signatures`,
  `testcases`, `topics`/`tags`, `bookmarks`, `class_assignments`, `problem_stats`.
- `02-bd/storage/problem-bank.md` — ranh giới PostgreSQL/MinIO, presigned URL cho "Tải lên hàng loạt".
- `02-bd/architecture/harness.md` mục 4 (lược đồ `TypeKind`), mục 4.5 (ngoại lệ F3-13), mục 6 (so khớp
  kết quả), mục 8 (ký hiệu định dạng đã chốt 2026-09-13: null-marker `#`, `true`/`false`, mảng nhiều
  chiều mỗi dòng một chiều).
- `02-bd/screens/admin/admin_queue_monitor.md` — khuôn mẫu cấu trúc file BD trục màn hình.
- `07-review/bd_screens_admin_open_questions_260913.md` — phân loại mức câu hỏi mở (A/B/C/D) áp dụng lại
  ở mục 8 file này.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0830-problem-lifecycle-two-states`,
  `DEC-2026-0831-problem-authoring-spec-tab`, `DEC-2026-0831-problem-authoring-round2`,
  `DEC-2026-0831-ai-instruction-injection-guard`, `DEC-2026-0831-problem-management-lifecycle-details`,
  `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`,
  `DEC-2026-0824-dual-submission-model-per-problem`.

**Status:** DONE_WITH_CONCERNS
**Summary:** Đã viết BD cho màn `problem_authoring` tại `02-bd/screens/shared/problem_authoring.md` —
layout 4 vùng (header, 5 tab, cột thuộc tính), component inventory theo từng tab (bao gồm tab "Đặc tả"
mới chưa có trong prototype), 16 screen states, API tiêu thụ (chỉ tên + BC sở hữu: chủ yếu `problem-bank`,
`harness` chỉ góp lược đồ kiểu qua shared kernel), navigation, access rights (A2 sở hữu bài của mình / A3
toàn quyền, hai FUNCTION `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT`), và 7 câu hỏi mở (toàn bộ mức C/D,
không có mâu thuẫn SoT hay khoảng trống schema).
**Concerns/Blockers:** Không có blocker cứng. Hai điểm cần lưu ý trước khi vào DD: (1) tab "Đặc tả" hoàn
toàn không có trong prototype (`DEC-2026-0831-problem-authoring-spec-tab` chỉ chốt phạm vi nội dung, chưa
chốt UI cụ thể) — `FunctionSignatureEditor` dạng picker cấu trúc là đề xuất BD, cần một vòng prototype bổ
sung hoặc xác nhận trực tiếp ở DD trước khi build; (2) nút "Nhờ AI soạn nháp" (Q4) vẫn là mục treo từ RD,
chưa có quyết định giữ/cắt — nên gộp vào phiên xác nhận chung với các câu hỏi mức A/B đã tồn đọng của trục
màn `admin`.
