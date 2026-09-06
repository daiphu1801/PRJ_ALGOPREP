# RD — Màn `admin_overview` (Tổng quan khu Quản trị)

> Slug: `admin_overview`. Bounded Context: `identity` (chính, theo mẫu đã áp cho màn dashboard cùng loại
> `instructor_overview` [SoT: 01-rd/screens/teacher/instructor_overview.md:4-6]) — màn tổng hợp số liệu đọc
> thêm từ `problem-bank`, `judge-orchestration`, `ai-review`, nhưng không sở hữu logic nghiệp vụ của các
> module đó [SoT: Suy luận — bảng slug mục 7.3 chưa có dòng cho màn này nên chưa có Bounded Context được
> chốt; suy theo tiền lệ `instructor_overview`]. Actor: A3 (Quản trị viên).
>
> Đối chiếu prototype: `09-layoutBase/Admin - Tổng quan.dc.html` (509 dòng, bản dựng lại hoàn toàn mới thay
> cho bản "(Sáng)", đã sửa `state.theme` về `light` theo `DEC-2026-0824-dark-light-theme`
> [SoT: 06-plan/PROTOTYPE_DEBT.md:502-513]; xác minh dòng khởi tạo hiện tại
> [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:306]).
>
> **Slug này được bổ sung ngày 2026-08-25 qua đợt đối chiếu prototype khu Admin.** Trước đó bảng slug khu
> quản trị chỉ liệt kê 8 màn và không có `admin_overview`, trong khi prototype `Admin - Tổng quan.dc.html` đã tồn tại
> và là item đầu tiên của nav (`key: 'overview'`, `items: []`, `activeKey = 'overview'`)
> [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:359-362]. Cùng loại việc mà `06-plan/PROTOTYPE_DEBT.md` mục
> 6.2.b đã làm cho khu Giảng viên — phát hiện `instructor_overview` và `instructor_grading` là slug **phát
> sinh khi dựng prototype**, không nằm trong danh sách hạt giống, rồi đồng bộ lại bảng slug và tổng số màn
> [SoT: 06-plan/PROTOTYPE_DEBT.md:534-545]. Đã đồng bộ trong cùng đợt: dòng `admin_overview` thêm vào bảng
> [SoT: 01-rd/overview/system_survey.md:547], khu quản trị từ 8 lên 11 slug, tổng số màn dự kiến 27 → 30
> [SoT: 01-rd/overview/system_survey.md:559-565].
>
> File này mô tả hành vi và UX ở mức yêu cầu — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md`,
> chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Là **trang đích mặc định khi `ADMIN` đăng nhập** [SoT: 01-rd/screens/shared/auth.md:90 — Q3 đã chốt:
`STUDENT` → `my_progress`, `INSTRUCTOR` → `instructor_overview`, `ADMIN` → `admin_overview`], cho quản trị
viên một cái nhìn tổng hợp về sức khoẻ nội dung và mức sử dụng hệ thống (lượt nộp, người dùng, phân bố ngôn
ngữ, tỉ lệ kết quả chấm, phân bố độ khó bài toán, bài phổ biến, người dùng mới/quay lại) trước khi đi vào
từng màn vận hành cụ thể qua nav. Đây thuần là màn **tổng hợp và điều hướng** — mọi hành vi nghiệp vụ chi
tiết nằm ở 10 màn con mà nav trỏ tới.

Chính file này đóng tham chiếu treo đã được ghi nhận ở `auth.md`: quyết định Q3 điều hướng `ADMIN` tới
`admin_overview` nhưng màn đó chưa tồn tại trong RD, code frontend đang trỏ tạm `/admin/queue`
(`HOME_PATH_BY_ROLE`) [SoT: 01-rd/screens/shared/auth.md:90 — cảnh báo Q3b]. Sau file này, đích của `ADMIN`
có màn thật; việc sửa `HOME_PATH_BY_ROLE` là việc của code, không thuộc RD.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Điều hướng theo vai trò sau đăng nhập — `ADMIN` → `admin_overview` | (quyết định RD, chưa có mã `Fx-nn`) | `01-rd/screens/shared/auth.md:90` (Q3 đã chốt) |
| Ma trận phân quyền gác từng chức năng quản trị mà nav trỏ tới | F1-10 tới F1-12 | `01-rd/req/identity.md` — F1-10 tới F1-12 |
| Quản lý tài khoản người dùng (đích nav "Người dùng") | F1-13 | `01-rd/req/identity.md` — F1-13 |
| Nhật ký hệ thống — audit hành động quản trị (đích nav "Nhật ký hệ thống") | F1-14 | `01-rd/req/identity.md` — F1-14 |
| Phân loại bài toán theo độ khó (trục biểu đồ "Độ khó bài toán": Dễ/Trung bình/Khó) | F2-02 | `01-rd/req/problem-bank.md` — F2-02 |
| Giám sát hàng đợi và cụm judge engine (đích nav "Hàng đợi chấm") | F4-10 | `01-rd/req/judge-orchestration.md` — F4-10 |
| Cấu hình ngôn ngữ và giới hạn tài nguyên (đích nav "Ngôn ngữ và giới hạn") | F4-11 | `01-rd/req/judge-orchestration.md` — F4-11 |
| ~~Chấm lại quy mô lớn (đích nav "Chấm lại")~~ | — | **Đã loại khỏi phạm vi 2026-08-28** — `DEC-2026-0828-remove-rejudge-scope`. Đích nav "Chấm lại" cần gỡ khỏi màn khi build FE thật. |
| Cấu hình prompt/rubric AI (đích nav "Cấu hình AI") | F5-23 | `01-rd/req/ai-review.md` — F5-23 |
| Ngân sách và token AI (đích nav "Token AI") | F5-21, F5-25 | `01-rd/req/ai-review.md` — F5-21, F5-25 |
| Quản lý ngân hàng câu hỏi phỏng vấn (đích nav "Câu hỏi phỏng vấn") | F6-12 | `01-rd/req/identity.md` — F1-12 (danh sách Function `INTERVIEW_BANK_MANAGEMENT`), `01-rd/req/interview-bank.md` — F6-12. F6-11 đã loại khỏi phạm vi 2026-08-28, `DEC-2026-0828-remove-per-class-interview-set` |
| Ba ngôn ngữ nộp bài — trục dữ liệu của biểu đồ "Lượt nộp theo ngôn ngữ" | (giới hạn phạm vi, không phải mã `Fx-nn`) | `01-rd/overview/system_survey.md:570` |
| Given-When-Then của các màn con | US-A3-01 tới US-A3-05 | `01-rd/req/user_stories/a3_admin.md` |

**Ghi chú traceability — đây là phát hiện chính của file này:** không có mã `Fx-nn` nào và không có
`US-A3-nn` nào mô tả **bản thân màn tổng quan** hay bất kỳ khối thống kê trên đó. `US-A3-01` tới `US-A3-05`
đều mô tả màn con (ma trận quyền, quản lý người dùng, giám sát hàng đợi, cấu hình, chấm lại)
[SoT: 01-rd/req/user_stories/a3_admin.md]. Cụ thể, **8 trong 9 khối nội dung của màn không gán được mã nào**
— chi tiết ở mục 3, quy về Câu hỏi mở Q2 tới Q6. Đây là cùng dạng khoảng trống đã ghi nhận cho
`instructor_overview` [SoT: 01-rd/screens/teacher/instructor_overview.md:39-42]. **Cập nhật 2026-08-31: toàn
bộ Q1-Q8 đã đóng.** Q2 cấp mã `F1-29` cho cả 8 khối; Q5 xác nhận legend "Độ khó bài toán" là nhãn vẽ sai; Q3
bỏ ô tìm kiếm; Q4 hiện đủ 5 verdict; Q6 thêm một liên kết cho "Bài phổ biến nhất"; Q7 mở rộng phạm vi debt
i18n; Q8 thêm 3 trạng thái theo từng khối.

## 3. Trạng thái và cấu trúc màn (đối chiếu prototype)

Đối chiếu `09-layoutBase/Admin - Tổng quan.dc.html` — hành vi UX thật đã dựng, không suy diễn trừ khi ghi rõ
`[SoT: Suy luận]`. Cột "Mã" ghi mã `Fx-nn` gán được; `—` nghĩa là **không gán được mã nào trong các file `01-rd/req/*.md`**.

1. **Shell khu Admin, nav 5 nhóm / 11 đích** (HTML dòng 68-94, dữ liệu dòng 361-381): Tổng quan (không có
   item con, `items: []`) · Nội dung (Quản lý bài tập, Câu hỏi phỏng vấn) · Vận hành (Hàng đợi chấm, Chấm
   lại, Ngôn ngữ và giới hạn) · AI (Cấu hình AI, Token AI) · Hệ thống (Người dùng, Nhật ký hệ thống, Ma trận
   phân quyền) [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:361-381]. Nhóm có item con thì bấm vào chỉ mở/
   đóng nhóm (`toggleGroup`, `href: '#'`), không điều hướng; chỉ "Tổng quan" điều hướng trực tiếp vì không có
   con [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:386-389]. **Mã: —** cho chính cơ chế nav; từng đích thì
   có mã, xem bảng mục 2. **Hai đích "Quản lý bài tập" và "Câu hỏi phỏng vấn" trước đợt này không có slug
   nào**; đã viết RD, đổi tên bỏ tiền tố `admin_` và chuyển sang mục 7.0 (khu dùng chung A2+A3) là
   `problem_management` và `interview_question_management` — xem Câu hỏi mở Q1 (đã đóng).
2. **Thanh công cụ đầu trang** (HTML dòng 106-126): ô tìm kiếm "Tìm người dùng, bài toán…" (dòng 109), 3 nút
   biểu tượng không nhãn `layout-grid`/`moon`/`shield-check` (dòng 113-117, dữ liệu dòng 422 — không có
   handler, không có `href`), khối danh tính "Phú Đại / Quản trị viên" (dòng 121-122). **Mã: —** cho ô tìm
   kiếm: F2-11 là tìm kiếm **bài toán cho A1** [SoT: 01-rd/req/problem-bank.md — F2-11], không phải tìm kiếm liên thực
   thể (người dùng + bài toán) cho A3 — xem Câu hỏi mở Q3.
3. **Dải chỉ số tổng — 2 thẻ** (HTML dòng 128-144, dữ liệu dòng 426-429): "Tổng lượt nộp bài" = `9.416`
   (delta `+12,1%`) và "Người dùng hoạt động" = `1.284` (delta `+8,4%`), mỗi thẻ kèm sparkline
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:426-429]. **Mã: —.** Không có mã nào định nghĩa "người dùng
   hoạt động" (hoạt động trong bao lâu? loại trừ `DEACTIVATED` của F1-16 không?) hay khung thời gian so sánh
   delta — xem Câu hỏi mở Q2.
4. **"Lượt nộp theo ngôn ngữ"** (HTML dòng 146-160, dữ liệu dòng 431-435): legend Python · C++ · Java, 20 cột
   bar [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:431-435]. Ba ngôn ngữ khớp giới hạn phạm vi
   [SoT: 01-rd/overview/system_survey.md:570], nhưng **trục hoành 20 cột không có nhãn** — không rõ 20 cột là
   20 ngày, 20 tuần, hay 20 nhóm nào [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:155-159 — HTML không render
   nhãn trục]. **Mã: —** cho bản thân số liệu thống kê; xem Q2.
5. **"Kết quả chấm"** (HTML dòng 162-181, dữ liệu dòng 437-453): đồng hồ nửa vòng 40 vạch + legend 3 mức —
   `Accepted` 48% · `Sai / lỗi` 29% · `Time limit` 23%, tổng đúng 100%
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:437-439]. **Mã: —.** Tập verdict thật của hệ thống là
   AC/WA/TLE/CE/RE... [SoT: 01-rd/req/identity.md — F1-18 liệt kê bộ lọc verdict], nên nhóm "Sai / lỗi" đang
   gộp WA + CE + RE thành một, quy tắc gộp chưa được định nghĩa ở đâu — xem Câu hỏi mở Q4.
6. **"Độ khó bài toán"** (HTML dòng 184-209, dữ liệu dòng 455-468): cột đôi theo 3 mức Dễ / Trung bình / Khó,
   legend **"AI sinh"** và **"Giảng viên soạn"** [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:455-459]. Trục
   độ khó khớp F2-02 [SoT: 01-rd/req/problem-bank.md — F2-02]. **Mã: — cho trục phân loại nguồn gốc bài toán, và đây là
   phát hiện nghiêm trọng nhất của màn:** `problem-bank.md` chỉ có **F2-14 — AI hỗ trợ sinh testcase**
   [SoT: 01-rd/req/problem-bank.md — F2-14], **không có mã nào cho AI sinh chính bài toán/đề bài**, cũng không có mã
   nào nói bài toán lưu thuộc tính "ai là tác giả" để chia hai nhóm này. Xem Câu hỏi mở Q5.
7. **"Lượt nộp theo ngày"** (HTML dòng 211-234, dữ liệu dòng 471-475): lưới dot 7 cột T2→CN, mỗi cột 10 dot
   sáng/mờ theo giá trị; chân widget hiện `9.416 · Tổng lượt nộp` và `1.345 · TB/ngày`
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:471-475]. **Mã: —.** Xem Q2.
8. **"Lượt nộp theo tháng"** (HTML dòng 237-253, dữ liệu dòng 477-480): số tổng `9.416` + đường 6 tháng
   T1→T6 [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:477-480]. **Mã: —.** Xem Q2.
9. **"Bài phổ biến nhất"** (HTML dòng 255-272, dữ liệu dòng 482-487): 4 bài (Two Sum 295, Course Schedule
   285, Coin Change 265, Merge Intervals 245) kèm thanh tỉ lệ theo bài cao nhất
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:482-487]. Không có liên kết đi tiếp sang màn quản lý bài
   tập. **Mã: —.** Xem Q2 và Q6.
10. **"Người dùng mới / cũ"** (HTML dòng 274-296, dữ liệu dòng 489-493): cột đôi 6 tháng, legend "Người dùng
    mới" / "Quay lại" [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:489-493]. **Mã: —.** Phân biệt "mới" và
    "quay lại" đòi hỏi theo dõi lần truy cập/đăng nhập theo thời gian, một loại dữ liệu chưa có mã `Fx-nn`
    nào trong F1 — xem Câu hỏi mở Q2.
11. **Nhóm liên kết "KHÁC"** (HTML dòng 88-93, dữ liệu dòng 406-410): "Cài đặt" → `Admin - Ngôn ngữ và giới
    hạn.dc.html`, "Đăng nhập & Đăng ký" → màn `auth`, "Trang cá nhân" → màn `profile`
    [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:406-410]. **Cảnh báo:** "Cài đặt" trỏ tới màn
    `admin_language_config` (cấu hình hệ thống), không phải màn `settings` (tuỳ chọn cá nhân, F1-20
    [SoT: 01-rd/req/identity.md — F1-18, F1-20]) — hai thứ khác nhau, nhãn gây hiểu nhầm. Đề xuất sửa khi dựng UI thật;
    không cần mã mới.
12. **Theme** (HTML dòng 96-100, dữ liệu dòng 412-420): chỉ công tắc Sáng/Tối, lưu `localStorage` khoá
    `algoprep-admin-theme` [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:328], mặc định `light` đúng
    `DEC-2026-0824-dark-light-theme`. **Không có scaffold
    `data-ui-lang`/`data-lang`** trong toàn bộ file (xác minh: `data-ui-lang` không xuất hiện lần nào) —
    `DEC-2026-0824-i18n-vi-en` chỉ nêu tên 4 màn Admin còn thiếu scaffold (`Chấm lại`, `Cấu hình AI`, `Hàng
    đợi chấm`, `Ngôn ngữ và giới hạn`), **không có `Tổng quan`**
    [SoT: `DEC-2026-0824-i18n-vi-en`] — xem Câu hỏi mở Q7. **Cập nhật 2026-08-28:** `Chấm
    lại` (`admin_rejudge`) đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`), amendment ghi ở
    cùng decision entry — còn 3 màn thật sự cần scaffold.
13. **Không có trạng thái rỗng / lỗi / đang tải**: toàn bộ dữ liệu là hằng số tĩnh trả về ngay trong
    `renderVals()` [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:355-504], không có nhánh nào cho "chưa có
    dữ liệu", "một nguồn số liệu không phản hồi", hay "đang tải" — xem Câu hỏi mở Q8.
14. **Cảnh báo dữ liệu mẫu không nhất quán** (không phải lỗi yêu cầu, nhưng dễ bị sao chép nguyên vào BD/DD):
    cùng con số `9.416` được dùng cho ba phạm vi khác nhau — thẻ "Tổng lượt nộp bài" toàn hệ thống (dòng
    427), `weekTotal` tổng lượt nộp trong tuần (dòng 475), và `monthTotal` (dòng 480) — trong khi `dayAvg`
    = `1.345` chỉ khớp với phạm vi **tuần** (`1.345 × 7 ≈ 9.416`)
    [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:427, 475, 480]. Khi viết BD phải chốt rõ ba phạm vi này là
    ba đại lượng khác nhau.

## 4. Given-When-Then bổ sung ở mức màn

Vì **chưa có `US-A3-nn` nào phủ chính màn tổng quan** (mục 2), phần lớn mục này là suy luận ở mức UX dựa trên
hành vi đã dựng trong prototype — đánh dấu nguồn từng dòng.

- **Cho** tôi là `ADMIN` vừa đăng nhập thành công, **Khi** hệ thống điều hướng theo vai trò, **Thì** tôi được
  đưa thẳng tới `admin_overview`, không phải `my_progress` và không phải một màn vận hành cụ thể
  [SoT: 01-rd/screens/shared/auth.md:90 — Q3 đã chốt, không phải suy luận].
- **Cho** tôi đến `auth` từ một URL khu quản trị cần đăng nhập trước, **Khi** tôi đăng nhập xong, **Thì** hệ
  thống quay lại đúng URL gốc đó thay vì `admin_overview` [SoT: 01-rd/screens/shared/auth.md:90 — Q3 đã chốt].
- **Cho** tôi đang ở `admin_overview`, **Khi** tôi bấm một nhóm nav có item con (Nội dung / Vận hành / AI /
  Hệ thống), **Thì** nhóm đó mở ra danh sách item con và **màn hiện tại không đổi**; chỉ khi bấm một item con
  tôi mới được điều hướng [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:386-389 — `href: '#'` +
  `preventDefault()` cho nhóm có con].
- **Cho** tôi mở nav ở trạng thái thu gọn, **Khi** một nhóm đang mở, **Thì** danh sách item con bị ẩn (chỉ
  hiện khi nav mở rộng) [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:395 — `expanded: open && !collapsed`].
- **Cho** tôi là `ADMIN` nhưng ma trận phân quyền F1-10 không cấp cho vai trò của tôi một Function nào đó,
  **Khi** tôi mở `admin_overview`, **Thì** đích nav tương ứng và khối số liệu lấy từ Function đó không được
  hiển thị (hoặc hiển thị trạng thái không có quyền) thay vì lỗi kỹ thuật [SoT: Suy luận — suy từ nguyên tắc
  F1-10/F1-12 rằng mọi chức năng quản trị đều bị gác bởi ma trận quyền, `01-rd/req/identity.md` — F1-10, F1-12; prototype
  render nav tĩnh, không có nhánh theo quyền].
- **Cho** một nguồn số liệu (`problem-bank` / `judge-orchestration` / `ai-review`) không phản hồi, **Khi**
  tôi mở màn, **Thì** các khối còn lại vẫn hiển thị và chỉ khối bị ảnh hưởng báo lỗi — không sập cả màn
  [SoT: Suy luận — suy từ nguyên tắc "AI subsystem degrades gracefully" ở `CLAUDE.md` mục Rules áp cho khối
  số liệu đọc từ `ai-review`; prototype không có trạng thái lỗi, xem Q8].

## 5. Câu hỏi mở (toàn bộ 8 câu đã đóng 2026-08-31)

**Sửa định dạng 2026-09-03:** bảng này từng có header 6 cột trong khi cả 8 hàng chỉ có 5 ô, nên từ cột thứ
tư nội dung bị lệch sang trái một cột — kết luận rơi vào cột "Vì sao chưa trả lời được", trạng thái rơi vào
cột "Đề xuất". Bỏ cột "Vì sao chưa trả lời được" (không còn nghĩa khi mọi câu đã đóng) và đổi tên cột cuối
thành "Trạng thái" cho khớp dữ liệu thật. Nội dung từng ô không đổi.

| # | Câu hỏi | Ưu tiên | Đề xuất và kết luận | Trạng thái |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`admin_overview` chưa có dòng nào trong bảng slug, và hai đích nav "Quản lý bài tập" / "Câu hỏi phỏng vấn" cũng chưa có slug.~~ **ĐÃ CHỐT 2026-08-25:** `admin_overview` thêm vào mục 7.3 (đúng như đề xuất). Hai đích nội dung kia mở đợt đối chiếu riêng như đề xuất, kết quả: `admin_problem_management`/`admin_interview_question_management` được viết RD rồi đổi tên bỏ tiền tố, chuyển sang mục 7.0 (khu dùng chung A2+A3) — `DEC-2026-0825-shared-content-authoring-screens`. | — | Đã chốt. Chi tiết: `01-rd/screens/shared/problem_management.md`, `01-rd/screens/shared/interview_question_management.md`, `01-rd/screens/shared/problem_authoring.md`. | Đã đóng |
| Q2 | ~~**8/9 khối thống kê trên màn không gán được mã `Fx-nn` nào**...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** cấp **một** mã mới duy nhất — `F1-29` — cho cả 8 khối, không cấp mã rời. "Người dùng hoạt động" = đăng nhập trong 30 ngày gần nhất, loại trừ `DEACTIVATED` (F1-16). Khung thời gian delta và đơn vị trục biểu đồ ngôn ngữ để BD/DD quyết định. | — | Xem `01-rd/req/identity.md` (F1-29, sau F1-28) và `DEC-2026-0831-admin-overview-dashboard-stats`. Quy tắc gộp verdict ở "Kết quả chấm" **không nằm trong đợt chốt này** — vẫn là Q4, mức Trung bình. | Đã đóng |
| Q3 | ~~Ô tìm kiếm "Tìm người dùng, bài toán…"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** bỏ khỏi UI thật. | — | Không hứa tính năng chưa làm — xoá ô tìm kiếm này khi dựng UI Next.js thật. Xem `DEC-2026-0831-admin-overview-ui-decisions`. | Đã đóng |
| Q4 | ~~Widget "Kết quả chấm" gộp verdict thành 3 nhóm...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** hiện đủ **5 verdict thật** (AC/WA/TLE/CE/RE) thay vì gộp 3 nhóm. | — | Giữ tín hiệu CE (lỗi harness F3) và RE (lỗi sandbox) tách biệt. Đã ghi vào `01-rd/req/identity.md` (amendment F1-29). Xem `DEC-2026-0831-admin-overview-ui-decisions`. | Đã đóng |
| Q5 | ~~**Legend "AI sinh" / "Giảng viên soạn"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** đây là **nhãn dữ liệu mẫu vẽ sai** ở prototype, không phải tính năng thật — hệ thống không có AI sinh đề bài, chỉ có F2-14 (AI sinh testcase). | — | Khi dựng UI thật: đổi trục thành phân loại có thật (ví dụ tỉ lệ Accepted theo độ khó) `[SoT: Suy luận — BD chọn trục cụ thể]`. Không cấp mã F2 mới, không thêm thuộc tính "nguồn gốc bài toán". Xem `DEC-2026-0831-admin-overview-dashboard-stats`. | Đã đóng |
| Q6 | ~~Các widget... không có liên kết đi tiếp...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ "Bài phổ biến nhất" có liên kết "Xem tất cả", các widget biểu đồ còn lại giữ nguyên là tóm tắt. | — | Liên kết dẫn sang `problem_management`. Xem `DEC-2026-0831-admin-overview-ui-decisions`. | Đã đóng |
| Q7 | ~~`Admin - Tổng quan.dc.html` không có scaffold `data-ui-lang`/`data-lang`...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng phạm vi debt của `DEC-2026-0824-i18n-vi-en` thành "mọi màn khu Admin và khu Giảng viên" thay vì liệt kê tên từng file. | — | Xem `DEC-2026-0831-i18n-scope-expansion` (amendment, `DEC-2026-0824-i18n-vi-en` vẫn ACTIVE). Cùng câu hỏi với Q6 của `instructor_overview.md` — coi như đóng chung. | Đã đóng |
| Q8 | ~~Màn chưa có trạng thái rỗng/đang tải/lỗi...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** 3 trạng thái `empty`/`loading`/`error`, theo **từng khối riêng**. | — | Khối lấy số liệu từ `ai-review` chết không làm sập cả màn, khớp nguyên tắc AI degrade gracefully. Đã ghi vào `01-rd/req/identity.md` (amendment F1-29). Xem `DEC-2026-0831-admin-overview-ui-decisions`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Layout, lưới bố cục (3 cột / 2 cột / 3 cột), bảng màu, token theme, component biểu đồ cụ thể — thuộc BD
  (`02-bd/screens/admin/admin_overview.md`, chưa viết).
- Hợp đồng API tổng hợp số liệu (endpoint trả về thẻ chỉ số, phân bố ngôn ngữ, phân bố verdict, top bài
  toán, chuỗi thời gian ngày/tháng), cơ chế cache và tần suất làm mới — thuộc DD (`03-dd/api/identity.md` và
  các module liên quan, chưa viết).
- Công thức truy vấn/aggregate cụ thể của từng chỉ số — thuộc DD (`03-dd/logic/`), sau khi Q2 được chốt.
- Logic nghiệp vụ của 9 màn con mà nav trỏ tới (F1-10→14, F4-10/11, F5-21/23/25, F6-12) — thuộc RD/BD/DD
  của từng màn đó, không lặp lại ở đây.
- ~~Quyết định có cấp mã `Fx-nn` mới cho thống kê (Q2) và cho AI sinh đề bài (Q5) — chờ chủ dự án.~~
  **Đã chốt 2026-08-31** (ghi nhận 2026-09-05): Q2 cấp `F1-29` — một mã tổng hợp cho cả 8 khối thống kê
  (`DEC-2026-0831-admin-overview-dashboard-stats`); Q5 xác nhận legend "AI sinh / Giảng viên soạn" là
  **nhãn dữ liệu mẫu vẽ sai** ở prototype, hệ thống không có AI sinh đề bài nên **không cấp mã F2 mới**.

## 7. Tham chiếu

- `01-rd/screens/shared/auth.md:90` — Q3 đã chốt (`ADMIN` → `admin_overview`) và cảnh báo Q3b (tham chiếu
  treo mà file này đóng lại).
- `01-rd/overview/system_survey.md:547` — mục 7.3, dòng `admin_overview` (bổ sung 2026-08-25).
- `01-rd/overview/system_survey.md:559-565` — tổng 31 màn dự kiến sau đợt đối chiếu khu Admin, tách
  `class_management`/`class_assignments`, thêm `class_student_detail`, và loại bỏ `admin_rejudge`
  (2026-08-28).
- `01-rd/overview/system_survey.md:570` — giới hạn đúng ba ngôn ngữ nộp bài.
- `01-rd/req/identity.md` — F1-10 tới F1-14, F1-18, F1-20.
- `01-rd/req/problem-bank.md` — F2-02 (phân loại độ khó), F2-11 (tìm kiếm bài toán cho A1), F2-14 (AI sinh testcase, cơ chế nháp chờ Admin xác nhận) — mốc so sánh cho Q5.
- `01-rd/req/judge-orchestration.md` — F4-10, F4-11 (F4-09a→e đã loại khỏi phạm vi, `DEC-2026-0828-remove-rejudge-scope`).
- `01-rd/req/ai-review.md` — F5-21, F5-23, F5-25. `01-rd/req/interview-bank.md` — F6-12.
- `01-rd/req/user_stories/a3_admin.md` — `US-A3-01` tới `US-A3-05` (đều thuộc màn con).
- `01-rd/screens/admin/admin_queue_monitor.md` — khuôn mẫu cấu trúc file này.
- `01-rd/screens/teacher/instructor_overview.md` — màn dashboard cùng loại, tiền lệ xử lý khoảng trống
  traceability.
- `06-plan/PROTOTYPE_DEBT.md:502-513` — mục 6.1.c, lịch sử file prototype `Admin - Tổng quan.dc.html`.
- `06-plan/PROTOTYPE_DEBT.md:534-545` — mục 6.2.b, tiền lệ phát hiện slug phát sinh khi dựng prototype.
- `DEC-2026-0824-dark-light-theme` — quyết định theme Sáng/Tối.
- `DEC-2026-0824-i18n-vi-en` — quyết định song ngữ VI/EN.
- `09-layoutBase/Admin - Tổng quan.dc.html` — prototype đối chiếu chính của file này.
