# RD (Yêu cầu hệ thống mới) — Giám sát hàng đợi / `ADM0401`

> Mã màn hình: `ADM0401` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_queue_monitor` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`].
> Phạm vi/Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/F4_judge-orchestration.md` (F4-07, F4-10), `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (prototype gốc, chỉ để tham khảo), `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`).
> Phiên bản RD: sửa 2026-10-10 theo `DEC-2026-1010-admin-operations-regroup`; sửa 2026-11-22 theo `DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401` (khối "Cụm worker" đổi thành "Luồng chấm", thêm tốc độ chấm và ước lượng hết hàng đợi). Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Giám sát trực tiếp độ dài hàng đợi, luồng chấm đang chạy (số consumer chấm bài), tốc độ chấm, thời gian dự kiến
hết hàng đợi và job đang chờ/đang chấm, để quản trị viên biết hệ thống có đang khoẻ không và xử lý kịp khi có
bài nộp treo [SoT: 01-rd/req/F4_judge-orchestration.md — F4-10].

Màn này **chỉ hiện cái bất thường trước**: một dải trạng thái một dòng cho biết hệ thống đang ổn hay không,
phần chi tiết (luồng chấm, job) nằm bên dưới và gọn. Log hạ tầng của máy móc (bản ghi Outbox kẹt,
lượt nộp lỗi sandbox) **không còn nằm ở màn này** mà ở màn riêng `ADM0404` Sự kiện hạ tầng
(`01-rd/screens/admin/ADM0404_infra_events.md`), vì nó chỉ đọc khi có sự cố và cần lọc, tìm kiếm, xem lùi.

Sau dải trạng thái, màn đặt các số liệu vận hành trong một hàng để admin đọc nhanh trước khi xem danh sách bài nộp. Số đếm bài chờ và bài lỗi nằm ở bộ lọc bảng. Khi một danh sách rỗng, màn hiện một dòng theo bộ lọc. Các công tắc, mục tiêu số luồng và lần thay đổi gần nhất nằm trong nhóm Điều khiển có thể mở khi cần. Trạng thái tạm dừng vẫn hiển thị ở dải trạng thái.

Đối chiếu prototype gốc: `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (chỉ tham khảo từ 2026-10-03; prototype
Next.js 2026-10-10 là sự thật về hình). File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc
tả chức năng đã có ở `01-rd/req/F4_judge-orchestration.md` (mục F4), chỉ trỏ tới và bổ sung phần đặc thù của màn.
Khái niệm **"kỳ thi"** **không tồn tại** trong hệ thống
(`DEC-2026-0831-judge-orchestration-ops-details`), và màn `admin_rejudge` đã loại khỏi phạm vi
(`DEC-2026-0828-remove-rejudge-scope`).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giám sát hàng đợi và tình trạng cụm judge engine | F4-10 | `01-rd/req/F4_judge-orchestration.md` — F4-10 |
| Timeout sweep bài nộp treo (bỏ qua job `PENDING` khi hàng đợi đang tạm dừng) | F4-07 | `01-rd/req/F4_judge-orchestration.md` — F4-07, amendment 2026-10-10 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |

Các khối của màn (thiết kế lại 2026-10-10, `DEC-2026-1010-admin-operations-regroup`):

1. **Dải trạng thái** — một dòng: "Bình thường" / "Đang dồn N job" / "Đang tạm dừng", kèm
   "Cập nhật lúc HH:mm:ss"; quá 60 giây không nhận dữ liệu mới thì ghi "Dữ liệu cũ". Trạng thái "Worker quá
   tải" đã bỏ từ 2026-10-25 vì go-judge không có mức tải từng worker (xem mục 2.2). Đây **không phải thẻ
   KPI** nên không trái `DEC-2026-1001-single-overview-page-kpi` (chỉ trang tổng quan hiển thị KPI). Dải 4
   chỉ số tổng của mockup (Job đang chờ, Đang chấm, Thông lượng, Job lỗi 24 giờ) vẫn **không dựng**.
2. **Luồng chấm** (thay khối "Cụm worker go-judge", 2026-11-22) — một thẻ cho biết cụm chấm bài đang làm gì,
   gồm bốn nhóm thông tin, xem mục 2.2: số luồng chấm đang chạy trên mục tiêu và mức tối đa; lần mở rộng
   hoặc thu hẹp gần nhất; tốc độ chấm (lượt mỗi phút); thời gian dự kiến hết hàng đợi. Chỉ dựng ở chế độ dữ
   liệu thật. Bản demo dữ liệu mẫu vẫn giữ danh sách worker minh hoạ cũ.
3. **Điều khiển** — một thẻ gồm độ trễ chờ trung bình (một con số) và hai công tắc: "Tạm dừng tiêu thụ hàng
   đợi", "Tự động mở rộng worker" (Q1 cũ đã chốt F4-10 bao gồm hai điều khiển này). Tạm dừng nghĩa là worker ngừng nhận job mới, job đang chấm vẫn chạy hết. Bật tạm dừng và tắt tự
   động mở rộng đều **hỏi xác nhận** trước khi gửi lệnh.
4. **Bảng job** — tab có đếm: `Đang chờ N · Lỗi N · Tất cả N`, mặc định "Đang chờ"; 4 cột (Lượt nộp, Ngôn
   ngữ, Trạng thái, Chờ), sắp xếp theo thời gian chờ, phân trang theo tham số "số dòng mỗi trang".
5. **Thẻ liên kết sự kiện hạ tầng** — số cảnh báo và số lỗi gần đây, bấm sang `ADM0404`.
6. **Cấu hình** — nút ở thanh tiêu đề mở popup "Tham số hàng đợi" (bốn số nguyên); cạnh nút "Làm mới" **tải lại
   thật**.

Đã **bỏ khỏi màn** (không phải bỏ chức năng):

- Mức độ trễ thứ hai "Hàng đợi ưu tiên cao" và cột "Ưu tiên" của bảng job: **chủ dự án đã xác nhận 2026-10-10**
  (`DEC-2026-1010-admin-operations-regroup`, đóng Q4 của BD). Lý do: bảng `submissions` không có cờ ưu tiên
  nào, hiển thị nó là bịa dữ liệu.
- Cột "Job" (`#J-...`): không có thực thể "job" riêng, chỉ có `submissions.id`.
- Mức "Hàng đợi chấm lại", giá trị `Chấm lại` và nút chấm lại: hết ý nghĩa sau `DEC-2026-0828-remove-rejudge-scope`.
- Khối "Sự kiện hạ tầng gần đây": chuyển sang `ADM0404`.
- Bảng worker từng hàng với mức tải %, uptime và số vCPU: không có nguồn thật ở chế độ dữ liệu thật (mục 2.2).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0401` / `admin_queue_monitor` | `judge-orchestration` |
| Màn liên quan | `ADM0404` / `admin_infra_events` — log máy | `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/F4_judge-orchestration.md` (F4-07, F4-10) | Nguồn SoT của màn |
| Prototype gốc | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` | Bằng chứng bố cục ban đầu, chỉ tham khảo |

### 2.2 Khối "Luồng chấm" và việc đảo lại quyết định cũ

Mục này mô tả thông tin quản trị viên cần thấy, và ghi rõ quyết định nào bị đảo lại. Bảng 1 liệt kê các thông tin của khối.

Bảng 1: Thông tin của khối "Luồng chấm" (chế độ dữ liệu thật).

| Thông tin | Ý nghĩa với quản trị viên | Khi chưa có dữ liệu |
| :--- | :--- | :--- |
| Số luồng chấm đang chạy trên mức tối đa | Hiện có bao nhiêu luồng chấm bài đang nhận việc, trên tối đa cho phép (ví dụ "3 / 8") | Báo rõ "Chưa nhận được báo cáo từ worker chấm bài" |
| Mục tiêu | Số luồng hệ thống đang hướng tới; khác số đang chạy khi luồng thừa còn chờ chấm xong bài | Như trên |
| Lần mở rộng hoặc thu hẹp gần nhất | Thời điểm, tăng hay giảm, từ mấy lên mấy, lý do (hàng đợi dồn / hàng đợi trống / tắt tự động mở rộng) và số job đang chờ lúc đó | Báo "Chưa có lần đổi nào" khi worker chưa từng đổi số luồng |
| Tốc độ chấm | Số lượt nộp chấm xong trong một phút gần nhất (lượt/phút) | Hiện 0 khi phút qua không có lượt nào |
| Dự kiến hết hàng đợi | Thời gian ước tính để chấm hết số job đang chờ theo tốc độ hiện tại | Hiện "Chưa đủ dữ liệu" khi không ước lượng được |

**Đảo lại quyết định cũ.** Ba điều dưới đây đổi so với bản RD 2026-10-10 và so với lựa chọn của chủ dự án ngày 2026-10-11 (giữ bảng worker ở dạng mẫu cho tới khi cài go-judge thật), theo `DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401` (chủ dự án duyệt 2026-11-22 sau khi chạy thử 30 bài nộp, tự động mở rộng từ 1 lên 5 luồng, và thấy màn không hiện được điều cụm vừa làm):

1. Số liệu cụm **không** lấy từ Prometheus và **không** là bảng worker. Nó là số luồng chấm do chính hệ thống báo về (xem BD `02-bd/database/F4_judge-orchestration.md` mục 1.11). Màn không phụ thuộc việc máy chủ đo đạc có chạy hay không.
2. Khối "Cụm worker go-judge" ở chế độ dữ liệu thật được thay bằng "Luồng chấm". Go-judge không có khái niệm worker từng hàng, nên danh sách worker chỉ còn ở bản demo dữ liệu mẫu.
3. Hai thông tin mới: tốc độ chấm và thời gian dự kiến hết hàng đợi. Công thức ước lượng dùng lại đúng công thức của F4-14 (`DEC-2026-1115-f4-queue-wait-in-progress-snapshot`): số job đang chờ chia tốc độ chấm trong một phút gần nhất, làm tròn lên, không ước lượng khi dưới 3 lượt chấm xong, trần 3600 giây.

**Không hiển thị** (không có nguồn thật, tiêu chí ponytail bậc 2): mức tải %, uptime, số vCPU, trạng thái từng worker. **Không làm** (bậc 3): bấm vào một dòng job để xem chi tiết, vì ảnh chụp hàng đợi cố ý không mang dữ liệu học viên, bài toán hay testcase. **Không** thêm Prometheus làm phụ thuộc của màn.

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0401_queue_monitor.md`).
- Hợp đồng API (số liệu luồng chấm, điều khiển pause/autoscale, kênh cập nhật hàng đợi) — thuộc DD
  (`03-dd/api/F4_judge-orchestration.md` mục 16).
- Bấm dòng job để xem chi tiết, mức tải %, uptime, vCPU từng worker, Prometheus làm nguồn của màn
  (mục 2.2).
- ~~Khái niệm "kỳ thi" (Q2) — chờ chủ dự án quyết định phạm vi.~~ **Đã chốt 2026-08-31**: không có khái niệm
  kỳ thi (`DEC-2026-0831-judge-orchestration-ops-details`).
- Chấm lại hàng loạt (`DEC-2026-0828-remove-rejudge-scope`).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Hai toggle "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-10, không cần mã mới. | — | Đã ghi vào `01-rd/req/F4_judge-orchestration.md` (amendment F4-10). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |
| Q2 | ~~**Khái niệm "kỳ thi"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ là nhãn phân loại độ ưu tiên, không có luồng "quản lý kỳ thi" nào. **Cập nhật 2026-10-10:** nhãn "ưu tiên" nay đề xuất bỏ hẳn khỏi màn (xem mục 2, chờ chủ dự án xác nhận). | — | Xem `DEC-2026-0831-judge-orchestration-ops-details` và `DEC-2026-1010-admin-operations-regroup`. | Chờ xác nhận phần bỏ ưu tiên |
| Q3 | **Quy tắc mới 2026-10-10, đã chốt:** khi Admin tạm dừng tiêu thụ hàng đợi, timeout sweep (F4-07) bỏ qua job `PENDING`; mọi thao tác ghi vận hành của màn được ghi vào Nhật ký hệ thống, phân loại "Cấu hình", kèm giá trị cũ và mới (đúng F1-14). | — | `01-rd/req/F4_judge-orchestration.md` (amendment 2026-10-10); `DEC-2026-1010-admin-operations-regroup` | Đã đóng |
| Q4 | ~~Admin có cần trần số worker và ngưỡng thu hẹp cho tự động mở rộng không?~~ **ĐÃ CHỐT 2026-10-10 (chủ dự án đã xác nhận 2026-10-10, tiêu chí ponytail bậc 3 và 5):** không thêm; "tối đa 8" là hằng số `application.yml`. Rủi ro autoscale thật trên Docker Compose ghi ở BD `ADM0401` mục "Rủi ro kỹ thuật" (R2) | - | Đã chốt, trùng BD Q8 | Đã đóng |
| Q5 | Khối "Luồng chấm" chỉ hiện lần đổi số luồng **gần nhất**. Quản trị viên có cần xem lịch sử nhiều lần đổi không? | Không có nguồn lưu lịch sử (bảng chỉ giữ một dòng); thêm bảng lịch sử là việc lớn hơn nhu cầu buổi demo. Đề xuất: **không**, tiêu chí ponytail bậc 3. Nếu cần xem lại thì đã có log hệ thống ghi mỗi lần đổi `{oldTarget,newTarget,pendingCount,threshold,reason}` | Chủ dự án (xác nhận đề xuất) |
| Q6 | Khi chưa nhận được báo cáo từ worker (quá 60 giây hoặc chưa từng báo), phần "Luồng chấm" chỉ báo "Chưa nhận được báo cáo" và vẫn hiện tốc độ chấm cùng dự kiến hết hàng đợi (hai số này tính từ bài nộp, không cần worker). Chủ dự án có muốn ẩn luôn cả khối khi worker im lặng không? | Ẩn cả khối làm mất thông tin đúng lúc quản trị viên cần (worker chết mà hàng đợi vẫn dồn). Đề xuất: **giữ hai số**, `[SoT: Suy luận]` | Chủ dự án (xác nhận đề xuất) |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giám sát hàng đợi và tình trạng cụm judge engine (F4-10) | Chức năng | `01-rd/req/F4_judge-orchestration.md` — F4-10 |
| REQ-02 | Timeout sweep bài nộp treo (F4-07), bỏ qua job `PENDING` khi hàng đợi đang tạm dừng | Chức năng | `01-rd/req/F4_judge-orchestration.md` — F4-07, amendment 2026-10-10 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |
| REQ-04 | **Cho** hàng đợi đang bình thường, **Khi** quản trị viên mở màn, **Thì** dải trạng thái hiện "Bình thường" kèm "Cập nhật lúc"; **Khi** hàng đợi đang dồn hoặc đang tạm dừng, **Thì** dải đổi sang trạng thái tương ứng; **Khi** quá 60 giây không nhận dữ liệu mới, **Thì** hiện "Dữ liệu cũ". Trạng thái "Worker quá tải" đã bỏ từ 2026-10-25 (không có mức tải từng worker). | Chức năng | Chốt 2026-10-10 (`DEC-2026-1010-admin-operations-regroup`); sửa 2026-11-22 |
| REQ-05 | **Cho** quản trị viên bật "Tạm dừng tiêu thụ hàng đợi" hoặc tắt "Tự động mở rộng worker", **Khi** bấm công tắc, **Thì** hiện hộp xác nhận nêu hậu quả; chỉ gửi lệnh sau khi xác nhận; **Khi** lệnh thành công, **Thì** thao tác được ghi vào Nhật ký hệ thống (phân loại "Cấu hình"). Tắt tạm dừng và bật tự động mở rộng không cần hỏi. | Chức năng | Chốt 2026-10-10 |
| REQ-06 | **Cho** quản trị viên bấm "Làm mới", **Khi** tải xong, **Thì** mọi số liệu trên màn là dữ liệu mới và "Cập nhật lúc" đặt lại; **Khi** tải thất bại, **Thì** giữ dữ liệu cũ và báo lỗi. Dữ liệu cũng tự cập nhật qua kênh thời gian thực. | Chức năng | Chốt 2026-10-10 |
| REQ-07 | **Cho** cụm chấm bài đang chạy và báo số liệu về hệ thống, **Khi** quản trị viên mở màn (chế độ dữ liệu thật), **Thì** khối "Luồng chấm" hiện số luồng chấm đang chạy trên mức tối đa, mục tiêu hiện tại, và lần mở rộng hoặc thu hẹp gần nhất gồm thời điểm, tăng hay giảm, từ mấy lên mấy, lý do (hàng đợi dồn / hàng đợi trống / tắt tự động mở rộng) và số job đang chờ lúc đó; **Khi** chưa từng đổi số luồng, **Thì** ghi "Chưa có lần đổi nào"; **Khi** quá 60 giây không nhận được báo cáo từ worker hoặc chưa từng có báo cáo, **Thì** khối ghi rõ "Chưa nhận được báo cáo từ worker chấm bài" và **không** hiện số luồng cũ như thể còn đúng. | Chức năng | Chốt 2026-11-22 (`DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401`) |
| REQ-08 | **Cho** hàng đợi đang có job chờ, **Khi** quản trị viên xem khối "Luồng chấm", **Thì** thấy tốc độ chấm (lượt/phút, đếm số lượt chấm xong trong một phút gần nhất) và thời gian dự kiến hết hàng đợi; **Khi** chưa đủ dữ liệu để ước lượng (dưới 3 lượt chấm xong trong phút qua, hoặc không có job chờ), **Thì** hiện "Chưa đủ dữ liệu" thay cho con số, không hiện 0 hay vô hạn; thời gian dự kiến không vượt 1 giờ. | Chức năng | Chốt 2026-11-22; công thức dùng lại F4-14 (`DEC-2026-1115-f4-queue-wait-in-progress-snapshot`) |
| REQ-09 | **Cho** màn ở chế độ dữ liệu thật, **Khi** hiển thị luồng chấm, **Thì** không hiện mức tải %, uptime, số vCPU hay danh sách worker, và dòng job trong bảng không bấm được để xem chi tiết. | Ràng buộc | Chốt 2026-11-22 (ponytail bậc 2 và 3) |

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
| BD màn hình | `02-bd/screens/admin/ADM0401_queue_monitor.md` (V1.6) |
| DD/API liên quan | `03-dd/api/F4_judge-orchestration.md` mục 16; `03-dd/screens/admin/ADM0401_queue_monitor.md` |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | Xem các yêu cầu tại chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/F4_judge-orchestration.md` — F4-07, F4-10. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-03`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_queue_monitor`. |
| Prototype gốc | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — chỉ tham khảo. |
| Màn liên quan | `01-rd/screens/admin/ADM0404_infra_events.md` — sự kiện hạ tầng. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope`; `DEC-2026-0831-judge-orchestration-ops-details`; `DEC-2026-1001-single-overview-page-kpi`; `DEC-2026-1010-admin-operations-regroup`; `DEC-2026-1115-f4-queue-wait-in-progress-snapshot`; `DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401`. |
| Báo cáo | `06-plan/reports/261010-1030-admin-van-hanh-userflow-va-mau-thuan/report.md` — phát hiện F1-F12, lỗ hổng G1-G6. |
