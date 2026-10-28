# Kế hoạch lập test case QA — AlgoPrep

[Checklist tiến độ](CHECKLIST.md) · [Mục lục testcase theo module](modules/README.md)

> Trạng thái: **đợt 1 đã đóng mốc thiết kế có điều kiện**, có **476 ca thiết kế**, 6 oracle đã chốt ngày 2026-10-27; một đối chứng golden P2 chờ codegen, chưa có bằng chứng thực thi. [Biên bản audit đợt 1](AUDIT_DOT1.md). Nguồn quyết định: RD → BD → DD → TDD hiện hành; prototype chỉ dùng để quan sát giao diện đang có.

## 1. Thời điểm và các mốc

Thiết kế và review test case **bắt đầu được trước khi coding hoàn tất** khi AC cùng DD đã xác định được đầu vào, thao tác và kết quả Đạt/Trượt. Có thể kiểm sớm luồng giao diện với frontend và dữ liệu giả, nhưng kết quả đó chỉ chứng minh hành vi giao diện trong môi trường giả. Test API, DB, event, job, phân quyền máy chủ, đồng thời và E2E cần backend cùng hạ tầng tương ứng chạy thật. Chất lượng nội dung AI cần thêm đợt đánh giá thủ công khi nối nhà cung cấp thật.

| Mốc | Đầu ra | Điều kiện bắt đầu | Điều kiện hoàn tất |
| :--- | :--- | :--- | :--- |
| Thiết kế | Test case có mã ổn định, dữ liệu và oracle cụ thể | AC và DD nguồn rõ | Review hai chiều AC ↔ nhánh test case; chưa ghi Đạt/Trượt |
| Kiểm FE sớm | Bằng chứng màn với mock/fixture | Màn tương ứng chạy được | Ghi rõ môi trường mock; không dùng làm bằng chứng hợp đồng backend |
| Kiểm tích hợp | Bằng chứng UI/API/DB/event/job theo ca | Dịch vụ và hạ tầng liên quan chạy thật | Ghi bản build, môi trường, dữ liệu, kết quả thực tế và lỗi nếu có |
| Nghiệm thu | Báo cáo độ phủ và rủi ro còn lại | Các ca ưu tiên đã thực thi | Không còn ca P0/P1 chưa xử lý hoặc có ngoại lệ được chủ dự án chấp nhận |

## 2. Cấu trúc và thứ tự triển khai

Đặt ca máy chủ trong `modules/<mã module>_<tên module>/`, chia tiếp thành từng file theo cụm rủi ro; mỗi thư mục có `README.md` để tra cứu. Ca màn nằm trong `screens/{users,teacher,admin,shared}/`. [Mục lục module](modules/README.md) dẫn tới các cụm đã có. `04-tdd/overview.md` giữ ma trận độ phủ và tiến độ; cột **Test case** của AC nhận mã sau khi ca được review.

1. **Đợt 1 — F2/F3/F4:** snapshot testcase, phiên bản bộ chấm, lỗi đặc tả, verdict/điểm, rò Hidden/stderr, Run Code, hàng đợi và worker. Làm một cụm mẫu để thống nhất định dạng trước khi nhân rộng.
2. **Đợt 2 — F1 và quyền:** OTP, phiên, tài khoản tự xoá/khôi phục, ma trận quyền, lớp, phạm vi dữ liệu và thống kê.
3. **Đợt 3 — F5/F6:** ngân sách, cache, phiên phỏng vấn, prompt injection, phản hồi sai cấu trúc, suy giảm AI và tiến độ ôn.
4. **Đợt 4 — màn hình:** đi theo luồng người dùng qua nhiều module; kiểm loading/rỗng/lỗi, điều hướng, giữ nháp, retry, phản hồi chậm và khả năng dùng bằng bàn phím khi DD yêu cầu. Tái sử dụng fixture của module thay vì lặp lại oracle máy chủ.
5. **Đợt 5 — E2E và hồi quy:** các hành trình học viên/giảng viên/quản trị viên qua frontend, backend, cơ sở dữ liệu và hạ tầng thật; chọn bộ smoke và hồi quy theo rủi ro từ các ca đã có.

### Tiến độ thực tế (2026-10-28)

| Đợt | Tình trạng | Bằng chứng thiết kế | Thực thi |
| :--- | :--- | :--- | :--- |
| 1 — F2/F3/F4 | Đóng thiết kế có điều kiện | 476 ca trong 40 file cụm; 221/221 AC có nguồn testcase: F2 94/94, F3 50/50, F4 77/77. AC06-02 còn đối chứng byte golden phụ thuộc codegen. | 0 ca có run record |
| 2 — F1 và quyền | Đóng audit thiết kế | [165 ca trong 11 cụm](modules/F1_identity/README.md); 80/80 AC có trace; [biên bản](AUDIT_DOT2_F1.md) | Chưa chạy |
| 3 — F5/F6 | Đã audit 14 cụm; chờ một policy schema | [F5 144 ca/58 AC](modules/F5_ai-review/README.md); [F6 110 ca/41 AC](modules/F6_interview-bank/README.md). Hai ca 11 phần tử chờ cắt/từ chối; [biên bản](AUDIT_DOT3_F5_F6.md) | Chưa chạy |
| 4 — Màn hình | Đóng audit thiết kế | [212 ca/40 màn, 152/152 AC](screens/README.md); [inventory](screens/AUDIT_INVENTORY.md), [biên bản](AUDIT_DOT4_SCREENS.md) | Chưa chạy |
| 5 — E2E và hồi quy | Đã audit bộ chọn thiết kế | [8 smoke P0 và 3 hành trình E2E](PHASE5_SMOKE_E2E_SELECTION.md); [audit fixture/dependency](AUDIT_PHASE5_SELECTION.md); [ma trận hồi quy](PHASE5_REGRESSION_MATRIX.md); [mẫu run record](PHASE5_RUN_RECORD_TEMPLATE.md) | Chưa chạy |

Tổng hiện tại: **1.107 ID testcase thiết kế duy nhất** (895 ca module F1–F6 và 212 ca màn); **0 run record**. Đợt audit này bổ sung 20 ca so với mốc 1.087. Hai ca boundary còn policy chưa chốt; độ phủ/trace không thay thế kết quả chạy.

Audit theo cụm dùng [skill QA audit](../../.agents/skills/algoprep-qa-audit/SKILL.md); [mẫu AC-25–27 F1](AUDIT_SAMPLE_F1_RBAC_AC25_27.md) ghi cách kiểm và điều chỉnh skill sau khi phát hiện race phân quyền.

Các cụm đợt 1 được quản lý theo module: [F2 problem-bank](modules/F2_problem-bank/README.md) có 158 ca trong 13 cụm; [F3 harness](modules/F3_harness/README.md) có 145 ca trong 11 cụm; [F4 judge-orchestration](modules/F4_judge-orchestration/README.md) có 173 ca trong 16 cụm. Đợt 2 có [F1 identity](modules/F1_identity/README.md): 165 ca trong 11 cụm. Đợt 3 có [F5](modules/F5_ai-review/README.md) 144 ca và [F6](modules/F6_interview-bank/README.md) 110 ca. Đợt 4 có [mục lục 40 màn](screens/README.md) với 212 ca. Đợt 2/4 đã đóng audit; đợt 3 chỉ còn policy tràn mảng AI. Chưa có run record.

**Các cổng tiếp theo:** [audit F1](AUDIT_DOT2_F1.md), [audit F5/F6](AUDIT_DOT3_F5_F6.md), [audit màn](AUDIT_DOT4_SCREENS.md) và [checklist](CHECKLIST.md) ghi các oracle/nhánh còn mở. F3 P2-T01 cần sáu golden sau khi codegen chạy được. [Bộ chọn smoke/E2E](PHASE5_SMOKE_E2E_SELECTION.md) đã có bản nháp; việc chạy backend/E2E đợi build và hạ tầng tương ứng. FE mock có thể kiểm sớm và lưu kết quả tách biệt.

## 3. Quy ước mỗi test case

- Mã ổn định theo dạng `TC-F2-AC93-01` hoặc `TC-SHR0202-PAAC02-01`; thêm mã mới, không đánh số lại mã đã dẫn ở TDD.
- Ghi AC và DD nguồn, mục tiêu/rủi ro, mức ưu tiên, môi trường cần có, tiền điều kiện, fixture với giá trị cụ thể, thao tác, kết quả mong đợi tại từng bề mặt quan sát được, cách dọn dữ liệu và bằng chứng cần lưu.
- Tách nhánh Normal, Abnormal, Boundary, Security, Concurrency và Recovery khi hợp đồng có. Một AC có thể có nhiều mã test case. Đặc biệt tách các AC gộp nhánh liệt kê ở `04-tdd/overview.md` mục 7.
- Kiểm cả tác động **không được xảy ra**: không ghi DB khi bị từ chối, không lộ Hidden/stderr, không tăng hạn mức hoặc số liệu sai, không phát tác động nghiệp vụ hai lần khi retry.
- Nếu AC/DD thiếu oracle, ghi câu hỏi cụ thể kèm nguồn xung đột; sửa nguồn trước khi phê duyệt test case. Không lấy hành vi prototype làm quy tắc mới.

## 4. Review và theo dõi

Review hai chiều: từng test case phải dẫn đúng AC/DD; từng nhánh rủi ro của AC phải có ca độc lập. Gắn mã test case vào TDD sau review. Trạng thái `Chưa thực hiện` giữ nguyên cho đến khi chạy; kết quả mock, tích hợp và E2E được ghi riêng với môi trường và phiên bản build. Không báo “test pass” từ số lượng AC hoặc test case đã viết.
