# Audit thiết kế đợt 2 — F1 identity

> Cập nhật 2026-10-28. Trạng thái: **đã audit thiết kế 11 cụm theo hợp đồng hiện hành**; **0 ca thực thi**.

## Kết quả đã đối chiếu

- [165 testcase trong 11 cụm](modules/F1_identity/README.md), ID duy nhất; mỗi ca có đủ 10 ô bắt buộc của skill QA. TDD F1 đã gắn ít nhất một ca cho **80/80 AC**.
- Hai quyết định mới đã vào DD/TDD/testcase và [decision registry](../../.nexa/control/decision-registry.md): self-lock/self-demotion yêu cầu `acknowledgeSelfImpact=true`; danh sách học viên cần chú ý xếp mức độ trong từng nhãn rồi lấy tối đa 5.
- Quy tắc phân quyền `403 IDT-445` và xoá role `422 IDT-421`/`422 IDT-422`/`204` kèm audit `ROLE_DELETED` đã được chủ dự án chốt 2026-10-28, đồng bộ vào DD/TDD/ca AC-25–27. Nhánh ghi audit lỗi trả `503 IDT-446` đã có ca recovery AC27-03.
- [Audit mẫu AC-25–27](AUDIT_SAMPLE_F1_RBAC_AC25_27.md) đã phát hiện race giữa lời hứa thu hồi quyền ngay và Redis pub/sub. Chủ dự án giữ bảo đảm ngay; DD dùng `roles.permission_version` đọc từ PostgreSQL primary trước cache, testcase giữ Redis delivery chậm để kiểm. [Skill audit](../../.agents/skills/algoprep-qa-audit/SKILL.md) được tạo và cải tiến qua lần chạy mẫu.
- [Audit AC-28–32](AUDIT_F1_RBAC_AC28_32.md) đã chốt nhánh quyền cuối, tự khoá, ADMIN cuối, guard đọc audit `403 IDT-447` và CSV content mô tả theo locale. 13 ca/5 AC qua kiểm cấu trúc và trace; chưa thực thi.
- [Inventory AC-01–24](modules/F1_identity/F1_foundation_branch_inventory.md) tách nhánh normal, abnormal, security và concurrency/recovery. Các cụm mở rộng có bảng nhánh riêng; đã audit bằng skill trong đợt bổ sung bên dưới.

## Audit tiếp bằng skill ngày 2026-10-28

Đối chiếu 11 file testcase (165 ca, 80 AC) với [TDD](../modules/F1_identity.md), DD [API](../../03-dd/api/F1_identity.md), [logic](../../03-dd/logic/F1_identity.md), [validation](../../03-dd/validation/F1_identity.md), [jobs](../../03-dd/jobs/F1_identity.md) và F4 API §23–24/§consumer. Checker hỗ trợ ID chữ thường và anchor sau cải tiến từ lần chạy thực tế. Mọi ca giữ chưa thực thi.

| Phát hiện | Sửa và nguồn oracle |
| :--- | :--- |
| AC10-01 vẫn tạo OTP cho OAuth-only và đòi IDT-108 | Dùng forgot không OTP, email provider; reset trả chung `422 IDT-107` đúng API §1/owner 2026-10-27. |
| AC01-04 candidate không có số nên không tới validation trùng danh tính | Dùng email/displayName có chữ và số, reset từng trial; cô lập `400 IDT-V04`. |
| AC33-01 roster chung có S3 nhưng mẫu số chỉ đếm S1/S2 | Override fixture: chỉ S1/S2 active; giá trị 75%/87.5% có mẫu số kiểm được. |
| AC72-02 timestamp assigned/accepted trái nhau | Fix accepted T0−3d, assignment T0−2d để chứng minh Accepted trước lần giao không hoàn thành bài. |
| AC73-02 kiểm cache còn phụ thuộc invalidate | Giữ delivery Redis, probe #26 trên instance khác sau PATCH và kiểm primary permission_version trước use case. |
| AC76-02 thiếu ack nên race self-demotion bị chặn trước guard | Cả hai body có acknowledgeSelfImpact=true; race tới bất biến ADMIN cuối, đúng BR-15. |
| AC45 tưởng grant permission vượt ADMIN guard F4 | TDD/ca phân biệt cell đã cấp và quyền có hiệu lực: F4 §23–24/BD ADM0501/security vẫn yêu cầu ADMIN, INSTRUCTOR nhận `403 JDG-301`. |
| AC75-03 đòi topLanguage null cho hai SYSTEM_ERROR | JAVA vẫn đếm language/day; chỉ AC denominator loại SYSTEM_ERROR, đúng API §2. |
| AC21 recovery nhắc consumer xoá dữ liệu F4 không tồn tại | Dùng publish spy, lịch sử và ClassScopeReadPort; F4 không tiêu thụ event này. Consumer ClassDeleted ở problem-bank vẫn có. |
| AC42 thiếu response exact và nhầm batch API | Owner chốt `422 IDT-448`, sửa DD/TDD/ca và decision registry; #37 một user, màn gom response riêng. |
| AC78 export audit tự chèn có thể lọt tập dữ liệu | Filter chỉ ngày Oct 26, export T0 Oct 27; thêm q 101 trong bước negative. |
| AC35-01 tưởng temporary password do client gửi | Generator test tạo secret, lấy qua mail sink; client #50 gửi email/displayName/roleId. |

Bổ sung 5 ca độc lập: AC59-03 (JOB-ID-04 giữ lịch sử token sáu tháng); AC63-03 (JOB-ID-05 DELETE/ARCHIVE và retry lô); AC75-04 (JOB-ID-03 cutoff 90 ngày và retry); AC54-03 (DB rollback giữ OTP, Redis DEL lỗi không replay reset); AC62-03 (SSE mốc đã dọn, resync/heartbeat/redaction). TDD đã gắn thêm đúng ID/file. Các ca job có mốc bằng/trước/sau, lỗi lô và advisory lock nhiều instance.

## Phạm vi đóng và cổng thực thi

- Đóng review thiết kế theo AC/DD hiện hành: 165 ca/80 AC, không có oracle nghiệp vụ cần owner quyết thêm sau IDT-448. Không đồng nghĩa sản phẩm đã pass.
- Giữ khác biệt thứ tự response của login đồng thời là không quy định; AC04-05 chỉ kiểm invariant sau ngưỡng. Không bịa thứ tự in-flight hoặc mã lỗi không có trong DD.
- AC54-03 trial DB failure kiểm rollback/key TTL/retry; DD chưa yêu cầu status/code riêng cho fault dependency này, nên chỉ assert state/recovery đã đặc tả. Khi xây error handler, đối chiếu error envelope chung trước chạy exact HTTP nếu bổ sung hợp đồng.
- Chạy cần build BE/API, PostgreSQL/Redis/RabbitMQ, mail/provider stubs, clock và fault injection có thể lặp lại. Chưa có run record; FE mock không chứng minh quyền máy chủ, DB/event/job.
