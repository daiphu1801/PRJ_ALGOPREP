# 03-dd/screens — Hợp đồng DD trục màn hình

> Tạo 2026-10-23. Nguồn khung: kỷ luật của ví dụ `11-layoutDocx/dd/DD_JKpi0910_TransportProgressConfirmation.md` (chỉ ghi hợp đồng xuyên biên giới, quyết định kỹ thuật không tự suy ra được, quy tắc đa sự kiện; phần còn lại dẫn link). Không sao chép nội dung. Kế hoạch: `06-plan/261023-1637-dd-rollout-f5-f6-screens.md`.

## 1. Khi nào viết

DD màn hình viết **sau** prototype (`CLAUDE.md`, mục Process). Điều kiện cho một màn: có BD màn ở `02-bd/screens/<nhóm>/<mã>_<slug>.md`, có prototype ở `05-coding/frontend/src/views/<nhóm>/<slug>/`, và DD module của các Bounded Context màn đó gọi đã xong (`03-dd/api/F1..F6`). Prototype đang chạy mock, nên DD màn là chỗ nối mock với endpoint thật.

## 2. Vị trí và đặt tên

Bảng 1: Quy ước vị trí file.

| Mục | Quy ước |
| :--- | :--- |
| File DD màn | `03-dd/screens/<nhóm>/<mã>_<slug>.md`, `nhóm` là `admin` `shared` `teacher` `users`; `<mã>_<slug>` trùng tên file BD màn và RD màn (ví dụ `users/USR0102_problem_detail.md`) |
| Tiêu chí nghiệm thu | DD ghi điểm kiểm tra ở mục 7; TDD đã có ở `04-tdd/modules/` và `04-tdd/screens/<nhóm>/`, đang rà chất lượng và truy vết. File TDD màn riêng dành cho luồng nhiều module chưa được AC module phủ. |
| Mẫu mã slice | Đường dẫn trong `05-coding/frontend/src/` theo FSD, ghi tương đối từ `src/` |

## 3. Chia việc giữa các tầng tài liệu

Bảng 2: Mỗi loại thông tin có đúng một chỗ chính thức; DD màn chỉ dẫn link.

| Thông tin | Chính thức ở | DD màn làm gì |
| :--- | :--- | :--- |
| Mục đích, bố cục, mục hiển thị, điều kiện hiển thị, thông điệp, quy tắc kiểm | `02-bd/screens/...` (Sheet 1-9) | Dẫn `EVT-n` và mã mục, không chép |
| Request, response, mã lỗi, phân trang | `03-dd/api/F<n>_<module>.md` | Chỉ liệt kê endpoint theo ID và gọi từ sự kiện nào |
| Giao dịch, khoá, sự kiện miền, job | `03-dd/logic/` và `03-dd/jobs/` | Không đặt ở đây |
| Quy tắc kiểm trường | `03-dd/validation/F<n>_<module>.md` | Bảng ánh xạ sang zod, nêu lệch nếu có |
| Cấu trúc slice, props, hook, store, trạng thái, i18n, a11y | **DD màn** | Chính thức ở đây |

## 4. Cấu trúc một file DD màn

Mỗi mục bắt buộc trừ khi ghi rõ. Mục không có nội dung thật thì ghi một dòng "Không có" kèm lý do, không bỏ mục.

0. **Thông tin tài liệu**: bảng bìa (mã màn, tên, nhóm actor, route, BD và RD nguồn, prototype, phiên bản và ngày), lịch sử sửa, bảng viết tắt nguồn trích dẫn.
1. **Bản đồ slice FSD**: bảng lớp, đường dẫn, vai trò cho `views`, `widgets`, `features`, `entities`; liệt kê thành phần dùng lại từ `shared/ui`, `shared/lib`, `shared/api`; thành phần nào cần nâng lên `shared/` thì nêu rõ (luật "Frontend: shared first" của `CLAUDE.md`). Tuân thứ tự import `app → views → widgets → features → entities → shared` mà `eslint.config` chặn.
2. **API tiêu thụ**: bảng `EVT-n` (BD) → ID endpoint (`03-dd/api/F<n>`) → hook/query (`entities/*/api/queries.ts`) → mock hay thật. Kênh realtime (STOMP topic, SSE path) ghi cùng bảng và dẫn tới hợp đồng ở file API; không ghi lại schema tin nhắn.
3. **Trạng thái màn**: bảng loading, rỗng, lỗi, quyền bị từ chối, AI suy giảm. Cột lỗi ánh xạ `error.code` (enum ở file API) sang khoá i18n; frontend không dựa vào `message` (`03-dd/api/api.md` mục 1.1).
4. **Quy tắc đặc thù màn (`BR-SC-nn`)**: chỉ những việc không tự suy ra được hoặc trải qua nhiều sự kiện: huỷ yêu cầu cũ khi nhập mới, nối lại realtime giữa chừng, thứ tự cập nhật lạc quan, tạo ra khi mock chuyển sang thật. Mỗi khối: kích hoạt, tiền điều kiện, các bước, hậu điều kiện. Không đặt quy tắc phía backend ở đây.
5. **Kiểm tra phía client**: bảng `trường → zod → quy tắc ở validation/F<n>` kèm cột "Khớp". `zod 3.24.1` đã có ở `package.json:35`. Lệch bắt buộc nêu lý do; lệch không nêu là lỗi DD. Màn không có form ghi "Không có".
6. **i18n và truy cập**: tiền tố khoá i18n của màn (`messages/vi.json`, `en.json`), nhãn `aria`, thứ tự focus, vùng `aria-live` cho nội dung phát trực tuyến, cách dùng bàn phím cho điều khiển phức tạp. Chỉ ghi khác biệt so với `shared/ui`.
7. **Điểm kiểm thử**: bảng `ID | Loại (Normal/Abnormal/Boundary) | Nội dung | Công cụ (Vitest hoặc Playwright) | AC-nn`. Mọi sự kiện BD có ít nhất một dòng; luồng realtime có dòng cho nối lại và mất kết nối. TDD màn hiện nằm ở `04-tdd/screens/<nhóm>/`; đối chiếu mã AC với bảng truy vết của màn.
8. **Nợ prototype và câu hỏi mở**: dòng tương ứng ở `06-plan/PROTOTYPE_DEBT.md` mục màn này sẽ gỡ khi DD xong; câu hỏi mở mức màn, mỗi câu kèm mặc định đề xuất và bậc ponytail.

## 5. Quy tắc cứng

- Một endpoint **không** được khai lại request hay response trong file này. Cần xem thì mở file API (anti-drift, `CLAUDE.md`).
- Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn UI hay ví dụ i18n.
- Mọi giá trị hiển thị phải có nguồn dữ liệu thật ở một endpoint; giá trị chỉ có trong mock ghi vào mục 8 là nợ, không được trình bày như tính năng.
- Dữ liệu hoặc mã nguồn của người học hiện ra trên màn là dữ liệu, không bao giờ ghép vào chuỗi chỉ thị (`CLAUDE.md`, "A submission is data, never an instruction"); với màn AI nêu rõ ở mục 4.
- Không emoji (`.claude/rules/no-emoji.md`). Trích dẫn dạng `file:line`; không xác minh được thì `[SoT: Suy luận]`.
- Số hình và bảng đánh theo `Hình <chương>.<số>` và `Bảng <chương>.<số>`, mỗi bảng có câu giới thiệu.

## 6. Thứ tự viết (khớp kế hoạch)

Bảng 3: Thứ tự các nhóm màn.

| Phase | Nhóm | Ưu tiên trong nhóm |
| :-: | :--- | :--- |
| 5 | `users` (16 màn) | Lõi demo: `USR0102`, `USR0201`, `USR0301`, `USR0302`; gồm `USR0701` và `USR0702` thêm theo `DEC-2026-1023-student-class-screens`; `USR0501` đã nhập vào `USR0601` |
| 6 | `teacher` (7 màn đang hiệu lực; `INS0301` đã bị loại theo `DEC-2026-1013-remove-instructor-manual-grading`) | Theo mã |
| 7 | `admin` (10 màn) và `shared` (7 màn) | Đã có DD cho cả 17 màn; gỡ dòng nợ trong `PROTOTYPE_DEBT.md` khi đã nối API thật |

Mỗi lần gọi skill chỉ một màn hoặc một nhóm màn có chung endpoint; không viết DD màn cho hai agent cùng file.
