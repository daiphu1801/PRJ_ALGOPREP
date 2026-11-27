# RD (Yêu cầu hệ thống mới) — Tổng quan / `USR0601`

> Mã màn hình: `USR0601` [Nguồn: `02-bd/_rules/bd-template-9sheet.md` — mục 8].
> Slug chính tắc: `dashboard`.
> Phạm vi/Bounded Context: `identity` (F1), có đọc thêm từ `problem-bank` (F2), `judge-orchestration` (F4) và `ai-review` (F5) cho các khối gợi ý bài, hoạt động và báo cáo gần đây. Actor: A1.
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-31), `.nexa/control/decision-registry.md` (`DEC-2026-0927-student-dashboard-home`), `09-layoutBase/Dashboard AlgoPrep.dc.html` (prototype).
> Tài liệu này mô tả hành vi và UX mức yêu cầu của màn hình, không lặp lại đặc tả chức năng nguồn.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trang đích sau khi người học đăng nhập. Trả lời đúng một câu hỏi: **làm gì tiếp theo** — tiếp tục bài đang làm dở, hay bắt một bài mới ở chủ đề đang yếu nhất [SoT: `01-rd/req/F1_identity.md` — F1-31].

Màn này được bổ sung ngày 2026-09-27 theo `DEC-2026-0927-student-dashboard-home`. Trước đó `STUDENT` là vai trò duy nhất không có màn tổng quan riêng, trong khi `ADMIN` đã có `ADM0101_overview` và `INSTRUCTOR` đã có `INS0101_instructor_overview`.

**Cập nhật cùng ngày (`DEC-2026-0927-student-area-merge-and-shared-shell`): màn `USR0501_my_progress` đã gộp vào đây**, route `/progress` bỏ. Khu Người học vẫn 12 màn (13 trừ 1 đã gộp). Ba khối của `USR0501` mà màn này chưa có — bảng "Theo chủ đề" đầy đủ, khối "Theo độ khó", khối "Nên ưu tiên" — chuyển sang đây và thay thế phần thanh tiến độ chủ đề rút gọn của prototype dashboard, vốn nói ít hơn hẳn. Mọi yêu cầu REQ-01 tới REQ-06 của `USR0501` vẫn còn hiệu lực, chỉ đổi nơi thực hiện; hai quyết định Q1 (định nghĩa streak) và Q2 (quy tắc "Nên ưu tiên", không gọi AI) của file đó được dùng lại nguyên văn, không định nghĩa lại.

Quyết định đó cũng **sửa một phần Q3 của `01-rd/screens/shared/SHR0101_auth.md`** (chốt 2026-08-25): đích sau đăng nhập của `STUDENT` đổi từ `my_progress` sang `dashboard`. Hai đích còn lại và quy tắc ưu tiên returnUrl không đổi.

**Ghi chú thay đổi 2026-10-18** (`DEC-2026-1018-usr-open-questions-phase1-scope`, `DEC-2026-1018-usr-open-questions-phase2-5`; RD chưa có bảng lịch sử nên ghi tại đây): (1) màn nhận thêm F6-10, tỉ lệ hoàn thành ôn câu hỏi theo chủ đề (mục 2 dòng 12, REQ-11); (2) đóng Q2 (thang năng lực) và Q3 (số hàng đợi ở chân trang) ở chương 4; (3) bộ lọc khoảng thời gian của REQ-05 chỉ áp cho biểu đồ, cột "Tỉ lệ AC" và "Lần cuối" của bảng "Theo chủ đề", "Nên ưu tiên" và khối phiên phỏng vấn; (4) nút "Xem lộ trình" đã bỏ từ trước cùng ngày (Q1). Không đổi REQ nào khác.

**Ghi chú thay đổi 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`, chủ dự án duyệt từng câu một sau khi chụp cả trang thật thấy cùng một thông tin hiện ở nhiều khối; RD chưa có bảng lịch sử nên ghi tại đây): màn hiện **mỗi thông tin một lần**. (1) **Bỏ ba khối**: radar "Năng lực theo chủ đề", khối "Nên ưu tiên" (gộp từ `USR0501`) và lưới "Hoạt động 12 tháng". Chỉ bỏ khối, **dữ liệu giữ nguyên** (tiến độ theo chủ đề, lịch hoạt động F1 #55, tóm tắt phỏng vấn). Chuyện chủ đề yếu do lời chào (REQ-04) và bảng "Theo chủ đề" kể; chuyện hoạt động do biểu đồ "Bài nộp theo ngày" kể. (2) Khối Mock Interview: bỏ dải tóm tắt (số phiên, điểm trung bình, tiêu chí yếu nhất) vì thẻ thống kê đã có số phiên và điểm trung bình; 3 phiên gần nhất mỗi phiên **một dòng** gồm điểm tổng và tiêu chí yếu nhất của phiên đó, không còn thanh theo tiêu chí. (3) "Bài toán gợi ý": chỉ bài **chưa giải**, tối đa 5 dòng, bỏ tab độ khó và chip chủ đề, thêm liên kết "Tất cả" tới danh sách bài; xếp hạng phía máy chủ (F2 #12) không đổi. (4) Các khối còn lại: lời chào cùng dải khoảng thời gian, bốn thẻ thống kê, "Bài nộp theo ngày", "Theo độ khó", "Theo chủ đề", "Ôn câu hỏi theo chủ đề" (F6-10), "Bài toán gợi ý", "Mock Interview", "Solution Review". (5) Vỏ khu Người học: chân trang một dòng, vùng nội dung cuộn theo trang và chân trang đi theo nội dung (chi tiết ở `02-bd/screens/users/_shell.md`). Ảnh hưởng: REQ-01, REQ-05, REQ-06, REQ-10 viết lại (câu cũ gạch ngang); mục 5, 6, 7, 9 của chương 2 và Q2, Q5 chương 4 có ghi chú đóng. `[SoT: .nexa/control/decision-registry.md — DEC-2026-1123-student-dashboard-trim]`

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
|---|---|---|
| Dashboard tổng quan luyện tập của người học | F1-31 | `01-rd/req/F1_identity.md` — F1-31 |
| Bài đã giải theo chủ đề (dữ liệu nền cho thẻ thống kê và tiến độ chủ đề) | F1-06 | `01-rd/req/F1_identity.md` — F1-06 |
| Tỉ lệ chấp thuận (dữ liệu nền cho thẻ Acceptance rate và biểu đồ) | F1-07 | `01-rd/req/F1_identity.md` — F1-07 |
| Lịch sử phỏng vấn (dữ liệu nền cho khối Mock Interview gần đây) | F1-08 | `01-rd/req/F1_identity.md` — F1-08 |
| Tỉ lệ hoàn thành ôn câu hỏi phỏng vấn theo từng chủ đề (bảng phụ trong khối "Theo chủ đề") | F6-10 | `01-rd/req/F6_interview-bank.md:26-27` — F6-10; chủ dự án giao cho màn này 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`) |

Đối chiếu `09-layoutBase/Dashboard AlgoPrep.dc.html`:

1. **Lời chào + chủ đề yếu nhất tuần** — `"Chào {firstName}, tiếp tục nhé"`, kèm một dòng nêu chủ đề có tỉ lệ AC thấp nhất trong 6 chủ đề (dòng 101-103). Suy luận hiển thị từ F1-06/F1-07, cùng nhóm quy tắc với khối "Nên ưu tiên" của `USR0501` (đã chốt ở Q2 màn đó; khối đó đã bỏ 2026-11-23, lời chào giữ quy tắc).
2. **Một nút hành động** — "Tiếp tục bài đang làm" (dòng 106), dẫn về bản nháp gần nhất trong Workspace. Nút "Xem lộ trình" (dòng 105) của prototype **bỏ khỏi yêu cầu — đã chốt 2026-10-18 (owner)**, xem Câu hỏi mở Q1.
3. **Bốn thẻ thống kê** — Đã giải (`182 / 640`), Acceptance rate (`61%`), Streak (`23 ngày`), Mock Interview (`7 phiên`, rubric trung bình `3.6 / 5`) (dòng 111-125). Khớp F1-06/F1-07/F1-08; "Streak" dùng lại đúng định nghĩa đã chốt ở `USR0501` Q1, không định nghĩa lại.
4. **Biểu đồ "Bài nộp theo ngày"** — đường có vùng tô, kèm dòng tóm tắt tổng và trung bình mỗi ngày (dòng 131-157). Cùng dữ liệu F1-07. Ba tab 7/30/90 ngày của prototype **không** dựng riêng cho khối này — sau khi gộp, khoảng thời gian do dải chung của màn quyết định, xem chương 4 Q5.
5. ~~**"Năng lực theo chủ đề"** — biểu đồ radar 6 trục, thang 0-100; prototype chú thích "dựa trên acceptance rate và độ khó" (dòng 160-184) nhưng **công thức đã chốt 2026-10-18 không có trọng số độ khó** (70% tỉ lệ AC + 30% tỉ lệ bài đã giải), nên chú thích không nhắc độ khó. Là suy luận hiển thị, không phải dữ liệu mới — xem Câu hỏi mở Q2.~~ — **BỎ KHỐI 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): radar nói lại điều bảng "Theo chủ đề" và lời chào đã nói. Công thức điểm năng lực **giữ** vì lời chào (REQ-04) vẫn dùng để chọn chủ đề yếu nhất; chỉ không còn vẽ radar.
6. ~~**"Hoạt động 12 tháng"** — lưới 52 tuần x 7 ngày, 5 mức đậm nhạt, kèm chú giải Ít/Nhiều và dòng tóm tắt số ngày có bài nộp (dòng 188-213). Cùng dữ liệu F1-07, trình bày theo năm.~~ — **BỎ KHỐI 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): câu chuyện hoạt động do biểu đồ "Bài nộp theo ngày" (mục 4) kể. Dữ liệu lịch hoạt động (F1 #55) giữ nguyên, chỉ không còn khối hiển thị.
7. **"Bài toán gợi ý"** — ~~bảng bài toán kèm bộ lọc độ khó và chip chủ đề, mỗi dòng có dấu trạng thái đã giải/đang làm/chưa làm, mã bài, nhãn `function`, chủ đề, độ khó, AC rate (dòng 220-265)~~ **(2026-11-23, `DEC-2026-1123-student-dashboard-trim`)** bảng chỉ gồm bài **chưa giải**, tối đa **5 dòng**, không có tab độ khó và chip chủ đề, có liên kết "Tất cả" tới danh sách bài toán. Thứ tự xếp hạng do máy chủ trả (F2 #12), màn chỉ bỏ các dòng đã giải rồi lấy 5 dòng đầu. Đọc từ `problem-bank` (F2); quy tắc xếp hạng là suy luận — xem Câu hỏi mở Q2.
8. ~~**"Tiến độ theo chủ đề"** — 6 thanh tiến độ rút gọn (dòng 271-285)~~ — **thay bằng bảng "Theo chủ đề" đầy đủ** của `USR0501` (`09-layoutBase/Tiến độ của tôi.dc.html`:115-149) khi gộp hai màn: bảng có thêm tỉ lệ AC và lần nộp cuối cho từng chủ đề, nói nhiều hơn hẳn thanh rút gọn.
9. **"Mock Interview" gần đây** — 3 phiên, mỗi phiên có tên bài, điểm tổng, ~~4 thanh rubric~~, giai đoạn và ngày, kèm liên kết "Tất cả" (dòng 289-313). Khớp F1-08. **Sửa 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): ~~dải tóm tắt (số phiên, điểm trung bình, tiêu chí yếu nhất)~~ bỏ vì thẻ thống kê đã có số phiên và điểm trung bình; mỗi phiên là **một dòng** gồm điểm tổng (thang 0 đến 10 theo chủ dự án, xem Q6) và **tiêu chí yếu nhất của chính phiên đó**, không còn thanh theo tiêu chí.
10. **"Solution Review" gần đây** — 3 báo cáo, mỗi dòng có tên bài, ngày, độ phức tạp, kèm liên kết "Tất cả" (dòng 317-339). Đọc từ `ai-review` (F5.1).
11. **Chân trang** (dòng 342 trở đi) — đã dựng, nhưng theo đặc tả chung của khu ở `02-bd/screens/users/_shell.md` mục 3 (ba khu: thương hiệu + bản quyền · trạng thái cụm · số phiên bản), không theo bố cục 3 cột của riêng prototype này. Câu hỏi mở Q3 đã chốt: chân trang giữ câu trạng thái tĩnh, không có số hàng đợi. **Bổ sung 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): chân trang chỉ một dòng, vùng nội dung của khu cuộn theo trang và chân trang đi theo nội dung (không ghim); chi tiết ở `02-bd/screens/users/_shell.md`.
12. **Tỉ lệ hoàn thành ôn câu hỏi theo chủ đề** (F6-10) — bảng phụ dưới bảng "Theo chủ đề": mỗi chủ đề câu hỏi phỏng vấn có số câu đã thử trên tổng câu và ngày luyện gần nhất. Không có trong prototype tĩnh; thêm theo quyết định của chủ dự án 2026-10-18 (`01-rd/req/F6_interview-bank.md:26-27`).

Ngoài ra, ba khối lấy từ `09-layoutBase/Tiến độ của tôi.dc.html` khi gộp `USR0501`: bảng **"Theo chủ đề"** (dòng 115-149), khối **"Theo độ khó"** (dòng 166-181), ~~khối **"Nên ưu tiên"** (dòng 184-196)~~ — **khối "Nên ưu tiên" BỎ 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): nó nêu một chủ đề yếu nhất khác với lời chào. Chủ đề yếu do lời chào và bảng "Theo chủ đề" kể.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0601` / `dashboard` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/F1_identity.md` (F1-31) | Nguồn SoT của màn |
| Màn đã gộp vào đây | `01-rd/screens/users/USR0501_my_progress.md` | Hồ sơ gốc của REQ-01 tới REQ-06 đã chuyển sang màn này |
| Quyết định mở màn | `.nexa/control/decision-registry.md` — `DEC-2026-0927-student-dashboard-home` | Lý do màn này tồn tại |
| Prototype | `09-layoutBase/Dashboard AlgoPrep.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
|---|---|---|
| `09-layoutBase/Dashboard AlgoPrep.dc.html` | Prototype đối chiếu bố cục và trạng thái màn | [SoT: 09-layoutBase/Dashboard AlgoPrep.dc.html] |
| Bounded Context `identity` (F1) | BC sở hữu dữ liệu và logic đứng sau màn | [SoT: 01-rd/req/F1_identity.md — F1-31] |
| `problem-bank` (F2), `judge-orchestration` (F4), `ai-review` (F5) | BC đọc thêm cho khối gợi ý bài, hoạt động, báo cáo gần đây | [SoT: 09-layoutBase/Dashboard AlgoPrep.dc.html:220-339] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/USR0601_dashboard.md`, chưa viết).
- Hợp đồng API (công thức thang năng lực, ~~dữ liệu lưới 12 tháng~~ (khối đã bỏ 2026-11-23), quy tắc xếp hạng bài gợi ý) — thuộc DD (`03-dd/api/F1_identity.md`, chưa viết).
- Định nghĩa "streak" — đã chốt ở `01-rd/screens/users/USR0501_my_progress.md` Q1, màn này dùng lại, không định nghĩa lại.
- ~~Bảng chủ đề đầy đủ, khối "Theo độ khó", khối "Nên ưu tiên" — thuộc `USR0501_my_progress`~~ — **không còn ngoài phạm vi** kể từ 2026-09-27: `USR0501` đã gộp vào màn này, ba khối đó nay thuộc màn này.

---

## 4. Tiền đề và ràng buộc

> Prototype này là bản dựng tham khảo; chủ dự án yêu cầu tự chốt phần suy luận thấp rủi ro thay vì để mở, phần thuần UI đánh dấu **[Đợi nextjs]** để dựng lại đúng khi có frontend Next.js thật.

| # | Câu hỏi | Quyết định | Ghi chú |
|---|---|---|---|
| Q1 | Nút **"Xem lộ trình"** (dòng 105) không có đích nào trong prototype và không có mã `Fx-nn` nào mô tả tính năng lộ trình học. | **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất: bỏ.** "Lộ trình học" là một tính năng riêng chưa từng được đặc tả ở bất kỳ tài liệu `01-rd/req/` nào, không phải chi tiết UI của màn này. Bản dựng đã không có nút này thay vì tạo một liên kết chết. Tiêu chí ponytail bậc 1, 2. | `DEC-2026-1018-usr-open-questions-phase1-scope` mục (4). Nếu sau này muốn có lộ trình học, đó là phạm vi F1/F2 mới, cần quyết định riêng. |
| Q2 | **Thang "Năng lực theo chủ đề" (0-100)** và **quy tắc xếp hạng "Bài toán gợi ý"** không có mã `Fx-nn` riêng. | **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** Không cần mã mới — cả hai là suy luận hiển thị trên dữ liệu đã có (F1-06 tỉ lệ đã giải, F1-07 tỉ lệ AC), **không gọi AI**, cùng nguyên tắc đã chốt cho khối "Nên ưu tiên" ở `USR0501` Q2. **Năng lực theo chủ đề = 70% tỉ lệ AC + 30% tỉ lệ bài đã giải của chủ đề, không có trọng số độ khó** (độ khó là danh mục ADMIN quản lý, không gắn logic); bài gợi ý = ưu tiên chủ đề có điểm năng lực thấp nhất, trong đó ưu tiên bài chưa giải. Tiêu chí ponytail bậc 5. **Ghi chú đóng 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): radar đã bỏ nên vế "thang Năng lực theo chủ đề" chỉ còn dùng cho **lời chào** (REQ-04) để chọn chủ đề yếu nhất; công thức giữ nguyên. Vế xếp hạng bài gợi ý giữ nguyên ở máy chủ, màn chỉ lấy tối đa 5 bài chưa giải. | `DEC-2026-1018-usr-open-questions-phase2-5` mục PHASE 5. Thay cho câu cũ "điều chỉnh theo độ khó bài đã giải"; BD ghi chi tiết ở `02-bd/screens/users/USR0601_dashboard.md` Câu hỏi mở Q1. |
| Q3 | Khu Người học có chân trang chung không? Chỉ 2/12 prototype có. | **ĐÃ CÓ SẴN CÂU TRẢ LỜI, không phải câu hỏi mở** — `02-bd/screens/users/_shell.md` mục 3 đã chốt từ 2026-09-21: chân trang áp cho **mọi màn** của khu, và chính BD đó ghi rõ đây là quyết định về tính nhất quán chứ không phải kết luận rút từ prototype. Một đợt dựng trước ghi nhầm thành câu hỏi mở vì chỉ đọc prototype mà không đọc BD. Đã dựng 2026-09-27. **Phần số "hàng đợi {n} bài" động: ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất — không hiển thị, chân trang giữ câu trạng thái cụm tĩnh.** Tiêu chí ponytail bậc 5. | `DEC-2026-1018-usr-open-questions-phase2-5` mục PHASE 3. Trước đó `_shell.md` mục 6 Q3 để mở chờ `03-dd/api/F4_judge-orchestration.md`. |
| Q4 | Màn này và `USR0501_my_progress` trùng nhau bao nhiêu, và ranh giới ở đâu? | **ĐÃ ĐÓNG BẰNG CÁCH GỘP, cùng ngày** (`DEC-2026-0927-student-area-merge-and-shared-shell`): không còn ranh giới nào để vạch vì không còn hai màn. Ranh giới "làm gì tiếp theo" / "tôi đang ở đâu" chốt vài giờ trước đó trong `DEC-2026-0927-student-dashboard-home` là lịch sử. | Đối chiếu trực tiếp hai file prototype cho thấy khoảng 60% khối trùng nhau. Chủ dự án ban đầu chọn tách hai màn, sau khi thấy cả hai đã dựng thì chọn gộp — về đúng phương án đã được khuyến nghị từ đầu. |
| Q6 | **[MỞ, chủ sở hữu: chủ dự án; ưu tiên: Trung bình]** Thang điểm tổng của một phiên Mock Interview ở dòng "Mock Interview gần đây": chủ dự án nêu "0 đến 10" (`DEC-2026-1123-student-dashboard-trim`), còn thẻ thống kê ở mục 3 chương 2 ghi rubric trung bình `3.6 / 5` (prototype dòng 111-125). | Chưa chốt: hai nơi trên cùng màn có thể dùng hai thang khác nhau. RD này không tự suy luận thang. | Cần chủ dự án xác nhận một thang cho cả thẻ và dòng phiên; đối chiếu `01-rd/req/F1_identity.md` F1-08 và F5 rubric trước khi chốt. `[SoT: Suy luận]` |
| Q5 | **[ĐÃ CHỐT 2026-10-18 (owner)]** Dải chọn khoảng thời gian: dashboard có tab riêng cho biểu đồ (7/30/90 ngày), `my_progress` có tab áp cho cả màn (7 ngày/30 ngày/Tất cả). Sau khi gộp thì theo cái nào? | **Chốt: một dải duy nhất áp cho cả màn**, đặt ở hàng lời chào, dùng bộ giá trị của `my_progress` (7 ngày / 30 ngày / Tất cả). Nó điều khiển biểu đồ "Bài nộp theo ngày", bảng "Theo chủ đề" và ~~khối "Nên ưu tiên"~~ (khối đã bỏ 2026-11-23, `DEC-2026-1123-student-dashboard-trim`). Điều này **sửa REQ-05** bên dưới, vốn viết khi biểu đồ còn có tab riêng. | Hai dải với hai bộ giá trị khác nhau trên cùng một trang sẽ khiến "khoảng đang chọn" có hai nghĩa. `[SoT: Suy luận]` |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Dashboard tổng quan luyện tập của người học, gồm các khối ở chương 2 (F1-31). **Từ 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`) các khối là: lời chào kèm dải khoảng thời gian, bốn thẻ thống kê, "Bài nộp theo ngày", "Theo độ khó", "Theo chủ đề", "Ôn câu hỏi theo chủ đề" (F6-10), "Bài toán gợi ý", "Mock Interview", "Solution Review"; ~~radar "Năng lực theo chủ đề", "Nên ưu tiên", "Hoạt động 12 tháng"~~ đã bỏ | Chức năng | `01-rd/req/F1_identity.md` — F1-31 |
| REQ-02 | **Cho** tôi là `STUDENT` và đăng nhập thành công không đến từ một liên kết cụ thể, **Khi** hệ thống điều hướng sau đăng nhập, **Thì** tôi vào thẳng màn `dashboard`, không phải `my_progress` | Chức năng (GWT) | `DEC-2026-0927-student-dashboard-home`; sửa một phần `01-rd/screens/shared/SHR0101_auth.md` Q3 |
| REQ-03 | **Cho** tôi có ít nhất một bản nháp chưa nộp trong Workspace, **Khi** tôi mở `dashboard`, **Thì** nút "Tiếp tục bài đang làm" dẫn thẳng về bản nháp gần nhất; **nếu không có bản nháp nào**, nút đó dẫn về danh sách bài toán | Chức năng (GWT) | [SoT: `09-layoutBase/Dashboard AlgoPrep.dc.html`:106] |
| REQ-04 | **Cho** tôi đã nộp bài ở nhiều chủ đề, **Khi** tôi mở `dashboard`, **Thì** dòng dưới lời chào nêu đúng chủ đề có điểm năng lực thấp nhất kèm tỉ lệ AC của chủ đề đó | Chức năng (GWT) | [SoT: `09-layoutBase/Dashboard AlgoPrep.dc.html`:102-103] |
| REQ-05 | ~~**Cho** tôi đang ở màn Tổng quan, **Khi** tôi đổi dải khoảng thời gian ở hàng lời chào (7 ngày / 30 ngày / Tất cả), **Thì** biểu đồ "Bài nộp theo ngày", cột "Tỉ lệ AC" và "Lần cuối" của bảng "Theo chủ đề", khối "Nên ưu tiên" và khối "Mock Interview" gần đây cùng tính lại theo khoảng đó; các khối luỹ kế (4 thẻ chỉ số, năng lực theo chủ đề, cột "Đã giải", "Theo độ khó", "Bài toán gợi ý", "Hoạt động 12 tháng", bảng phụ F6-10) **không** đổi.~~ **(Viết lại 2026-11-23, `DEC-2026-1123-student-dashboard-trim`)** **Cho** tôi đang ở màn Tổng quan, **Khi** tôi đổi dải khoảng thời gian ở hàng lời chào (7 ngày / 30 ngày / Tất cả), **Thì** biểu đồ "Bài nộp theo ngày", cột "Tỉ lệ AC" và "Lần cuối" của bảng "Theo chủ đề" và khối "Mock Interview" gần đây cùng tính lại theo khoảng đó; các khối luỹ kế (4 thẻ thống kê, cột "Đã giải", "Theo độ khó", "Bài toán gợi ý", bảng phụ F6-10) **không** đổi. Biểu đồ vẽ 7 cột cho "7 ngày", 14 cột cho "30 ngày", gom theo tuần cho "Tất cả" (chốt 2026-10-18) | Chức năng (GWT) | Chương 4 Q5; `01-rd/screens/users/USR0501_my_progress.md` REQ-06 |
| REQ-09 | **Cho** tôi mở màn ở khung hẹp (dưới ngưỡng `lg`), **Khi** thanh điều hướng không đủ chỗ, **Thì** 6 mục nav gom vào một nút menu, thương hiệu và khối người dùng vẫn nằm trên thanh | Chức năng (GWT) | `02-bd/screens/users/_shell.md` mục 6 Q5 |
| REQ-10 | **Cho** tôi ở bất kỳ màn nào của khu Người học, **Khi** tôi cuộn xuống cuối trang, **Thì** tôi thấy chân trang chung: thương hiệu + bản quyền, trạng thái cụm go-judge, số phiên bản. **Bổ sung 2026-11-23** (`DEC-2026-1123-student-dashboard-trim`): chân trang chỉ một dòng, trang cuộn theo cách mặc định và chân trang đi theo nội dung, không ghim | Chức năng (GWT) | `02-bd/screens/users/_shell.md` mục 3 |
| REQ-06 | ~~**Cho** tôi đang xem "Bài toán gợi ý", **Khi** tôi chọn một độ khó hoặc một chip chủ đề, **Thì** bảng lọc lại theo lựa chọn đó và số lượng bài hiển thị ở góc phải tiêu đề cập nhật theo~~ **(Viết lại 2026-11-23: bộ lọc không còn)** **Cho** hệ thống đã xếp hạng bài gợi ý cho tôi và trong đó có bài tôi chưa giải, **Khi** tôi mở `dashboard`, **Thì** khối "Bài toán gợi ý" hiện tối đa 5 bài chưa giải theo đúng thứ tự máy chủ trả, không có tab độ khó và chip chủ đề, và có liên kết "Tất cả" dẫn tới danh sách bài toán; **nếu** không còn bài chưa giải nào thì khối hiện trạng thái rỗng riêng (REQ-08), không hiện bài đã giải | Chức năng (GWT) | `DEC-2026-1123-student-dashboard-trim`; ~~[SoT: `09-layoutBase/Dashboard AlgoPrep.dc.html`:221-233]~~ |
| REQ-07 | **Cho** phân hệ AI (F5) hỏng hoặc hết hạn mức, **Khi** tôi mở `dashboard`, **Thì** hai khối "Mock Interview" và "Solution Review" hiện trạng thái lỗi riêng của chúng, và **mọi khối còn lại vẫn hiển thị bình thường** | Chức năng (GWT) | `README.md` — nguyên tắc phân hệ AI suy giảm êm; cùng nguyên tắc 3 trạng thái theo khối của F1-29/F1-30 |
| REQ-08 | **Cho** tôi là người dùng mới chưa nộp bài nào, **Khi** tôi mở `dashboard`, **Thì** từng khối hiện trạng thái rỗng riêng của nó thay vì số 0 hoặc biểu đồ trống không giải thích | Chức năng (GWT) | `01-rd/req/F1_identity.md` — F1-31, gạch đầu dòng cuối |
| REQ-11 | **Cho** tôi đã luyện (hoặc chưa luyện) câu hỏi phỏng vấn ở nhiều chủ đề, **Khi** tôi mở `dashboard`, **Thì** dưới bảng "Theo chủ đề" tôi thấy, với từng chủ đề câu hỏi, số câu đã thử trên tổng câu và ngày luyện gần nhất; chủ đề chưa luyện câu nào vẫn hiện với `0 / tổng`. Bảng này luỹ kế, không đổi theo dải khoảng thời gian | Chức năng (GWT) | `01-rd/req/F6_interview-bank.md:26-27` — F6-10; `DEC-2026-1018-usr-open-questions-phase2-5` (mục FOLLOW-UPS) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt. Màn không sở hữu dữ liệu mới nào — mọi khối đều đọc lại dữ liệu đã có của F1-06/F1-07/F1-08, `problem-bank` và `ai-review`.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Thực hiện | Rà soát | Trạng thái |
|---|---|---|---|
| RD màn `dashboard` | Nhóm phát triển | Chủ dự án | Có tài liệu này |
| BD màn `dashboard` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết — ghi nợ ở `06-plan/PROTOTYPE_DEBT.md` mục 11 |
| DD API `identity` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết |

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
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-31 (nền: F1-06, F1-07, F1-08); `01-rd/req/F6_interview-bank.md` — F6-10 |
| Quyết định | `.nexa/control/decision-registry.md` — `DEC-2026-0927-student-dashboard-home`; `DEC-2026-1018-usr-open-questions-phase1-scope`, `DEC-2026-1018-usr-open-questions-phase2-5`; `DEC-2026-1123-student-dashboard-trim` |
| Màn anh em | `01-rd/screens/users/USR0501_my_progress.md` (ranh giới nội dung), `01-rd/screens/shared/SHR0101_auth.md` (Q3, đích sau đăng nhập) |
| Prototype | `09-layoutBase/Dashboard AlgoPrep.dc.html` |
