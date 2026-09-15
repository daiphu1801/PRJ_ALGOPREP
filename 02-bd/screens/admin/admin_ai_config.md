# BD — Màn `admin_ai_config` (Cấu hình trợ lý AI)

> Trục: **màn hình** (`02-bd/screens/`). Actor: A3 (`ADMIN`). Bounded Context chủ: `ai-review` (F5)
> [SoT: 01-rd/screens/admin/admin_ai_config.md:4]. Đọc cùng `01-rd/screens/admin/admin_ai_config.md` (spec
> hành vi ở mức RD, không lặp lại ở đây) và ba file BD module: `02-bd/architecture/ai-review.md`,
> `02-bd/database/ai-review.md`, `02-bd/security/ai-review.md`.
>
> **Không thiết kế** "Gợi ý theo bậc" (thẻ prompt, dòng giới hạn tần suất "Gợi ý mỗi bài") — đã cắt khỏi
> phạm vi `DEC-2026-0831-remove-tiered-hints-ai-config`
> [SoT: 01-rd/screens/admin/admin_ai_config.md:66]. **Không thiết kế** UI cho phép sửa trọng số rubric
> Mock Interview (F5-15) — cố định 25/30/25/20%, đã đóng 2026-09-13
> [SoT: 06-plan/reports/260913-1406-tom-tat-phien-bd.md mục 4 · 02-bd/database/ai-review.md:97].

## 1. Layout regions

Đối chiếu `09-layoutBase/Admin - Cấu hình AI.dc.html` (SoT chỉ-đọc bằng chứng layout, không phải design
system cuối cùng — `CLAUDE.md` mục Directory map).

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Sidebar điều hướng (khung chung Admin) | `:67-143` | Nhóm "AI" đang mở, mục con "Cấu hình AI" active, mục con "Token AI" liền kề — dùng lại khung điều hướng chung của mọi màn Admin, không mô tả lại chi tiết ở đây |
| Thanh tiêu đề (sticky header) | `:147-158` | Tiêu đề "Cấu hình trợ lý AI", mô tả phụ "Prompt, rubric chấm và giới hạn tần suất", nút hành động chính "Lưu và phát hành" |
| Cột trái — "Prompt theo tính năng" | `:163-190` | Danh sách thẻ prompt theo `feature_code`, nút "Nhật ký phiên bản" |
| Cột trái — "Rubric chấm bài giải" | `:192-218` | 5 tiêu chí trọng số + thanh tổng, chỉ áp dụng `SOLUTION_REVIEW` |
| Cột phải — "Giới hạn tần suất" | `:222-236` | Danh sách giới hạn dạng nhãn + giá trị, không có control chỉnh trực tiếp trên thẻ (xem mục 3, trạng thái Sửa) |
| Cột phải — "Nguyên tắc trả lời" | `:238-253` | 3 toggle bật/tắt (đã bỏ "Chuyển giảng viên khi bí" — xem mục 5) |
| Cột phải — "Trước khi phát hành" | `:255-260` | Mô tả bộ đối chiếu 30 bài mẫu, nút "Chạy đối chiếu", dòng kết quả lần chạy gần nhất + liên kết sang `admin_ai_usage` |
| Footer chung (khung Admin) | `:264-277` | Trạng thái dịch vụ, liên kết phụ — dùng lại khung chung, không mô tả lại |

Bố cục hai cột (`minmax(0,1.55fr) minmax(300px,1fr)` — `:160`): cột trái chứa nội dung cần chỉnh biên soạn
nhiều (prompt, rubric), cột phải chứa các khối cấu hình ngắn (giới hạn, toggle, đối chiếu) — giữ nguyên
cấu trúc này ở Next.js, không quy định màu sắc/khoảng cách/typography cụ thể (đúng nguyên tắc "layout
structurally", chưa chốt design token cho Next.js).

## 2. Component inventory

| Component | Mô tả | Dữ liệu hiển thị | Ghi chú thiết kế |
| :--- | :--- | :--- | :--- |
| `PromptFeatureCard` | Một thẻ trong "Prompt theo tính năng" | tên tính năng, `version`, trạng thái (`ACTIVE`/`DRAFT`/`ARCHIVED` → hiển thị "Đang chạy"/"Bản nháp"/…), mô tả ngắn, model, nhiệt độ, giới hạn token, ngày cập nhật, nút "Chỉnh sửa" | Đúng 3 thẻ: "Phân tích bài giải" (F5.1), "Phỏng vấn giả lập" (F5.2), "Sinh testcase" (F2-14) — **không** thẻ "Gợi ý theo bậc" [SoT: 01-rd/screens/admin/admin_ai_config.md:66]. Nguồn dữ liệu: `prompt_templates` [SoT: 02-bd/database/ai-review.md:8-25]. "Sinh testcase" thuộc `problem-bank`/F2-14 nhưng dùng chung khung `prompt_templates` — xem mục 6 câu hỏi mở về owner API |
| `PromptVersionHistoryButton` | Nút "Nhật ký phiên bản" | mở danh sách phiên bản trước đây của prompt (xem/khôi phục — F5-23) | Màn con hoặc modal, chưa thiết kế chi tiết ở đây — thuộc phạm vi màn hình phụ, đề xuất modal cùng trang, xem mục 6 |
| `RubricWeightEditor` | Khối "Rubric chấm bài giải" | 5 hàng tiêu chí, mỗi hàng: tên, mô tả phụ, nút `−`/`+`, giá trị %, thanh tiến trình; tổng trọng số ở góc phải khối | Nguồn: `rubric_configs` gắn với `prompt_template_id` của bản Solution Review đang biên soạn [SoT: 02-bd/database/ai-review.md:27-38]. **Chỉ áp dụng rubric Solution Review 5 tiêu chí** — không hiển thị/không cho sửa rubric Mock Interview 4 tiêu chí (cố định, ngoài phạm vi màn này) |
| `RubricWeightTotalBadge` | Nhãn tổng trọng số | `Tổng {tổng}%`, đổi màu khi khác 100% | [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html:195-196, 449, 474] — logic đổi màu là hành vi client-side thuần (so `tổng === 100`), không gọi API riêng |
| `RateLimitList` | Khối "Giới hạn tần suất" | 3 dòng: "Phân tích mỗi ngày" (10 lượt), "Phỏng vấn mỗi tuần" (5 phiên), "Chờ giữa hai yêu cầu" (20 giây) | Đã bỏ dòng "Gợi ý mỗi bài". Nguồn: cấu hình rate-limit hệ thống (Function `AI_CONFIG`, số cụ thể còn mở — `02-bd/database/ai-review.md` mục 6, 7) |
| `ResponseGuardToggleList` | Khối "Nguyên tắc trả lời" | 3 toggle: "Không đưa lời giải đầy đủ", "Trích dẫn dòng mã người học", "Trả lời bằng tiếng Việt" | Đã bỏ toggle "Chuyển giảng viên khi bí" (Câu hỏi mở Q2 của RD, đã đóng — moot theo Q1) [SoT: 01-rd/screens/admin/admin_ai_config.md:67]. Các toggle này ánh xạ tới nội dung `system_instruction` hoặc một cấu hình rời — điểm mở, xem mục 6 |
| `RegressionCheckPanel` | Khối "Trước khi phát hành" | mô tả bộ 30 bài mẫu, nút "Chạy đối chiếu", dòng "Lần chạy gần nhất" + liên kết "Xem tiêu thụ token" (→ `admin_ai_usage`) | **Chỉ hiển thị, không backend** — đã chốt phạm vi ở `01-rd/req/ai-review.md` (F5-23, giữ tầng giao diện) [SoT: 01-rd/screens/admin/admin_ai_config.md:26,50,74]. Nút "Chạy đối chiếu" không gọi API ở đợt này — chỉ dữ liệu tĩnh/giả lập hoặc để trống chờ backend tương lai |
| `SaveAndPublishButton` | Nút "Lưu và phát hành" ở thanh tiêu đề | | Hành động tổng: xác nhận toàn bộ thay đổi đang biên soạn (prompt DRAFT → ACTIVE, trọng số rubric, giới hạn tần suất, toggle) — xem trạng thái "Xác nhận phát hành" ở mục 3 |
| `PromptEditModal`/`PromptEditPanel` | Mở khi bấm "Chỉnh sửa" trên một `PromptFeatureCard` | form sửa `system_instruction`, model, nhiệt độ, giới hạn token cho **bản DRAFT** của prompt đó | Prototype chỉ có liên kết "Chỉnh sửa" (`:178`), chưa có form chi tiết — form cụ thể để DD/prototype kế tiếp thiết kế. Sửa **không ảnh hưởng bản đang chạy (`ACTIVE`) cho tới khi publish** [SoT: 01-rd/screens/admin/admin_ai_config.md:58-60] |

## 3. Screen states

| Trạng thái | Điều kiện | Hiển thị |
| :--- | :--- | :--- |
| Đang tải (loading) | Vừa vào màn, chưa nhận dữ liệu `prompt_templates`/`rubric_configs`/giới hạn/toggle | Khung skeleton cho từng khối (3 thẻ prompt, 5 hàng rubric, 3 dòng giới hạn, 3 toggle) — `hint-placeholder-count` trong prototype gợi ý số lượng khung chờ tương ứng [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html:172,199,226,241] |
| Đã tải — bình thường | Dữ liệu tải xong, tổng trọng số = 100% | Hiển thị đầy đủ như mục 1-2, `RubricWeightTotalBadge` màu trung tính |
| Cảnh báo — tổng trọng số lệch 100% | Admin dùng `−`/`+` làm tổng khác 100% | `RubricWeightTotalBadge` đổi màu cảnh báo (không phải đỏ lỗi cứng — cùng màu "neg" của prototype); `SaveAndPublishButton` **chặn** hành động lưu cho tới khi tổng = 100% (đề xuất `[SoT: Suy luận]` — RD chỉ nói "hiện cảnh báo", không nói rõ có chặn lưu hay chỉ cảnh báo mềm; đề xuất chặn vì `rubric_configs` yêu cầu tổng = 100 ở tầng ứng dụng theo `02-bd/database/ai-review.md:37`, cho lưu tổng sai sẽ tạo dữ liệu vi phạm bất biến đó) |
| Đang biên soạn (Draft đang mở) | Admin bấm "Chỉnh sửa" trên một prompt | `PromptEditModal`/`PromptEditPanel` mở, thẻ tương ứng hiển thị nhãn "Đang sửa bản nháp"; các thẻ khác không bị khoá | Áp dụng cho mọi trạng thái nguồn (kể cả "Bản nháp" như "Sinh testcase" v1.9 — sửa tiếp bản nháp không ảnh hưởng bản `ACTIVE`) [SoT: 01-rd/screens/admin/admin_ai_config.md:58-60] |
| Xác nhận phát hành | Bấm "Lưu và phát hành" | Hộp thoại xác nhận (đề xuất `[SoT: Suy luận]` — hành động đổi `status: DRAFT → ACTIVE` là không thể phục hồi tức thời về mặt hiển thị mới nhất, cần một bước xác nhận rõ ràng, nhất quán với các hành động "phát hành" khác trong hệ thống như `problem_management` publish) |
| Thành công | Sau khi lưu/phát hành thành công | Toast/banner xác nhận, các thẻ cập nhật `version`/`updated_at`/`status` mới |
| Lỗi lưu | API trả lỗi nghiệp vụ (ví dụ tổng trọng số không hợp lệ ở phía server, xung đột phiên bản) | Banner lỗi tại đúng khối gây lỗi (không thay đổi dữ liệu đã hiển thị, không rollback ngầm phần đã sửa ở khối khác) |
| Trống/không có prompt cho một feature | `[SoT: Suy luận]` — trường hợp biên, hiếm khi xảy ra vì migration seed 2 dòng `ACTIVE` ban đầu (`02-bd/database/ai-review.md` mục 5); vẫn cần trạng thái rỗng để không vỡ layout nếu seed thiếu | Thẻ hiển thị "Chưa có prompt — Tạo mới" thay vì các trường version/model |

## 4. APIs consumed

Chỉ liệt kê tên + Bounded Context sở hữu — hợp đồng chi tiết (request/response) thuộc `03-dd/api/ai-review.md`
(chưa viết) theo đúng nguyên tắc anti-drift của `bd-generation` Layer 2.

| Hành động màn hình | Endpoint (tên nghiệp vụ, chưa chốt path) | BC sở hữu |
| :--- | :--- | :--- |
| Tải danh sách prompt theo tính năng | `ListPromptTemplates` (lọc `feature_code IN (SOLUTION_REVIEW, MOCK_INTERVIEW)`, và bản ghi "Sinh testcase" — xem câu hỏi mở mục 6) | `ai-review` (F5) — hoặc `problem-bank` (F2) cho "Sinh testcase", chưa chốt |
| Xem nhật ký phiên bản một prompt | `GetPromptTemplateHistory` | `ai-review` |
| Sửa nội dung một bản prompt (DRAFT) | `UpdatePromptTemplateDraft` | `ai-review` |
| Publish một bản prompt (DRAFT → ACTIVE) | `PublishPromptTemplate` | `ai-review` |
| Tải cấu hình trọng số rubric Solution Review | `GetRubricConfig` | `ai-review` |
| Cập nhật trọng số rubric Solution Review | `UpdateRubricConfig` (server validate tổng = 100) | `ai-review` |
| Tải giới hạn tần suất hiện hành | `GetRateLimitConfig` | `ai-review` |
| Cập nhật giới hạn tần suất | `UpdateRateLimitConfig` | `ai-review` |
| Tải trạng thái nguyên tắc trả lời (toggle) | `GetResponseGuardConfig` | `ai-review` |
| Cập nhật nguyên tắc trả lời | `UpdateResponseGuardConfig` | `ai-review` |
| Lưu và phát hành toàn bộ thay đổi đang biên soạn | `PublishAiConfigChanges` (có thể là tổ hợp gọi nhiều endpoint trên trong một giao dịch UI, hoặc một endpoint tổng — chốt ở DD) | `ai-review` |
| Liên kết "Xem tiêu thụ token" | không gọi API tại đây — điều hướng sang màn `admin_ai_usage`, API thuộc phạm vi màn đó | `ai-review` |

Không có endpoint cho "Chạy đối chiếu" — khối `RegressionCheckPanel` không gọi backend ở đợt này
[SoT: 01-rd/screens/admin/admin_ai_config.md:26,50,74].

## 5. Navigation

- Vào màn: từ sidebar nhóm "AI" → mục con "Cấu hình AI" [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html:337].
- Điều hướng ra: liên kết "Xem tiêu thụ token" → `admin_ai_usage` [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html:259, 338].
- Không có điều hướng sang màn con dạng route riêng cho "Nhật ký phiên bản" trong prototype (biểu hiện dưới
  dạng nút, không phải link) — đề xuất modal/side-panel cùng trang, không phải điều hướng route mới
  `[SoT: Suy luận]`.
- Không điều hướng ra khỏi ngữ cảnh Admin trừ nút "Thoát" dùng chung khung sidebar (`→ Dashboard AlgoPrep`,
  ngoài phạm vi màn này).

## 6. Access rights

- Actor duy nhất: **A3 (`ADMIN`)** [SoT: 01-rd/screens/admin/admin_ai_config.md:4].
- Function gác quyền: `AI_CONFIG` (F1-12) cho toàn bộ thao tác đọc/sửa prompt, rubric Solution Review,
  nguyên tắc trả lời [SoT: 02-bd/security/ai-review.md:79-82]. Giới hạn tần suất (`RateLimitList`) —
  `[SoT: Suy luận]` cùng `AI_CONFIG` vì đây là tham số vận hành tổng quát của trợ lý AI, không phải ngân
  sách token (`AI_TOKEN_BUDGET`, thuộc `admin_ai_usage` — hai Function tách biệt cố ý theo
  `02-bd/security/ai-review.md:79-82`, xác nhận lại ở DD).
- `STUDENT`/`INSTRUCTOR` không có quyền truy cập màn này — chặn ở tầng route (frontend) và tầng
  `@PreAuthorize` (backend), không chỉ ẩn UI.
- Dữ liệu cấu hình hiển thị ở màn này **không có phạm vi theo lớp/theo người dùng** — là cấu hình toàn hệ
  thống, mọi `ADMIN` có `AI_CONFIG` nhìn thấy cùng một bộ dữ liệu [SoT: 02-bd/security/ai-review.md:47-49].

## 7. Câu hỏi mở (liệt kê, không tự chốt)

| # | Câu hỏi | Vì sao chưa trả lời | Chủ sở hữu đề xuất |
| :-: | :--- | :--- | :--- |
| Q1 | Thẻ "Sinh testcase" (F2-14) thuộc `prompt_templates` của schema `ai` hay một bảng tương tự riêng ở `problem-bank`? RD chỉ nói khớp mã F2-14, không nói rõ owner dữ liệu. `02-bd/database/ai-review.md` mục 1.1 chỉ định nghĩa `feature_code ENUM(SOLUTION_REVIEW, MOCK_INTERVIEW)` — **không có `GENERATE_TESTCASE`** trong enum đó. | Ảnh hưởng endpoint `ListPromptTemplates` gọi một hay hai module | DD `ai-review` + `problem-bank` cùng xác nhận |
| Q2 | "Nguyên tắc trả lời" (3 toggle) — lưu như một phần của `prompt_templates.system_instruction` (chèn/xoá câu tương ứng khi bật/tắt) hay một bảng cấu hình boolean rời áp dụng cho mọi prompt? | BD module `ai-review` chưa thiết kế bảng riêng cho các toggle này | DD `ai-review` |
| Q3 | "Lưu và phát hành" là một hành động tổng hợp (transaction phía backend gộp mọi thay đổi) hay UI tự gọi tuần tự nhiều endpoint con? Ảnh hưởng trải nghiệm khi một phần thất bại giữa chừng. | Chưa thiết kế ở BD/DD module | DD `ai-review` |
| Q4 | Modal "Nhật ký phiên bản" hiển thị cho tất cả `feature_code` cùng lúc hay phải chọn một thẻ trước? | Prototype chỉ có một nút chung ở đầu khối, không rõ phạm vi | Prototype kế tiếp/DD screens |
| Q5 | Giới hạn tần suất hiển thị ở đây (`GetRateLimitConfig`) có trùng nguồn dữ liệu với ngưỡng đã đề xuất ở `02-bd/database/ai-review.md` mục 6 (10 lượt/giờ Solution Review, 3 phiên/giờ Mock Interview) hay là một bộ số khác theo đơn vị ngày/tuần như prototype hiển thị ("10 lượt/ngày", "5 phiên/tuần")? Hai nơi dùng đơn vị thời gian khác nhau, cần đối chiếu khi viết DD. | Chưa đối chiếu giữa BD module và RD màn hình | DD `ai-review` |

**Status:** DONE
**Summary:** Đã viết BD màn `admin_ai_config` tại `02-bd/screens/admin/admin_ai_config.md` — layout regions, component inventory (đã loại "Gợi ý theo bậc" và toggle "Chuyển giảng viên khi bí" theo quyết định đã chốt), screen states, APIs consumed (chỉ tên + BC sở hữu, không viết request/response), navigation, access rights (`AI_CONFIG`), và 5 câu hỏi mở thật sự chưa trả lời được (owner dữ liệu "Sinh testcase", cách lưu toggle, phạm vi "Lưu và phát hành", phạm vi modal nhật ký phiên bản, đơn vị thời gian giới hạn tần suất).
**Concerns/Blockers:** Không có blocker. Một lưu ý nhẹ: `02-bd/database/ai-review.md` chưa có `feature_code = GENERATE_TESTCASE` trong enum `prompt_templates` dù thẻ "Sinh testcase" hiển thị ở màn này — đã ghi thành Q1 câu hỏi mở, cần một phiên khác đối chiếu `problem-bank` trước khi viết DD.
