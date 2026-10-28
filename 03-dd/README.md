# 03-dd — Thiết kế chi tiết (DD)

DD chia hai trục, không trộn (`CLAUDE.md`, mục Process).

Bảng 1: Bản đồ hai trục của thư mục DD.

| Trục | Đơn vị | Thư mục | Trạng thái 2026-10-26 |
| :--- | :--- | :--- | :--- |
| Bounded Context | `F1`..`F6` | `api/` `logic/` `validation/` `jobs/` | F1-F6 đủ; F2 có relay/dọn Outbox ở `jobs/F2_problem-bank.md`, F6 không có `jobs` (xem `logic/F6_interview-bank.md`) |
| Màn hình | mã màn (`USR0102`...) | `screens/<nhóm>/` | 16 DD users, 7 DD teacher, 10 DD admin và 7 DD shared có đủ mục 0–8; các chốt nghiệp vụ đã đối chiếu sang API/logic/DB |

Quy ước:

- Tên file module: `F{n}_{slug}.md`, dùng chung ở mọi tầng (`DEC-2026-1023-module-docs-f-code-names`).
- Hợp đồng API nằm đúng một nơi: `api/F{n}_{slug}.md`. `api/api.md` chỉ là mục lục và bảng mã lỗi.
- `screens/` chỉ liệt kê và dẫn link tới endpoint, không chép request/response, không chứa logic backend.
- TDD bắt đầu triển khai 2026-10-26 sau khi DD được đóng; mục 7 của DD là điểm kiểm tra để truy vết sang `04-tdd/modules/` và `04-tdd/screens/`.
- Kế hoạch triển khai: `06-plan/261023-1637-dd-rollout-f5-f6-screens.md`.

Ba lựa chọn kỹ thuật cuối đã được owner chốt 2026-10-26 và ghi vào decision registry: Run Code 60 lượt/60 phút trượt/tài khoản, mã `SPEC_UNAVAILABLE`, và F2 Outbox. Không còn câu hỏi owner chờ quyết cho ba hợp đồng này. TDD được triển khai sau khi đóng DD.
