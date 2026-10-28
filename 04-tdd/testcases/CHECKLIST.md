# Checklist tiến độ testcase QA

> Cập nhật 2026-10-28. [Kế hoạch và quy trình](README.md) · [Biên bản audit đợt 1](AUDIT_DOT1.md) · [Tổng quan TDD](../overview.md) · [Mục lục testcase theo module](modules/README.md). **Mốc thiết kế đợt 1 đóng có điều kiện**: AC06-02 chờ golden sau codegen; chưa có ca nào được chạy.

Audit bằng skill ngày 2026-10-28: **1.107 ca** (895 module + 212 màn), đã review các cụm còn mở của đợt 2–4 và bộ chọn đợt 5. Đợt 2/4 đóng thiết kế; đợt 3 còn đúng **một policy mảng AI vượt 10 phần tử**, ảnh hưởng hai ca. F3 golden và mọi cổng thực thi vẫn chờ coding/build. [Baseline nguồn đã audit](AUDIT_SOURCE_BASELINE.json) giúp lần sau chỉ review lại phần thay đổi và phạm vi phụ thuộc.

[Kết quả kiểm cấu trúc](AUDIT_STRUCTURE_20261028.json): 90 file cụm, 1.107 ID/1.107 trace, 0 lỗi cấu trúc; 2 semantic flags đúng hai ca chờ policy. Kiểm 168 file Markdown không có link đích gãy. Đây là kiểm tài liệu; chưa chạy ứng dụng.

## Đợt 1 — F2/F3/F4

- [x] Chia testcase theo module và cụm, có mục lục dẫn tới từng file.
- [x] Lập 476 ca: [F2 158 ca](modules/F2_problem-bank/README.md), [F3 145 ca](modules/F3_harness/README.md), [F4 173 ca](modules/F4_judge-orchestration/README.md).
- [x] Gắn ít nhất một ID testcase cho 221/221 AC: F2 94/94, F3 50/50, F4 77/77.
- [x] Kiểm ID không trùng, liên kết tài liệu không gãy, không có AC đợt 1 thiếu trace.
- [x] Audit hai chiều AC/DD ↔ testcase trong phạm vi thiết kế đợt 1; bổ sung các nhánh còn thiếu AC57/59 F2 và AC10/12 F4. Kiểm nguồn testcase phủ 221/221 AC, 476 ID duy nhất; [biên bản](AUDIT_DOT1.md) nêu ngoại lệ golden và ID cũ.
- [x] Audit cụm F3 P1/P2 về quyền và từ chối Wrapper: thêm ca P2-T03 và bài đã xoá của P1-T08; [trace F3](../modules/F3_harness.md). Các cụm còn lại tiếp tục audit.
- [x] Audit cụm F3 P3 theo DD §6.1 và AC-14–22: thêm ca stderr không parse được và ca trần 10 diagnostic; tách kết quả độc lập. Note C++ đã có oracle riêng trong DTO.
- [x] Audit F3 P4/P5 theo DD logic §7 và validation Bảng 1–3: thêm 13 ca cho dòng trống, ARRAY hai chiều, `NaN`/`Infinity`, `HRN-050/051`, `HRN-V06/V10/V15/V16`, keyword/override dành riêng và tổ hợp suy biến; đồng bộ AC-24/25/27/30/32/35/36/37.
- [x] Cố định kịch bản DD F3 P2-T01 và [handoff sáu golden](modules/F3_harness/F3_P2_golden_handoff.md) cho `TC-F3-AC06-02`; việc tạo tệp/hash và phê duyệt so byte nằm ở cổng sau codegen bên dưới.
- [x] Chốt 6 oracle dưới đây ngày 2026-10-27 và sửa DD/TDD/testcase liên quan.
- [x] Review cuối trường fixture, thao tác, expected, cleanup và evidence: 476/476 ca có đủ ô bắt buộc; 475 ca được đánh dấu đã review đầy đủ, AC06-02 review kịch bản với oracle byte chờ golden. Đóng mốc thiết kế **có điều kiện**, không xem là kết quả kiểm thử.
- [x] Cụ thể hoá [TC-F2-AC86-01](modules/F2_problem-bank/F2_ports_privacy_cache.md): 29 route ghi, body/fixture hợp lệ từng route, actor thiếu/đủ đúng `Function:Action`, `403 PRB-002` và đối chứng 2xx trên cùng subject.
- [x] Đối chiếu F2 AC-84 với DD: thêm nhánh bài `deleted` vẫn có summary/assignment lịch sử và ca riêng cho quan hệ chủ đề lịch sử so với tiến độ hiện hành.
- [x] Cố định F2 AC-85: liệt kê đủ 15 route học viên ở DD Bảng 0.1, gồm PUT/DELETE bookmark và #13A/#13B; quét DTO/INFO log bằng marker Hidden.
- [x] Bổ sung ô `Test data` cho 35 ca F2 còn thiếu (9 AI, 12 giao bài, 10 cổng/quyền/cache, 4 hạn mức/tiến độ); quét cấu trúc không còn ca thiếu ô. Độ cụ thể của từng fixture vẫn nằm trong bước review P0/P1.
- [x] Thêm 7 ca cuối: F2 AC57/59 cho hạ duyệt/xoá nháp/xoá lặp/ngưỡng/MinIO; F4 AC10/12 cho ba Sample/giữ nháp khi timeout. Đồng bộ trace và mục lục.

### Cổng sau coding — chưa thực thi

- [ ] Tạo, review và khoá sáu golden `PreparedProgram` cùng SHA-256 theo [handoff P2](modules/F3_harness/F3_P2_golden_handoff.md); sau đó chạy so byte cho AC06-02.

- [ ] Chạy test tích hợp khi backend/hạ tầng liên quan sẵn sàng; lưu run record theo build, môi trường và dữ liệu.

### Oracle đã chốt ngày 2026-10-27

Sáu quyết định có `decision` và `rationale` trong [decision registry](../../.nexa/control/decision-registry.md) (`DEC-2026-1027-qa-*`).

- [x] F3 preview body vượt 16 KiB: `413 HRN-062`.
- [x] F2 giao bài khi ghi Outbox lỗi: rollback riêng bài, lô `200`, `failedItems[].code=PRB-056`.
- [x] F3 AC-45: adapter chặn ngôn ngữ lạ với `HRN-010` trước enum; giữ chữ ký P2/P3.
- [x] F3 AC-05: kiểm thiếu chữ ký trước cờ hỗ trợ, trả `HRN-005`.
- [x] F3 AC-22: note C++ thuộc mã người học thành phần tử riêng trong `diagnostics[]`.
- [x] F4 AC-39: `dailyCounts[].day` của nhóm tuần `all` là thứ Hai đầu tuần UTC+7.

## Các đợt tiếp theo — checklist thiết kế và thực thi

Quy ước: mỗi mục chỉ đánh dấu xong khi có tài liệu hoặc run record kiểm được; số AC là mốc truy vết từ [tổng quan TDD](../overview.md), **không phải chỉ tiêu số testcase**. Thiết kế đợt 2–4 có thể làm trước coding; kết quả chạy chỉ ghi khi có build, môi trường và bằng chứng. Giữ `Chưa thực hiện` cho ca chưa chạy.

### Đợt 2 — F1 định danh, lớp và quyền (80 AC)

Nguồn chính: [TDD F1](../modules/F1_identity.md), DD F1 API/logic/validation/jobs và DD màn liên quan. Đầu ra đặt ở `modules/F1_identity/`, chia file theo cụm rủi ro, có `README.md` dẫn đến từng cụm.

Tiến độ: **đóng audit thiết kế 165 ca trong 11 cụm, 80/80 AC có trace, 0 ca thực thi**. [Mục lục F1](modules/F1_identity/README.md) · [audit đợt 2](AUDIT_DOT2_F1.md) · [inventory nhánh nền tảng](modules/F1_identity/F1_foundation_branch_inventory.md). Giới hạn hợp đồng và cổng sau coding ghi trong biên bản.

Rà soát 2026-10-28: AC-25 chốt `403 IDT-445` cho thiếu quyền; AC-26/27 chốt `422 IDT-421`/`422 IDT-422`, xoá role thành công `204` cùng audit `ROLE_DELETED`, audit lỗi rollback `503 IDT-446`. Owner chốt thêm `422 IDT-448` cho mở lại tài khoản đã ẩn danh. DD/TDD/testcase và decision registry đã đồng bộ; review 11 cụm hoàn tất theo [biên bản](AUDIT_DOT2_F1.md).

- [x] Làm [audit mẫu AC-25–27](AUDIT_SAMPLE_F1_RBAC_AC25_27.md): 6 ca, 3 AC, rà nhánh và oracle; chốt cơ chế `permission_version` đọc PostgreSQL primary để bảo đảm quyền mới ngay sau PATCH, kể cả Redis chậm. Tạo [skill audit theo cụm](../../.agents/skills/algoprep-qa-audit/SKILL.md) và chạy thử trên F1/F6; điều chỉnh skill theo hai lỗi phát hiện thực tế. Đây là mốc **mẫu quy trình**, chưa đóng audit đợt 2.
- [x] Áp skill cho [AC-28–32](AUDIT_F1_RBAC_AC28_32.md): 13 ca/5 AC, chốt `403 IDT-447` và `content` CSV theo locale, sửa oracle quyền actor và dữ liệu export. Sau đó audit đủ các cụm F1 còn lại, ghi [biên bản tổng](AUDIT_DOT2_F1.md).

- [x] Lập inventory 80 AC và các nhánh Normal/Abnormal/Boundary/Security/Concurrency/Recovery trong inventory nền tảng và bảng nhánh từng cụm; đối chiếu theo DD hiện hành.
- [x] Viết cụm **xác thực và phiên**: OTP đúng/sai/hết hạn, gửi lại và hạn mức, đăng nhập/đăng xuất, refresh, phiên hết hạn hoặc bị thu hồi; kiểm không phát hành phiên khi từ chối.
- [x] Viết cụm **hồ sơ và vòng đời tài khoản**: cập nhật hồ sơ/cài đặt, tự xoá, huỷ xoá, khôi phục và dữ liệu còn/không còn sau từng mốc.
- [x] Viết cụm **lớp học và phạm vi dữ liệu**: mã mời và giới hạn thử, vai trò trong lớp, học viên trùng lớp, rời/xoá lớp, quyền xem bài giao, tiến độ và thống kê qua ranh giới lớp.
- [x] Viết cụm **RBAC/quản trị/nhật ký**: ma trận quyền theo actor và hành động, truy cập chéo vai trò/chủ sở hữu, thay đổi quyền, audit log, thao tác bị từ chối không ghi dữ liệu hoặc phát sự kiện sai.
- [x] Review hai chiều 80/80 AC ↔ ca và các nhánh DD; sửa 12 phát hiện và thêm 5 ca recovery/job/SSE. Checker 165 ca/165 trace/0 lỗi; 212 link và 14 anchor F1 hợp lệ; cập nhật trace và mục lục.
- [x] Ghi [biên bản đóng thiết kế đợt 2](AUDIT_DOT2_F1.md): 165 ca/80 AC; owner chốt `422 IDT-448`, giữ giới hạn exact HTTP chưa được DD quy định cho DB fault AC54-03. Các oracle state/rollback/retry của ca đó đã xác định; chưa có run record.

### Đợt 3 — F5 AI và F6 ngân hàng phỏng vấn (99 AC: F5 58, F6 41)

Tiến độ: **đã audit 14/14 cụm**, [F5](modules/F5_ai-review/README.md) **144 ca/58 AC**, [F6](modules/F6_interview-bank/README.md) **110 ca/41 AC**. Checker 254 ID/254 trace, 0 lỗi cấu trúc; còn đúng một quyết định tràn mảng AI trên 10 phần tử, ảnh hưởng hai ca. **0 ca thực thi**; chưa đóng đợt 3 tới khi chốt policy này.

Rà soát 2026-10-28: đã đồng bộ `503 INB-503`, owner chốt thêm audit F5 `503 AIR-503` và baseline anomaly tính ADMIN trong trung bình nhưng không cảnh báo ADMIN. Sửa oracle quota/cache-event, forecast, race sweep, phân trang và sort aggregate; chi tiết cùng inventory từng file tại [biên bản đợt 3](AUDIT_DOT3_F5_F6.md).

Nguồn chính: [TDD F5](../modules/F5_ai-review.md), [TDD F6](../modules/F6_interview-bank.md) và DD API/logic/validation/jobs tương ứng. Đầu ra đặt ở `modules/F5_ai-review/` và `modules/F6_interview-bank/`, mỗi module có `README.md` theo cụm.

- [x] Lập [inventory 14 cụm/99 AC](AUDIT_DOT3_F5_F6.md), đối chiếu fixture/clock/job/AI stub; ghi riêng chất lượng AI thật và quyết định oracle còn mở.
- [x] F5: viết cụm **phân tích bài giải và tra cứu báo cáo** gồm dữ liệu gửi AI, quyền xem, cache/phiên bản, phản hồi sai cấu trúc, retry, hết ngân sách và nhà cung cấp lỗi; kiểm không lộ Hidden/mã hoặc dữ liệu ngoài quyền trong prompt/log.
- [x] F5: viết cụm **phỏng vấn giả lập** gồm tạo/tiếp tục/kết thúc phiên, trạng thái và giới hạn lượt, câu hỏi/phản hồi, timeout, retry và tác động khi AI suy giảm.
- [x] F5: viết cụm **quản trị cấu hình và hạn mức** gồm quyền đổi cấu hình, ngưỡng chi tiêu, đếm lượt, đồng thời, bất biến khi lỗi và đối chiếu sự kiện/job.
- [x] F6: viết cụm **duyệt/tìm/lọc/đánh dấu và quản trị câu hỏi** gồm phân trang, quyền xuất bản, sửa/xoá, dữ liệu cũ và không rò nội dung không được xem.
- [x] F6: viết cụm **học/tự chấm/luyện/tiến độ** gồm chuyển trạng thái, đáp án, làm lại, ngày/tuần ranh giới, đồng bộ và tính số liệu khi dữ liệu hoặc job thiếu.
- [x] Review hai chiều F5 58/58 và F6 41/41 AC ↔ ca; cập nhật trace/mục lục, đủ trường/ID/link; ghi [biên bản](AUDIT_DOT3_F5_F6.md) và hai ca boundary còn chờ policy.
- [ ] Chốt `QA-F5-F6-ARRAY-OVERFLOW-10`: AI trả 11 phần tử hợp lệ thì cắt còn 10 hay từ chối schema; cập nhật F5 validation §3, F6 validation §7, `TC-F5-AC01-04`/`TC-F6-AC10-02`, decision registry và đóng thiết kế đợt 3. Đã hỏi owner, chưa có trả lời.

### Đợt 4 — 40 màn, 152 AC (users 82, teacher 17, admin 29, shared 24)

Tiến độ: **đóng audit thiết kế đợt 4**, [mục lục màn](screens/README.md) có **212 ca/40 màn, 152/152 AC đã gắn trace TDD**: shared 53, users 95, teacher 28, admin 36. **0 ca thực thi**. [Biên bản](AUDIT_DOT4_SCREENS.md) · [inventory 40 màn](screens/AUDIT_INVENTORY.md).

Rà soát 2026-10-28: sửa expected lệch AC về Workspace, initial AI request, defaultLanguage/export, FAILED/history và snapshot ngôn ngữ; đối chiếu oracle quyền, CSV, signed upload, modal, lỗi và điều hướng. [Quy ước fixture](screens/FIXTURE_CONVENTIONS.md) xác định actor/UUID/clock/reset và ranh giới FE mock/tích hợp.

Nguồn chính: [ma trận màn TDD](../overview.md), TDD và DD từng màn. Đầu ra đặt ở `screens/{users,teacher,admin,shared}/`; testcase màn tham chiếu ca module khi kiểm cùng quy tắc máy chủ.

- [x] Lập [inventory 40 màn/152 AC](screens/AUDIT_INVENTORY.md), dẫn DD/case từng màn; fixture và ranh giới FE mock/tích hợp theo [quy ước](screens/FIXTURE_CONVENTIONS.md) và [hành trình F1–F6](PHASE5_SMOKE_E2E_SELECTION.md).
- [x] Viết nhóm **shared** (7 màn, 24 AC): xác thực, quản lý/biên soạn/thông tin bài và câu hỏi; tách lỗi từng bước, nháp, xác nhận, điều hướng và giữ dữ liệu khi bị từ chối.
- [x] Viết nhóm **users** (16 màn, 82 AC): tìm bài, workspace/nộp/xem kết quả, AI/phỏng vấn, ngân hàng ôn, hồ sơ, Dashboard và lớp; kiểm loading/rỗng/lỗi, quay lại, retry, trạng thái cũ và dữ liệu theo quyền.
- [x] Viết nhóm **teacher** (7 màn, 17 AC): quản lý lớp/bài giao/học viên/tiến độ/lượt nộp, trạng thái thiếu dữ liệu và ranh giới lớp.
- [x] Viết nhóm **admin** (10 màn, 29 AC): quyền/người dùng, AI, queue/log/sự kiện, ngôn ngữ; kiểm xác nhận thao tác, lỗi từng phần và quyền truy cập màn/hành động.
- [x] Đối chiếu trạng thái loading/rỗng/lỗi/thành công, validation, giữ nháp, điều hướng, phản hồi chậm và bàn phím khi DD yêu cầu; phân biệt quan sát FE và oracle máy chủ theo inventory.
- [x] Review hai chiều 152/152 AC ↔ ca và điểm kiểm DD; 212 case/212 trace, 0 lỗi cấu trúc. Đóng [biên bản thiết kế đợt 4](AUDIT_DOT4_SCREENS.md); các cổng còn lại là thực thi sau build/seed/fault/evidence.

### Đợt 5 — chọn bộ chạy, thực thi và nghiệm thu

Đợt này có thể **chuẩn bị bộ chọn** khi đợt 2–4 đang viết; kết quả thực thi cần build và hạ tầng thật. Không chuyển ca FE mock thành kết quả tích hợp/E2E.

- [x] Chọn [8 ca smoke P0](PHASE5_SMOKE_E2E_SELECTION.md) có ID testcase nguồn, fixture và oracle trọng yếu; bộ chọn còn là bản nháp thiết kế, chưa chạy.
- [x] Chọn [3 hành trình E2E](PHASE5_SMOKE_E2E_SELECTION.md) theo vai trò học viên/giảng viên/quản trị viên, có checkpoint, dữ liệu khởi tạo, cleanup và evidence; chưa chạy.
- [x] Lập [ma trận hồi quy bản thiết kế](PHASE5_REGRESSION_MATRIX.md): API/AC → ca module/màn/E2E, ưu tiên P0/P1. Khi có **thay đổi code**, bổ sung symbol/flow thực tế từ cổng impact/detect_changes của AGENTS.md trước khi chọn bộ chạy.
- [x] [Audit bộ chọn smoke/E2E](AUDIT_PHASE5_SELECTION.md): sửa fixture xuất bản 8 testcase, phân biệt fixture nguồn/tích hợp, thứ tự I/L và cleanup; F5 bắt buộc cho đủ 8 smoke, fault Outbox kiểm batch P3 lỗi/P4 thành công đúng phạm vi quyền. Chưa chạy.
- [ ] Chuẩn bị môi trường và dữ liệu có thể lặp lại: build/version, API/DB/event/job/MinIO/AI stub hoặc nhà cung cấp thật theo từng ca, clock, tài khoản/vai trò và reset; ghi rõ phần nào còn thiếu.
- [ ] Chạy FE mock sớm khi có màn chạy được; lưu run record riêng ghi build, mock/fixture, bước và bằng chứng. Không dùng kết quả này để kết luận quyền, DB, event hoặc job backend.
- [ ] Khi coding/hạ tầng tương ứng sẵn sàng, chạy smoke rồi integration/E2E/hồi quy; mỗi run record ghi môi trường, build, dữ liệu, kết quả thực tế, evidence, defect và lần chạy lại.
- [ ] Hoàn tất cổng golden F3 P2 và các cổng sau coding của đợt 1 ở trên; đối chiếu toàn bộ ca P0/P1 chưa chạy, fail, blocked và ngoại lệ được chấp nhận trước báo cáo nghiệm thu.
- [ ] Lập báo cáo cuối: độ phủ thiết kế và thực thi tách riêng, kết quả theo mức ưu tiên/môi trường, lỗi còn mở, rủi ro và quyết định nghiệm thu; không suy pass từ số testcase đã viết.
