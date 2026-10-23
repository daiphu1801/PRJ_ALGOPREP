# Tài liệu thiết kế cơ bản (BD) — Thiết lập (`USR0503`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `USR0503` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8 dòng 149; tên file mang tiền
  tố mã.
- Khung điều hướng **không mô tả lại ở đây** — dùng chung `02-bd/screens/users/_shell.md`. Khu Người học
  dùng **header ngang dính trên, không có sidebar**, và màn này nằm trong **menu người dùng** chứ không
  phải nav chính [Nguồn: 02-bd/screens/users/_shell.md:24,62-64]. Chân trang cũng thuộc khung chung
  [Nguồn: 02-bd/screens/users/_shell.md:86-104].
- Màn này có **ba popup**: xác nhận xoá tài khoản, xác nhận rời màn khi còn thay đổi chưa lưu, và đổi/đặt
  mật khẩu (từ V0.7). Cả ba đều **không có trong prototype** — xem mục 4.4.

> Đọc cùng `01-rd/screens/users/USR0503_settings.md` (hành vi ở mức yêu cầu, không lặp lại ở đây),
> `01-rd/req/identity.md:197-202,231-238` (F1-16, F1-20, F1-21, F1-22), `01-rd/req/ai-review.md:103-107`
> (F5-28), `02-bd/database/identity.md` mục 1.1 và 1.12, `02-bd/security/identity.md` mục 3 và 4.
>
> **Không thiết kế** cấu hình ngôn ngữ **lập trình** cho phép nộp bài, hệ số giới hạn hay tham số sandbox —
> đó là màn `admin_language_config` (`ADM0501`) của quản trị viên
> [Nguồn: 02-bd/screens/admin/ADM0501_language_config.md:387-388]. Hai khái niệm **khác nhau hoàn toàn**:
> màn này đổi ngôn ngữ **giao diện** (i18n, vi/en) và chọn ngôn ngữ nộp bài **mặc định của cá nhân**; màn
> kia quyết định ngôn ngữ nộp bài nào được bật cho toàn hệ thống.
> **Không thiết kế** dữ liệu nhận dạng của người dùng — tên hiển thị, ảnh đại diện, email, liên kết OAuth
> thuộc `profile` (`USR0502`). Ranh giới đã áp: `profile` giữ **dữ liệu nhận dạng**, `settings` giữ
> **tuỳ chọn hành vi ứng dụng**. **Ngoại lệ từ V0.7: đổi/đặt mật khẩu nằm ở màn này** (nhóm Bảo mật), trang
> `profile` chỉ còn dòng phương thức đăng nhập chỉ-đọc
> [Nguồn: .nexa/control/decision-registry.md:4245-4247, `DEC-2026-1020-student-workspace-split-and-password-in-settings`].
> Không thiết kế lại cơ chế băm, thu hồi phiên hay quên mật khẩu — thuộc `02-bd/security/identity.md` và
> `SHR0101`.
> **Không thiết kế** job gửi email nhắc luyện tập và báo cáo tuần — màn này chỉ bật/tắt; job thuộc
> `03-dd/jobs/identity.md`.
> **Không thiết kế** giao diện một phiên phỏng vấn — thuộc `mock_interview` (`USR0302`); màn này chỉ đặt
> tham số mặc định cho phiên tự luyện.

> **Quy ước đặt tên khối** [Nội bộ]. Mười hai khối, đặt tên theo **vai trò** chứ không theo vị trí trên màn —
> kế thừa quy ước chốt ở `USR0101` và dùng lại ở `USR0501`: `appearance` (nhóm Giao diện), `workspace`
> (nhóm Workspace), `interview` (nhóm Phỏng vấn giả lập), `notification` (nhóm Thông báo), `saveBar`
> (khối trạng thái và nút Lưu), `dataExport` (khối Dữ liệu của bạn), `profileShortcut` (lối tắt về hồ sơ),
> `dangerZone` (Vùng nguy hiểm), `deleteConfirm` (popup xác nhận xoá tài khoản), `leaveConfirm` (popup xác
> nhận rời màn), `security` (nhóm Bảo mật, V0.7), `passwordChange` (popup đổi/đặt mật khẩu, V0.7). Tiền tố ID item của toàn màn là `settings.`. Tiền tố loại (`btn`, `link`) đặt ở **tên
> trường**, không ở tên khối.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `identity` (F1) — có phụ thuộc đọc/ghi sang `ai-review` (F5) cho nhóm Phỏng vấn giả lập, và đọc sang `judge-orchestration` (F4) khi xuất lượt nộp |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Thiết lập |
| Mã màn hình | `USR0503` |
| Tên vật lý (slug) | `settings` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A1 (`STUDENT`) |
| Phiên bản | V0.9 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/22 |
| Người cập nhật | Claude |
| Ngày cập nhật | 2026/10/22 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo mẫu 9 sheet. Kế thừa quy ước cụm màn cá nhân từ `USR0501`: tiền tố ID `settings.`, tên khối theo vai trò, tiền tố nghiệp vụ `My` cho phạm vi khoá cứng theo người đăng nhập, suy giảm êm là điều kiện hiển thị chứ không phải thông báo lỗi. Chốt phần đặc thù: tách hai chế độ lưu (áp ngay theo thiết bị cho nhóm Giao diện, chờ nút Lưu cho ba nhóm còn lại), bổ sung hai popup mà prototype không có, sửa câu chữ Vùng nguy hiểm cho khớp F1-16. Phát hiện **sáu tuỳ chọn trên prototype chưa có cột lưu**. Phát sinh 11 câu hỏi mở | 2026/09/22 | Nhóm phát triển AlgoPrep |
| V0.2 | Sheet 5, 6, 8, 9 | Kết quả lưu cài đặt, xuất tệp và xoá tài khoản báo bằng toast; bỏ biến thể "vừa lưu xong" của câu trạng thái và nhãn "Đã lưu" của nút; giữ nguyên khối lỗi tải, dòng suy giảm AI và popup xác nhận. Theo `DEC-2026-1003-toast-feedback-channel`. | 2026/10/03 | AI |
| V0.3 | Sheet 4 (4.3), Sheet 5 (Khu vực B NO 2, F NO 2, 3, H NO 2, I NO 3), Sheet 6 (B NO 2, F NO 3), Sheet 7 (7.1 NO 3, 12; 7.2; 7.3), Sheet 8 (EVT-7, 8, 9), Câu hỏi mở Q4, Q5, Q6, Q7, Q8, Q10, Q11 | Owner 2026/10/18 (`DEC-2026-1018-usr-open-questions-phase2-5`, theo đề xuất): đóng 7 câu hỏi. Q4 nút ngôn ngữ nộp bài mặc định là danh sách động theo `judge.language_configs.enabled = true`, ngôn ngữ đã lưu bị tắt thì lùi về ngôn ngữ đầu tiên đang bật (thêm bảng `judge.language_configs` vào danh sách bảng, thêm endpoint đọc danh sách); Q5 xuất dữ liệu trả tệp đồng bộ có giới hạn tần suất; Q6 xuất hội thoại phỏng vấn do `identity` sở hữu, `ai-review` hỏng thì báo lỗi bằng toast và giữ nút; Q7 xác nhận xoá bằng gõ lại email; Q8 ân hạn 30 ngày không có đường tự huỷ, câu chữ không hứa khôi phục; Q10 "Xuất dữ liệu" và "Vùng nguy hiểm" ở lại `settings`; Q11 mỗi nhóm báo kết quả lưu riêng. Bản dựng: danh sách ngôn ngữ có cờ `enabled` và lùi về ngôn ngữ đầu tiên đang bật (`preferences-form.tsx:25-33,102-108`), toast lưu theo từng nhóm (`preferences-form.tsx:109-125`), lỗi xuất tệp hiện tại chỗ và nút giữ kích hoạt (`data-export-card.tsx:14-22,39-43`). Cột DB không đổi (`judge.language_configs.enabled` đã có ở `02-bd/database/judge-orchestration.md:110`) | 2026/10/18 | Claude |
| V0.4 | Sheet 5 (Khu vực H NO 2), Câu hỏi mở Q8 | Đảo ngược Q8 theo `DEC-2026-1018-account-deletion-grace-cancel`: trong 30 ngày ân hạn người dùng đăng nhập lại để huỷ yêu cầu xoá; sửa câu hệ quả của vùng nguy hiểm | 2026/10/18 | Claude |
| V0.5 | Sheet 5, 6 (Khu vực B NO 2, E NO 2, I NO 3-5, J), Ghi chú bản dựng | Owner 2026/10/19, sau đợt rà soát prototype `06-plan/reports/261019-0814-usr-fe-prototype-audit/report.md`: (D30) nút "Huỷ" của popup xoá không còn bị khoá khi mở popup, chỉ nút xác nhận khoá đến khi email khớp (`ConfirmDialog.confirmDisabled`); (D38) so khớp email xác nhận dùng `trim().toLowerCase()` ở cả hai vế, ô nhập vẫn chỉ tô viền khi chưa khớp (thông điệp theo kênh toast); (D35) ngôn ngữ lùi về ngôn ngữ đầu tiên đang bật **chỉ để hiển thị**, không ghi vào dữ liệu cho tới khi người dùng chọn lại; (D32) popup rời màn (Khu vực J) đã dựng bằng hook `useUnsavedChangesGuard` và `UnsavedChangesDialog` dùng chung, ba nút "Lưu và thoát" / "Thoát không lưu" / "Ở lại" (BD ghi hai nút, bản dựng thêm nút lưu), câu trạng thái có biến thể "Bạn có thay đổi chưa lưu"; (D31) nhóm Phỏng vấn giả lập chờ đọc xong rồi mới khởi tạo bản nháp, khi nhóm suy giảm thì nút Lưu không gửi nhóm đó; (D33) lỗi đọc hồ sơ hiện trạng thái lỗi kèm nút thử lại thay cho khung chờ vô hạn ở Vùng nguy hiểm; (D34) sau khi xoá thành công nút xác nhận vẫn khoá đến khi chuyển sang `/login`. Chưa duyệt hình | 2026/10/19 | Claude |
| V0.6 | Sheet 5 (Khu vực J), 6 (Khu vực J), 8 (EVT-14), Câu hỏi mở Q12 | Owner chốt 2026/10/19 (`DEC-2026-1019-usr-fe-audit-fixes`, chốt nốt câu hỏi mở còn lại sau rà prototype). Chỉ tài liệu, không đổi code, chưa duyệt hình. Giữ hộp thoại rời màn ba nút dùng chung (Lưu và thoát, Thoát không lưu, Ở lại); sửa phần BD từng ghi hai nút, thêm NO 5 và bước Lưu và thoát ở EVT-14 (ponytail bậc 4). | 2026/10/19 | Claude |
| V0.7 | Đầu file, Sheet 3, 4 (4.1, 4.3, 4.4, 4.5), 5 (Khu vực K, L), 6 (K, L), 7 (7.1 NO 13-16, 7.2, 7.3 NO 9-10), 8 (EVT-16 đến EVT-20), 9 (NO 11-15), Câu hỏi mở Q13 | Owner 2026/10/20-21 (`DEC-2026-1020-student-workspace-split-and-password-in-settings`, phần PASSWORD): đổi/đặt mật khẩu **chỉ còn ở màn Thiết lập**, trang `profile` (`USR0502`) bỏ nút và popup, chỉ giữ dòng phương thức đăng nhập chỉ-đọc. Thêm nhóm "Bảo mật" (một dòng "Mật khẩu" kèm mô tả "đổi lần cuối" và nút mở popup) và popup đổi/đặt mật khẩu: ô hiện/ẩn, thanh độ mạnh **chỉ hiển thị**, danh sách bốn quy tắc cập nhật theo lúc gõ; quy tắc chặn vẫn 8-128 ký tự, có chữ và số. **Chuyển nguyên** mô tả hành vi, kiểm nhập liệu và thu hồi phiên khác từ BD `USR0502` (EVT-9 đến EVT-11, Sheet 9 NO 7, 10, 11) sang đây, đánh số mới từ cuối dãy, không đánh số lại. Bản dựng: `security-card.tsx`, `change-password-dialog.tsx`, `use-change-password.ts`; `Modal` dùng chung portal ra gốc shell; màn dùng chung với giảng viên nên nhóm Bảo mật cũng hiện ở Thiết lập giảng viên. Chỉ tài liệu, chưa duyệt hình | 2026/10/21 | Claude |
| V0.8 | Sheet 4 (4.1, 4.3), Sheet 5 (Khu vực C NO 2, Khu vực D NO 2, 3, Khu vực I NO 5), Sheet 6 (Khu vực D NO 2), Sheet 7 (7.1 NO 7, 8; 7.2 NO 1, 2), Sheet 9 (NO 7), các dòng `[Nguồn]` | Sửa 2026-10-22 đồng bộ DB, `DEC-2026-1022-identity-schema-sync` mục 6 (ID-07), mục 1 (ID-02) và `DEC-2026-1022-ai-judge-schema-sync` mục 1 (AI-03): (1) đổi mọi chỗ `identity.notification_preferences` thành `identity.user_preferences` (một bảng cho `streak_reminder_enabled`, `weekly_report_enabled`, `preferred_interview_level`; mặc định đã có ở `02-bd/database/identity.md:196`: bật, tắt, `MIDDLE`); tên repository đổi theo `[Suy luận]`; tên DTO `MyNotificationPreferencesDto` giữ nguyên; (2) mức người phỏng vấn ghi rõ là enum cố định 4 giá trị; (3) tự xoá tài khoản ghi thêm `deactivation_source = SELF` (job ẩn danh hoá chỉ quét `SELF`); (4) đọc lại số dòng mọi trích dẫn tới 4 file DB (đã dịch dòng; riêng các dòng lịch sử V0.1 đến V0.7 và Q4 giữ nguyên văn). Không đổi RD | 2026/10/22 | Claude |
| V0.9 | Sheet 4 (4.3), Sheet 5 (Khu vực D NO 3, 4), Sheet 6 (Khu vực D, NO 6), Sheet 7 (7.1 NO 10, 11; 7.2 NO 2, bảng cổng ra NO 3, 4), Sheet 8 (EVT-7), Sheet 9 (NO 5), Câu hỏi mở Q1, Q11 | Sửa 2026-10-22 (đồng bộ DB vòng 4, `DEC-2026-1022-db-sync-round4` mục 4, ID-23): hai tuỳ chọn "Số lượt tối đa mỗi phiên" và "Cho phép gợi ý" nay đọc và ghi bảng `identity.user_preferences` (cột `preferred_max_turns SMALLINT NOT NULL DEFAULT 12`, `preferred_hint_allowed BOOLEAN NOT NULL DEFAULT true`, `02-bd/database/identity.md` mục 1.12) qua `GetMySettings`/`UpdateMySettings` của `identity`, không còn qua `ai-review`. Hệ quả: nhóm Phỏng vấn giả lập không còn phụ thuộc phân hệ AI để đọc/lưu mặc định; các điểm còn nhắc `GetMyInterviewPreferences`/`UpdateMyInterviewPreferences` (`ai-review`) được đánh dấu lỗi thời, chưa viết lại toàn bộ luồng suy giảm (chờ chủ dự án duyệt, bản dựng prototype vẫn gọi hai mutation riêng, `05-coding/frontend/src/entities/user/model/types.ts:65-75`). Không đổi RD | 2026/10/22 | Claude |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, popup tô
> vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng khu Người học → Thiết lập

[Điều kiện mở] Mở menu người dùng trên header ngang rồi chọn mục "Cài đặt" (mục thứ tư trong năm mục)
[Nguồn: 02-bd/screens/users/_shell.md:62-64; 09-layoutBase/Cài đặt.dc.html:87].

[Chế độ mở] Không có.

[Thông tin truyền] Không có. Màn luôn làm việc trên tuỳ chọn của chính người đang đăng nhập, không nhận
tham số `user_id` từ bên ngoài.

[Giá trị trả về] Không có.

[Khi thành công] Tải bốn nhóm tuỳ chọn; nhóm Giao diện lấy giá trị từ bộ nhớ thiết bị nên hiển thị ngay,
ba nhóm còn lại chờ máy chủ trả về.

[Khi huỷ] Không có.

#### Thiết lập → Hồ sơ

[Điều kiện mở] Bấm liên kết "Về trang cá nhân" ở cuối cột phải
[Nguồn: 09-layoutBase/Cài đặt.dc.html:155].

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Điều hướng sang màn `profile`. Còn thay đổi chưa lưu thì mở popup xác nhận rời màn trước
(EVT-14), không rời thẳng.

[Khi huỷ] Ở lại màn `settings`, giữ nguyên mọi thay đổi chưa lưu.

#### Thiết lập → Đăng nhập và đăng ký

[Điều kiện mở] Xác nhận xoá tài khoản thành công trong popup `deleteConfirm`.

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Phiên đăng nhập bị chấm dứt, điều hướng sang màn `auth`. Mọi thay đổi chưa lưu trên màn
bị bỏ, **không** hỏi xác nhận lần hai — người dùng vừa xác nhận một hành động nặng hơn.

[Khi huỷ] Không có.

#### Thiết lập → Popup đổi / đặt mật khẩu

[Điều kiện mở] Bấm nút ở dòng "Mật khẩu" của nhóm Bảo mật (V0.7). Nhãn nút là "Đổi mật khẩu" khi tài khoản
đã có mật khẩu, "Đặt mật khẩu" khi tài khoản chỉ đăng ký qua OAuth (`password_hash IS NULL`)
[Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:19,42]. Trang `profile` **không còn**
mở popup này (`DEC-2026-1020-student-workspace-split-and-password-in-settings`); liên kết "Về trang cá nhân"
ở trên giữ nguyên.

[Chế độ mở] Popup chặn thao tác nền, hiển thị tại gốc khung của khu (shell) chứ không nằm trong thẻ nhóm
[Nguồn: 05-coding/frontend/src/shared/ui/overlay/modal.tsx:22,50].

[Thông tin truyền] Biến thể popup, suy từ `hasPassword` của hồ sơ đã tải.

[Giá trị trả về] Đã đổi / đã đặt mật khẩu, hoặc đã đóng.

[Khi thành công] Đóng popup, hiện toast thành công, làm mới dòng mô tả "đổi lần cuối" của nhóm Bảo mật
(EVT-19). Popup **không** tính là thay đổi chưa lưu của nút "Lưu cài đặt": có nút xác nhận riêng và không
dùng chung trạng thái với ba nhóm chờ Lưu.

[Khi huỷ] Đóng popup, xoá sạch ba ô mật khẩu, không hỏi xác nhận (EVT-20).

#### Thiết lập → Popup xác nhận xoá tài khoản

[Điều kiện mở] Bấm nút "Xoá tài khoản" trong khối Vùng nguy hiểm
[Nguồn: 09-layoutBase/Cài đặt.dc.html:131].

[Chế độ mở] Popup chặn thao tác nền.

[Thông tin truyền] Không có. Popup làm việc trên chính tài khoản đang đăng nhập.

[Giá trị trả về] Đã xác nhận hoặc đã huỷ.

[Khi thành công] Tài khoản chuyển `DEACTIVATED`, chuyển sang màn `auth`
[Nguồn: 02-bd/security/identity.md:60-62].

[Khi huỷ] Đóng popup, màn giữ nguyên trạng thái, không gọi máy chủ.

#### Thiết lập → Popup xác nhận rời màn

[Điều kiện mở] Rời màn bằng bất kỳ đường nào (mục nav, menu người dùng, liên kết "Về trang cá nhân", nút
lùi trình duyệt) trong lúc còn thay đổi chưa lưu ở ba nhóm Workspace, Phỏng vấn giả lập, Thông báo.

[Chế độ mở] Popup chặn thao tác nền.

[Thông tin truyền] Đích điều hướng đang chờ.

[Giá trị trả về] Ở lại, thoát không lưu, hoặc lưu rồi thoát.

[Khi thành công] Thoát không lưu thì bỏ toàn bộ thay đổi chưa lưu và điều hướng tới đích đang chờ; Lưu và thoát thì lưu xong mới điều hướng.

[Khi huỷ] Ở lại màn, giữ nguyên thay đổi chưa lưu.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Menu người dùng<br/>khung khu Người học"] -->|"chọn Cài đặt"| main["Thiết lập<br/>settings"]
    main -->|"Về trang cá nhân"| profile["Hồ sơ<br/>profile"]
    main -->|"Đổi / Đặt mật khẩu"| pwd["Popup đổi / đặt mật khẩu"]
    main -->|"Xoá tài khoản"| del["Popup xác nhận xoá tài khoản"]
    del -->|"xác nhận"| auth["Đăng nhập và đăng ký<br/>auth"]
    main -->|"rời màn khi còn thay đổi chưa lưu"| leave["Popup xác nhận rời màn"]
    leave -->|"rời đi"| profile

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000
    classDef popup fill:#FFF8E1,stroke:#C4820E,color:#000

    class nav,profile,auth source
    class main screen
    class del,leave,pwd popup
```

[Nguồn: 09-layoutBase/Cài đặt.dc.html:87,131,155; 02-bd/screens/users/_shell.md:62-64;
05-coding/frontend/src/views/users/settings/ui/security-card.tsx:35-50]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho người học tự đặt **tuỳ chọn hành vi của ứng dụng**: giao diện sáng/tối và ngôn ngữ
giao diện, cách cư xử của vùng soạn code (F1-20), tham số mặc định của phiên phỏng vấn tự luyện (F5-28),
email định kỳ (F1-21), xuất dữ liệu cá nhân (F1-22) và tự xoá tài khoản (F1-16)
[Nguồn: 01-rd/req/identity.md:197-202,231-236; 01-rd/req/ai-review.md:103-107].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: nhóm Giao diện hiển thị ngay từ bộ nhớ thiết bị, không chờ máy chủ. Ba nhóm còn
   lại tải từ máy chủ; nhóm Phỏng vấn giả lập tải bằng một lời gọi riêng sang `ai-review`. Nhóm Bảo mật
   và Vùng nguy hiểm chỉ hiện sau khi hồ sơ (`GetMyProfile`) tải xong
   [Nguồn: 05-coding/frontend/src/views/users/settings/ui/settings-view.tsx:38-60].
2. **Đổi tuỳ chọn Giao diện**: chọn Sáng/Tối hoặc Tiếng Việt/English thì áp **ngay lập tức** trên toàn bộ
   ứng dụng, không cần bấm Lưu và không gọi máy chủ
   [Nguồn: 01-rd/screens/users/USR0503_settings.md:94].
3. **Đổi tuỳ chọn ba nhóm còn lại**: giá trị mới chỉ nằm trong màn, chưa có hiệu lực thật cho tới khi bấm
   "Lưu cài đặt" [Nguồn: 01-rd/screens/users/USR0503_settings.md:95].
4. **Xuất dữ liệu**: bấm một trong hai nút để tải về tệp dữ liệu của chính mình.
5. **Xoá tài khoản**: bấm nút ở Vùng nguy hiểm, xác nhận trong popup, tài khoản chuyển `DEACTIVATED` ngay
   và phiên đăng nhập chấm dứt.
6. **Đổi / đặt mật khẩu** (V0.7): bấm nút ở nhóm Bảo mật, nhập mật khẩu trong popup (đây là **nơi duy nhất**
   của hệ thống cho việc này sau khi đăng nhập), xác nhận thì mật khẩu đổi ngay, không qua nút "Lưu cài đặt".
   Có hiệu lực tức thì và đăng xuất các thiết bị khác (EVT-19).

[Người dùng] Người học đã đăng nhập (`STUDENT`). Màn chỉ làm việc trên tuỳ chọn của chính người đang đăng
nhập.

[Tệp liên quan] Hai tệp **xuất ra**: lượt nộp dạng CSV và hội thoại phỏng vấn dạng JSON
[Nguồn: 09-layoutBase/Cài đặt.dc.html:269-272]. Màn **không** nhập tệp nào.

[Phạm vi]
- Đọc và ghi tuỳ chọn của chính người đăng nhập. Không có đường nào xem hay sửa tuỳ chọn của người khác.
- **Không** cấu hình ngôn ngữ lập trình được phép nộp bài, hệ số giới hạn hay tham số sandbox — việc của
  `admin_language_config` (`ADM0501`).
- **Không** sửa tên hiển thị, ảnh đại diện, email hay liên kết OAuth — việc của `profile`. **Mật khẩu là
  ngoại lệ, thuộc màn này từ V0.7** (chỉ đổi hoặc đặt; danh sách phương thức đăng nhập đã liên kết ở lại
  `profile`, màn này chỉ nhắc tên nhà cung cấp trong dòng mô tả khi tài khoản chưa có mật khẩu).
- Màn dùng chung cho Giảng viên qua thuộc tính `area`, nên nhóm Bảo mật cũng hiện ở Thiết lập của giảng
  viên [Nguồn: 05-coding/frontend/src/views/users/settings/ui/settings-view.tsx:24-28,66].
- Nhóm Phỏng vấn giả lập lấy và ghi dữ liệu qua `ai-review` nên **phải suy giảm êm**: phân hệ AI hỏng thì
  ba nhóm còn lại vẫn xem và lưu được bình thường. Đây là ràng buộc bắt buộc của dự án, không phải lựa
  chọn thiết kế.

[Quyền sử dụng]
- Xem: được, với chính tuỳ chọn của mình.
- Thêm: không có bản ghi nào do người dùng tạo mới trực tiếp; dòng `user_preferences` được tạo
  ngầm ở lần lưu đầu tiên.
- Sửa: được, với chính tuỳ chọn của mình.
- Xoá: không xoá bản ghi nào. "Xoá tài khoản" là **đổi trạng thái**, không phải xoá dòng
  [Nguồn: 02-bd/database/identity.md:24,28].

[Số bản ghi tối đa] Màn không có danh sách và không phân trang. Bốn nhóm cố định, tổng cộng 11 tuỳ chọn:
Giao diện 2, Workspace 4, Phỏng vấn giả lập 3, Thông báo 2
[Nguồn: 09-layoutBase/Cài đặt.dc.html:215-260].

[Nguồn: 01-rd/screens/users/USR0503_settings.md:36-43,94-96; 02-bd/database/identity.md:23,196]

### 4.2 DTO liên quan

- `MySettingsDto`
- `MyWorkspacePreferencesDto`
- `MyNotificationPreferencesDto`
- `MyInterviewPreferencesDto`
- `MyDataExportDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/identity.md` và `03-dd/api/ai-review.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (8)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `identity.users` | [Nguồn: 02-bd/database/identity.md:8-43] |
| 2 | `identity.user_preferences` | [Nguồn: 02-bd/database/identity.md:196] |
| 3 | `judge.submissions` | [Nguồn: 02-bd/database/judge-orchestration.md:8-43] |
| 4 | `ai.interview_sessions` | [Nguồn: 02-bd/database/ai-review.md:95-118] |
| 5 | `ai.interview_turns` | [Nguồn: 02-bd/database/ai-review.md:120-140] |
| 6 | `judge.language_configs` | Chỉ đọc cột `enabled` để dựng danh sách ngôn ngữ nộp bài đang bật [Nguồn: 02-bd/database/judge-orchestration.md:101-116] |
| 7 | `identity.refresh_tokens` | Nhóm Bảo mật: đổi mật khẩu thành công ghi `revoked_at` cho các phiên khác, giữ phiên đang thao tác [Nguồn: 02-bd/database/identity.md:89-100; 03-dd/logic/identity.md:94-97] |
| 8 | `identity.oauth_identities` | Nhóm Bảo mật: chỉ đọc `provider` để nêu tên nhà cung cấp khi tài khoản chưa có mật khẩu [Nguồn: 02-bd/database/identity.md:83-87] |

Cột dùng cho nhóm Bảo mật nằm ở bảng số 1: `password_hash` (nullable, `NULL` = chưa từng đặt mật khẩu) và
`password_changed_at` (nullable, `NULL` = chưa từng đổi) [Nguồn: 02-bd/database/identity.md:15,20].

Tập giá trị của `default_language` vẫn là enum khoá cứng `JAVA`/`CPP`/`PYTHON`, nhưng **danh sách nút hiển thị**
là danh sách động: chỉ các ngôn ngữ có `judge.language_configs.enabled = true` (Q4, chốt 2026-10-18)
[Nguồn: 02-bd/database/judge-orchestration.md:111]. Màn đọc qua cổng ra của `judge-orchestration`, không
truy vấn chéo schema.

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Cài đặt.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải** design system cuối
cùng.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Header ngang dính trên (khung chung khu Người học) | `:46-93` | Thương hiệu, 6 mục nav, **bộ chuyển Sáng/Tối và VI/EN**, menu người dùng đang mở với mục "Cài đặt" được tô sáng |
| Cột trái — bốn nhóm tuỳ chọn | `:97-122` | Một vòng lặp dựng bốn khối giống hệt nhau: tiêu đề nhóm, rồi từng dòng gồm tên tuỳ chọn, một câu mô tả, và một điều khiển bên phải (cụm nút phân đoạn hoặc công tắc) |
| Cột trái — Bảo mật (V0.7) | Không có trong prototype; bản dựng [Nguồn: 05-coding/frontend/src/views/users/settings/ui/settings-view.tsx:38-40] | Thẻ tiêu đề "Bảo mật", một dòng "Mật khẩu" gồm câu mô tả và nút có biểu tượng chìa khoá. Đặt **giữa** nhóm chờ Lưu và Vùng nguy hiểm, chỉ hiện khi hồ sơ đã tải |
| Cột trái — Vùng nguy hiểm | `:124-133` | Viền cảnh báo, tiêu đề, câu mô tả hệ quả, nút "Xoá tài khoản" |
| Cột phải — Trạng thái và lưu | `:136-141` | Tiêu đề "Trạng thái", một câu nhắc chế độ lưu, nút "Lưu cài đặt" chiếm hết chiều ngang |
| Cột phải — Dữ liệu của bạn | `:143-153` | Hai nút xuất, mỗi nút có nhãn bên trái và định dạng tệp bên phải |
| Cột phải — Lối tắt hồ sơ | `:155` | Một liên kết "Về trang cá nhân" dạng nút viền |
| Chân trang (khung chung khu Người học) | Không có trong file này | Prototype của màn này **không dựng** `<footer>`; khung chung vẫn áp chân trang cho mọi màn của khu, đây là divergence có chủ đích đã ghi ở khung [Nguồn: 02-bd/screens/users/_shell.md:92-95] |

Bố cục hai cột `minmax(0, 1fr) 320px` (`:95`): cột trái chứa sáu khối rộng (thêm Bảo mật từ V0.7), cột phải chứa ba khối hẹp.
Giữ nguyên cấu trúc này khi dựng Next.js; không quy định màu sắc, khoảng cách hay typography ở BD.

**Năm điểm màn hình này khác prototype, cố ý** (điểm 5 thêm ở V0.7):

1. **Bổ sung popup xác nhận xoá tài khoản.** Prototype cho bấm thẳng nút "Xoá tài khoản" mà không có bước
   xác nhận nào (`:131` là một `<button>` không gắn `onClick`). RD lại yêu cầu rõ "Khi tôi xác nhận"
   [Nguồn: 01-rd/screens/users/USR0503_settings.md:96], và đây là hành động không hoàn tác được trong 30
   ngày ân hạn [Nguồn: 02-bd/security/identity.md:63]. BD này bổ sung popup.
2. **Bổ sung popup xác nhận rời màn.** Ba nhóm chờ nút Lưu nên màn có trạng thái "còn thay đổi chưa lưu";
   prototype không dựng đường rời màn nào nên không lộ ra vấn đề này. Không có popup thì thay đổi mất âm
   thầm.
3. **Sửa câu chữ Vùng nguy hiểm.** Prototype ghi "Xoá vĩnh viễn tài khoản cùng toàn bộ lượt nộp, bài đã
   lưu và phiên phỏng vấn" [Nguồn: 09-layoutBase/Cài đặt.dc.html:129] — **sai** so với F1-16 đã chốt: khoá
   mềm rồi ẩn danh hoá, dữ liệu không mất [Nguồn: 01-rd/req/identity.md:197-202]. RD đã chốt sửa câu chữ,
   đánh dấu **[Đợi nextjs]** [Nguồn: 01-rd/screens/users/USR0503_settings.md:78]. BD này ghi câu chữ đúng
   ở Sheet 5 Khu vực H NO 2 và không sửa file prototype.
4. **Bỏ hiển thị song ngữ đồng thời.** Prototype hiển thị cả nhãn tiếng Việt và tiếng Anh cạnh nhau. Đó là
   cách bản mẫu minh hoạ i18n, không phải yêu cầu hiển thị song ngữ cùng lúc — cùng kết luận đã ghi ở
   khung [Nguồn: 02-bd/screens/users/_shell.md:69-74].
5. **Bổ sung nhóm Bảo mật và popup đổi / đặt mật khẩu** (V0.7). Prototype `Cài đặt.dc.html` không có chữ
   "mật khẩu" nào; đổi mật khẩu từng ở trang `profile` (F1-19 [Nguồn: 01-rd/req/identity.md:46]), owner chuyển
   hẳn sang màn này ngày 2026/10/20 để hai nơi vào không lệch nhau
   [Nguồn: .nexa/control/decision-registry.md:4245-4255].

**Bốn marker `[Đợi nextjs]` của RD** [Nguồn: 01-rd/screens/users/USR0503_settings.md:19,43,73,78] là hành
vi **cố ý hoãn tới lúc dựng UI thật**, không phải khoảng trống thiết kế: chi tiết trình bày thuần hình
thức của bốn nhóm tuỳ chọn, và câu chữ chính xác của Vùng nguy hiểm. BD này chốt **cấu trúc, nguồn dữ
liệu và chế độ lưu** của từng mục; câu chữ hiển thị cuối cùng chốt khi dựng Next.js.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/users/settings` | Quy ước FSD của dự án |
| Khung khu Người học | Dùng lại `widgets/app-shell` | `02-bd/screens/users/_shell.md` |
| Nhóm Giao diện | Dùng lại `features/theme-switch` + `features/locale-switch` của khung | Prototype `:60,63` và `:215-225` là **cùng một cặp điều khiển**, không dựng hai lần |
| Ba nhóm chờ nút Lưu | `widgets/settings-form` + `features/save-my-settings` | Prototype `:97-122`, `:136-141` |
| Xuất dữ liệu | `features/export-my-data` | Prototype `:143-153` |
| Vùng nguy hiểm và popup xác nhận | `features/deactivate-my-account` | Prototype `:124-133` |
| Nhóm Bảo mật (V0.7) | `views/users/settings/ui/security-card.tsx` (thẻ) + `features/change-my-password` (popup và hook `use-change-password`) | Bản dựng [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:15-53; 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:63] |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại. Điểm đáng chú ý: nhóm Giao diện **không
có slice riêng** — nó là cùng hai điều khiển mà khung đã có, chỉ trình bày ở dạng hàng đầy đủ thay vì
dạng nút nhỏ trên header.

**Hiện trạng bản dựng nhóm Bảo mật (V0.7, chưa duyệt hình).** Bảng giúp đối chiếu BD với code đã dựng.

| Hạng mục | Hiện trạng | Nguồn |
| :--- | :--- | :--- |
| Vị trí | Cột trái, giữa `PreferencesForm` và `DangerZoneCard`, chỉ dựng khi hồ sơ đã tải | `settings-view.tsx:35-60` |
| Thẻ | Tiêu đề "Bảo mật", một `SettingRow` "Mật khẩu", nút ghost có biểu tượng chìa khoá | `security-card.tsx:33-44` |
| Popup | `ChangePasswordDialog` dùng chung `features/change-my-password`; hook giữ ba ô, lỗi theo ô, trạng thái đang gửi | `change-password-dialog.tsx:63-81`, `use-change-password.ts:12-80` |
| Ô nhập | `TextField` có thuộc tính `trailing` chứa nút mắt, tối đa 128 ký tự | `text-field.tsx:21,79-93` |
| Độ mạnh và quy tắc | Thanh bốn mức chỉ hiển thị, bốn dòng quy tắc cập nhật theo lúc gõ | `change-password-dialog.tsx:85-99,156-207` |
| Quy tắc chặn | `changePasswordSchema`: 8-128 ký tự, có chữ, có số, hai ô khớp, thiếu mật khẩu hiện tại thì lỗi | `schema.ts:50-71` |
| Modal | Portal tới `.admin-shell`, `.instructor-shell` hoặc `.student-shell` gần nhất, không có thì `<body>` (sửa lỗi hộp thoại bị kẹt trong thẻ kính mờ) | `modal.tsx:22,50` |
| Dùng chung | Màn nhận `area` ("student" hoặc "instructor") nên nhóm Bảo mật hiện cả ở Thiết lập giảng viên | `settings-view.tsx:24-28` |
| Dữ liệu | `changeMyPassword` đang là mock (`withMockData`), chưa nối máy chủ | `mutations.ts:43-44` |

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/users/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.
>
> Mỗi dòng tuỳ chọn trong bốn nhóm đều có thêm **một câu mô tả tĩnh** bên dưới tên
> [Nguồn: 09-layoutBase/Cài đặt.dc.html:107]. Câu mô tả là nhãn tĩnh i18n, không lấy từ cột DB nào, nên
> không tách thành item riêng; nội dung ghi ở cột Ghi chú của chính tuỳ chọn đó.

### Khu vực A — Nhóm Giao diện

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm Giao diện | | | | | | | | | | | | | |
| | 1 | Tiêu đề nhóm | `settings.appearance.title` | - | - | Label | String | - | - | O | Giao diện | - | Tiêu đề nhóm<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:216]<br>[EVT liên quan] - |
| | 2 | Chủ đề màu | `settings.appearance.theme` | - | - | Button | Enum | - | Có | I/O | Sáng | 2 nút phân đoạn | Hai lựa chọn "Sáng" / "Tối". Mô tả: "Áp cho toàn bộ màn hình và được ghi nhớ trên thiết bị này". Áp **ngay**, không chờ nút Lưu<br>[Nguồn giá trị] Bộ nhớ thiết bị qua `next-themes`, **không phải cột DB** — `DEC-2026-0825-frontend-base-architecture` chốt `next-themes` với `defaultTheme="light"`; giá trị mặc định "Sáng" khớp `DEC-2026-0824-dark-light-theme`. Xem Câu hỏi mở Q2<br>[EVT liên quan] EVT-2 |
| | 3 | Ngôn ngữ giao diện | `settings.appearance.uiLanguage` | - | - | Button | Enum | - | Có | I/O | Tiếng Việt | 2 nút phân đoạn | Hai lựa chọn "Tiếng Việt" / "English". Mô tả: "Nội dung đề bài vẫn giữ ngôn ngữ gốc". Áp **ngay**, không chờ nút Lưu. **Không phải** ngôn ngữ nộp bài — xem NO 2 Khu vực B<br>[Nguồn giá trị] Cookie `NEXT_LOCALE`, **không phải cột DB** — `DEC-2026-0825-frontend-base-architecture` chốt `next-intl` không có đoạn locale trên URL, locale lấy từ cookie. Tập giá trị vi/en theo `DEC-2026-0824-i18n-vi-en`. Xem Câu hỏi mở Q2<br>[EVT liên quan] EVT-3 |

### Khu vực B — Nhóm Workspace

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm Workspace | | | | | | | | | | | | | |
| | 1 | Tiêu đề nhóm | `settings.workspace.title` | - | - | Label | String | - | - | O | Workspace | - | Tiêu đề nhóm<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:227]<br>[EVT liên quan] - |
| | 2 | Ngôn ngữ mặc định | `settings.workspace.defaultLanguage` | `identity.users` | `default_language` | Button | Enum | - | Có | I/O | Python 3 | Một nút phân đoạn cho mỗi ngôn ngữ đang bật | Ngôn ngữ **nộp bài** được chọn sẵn khi mở một bài toán. Mô tả: "Được chọn sẵn khi mở một bài toán". Chờ nút Lưu. **Danh sách nút là danh sách động** các ngôn ngữ có `judge.language_configs.enabled = true`; ngôn ngữ đã lưu mà bị admin tắt thì màn hiển thị ngôn ngữ **đầu tiên đang bật** (Q4, chốt 2026-10-18)<br>[Nguồn giá trị] Cột `default_language` [Nguồn: 02-bd/database/identity.md:23]; danh sách bật lấy từ cột `enabled` [Nguồn: 02-bd/database/judge-orchestration.md:111]. Mã enum `JAVA`/`CPP`/`PYTHON`, nhãn "Java 21" / "C++ 17" / "Python 3" là nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:232]. Giá trị mặc định "Python 3" lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:165]. Bản dựng: hằng số có cờ `enabled` thay cho dữ liệu thật [Nguồn: 05-coding/frontend/src/views/users/settings/ui/preferences-form.tsx:25-33,102-108]<br>[EVT liên quan] EVT-4 |
| | 3 | Cỡ chữ editor | `settings.workspace.editorFontSize` | - | - | Button | Enum | - | Có | I/O | 14 | 3 nút phân đoạn | Ba lựa chọn "13" / "14" / "16". Mô tả: "Chỉ áp cho vùng soạn code". Chờ nút Lưu<br>[Nguồn giá trị] **Chưa có cột lưu.** F1-20 yêu cầu lưu theo tài khoản [Nguồn: 01-rd/req/identity.md:231-232] nhưng `02-bd/database/identity.md` mục 1.1 và 1.12 không có cột nào cho giá trị này — **đây là một câu hỏi mở, không phải thiết kế đã chốt**, xem Câu hỏi mở Q1<br>[EVT liên quan] EVT-4 |
| | 4 | Tự lưu bản nháp | `settings.workspace.autosaveDraft` | - | - | Toggle | Boolean | - | Có | I/O | Bật | Công tắc | Mô tả: "Giữ lại code chưa nộp của từng bài, tải lại trang không mất". Chờ nút Lưu<br>[Nguồn giá trị] **Chưa có cột lưu** — xem Câu hỏi mở Q1. Giá trị mặc định "Bật" lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:165]<br>[EVT liên quan] EVT-4 |
| | 5 | Phím tắt Vim | `settings.workspace.vimMode` | - | - | Toggle | Boolean | - | Có | I/O | Tắt | Công tắc | Mô tả: "Áp dụng trong vùng soạn code". Chờ nút Lưu<br>[Nguồn giá trị] **Chưa có cột lưu** — xem Câu hỏi mở Q1. Giá trị mặc định "Tắt" lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:165]<br>[EVT liên quan] EVT-4 |

### Khu vực C — Nhóm Phỏng vấn giả lập

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm Phỏng vấn giả lập | | | | | | | | | | | | | |
| | 1 | Tiêu đề nhóm | `settings.interview.title` | - | - | Label | String | - | - | O | Phỏng vấn giả lập | - | Tiêu đề nhóm<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:240]<br>[EVT liên quan] - |
| | 2 | Mức người phỏng vấn | `settings.interview.interviewerLevel` | `identity.user_preferences` | `preferred_interview_level` | Button | Enum | - | Có | I/O | Middle | 4 nút phân đoạn | Bốn lựa chọn "Intern" / "Junior" / "Middle" / "Senior" (`DEC-2026-0922-users-and-admin-conflict-resolutions`). Mô tả: "Quyết định độ sâu của các câu hỏi truy vấn". Chờ nút Lưu<br>[Nguồn giá trị] Cột `preferred_interview_level` trong bảng `identity.user_preferences` [Nguồn: 02-bd/database/identity.md:196]. Khớp 4 mức enum toàn hệ thống: `INTERN`/`JUNIOR`/`MIDDLE`/`SENIOR`, cố định (không do admin cấu hình, `DEC-2026-1022-ai-judge-schema-sync` mục 1); mặc định `MIDDLE` là mặc định ở DB (`02-bd/database/identity.md:196`)<br>[EVT liên quan] EVT-5 |
| | 3 | Số lượt tối đa mỗi phiên | `settings.interview.maxTurns` | `identity.user_preferences` | `preferred_max_turns` | Button | Enum | - | Có | I/O | 12 | 3 nút phân đoạn | Ba lựa chọn "8" / "12" / "16". Mô tả: "Đến giới hạn này phiên sẽ kết và trả nhận xét". Chờ nút Lưu<br>[Nguồn giá trị] **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** cột `preferred_max_turns` của `identity.user_preferences` (`SMALLINT NOT NULL DEFAULT 12`, ba giá trị 8/12/16) [Nguồn: 02-bd/database/identity.md mục 1.12]. ~~Chưa có cột lưu giá trị mặc định theo người dùng~~. Khi tạo phiên phỏng vấn, frontend truyền giá trị này vào `ai.interview_sessions.max_turns` [Nguồn: 02-bd/database/ai-review.md:106]. Ba giá trị 8/12/16 lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:246]. Xem Câu hỏi mở Q1<br>[EVT liên quan] EVT-5 |
| | 4 | Cho phép gợi ý | `settings.interview.hintAllowed` | `identity.user_preferences` | `preferred_hint_allowed` | Toggle | Boolean | - | Có | I/O | Bật | Công tắc | Mô tả: "Người phỏng vấn có thể gợi ý khi bạn bị bí". Chờ nút Lưu<br>[Nguồn giá trị] **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** cột `preferred_hint_allowed` của `identity.user_preferences` (`BOOLEAN NOT NULL DEFAULT true`) [Nguồn: 02-bd/database/identity.md mục 1.12]. ~~Chưa có cột lưu giá trị mặc định theo người dùng~~. Frontend truyền giá trị này vào `ai.interview_sessions.hint_allowed` khi tạo phiên [Nguồn: 02-bd/database/ai-review.md:107]. Xem Câu hỏi mở Q1<br>[EVT liên quan] EVT-5 |
| | 5 | Dòng phạm vi áp dụng | `settings.interview.scopeNote` | - | - | Label | String | 140 | - | O | - | Câu tĩnh | Câu nói rõ ba tuỳ chọn trên **chỉ áp cho phiên tự luyện**, không áp cho phiên mở từ một bài nộp `Accepted`<br>[Nguồn giá trị] Nhãn tĩnh i18n, nội dung "Ba tuỳ chọn này chỉ áp cho phiên bạn tự mở từ ngân hàng câu hỏi hoặc từ chủ đề tự chọn. Phiên mở từ một bài nộp Accepted luôn dùng tham số chuẩn." Căn cứ F5-28 [Nguồn: 01-rd/req/ai-review.md:105-107]. Prototype **không có** câu này, BD bổ sung vì thiếu nó người dùng hiểu sai phạm vi<br>[EVT liên quan] - |
| | 6 | Dòng suy giảm khi AI hỏng | `settings.interview.degradedNotice` | - | - | Label | String | 140 | - | O | - | Câu tĩnh | Câu thay thế toàn bộ nội dung nhóm khi `ai-review` không phản hồi<br>[Nguồn giá trị] Nhãn tĩnh i18n, nội dung "Chưa lấy được tuỳ chọn phỏng vấn giả lập. Các nhóm còn lại vẫn xem và lưu được bình thường."<br>[EVT liên quan] EVT-1 |

### Khu vực D — Nhóm Thông báo

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm Thông báo | | | | | | | | | | | | | |
| | 1 | Tiêu đề nhóm | `settings.notification.title` | - | - | Label | String | - | - | O | Thông báo | - | Tiêu đề nhóm<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:253]<br>[EVT liên quan] - |
| | 2 | Nhắc luyện tập | `settings.notification.streakReminder` | `identity.user_preferences` | `streak_reminder_enabled` | Toggle | Boolean | - | Có | I/O | Bật | Công tắc | Mô tả: "Một email mỗi ngày khi chuỗi ngày luyện tập sắp mất". Chờ nút Lưu<br>[Nguồn giá trị] Cột `streak_reminder_enabled` [Nguồn: 02-bd/database/identity.md:196], phục vụ F1-21 [Nguồn: 01-rd/req/identity.md:233-234]<br>[EVT liên quan] EVT-6 |
| | 3 | Báo cáo hằng tuần | `settings.notification.weeklyReport` | `identity.user_preferences` | `weekly_report_enabled` | Toggle | Boolean | - | Có | I/O | Tắt | Công tắc | Mô tả: "Tiến độ theo chủ đề, gửi vào thứ Hai". Chờ nút Lưu<br>[Nguồn giá trị] Cột `weekly_report_enabled` [Nguồn: 02-bd/database/identity.md:196]. Giá trị mặc định "Tắt" lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:167]<br>[EVT liên quan] EVT-6 |

### Khu vực E — Khối trạng thái và lưu

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Khối trạng thái và lưu | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `settings.saveBar.title` | - | - | Label | String | - | - | O | Trạng thái | - | Tiêu đề khối<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:138]<br>[EVT liên quan] - |
| | 2 | Câu trạng thái | `settings.saveBar.hint` | - | - | Label | String | 120 | - | O | - | Câu tĩnh, hai biến thể | Câu nhắc chế độ lưu, đổi theo trạng thái màn<br>[Công thức] Chưa có thay đổi chưa lưu: "Chủ đề màu và ngôn ngữ áp ngay; các mục còn lại lưu bằng nút bên dưới." Có thay đổi chưa lưu: "Bạn có thay đổi chưa lưu." Hai câu là nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:264-267]; biến thể sau là BD bổ sung. Câu "vừa lưu xong" của prototype bỏ: kết quả lưu báo bằng toast. Bản dựng hiện tại chỉ hiện câu mặc định [Nguồn: 05-coding/frontend/src/views/users/settings/ui/preferences-form.tsx:175]<br>[EVT liên quan] EVT-4, EVT-5, EVT-6, EVT-7 |
| | 3 | Lưu cài đặt | `settings.saveBar.btnSave` | - | - | Button | - | - | - | I | - | Lưu cài đặt | Ghi ba nhóm Workspace, Phỏng vấn giả lập, Thông báo trong một lần bấm. **Không** ghi nhóm Giao diện<br>[Nguồn giá trị] Nhãn tĩnh i18n; giữ nguyên "Lưu cài đặt" sau khi lưu, kết quả báo bằng toast (prototype từng đổi nhãn thành "Đã lưu" [Nguồn: 09-layoutBase/Cài đặt.dc.html:263])<br>[EVT liên quan] EVT-7 |

### Khu vực F — Khối xuất dữ liệu

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Khối xuất dữ liệu | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `settings.dataExport.title` | - | - | Label | String | - | - | O | Dữ liệu của bạn | - | Tiêu đề khối<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:144]<br>[EVT liên quan] - |
| | 2 | Xuất lượt nộp | `settings.dataExport.btnExportSubmissions` | `judge.submissions` | - | Button | - | - | - | I | - | Xuất lượt nộp · CSV | Tải về toàn bộ lượt nộp của chính người đăng nhập dạng CSV (F1-22)<br>[Nguồn giá trị] Nhãn tĩnh i18n kèm nhãn định dạng "CSV" [Nguồn: 09-layoutBase/Cài đặt.dc.html:270]; dữ liệu đọc từ `judge.submissions` qua cổng ra [Nguồn: 02-bd/database/judge-orchestration.md:8-43]. Trả tệp **đồng bộ** kèm giới hạn tần suất (Q5, chốt 2026-10-18); tập cột của tệp CSV thuộc DD<br>[EVT liên quan] EVT-8 |
| | 3 | Xuất hội thoại phỏng vấn | `settings.dataExport.btnExportInterviews` | `ai.interview_turns` | - | Button | - | - | - | I | - | Xuất hội thoại phỏng vấn · JSON | Tải về toàn bộ hội thoại phỏng vấn của chính người đăng nhập dạng JSON (F1-22)<br>[Nguồn giá trị] Nhãn tĩnh i18n kèm nhãn định dạng "JSON" [Nguồn: 09-layoutBase/Cài đặt.dc.html:271]; dữ liệu đọc từ `ai.interview_turns` ghép `ai.interview_sessions` qua cổng ra [Nguồn: 02-bd/database/ai-review.md:120-140]. `identity` sở hữu nghiệp vụ này; `ai-review` hỏng thì báo **lỗi bằng toast** (kênh phản hồi duy nhất, `DEC-2026-1003-toast-feedback-channel`) và **giữ nút kích hoạt** (Q6, chốt 2026-10-18). Bản dựng [Nguồn: 05-coding/frontend/src/views/users/settings/ui/data-export-card.tsx:39-43]<br>[EVT liên quan] EVT-9 |

### Khu vực G — Lối tắt hồ sơ

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Lối tắt hồ sơ | | | | | | | | | | | | | |
| | 1 | Về trang cá nhân | `settings.profileShortcut.linkProfile` | - | - | Link | - | - | - | I | - | Về trang cá nhân | Điều hướng sang màn `profile`<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:155]<br>[EVT liên quan] EVT-10 |

### Khu vực H — Vùng nguy hiểm

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Vùng nguy hiểm | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `settings.dangerZone.title` | - | - | Label | String | - | - | O | Vùng nguy hiểm | - | Tiêu đề khối<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:125]<br>[EVT liên quan] - |
| | 2 | Câu mô tả hệ quả | `settings.dangerZone.consequenceText` | - | - | Label | String | 200 | - | O | - | Câu tĩnh | Câu nói đúng hệ quả thật của F1-16<br>[Nguồn giá trị] Nhãn tĩnh i18n, nội dung "Tài khoản chuyển sang trạng thái ngừng hoạt động ngay. Trong 30 ngày ân hạn bạn có thể đăng nhập lại để huỷ yêu cầu xoá. Sau 30 ngày, thông tin định danh của bạn được ẩn danh hoá; lượt nộp, bài đã lưu và phiên phỏng vấn vẫn được giữ lại." Số 30 ngày lấy từ [Nguồn: 02-bd/security/identity.md:63]. **Thay cho** câu sai của prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:129], theo chốt của RD [Nguồn: 01-rd/screens/users/USR0503_settings.md:78]. Câu chữ nói rõ người dùng **đăng nhập lại trong 30 ngày để huỷ yêu cầu xoá** (banner khôi phục ở `SHR0101` Khu vực E, `RestoreDeactivatedAccount`; Q8, đảo ngược 2026-10-18 theo `DEC-2026-1018-account-deletion-grace-cancel`)<br>[EVT liên quan] - |
| | 3 | Xoá tài khoản | `settings.dangerZone.btnDelete` | - | - | Button | - | - | - | I | - | Xoá tài khoản | Mở popup xác nhận, **không** gọi máy chủ ngay<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Cài đặt.dc.html:131]<br>[EVT liên quan] EVT-11 |

### Khu vực I — Popup xác nhận xoá tài khoản

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup xác nhận xoá tài khoản | | | | | | | | | | | | | |
| | 1 | Tiêu đề popup | `settings.deleteConfirm.title` | - | - | Label | String | - | - | O | Xác nhận xoá tài khoản | - | Tiêu đề popup<br>[Nguồn giá trị] Nhãn tĩnh i18n. Popup là phần BD bổ sung, prototype không có — xem mục 4.4<br>[EVT liên quan] EVT-11 |
| | 2 | Câu mô tả hệ quả | `settings.deleteConfirm.consequenceText` | - | - | Label | String | 200 | - | O | - | Câu tĩnh | Nhắc lại đúng nội dung Khu vực H NO 2, để người dùng không phải nhớ lại câu vừa đọc<br>[Nguồn giá trị] Cùng nhãn tĩnh i18n với Khu vực H NO 2<br>[EVT liên quan] - |
| | 3 | Ô nhập xác nhận | `settings.deleteConfirm.confirmPhrase` | - | - | TextBox | String | 80 | Có | I | rỗng | Email của chính mình, không phân biệt hoa thường | Người dùng gõ lại email của chính mình để mở khoá nút xác nhận. So khớp bỏ khoảng trắng hai đầu và không phân biệt hoa thường, áp cho cả giá trị gõ và email của tài khoản (owner 2026/10/19, D38). Không dùng mật khẩu vì tài khoản chỉ đăng ký qua OAuth có thể chưa từng có mật khẩu [Nguồn: 02-bd/database/identity.md:15] (Q7 đã chốt 2026-10-18: xác nhận bằng gõ lại email)<br>[Nguồn giá trị] So khớp với `identity.users.email` của chính người đăng nhập [Nguồn: 02-bd/database/identity.md:14], so khớp thực hiện ở phía máy chủ<br>[EVT liên quan] EVT-12 |
| | 4 | Huỷ | `settings.deleteConfirm.btnCancel` | - | - | Button | - | - | - | I | - | Huỷ | Đóng popup, không gọi máy chủ<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-13 |
| | 5 | Xác nhận xoá | `settings.deleteConfirm.btnConfirm` | `identity.users` | `status`, `deactivated_at`, `deactivation_source` | Button | - | - | - | I | - | Xoá tài khoản của tôi | Gọi nghiệp vụ huỷ kích hoạt tài khoản<br>[Nguồn giá trị] Nhãn tĩnh i18n; hệ quả ghi vào `status`, `deactivated_at` và `deactivation_source = SELF` (job ẩn danh hoá chỉ quét dòng `SELF` quá 30 ngày ân hạn, `DEC-2026-1022-identity-schema-sync` mục 1) [Nguồn: 02-bd/database/identity.md:24,28]<br>[EVT liên quan] EVT-12 |

### Khu vực J — Popup xác nhận rời màn

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup xác nhận rời màn | | | | | | | | | | | | | |
| | 1 | Tiêu đề popup | `settings.leaveConfirm.title` | - | - | Label | String | - | - | O | Thay đổi chưa lưu | - | Tiêu đề popup<br>[Nguồn giá trị] Nhãn tĩnh i18n. Popup là phần BD bổ sung, prototype không có — xem mục 4.4<br>[EVT liên quan] EVT-14 |
| | 2 | Nội dung | `settings.leaveConfirm.message` | - | - | Label | String | 140 | - | O | - | Câu tĩnh | Câu cảnh báo<br>[Nguồn giá trị] Nhãn tĩnh i18n, nội dung "Bạn có thay đổi chưa lưu ở nhóm Workspace, Phỏng vấn giả lập hoặc Thông báo. Rời màn bây giờ thì các thay đổi đó bị bỏ."<br>[EVT liên quan] - |
| | 3 | Ở lại | `settings.leaveConfirm.btnStay` | - | - | Button | - | - | - | I | - | Ở lại | Đóng popup, giữ nguyên thay đổi chưa lưu<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-14 |
| | 4 | Thoát không lưu | `settings.leaveConfirm.btnLeave` | - | - | Button | - | - | - | I | - | Thoát không lưu | Bỏ thay đổi chưa lưu và điều hướng tới đích đang chờ<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-14 |
| | 5 | Lưu và thoát | `settings.leaveConfirm.btnSaveAndLeave` | - | - | Button | - | - | - | I | - | Lưu và thoát | Chạy lưu cài đặt như EVT-7; lưu được thì điều hướng tới đích đang chờ, không lưu được thì ở lại màn. Khoá cả ba nút trong lúc đang lưu<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-14, EVT-7 |

### Khu vực K — Nhóm Bảo mật (V0.7)

> Nhóm này **không có trong prototype**. Nội dung chuyển từ BD `USR0502` (mục `security` và `passwordChange`)
> sau `DEC-2026-1020-student-workspace-split-and-password-in-settings`. Nhãn i18n lấy từ namespace `settings.security.*` và
> `profile.security.*` [Nguồn: 05-coding/frontend/messages/vi.json:2604-2609,2763-2768].

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm Bảo mật | | | | | | | | | | | | | |
| | 1 | Tiêu đề nhóm | `settings.security.title` | - | - | Label | String | - | - | O | Bảo mật | - | Tiêu đề thẻ<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:33]<br>[EVT liên quan] - |
| | 2 | Nhãn dòng mật khẩu | `settings.security.passwordLabel` | - | - | Label | String | - | - | O | Mật khẩu | - | Tên của dòng duy nhất trong nhóm<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:34]<br>[EVT liên quan] - |
| | 3 | Mô tả trạng thái mật khẩu | `settings.security.passwordStatus` | `identity.users` | `password_changed_at`, `password_hash` (chỉ xét `IS NULL`) | Label | String | 120 | - | O | - | Câu động, ba biến thể | Có mật khẩu và có ngày đổi: "Đổi lần cuối {ngày}" (`dd/MM/yyyy`). Có mật khẩu nhưng `password_changed_at` là `NULL`: "Chưa từng đổi mật khẩu". Chưa có mật khẩu (tài khoản chỉ OAuth): "Chưa đặt mật khẩu — đang đăng nhập bằng {nhà cung cấp}", nhà cung cấp là phần tử đầu của `linkedProviders`, thiếu thì ghi "OAuth"<br>[Nguồn giá trị] Hồ sơ `GetMyProfile` (dùng lại của `USR0502`), cột `password_changed_at` nullable [Nguồn: 02-bd/database/identity.md:20]; chuỗi `profile.security.passwordChangedAt/passwordNoDate/noPassword` [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:21-30]<br>[EVT liên quan] EVT-19 |
| | 4 | Nút mở popup mật khẩu | `settings.security.btnPassword` | - | - | Button | - | - | - | I | - | "Đổi mật khẩu" hoặc "Đặt mật khẩu" | Biểu tượng chìa khoá bên trái nhãn. Nhãn "Đổi mật khẩu" khi `hasPassword`, "Đặt mật khẩu" khi chưa có<br>[Nguồn giá trị] Nhãn tĩnh i18n `settings.security.changeBtn/setBtn` [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:35-43]<br>[EVT liên quan] EVT-16 |

### Khu vực L — Popup đổi / đặt mật khẩu (V0.7)

> Hai biến thể theo `hasPassword`: "Đổi mật khẩu" (tài khoản đã có mật khẩu) và "Đặt mật khẩu" (tài khoản chỉ
> OAuth, không có ô mật khẩu hiện tại). Nhãn i18n: `profile.passwordDialog.*`
> [Nguồn: 05-coding/frontend/messages/vi.json:2637-2663]. Ba ô mật khẩu đều có nút mắt hiện / ẩn đặt ở mép
> phải ô, nhờ thuộc tính `trailing` của `TextField` dùng chung
> [Nguồn: 05-coding/frontend/src/shared/ui/form/text-field.tsx:21,79-93; 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:21-61].

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup đổi / đặt mật khẩu | | | | | | | | | | | | | |
| | 1 | Tiêu đề popup | `settings.passwordChange.title` | - | - | Label | String | - | - | O | Đổi mật khẩu | - | "Đổi mật khẩu" hoặc "Đặt mật khẩu" theo biến thể<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:111-115]<br>[EVT liên quan] EVT-16 |
| | 2 | Mật khẩu hiện tại | `settings.passwordChange.currentPassword` | `identity.users` | `password_hash` (đối chiếu phía máy chủ) | TextBox | String | 128 | Có | I | rỗng | Che ký tự, có nút hiện / ẩn | **Chỉ có ở biến thể "Đổi mật khẩu"**. Máy chủ đối chiếu với `password_hash`, không gửi hash về màn [Nguồn: 02-bd/security/identity.md:5]; viền đỏ khi thiếu hoặc khi máy chủ từ chối<br>[Nguồn giá trị] Người dùng nhập [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:138-147]<br>[EVT liên quan] EVT-19 |
| | 3 | Mật khẩu mới | `settings.passwordChange.newPassword` | `identity.users` | `password_hash` (băm phía máy chủ) | TextBox | String | 128 | Có | I/O | rỗng | Che ký tự, có nút hiện / ẩn | Mỗi lần gõ cập nhật thanh độ mạnh (NO 4) và danh sách quy tắc (NO 6)<br>[Nguồn giá trị] Người dùng nhập; băm BCrypt phía máy chủ [Nguồn: 02-bd/security/identity.md:5]; giới hạn 128 ký tự ở thuộc tính ô [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:44,148-155]<br>[EVT liên quan] EVT-17, EVT-19 |
| | 4 | Thanh độ mạnh | `settings.passwordChange.strengthBar` | - | - | Label | Enum | - | - | O | - | Thanh bốn mức kèm nhãn "Yếu" / "Trung bình" / "Tốt" / "Mạnh" | **Chỉ hiển thị, không chặn gửi.** Chỉ hiện khi ô mật khẩu mới không rỗng. Điểm = số quy tắc đạt trong ba quy tắc đầu (đủ 8 ký tự, có chữ, có số) cộng 1 nếu từ 12 ký tự trở lên, tối đa 4; điểm 0-1 là "Yếu", 2 là "Trung bình", 3 là "Tốt", 4 là "Mạnh"; màu theo mức (hai mức đầu đỏ, mức 2 vàng, hai mức cuối xanh)<br>[Nguồn giá trị] Tính ở màn từ giá trị ô NO 3, không lưu, không gửi máy chủ [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:11-18,94-99,156-179]<br>[EVT liên quan] EVT-17 |
| | 5 | Nhập lại mật khẩu mới | `settings.passwordChange.confirmPassword` | - | - | TextBox | String | 128 | Có | I | rỗng | Che ký tự, có nút hiện / ẩn | Ô xác nhận, chỉ tồn tại ở màn, không gửi lên máy chủ<br>[Nguồn giá trị] Người dùng nhập [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:180-187]<br>[EVT liên quan] EVT-17, EVT-19 |
| | 6 | Danh sách quy tắc | `settings.passwordChange.ruleList` | - | - | Label | List | - | - | O | - | Bốn dòng, mỗi dòng có dấu "đạt" hoặc "chưa đạt" | Bốn quy tắc cập nhật theo lúc gõ: (1) tối thiểu 8 ký tự, (2) có ít nhất một chữ cái, (3) có ít nhất một chữ số, (4) hai mật khẩu khớp nhau (chỉ đạt khi ô nhập lại không rỗng). Dòng đạt đổi màu thành công; **không** chặn nút Xác nhận, việc chặn nằm ở Sheet 9 NO 11<br>[Nguồn giá trị] Tính ở màn [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:85-93,188-207]; nhãn `profile.passwordDialog.rule.*` [Nguồn: 05-coding/frontend/messages/vi.json:2657-2662]<br>[EVT liên quan] EVT-17 |
| | 7 | Huỷ | `settings.passwordChange.btnCancel` | - | - | Button | - | - | - | I | - | Huỷ | Đóng popup, xoá sạch ba ô, không hỏi xác nhận<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:118-125]<br>[EVT liên quan] EVT-20 |
| | 8 | Xác nhận | `settings.passwordChange.btnConfirm` | `identity.users`, `identity.refresh_tokens` | `password_hash`, `password_changed_at`; `revoked_at` | Button | - | - | - | I | - | Xác nhận | Gửi yêu cầu đổi hoặc đặt mật khẩu<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:126-133]; hệ quả ghi cột nêu trên [Nguồn: 02-bd/database/identity.md:20; 03-dd/logic/identity.md:94-97]<br>[EVT liên quan] EVT-19 |

[Nguồn: 09-layoutBase/Cài đặt.dc.html:125-131,138-144,155,163-167,215-260,263-272; 02-bd/database/identity.md:23,196; 02-bd/database/ai-review.md:105-107]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Nhóm Giao diện

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm Giao diện | | | | |
| | 1 | Tiêu đề nhóm | Có | - |
| | 2 | Chủ đề màu | Có | [Điều kiện kích hoạt] Luôn kích hoạt, kể cả khi ba nhóm còn lại đang tải hoặc đang lỗi — nhóm này không phụ thuộc máy chủ.<br>[Tự động đặt] Nút đang chọn được tô sáng ngay khi bấm; giá trị đọc lại từ bộ nhớ thiết bị ở mỗi lần vào màn. |
| | 3 | Ngôn ngữ giao diện | Có | [Điều kiện kích hoạt] Luôn kích hoạt, cùng lý do NO 2.<br>[Tự động đặt] Đổi giá trị thì toàn bộ nhãn trên màn đổi ngôn ngữ ngay, **không** tải lại trang và **không** đổi đường dẫn — đúng cơ chế đã chốt ở `DEC-2026-0825-frontend-base-architecture`. |

### Khu vực B — Nhóm Workspace

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm Workspace | | | | |
| | 1 | Tiêu đề nhóm | Có | - |
| | 2 | Ngôn ngữ mặc định | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ tại chỗ đúng kích thước cụm nút.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc nhóm đang tải hoặc đang gửi lưu.<br>[Tự động đặt] Chỉ liệt kê ngôn ngữ đang bật; ngôn ngữ đã lưu bị tắt thì hiển thị ngôn ngữ đầu tiên đang bật (Q4, chốt 2026-10-18). Phần lùi này **chỉ để hiển thị**: không đánh dấu thay đổi chưa lưu và không ghi gì cho tới khi người dùng tự chọn một ngôn ngữ (owner 2026/10/19, D35). |
| | 3 | Cỡ chữ editor | Có | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2. |
| | 4 | Tự lưu bản nháp | Có | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2. |
| | 5 | Phím tắt Vim | Có | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2. |

### Khu vực C — Nhóm Phỏng vấn giả lập

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm Phỏng vấn giả lập | | | | |
| | 1 | Tiêu đề nhóm | Có | - |
| | 2 | Mức người phỏng vấn | Điều kiện | [Điều kiện hiển thị] Ẩn khi nhóm đang ở trạng thái suy giảm (NO 6 đang hiển thị).<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc nhóm đang tải hoặc đang gửi lưu. |
| | 3 | Số lượt tối đa mỗi phiên | Điều kiện | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2. |
| | 4 | Cho phép gợi ý | Điều kiện | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2. |
| | 5 | Dòng phạm vi áp dụng | Điều kiện | [Điều kiện hiển thị] Ẩn khi nhóm đang ở trạng thái suy giảm; hiển thị trong mọi trường hợp còn lại. |
| | 6 | Dòng suy giảm khi AI hỏng | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi lời gọi `GetMyInterviewPreferences` thất bại hoặc quá hạn chờ. Không có nút "Thử lại" ở nhóm này: người dùng không sửa được sự cố của phân hệ AI, và nút thử lại chỉ khuyến khích gọi lặp vào một dịch vụ đang hỏng — cùng quy ước đã áp ở `USR0501` Khu vực F [Nguồn: 02-bd/screens/users/USR0501_my_progress.md:446]. |

### Khu vực D — Nhóm Thông báo

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm Thông báo | | | | |
| | 1 | Tiêu đề nhóm | Có | - |
| | 2 | Nhắc luyện tập | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ tại chỗ đúng kích thước công tắc.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc nhóm đang tải hoặc đang gửi lưu.<br>[Tự động đặt] Người dùng chưa có dòng `user_preferences` thì công tắc hiển thị theo giá trị mặc định ở Sheet 5, dòng được tạo ở lần lưu đầu tiên. |
| | 3 | Báo cáo hằng tuần | Có | [Điều kiện hiển thị] Như NO 2.<br>[Điều kiện kích hoạt] Như NO 2.<br>[Tự động đặt] Như NO 2. |

### Khu vực E — Khối trạng thái và lưu

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Khối trạng thái và lưu | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Câu trạng thái | Có | [Tự động đặt] Đổi sang biến thể "Bạn có thay đổi chưa lưu" ngay khi một tuỳ chọn của ba nhóm chờ Lưu bị đổi; quay lại biến thể mặc định khi lưu xong (kết quả lưu báo bằng toast, không có biến thể "vừa lưu xong") và khi vào màn lần sau. |
| | 3 | Lưu cài đặt | Điều kiện | [Điều kiện hiển thị] Luôn hiển thị.<br>[Điều kiện kích hoạt] Chỉ kích hoạt khi có ít nhất một thay đổi chưa lưu **và** không có lời gọi lưu nào đang chạy. Không có thay đổi thì nút ở trạng thái không kích hoạt, nhãn giữ "Lưu cài đặt".<br>[Tự động đặt] Trong lúc gửi, nhãn đổi sang trạng thái đang xử lý; lưu xong nhãn trở lại "Lưu cài đặt" và kết quả báo bằng toast. |

### Khu vực F — Khối xuất dữ liệu

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Khối xuất dữ liệu | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Xuất lượt nộp | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc chính lời gọi xuất đó đang chạy; **vẫn kích hoạt** khi ba nhóm tuỳ chọn đang tải hoặc đang lỗi, vì hai việc không liên quan nhau. |
| | 3 | Xuất hội thoại phỏng vấn | Có | [Điều kiện kích hoạt] Như NO 2. Người dùng chưa có phiên phỏng vấn nào thì nút vẫn kích hoạt và tệp trả về là một mảng rỗng — **không** ẩn nút, vì ẩn nút sẽ để lộ việc người dùng chưa từng phỏng vấn ngay trên màn cài đặt mà không giải thích gì. Khi lời gọi xuất lỗi (kể cả `ai-review` hỏng) thì hiện lỗi tại chỗ dưới các nút, nút **vẫn kích hoạt** (Q6, chốt 2026-10-18). |

### Khu vực G — Lối tắt hồ sơ

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Lối tắt hồ sơ | | | | |
| | 1 | Về trang cá nhân | Có | [Điều kiện kích hoạt] Luôn kích hoạt. Còn thay đổi chưa lưu thì mở popup xác nhận rời màn trước, không rời thẳng. |

### Khu vực H — Vùng nguy hiểm

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Vùng nguy hiểm | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Câu mô tả hệ quả | Có | - |
| | 3 | Xoá tài khoản | Có | [Điều kiện kích hoạt] Luôn kích hoạt, kể cả khi ba nhóm tuỳ chọn đang lỗi — người dùng phải xoá được tài khoản kể cả khi một phần màn hỏng. |

### Khu vực I — Popup xác nhận xoá tài khoản

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup xác nhận xoá tài khoản | | | | |
| | 1 | Tiêu đề popup | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi popup đang mở. |
| | 2 | Câu mô tả hệ quả | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 3 | Ô nhập xác nhận | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Kích hoạt khi popup mở và chưa gửi lời gọi huỷ kích hoạt.<br>[Tự động đặt] Ô rỗng mỗi lần mở popup, không nhớ giá trị lần trước. |
| | 4 | Huỷ | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Luôn kích hoạt khi popup mở, kể cả khi lời gọi đang chạy. |
| | 5 | Xác nhận xoá | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Chỉ kích hoạt khi giá trị ở NO 3 khớp email của chính người đăng nhập (sau `trim()` và hạ chữ thường ở cả hai vế); chỉ nút này bị khoá, nút Huỷ (NO 4) luôn dùng được; kiểm sơ bộ ở màn để mở khoá nút, kiểm quyết định vẫn ở máy chủ. |

### Khu vực J — Popup xác nhận rời màn

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup xác nhận rời màn | | | | |
| | 1 | Tiêu đề popup | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi có thay đổi chưa lưu **và** người dùng vừa kích hoạt một đường rời màn. |
| | 2 | Nội dung | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 3 | Ở lại | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Luôn kích hoạt khi popup mở. |
| | 4 | Thoát không lưu | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Luôn kích hoạt khi popup mở, trừ lúc đang lưu. |
| | 5 | Lưu và thoát | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Kích hoạt khi popup mở, trừ lúc đang lưu. |

### Khu vực K — Nhóm Bảo mật (V0.7)

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm Bảo mật | | | | |
| | 1 | Tiêu đề nhóm | Điều kiện | [Điều kiện hiển thị] Cả nhóm chỉ hiển thị sau khi hồ sơ đã tải xong; trong lúc tải và khi lỗi tải hồ sơ thì **không** dựng nhóm (khối lỗi có nút "Thử lại" thuộc Vùng nguy hiểm, Khu vực H) [Nguồn: 05-coding/frontend/src/views/users/settings/ui/settings-view.tsx:38-60]. |
| | 2 | Nhãn dòng mật khẩu | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 3 | Mô tả trạng thái mật khẩu | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Tự động đặt] Làm mới sau khi đổi / đặt mật khẩu thành công vì hồ sơ được tải lại (EVT-19). |
| | 4 | Nút mở popup mật khẩu | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Luôn kích hoạt khi nhóm hiển thị, kể cả khi ba nhóm tuỳ chọn đang lỗi hoặc còn thay đổi chưa lưu — hai việc độc lập nhau.<br>[Tự động đặt] Nhãn đổi giữa "Đổi mật khẩu" và "Đặt mật khẩu" theo `hasPassword`; tài khoản OAuth đặt mật khẩu xong thì nhãn chuyển thành "Đổi mật khẩu". |

### Khu vực L — Popup đổi / đặt mật khẩu (V0.7)

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup đổi / đặt mật khẩu | | | | |
| | 1 | Tiêu đề popup | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi popup đang mở. Nội dung theo biến thể. |
| | 2 | Mật khẩu hiện tại | Điều kiện | [Điều kiện hiển thị] Chỉ khi popup mở **và** tài khoản đã có mật khẩu; biến thể "Đặt mật khẩu" ẩn hẳn ô này.<br>[Tự động đặt] Rỗng mỗi lần mở popup. |
| | 3 | Mật khẩu mới | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Tự động đặt] Rỗng mỗi lần mở popup. Nút mắt của từng ô tự đặt lại về "che" mỗi lần mở. |
| | 4 | Thanh độ mạnh | Điều kiện | [Điều kiện hiển thị] Chỉ khi popup mở **và** ô mật khẩu mới không rỗng.<br>[Tự động đặt] Đổi mức và màu theo từng ký tự gõ. Không bao giờ chặn nút Xác nhận. |
| | 5 | Nhập lại mật khẩu mới | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Tự động đặt] Rỗng mỗi lần mở popup. |
| | 6 | Danh sách quy tắc | Điều kiện | [Điều kiện hiển thị] Như NO 1; hiển thị cả bốn dòng ngay khi mở, trạng thái ban đầu "chưa đạt".<br>[Tự động đặt] Mỗi dòng đổi sang "đạt" ngay khi điều kiện thoả. |
| | 7 | Huỷ | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang gửi yêu cầu [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:118-124]. |
| | 8 | Xác nhận | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] **Không bị khoá theo bốn quy tắc** — người dùng bấm được ngay, quy tắc chặn kiểm lúc bấm (Sheet 9 NO 11); chỉ khoá trong lúc đang gửi yêu cầu.<br>[Tự động đặt] Trong lúc gửi, nút ở trạng thái đang xử lý [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:126-132]. |

[Nguồn: 09-layoutBase/Cài đặt.dc.html:263-267; 01-rd/screens/users/USR0503_settings.md:94-95]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `MySettingsDto` | `workspace` | Object | - | - | Toàn bộ Khu vực B | Có | [Nguồn] Phản hồi của `GetMySettings`<br>[Chuyển đổi] Gói bốn tuỳ chọn Workspace thành một đối tượng con để `UpdateMySettings` nhận lại đúng hình dạng đó. |
| 2 | `MySettingsDto` | `notification` | Object | - | - | Toàn bộ Khu vực D | Có | [Nguồn] Phản hồi của `GetMySettings`. |
| 3 | `MyWorkspacePreferencesDto` | `defaultLanguage` | Enum | `identity.users` | `default_language` | Workspace "Ngôn ngữ mặc định" | Có | [Chuyển đổi] Giá trị truyền là mã enum `JAVA`/`CPP`/`PYTHON`; nhãn "Java 21"/"C++ 17"/"Python 3" tra ở nhãn tĩnh i18n, **không** trả chuỗi hiển thị từ máy chủ. Danh sách ngôn ngữ đang bật lấy từ `GetEnabledSubmissionLanguages` (7.3 NO 8); ngôn ngữ đã lưu không còn bật thì màn lùi về ngôn ngữ đầu tiên đang bật khi hiển thị (Q4). |
| 4 | `MyWorkspacePreferencesDto` | `editorFontSize` | Number | - | - | Workspace "Cỡ chữ editor" | Có | [Nguồn] **Chưa có cột lưu**, xem Câu hỏi mở Q1. |
| 5 | `MyWorkspacePreferencesDto` | `autosaveDraftEnabled` | Boolean | - | - | Workspace "Tự lưu bản nháp" | Có | [Nguồn] **Chưa có cột lưu**, xem Câu hỏi mở Q1. |
| 6 | `MyWorkspacePreferencesDto` | `vimModeEnabled` | Boolean | - | - | Workspace "Phím tắt Vim" | Có | [Nguồn] **Chưa có cột lưu**, xem Câu hỏi mở Q1. |
| 7 | `MyNotificationPreferencesDto` | `streakReminderEnabled` | Boolean | `identity.user_preferences` | `streak_reminder_enabled` | Thông báo "Nhắc luyện tập" | Có | [Nguồn] Chưa có dòng thì máy chủ trả giá trị mặc định, không trả `null`. |
| 8 | `MyNotificationPreferencesDto` | `weeklyReportEnabled` | Boolean | `identity.user_preferences` | `weekly_report_enabled` | Thông báo "Báo cáo hằng tuần" | Có | [Nguồn] Như NO 7. |
| 9 | `MyInterviewPreferencesDto` | `interviewerLevel` | Enum | `identity.user_preferences` | `preferred_interview_level` | Phỏng vấn "Mức người phỏng vấn" | Có | [Nguồn] Phản hồi của `GetMySettings` (`identity`). Enum `INTERN`/`JUNIOR`/`MIDDLE`/`SENIOR` (`DEC-2026-0922-users-and-admin-conflict-resolutions`). |
| 10 | `MyInterviewPreferencesDto` | `maxTurns` | Number | `identity.user_preferences` | `preferred_max_turns` | Phỏng vấn "Số lượt tối đa mỗi phiên" | Có | [Nguồn] Như NO 9. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** có cột (`8`/`12`/`16`, mặc định `12`), đọc/ghi qua `GetMySettings`/`UpdateMySettings` (`03-dd/api/identity.md` #14, #15). |
| 11 | `MyInterviewPreferencesDto` | `hintAllowed` | Boolean | `identity.user_preferences` | `preferred_hint_allowed` | Phỏng vấn "Cho phép gợi ý" | Có | [Nguồn] Như NO 9. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** có cột (mặc định bật), đọc/ghi qua `identity`. |
| 12 | `MyDataExportDto` | `format`, `fileName`, `content` | Enum, String, Binary | `judge.submissions`, `ai.interview_turns` | - | Xuất dữ liệu "Xuất lượt nộp", "Xuất hội thoại phỏng vấn" | Không | [Nguồn] Phản hồi của `ExportMySubmissions` / `ExportMyInterviewTranscripts`<br>[Đích] Trình duyệt tải tệp về, **không** hiển thị nội dung trên màn. Tệp trả **đồng bộ**, có giới hạn tần suất (Q5, chốt 2026-10-18). |
| 13 | `MyProfileDto` | `hasPassword` | Boolean | `identity.users` | `password_hash` (chỉ xét `IS NULL`) | Bảo mật "Nút mở popup mật khẩu" và biến thể popup | Không | [Nguồn] Phản hồi của `GetMyProfile`, **DTO dùng lại của `USR0502`** — không định nghĩa lại ở đây [Nguồn: 05-coding/frontend/src/entities/user/model/types.ts:38]<br>[Chuyển đổi] Máy chủ không trả hash, chỉ trả cờ. |
| 14 | `MyProfileDto` | `passwordChangedAt` | Date | `identity.users` | `password_changed_at` | Bảo mật "Mô tả trạng thái mật khẩu" | Có | [Nguồn] Như NO 13; `null` nghĩa là chưa từng đổi [Nguồn: 02-bd/database/identity.md:20]. |
| 15 | `MyProfileDto` | `linkedProviders` | List | `identity.oauth_identities` | `provider` | Bảo mật "Mô tả trạng thái mật khẩu" | Có | [Nguồn] Như NO 13; chỉ dùng phần tử đầu để nêu tên nhà cung cấp khi chưa có mật khẩu. |
| 16 | `ChangeMyPasswordInput` | `currentPassword`, `newPassword` | String | `identity.users` | `password_hash` | Popup mật khẩu NO 2, 3 | Không | [Đích] Tham số của `ChangeMyPassword`, **DTO dùng lại của `USR0502`**<br>[Chuyển đổi] `currentPassword` để trống ở biến thể "Đặt mật khẩu"; băm BCrypt phía máy chủ. Ô "Nhập lại mật khẩu mới" **không** nằm trong DTO, chỉ kiểm ở màn. |

Nhóm Giao diện **không có trường DTO nào**: hai tuỳ chọn của nó nằm ở bộ nhớ thiết bị và cookie, không đi
qua lời gọi máy chủ nào của màn này.

### 7.2 Truy cập bảng dữ liệu (8)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Người dùng | `identity.users` | `UserRepository` | R, U | Đọc và ghi ngôn ngữ nộp bài mặc định; đổi trạng thái khi tự xoá tài khoản | `GetMySettings`: R<br>`UpdateMySettings`: R, U (`default_language`)<br>`DeactivateMyAccount`: R, U (`status`, `deactivated_at`, `deactivation_source = SELF`)<br>`GetMyProfile`: R (`password_hash IS NULL`, `password_changed_at`)<br>`ChangeMyPassword`: R, U (`password_hash`, `password_changed_at`) |
| 2 | Tuỳ chọn thông báo | `identity.user_preferences` | `UserPreferenceRepository` `[Suy luận]` (tên do BD đề xuất; một bảng cho mọi tuỳ chọn cá nhân của F1-21) | C, R, U | Đọc và ghi hai công tắc email định kỳ, `preferred_interview_level`, `preferred_max_turns` và `preferred_hint_allowed` (ID-23, vòng 4) (cùng một dòng) | `GetMySettings`: R<br>`UpdateMySettings`: C (lần lưu đầu tiên, dòng chưa tồn tại), U |
| 3 | Bài nộp | `judge.submissions` | Không truy cập trực tiếp — qua cổng ra | R | Nguồn dữ liệu của tệp CSV xuất lượt nộp | `ExportMySubmissions`: R. `identity` **không** đọc chéo schema `judge` |
| 4 | Phiên phỏng vấn | `ai.interview_sessions` | Không truy cập trực tiếp — qua cổng ra | R | Siêu dữ liệu từng phiên trong tệp JSON xuất hội thoại | `ExportMyInterviewTranscripts`: R |
| 5 | Lượt hội thoại | `ai.interview_turns` | Không truy cập trực tiếp — qua cổng ra | R | Nội dung từng lượt hội thoại trong tệp JSON | `ExportMyInterviewTranscripts`: R |
| 6 | Cấu hình ngôn ngữ | `judge.language_configs` | Không truy cập trực tiếp — qua cổng ra | R | Đọc cột `enabled` để dựng danh sách ngôn ngữ nộp bài | `GetEnabledSubmissionLanguages`: R |
| 7 | Phiên làm mới | `identity.refresh_tokens` | `RefreshTokenRepository` | U | Thu hồi các phiên khác khi đổi mật khẩu | `ChangeMyPassword`: U (`revoked_at`), giữ phiên đang thao tác. Bảng thuộc `identity` [Nguồn: 02-bd/database/identity.md:89-100] |
| 8 | Liên kết OAuth | `identity.oauth_identities` | `OauthIdentityRepository` | R | Lấy tên nhà cung cấp cho dòng mô tả mật khẩu | `GetMyProfile`: R (`provider`) |

Màn **không xoá dòng nào**: "Xoá tài khoản" là thao tác `U` trên `identity.users`, không phải `D`
[Nguồn: 02-bd/database/identity.md:24,28]. Bốn bảng số 3 tới 6 thuộc schema của module khác, `identity`
đọc qua cổng ra chứ không truy vấn chéo schema — cùng nguyên tắc đã áp ở `USR0501`
[Nguồn: 02-bd/screens/users/USR0501_my_progress.md:489-491].

Ba tuỳ chọn của nhóm Phỏng vấn giả lập **không có dòng nào trong bảng này** vì chưa xác định được bảng
lưu — xem Câu hỏi mở Q1.

`[Suy luận]` — tên repository do BD này đề xuất (hai repository số 7 và 8 là tên đề xuất mới, chưa kiểm
`USR0502`), DD module chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/identity.md` và
> `03-dd/api/ai-review.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `GetMySettings` | Tải tuỳ chọn Workspace và tuỳ chọn Thông báo của người đăng nhập | `identity` |
| 2 | `UpdateMySettings` | Ghi tuỳ chọn Workspace và tuỳ chọn Thông báo trong một lần | `identity` |
| 3 | `GetMyInterviewPreferences` | ~~Tải tham số mặc định của phiên phỏng vấn tự luyện~~ **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** lỗi thời: giá trị nằm trong `user_preferences` nên do `GetMySettings` (`identity`) trả; đợi chủ dự án duyệt trước khi gỡ hẳn dòng và luồng suy giảm | ~~`ai-review`~~ `identity` |
| 4 | `UpdateMyInterviewPreferences` | ~~Ghi tham số mặc định của phiên phỏng vấn tự luyện~~ **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** lỗi thời: do `UpdateMySettings` (`identity`) ghi cùng giao dịch (`03-dd/logic/identity.md` BR-07) | ~~`ai-review`~~ `identity` |
| 5 | `ExportMySubmissions` | Xuất toàn bộ lượt nộp của người đăng nhập ra CSV | `identity` |
| 6 | `ExportMyInterviewTranscripts` | Xuất toàn bộ hội thoại phỏng vấn của người đăng nhập ra JSON | `identity` |
| 7 | `DeactivateMyAccount` | Chuyển tài khoản sang trạng thái ngừng hoạt động và chấm dứt phiên đăng nhập | `identity` |
| 8 | `GetEnabledSubmissionLanguages` | Trả danh sách ngôn ngữ nộp bài đang bật (`enabled = true`) để dựng nút "Ngôn ngữ mặc định" (Q4). `[Suy luận]` tên do BD này đề xuất, nếu `problem_detail` đã có endpoint cùng nghiệp vụ thì dùng lại, DD chốt | `judge-orchestration` |
| 9 | `GetMyProfile` | Tải hồ sơ để dựng nhóm Bảo mật và Vùng nguy hiểm (V0.7). **Dùng lại endpoint của `USR0502`** [Nguồn: 03-dd/api/identity.md:40], không định nghĩa lại | `identity` |
| 10 | `ChangeMyPassword` | Đổi mật khẩu hoặc đặt mật khẩu lần đầu (V0.7). **Chuyển chỗ gọi** từ `USR0502` sang màn này; hợp đồng vẫn một nơi duy nhất `03-dd/api/identity.md` [Nguồn: 03-dd/api/identity.md:44] | `identity` |

Ghi chú ranh giới:

- **Bảy endpoint đầu đều là mới**, chưa màn nào khác dùng; endpoint 8 là lời gọi đọc qua cổng ra của `judge-orchestration`; endpoint 9 và 10 là endpoint có sẵn của `identity` (V0.7), `GetMyProfile` còn do `USR0502` gọi, `ChangeMyPassword` chỉ còn màn này gọi. Không màn nào khác của khu Người học đặt tên cho
  cùng nghiệp vụ này.
- **Tiền tố `My` nghĩa là phạm vi khoá cứng theo người dùng đăng nhập**, không nhận `user_id` làm tham số
  — kế thừa nguyên quy ước chốt ở `USR0101` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md:530].
  Với bốn endpoint ghi và hai endpoint xuất, đây không phải quy ước đặt tên mà là ràng buộc bảo mật:
  nhận `user_id` từ phía gọi sẽ mở ngay một lỗ IDOR cho phép sửa tuỳ chọn hoặc tải dữ liệu của người khác
  [Nguồn: 02-bd/security/identity.md:53-56].
- **Không dùng lại tên nghiệp vụ của màn quản trị.** `GetLanguageConfigs` / `UpdateLanguageConfigs` của
  `ADM0501` [Nguồn: 02-bd/screens/admin/ADM0501_language_config.md:387-388] là cấu hình **toàn hệ thống**
  cho ngôn ngữ lập trình, thuộc `judge-orchestration`, actor A3. Màn này đặt tuỳ chọn **cá nhân**, thuộc
  `identity`, actor A1. Hai việc chỉ trùng nhau ở chữ "ngôn ngữ" trong tiếng Việt.
- **Endpoint 3 và 4 tách riêng khỏi 1 và 2 một cách có chủ đích.** Đó là điều kiện kỹ thuật để phân hệ AI
  hỏng mà ba nhóm còn lại vẫn xem và lưu được — nếu gộp vào một lời gọi thì `ai-review` hỏng sẽ kéo sập cả
  màn cài đặt, vi phạm ràng buộc suy giảm êm. Hệ quả: một lần bấm "Lưu cài đặt" gọi **hai** endpoint ghi ở
  **hai** BC, nên phải định nghĩa rõ hành vi khi một trong hai thất bại (Q11 đã chốt 2026-10-18: mỗi nhóm báo kết quả riêng).
- **Endpoint 6 do `identity` sở hữu dù dữ liệu nằm ở `ai-review`**, vì F1-22 đặt việc xuất dữ liệu cá nhân
  vào `identity` [Nguồn: 01-rd/req/identity.md:235-236] và ràng buộc bảo mật cũng viết ở
  `02-bd/security/identity.md:53-56`. `identity` đọc qua cổng ra, không truy vấn chéo schema (Q6 đã chốt
  2026-10-18).
- **Không có endpoint nào cho nhóm Giao diện.** Đổi chủ đề màu và ngôn ngữ giao diện không gọi máy chủ.

[Nguồn: 02-bd/database/identity.md:8-43,196; 02-bd/database/judge-orchestration.md:8-43; 02-bd/database/ai-review.md:95-118,120-140; 02-bd/security/identity.md:53-63]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Thiết lập

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì dựng nhóm Giao diện từ bộ nhớ thiết bị và tải ba nhóm còn lại từ máy chủ. | Không | Có | `GetMySettings`, `GetMyInterviewPreferences` | [Các bước]<br>1. Kiểm tra người dùng đã đăng nhập.<br>2. Dựng ngay nhóm Giao diện từ `next-themes` và cookie `NEXT_LOCALE`, không chờ máy chủ.<br>3. Hiển thị khung chờ cho ba nhóm còn lại, gọi song song; lời gọi `ai-review` tách riêng, không chặn lời gọi kia. Bản dựng còn gọi thêm `GetMyProfile` để dựng nhóm Bảo mật (V0.7) và Vùng nguy hiểm [Nguồn: 05-coding/frontend/src/views/users/settings/ui/settings-view.tsx:30,38-60]; lỗi tải hồ sơ không chặn các nhóm khác.<br>[Khi thành công] Bốn nhóm hiển thị đầy đủ, nút "Lưu cài đặt" ở trạng thái không kích hoạt vì chưa có thay đổi nào.<br>[Khi lỗi] `GetMySettings` thất bại thì hai nhóm Workspace và Thông báo chuyển sang trạng thái lỗi kèm nút "Thử lại" (EVT-15), các khối khác không đổi. `GetMyInterviewPreferences` thất bại thì nhóm Phỏng vấn giả lập chuyển sang dòng suy giảm, **không** có nút "Thử lại". |
| 2 | Nút | Đổi Chủ đề màu | Bấm "Sáng" hoặc "Tối". | Không | Không | - | [Các bước]<br>1. Ghi lựa chọn vào bộ nhớ thiết bị qua `next-themes`.<br>2. Áp bộ token màu mới cho toàn bộ ứng dụng ngay lập tức.<br>[Khi thành công] Toàn màn đổi màu ngay, **không** gọi máy chủ và **không** đánh dấu màn là có thay đổi chưa lưu — đây là hai chế độ lưu khác nhau, không phải một [Nguồn: 01-rd/screens/users/USR0503_settings.md:94].<br>[Khi lỗi] Không có đường lỗi: thao tác hoàn toàn cục bộ. |
| 3 | Nút | Đổi Ngôn ngữ giao diện | Bấm "Tiếng Việt" hoặc "English". | Không | Không | - | [Các bước]<br>1. Ghi locale vào cookie `NEXT_LOCALE`.<br>2. Áp bộ nhãn tĩnh i18n mới cho toàn bộ ứng dụng ngay.<br>[Khi thành công] Mọi nhãn đổi ngôn ngữ, **đường dẫn không đổi** (`DEC-2026-0825-frontend-base-architecture`), không tải lại trang, không đánh dấu thay đổi chưa lưu. Giá trị đang chọn ở ba nhóm kia giữ nguyên, kể cả khi chúng đang là thay đổi chưa lưu.<br>[Khi lỗi] Không có đường lỗi. |
| 4 | Nút | Đổi một tuỳ chọn nhóm Workspace | Bấm một nút phân đoạn hoặc gạt một công tắc trong nhóm Workspace. | Không | Không | - | [Các bước]<br>1. Ghi giá trị mới vào trạng thái màn.<br>2. Đánh dấu màn là **có thay đổi chưa lưu**, đổi câu trạng thái và kích hoạt nút "Lưu cài đặt".<br>[Khi thành công] Giá trị mới hiển thị ngay nhưng **chưa có hiệu lực thật** cho tới khi bấm Lưu [Nguồn: 01-rd/screens/users/USR0503_settings.md:95]. |
| 5 | Nút | Đổi một tuỳ chọn nhóm Phỏng vấn giả lập | Bấm một nút phân đoạn hoặc gạt công tắc trong nhóm Phỏng vấn giả lập. | Không | Không | - | [Các bước]<br>1. Ghi giá trị mới vào trạng thái màn.<br>2. Đánh dấu màn là có thay đổi chưa lưu.<br>[Khi thành công] Như EVT-4. Nhóm đang ở trạng thái suy giảm thì sự kiện này không xảy ra được vì các điều khiển đã bị ẩn. |
| 6 | Nút | Bật hoặc tắt một công tắc Thông báo | Gạt "Nhắc luyện tập" hoặc "Báo cáo hằng tuần". | Không | Không | - | [Các bước]<br>1. Ghi giá trị mới vào trạng thái màn.<br>2. Đánh dấu màn là có thay đổi chưa lưu.<br>[Khi thành công] Như EVT-4. Email định kỳ chỉ thực sự bật hoặc tắt sau khi bấm Lưu. |
| 7 | Nút | Lưu cài đặt | Bấm "Lưu cài đặt". | Không | Có | `UpdateMySettings`, `UpdateMyInterviewPreferences` | [Các bước]<br>1. Kiểm giá trị từng tuỳ chọn theo Sheet 9.<br>2. Chuyển nút sang trạng thái đang xử lý, khoá các điều khiển của ba nhóm chờ Lưu.<br>3. Gọi `UpdateMySettings`; chỉ gọi `UpdateMyInterviewPreferences` khi nhóm Phỏng vấn giả lập có thay đổi và không ở trạng thái suy giảm.<br>[Khi thành công] Xoá cờ thay đổi chưa lưu, hiện toast thành công "Đã lưu cài đặt." khi cả hai nhóm cùng lưu được [Nguồn: 05-coding/frontend/src/views/users/settings/ui/preferences-form.tsx:114-117]. Không rời màn.<br>[Khi lỗi] Giữ nguyên toàn bộ giá trị người dùng vừa đặt, **không** hoàn về giá trị cũ; cờ thay đổi chưa lưu vẫn bật để người dùng bấm lại. Mỗi nhóm báo kết quả riêng (Q11, chốt 2026-10-18): Workspace và Thông báo (`identity`) lưu được thì toast thành công, không thì toast lỗi; Phỏng vấn giả lập (`ai-review`) lưu được thì toast thành công, không thì toast cảnh báo; nhóm kia vẫn được coi là đã lưu theo kết quả của riêng nó [Nguồn: 05-coding/frontend/src/views/users/settings/ui/preferences-form.tsx:109-125].<br>[Thông báo hoàn tất] Toast "Đã lưu cài đặt.", không dùng hộp thoại. |
| 8 | Nút | Xuất lượt nộp | Bấm "Xuất lượt nộp". | Không | Có | `ExportMySubmissions` | [Các bước]<br>1. Khoá nút trong lúc lời gọi đang chạy.<br>2. Nhận tệp CSV và giao cho trình duyệt tải về.<br>[Khi thành công] Tệp được tải về và hiện toast thành công [Nguồn: 05-coding/frontend/src/views/users/settings/ui/data-export-card.tsx:18-22], màn không đổi trạng thái nào khác, thay đổi chưa lưu giữ nguyên. Máy chủ trả tệp đồng bộ và giới hạn tần suất (Q5, chốt 2026-10-18).<br>[Khi lỗi] Hiển thị lỗi **tại chỗ** dưới hai nút, nút vẫn kích hoạt [Nguồn: 05-coding/frontend/src/views/users/settings/ui/data-export-card.tsx:39-43], không rời màn và không ảnh hưởng ba nhóm tuỳ chọn. |
| 9 | Nút | Xuất hội thoại phỏng vấn | Bấm "Xuất hội thoại phỏng vấn". | Không | Có | `ExportMyInterviewTranscripts` | [Các bước]<br>1. Khoá nút trong lúc lời gọi đang chạy.<br>2. Nhận tệp JSON và giao cho trình duyệt tải về.<br>[Khi thành công] Như EVT-8. Chưa có phiên nào thì tệp chứa một mảng rỗng, vẫn coi là thành công.<br>[Khi lỗi] Như EVT-8: lỗi bằng toast, nút giữ kích hoạt (Q6, chốt 2026-10-18). `ai-review` hỏng thì lời gọi này lỗi nhưng "Xuất lượt nộp" vẫn dùng được. |
| 10 | Liên kết | Về trang cá nhân | Bấm "Về trang cá nhân". | Có | Không | - | [Các bước]<br>1. Còn thay đổi chưa lưu thì mở popup xác nhận rời màn (EVT-14) và dừng ở đây.<br>2. Không còn thay đổi thì điều hướng sang màn `profile`.<br>[Khi thành công] Mở `profile`. |
| 11 | Nút | Mở popup xác nhận xoá tài khoản | Bấm "Xoá tài khoản" ở Vùng nguy hiểm. | Không | Không | - | [Các bước]<br>1. Mở popup, làm rỗng ô nhập xác nhận.<br>[Khi thành công] Popup hiện, nền bị chặn thao tác. **Không** gọi máy chủ ở bước này. |
| 12 | Nút | Xác nhận xoá tài khoản | Bấm "Xoá tài khoản của tôi" trong popup. | Có | Có | `DeactivateMyAccount` | [Các bước]<br>1. Máy chủ kiểm chuỗi xác nhận khớp email của chính người đăng nhập.<br>2. Đổi `status` sang `DEACTIVATED`, ghi `deactivated_at` [Nguồn: 02-bd/security/identity.md:60-62].<br>3. Chấm dứt phiên đăng nhập và điều hướng sang màn `auth`.<br>[Khi xác nhận] Mọi thay đổi chưa lưu bị bỏ **không hỏi lại** — người dùng vừa xác nhận một hành động nặng hơn hẳn.<br>[Khi thành công] Về màn `auth`; đăng nhập lại bằng tài khoản đó bị từ chối.<br>[Khi lỗi] Chuỗi xác nhận sai hoặc lời gọi hỏng thì popup vẫn mở, hiện toast lỗi, tài khoản không đổi trạng thái. Gửi yêu cầu thành công thì hiện toast "Đã gửi yêu cầu xoá tài khoản" [Nguồn: 05-coding/frontend/src/features/deactivate-my-account/ui/delete-account-dialog.tsx:15-16]. |
| 13 | Nút | Huỷ xoá tài khoản | Bấm "Huỷ" trong popup xoá tài khoản, hoặc bấm ra ngoài popup. | Không | Không | - | [Các bước]<br>1. Đóng popup, xoá nội dung ô nhập xác nhận.<br>[Khi huỷ] Màn giữ nguyên mọi trạng thái, kể cả thay đổi chưa lưu. Không gọi máy chủ. |
| 14 | Màn hình | Rời màn khi còn thay đổi chưa lưu | Kích hoạt bất kỳ đường rời màn nào trong lúc còn thay đổi chưa lưu. | Có | Không | - | [Các bước]<br>1. Chặn điều hướng, mở popup xác nhận rời màn.<br>2. Bấm "Ở lại" thì đóng popup và giữ nguyên mọi thứ.<br>3. Bấm "Thoát không lưu" thì bỏ toàn bộ thay đổi chưa lưu và đi tiếp tới đích đang chờ.<br>4. Bấm "Lưu và thoát" thì chạy EVT-7; lưu thành công thì đi tiếp tới đích đang chờ, lưu lỗi thì ở lại màn.<br>[Khi xác nhận] Chỉ nhóm Giao diện không bị ảnh hưởng, vì nó đã được áp và ghi từ trước.<br>[Khi huỷ] Ở lại màn. |
| 15 | Nút | Thử lại một nhóm | Bấm "Thử lại" trong nhóm Workspace hoặc Thông báo đang ở trạng thái lỗi. | Không | Có | `GetMySettings` | [Các bước]<br>1. Chuyển hai nhóm đó về trạng thái đang tải.<br>2. Gọi lại `GetMySettings`, không gọi lại cả màn.<br>[Khi thành công] Hai nhóm hiển thị dữ liệu, các khối khác không đổi trạng thái.<br>[Khi lỗi] Giữ nguyên trạng thái lỗi, nút "Thử lại" vẫn kích hoạt. |
| 16 | Nút | Mở popup đổi / đặt mật khẩu | Bấm nút ở dòng "Mật khẩu" của nhóm Bảo mật. | Không | Không | - | [Các bước]<br>1. Chọn biến thể popup theo `hasPassword` của hồ sơ đã tải.<br>2. Mở popup Khu vực L với ba ô rỗng, nút mắt ở trạng thái che [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:39,45-50].<br>[Khi thành công] Popup hiện, nền bị chặn thao tác. Ba nhóm tuỳ chọn phía dưới **không** bị ảnh hưởng, thay đổi chưa lưu vẫn còn nguyên. **Không** gọi máy chủ ở bước này. |
| 17 | Màn hình | Gõ vào ô mật khẩu mới hoặc ô nhập lại | Gõ, xoá hoặc dán ký tự vào "Mật khẩu mới" hoặc "Nhập lại mật khẩu mới". | Không | Không | - | [Các bước]<br>1. Tính lại bốn quy tắc: đủ 8 ký tự, có chữ cái, có chữ số, hai mật khẩu khớp (chỉ đạt khi ô nhập lại không rỗng).<br>2. Tính lại điểm độ mạnh: số quy tắc đạt trong ba quy tắc đầu, cộng 1 nếu từ 12 ký tự trở lên, tối đa 4; ô mật khẩu mới rỗng thì ẩn thanh độ mạnh.<br>[Khi thành công] Danh sách quy tắc và thanh độ mạnh đổi ngay. Đây là **gợi ý hiển thị**: thanh độ mạnh không bao giờ chặn gửi, các quy tắc cũng không khoá nút Xác nhận — chặn thật xảy ra lúc bấm Xác nhận (EVT-19) [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:85-99]. |
| 18 | Nút | Hiện / ẩn mật khẩu | Bấm nút mắt ở mép phải một ô mật khẩu. | Không | Không | - | [Các bước]<br>1. Đổi kiểu ô đó giữa che ký tự và hiện ký tự; nhãn trợ năng đổi giữa "Hiện mật khẩu" và "Ẩn mật khẩu".<br>[Khi thành công] Chỉ ô được bấm đổi; ba ô độc lập nhau [Nguồn: 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:36-57]. |
| 19 | Nút | Xác nhận đổi / đặt mật khẩu | Bấm "Xác nhận" trong popup mật khẩu. | Không | Có | `ChangeMyPassword` | [Các bước]<br>1. Chạy kiểm nhập liệu Sheet 9 NO 11 ở màn: thiếu mật khẩu hiện tại (nếu có ô đó), mật khẩu mới ngoài 8-128 ký tự hoặc thiếu chữ hoặc thiếu số, hai mật khẩu không khớp. Có lỗi thì **không** gọi máy chủ, đổi viền ô sai và hiện toast với lỗi đầu tiên.<br>2. Khoá nút trong lúc gửi; gửi mật khẩu hiện tại (nếu có) và mật khẩu mới. Máy chủ đặt `password_changed_at` và thu hồi mọi refresh token của phiên khác, giữ phiên đang thao tác (Q9 của `USR0502`, chốt 2026/10/18, chuyển về đây).<br>3. Thành công: xoá sạch các ô, làm mới hồ sơ (dòng mô tả "đổi lần cuối" và nhãn nút cập nhật), đóng popup [Nguồn: 05-coding/frontend/src/features/change-my-password/model/use-change-password.ts:20-57].<br>[Khi thành công] Đóng popup và hiện **một** toast "Đã đổi mật khẩu. Các thiết bị khác cần đăng nhập lại." [Nguồn: 05-coding/frontend/messages/vi.json:2646; 05-coding/frontend/src/features/change-my-password/ui/change-password-dialog.tsx:101-105]. Tài khoản chỉ OAuth chuyển sang có mật khẩu, nhãn nút ở nhóm Bảo mật đổi từ "Đặt mật khẩu" sang "Đổi mật khẩu". Không rời màn, không đụng thay đổi chưa lưu của ba nhóm.<br>[Khi lỗi] Máy chủ từ chối (ví dụ sai mật khẩu hiện tại, mã `errors.currentPasswordInvalid` "Mật khẩu hiện tại không đúng") thì popup **không đóng**, viền ô mật khẩu hiện tại chuyển đỏ và hiện toast lỗi. Hiện trạng bản dựng: **mọi** lỗi trả từ máy chủ đều gán vào ô mật khẩu hiện tại và **không** xoá ô đó [Nguồn: 05-coding/frontend/src/features/change-my-password/model/use-change-password.ts:46-49]; mô tả cũ ở `USR0502` nói xoá ô này, bản dựng không làm (xem Q13). |
| 20 | Nút | Huỷ popup mật khẩu | Bấm "Huỷ" hoặc đóng popup mật khẩu. | Không | Không | - | [Các bước]<br>1. Đóng popup; popup gỡ khỏi cây giao diện nên ba ô mật khẩu mất, lần mở sau rỗng [Nguồn: 05-coding/frontend/src/views/users/settings/ui/security-card.tsx:45-50].<br>[Khi huỷ] Không hỏi xác nhận dù đã nhập dở — nội dung nhạy cảm, không giữ lại. Nút "Huỷ" không kích hoạt trong lúc đang gửi. |

[Nguồn: 09-layoutBase/Cài đặt.dc.html:131,155,263-267; 01-rd/screens/users/USR0503_settings.md:94-96; 02-bd/security/identity.md:60-63]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/settings.md`, không lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Phải đăng nhập | [Nội dung kiểm] Người dùng chưa đăng nhập thì không vào được màn, chuyển về màn `auth`.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ, không chỉ ẩn giao diện. | Lỗi | Chưa có mã thông báo | Nội dung "Vui lòng đăng nhập để mở phần thiết lập." | EVT-1 | 1 |
| 2 | Kiểm quyền | Phạm vi dữ liệu cá nhân tuyệt đối | [Nội dung kiểm] Cả bảy endpoint của màn lấy `user_id` từ token của phiên đăng nhập, **không** endpoint nào nhận `user_id` từ phía gọi. Vai trò `INSTRUCTOR`/`ADMIN` **không** có đường đọc hay sửa tuỳ chọn của người khác qua màn này, kể cả khi ma trận phân quyền F1-10 cấp toàn quyền.<br>[Nơi thực thi] Máy chủ, ở tầng dịch vụ. | Lỗi | Mã lỗi trong phản hồi | Với bốn endpoint ghi và hai endpoint xuất, đây là ràng buộc cứng chứ không phải quy ước đặt tên: nhận `user_id` từ client sẽ mở lỗ IDOR cho phép sửa tuỳ chọn và tải dữ liệu của người khác [Nguồn: 02-bd/security/identity.md:53-56]. Quy ước tiền tố `My` của khu Người học là hình thức kỹ thuật của ràng buộc này [Nguồn: 02-bd/screens/users/USR0101_problem_list.md:530]. | EVT-1, EVT-7, EVT-8, EVT-9, EVT-12, EVT-19 | 2 |
| 3 | Kiểm nhập liệu | Giá trị enum hợp lệ | [Nội dung kiểm] Mỗi tuỳ chọn chỉ nhận đúng tập giá trị đã khai ở Sheet 5: chủ đề màu `light`/`dark`; ngôn ngữ giao diện `vi`/`en`; ngôn ngữ nộp bài `JAVA`/`CPP`/`PYTHON`; cỡ chữ 13/14/16; mức người phỏng vấn `JUNIOR`/`MIDDLE`/`SENIOR`; số lượt 8/12/16. Giá trị ngoài tập thì máy chủ từ chối và màn giữ nguyên giá trị cũ.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Tuỳ chọn vi phạm. | Lỗi | Mã lỗi trong phản hồi | Nội dung "Giá trị không hợp lệ." Ba giá trị cỡ chữ và ba giá trị số lượt lấy từ prototype [Nguồn: 09-layoutBase/Cài đặt.dc.html:234,246]. | EVT-7 | 1 |
| 4 | Kiểm nhập liệu | Không nhầm hai loại ngôn ngữ | [Nội dung kiểm] Trường ngôn ngữ nộp bài mặc định chỉ nhận mã ngôn ngữ lập trình, **không** nhận mã locale (`vi`/`en`); ngược lại locale giao diện không được ghi vào `users.default_language`.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Workspace "Ngôn ngữ mặc định". | Lỗi | Mã lỗi trong phản hồi | Hai khái niệm trùng chữ "ngôn ngữ" trong tiếng Việt và nằm cách nhau đúng một nhóm trên màn, nên đây là lỗi rất dễ xảy ra khi dựng mã. Cột đích là `users.default_language`, mô tả rõ là ngôn ngữ nộp bài [Nguồn: 02-bd/database/identity.md:23]. | EVT-7 | 2 |
| 5 | Kiểm nghiệp vụ | Suy giảm êm khi phân hệ AI hỏng | [Nội dung kiểm] `GetMyInterviewPreferences` và `UpdateMyInterviewPreferences` lỗi hoặc quá hạn chờ thì **không** được chặn khởi tạo màn, không được chặn việc lưu ba nhóm còn lại, và không được chặn "Xuất lượt nộp".<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Nhóm Phỏng vấn giả lập. | Cảnh báo | Chưa có mã thông báo | Nội dung "Chưa lấy được tuỳ chọn phỏng vấn giả lập. Các nhóm còn lại vẫn xem và lưu được bình thường." Đây là **điều kiện hiển thị** của nhóm, không phải một thông báo lỗi bật lên — cùng cách xử lý đã chốt ở `USR0501` [Nguồn: 02-bd/screens/users/USR0501_my_progress.md:446]. Ràng buộc bắt buộc của dự án, không phải lựa chọn thiết kế. | EVT-1, EVT-7 | 1 |
| 6 | Kiểm nghiệp vụ | Xoá tài khoản phải qua xác nhận | [Nội dung kiểm] `DeactivateMyAccount` chỉ thực thi khi chuỗi xác nhận khớp email của chính người đăng nhập; kiểm ở máy chủ, không chỉ ở màn.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Popup xác nhận xoá tài khoản; toast. | Lỗi | Chưa có mã thông báo | Nội dung "Chuỗi xác nhận không khớp email của bạn." Kiểm ở màn chỉ để mở khoá nút; máy chủ vẫn phải kiểm lại vì lời gọi có thể đến thẳng từ bên ngoài giao diện. | EVT-12 | 1 |
| 7 | Kiểm nghiệp vụ | Xoá tài khoản là khoá mềm, không xoá cứng | [Nội dung kiểm] `DeactivateMyAccount` chỉ đổi `status`, ghi `deactivated_at` và đặt `deactivation_source = SELF`; **không** xoá dòng `users`, **không** cascade xoá lượt nộp, bài đã lưu hay phiên phỏng vấn.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Vùng nguy hiểm. | Lỗi | Mã lỗi trong phản hồi | Ràng buộc cứng của F1-16 [Nguồn: 01-rd/req/identity.md:197-202; 02-bd/database/identity.md:24,28]. Câu chữ trên UI cũng phải nói đúng việc này — câu hiện tại của prototype nói sai [Nguồn: 09-layoutBase/Cài đặt.dc.html:129]. | EVT-12 | 2 |
| 8 | Kiểm nghiệp vụ | Không rời màn âm thầm khi còn thay đổi chưa lưu | [Nội dung kiểm] Mọi đường rời màn phải đi qua popup xác nhận khi còn thay đổi chưa lưu ở ba nhóm chờ Lưu. Nhóm Giao diện **không** tính là thay đổi chưa lưu vì đã áp và ghi ngay.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Toàn màn. | Cảnh báo | Chưa có mã thông báo | Nội dung "Bạn có thay đổi chưa lưu ở nhóm Workspace, Phỏng vấn giả lập hoặc Thông báo. Rời màn bây giờ thì các thay đổi đó bị bỏ." Ngoại lệ có chủ đích: xác nhận xoá tài khoản thành công thì không hỏi lại. | EVT-10, EVT-14 | 1 |
| 9 | Kiểm nghiệp vụ | Tệp xuất chỉ chứa dữ liệu của chính người đăng nhập | [Nội dung kiểm] Nội dung tệp CSV và JSON chỉ gồm bản ghi có `user_id` khớp token; không chứa nội dung testcase ẩn, không chứa lời nhắc hệ thống của AI.<br>[Nơi thực thi] Máy chủ. | Lỗi | Mã lỗi trong phản hồi | Hai điều loại trừ này không phải suy diễn: testcase ẩn là tài sản của `problem-bank`, còn lời nhắc hệ thống lọt ra ngoài là một đường rò rỉ prompt. | EVT-8, EVT-9 | 1 |
| 10 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại thì chỉ khối tương ứng chuyển sang trạng thái lỗi, giữ nguyên giá trị người dùng vừa đặt, không hoàn về giá trị cũ.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." Lỗi lưu, xoá tài khoản và đổi mật khẩu (EVT-7, EVT-12, EVT-19) hiện bằng toast, lỗi xuất tệp (EVT-8, EVT-9) hiện tại chỗ dưới hai nút; lỗi tải nhóm (EVT-1, EVT-15) là nội dung thay chỗ nhóm kèm nút Thử lại. Không hoàn về giá trị cũ là có chủ đích: người dùng vừa gõ xong, mất công gõ lại là tệ hơn hiển thị một lỗi. | EVT-1, EVT-7, EVT-8, EVT-9, EVT-12, EVT-15 | 1 |
| 11 | Kiểm nhập liệu | Mật khẩu mới hợp lệ (chuyển từ `USR0502` Sheet 9 NO 7, V0.7) | [Nội dung kiểm] Bốn điều kiện chặn gửi: (1) mật khẩu mới từ 8 đến 128 ký tự; (2) có ít nhất một chữ cái; (3) có ít nhất một chữ số; (4) ô nhập lại khớp mật khẩu mới. Thêm điều kiện (5): biến thể "Đổi mật khẩu" bắt buộc có mật khẩu hiện tại. Kiểm theo thứ tự các ô, báo lỗi đầu tiên.<br>[Nơi thực thi] Độ dài, chữ, số, khớp nhau và thiếu mật khẩu hiện tại kiểm ở màn **và** máy chủ; "không trùng email hoặc tên hiển thị" (`IDT-V04`) chỉ máy chủ kiểm được.<br>[Tiêu điểm] Viền ô sai trong popup + toast. | Lỗi | Mã lỗi trong phản hồi | Nội dung theo khoá i18n: "Mật khẩu tối thiểu 8 ký tự", "Mật khẩu tối đa 128 ký tự", "Mật khẩu cần ít nhất một chữ cái", "Mật khẩu cần ít nhất một chữ số", "Hai mật khẩu không khớp", "Vui lòng nhập mật khẩu hiện tại" [Nguồn: 05-coding/frontend/messages/vi.json:2670-2675; 05-coding/frontend/src/entities/user/model/schema.ts:50-71]. Ngưỡng và mã `IDT-V01` đến `IDT-V04` ở DD [Nguồn: 03-dd/validation/identity.md:11-17]. **Thanh độ mạnh và danh sách bốn quy tắc là hiển thị**, không phải điều kiện chặn bổ sung: điều kiện (4) đồng thời là một dòng của danh sách, các điều kiện (1) đến (3) cũng vậy. | EVT-19 | 1 |
| 12 | Kiểm nghiệp vụ | Sai mật khẩu hiện tại (chuyển từ `USR0502`, V0.7) | [Nội dung kiểm] Máy chủ đối chiếu `currentPassword` với `password_hash`; sai thì từ chối, không đổi gì, không thu hồi phiên nào.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Viền ô mật khẩu hiện tại + toast. | Lỗi | Mã lỗi trong phản hồi | Nội dung "Mật khẩu hiện tại không đúng" (`errors.currentPasswordInvalid`) [Nguồn: 05-coding/frontend/messages/vi.json:2676]. Popup giữ mở để nhập lại. Giới hạn số lần thử sai mật khẩu hiện tại: chưa có nguồn, xem Q13. | EVT-19 | 2 |
| 13 | Kiểm nghiệp vụ | Tài khoản chưa từng có mật khẩu (chuyển từ `USR0502` Sheet 9 NO 10, V0.7) | [Nội dung kiểm] `password_hash IS NULL` thì `ChangeMyPassword` **không** yêu cầu mật khẩu hiện tại; ngược lại, tài khoản đã có mật khẩu mà gửi lên thiếu mật khẩu hiện tại thì từ chối.<br>[Nơi thực thi] Máy chủ (và màn: biến thể popup suy từ `hasPassword`). | Lỗi | Mã lỗi trong phản hồi | Nội dung "Vui lòng nhập mật khẩu hiện tại". Ca `NULL` là tài khoản đăng ký qua OAuth [Nguồn: 02-bd/database/identity.md:15]. Nhánh đặt lần đầu chỉ cần phiên đăng nhập hợp lệ, không OTP [Nguồn: 03-dd/logic/identity.md:90-93]. | EVT-16, EVT-19 | 3 |
| 14 | Kiểm nghiệp vụ | Đổi mật khẩu thu hồi các phiên khác (chuyển từ `USR0502` Sheet 9 NO 11, V0.7) | [Nội dung kiểm] Đổi mật khẩu thành công thì thu hồi refresh token của các phiên khác, giữ lại phiên đang thao tác. Hiển thị đúng một câu thông báo.<br>[Nơi thực thi] Máy chủ. | Cảnh báo | Chưa có mã thông báo | Nội dung "Đã đổi mật khẩu. Các thiết bị khác cần đăng nhập lại." [Nguồn: 03-dd/logic/identity.md:94-97; 05-coding/frontend/messages/vi.json:2646]. Cột `password_changed_at` không dùng để vô hiệu token [Nguồn: 02-bd/database/identity.md:20]. Câu thông báo hiện cho cả ca "Đặt mật khẩu" lần đầu dù ca đó không có phiên khác để thu hồi (BR-05 không nói thu hồi): xem Q13. | EVT-19 | 4 |
| 15 | Kiểm quyền | Đổi mật khẩu chỉ cho chính chủ | [Nội dung kiểm] `ChangeMyPassword` lấy `user_id` từ token của phiên đăng nhập, **không** nhận `user_id` từ phía gọi. Vai trò `INSTRUCTOR`/`ADMIN` đổi mật khẩu của chính mình qua cùng điểm vào (màn dùng chung), không có đường đổi mật khẩu người khác.<br>[Nơi thực thi] Máy chủ, ở tầng dịch vụ. | Lỗi | Mã lỗi trong phản hồi | Cùng nguyên tắc tiền tố `My` ở 7.3 [Nguồn: 02-bd/screens/users/USR0101_problem_list.md:530]. | EVT-19 | 5 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/security/identity.md:53-63; 02-bd/database/identity.md:23-24,28]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | **[ĐÃ ĐÓNG] Sáu tuỳ chọn trên prototype lưu ở đâu?** Chốt theo `DEC-2026-0922-users-and-admin-conflict-resolutions`: Tuỳ chọn Workspace (cỡ chữ, vim mode) lưu tại `localStorage` thiết bị theo tiêu chí ponytail tinh gọn. Cấp độ phỏng vấn (`preferred_interview_level`) lưu vào `identity.user_preferences`. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** số lượt tối đa (`preferred_max_turns`) và cho phép gợi ý (`preferred_hint_allowed`) cũng lưu ở bảng này, đóng nốt phần "chưa có cột" của NO 3, 4 (Sheet 5 Khu vực D). | Đã giải quyết theo tiêu chí tinh gọn không đẻ bảng nháp | Chủ dự án |
| Q2 | **[ĐÃ ĐÓNG] Chủ đề màu và ngôn ngữ giao diện lưu theo thiết bị hay theo tài khoản?** Đã chốt lưu theo **thiết bị** (Cookie `NEXT_LOCALE` + LocalStorage `next-themes`) theo `DEC-2026-0922-users-and-admin-conflict-resolutions`. Không lưu cột vào `users`. | Đã chốt theo DEC-2026-0922 | Chủ dự án |
| Q3 | **[ĐÃ ĐÓNG] Khung khu Người học có bộ chuyển trên header hay không?** Đã chốt: **Giữ bộ chuyển trên header** cho toàn bộ khu Người học theo `DEC-2026-0922-users-and-admin-conflict-resolutions` và đã cập nhật vào `02-bd/screens/users/_shell.md`. | Đã chốt theo DEC-2026-0922 | Chủ dự án |
| Q4 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] Nút ngôn ngữ nộp bài mặc định là danh sách động các ngôn ngữ có `judge.language_configs.enabled = true`; ngôn ngữ đã lưu bị tắt thì lùi về ngôn ngữ đầu tiên đang bật. Tiền đề cũ "bảng không có cột bật" đã hết hiệu lực: cột `enabled` có từ 2026-10-10 [Nguồn: 02-bd/database/judge-orchestration.md:111]. Tiêu chí ponytail bậc 4, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **Ngôn ngữ nộp bài mặc định có ẩn ngôn ngữ đang bị tắt không?** `ADM0501` nói màn quản trị ghi "trạng thái bật" của từng ngôn ngữ [Nguồn: 02-bd/screens/admin/ADM0501_language_config.md:388], nhưng bảng `judge.language_configs` không có cột nào cho trạng thái bật [Nguồn: 02-bd/database/judge-orchestration.md:93-102]. Hệ quả cho màn này: nếu trạng thái bật có thật thì cụm ba nút phải là **danh sách động** đọc từ `judge-orchestration`, và phải xử lý trường hợp người dùng đã chọn một ngôn ngữ sau đó bị admin tắt. BD này chốt tạm ba nút cố định, không gọi endpoint nào. Đề xuất: làm rõ ở BD `judge-orchestration` trước, vì cùng một câu hỏi sẽ quay lại ở màn `problem_detail` | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Hai tài liệu BD đang nói khác nhau về cùng một bảng | Chủ dự án + BD `judge-orchestration` |
| Q5 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] Xuất dữ liệu trả tệp đồng bộ, kèm giới hạn tần suất (ví dụ mỗi loại tệp một lần trong 5 phút, số chính xác ở DD); tập cột CSV và hình dạng JSON thuộc DD. Tiêu chí ponytail bậc 5, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **Xuất dữ liệu trả tệp đồng bộ hay chạy job nền?** Prototype chỉ vẽ hai nút, không có trạng thái chờ hay thông báo nào [Nguồn: 09-layoutBase/Cài đặt.dc.html:143-153]. Một người học lâu năm có thể có hàng nghìn lượt nộp, và tệp JSON hội thoại phỏng vấn còn nặng hơn vì chứa toàn văn từng lượt. Đề xuất: **đồng bộ cho đợt này** (quy mô đồ án, dữ liệu một người dùng không lớn) kèm một giới hạn tần suất, ví dụ mỗi loại tệp một lần trong 5 phút; chuyển sang job nền chỉ khi đo thấy thật sự chậm. Tập cột của tệp CSV và hình dạng tệp JSON cũng chưa chốt, thuộc DD | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: RD không nêu; prototype không dựng trạng thái chờ | Chủ dự án + DD `identity` |
| Q6 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] Xuất hội thoại phỏng vấn do `identity` sở hữu; `ai-review` hỏng thì báo lỗi bằng toast và giữ nút kích hoạt. Tiêu chí ponytail bậc 4, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **Xuất hội thoại phỏng vấn thuộc BC nào, và có phải suy giảm êm không?** F1-22 đặt việc này vào `identity` [Nguồn: 01-rd/req/identity.md:235-236] nhưng dữ liệu nằm hoàn toàn ở `ai.interview_sessions` và `ai.interview_turns`. BD này chốt tạm `identity` sở hữu và đọc qua cổng ra. Câu hỏi thật là: `ai-review` hỏng thì nút này báo lỗi hay bị khoá? BD này chốt **báo lỗi bằng toast, không khoá nút** — khoá nút sẽ khiến người dùng tưởng mình không có dữ liệu để xuất. Cần xác nhận vì nó đụng ràng buộc suy giảm êm của dự án | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Ranh giới BC cho một nghiệp vụ nằm vắt qua hai module chưa có tiền lệ trong tài liệu | Chủ dự án + BD `ai-review` |
| Q7 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] Xác nhận xoá tài khoản bằng cách gõ lại email của chính mình. Tiêu chí ponytail bậc 6, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **Xác nhận xoá tài khoản bằng gì?** BD này chốt tạm **gõ lại email của chính mình**, vì tài khoản chỉ đăng ký qua OAuth có thể chưa từng có mật khẩu [Nguồn: 02-bd/database/identity.md:15] nên không dùng được cách nhập mật khẩu. Hai khả năng khác: nhập mật khẩu với tài khoản có mật khẩu và bỏ qua bước xác nhận với tài khoản OAuth (không nhất quán), hoặc gửi mã OTP qua email như luồng F1-17 (chắc chắn hơn nhưng thêm một bước và một lời gọi). Prototype **không có** bước xác nhận nào | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Chưa nguồn nào chốt; đây là hành động không hoàn tác được nên không tự quyết | Chủ dự án |
| Q8 | **[ĐÃ CHỐT 2026-10-18, owner; ĐẢO NGƯỢC cùng ngày] Trong 30 ngày ân hạn người dùng đăng nhập lại để huỷ yêu cầu xoá (luồng khôi phục của `SHR0101`); câu chữ trên UI nói rõ điều đó. Tiêu chí ponytail bậc 4 (dùng lại luồng khôi phục có sẵn), `DEC-2026-1018-account-deletion-grace-cancel`. Câu trả lời đầu trong ngày ("không có đường tự huỷ", `DEC-2026-1018-usr-open-questions-phase2-5`) sai vì coi "không đăng nhập lại được" là đúng cho cả đăng nhập mở banner khôi phục.** Câu hỏi gốc: **Trong 30 ngày ân hạn, người dùng có đường nào huỷ việc xoá không?** `02-bd/security/identity.md:60-62` nói tài khoản chuyển `DEACTIVATED` ngay và mọi token hết hiệu lực — tức **không đăng nhập lại được**, nên cũng không có đường nào tự huỷ. Nhưng khoảng ân hạn 30 ngày chỉ có nghĩa nếu nó cho phép đổi ý. BD này chốt tạm: không có đường tự huỷ, và câu chữ trên UI **không hứa** có thể khôi phục. Đề xuất: nếu muốn cho đổi ý thì đường rẻ nhất là để A3 mở khoá lại qua F1-13, và câu chữ trên UI phải nói rõ "liên hệ quản trị viên" thay vì im lặng | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Ràng buộc bảo mật và ý nghĩa của khoảng ân hạn đang mâu thuẫn nhẹ với nhau | Chủ dự án |
| Q9 | **[ĐÃ ĐÓNG] `users.default_language` xuất hiện ở màn nào — `profile`, `settings`, hay cả hai?** Đã chốt theo `DEC-2026-0922-users-and-admin-conflict-resolutions`: Cột `default_language` là tuỳ chọn hành vi ứng dụng nên thuộc về **`settings` (USR0503)**; đã loại bỏ ô chọn này khỏi form sửa `profile` (USR0502) để tránh xung đột 2 đường ghi. | Đã chốt theo DEC-2026-0922 | Chủ dự án |
| Q10 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] "Xuất dữ liệu" và "Vùng nguy hiểm" ở lại `settings`. Tiêu chí ponytail bậc 4, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **"Xuất dữ liệu" và "Vùng nguy hiểm" thuộc `settings` hay `profile`?** Cả hai đều thao tác trên **tài khoản** chứ không phải trên tuỳ chọn hành vi, nên theo đúng câu chữ của ranh giới thì chúng nghiêng về `profile`. Nhưng prototype đặt cả hai ở màn này [Nguồn: 09-layoutBase/Cài đặt.dc.html:124-133,143-153], và F1-16/F1-22 được RD của màn này nhận [Nguồn: 01-rd/screens/users/USR0503_settings.md:86,91]. BD này giữ cả hai ở `settings` theo prototype và theo RD. Nêu ra vì đây đúng là loại mục có thể thuộc cả hai màn, và nếu `USR0502` cũng thiết kế chúng thì hai file sẽ trùng nhau | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Ranh giới "nhận dạng" và "hành vi ứng dụng" không cắt gọn qua hai mục này | Chủ dự án + BD `USR0502` |
| Q11 | **[ĐÃ CHỐT 2026-10-18, owner, theo đề xuất] Mỗi nhóm báo kết quả lưu riêng; nhóm lưu được thì xoá cờ thay đổi chưa lưu của riêng nó. Tiêu chí ponytail bậc 4, `DEC-2026-1018-usr-open-questions-phase2-5`.** Câu hỏi gốc: **Một nút "Lưu cài đặt" ghi sang hai Bounded Context — thất bại một nửa thì thế nào?** EVT-7 gọi `UpdateMySettings` (`identity`) và `UpdateMyInterviewPreferences` (`ai-review`). Không thể gộp thành một giao dịch vì hai module khác nhau, và cũng không nên gộp vì gộp thì `ai-review` hỏng sẽ chặn luôn việc lưu thông báo. BD này chốt tạm: **mỗi nhóm báo kết quả riêng**, nhóm lưu được thì xoá cờ thay đổi chưa lưu của riêng nó. Hai khả năng khác: tách thành hai nút Lưu (rõ ràng nhưng lệch prototype), hoặc chỉ báo "lưu thất bại" chung và giữ nguyên toàn bộ cờ (đơn giản nhưng khiến người dùng bấm lại và ghi đè lần nữa lên nhóm đã lưu thành công) | Đã chốt 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; câu "vì sao chưa trả lời được" cũ: Prototype chỉ có một nút và không dựng trạng thái lỗi nào. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** nửa `ai-review` của câu hỏi này lỗi thời vì nhóm Phỏng vấn giả lập nay lưu ở `identity` (một giao dịch với nhóm Thông báo); câu hỏi còn đúng cho phần Workspace (thiết bị) so với phần máy chủ. Chưa viết lại EVT-7 và dòng suy giảm (NO 6 Khu vực D, Sheet 9 NO 5), chờ chủ dự án duyệt | Chủ dự án + DD màn hình |
| Q12 | **Popup rời màn có hai nút (Ở lại, Rời đi) hay ba nút (thêm Lưu và thoát)?** | **ĐÃ CHỐT 2026-10-19 (owner)**: giữ hộp thoại ba nút dùng chung `UnsavedChangesDialog` (`Lưu và thoát` / `Thoát không lưu` / `Ở lại`) [Nguồn: 05-coding/frontend/src/shared/ui/feedback/unsaved-changes-dialog.tsx:44-63]; BD từng ghi hai nút nên đã sửa Khu vực J, Sheet 6 và EVT-14 theo bản dựng. Ponytail bậc 4 (dùng lại thành phần `shared/ui`). | Chủ dự án |
| Q13 | **Ba điểm nhỏ của popup mật khẩu sau khi chuyển về `settings` (V0.7), chưa ai chốt.** (a) Bản dựng gán **mọi** lỗi máy chủ vào viền ô mật khẩu hiện tại và giữ nguyên giá trị ô; BD `USR0502` cũ ghi "xoá ô mật khẩu hiện tại" khi sai. Giữ theo bản dựng hay sửa code theo mô tả cũ? (b) Toast "Các thiết bị khác cần đăng nhập lại" cũng hiện ở ca "Đặt mật khẩu" lần đầu (OAuth) — ca này DD không nói thu hồi phiên [Nguồn: 03-dd/logic/identity.md:90-93] nên câu có thể sai; cần câu riêng hay chấp nhận? (c) Có giới hạn số lần nhập sai mật khẩu hiện tại ở popup này không (rate-limit đăng nhập `IDT-103` chỉ áp cho `/auth/login` [Nguồn: 04-tdd/identity.md:23-26])? Màn dùng chung với giảng viên nên (a)-(c) áp cho cả hai khu. | **ĐÃ CHỐT 2026/10/21 (owner):** (a) giữ theo bản dựng, sai mật khẩu hiện tại thì không xoá ô, chỉ hiện lỗi; (b) ca Đặt mật khẩu lần đầu hiện toast "Đã đặt mật khẩu." không kèm câu "Các thiết bị khác cần đăng nhập lại" (đã sửa code, khoá `profile.passwordDialog.doneSet`); (c) chưa giới hạn số lần nhập sai mật khẩu hiện tại, ghi nợ prototype (ponytail bậc 3); (d) thẻ Bảo mật vẫn hiện ở Cài đặt giảng viên (giữ). Nội dung cũ: Prototype chưa có DD màn hình; điều (a) và (b) là khác biệt giữa BD cũ, DD và code; điều (c) không nguồn nào nói | Chủ dự án |

---

## Tham chiếu

- `01-rd/screens/users/USR0503_settings.md` — RD của màn.
- `01-rd/req/identity.md:197-202,231-238` — F1-16, F1-20, F1-21, F1-22 · `01-rd/req/identity.md:46` — F1-19 (đổi mật khẩu, V0.7) · `01-rd/req/ai-review.md:103-107` — F5-28.
- `02-bd/screens/users/_shell.md` — khung điều hướng khu Người học.
- `02-bd/screens/users/USR0501_my_progress.md` — gốc cụm ba màn cá nhân, nguồn của quy ước đặt tên khối,
  tiền tố ID và cách xử lý suy giảm êm.
- `02-bd/screens/users/USR0101_problem_list.md:530` — quy ước tiền tố nghiệp vụ `My`.
- `02-bd/database/identity.md` mục 1.1, 1.12 · `02-bd/security/identity.md` mục 3, 4 ·
  `02-bd/database/ai-review.md` mục 1.4 và bổ sung `interview_turns` ·
  `02-bd/database/judge-orchestration.md` mục 1.1, 1.5.
- `02-bd/screens/admin/ADM0501_language_config.md` — màn cấu hình ngôn ngữ **lập trình** của quản trị
  viên, đọc để không nhầm với ngôn ngữ giao diện.
- `09-layoutBase/Cài đặt.dc.html` — bằng chứng bố cục chỉ-đọc.
- `02-bd/_rules/bd-template-9sheet.md` — quy ước mẫu 9 sheet.
- Quyết định: `DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en`,
  `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0831-i18n-scope-expansion`,
  `DEC-2026-1020-student-workspace-split-and-password-in-settings` (phần PASSWORD).
- `02-bd/screens/users/USR0502_profile.md` — nơi mô tả đổi mật khẩu trước V0.7; `03-dd/api/identity.md:44`,
  `03-dd/logic/identity.md:90-97`, `03-dd/validation/identity.md:7-19` — hợp đồng, BR-05/BR-06 và quy tắc mật khẩu.
