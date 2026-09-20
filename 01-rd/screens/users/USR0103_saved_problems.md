# RD (Yêu cầu hệ thống mới) — Bài đã lưu / `USR0103`

> Mã màn hình: `USR0103`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8.
> Slug chính tắc: `saved_problems` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `saved_problems`
> [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `saved_problems`].
> Phạm vi/Bounded Context: `problem-bank` (F2) [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng
> `saved_problems`]. Actor: A1.
> Nguồn sự thật (SoT): `01-rd/req/problem-bank.md` (F2-13), `01-rd/req/user_stories/a1_student.md`
> (`US-A1-09`), `09-layoutBase/Bài đã lưu.dc.html` (prototype).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Xem lại danh sách bài toán đã đánh dấu (bookmark) kèm ghi chú riêng tư theo từng bài, lọc theo độ khó và
trạng thái làm bài, bỏ lưu hoặc vào giải trực tiếp từ danh sách [SoT: 01-rd/req/problem-bank.md — F2-13].

File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở
`01-rd/req/problem-bank.md` (mục F2) và `01-rd/req/user_stories/a1_student.md` (`US-A1-09`), chỉ trỏ tới và
bổ sung phần đặc thù của màn. Đây là màn **khớp tốt nhất** với RD trong số 5 màn Phase 2 — không có xung
đột nghiêm trọng, chỉ một khoảng trống nhỏ (Câu hỏi mở Q1).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Lưu bài toán (bookmark) kèm ghi chú riêng tư tuyệt đối theo `(user_id, problem_id)` | F2-13 | `01-rd/req/problem-bank.md` — F2-13 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-09`) |

Đối chiếu `09-layoutBase/Bài đã lưu.dc.html` [SoT: 09-layoutBase/Bài đã lưu.dc.html], phạm vi trên thể hiện
qua các khối màn sau:

1. **Dải chỉ số** — số liệu tóm tắt tập bài đã lưu (dòng 100-107, ví dụ tổng số đã lưu/đã giải trong số đó).
2. **Thanh công cụ** — tìm kiếm theo tên/mã bài, tab lọc theo độ khó, tab lọc theo trạng thái làm bài (dòng
   112-123).
3. **Bảng danh sách** — mỗi dòng: bài toán (mã + tên + độ khó), chủ đề, trạng thái (`attempted`/`todo`/
   `solved`), **ghi chú riêng tư** hiển thị trực tiếp trong bảng, ngày lưu, và khi rê chuột hiện nút "Bỏ lưu"
   và nút vào giải (dòng 138-165) — khớp đúng F2-13 (ghi chú tự do, riêng tư, sửa được).
4. **Trạng thái rỗng** — khi chưa lưu bài nào, hiện tiêu đề và ghi chú hướng dẫn thay cho bảng (dòng
   167-172).
5. **Chân bảng** — tóm tắt số dòng và dòng nhắc "Bài đã lưu chỉ hiển thị với bạn" (dòng 174-177) — đúng tinh
   thần "riêng tư tuyệt đối" của F2-13, nhưng câu chữ này nói về *toàn bộ danh sách bookmark*, còn F2-13 chốt
   riêng tư ở mức *ghi chú* — hai phạm vi gần nhau nhưng không hoàn toàn giống nhau (xem Câu hỏi mở Q1).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0103` / `saved_problems` | `problem-bank` |
| Tài liệu yêu cầu | `01-rd/req/problem-bank.md` (F2-13) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Bài đã lưu.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `09-layoutBase/Bài đã lưu.dc.html` | Prototype đối chiếu bố cục và trạng thái màn | [SoT: 09-layoutBase/Bài đã lưu.dc.html] |
| Bounded Context `problem-bank` (F2) | BC sở hữu dữ liệu và logic đứng sau màn | [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `saved_problems`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/USR0103_saved_problems.md`,
  chưa viết).
- Hợp đồng API (lưu/bỏ lưu, sửa ghi chú, lọc/tìm kiếm) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Giới hạn độ dài ghi chú, định dạng cho phép — thuộc DD, không thuộc file RD theo trục màn này.

---

## 4. Tiền đề và ràng buộc

> Tự quyết theo yêu cầu trực tiếp của chủ dự án — prototype là bản dựng tham khảo.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Câu chữ chân bảng ngụ ý toàn bộ danh sách bookmark là riêng tư, `problem-bank.md` chỉ chốt rõ ở mức ghi chú. | **Chốt: áp cùng mức riêng tư tuyệt đối cho cả việc bookmark**, không chỉ ghi chú — nhất quán với câu chữ UI và cách xử lý dữ liệu cá nhân ở các màn khác. Mở rộng phạm vi F2-13 (không cần mã mới). | `[SoT: Suy luận]`, đã chốt, không còn mở. |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Lưu bài toán (bookmark) kèm ghi chú riêng tư tuyệt đối theo `(user_id, problem_id)` (F2-13) | Chức năng | `01-rd/req/problem-bank.md` — F2-13 |
| REQ-02 | Given-When-Then đầy đủ (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-09`) |
| REQ-03 | **Cho** tôi lọc theo một trạng thái làm bài (ví dụ `todo`), **Khi** bộ lọc áp dụng, **Thì** danh sách chỉ hiện các bookmark có đúng trạng thái đó, ghi chú của từng dòng vẫn hiển thị đầy đủ | Chức năng (GWT) | [SoT: 09-layoutBase/Bài đã lưu.dc.html:213-214, 148-151] |
| REQ-04 | **Cho** tôi bấm "Bỏ lưu" một bài, **Khi** tôi xác nhận (nếu có), **Thì** bài đó biến mất khỏi danh sách ngay, và ghi chú riêng tư gắn với bookmark đó cũng không còn truy cập được qua màn này | Chức năng (GWT) | [SoT: 09-layoutBase/Bài đã lưu.dc.html:156, 212] |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa xác định — không tự suy luận |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | Xem các yêu cầu tại chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-13. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-09`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `saved_problems`. |
| Prototype | `09-layoutBase/Bài đã lưu.dc.html`. |
