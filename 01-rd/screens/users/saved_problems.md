# RD — Màn `saved_problems` (Bài đã lưu)

> Slug: `saved_problems` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `saved_problems` [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `saved_problems`].
> Bounded Context: `problem-bank` (F2) [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `saved_problems`]. Actor: A1.
>
> Đối chiếu prototype: `09-layoutBase/Bài đã lưu.dc.html`. File này mô tả **hành vi và UX ở mức yêu cầu** —
> không lặp lại đặc tả chức năng đã có ở `01-rd/req/problem-bank.md` (mục F2) và `01-rd/req/user_stories/a1_student.md`
> (`US-A1-09`), chỉ trỏ tới và bổ sung phần đặc thù của màn. Đây là màn **khớp tốt nhất** với RD trong số 5
> màn Phase 2 — không có xung đột nghiêm trọng, chỉ một khoảng trống nhỏ (Câu hỏi mở Q1).

## 1. Mục đích màn hình

Xem lại danh sách bài toán đã đánh dấu (bookmark) kèm ghi chú riêng tư theo từng bài, lọc theo độ khó và
trạng thái làm bài, bỏ lưu hoặc vào giải trực tiếp từ danh sách [SoT: 01-rd/req/problem-bank.md — F2-13].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Lưu bài toán (bookmark) kèm ghi chú riêng tư tuyệt đối theo `(user_id, problem_id)` | F2-13 | `01-rd/req/problem-bank.md` — F2-13 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-09`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Bài đã lưu.dc.html`:

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

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-09`)

- **Cho** tôi lọc theo một trạng thái làm bài (ví dụ `todo`), **Khi** bộ lọc áp dụng, **Thì** danh sách chỉ
  hiện các bookmark có đúng trạng thái đó, ghi chú của từng dòng vẫn hiển thị đầy đủ
  [SoT: 09-layoutBase/Bài đã lưu.dc.html:213-214, 148-151].
- **Cho** tôi bấm "Bỏ lưu" một bài, **Khi** tôi xác nhận (nếu có), **Thì** bài đó biến mất khỏi danh sách
  ngay, và ghi chú riêng tư gắn với bookmark đó cũng không còn truy cập được qua màn này
  [SoT: 09-layoutBase/Bài đã lưu.dc.html:156, 212].

## 5. Câu hỏi mở — đã tự chốt 2026-08-25

> Tự quyết theo yêu cầu trực tiếp của chủ dự án — prototype là bản dựng tham khảo.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Câu chữ chân bảng ngụ ý toàn bộ danh sách bookmark là riêng tư, `problem-bank.md` chỉ chốt rõ ở mức ghi chú. | **Chốt: áp cùng mức riêng tư tuyệt đối cho cả việc bookmark**, không chỉ ghi chú — nhất quán với câu chữ UI và cách xử lý dữ liệu cá nhân ở các màn khác. Mở rộng phạm vi F2-13 (không cần mã mới). | `[SoT: Suy luận]`, đã chốt, không còn mở. |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/saved_problems.md`,
  chưa viết).
- Hợp đồng API (lưu/bỏ lưu, sửa ghi chú, lọc/tìm kiếm) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Giới hạn độ dài ghi chú, định dạng cho phép — thuộc DD, không thuộc file RD theo trục màn này.

## 7. Tham chiếu

- `01-rd/req/problem-bank.md` — F2-13.
- `01-rd/req/user_stories/a1_student.md` — `US-A1-09`.
- `01-rd/overview/system_survey.md` — mục 7.1 dòng `saved_problems`.
- `09-layoutBase/Bài đã lưu.dc.html` — prototype.
