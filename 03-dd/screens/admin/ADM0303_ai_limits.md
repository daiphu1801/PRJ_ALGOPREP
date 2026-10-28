# DD màn hình — Hạn mức và chi phí AI (`ADM0303`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/ai-limits` |
| Nguồn | `01-rd/screens/admin/ADM0303_ai_limits.md`; `02-bd/screens/admin/ADM0303_ai_limits.md` V0.9; API [F5](../../api/F5_ai-review.md) §18–26 và [F2](../../api/F2_problem-bank.md) §56–57 |
| Prototype | `05-coding/frontend/src/views/admin/ai-limits/ui/admin-ai-limits-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

Mọi ô ở màn này ghi ngay theo từng thao tác; không có bản nháp/phát hành như `ADM0301`. Hai popup là Đơn giá mô hình và Xác nhận mở khoá ngân sách. Gói đăng ký trả phí ngoài phạm vi đã chốt. [SoT: BD ADM0303 Sheet 4, Q6]

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/ai-limits/page.tsx` | Gắn view trong khung A3; quyền từng khối do API quyết |
| views | `views/admin/ai-limits/ui/admin-ai-limits-view.tsx` | Bố cục ngân sách, tần suất, cảnh báo, giới hạn sinh testcase, popup |
| features | `features/manage-ai-limits/` | Trạng thái ô hạn mức, thao tác ghi tức thời, hộp mở khoá; không sửa store mock trực tiếp |
| entities | `entities/ai-budget/api/queries.ts`, `mutations.ts`; `entities/problem/api/queries.ts` | Query/mutation F5 §18–26 và F2 §56–57; cache theo từng khối |
| shared | `NumberStepper`, `ProgressBar`, `ManagedListDialog`, `ConfirmDialog`, `Card`, `toast` | Dùng lại; popup trả focus về nút mở |

Prototype dùng store module (`aiTokenBudget`, `aiRateLimits`, `aiBudgetSettings`, `aiGenerationSettings`) và ghi đồng bộ trong bộ nhớ. Khi nối API, view nhận state từ query, mutation bất đồng bộ và kiểm quyền theo `AI_CONFIG`/`AI_TOKEN_BUDGET`; không dùng quyền khu `/admin` làm đủ điều kiện ghi. [SoT: `admin-ai-limits-view.tsx:1-104`; BD Sheet 9 NO 1]

## 2. API tiêu thụ

| EVT | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 mở màn | F5 §18 `GET /admin/token-budget`, §21 `GET /admin/limits-config`, §23 `GET /admin/model-prices`; F2 §56 `GET /settings/ai-testcase-generation` | Query độc lập theo khối; popup giá chỉ cần gọi §23 khi mở nếu chưa có cache |
| 2 lưu hạn mức tháng | F5 §19 `PUT /admin/token-budget/limit` | Một giá trị; tăng hạn mức không tự mở khoá |
| 3, 11 mở khoá | F5 §20 `POST /admin/token-budget/unlock` | Hộp xác nhận, chỉ gửi sau nút xác nhận |
| 4, 5 đổi tần suất/ngưỡng | F5 §22 `PATCH /admin/limits-config` | Gửi đúng trường vừa đổi, dùng response cập nhật khối |
| 6 mở popup giá | F5 §23 nếu chưa tải | Không tự sửa giá |
| 7 thêm/đổi tên mô hình | F5 §24 `POST`, §25 `PATCH /admin/model-prices/{priceId}` | Mutation theo từng dòng |
| 8 đổi đơn giá | F5 §25 `PATCH /admin/model-prices/{priceId}` | Mutation theo từng dòng, giá âm/không phải số về 0 theo BD Q4 |
| 9 xoá mô hình | F5 §26 `DELETE /admin/model-prices/{priceId}` | Làm mới danh sách giá sau thành công |
| 10 đổi trần sinh testcase | F2 §57 `PUT /settings/ai-testcase-generation` | Gửi 1–10; phản hồi làm baseline mới |

Không có SSE. F6 Chế độ luyện giới hạn 20 lượt/giờ ở module F6, không có ô điều chỉnh trong màn. [SoT: F5 §21.3; BD ADM0303 Q3]

## 3. Trạng thái màn

| Khối | Điều kiện | Hành vi |
| :--- | :--- | :--- |
| Ngân sách | F5 §18 đang tải/lỗi | Skeleton hoặc lỗi riêng; các khối F2 và giới hạn F5 còn dùng được |
| Ngân sách | `isLocked=true` | Nhãn khoá và nút Mở khoá; nếu `usedTokens >= limitTokens`, nút giữ nguyên để giải thích cần tăng hạn mức, không gửi §20 |
| Ngân sách | Hạn mức mới hợp lệ nhưng PUT lỗi | Giữ chữ đã nhập, toast lỗi; không đổi tỷ lệ/nhãn theo giá trị chưa được server nhận |
| Mở khoá | `422 AIR-203` | Đóng? Giữ hộp để Admin thấy lý do, giữ trạng thái khoá; cho sửa hạn mức trước |
| Tần suất/cảnh báo | F5 §21 lỗi | Các ô của khối này không dùng hằng số mock; hiện thử lại riêng |
| Đơn giá | Danh sách rỗng | Popup vẫn mở, có ô Thêm; chi phí không có giá hiển thị `-` ở màn sử dụng |
| Sinh testcase | F2 §56 lỗi | Chỉ thẻ F2 hiện lỗi, không làm mất dữ liệu F5 |
| Mọi mutation | Đang gửi | Khoá đúng ô/dòng đang gửi, không gửi lặp; thành công mới toast và cập nhật cache |
| Quyền | Thiếu cả hai READ | Không vào màn; có một READ thì khối còn lại chỉ xem theo BD, mutation vẫn bị endpoint chặn |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Ghi ngay và làm mới đúng khối

Mỗi nút +/- hoặc thao tác lưu chỉ gửi mutation của trường tương ứng. Sau thành công dùng response F5/F2 làm cache chuẩn cho khối đó; nếu endpoint không trả dữ liệu đầy đủ thì invalidate đúng query. Lỗi giữ giá trị máy chủ cũ và trường người dùng đang sửa, không hiện toast thành công. Nhiều thao tác khác khối chạy độc lập; không `Promise.all` để một module lỗi che khối khác. [SoT: BD Sheet 4; F5 §19–26; F2 §56–57]

### BR-SC-02 Ngân sách khoá

Phần trăm dùng tính từ `usedTokens / limitTokens`, kẹp hiển thị ở 100%; hạn mức là số nguyên dương. F5 §19 cho tăng hạn mức nhưng vẫn giữ `lockedAt` cho tới khi §20 được gọi. Nút Mở khoá hiển thị khi khoá; nếu `usedTokens >= limitTokens`, báo cần tăng hạn mức trước, vì §20 trả `AIR-203`. Xác nhận chỉ gọi một lần. Thành công lấy response §20 để cập nhật trạng thái, không tự đặt `locked=false` trong store trước phản hồi. [SoT: F5 §18–20; BD Sheet 8 EVT-2,3,11]

### BR-SC-03 Đơn giá mô hình

Popup đọc danh sách giá F5 §23. Thêm tên 1–100 ký tự, cắt hai đầu, trùng không phân biệt hoa thường do server kiểm; thêm với giá 0, sau đó sửa giá bằng §25. Đổi tên có thể làm lượt token cũ mất khớp giá, xoá không bị chặn bởi lịch sử; không viết lại lịch sử. Tên mô hình là dữ liệu hiển thị, không là khoá i18n hay HTML. Đơn giá âm hoặc ô không phải số đưa về 0 khi rời ô theo BD Q4; gửi số hữu hạn không âm tối đa bốn chữ số thập phân tới §25. [SoT: F5 §23–26; BD Q4]

## 5. Kiểm tra phía client

| Trường | Quy tắc client | Hợp đồng nguồn |
| :--- | :--- | :--- |
| `limitTokens` | số nguyên từ 1 tới 9.000.000.000.000; không dùng `Number("")=0` | F5 §19, validation F5 |
| `reviewPerHour`, `interviewPerHour`, `cooldownSeconds` | số nguyên 1–999 | F5 §22 `AIR-V16` |
| `warnPercent` | số nguyên 1–100, step 5 ở UI không thay cận API | F5 §22 `AIR-V16` |
| `modelName` | trim, 1–100 ký tự; kiểm trùng ở server | F5 §24–25 `AIR-V17`, `AIR-204` |
| `usdPerMillionTokens` | số hữu hạn 0–999999.9999, tối đa 4 thập phân; âm/sai dạng chuẩn hoá 0 ở control | F5 §25 `AIR-V18` |
| `maxPerProblem` | số nguyên 1–10 | F2 §57, validation F2 |

## 6. i18n và truy cập

Dùng `adminAiLimits.*`, `apiError.AIR-*`, `apiError.PRB-*`. Các số token dùng `Intl.NumberFormat` theo locale nhưng request là số JSON, không gửi dấu tách nghìn. `ProgressBar` có nhãn văn bản tỷ lệ/đã dùng/hạn mức, không chỉ màu. Nút +/- có tên đầy đủ gồm trường và chiều thay đổi, ô lỗi `aria-invalid`. Popup giá là dialog có heading; popup xác nhận mở khoá mô tả hậu quả và trả focus về nút Mở khoá khi đóng. Toast thành công sau phản hồi API; thông báo lỗi qua `role=alert`.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| AI-L-01 | Normal | Tải bốn khối độc lập; F2 lỗi không che ngân sách F5 | Vitest | TDD màn `AIL-AC-01` |
| AI-L-02 | Boundary | Hạn mức 0/âm/thập phân bị chặn; tăng hạn mức không tự mở khoá | Vitest | TDD màn `AIL-AC-02` |
| AI-L-03 | Abnormal | `usedTokens >= limitTokens` không gửi unlock, `AIR-203` giữ khoá | Vitest | TDD màn `AIL-AC-02` |
| AI-L-04 | Normal | Tần suất/ngưỡng gửi đúng trường đổi, lỗi giữ baseline cũ | Vitest | TDD màn `AIL-AC-01` |
| AI-L-05 | Boundary | Tên giá trùng khác hoa bị `AIR-204`; giá âm/sai dạng về 0 theo BD | Vitest | TDD màn `AIL-AC-03` |
| AI-L-06 | Normal | Xoá/đổi tên giá không sửa lịch sử; popup đóng trả focus | Playwright | TDD màn `AIL-AC-03` |
| AI-L-07 | Security | AI_CONFIG và AI_TOKEN_BUDGET được kiểm riêng theo từng endpoint | Playwright | TDD màn `AIL-AC-03` |
| AI-L-08 | Boundary | Giới hạn sinh testcase 1/10 hợp lệ, 0/11 bị chặn | Vitest | TDD màn `AIL-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype dùng store bộ nhớ của `entities/ai-budget` và `entities/problem`, chưa có API thật; mọi control còn có thể sửa khi role A3 thiếu quyền con. Khi nối API, bỏ thao tác `store.set` trực tiếp, thêm bốn query và mutation tương ứng. `PROTOTYPE_DEBT.md` mục 29 chỉ gỡ khi xong nối thật. Quyết định DD F5 đã đóng BD Q5: §20 trả `AIR-203` khi dùng hết hoặc vượt hạn mức, nên phải tăng hạn mức trước; Q2 về nhật ký đã ghi tại F5 §19–20 cho hai thao tác ngân sách.

| # | Câu hỏi | Kết luận DD |
| :--- | :--- | :--- |
| OQ-1 | Có mở khoá bất chấp số đã dùng không? | Không, F5 §20 bắt `usedTokens < limitTokens` |
| OQ-2 | Ghi ngay có cần nút Lưu chung không? | Không; mỗi mutation ghi riêng theo BD, không có phát hành |
| OQ-3 | Chi phí mô hình khi thiếu đơn giá | Hiện `-` theo BD Q4; không đoán giá |

TDD màn đã có tại `04-tdd/screens/admin/ADM0303_ai_limits.md`.
