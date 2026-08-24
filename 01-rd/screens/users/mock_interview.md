# RD — Màn `mock_interview` (Phỏng vấn giả lập)

> Slug: `mock_interview` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `mock_interview`
> [SoT: 01-rd/overview/system_survey.md:480]. Bounded Context: `ai-review` (F5.2), có đọc thêm từ
> `interview-bank` (F6) khi mở phiên từ lối vào "kho câu hỏi" [SoT: .nexa/control/dependency-map.md:127 —
> dòng `solution_review` · `mock_interview`; 01-rd/req/req.md:366-374 — F5-24]. Actor: A1 (chính).
>
> Theo lộ trình Phase 4 của `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` dòng 44-45. Đối
> chiếu prototype: `09-layoutBase/Phỏng vấn giả lập.dc.html`. File này mô tả **hành vi và UX ở mức yêu cầu**
> — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F5.2) và
> `01-rd/req/user_stories.md` (`US-A1-07`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Trang cho người học mở và tham gia một **phiên phỏng vấn giả lập 1:1 nhiều lượt** với AI, qua ba giai đoạn
(Giải trình → Phản biện → Mở rộng), từ một trong ba lối vào (bài nộp `Accepted`, kho câu hỏi F6, hoặc tự
chọn chủ đề), và xem lại rubric bốn tiêu chí khi kết phiên [SoT: 01-rd/req/user_stories.md:144-166 —
`US-A1-07`; 01-rd/overview/system_survey.md:480].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Mở phiên cho bài nộp `Accepted`; ba giai đoạn Giải trình/Phản biện/Mở rộng | F5-09 tới F5-12 | `01-rd/req/req.md:321-326` |
| Duy trì ngữ cảnh qua `ChatMemory`/Redis; stream phản hồi qua SSE | F5-13, F5-14 | `01-rd/req/req.md:327-328` |
| Kết phiên xuất rubric 4 tiêu chí kèm nhận xét; lưu phiên, mở lại từ trang tiến độ | F5-15, F5-16 | `01-rd/req/req.md:329-331` |
| Chống prompt injection; định hướng giáo dục (không phải điểm chính thức) | F5-17, F5-18 | `01-rd/req/req.md:335-338` |
| Rate limit, suy giảm có kiểm soát khi AI hỏng/hết quota | F5-19, F5-22, F5-25 | `01-rd/req/req.md:339-349` |
| Ba lối vào phiên: bài nộp `Accepted`, kho câu hỏi F6, tự chọn chủ đề | F5-24 | `01-rd/req/req.md:366-374` |
| Tự chỉnh tham số phiên tự luyện (mức độ, số lượt, gợi ý) — chỉ áp dụng 2 lối vào tự luyện | F5-28 | `01-rd/req/req.md:378-382` |
| Given-When-Then đầy đủ cho luồng phỏng vấn giả lập | — | `01-rd/req/user_stories.md:144-166` (`US-A1-07`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Phỏng vấn giả lập.dc.html`, màn có ba trạng thái chính (`isEntry`/`isRunning`/
`isResult`):

### 3.1. Trạng thái `entry` (trước khi bắt đầu phiên)

1. **Thanh chỉ số** — 3 ô: số phiên đã làm, nhận xét trung bình, tiêu chí yếu nhất gần đây [SoT:
   09-layoutBase/Phỏng vấn giả lập.dc.html:104-113, 601-605]. Là dữ liệu tổng hợp từ các phiên đã lưu
   (F5-16), không cần mã `Fx-nn` riêng — công thức tính "tiêu chí yếu nhất" cụ thể **[Đợi nextjs]**.
2. **Tab "Bắt đầu từ"** — ba lựa chọn khớp đúng F5-24: "Bài nộp đã Accepted" (mặc định), "Kho câu hỏi",
   "Tự chọn chủ đề" [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:118-121, 607-609].
   - **Lối "Bài nộp đã Accepted"** — danh sách bài nộp `Accepted` gần đây, mỗi dòng có nút "Phỏng vấn bài
     này" [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:124-142].
   - **Lối "Kho câu hỏi"** — danh sách rút gọn câu hỏi từ F6 kèm nút "Phỏng vấn câu này" và liên kết "Xem
     toàn bộ N câu hỏi" sang `interview_bank_list` [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:144-163].
     Đây chính là điểm `ai-review` cần đọc dữ liệu câu hỏi từ `interview-bank` khi mở phiên — hệ quả tất
     yếu của F5-24 đã chốt trước Phase 4 (`06-plan/PROTOTYPE_DEBT.md` mục 1.3, 2026-08-24), không phải phát
     hiện mới của phiên này; cách đọc cụ thể (outbound port hay domain event) thuộc BD của `ai-review`.
   - **Lối "Tự chọn chủ đề"** — tick nhiều chip chủ đề, chọn Trình độ, chọn Ngôn ngữ nói code, rồi bấm "Bắt
     đầu phiên" [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:166-196]. **Danh sách Trình độ ở đây có 4 mức
     (Intern/Junior/Middle/Senior)** [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:616-617], trong khi màn
     `settings` (F5-28, đã chốt 2026-08-25) chỉ định nghĩa 3 mức (Junior/Middle/Senior)
     [SoT: 01-rd/screens/users/settings.md:43]. **Đã tự chốt (2026-08-25)**: đây là dữ liệu mẫu còn sót lại
     từ trước khi màn `settings` khoá danh sách 3 mức — không sửa `req.md`/`settings.md` (đã chốt bởi phiên
     khác), giữ nguyên 3 mức làm chuẩn; tab "Intern" trong prototype của màn này là dữ liệu thừa, bỏ khi
     dựng UI thật **[Đợi nextjs]**.
3. **Khối "Một phiên diễn ra thế nào"** — liệt kê 3 giai đoạn kèm mô tả ngắn và ghi chú số lượt tối đa
   [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:200-213] — minh hoạ trực quan cho F5-10 tới F5-12.
4. **Khối "Bốn tiêu chí nhận xét"** — liệt kê tên 4 tiêu chí kèm **trọng số phần trăm** (25/30/25/20%)
   [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:215-225, 517-526]. Xem Câu hỏi mở đã chốt ở mục 5 — trọng số
   này có mâu thuẫn nội bộ với cách tính điểm tổng ở trạng thái `result`.
5. **Bảng "Phiên gần đây"** — Ngày, Nguồn phiên, Mạch chủ đề, Lượt, Nhận xét (điểm tổng), nút "Xem lại" mở
   lại `result` của phiên đó [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:229-260]. Khớp F5-16 ("mở lại
   được từ trang tiến độ") — ở đây được lặp lại ngay tại chính màn `mock_interview`, không chỉ ở
   `my_progress`; không phải xung đột, chỉ là thêm một điểm truy cập.

### 3.2. Trạng thái `running` (phiên đang diễn ra)

6. **Khung hội thoại** — mỗi lượt gắn nhãn vai (Bạn/Người phỏng vấn) và nhãn giai đoạn hiện tại, có hiệu
   ứng "đang gõ" khi chờ AI, khung nhập câu trả lời dạng textarea [SoT:
   09-layoutBase/Phỏng vấn giả lập.dc.html:267-317] — khớp F5-10 tới F5-14.
7. **Khối lỗi AI** (`aiError`) — khi AI không phản hồi được: thông báo rõ ràng, hai lựa chọn "Xem nhận xét
   phần đã trao đổi" hoặc "Thử lại lượt này", đúng tinh thần suy giảm có kiểm soát (F5-22) [SoT:
   09-layoutBase/Phỏng vấn giả lập.dc.html:295-304]. Đây là trạng thái lỗi được dựng đầy đủ nhất trong toàn
   bộ các màn AI đã viết RD tới nay (khác với `solution_review` — chưa có trạng thái lỗi nào) — dùng làm
   tham chiếu khi bổ sung trạng thái lỗi cho `solution_review` lúc dựng UI thật.
8. **Cột phải khi đang chạy** — theo dõi Giai đoạn (done/current/todo), Mạch chủ đề (topic thread dạng
   timeline), và Ngữ cảnh (bài nộp/câu hỏi/cấu hình tự chọn, có thể ẩn/hiện code nếu là lối vào từ bài nộp)
   [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:319-376].
9. **Nút "Huỷ phiên"** (`abort`, quay về `entry`, không lưu) và **"Kết phiên và nhận xét"** (`finish`, chuyển
   sang `result`) [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:307-316]. `abort` không có mã `Fx-nn` riêng —
   suy luận hợp lý từ việc phiên là hành động người dùng tự nguyện, không bắt buộc đi hết ba giai đoạn.

### 3.3. Trạng thái `result` (kết phiên)

10. **Điểm tổng lớn** (`overall`) hiển thị cạnh tiêu đề, chú thích "trung bình 4 tiêu chí" [SoT:
    09-layoutBase/Phỏng vấn giả lập.dc.html:389-391]. **Mã nguồn tính `overall` bằng trung bình cộng KHÔNG
    trọng số** (`RUBRIC.reduce((a,r)=>a+r.score,0)/RUBRIC.length`) [SoT:
    09-layoutBase/Phỏng vấn giả lập.dc.html:586], trong khi khối "Bốn tiêu chí nhận xét" ở trạng thái `entry`
    lại hiện trọng số phần trăm khác nhau cho từng tiêu chí (25/30/25/20%) — hai điều này mâu thuẫn nhau
    ngay trong chính prototype. Xem Câu hỏi mở đã chốt ở mục 5.
11. **4 thanh điểm rubric** kèm nhận xét từng tiêu chí, và khối cảnh báo "phản hồi để học, không phải điểm
    chính thức" [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:394-411] — khớp F5-15, F5-18.
12. **"Chủ đề đã đi qua"** và **"Việc nên làm tiếp"** — tổng hợp và gợi ý sau phiên, trong đó có gợi ý dẫn
    sang một câu hỏi cụ thể trong kho F6 [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:415-432, 729-733].
13. **Nút "Phiên mới"** (quay lại `entry`) và **"Ôn chủ đề còn yếu"** (dẫn sang `interview_bank_list`) [SoT:
    09-layoutBase/Phỏng vấn giả lập.dc.html:433-436].

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"`, theo đúng `DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-07`)

- **Cho** tôi đang ở trạng thái `entry` và chọn lối vào "Kho câu hỏi", **Khi** tôi bấm "Phỏng vấn câu này"
  trên một câu hỏi cụ thể, **Thì** phiên mở với ngữ cảnh là đúng câu hỏi đó (không có code, AI chất vấn dựa
  trên câu trả lời tôi nói ra) [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:146, 635-641, 711-713].
- **Cho** tôi đang ở trạng thái `running` và AI không phản hồi được (hết quota hoặc lỗi mô hình), **Khi** sự
  cố xảy ra, **Thì** tôi thấy khối lỗi rõ ràng, được chọn xem nhận xét dựa trên phần đã trao đổi hoặc thử lại
  đúng lượt vừa gửi, và bài nộp/kết quả chấm của tôi (nếu phiên mở từ một bài nộp) không bị ảnh hưởng (F5-22)
  [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:295-304].
- **Cho** tôi mở phiên tự luyện (kho câu hỏi hoặc tự chọn chủ đề) sau khi đã đặt tham số ở `settings`
  (F5-28), **Khi** phiên bắt đầu, **Thì** số lượt tối đa và việc có gợi ý khi bí áp đúng theo tham số đã đặt
  — chi tiết truyền tham số từ `settings` sang `mock_interview` **[Đợi nextjs]** [SoT:
  01-rd/req/req.md:378-382 — F5-28].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Trọng số 25/30/25/20% hiện ở khối "Bốn tiêu chí nhận xét" nhưng điểm tổng (`overall`) ở trạng thái `result` lại tính bằng trung bình cộng không trọng số — mâu thuẫn nội bộ ngay trong prototype.~~ **ĐÃ CHỐT (2026-08-25, tự quyết theo yêu cầu chủ dự án — prototype là bản dựng tham khảo)**: điểm tổng phải tính theo **trung bình có trọng số** đúng bằng các phần trăm đã hiện (25/30/25/20%), không phải trung bình cộng đơn giản — khớp đúng tinh thần F5-23 (cấu hình trọng số rubric). Công thức tính cụ thể và việc trọng số này có cấu hình được qua F5-23 hay cố định **[Đợi nextjs]**, chốt khi viết DD cho `ai-review`. | — | Đã chốt hướng tính, chi tiết cấu hình chờ DD. | Đã đóng |
| Q2 | Tab Trình độ ở lối vào "Tự chọn chủ đề" có 4 mức (Intern/Junior/Middle/Senior), trong khi `settings` (F5-28) đã chốt 3 mức (Junior/Middle/Senior). | Hai file prototype khác nhau (`Phỏng vấn giả lập.dc.html` và `Cài đặt.dc.html`) không khớp nhau về danh sách này. | **ĐÃ CHỐT (2026-08-25, tự quyết)**: giữ 3 mức theo `settings` (đã chốt trước, thuộc phạm vi phiên khác) — coi "Intern" trong màn này là dữ liệu mẫu thừa, bỏ khi dựng UI thật. Không sửa `req.md`/`settings.md`. | Đã đóng |
| Q3 | Nút "Thêm vào phiên giả lập tới" ở `interview_bank_list` (xem file RD riêng) chỉ đổi trạng thái tự chấm cục bộ, không có cơ chế "hàng đợi câu hỏi cho phiên tới" nào thực sự nối sang màn `mock_interview` — người dùng bấm nút này không thấy hệ quả rõ ràng. | Hai file prototype không chia sẻ state nào; đây là giới hạn của bản dựng tham khảo tĩnh, không phải một khoảng trống nghiệp vụ đã biết trước. | **[Đợi nextjs]** — khi dựng backend thật, cân nhắc có nên có một "hàng đợi câu hỏi tự luyện" theo người dùng hay bỏ hẳn cơ chế này, chỉ giữ lối vào tự chọn trực tiếp từ danh sách (đã có nút "Phỏng vấn câu này" ngay tại `interview_bank_list`/`mock_interview`). Không phải quyết định kiến trúc lớn, không cần chủ dự án chốt ngay. | Đợi nextjs |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/mock_interview.md`,
  chưa viết).
- Hợp đồng API (mở phiên, gửi lượt trả lời qua SSE, kết phiên lấy rubric) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết).
- Cách `ai-review` đọc dữ liệu câu hỏi từ `interview-bank` khi mở phiên từ lối vào "Kho câu hỏi" (outbound
  port hay domain event cụ thể) — thuộc BD/DD của `ai-review`, chỉ ghi nhận là điểm cần thiết kế, không tự
  quyết cơ chế ở mức RD theo màn.
- Nội dung và bố cục của `interview_bank_list`/`interview_question_detail` (đích của các liên kết "Xem toàn
  bộ N câu hỏi", "Ôn chủ đề còn yếu") — thuộc các file RD riêng của chúng.

## 7. Tham chiếu

- `01-rd/req/req.md:321-382` — F5-09 tới F5-28.
- `01-rd/req/user_stories.md:144-166` — `US-A1-07`.
- `01-rd/overview/system_survey.md:480` — dòng `mock_interview` trong bảng màn mục 7.1.
- `.nexa/control/dependency-map.md:127` — Bounded Context chạm bởi `mock_interview`.
- `01-rd/screens/users/settings.md:43` — danh sách 3 mức trình độ đã chốt (F5-28).
- `09-layoutBase/Phỏng vấn giả lập.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 4 sinh ra file này.
