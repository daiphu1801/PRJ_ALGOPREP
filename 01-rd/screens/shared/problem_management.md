# RD — Màn `problem_management` (Quản lý bài tập — bảng quản trị nội dung)

> Slug: `problem_management` (đổi tên từ `admin_problem_management` — quyết định 2026-08-25, xem dưới).
> Bounded Context: `problem-bank` (F2). **Actor: A2 và A3 — màn dùng chung, phạm vi dữ liệu theo quyền.**
> Đã chốt ngày 2026-08-25 (owner instruction, kết thúc Câu hỏi mở Q1 cũ): mount ở cả `/instructor/problems`
> và `/admin/problems`, cùng một view/BD/DD, A2 chỉ thấy bài của mình/lớp mình, A3 thấy toàn bộ kho — cơ chế
> đã có tiền lệ ở ma trận phân quyền `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` (`req.md:64`), cùng hàng với
> `INTERVIEW_BANK_MANAGEMENT` (`req.md:67`). Ghi quyết định:
> `DEC-2026-0825-shared-content-authoring-screens`.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Quản lý bài tập.dc.html` (618 dòng, dựng trong shell Admin —
> khu Giảng viên chưa có prototype tương ứng, xem `06-plan/PROTOTYPE_DEBT.md` mục 7.3.a). File này mô tả
> **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F2), chỉ
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
| Actor | A1 — người học [SoT: 01-rd/req/req.md:164] | A2 và A3 (dùng chung — đã chốt) |
| Mã chức năng | F2-11 — tìm kiếm và lọc bài toán theo chủ đề, độ khó, **trạng thái đã giải** [SoT: 01-rd/req/req.md:164] | Chưa có mã phủ trọn — xem mục 2 và Q2 |
| Mục đích | Ngân hàng bài toán để chọn bài giải [SoT: 01-rd/overview/system_survey.md:476] | Bảng quản trị nội dung: tạo/sửa/xuất bản/ẩn/xoá |
| Phạm vi dữ liệu | Chỉ bài **đã xuất bản** — prototype ghi rõ "24 bài đang hiển thị ở Ngân hàng bài toán" trên tổng 27 [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:152] và "Đúng bằng số bài trang người học thấy" [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:480] | Toàn bộ kho, kể cả Bản nháp và Đã ẩn [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:457-459] |
| Khối riêng cho lớp | Có — bài giao theo lớp hiển thị thành khối riêng cạnh bảng chính (F2-12) [SoT: 01-rd/req/req.md:165-171] | Không có trong prototype |

**Phần trùng về hình thức:** cả hai đều có ô tìm kiếm, bộ lọc độ khó (Easy/Medium/Hard), phân trang. Trùng
này là trùng **thành phần UI**, không phải trùng yêu cầu — bộ lọc thứ hai của màn này là *trạng thái xuất
bản* (Tất cả / Đã xuất bản / Bản nháp) [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:585], trong khi
bộ lọc thứ hai của `problem_list` là *trạng thái đã giải của chính người học* (F2-11)
[SoT: 01-rd/req/req.md:164]. Hai trục lọc khác hẳn nhau về nghiệp vụ.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Khối trên màn | Mã | Nguồn |
| :--- | :--- | :--- |
| Lối vào tạo bài mới / sửa bài (nút "Bài tập mới", tiêu đề bài, nút sửa) | F2-01, F2-02, F2-03, F2-04 | `01-rd/req/req.md:139-152` |
| Cột "TC" (số testcase) trên mỗi dòng, mục "Chưa có testcase" ở khối Bài cần chú ý | F2-05, F2-06 | `01-rd/req/req.md:154` |
| Cột "Độ khó" và bộ lọc độ khó | F2-02 | `01-rd/req/req.md:139` |
| Cột "Chủ đề", khối "Phân bố theo chủ đề", bulk action "Gán chủ đề" | F2-02 | `01-rd/req/req.md:139` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:203-215` (`US-A2-01`), `01-rd/req/user_stories.md:216-228` (`US-A2-02`) |
| Cơ chế gác quyền cho màn | F1-10, F1-11, F1-12 (`PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`) | `01-rd/req/req.md:55-68` |

**Các khối không gán được mã `Fx-nn` nào** (không tự phát minh mã mới — đưa vào mục 5): trạng thái vòng đời
bài toán (Đã xuất bản / Bản nháp / Đã ẩn), xoá bài toán, xử lý theo lô, Nhập CSV / Xuất CSV, nhân bản bài
toán, 4 chỉ số tổng của kho, khối "Bài cần chú ý". Chi tiết ở Q2 → Q5.

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
   thấy"), Bản nháp (kèm "1 bài chưa có testcase"), Tỉ lệ AC trung bình (tính trên 90 ngày gần nhất)
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:478-483]. **Không có mã** — xem Q5.
4. **Thanh lọc và tìm kiếm** — ô tìm "theo mã bài hoặc tiêu đề"
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:181], tab độ khó `Tất cả / Easy / Medium / Hard`, tab
   trạng thái `Tất cả / Đã xuất bản / Bản nháp`, và bộ đếm kết quả
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:584-588]. Lọc và tìm đều reset về trang 1
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:473, 587].
   **Cảnh báo:** tab trạng thái chỉ có 3 giá trị, thiếu `Đã ẩn`, dù dữ liệu có bài ở trạng thái `Đã ẩn`
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:459, 490] — bài `Đã ẩn` chỉ xem được qua tab "Tất
   cả", không lọc riêng được. Đây là khuyết của prototype, khi dựng UI thật nên bổ sung tab thứ tư
   [SoT: Suy luận — cùng một tập giá trị trạng thái thì bộ lọc phải phủ đủ, nếu không có một trạng thái
   không bao giờ xem riêng được].
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
   30%, đang ẩn khỏi trang người học, bản nháp quá 7 ngày
   [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:267-283, 574-579]. **Không có mã** — xem Q5.
10. **Hộp thoại xác nhận xoá** — tiêu đề "Xoá bài tập?", nội dung nêu bài hoặc số bài đã chọn, cảnh báo
    "Toàn bộ testcase và lượt nộp liên quan sẽ bị ẩn khỏi trang người học. Hành động không thể hoàn tác."
    [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:305-317]. Dùng chung cho cả xoá một dòng và xoá theo
    lô [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:553, 592]. Câu chữ này tự mâu thuẫn — xem Q3.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** kho bài có cả bài `Đã xuất bản`, `Bản nháp` và `Đã ẩn`, **Khi** người quản lý nội dung mở màn ở
  trạng thái mặc định (tab "Tất cả"), **Thì** bảng hiển thị đủ cả ba trạng thái, còn chỉ số "Đã xuất bản"
  phải bằng đúng số bài mà `problem_list` của người học nhìn thấy
  [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:480].
- **Cho** một bài toán ở trạng thái `Bản nháp`, **Khi** người học mở `problem_list`, **Thì** bài đó không
  xuất hiện trong ngân hàng bài toán của người học, và mọi liên kết trực tiếp tới bài đó cũng không mở được
  cho A1 [SoT: Suy luận — suy ra từ chú thích "Đúng bằng số bài trang người học thấy"
  (`09-layoutBase/Admin - Quản lý bài tập.dc.html:480`) và từ dữ liệu mẫu: hai bài `Bản nháp` có 0 lượt nộp
  và 0% AC (`09-layoutBase/Admin - Quản lý bài tập.dc.html:457-458`), tức chưa từng có ai nộp được. Nếu chỉ
  ẩn khỏi danh sách mà vẫn mở được bằng URL thì cơ chế nháp không có tác dụng bảo vệ nội dung chưa hoàn
  thiện].
- **Cho** một bài toán chưa có testcase nào, **Khi** người quản lý nội dung xem màn, **Thì** bài đó bị khối
  "Bài cần chú ý" nêu tên [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:575] — và **Khi** người đó cố
  chuyển bài sang `Đã xuất bản`, **Thì** hệ thống chặn kèm lý do, vì xuất bản một bài không có testcase
  Hidden thì không chấm được bài nộp nào [SoT: Suy luận — F2-06 quy định testcase Hidden là đầu vào của việc
  Nộp bài (`01-rd/req/req.md:154`); xuất bản mà thiếu nó thì luồng nộp bài của F4 không có gì để chấm. Cần
  chủ dự án xác nhận đây là chặn cứng hay chỉ cảnh báo].
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
  nguyên tắc "tách quyền thật ở tầng ứng dụng thay vì chỉ ẩn/hiện menu" [SoT: 01-rd/req/req.md:52-57].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Actor của màn này và của `problem_authoring` là A2, A3, hay cả hai?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** dùng chung một màn, mount ở cả `/instructor/problems` và `/admin/problems`, cùng một slug/BD/DD, phạm vi dữ liệu do ma trận phân quyền quyết định — A2 chỉ thấy bài của mình/lớp mình, A3 thấy toàn bộ kho. Tiền lệ: F1-12 đã liệt `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` trong ma trận [SoT: 01-rd/req/req.md:64], cùng hàng `INTERVIEW_BANK_MANAGEMENT` [SoT: 01-rd/req/req.md:67]. Ghi quyết định `DEC-2026-0825-shared-content-authoring-screens` — không đè Phương án B (mục 7.2 vẫn giữ khu Giảng viên layout riêng khỏi Admin), chỉ là hai route khác nhau cùng render một view. | — | Đã chốt. | Đã đóng |
| Q2 | **Trạng thái vòng đời của bài toán (`Đã xuất bản` / `Bản nháp` / `Đã ẩn`) chưa có mã `Fx-nn` nào.** Prototype dùng ba trạng thái này ở cột Trạng thái, ở bộ lọc, ở 4 chỉ số tổng, ở bulk action "Xuất bản / ẩn" và ở khối "Bài cần chú ý" [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:490, 585, 591]. `req.md` chỉ nhắc "khi xuất bản" một lần, gián tiếp, trong ngữ cảnh testcase do AI sinh [SoT: 01-rd/req/req.md:192] — không có mã nào định nghĩa trạng thái bài toán, ai đổi được, và điều kiện đổi. | Đây là khái niệm trung tâm của cả màn (chi phối 5 khối) nhưng không truy được về mã nào. Không tự phát minh mã mới theo quy tắc của dự án. Ngoài ra chưa rõ `Bản nháp` và `Đã ẩn` khác nhau thế nào về nghiệp vụ: bài `Đã ẩn` trong dữ liệu mẫu vẫn có 210 lượt nộp và 22% AC [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:459], tức từng xuất bản rồi bị rút xuống — khác hẳn `Bản nháp` chưa từng công khai. | Cấp một mã mới trong nhóm F2 cho vòng đời bài toán, đặc tả: ba trạng thái, các chuyển trạng thái hợp lệ, điều kiện tiên quyết để xuất bản (tối thiểu phải có testcase Hidden và đặc tả bài toán đủ theo F2-03), và hành vi của bài `Đã ẩn` với người đã từng nộp. Chưa chốt thì Q3/Q5 cũng chưa giải được vì cùng phụ thuộc khái niệm này. | Chủ dự án |
| Q3 | **Xoá bài toán: xoá thật hay ẩn mềm? Xoá được bài đã có lượt nộp không?** Hộp thoại xác nhận tự mâu thuẫn: câu đầu nói dữ liệu liên quan "sẽ **bị ẩn** khỏi trang người học", câu sau nói "Hành động **không thể hoàn tác**" [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:310]. Nếu chỉ ẩn thì hoàn tác được; nếu không hoàn tác được thì không phải ẩn. Prototype hiện thực bằng cách đánh dấu `deleted` trong state, tức ẩn mềm [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:494, 603-607]. | Chưa có mã `Fx-nn` nào cho việc xoá bài toán. Đây là hành động phá huỷ và có thể xoá theo lô [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:592], nên chọn sai là mất dữ liệu thật. Thêm nữa, xoá cứng một bài đã có lượt nộp sẽ phá vỡ lịch sử nộp bài (F1-18) và thống kê tiến độ lớp. | Áp dụng đúng nguyên tắc đã dùng cho F1-16 (tự xoá tài khoản): **ẩn mềm, dữ liệu không mất** — bài nộp, bookmark, phiên phỏng vấn liên quan vẫn giữ, chỉ gỡ khỏi tầm nhìn người học [SoT: 01-rd/req/req.md:93-98 — F1-16 đã chốt "khoá mềm rồi ẩn danh hoá", lý do nêu rõ là để không phá vỡ thống kê tiến độ lớp]. Nếu chốt như vậy thì "Xoá" thực chất trùng với trạng thái `Đã ẩn` của Q2, và nên gộp: hoặc bỏ nút Xoá, hoặc định nghĩa Xoá là một trạng thái thứ tư (`Đã lưu trữ`) không hiện trong bộ lọc thường. Câu chữ hộp thoại phải sửa lại cho khớp bất kể chốt phương án nào **[Đợi nextjs]**. | Chủ dự án |
| Q4 | **Nhập CSV / Xuất CSV / Nhân bản bài toán — ba tính năng không có mã `Fx-nn`.** "Nhập CSV" là nút cấp thanh tiêu đề [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:159]; "Nhân bản" và "Xuất CSV" là hành động theo lô [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:591]. `req.md` chỉ có F2-07 (tải lên **testcase** theo lô, lưu MinIO) [SoT: 01-rd/req/req.md:155] — đó là nhập testcase, không phải nhập **bài toán**. | Nhập CSV bài toán là tính năng nặng: một bài toán gồm đề Markdown + LaTeX, chữ ký hàm cho ba ngôn ngữ, chiến lược so khớp, giới hạn tài nguyên theo ngôn ngữ (F2-01 → F2-04, F2-10) — biểu diễn trọn bộ đó trong CSV phẳng là không tự nhiên. Không rõ đây là tính năng thật hay chỉ nút minh hoạ trong prototype. | Xử lý ba tính năng theo ba mức khác nhau: (a) **Nhân bản** — chi phí thấp, giá trị cao khi soạn nhiều bài cùng dạng, đề xuất giữ và cấp mã trong nhóm F2; (b) **Xuất CSV** — chỉ xuất phần dữ liệu bảng (mã, tiêu đề, chủ đề, độ khó, trạng thái, lượt nộp, AC, số testcase), không xuất đề bài, chi phí thấp, đề xuất giữ; (c) **Nhập CSV bài toán** — đề xuất **bỏ khỏi phạm vi đồ án**, thay bằng nhập theo tệp có cấu trúc (một thư mục/JSON cho mỗi bài) nếu thật cần nhập lô, vì CSV phẳng không chở nổi đặc tả F2-03. | Chủ dự án |
| Q5 | **4 chỉ số tổng của kho và khối "Bài cần chú ý" chưa có mã.** 4 chỉ số: Tổng bài, Đã xuất bản, Bản nháp, Tỉ lệ AC trung bình 90 ngày [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:478-483]. "Bài cần chú ý" nhãn "Tự phát hiện" với 4 quy tắc, trong đó ngưỡng "AC dưới 30%" và "Bản nháp quá 7 ngày" là hai con số chưa từng có ở đâu trong RD [SoT: 09-layoutBase/Admin - Quản lý bài tập.dc.html:575-578]. | Không có mã nào trong nhóm F2 nói về thống kê chất lượng kho bài hoặc phát hiện bài có vấn đề. Đây có thể coi là dẫn xuất trình bày (như `class_progress` là "dẫn xuất từ F1-06, F1-07" [SoT: 01-rd/overview/system_survey.md:524]) hoặc là tính năng riêng cần mã. Hai ngưỡng số thì chắc chắn phải chốt ở đâu đó mới hiện thực được. | Coi cả hai khối là **dẫn xuất trình bày** từ dữ liệu đã có (không cần mã mới), nhưng ghi hai ngưỡng thành tham số cấu hình được thay vì hằng số nằm trong mã: ngưỡng AC thấp và số ngày nháp quá hạn. Ngoài ra, quy tắc "Bản nháp quá 7 ngày" chỉ có nghĩa nếu Q2 đã chốt vòng đời bài toán — giải Q2 trước. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component, hành vi bảng (sắp xếp, phân trang, thanh hành động theo lô ở mức component)
  — thuộc BD (`02-bd/screens/shared/problem_management.md`, chưa viết).
- Hợp đồng API (danh sách bài toán phía quản trị kèm phân trang/lọc/sắp xếp, đổi trạng thái, xoá, thao tác
  lô, thống kê kho) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Đặc tả nội dung bài toán: Markdown + LaTeX, chữ ký hàm ba ngôn ngữ, chiến lược so khớp, giới hạn tài
  nguyên, testcase — thuộc màn con `01-rd/screens/shared/problem_authoring.md`, không viết lại ở đây.
- Vòng đời bài toán (Q2), xoá bài (Q3), nhập/xuất/nhân bản (Q4), thống kê kho (Q5) — không tự đặc tả trong
  file này, chờ chủ dự án quyết định.

## 7. Tham chiếu

- `01-rd/req/req.md:52-68` — F1-10 → F1-12, ma trận phân quyền và danh sách Function.
- `01-rd/req/req.md:136-195` — toàn bộ F2 (`problem-bank`).
- `01-rd/req/user_stories.md:203-228` — `US-A2-01`, `US-A2-02`.
- `01-rd/overview/system_survey.md:471-487` — mục 7.1, khu người học (`problem_list`).
- `01-rd/overview/system_survey.md` mục 7.0 (khu dùng chung) — `problem_management`, `problem_authoring`.
- `06-plan/PROTOTYPE_DEBT.md` mục 6.2.b và mục 7 — tiền lệ và bản ghi đợt đối chiếu khu Admin.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`.
- `09-layoutBase/Admin - Quản lý bài tập.dc.html` — prototype.
- `01-rd/screens/admin/admin_queue_monitor.md` — màn cùng khuôn cấu trúc (không cùng khu vực actor).
