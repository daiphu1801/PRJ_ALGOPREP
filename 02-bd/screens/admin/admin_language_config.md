# BD — Màn `admin_language_config` (Ngôn ngữ và giới hạn chấm)

> Trục: **màn hình** (không phải Bounded Context) — theo `CLAUDE.md` mục "Hai trục tài liệu". RD nguồn:
> `01-rd/screens/admin/admin_language_config.md`. Bounded Context chạm tới: `judge-orchestration` (chủ sở
> hữu `language_configs`) + `problem-bank` (chủ sở hữu giới hạn mặc định per-problem)
> [SoT: 01-rd/screens/admin/admin_language_config.md:4]. Actor: A3 (admin) duy nhất — không chia sẻ với A2
> như ba màn nội dung của `DEC-2026-0825-shared-content-authoring-screens` (màn này không nằm trong danh
> sách ba màn dùng chung).
>
> Bằng chứng layout: `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` (viết tắt "prototype" dưới đây).
> Đúng theo đính chính ở `CLAUDE.md` mục Directory map — `09-layoutBase` là SoT chỉ-đọc cho hành vi UI,
> trích dẫn kèm số dòng như RD đã làm.

## 1. Bối cảnh (không lặp RD — chỉ neo lại số liệu cần cho BD)

Màn cấu hình ba tham số theo ngôn ngữ (F2-10: hệ số thời gian; F4-11: giới hạn tài nguyên + 3 tham số
sandbox go-judge) cho đúng ba ngôn ngữ khoá cứng Java/C++/Python
[SoT: 01-rd/screens/admin/admin_language_config.md:12-13]. Bảng dữ liệu backing: `judge.language_configs`
(một dòng cố định mỗi ngôn ngữ, seed sẵn, A3 chỉ sửa giá trị — không tạo/xoá dòng)
[SoT: 02-bd/database/judge-orchestration.md:90-104]. Quyền chỉnh sửa gác bởi Function `LANGUAGE_CONFIG`
(tên đề xuất, chờ đối chiếu danh sách Function đầy đủ ở DD)
[SoT: 02-bd/security/judge-orchestration.md:74-77, 85].

## 2. Layout regions

Kế thừa khung Admin dùng chung cho mọi màn `/admin/*` (không phải riêng màn này) — mô tả ở đây để BD của
màn tự đủ, không cần đọc chéo BD khác:

| Vùng | Mô tả | Neo prototype |
| :--- | :--- | :--- |
| **Sidebar trái** (sticky, co giãn 72px/246px) | Logo + toggle thu gọn, nav nhóm theo actor (Tổng quan / Nội dung / **Vận hành** / AI / Hệ thống), khối trạng thái go-judge (thanh queue %), khối theme, khối tài khoản | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:68-144] |
| **Topbar sticky** | Tiêu đề "Ngôn ngữ và giới hạn chấm" + phụ đề "Hệ số thời gian theo ngôn ngữ áp dụng cho mọi bài toán", công tắc theme, nút CTA chính "Lưu thay đổi" | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:146-159] |
| **Khu nội dung chính** — grid 2 cột (`1.65fr` trái / `1fr` phải) | Cột trái: thẻ "Ngôn ngữ được hỗ trợ" (bảng). Cột phải: ba thẻ xếp dọc — "Giới hạn mặc định", "Sandbox", "Lưu ý khi đổi giới hạn" | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:161-244] |
| **Footer** | Phiên bản admin, trạng thái dịch vụ tổng quát, liên kết phụ (tài liệu API, trạng thái go-judge, nhật ký thay đổi, hỗ trợ) | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:246-259] |

Trên màn hình hẹp (dưới ngưỡng breakpoint — prototype không định nghĩa breakpoint cụ thể, `[SoT: Suy
luận]`): grid 2 cột nên rơi về 1 cột (thẻ bảng ngôn ngữ trước, ba thẻ phải xếp dưới), vì bảng có
`min-width: 720px` và tràn ngang (`overflow-x: auto`) trên desktop
[SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:170,163]. Số cụ thể của breakpoint không có trong
prototype — để ngỏ ở mục 7.

## 3. Component inventory

### 3.1. Thẻ "Ngôn ngữ được hỗ trợ" (cột trái)

Bảng 6 cột, đúng 3 dòng (Python 3, C++ 17, Java 21) — không có hàng "Thêm ngôn ngữ" (đã loại khỏi phạm vi
theo `06-plan/PROTOTYPE_DEBT.md` mục 1.1, Phương án A, dẫn chiếu tại `02-bd/architecture/harness.md:14-16`).

| Cột | Nội dung hiển thị | Nguồn dữ liệu (khái niệm) | Neo prototype |
| :--- | :--- | :--- | :--- |
| Ngôn ngữ | Chip 2 ký tự (PY/C+/JV) + tên đầy đủ + tên phiên bản trình biên dịch (compiler string dưới tên, ví dụ "CPython 3.11.6") | `language_configs.language` + hằng số hiển thị tên/compiler (không phải cột DB — xem mục 7 câu hỏi mở) | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:181-189, 402-405] |
| Trình biên dịch | Nhãn `go-judge #<id>` — mã định danh môi trường chấm nội bộ go-judge cho ngôn ngữ đó | `[SoT: Suy luận]` — không phải cột đã có ở `judge.language_configs` (mục 1.5 database judge-orchestration); có thể là ID sandbox image riêng, cần làm rõ ở DD | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:190, 403-405] |
| Hệ số TG | Số dạng `x<n>,<m>` (ví dụ `x3,0`), tô đậm nếu khác `x1,0` | `language_configs.time_limit_multiplier` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:191, 403-405, 411] |
| Giới hạn TG | Số mili-giây tuyệt đối hiển thị **chỉ đọc** (kết quả `time_limit_ms mặc định × hệ số`, không phải trường tự nhập riêng trong bảng này) | Tính toán hiển thị, không phải cột ghi trực tiếp — xem mục 7 câu hỏi mở về nguồn `time_limit_ms` mặc định | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:192, 403-405] |
| Bộ nhớ | Số MB hiển thị chỉ đọc trong bảng này | Tương tự — dẫn xuất từ giới hạn mặc định × `memory_limit_multiplier` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:193, 403-405] |
| Bật | Toggle switch (on/off) | Cột chưa có tên chính thức ở `judge.language_configs` mục 1.5 — cần một cột `enabled` bổ sung (xem mục 7) | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:194-198, 407] |

Chỉ **hệ số thời gian** và **bật/tắt** là các trường thực sự sửa được trực tiếp trong bảng này theo state
của prototype (`state.enabled`, cột `factor` không có `onChange` riêng trong bản mẫu — nhưng đây là hạn
chế của bản mẫu tĩnh, không phải chủ đích thiết kế: F2-10/F4-11 đòi hệ số phải sửa được, nên ô "Hệ số TG"
phải là input chỉnh sửa được ở bản Next.js thật, không chỉ hiển thị)
[SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:267-272, 406-414].

### 3.2. Thẻ "Giới hạn mặc định" (cột phải, thẻ 1/3)

Danh sách 4 dòng, mỗi dòng: nhãn + mô tả phụ (nhỏ, xám) + giá trị chỉ đọc (bản mẫu tĩnh không có form sửa
trong thẻ này):

| Nhãn | Mô tả phụ | Giá trị mẫu | Neo prototype |
| :--- | :--- | :--- | :--- |
| Giới hạn thời gian | "Nhân với hệ số ngôn ngữ" | `1.000 ms` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:417] |
| Giới hạn bộ nhớ | "Áp dụng cho mọi ngôn ngữ" | `256 MB` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:418] |
| Kích thước output | "Vượt quá sẽ báo Output limit" | `64 KB` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:419] |
| Thời gian biên dịch | "Không tính vào giới hạn chạy" | `10 s` | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:420] |

**Đây là các giá trị mặc định TOÀN HỆ THỐNG, dùng khi một bài toán không tự khai riêng**
[SoT: 01-rd/screens/admin/admin_language_config.md:32-33]. Cần sửa được (bản mẫu không thể hiện form sửa,
nhưng đây là màn cấu hình nên phải cho sửa) — chốt widget cụ thể (input số + đơn vị) ở DD. Nguồn lưu trữ
cho 4 giá trị này **chưa có bảng DB xác nhận** — xem mục 7 câu hỏi mở, không tự suy diễn thêm.

### 3.3. Thẻ "Sandbox" (cột phải, thẻ 2/3)

Ba dòng toggle, **một bộ dùng chung cho cả ba ngôn ngữ** (không phải 3 dòng × 3 ngôn ngữ) theo đúng state
phẳng của bản mẫu (`state.sb = { net, fork, stderr }`, không khoá theo `language`)
[SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:271, 424-428]:

| Tiêu đề | Mô tả phụ | Giá trị mặc định mẫu | Mã RD |
| :--- | :--- | :--- | :--- |
| Cho phép truy cập mạng | "Luôn tắt trong sandbox chấm bài" | tắt | F4-11 tham số 1/3 |
| Giới hạn tiến trình con | "Tối đa 60 tiến trình" | bật | F4-11 tham số 2/3 |
| Trả stderr cho người học | "Cắt còn 2 KB đầu" | bật | F4-11 tham số 3/3 |

**Mâu thuẫn cần chốt trước khi vào DD** (không tự sửa lặng lẽ theo quy tắc "phát hiện lệch, đề xuất, để
chủ dự án quyết"): `02-bd/database/judge-orchestration.md` mục 1.5 đã khoá ba cột sandbox
(`network_access_enabled`, `max_child_processes`, `return_stderr_to_student`) là **cột trên từng dòng
`language_configs` — nghĩa là per-language**, trong khi bằng chứng UI ở đây là **một bộ toggle chung cho cả
ba ngôn ngữ**, không có UI chọn ngôn ngữ trước khi bật/tắt sandbox
[SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:222-237, 271]. Hai khả năng:
1. Giữ schema per-language (đã chốt), UI thật phải tách sandbox thành 3 dòng × 3 ngôn ngữ (bản mẫu tĩnh vẽ
   thiếu) — bảng này (mục 3.3) phải sửa lại thành ma trận 3×3 khi build Next.js thật.
2. Sandbox vốn dĩ là tham số cấp **go-sandbox instance** (giống nhau cho mọi lần chạy bất kể ngôn ngữ, vì
   go-judge cách ly ở mức cgroup/namespace không phân biệt ngôn ngữ) — nên gộp về một bảng cấu hình toàn
   cục (ví dụ `judge.sandbox_defaults`, một dòng duy nhất), tách khỏi `language_configs`; chỉ
   `time_limit_multiplier`/`memory_limit_multiplier` mới thực sự cần per-language.

Đề xuất của BD màn này (nghiêng về phương án 2, vì lý do kỹ thuật: cgroup/namespace mà go-sandbox dựng cho
một lần gọi `JudgeExecutionPort.run()` không đọc `language` để quyết định giới hạn tiến trình con hay chặn
mạng — ba tham số này là thuộc tính của **lệnh chạy**, không phải thuộc tính của **ngôn ngữ**)
[SoT: Suy luận] — nhưng đây là **thay đổi schema đã chốt** của `judge-orchestration`
(`02-bd/database/judge-orchestration.md` mục 1.5), nên không tự sửa file đó ở đây; ghi nhận là câu hỏi mở
bắt buộc có quyết định (`DEC-`) trước khi viết DD của cả hai module (mục 7, câu hỏi Q1).

### 3.4. Thẻ "Lưu ý khi đổi giới hạn" (cột phải, thẻ 3/3)

Đoạn text tĩnh, không có input. Nội dung prototype: "Thay đổi hệ số hoặc giới hạn không tự áp dụng cho
lượt nộp cũ. Sau khi lưu, tạo một phiên chấm lại nếu muốn kết quả cũ được tính theo cấu hình mới." với liên
kết "chấm lại" trỏ sang `Admin - Chấm lại.dc.html`
[SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:239-241].

**Nội dung này đã lỗi thời so với quyết định đang ACTIVE** — `DEC-2026-0828-remove-rejudge-scope` loại bỏ
hoàn toàn tính năng chấm lại (`admin_rejudge`) khỏi phạm vi, và RD của chính màn này đã cập nhật: "đổi giới
hạn chỉ áp dụng cho lượt nộp mới từ lúc đổi trở đi... không có luồng chấm lại để đồng bộ"
[SoT: 01-rd/screens/admin/admin_language_config.md:39-41]. **Chốt cho bản Next.js thật:** bỏ hẳn liên kết
"chấm lại", đổi nội dung thẻ thành thông báo một chiều — ví dụ "Thay đổi hệ số hoặc giới hạn tài nguyên chỉ
áp dụng cho các lượt nộp mới kể từ thời điểm lưu. Các lượt nộp trước đó giữ nguyên kết quả đã chấm." Không
cần quyết định mới (đã có `DEC-2026-0828-remove-rejudge-scope` bao trùm), chỉ là áp dụng đúng quyết định đã
có vào nội dung màn này — nêu ở đây để không lặp lại lỗi debt của prototype khi build FE thật.

## 4. Screen states

| State | Mô tả | Ghi chú |
| :--- | :--- | :--- |
| `loading` | Khung skeleton cho bảng + 3 thẻ phải trong lúc gọi API lấy `language_configs` và giới hạn mặc định | `[SoT: Suy luận]` — chuẩn loading pattern, prototype tĩnh không thể hiện |
| `ready` (mặc định) | Dữ liệu đã tải, mọi input chỉnh sửa được, nút "Lưu thay đổi" ở trạng thái tĩnh (không nhấn mạnh) | [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:158] |
| `dirty` | Có ít nhất một trường (hệ số, bật/tắt ngôn ngữ, giới hạn mặc định, toggle sandbox) đã đổi so với dữ liệu tải về, chưa lưu | `[SoT: Suy luận]` — cần để nút "Lưu thay đổi" phân biệt được với `ready`, tránh nhấn nhầm khi không có gì đổi |
| `saving` | Đang gọi API lưu, khoá toàn bộ input + nút, hiện spinner trên nút | `[SoT: Suy luận]` |
| `save_success` | Toast xác nhận, quay về `ready` với dữ liệu vừa lưu, `updated_at`/`updated_by` (mục database 1.5) cập nhật hiển thị nếu có hiển thị | `[SoT: Suy luận]` |
| `save_error` | Toast lỗi, giữ nguyên input người dùng vừa nhập (không revert), cho phép sửa và lưu lại | `[SoT: Suy luận]` — lỗi có thể do validate hệ số âm/0, hoặc do quyền `LANGUAGE_CONFIG` bị thu hồi giữa phiên |
| `disable_confirm` (mở) | Khi A3 tắt một ngôn ngữ đang bật — RD không nói có cần hộp thoại xác nhận hay không, chỉ nói hệ quả (ngôn ngữ đó biến mất khỏi lựa chọn ở `problem_detail` cho lượt nộp mới) | Xem mục 7 câu hỏi mở |

## 5. API tiêu thụ (chỉ liệt kê tên + Bounded Context sở hữu — hợp đồng request/response thuộc DD)

| Hành động màn hình | Bounded Context sở hữu | Ghi chú phạm vi |
| :--- | :--- | :--- |
| Tải cấu hình 3 ngôn ngữ (hệ số, bật/tắt, sandbox) | `judge-orchestration` | Đọc `judge.language_configs` — hợp đồng ở `03-dd/api/judge-orchestration.md` (chưa viết) |
| Lưu thay đổi hệ số/bật-tắt/sandbox | `judge-orchestration` | Gác bởi Function `LANGUAGE_CONFIG` — hợp đồng ở `03-dd/api/judge-orchestration.md` (chưa viết) |
| Tải/lưu 4 giới hạn mặc định (thẻ 3.2) | Chưa xác định — `judge-orchestration` hoặc `problem-bank` | Xem câu hỏi mở Q2 mục 7 — chưa chốt module sở hữu nên chưa gán API |
| Trạng thái go-judge (thanh queue % ở sidebar) | `judge-orchestration` (cross-cutting, dùng chung mọi màn `/admin/*`) | Không riêng của màn này — kế thừa từ khung Admin, xem `admin_queue_monitor` |

Không có API nào của màn này chạm `ai-review`/`interview-bank`/`identity` ngoài việc đọc JWT để lấy `role`
phục vụ RBAC (mục 6).

## 6. Navigation và access rights

- **Route**: `/admin/language-config` (đề xuất, theo quy ước prefix thật cho khu vực Admin —
  `DEC-2026-0825-frontend-base-architecture` điểm (1): route group `(admin)` + prefix `/admin/*`).
  `[SoT: Suy luận]` — RD/prototype không nêu route string, chỉ nêu file mockup; slug chuẩn `admin_language_
  config` gợi ý cấu trúc URL trên.
- **Vào từ**: mục nav "Vận hành" (nhóm `VH`) → mục con "Ngôn ngữ và giới hạn", không có badge số
  [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:307-311]. Cùng nhóm còn "Hàng đợi chấm" (badge
  14) và "Chấm lại" (badge 2) trong bản mẫu — **mục "Chấm lại" phải bị xoá khỏi nav thật**, không phải lỗi
  cần sửa ở màn này mà là hệ quả đã chốt của `DEC-2026-0828-remove-rejudge-scope` áp dụng cho toàn bộ khung
  Admin, không lặp lại thiết kế nav ở đây.
- **Access rights**:
  - **Xem** cấu hình: chỉ role `ADMIN` — không có RD nào cho phép `INSTRUCTOR` xem màn này (khác ba màn
    dùng chung của `DEC-2026-0825-shared-content-authoring-screens`, màn này không nằm trong danh sách đó).
  - **Sửa** cấu hình (hệ số, bật/tắt, sandbox, giới hạn mặc định): gác bởi Function `LANGUAGE_CONFIG` (tên
    đề xuất), chỉ `ADMIN` [SoT: 02-bd/security/judge-orchestration.md:74-77]. Kiểm ở tầng application theo
    nguyên tắc chung của `identity` (`@PreAuthorize` khớp `FUNCTION:ACTION`, không chỉ ẩn nút trên UI)
    [SoT: 02-bd/security/identity.md:25-26].
  - Middleware tầng route đọc `ROLE_BY_AREA` cho prefix `/admin/*`
    [SoT: .nexa/control/decision-registry.md — DEC-2026-0825-frontend-base-architecture điểm (1)] chặn
    trước khi tới component; kiểm Function `LANGUAGE_CONFIG` là lớp thứ hai ở tầng application cho riêng
    hành động ghi, không thay thế nhau.
- **i18n**: màn này thuộc nhóm 3 màn Admin còn thiếu scaffold `data-ui-lang`/`data-lang` theo
  `DEC-2026-0824-i18n-vi-en` (đã sửa: chỉ còn 3 màn sau khi `Chấm lại` bị loại khỏi phạm vi) — cần bổ sung
  khi build Next.js thật, không phải việc của BD này nhưng ghi nhận lại để DD/coding không bỏ sót.
- **Theme**: sáng/tối theo `DEC-2026-0824-dark-light-theme`, đã có toggle trong bản mẫu
  [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:119-123, 153-157].

## 7. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Ba tham số sandbox (mạng, giới hạn tiến trình con, stderr) là **per-language** (đã chốt ở `02-bd/database/judge-orchestration.md` mục 1.5) hay **toàn cục dùng chung** (bằng chứng UI ở mục 3.3)? | Hai tài liệu SoT khác nhau (schema đã chốt vs. bằng chứng UI của chính màn này) mâu thuẫn nhau | Nghiêng về toàn cục (lý do kỹ thuật ở mục 3.3) — cần một `DEC-` mới sửa `02-bd/database/judge-orchestration.md` mục 1.5 trước khi viết DD của cả `judge-orchestration` lẫn màn này | Chủ dự án |
| Q2 | Bốn giá trị "Giới hạn mặc định" (thẻ 3.2) lưu ở bảng nào? `problems.time_limit_ms`/`memory_limit_mb`/`max_output_size_kb` hiện là cột bắt buộc mỗi bài (`02-bd/database/problem-bank.md` mục 1.1), không có nullable-fallback rõ ràng như `max_submissions_per_hour` (dòng 24, NULL = dùng mặc định hệ thống) | Chưa có bảng "giá trị mặc định toàn cục" nào được chốt ở BD `judge-orchestration` lẫn `problem-bank` | Thêm bảng đơn dòng (singleton) — ví dụ `judge.grading_defaults` — hoặc mở rộng `problems.*` thành nullable với fallback đọc từ đây; cả hai đều là thay đổi schema, cần `DEC-` trước khi DD | Chủ dự án + owner của `problem-bank` |
| Q3 | Cột "Trình biên dịch" hiển thị `go-judge #<id>` — đây là ID image/sandbox riêng theo ngôn ngữ hay chỉ là chuỗi trang trí tĩnh của bản mẫu? | Không có cột tương ứng ở `judge.language_configs` mục 1.5 | Nếu là ID thật, thêm cột `sandbox_image_ref` hoặc tương tự vào `language_configs`; nếu chỉ để hiển thị, bỏ khỏi UI thật hoặc thay bằng thông tin hữu ích hơn (ví dụ thời điểm `updated_at`) | Chủ dự án |
| Q4 | Tắt một ngôn ngữ có cần hộp thoại xác nhận (vì ảnh hưởng ngay tới học viên đang chọn ngôn ngữ đó ở `problem_detail`) hay chỉ toggle tức thì? | RD chỉ mô tả hệ quả (F3-13 side), không mô tả UX xác nhận | Đề xuất có xác nhận (dialog liệt kê hệ quả ngắn gọn) trước khi submit `save`, vì đây là thao tác ảnh hưởng toàn hệ thống, không phải thay đổi cục bộ một bản ghi | Chủ dự án |
| Q5 | Cột "Hệ số TG" trong bảng 3.1 — bản mẫu tĩnh không có `onClick`/input sửa riêng cho ô này (chỉ có toggle "Bật" có `onClick`) — xác nhận đây là thiếu sót cần bổ sung ở FE thật, không phải chủ đích ẩn khỏi UI | Không có bằng chứng khác trong prototype về input sửa hệ số | Bổ sung input số trực tiếp trong ô (inline edit) khi build Next.js — khớp đúng mục đích màn theo RD F2-10/F4-11 (cho phép sửa hệ số) | Chủ dự án (xác nhận, không phải câu hỏi thiết kế) |
| Q6 | Breakpoint responsive cho grid 2 cột (mục 2) — số cụ thể | Prototype không định nghĩa | Theo token responsive chung của toàn hệ thống khi design system chốt (`DEC-2026-0824-dark-light-theme` rationale — phần còn lại của design system vẫn hoãn) | Chủ dự án khi chốt design system |

## 8. Tham chiếu

- `01-rd/screens/admin/admin_language_config.md` — RD nguồn của màn.
- `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` — prototype, mọi trích dẫn dòng ở trên.
- `02-bd/database/judge-orchestration.md` mục 1.5 — schema `language_configs`.
- `02-bd/database/problem-bank.md` mục 1.1 — `time_limit_ms`/`memory_limit_mb`/`max_output_size_kb`,
  `max_submissions_per_hour` (mẫu nullable-fallback tham chiếu ở Q2).
- `02-bd/security/judge-orchestration.md` mục 5 — Function `LANGUAGE_CONFIG`/`JUDGE_MONITORING`.
- `02-bd/architecture/harness.md` mục 1, 3 — khoá phạm vi ba ngôn ngữ, loại bỏ "Thêm ngôn ngữ".
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en`,
  `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0831-judge-orchestration-ops-details` (dẫn theo RD).
- `.nexa/control/dependency-map.md` mục 5 — bảng "screen chạm Bounded Context nào" (`judge_monitor`/
  `ai_config` xếp "across everything" cho khu Admin nói chung; màn này hẹp hơn, chỉ 2 context).

**Status:** DONE_WITH_CONCERNS
**Summary:** Đã viết BD cho màn `admin_language_config` tại `02-bd/screens/admin/admin_language_config.md` — layout regions, component inventory (bảng ngôn ngữ + 3 thẻ phải), 7 screen states, API tiêu thụ (chỉ tên + BC sở hữu), navigation, access rights (Function `LANGUAGE_CONFIG`, chỉ ADMIN).
**Concerns/Blockers:** Phát hiện một mâu thuẫn thật giữa hai SoT khi viết mục 3.3 — schema `judge.language_configs` đã chốt trước đó (per-language sandbox) đối chọi với bằng chứng UI của chính màn này (một bộ sandbox toggle dùng chung cho cả ba ngôn ngữ). Không tự sửa `02-bd/database/judge-orchestration.md`, chỉ ghi nhận thành Q1 (mục 7) kèm đề xuất nghiêng về phương án toàn cục — cần chủ dự án quyết trước khi hai module viết DD. Ba câu hỏi mở khác (Q2 nguồn dữ liệu "Giới hạn mặc định", Q3 ý nghĩa cột "Trình biên dịch go-judge #id", Q4 có cần confirm-dialog khi tắt ngôn ngữ) cũng chặn phần DD tương ứng, không chặn phần còn lại của BD này.
