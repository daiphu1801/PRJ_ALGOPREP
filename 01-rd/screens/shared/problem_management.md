# RD — Màn `problem_management` (Quản lý bài tập — bảng quản trị nội dung)

> Slug: `problem_management` (đổi tên từ `admin_problem_management` — quyết định 2026-08-25, xem dưới).
> Bounded Context: `problem-bank` (F2). **Actor: A2 và A3 — màn dùng chung, phạm vi dữ liệu theo quyền.**
> Đã chốt ngày 2026-08-25 (owner instruction, kết thúc Câu hỏi mở Q1 cũ): mount ở cả `/instructor/problems`
> và `/admin/problems`, cùng một view/BD/DD, A2 chỉ thấy bài của mình/lớp mình, A3 thấy toàn bộ kho — cơ chế
> đã có tiền lệ ở ma trận phân quyền `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` (`01-rd/req/identity.md` — F1-12), cùng hàng với
> `INTERVIEW_BANK_MANAGEMENT` (`01-rd/req/identity.md` — F1-12). Ghi quyết định:
> `DEC-2026-0825-shared-content-authoring-screens`.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Quản lý bài tập.dc.html` (618 dòng, dựng trong shell Admin —
> khu Giảng viên chưa có prototype tương ứng, xem `06-plan/PROTOTYPE_DEBT.md` mục 7.3.a). File này mô tả
> **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/problem-bank.md` (mục F2), chỉ
> trỏ tới và bổ sung phần đặc thù của màn.
>
> **Lịch sử slug:** bổ sung 2026-08-25 với tên `admin_problem_management` (màn chưa từng có trong
> `system_survey.md` mục 7.3 dù prototype đã dựng thật), đổi tên cùng ngày sang `problem_management` và
> chuyển từ `01-rd/screens/admin/` sang `01-rd/screens/shared/` sau khi chốt dùng chung hai khu vực.
>
> **Đây là màn cha của `problem_authoring`.** Trong prototype, cả ba lối vào việc soạn đề đều từ màn này trỏ
> sang `Admin - Soạn đề bài.dc.html`: nút "Bài tập mới" ở thanh tiêu đề
> [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:160], tiêu đề bài toán trong bảng
> [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:220], và nút sửa cuối mỗi dòng
> [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:228]. Không có lối vào `problem_authoring` nào khác
> trong prototype.

## 1. Mục đích màn hình

Bảng quản trị nội dung của ngân hàng bài toán: người quản lý nội dung xem toàn bộ bài tập trong kho, tìm và
lọc theo độ khó/trạng thái, theo dõi sức khoẻ từng bài (số lượt nộp, tỉ lệ AC, số testcase, lần sửa cuối),
xử lý theo lô, và mở luồng tạo/sửa đề bài
[SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:151-152, 209-232].

Đây là **màn điều phối**, không phải màn đặc tả: việc soạn nội dung bài toán thật (Markdown + LaTeX, chữ ký
hàm ba ngôn ngữ, chiến lược so khớp, testcase kể cả lịch sử phiên bản bộ testcase F2-09) thuộc
`problem_authoring` (F2-01 → F2-04, F2-05 → F2-09, F2-14) — xem `01-rd/screens/shared/problem_authoring.md`.
`testcase_management` không còn là slug riêng, đã gộp vào tab Testcase của `problem_authoring` (chốt
2026-08-25, cùng đợt với Q1).

### 1.1. Phân biệt với `problem_list` — không trùng lặp

Hai màn cùng hiển thị một danh sách bài toán nhưng khác mục đích, khác actor, khác mã chức năng:

| Tiêu chí | `problem_list` | `problem_management` (màn này) |
| :--- | :--- | :--- |
| Actor | A1 — người học [SoT: 01-rd/req/problem-bank.md — F2-11] | A2 và A3 (dùng chung — đã chốt) |
| Mã chức năng | F2-11 — tìm kiếm và lọc bài toán theo chủ đề, độ khó, **trạng thái đã giải** [SoT: 01-rd/req/problem-bank.md — F2-11] | Vòng đời F2-15; chưa có mã phủ trọn cho xoá/xử lý theo lô/CSV/thống kê — xem mục 2, Q3-Q5 |
| Mục đích | Ngân hàng bài toán để chọn bài giải [SoT: 01-rd/overview/system_survey.md:476] | Bảng quản trị nội dung: tạo/sửa/xuất bản/ẩn/xoá |
| Phạm vi dữ liệu | Chỉ bài **đã xuất bản** — prototype ghi rõ "24 bài đang hiển thị ở Ngân hàng bài toán" trên tổng 27 [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:152] và "Đúng bằng số bài trang người học thấy" [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:480] | Toàn bộ kho, cả hai trạng thái `Đã xuất bản` và `Chưa xuất bản` (F2-15) |
| Khối riêng cho lớp | Có — bài giao theo lớp hiển thị thành khối riêng cạnh bảng chính (F2-12) [SoT: 01-rd/req/problem-bank.md — F2-12] | Không có trong prototype |

**Phần trùng về hình thức:** cả hai đều có ô tìm kiếm, bộ lọc độ khó (Easy/Medium/Hard), phân trang. Trùng
này là trùng **thành phần UI**, không phải trùng yêu cầu — bộ lọc thứ hai của màn này là *trạng thái xuất
bản* (Tất cả / Đã xuất bản / Chưa xuất bản, F2-15) [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:585],
trong khi bộ lọc thứ hai của `problem_list` là *trạng thái đã giải của chính người học* (F2-11)
[SoT: 01-rd/req/problem-bank.md — F2-11]. Hai trục lọc khác hẳn nhau về nghiệp vụ.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Khối trên màn | Mã | Nguồn |
| :--- | :--- | :--- |
| Lối vào tạo bài mới / sửa bài (nút "Bài tập mới", tiêu đề bài, nút sửa) | F2-01, F2-02, F2-03, F2-04 | `01-rd/req/problem-bank.md` — F2-01 → F2-04 |
| Cột "TC" (số testcase) trên mỗi dòng, mục "Chưa có testcase" ở khối Bài cần chú ý | F2-05, F2-06 | `01-rd/req/problem-bank.md` — F2-05, F2-06 |
| Cột "Độ khó" và bộ lọc độ khó | F2-02 | `01-rd/req/problem-bank.md` — F2-02 |
| Cột "Chủ đề", khối "Phân bố theo chủ đề", bulk action "Gán chủ đề" | F2-02 | `01-rd/req/problem-bank.md` — F2-02 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-01`, `US-A2-02`) |
| Cơ chế gác quyền cho màn | F1-10, F1-11, F1-12 (`PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`) | `01-rd/req/identity.md` — F1-10 → F1-12 |

**Các khối không gán được mã `Fx-nn` nào** (không tự phát minh mã mới — đưa vào mục 5): xoá bài toán, xử lý
theo lô, Nhập CSV / Xuất CSV, nhân bản bài toán, 4 chỉ số tổng của kho, khối "Bài cần chú ý". Chi tiết ở
Q3 → Q5. **Trạng thái vòng đời bài toán đã có mã** — `F2-15` (chốt 2026-08-30, xem Q2), hai trạng thái
`Đã xuất bản` / `Chưa xuất bản`, không còn trạng thái thứ ba `Đã ẩn`.

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Quản lý bài tập.dc.html`:

1. **Vị trí trong điều hướng khu Admin** (route `/admin/problems`; khu Giảng viên mount cùng view ở
   `/instructor/problems`, xem header) — nav cấp 1 trong nhóm "Nội dung", `activeKey = 'problems'`, badge
   số lượng `'27'`, cạnh `interview_question_management` (badge `'148'`)
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:343, 349-352]. Sidebar còn một thẻ "Kho bài" hiển thị
   `27` và thanh tiến độ "Đã xuất bản 89%"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:119-128].
2. **Thanh tiêu đề** — tên màn, dòng phụ "27 bài · 24 bài đang hiển thị ở Ngân hàng bài toán", nút "Nhập
   CSV", nút chính "Bài tập mới" trỏ sang `problem_authoring`
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:151-160]. "Nhập CSV" **không có mã** — xem Q4.
3. **4 chỉ số tổng của kho** — Tổng bài tập, Đã xuất bản (kèm chú thích "Đúng bằng số bài trang người học
   thấy"), Chưa xuất bản (nhãn prototype ghi "Bản nháp", nay đổi tên theo `F2-15`; kèm "1 bài chưa có
   testcase"), Tỉ lệ AC trung bình (tính trên 90 ngày gần nhất)
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:478-483]. 3/4 chỉ số có mã (`F2-15`); tỉ lệ AC trung
   bình vẫn không có mã — xem Q5.
4. **Thanh lọc và tìm kiếm** — ô tìm "theo mã bài hoặc tiêu đề"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:181], tab độ khó `Tất cả / Easy / Medium / Hard`, tab
   trạng thái `Tất cả / Đã xuất bản / Chưa xuất bản` (nhãn prototype ghi "Bản nháp"), và bộ đếm kết quả
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:584-588]. Lọc và tìm đều reset về trang 1
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:473, 587].
   **Đã giải quyết (2026-08-30):** bản trước của file này cảnh báo tab trạng thái thiếu giá trị thứ tư cho
   `Đã ẩn`. Theo `F2-15` (hai trạng thái, không có `Đã ẩn` riêng), 3 tab của prototype (`Tất cả` / `Đã xuất
   bản` / `Chưa xuất bản`) đã phủ đủ — không cần bổ sung tab nào nữa.
5. **Thanh hành động theo lô** — chỉ hiện khi đã chọn ít nhất một dòng, gồm 5 hành động "Xuất bản / ẩn",
   "Đổi độ khó", "Gán chủ đề", "Nhân bản", "Xuất CSV" cộng nút "Xoá"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:196-206, 591]. "Đổi độ khó" và "Gán chủ đề" là F2-02
   ở dạng thao tác lô; ba hành động còn lại và "Xoá" **không có mã** — xem Q2, Q3, Q4.
6. **Bảng bài toán** — 10 cột: ô chọn, Mã, Tiêu đề (liên kết sang `problem_authoring`), Chủ đề, Độ khó,
   Trạng thái, Lượt nộp, AC, "TC · Sửa" (số testcase và thời điểm sửa cuối gộp một cột), và nhóm nút
   sửa/xoá [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:209-232]. 7 cột sắp xếp được, đảo chiều khi
   bấm lại cùng cột [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:518-528]. Cột AC đổi màu theo ngưỡng
   60% và 35% [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:539].
7. **Phân trang** — 8 dòng mỗi trang, nút Trước/Sau và số trang, nhãn "Trang x / y · hiển thị n dòng"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:236-245, 513-516, 595].
8. **Khối "Phân bố theo chủ đề"** — thanh tỉ lệ theo từng chủ đề
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:249-265, 566-572]. Là cách trình bày thống kê của
   F2-02 (phân loại theo chủ đề).
   **Cảnh báo — dữ liệu mẫu tự mâu thuẫn:** dòng phụ của khối ghi "486 bài trên 12 chủ đề"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:251] trong khi toàn bộ phần còn lại của màn nói 27
   bài [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:152, 479] và danh sách chủ đề chỉ có 8 chủ đề
   thật [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:566-571]. Đây là dữ liệu mẫu sai, không phải yêu
   cầu — khi dựng UI thật, con số phải suy ra từ chính tập dữ liệu đang hiển thị.
9. **Khối "Bài cần chú ý"** (nhãn "Tự phát hiện") — 4 quy tắc phát hiện: chưa có testcase, tỉ lệ AC dưới
   30%, đang ở trạng thái `Chưa xuất bản` (prototype tách hai nhãn "đang ẩn khỏi trang người học" và "bản
   nháp quá 7 ngày" cho hai tình huống khác nhau của cùng một trạng thái theo `F2-15` — bài từng xuất bản
   rồi rút xuống, và bài chưa từng xuất bản — cả hai nay đều là `Chưa xuất bản`), chưa xuất bản quá 7 ngày
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:267-283, 574-579]. **Không có mã cho việc gộp 4 quy
   tắc thành một khối cảnh báo** — xem Q5.
10. **Hộp thoại xác nhận xoá** — tiêu đề "Xoá bài tập?", nội dung nêu bài hoặc số bài đã chọn, cảnh báo
    "Toàn bộ testcase và lượt nộp liên quan sẽ bị ẩn khỏi trang người học. Hành động không thể hoàn tác."
    [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:305-317]. Dùng chung cho cả xoá một dòng và xoá theo
    lô [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:553, 592]. Câu chữ này tự mâu thuẫn — xem Q3.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** kho bài có cả bài `Đã xuất bản` và `Chưa xuất bản`, **Khi** người quản lý nội dung mở màn ở trạng
  thái mặc định (tab "Tất cả"), **Thì** bảng hiển thị đủ cả hai trạng thái, còn chỉ số "Đã xuất bản" phải
  bằng đúng số bài mà `problem_list` của người học nhìn thấy (F2-15)
  [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:480].
- **Cho** một bài toán ở trạng thái `Chưa xuất bản`, **Khi** người học mở `problem_list`, **Thì** bài đó
  không xuất hiện trong ngân hàng bài toán của người học, và mọi liên kết trực tiếp tới bài đó cũng không mở
  được cho A1 (F2-15, chốt 2026-08-30 — trước đây `[SoT: Suy luận]`, nay là yêu cầu đã chốt).
- **Cho** một bài toán chưa có testcase nào, **Khi** người quản lý nội dung xem màn, **Thì** bài đó bị khối
  "Bài cần chú ý" nêu tên [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:575] — và **Khi** người đó cố
  chuyển bài sang `Đã xuất bản`, **Thì** hệ thống chặn kèm lý do, vì xuất bản một bài không có testcase
  Hidden thì không chấm được bài nộp nào (F2-15, chốt 2026-08-30 — trước đây `[SoT: Suy luận]` xin xác nhận
  chặn cứng hay cảnh báo, nay đã chốt: **chặn cứng**).
- **Cho** một bộ lọc và một từ khoá tìm kiếm đang áp dụng, **Khi** người dùng chọn nhiều dòng rồi thực hiện
  một hành động theo lô, **Thì** hành động chỉ áp lên đúng các dòng đã chọn tường minh, không áp lên toàn bộ
  kết quả lọc [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:557, 592 — tập chọn là danh sách mã bài
  tường minh, độc lập với bộ lọc].
- **Cho** người dùng đã chọn một số bài rồi đổi bộ lọc hoặc sang trang khác, **Khi** tập chọn không còn nằm
  trong kết quả đang hiển thị, **Thì** giao diện phải nêu rõ đang chọn bao nhiêu bài và bao nhiêu trong số đó
  đang không hiển thị, để không xoá lô ngoài ý muốn. Prototype giữ nguyên tập chọn qua đổi trang/đổi lọc
  nhưng chỉ ghi "Đã chọn N bài" [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:590] — chưa đủ rõ
  [SoT: Suy luận — nút "Xoá" theo lô là hành động phá huỷ, mà nhãn hiện tại không cho biết những bài nào sẽ
  bị ảnh hưởng].
- **Cho** một bài toán đã có lượt nộp, **Khi** người quản lý nội dung bấm xoá, **Thì** hệ thống nêu rõ số
  lượt nộp sẽ bị ảnh hưởng trong hộp thoại xác nhận, thay vì chỉ hiện mã và tiêu đề bài
  [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:309 — nội dung hộp thoại chỉ là nhãn `mã · tiêu đề`].
- **Cho** một người dùng có vai trò không được cấp `PROBLEM_AUTHORING:DELETE` trong ma trận phân quyền,
  **Khi** người đó mở màn, **Thì** hành động xoá bị chặn ở tầng ứng dụng chứ không chỉ ẩn nút — theo đúng
  nguyên tắc "tách quyền thật ở tầng ứng dụng thay vì chỉ ẩn/hiện menu" [SoT: 01-rd/req/identity.md — F1-10, F1-11].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Actor của màn này và của `problem_authoring` là A2, A3, hay cả hai?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** dùng chung một màn, mount ở cả `/instructor/problems` và `/admin/problems`, cùng một slug/BD/DD, phạm vi dữ liệu do ma trận phân quyền quyết định — A2 chỉ thấy bài của mình/lớp mình, A3 thấy toàn bộ kho. Tiền lệ: F1-12 đã liệt `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` trong ma trận [SoT: 01-rd/req/identity.md — F1-12], cùng hàng `INTERVIEW_BANK_MANAGEMENT` [SoT: 01-rd/req/identity.md — F1-12]. Ghi quyết định `DEC-2026-0825-shared-content-authoring-screens` — không đè Phương án B (mục 7.2 vẫn giữ khu Giảng viên layout riêng khỏi Admin), chỉ là hai route khác nhau cùng render một view. | — | Đã chốt. | Đã đóng |
| Q2 | ~~Trạng thái vòng đời của bài toán (`Đã xuất bản` / `Bản nháp` / `Đã ẩn`) chưa có mã `Fx-nn` nào.~~ **ĐÃ CHỐT 2026-08-30 (owner instruction):** rút gọn còn đúng **hai trạng thái** — `Chưa xuất bản` / `Đã xuất bản` — bỏ hẳn trạng thái thứ ba `Đã ẩn` mà prototype từng dùng. Bài từng xuất bản rồi bị rút xuống quay lại đúng `Chưa xuất bản`, không phân biệt với bài chưa từng công khai. Cấp mã `F2-15` (`01-rd/req/problem-bank.md`): mặc định tạo mới là `Chưa xuất bản`; điều kiện xuất bản — có ít nhất một testcase Hidden (F2-06) và đặc tả đủ theo F2-03, thiếu thì chặn cứng kèm lý do; rút xuống không điều kiện, dữ liệu liên quan (lượt nộp, bookmark, phiên phỏng vấn) không bị xoá. Ghi quyết định `DEC-2026-0830-problem-lifecycle-two-states`. Bổ sung `US-A2-01b` (`01-rd/req/user_stories/a2_instructor.md`). | — | Đã chốt — xem `F2-15`, `DEC-2026-0830-problem-lifecycle-two-states`. | Đã đóng |
| Q3 | ~~**Xoá bài toán: xoá thật hay ẩn mềm?**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** ẩn mềm — chuyển về `Chưa xuất bản` + cờ `deleted` riêng. | — | Dữ liệu liên quan (lượt nộp, bookmark, phiên phỏng vấn) không bị xoá, cùng nguyên tắc F1-16. Đã ghi vào `01-rd/req/problem-bank.md` (amendment F2-15). Sửa câu chữ hộp thoại xác nhận khi dựng UI thật **[Đợi nextjs]**. Xem `DEC-2026-0831-problem-management-lifecycle-details`. | Đã đóng |
| Q4 | ~~**Nhập CSV / Xuất CSV / Nhân bản bài toán...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** giữ Nhân bản (`F2-16`) + Xuất CSV (`F2-17`), cắt Nhập CSV bài toán khỏi phạm vi. | — | CSV phẳng không chở nổi đặc tả F2-03 (chữ ký hàm 3 ngôn ngữ) hay nội dung Markdown/LaTeX. Xoá nút "Nhập CSV" khi dựng UI thật. Xem `DEC-2026-0831-problem-management-lifecycle-details`. | Đã đóng |
| Q5 | ~~4 chỉ số tổng của kho và khối "Bài cần chú ý"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** dẫn xuất trình bày, ngưỡng chốt theo đúng số prototype — AC dưới 30%, chưa xuất bản quá 7 ngày. | — | Đã ghi vào `01-rd/req/problem-bank.md` (amendment F2-15), đánh dấu `[SoT: Suy luận]`. Xem `DEC-2026-0831-problem-management-lifecycle-details`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component, hành vi bảng (sắp xếp, phân trang, thanh hành động theo lô ở mức component)
  — thuộc BD (`02-bd/screens/shared/problem_management.md`, chưa viết).
- Hợp đồng API (danh sách bài toán phía quản trị kèm phân trang/lọc/sắp xếp, đổi trạng thái, xoá, thao tác
  lô, thống kê kho) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Đặc tả nội dung bài toán: Markdown + LaTeX, chữ ký hàm ba ngôn ngữ, chiến lược so khớp, giới hạn tài
  nguyên, testcase — thuộc màn con `01-rd/screens/shared/problem_authoring.md`, không viết lại ở đây.
- Xoá bài toán (Q2), vòng đời bài toán (Q2), nhập/xuất/nhân bản (Q4), thống kê kho (Q5) — **tất cả đã chốt
  2026-08-31**, xem `01-rd/req/problem-bank.md` F2-15, F2-16, F2-17.

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-10 → F1-12, ma trận phân quyền và danh sách Function.
- `01-rd/req/problem-bank.md` — toàn bộ F2 (`problem-bank`).
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-01`, `US-A2-01b` (vòng đời bài toán, F2-15), `US-A2-02`.
- `01-rd/overview/system_survey.md:471-487` — mục 7.1, khu người học (`problem_list`).
- `01-rd/overview/system_survey.md` mục 7.0 (khu dùng chung) — `problem_management`, `problem_authoring`.
- `06-plan/PROTOTYPE_DEBT.md` mục 6.2.b và mục 7 — tiền lệ và bản ghi đợt đối chiếu khu Admin.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0830-problem-lifecycle-two-states`.
- `09-layoutBase/Admin - Quản lý bài tập.dc.html` — prototype.
- `01-rd/screens/admin/admin_queue_monitor.md` — màn cùng khuôn cấu trúc (không cùng khu vực actor).
