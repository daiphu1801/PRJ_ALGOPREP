# DD màn hình — Cấu hình trợ lý AI (`ADM0301`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/ai-config` |
| Nguồn | `01-rd/screens/admin/ADM0301_ai_config.md`; `02-bd/screens/admin/ADM0301_ai_config.md` Sheet 1–9; [F5](../../api/F5_ai-review.md) §14–17 |
| Prototype | `05-coding/frontend/src/views/admin/ai-config/ui/admin-ai-config-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/ai-config/page.tsx` | Actor A3 và quyền `AI_CONFIG` |
| views | `views/admin/ai-config/ui/admin-ai-config-view.tsx` | Tab tính năng, popup prompt/lịch sử, hàng chờ phát hành |
| features | `features/manage-ai-prompts/` | Bản nháp form, chọn model, phát hành một giao dịch |
| entities | `entities/ai-prompt/api/queries.ts`, `mutations.ts` | F5 §14–17, cache theo feature |
| shared | `MarkdownEditor`, `Tabs`, `ConfirmDialog`, `DiffView` | Dùng lại, không nhúng chỉ thị người dùng vào UI |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 mở, 23 đổi tab | F5 §14 `GET /admin/prompt-templates` | Một ACTIVE và tối đa một DRAFT mỗi tính năng; model options từ response |
| 2–4 sửa/lưu nháp | F5 §16 `PUT /admin/prompt-templates/{featureCode}/draft` | Chỉ lưu nháp tính năng đang sửa; Hủy đóng không ghi |
| 5–7 lịch sử/khôi phục | F5 §15 `GET /admin/prompt-templates/{featureCode}/history`; §16 khi chọn bản cũ để tạo draft mới | Không sửa bản ARCHIVED đã có |
| 12–13 phát hành | F5 §17 `POST /admin/prompt-templates/publish` | Gửi tất cả thay đổi chờ cùng một lần; response thành công mới đổi ACTIVE |
| 22 model | Không gọi API ngay | Chọn trong `modelOptions` từ §14, lưu qua §16 |
| 24 huỷ thay đổi | Không gọi API nếu chỉ sửa local | Trở về baseline server; nếu draft đã lưu thì cần thao tác F5 theo contract, không tự xoá |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Danh sách/hist tải lỗi | Lỗi riêng, nút Thử lại; không thay bằng prompt mẫu |
| Một tính năng chưa có draft | Hiện ACTIVE và trạng thái “chưa có thay đổi chờ” |
| Lưu draft lỗi | Giữ nội dung đang sửa và dialog, không đổi nhãn “đã lưu” |
| Phát hành lỗi một phần | F5 §17 hoàn tác tất cả; UI giữ toàn bộ DRAFT và báo lỗi, không đánh dấu feature nào ACTIVE |
| `AI_CONFIG` thiếu READ/UPDATE | 403 hoặc chế độ chỉ đọc theo quyền; server kiểm mọi mutation |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Draft, lịch sử, phát hành

Mỗi feature có tối đa một DRAFT; sửa prompt/model/response guard trong form local rồi lưu F5 §16. Lịch sử là bản bất biến; “khôi phục” sao chép nội dung phiên cũ thành draft mới qua §16, không viết đè lịch sử. Nút Phát hành gom toàn bộ draft đang chờ và gọi §17 một lần; nếu bất kỳ feature lỗi thì toàn bộ giao dịch giữ nguyên. Sau thành công invalidate danh sách và lịch sử. Bảo vệ rời dialog khi form local dirty. [SoT: BD Q3, EVT-3,6,12; F5 §14–17]

### BR-SC-02 Model và guard

Model chỉ chọn từ `modelOptions` của F5 §14, không hardcode. `maxTokens` và `responseSchemaVersion` chỉ đọc. `responseGuards` chỉ áp feature có guard; `noFullSolution` nhận cả true/false khi lưu, không ép luôn bật. Prompt có thể chứa mẫu kỹ thuật nhưng nội dung bài nộp người học không được ghép thành chỉ thị hệ thống khi chạy AI; quy tắc runtime thuộc F5. [SoT: F5 §14, §16; BD Sheet 4]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `systemInstruction` | Bắt buộc và giới hạn theo validation F5, giữ nguyên xuống dòng | F5 §16 |
| `modelId` | Phải thuộc `modelOptions` vừa tải | F5 §14, §16 |
| `responseGuards` | Boolean đúng ba cờ ở feature hỗ trợ; không gửi cho testcase | F5 §14, §16 |

## 6. i18n và truy cập

Dùng `adminAiConfig.*`, `apiError.AIR-*`. Tab có `aria-selected`; trạng thái ACTIVE/DRAFT đọc bằng chữ và version. Dialog editor có heading, lỗi gắn trường, Esc theo guard dirty. Diff lịch sử có nhãn “bản cũ/bản nháp” và văn bản thay đổi, không chỉ màu. Xác nhận phát hành nêu số feature sẽ đổi, trả focus về nút phát hành.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| AIC-01 | Normal | EVT-1/23 ACTIVE và DRAFT theo ba feature; model từ F5 | Vitest | TDD màn `AIC-AC-01` |
| AIC-02 | Normal | EVT-2–4 sửa, lưu draft; Hủy local không gọi API | Vitest | TDD màn `AIC-AC-01` |
| AIC-03 | Normal | EVT-5–7 khôi phục lịch sử thành draft, ARCHIVED bất biến | Vitest | TDD màn `AIC-AC-02` |
| AIC-04 | Abnormal | EVT-12 phát hành lỗi giữ toàn bộ draft, không ACTIVE một phần | Vitest | TDD màn `AIC-AC-02` |
| AIC-05 | Boundary | `noFullSolution=false` được gửi đúng; `maxTokens` chỉ đọc | Vitest | TDD màn `AIC-AC-02` |
| AIC-06 | Normal | Guard rời form dirty, dialog và focus bàn phím | Playwright | TDD màn `AIC-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn store prompt local; nối F5 §14–17, bỏ thao tác phát hành từng thẻ riêng. BD đã chốt phát hành nguyên tử và model do dev cho phép; không còn câu hỏi mức màn chờ owner. Chỉ gỡ nợ `PROTOTYPE_DEBT.md` sau khi API thật chạy. TDD màn đã có tại `04-tdd/screens/admin/ADM0301_ai_config.md`.
