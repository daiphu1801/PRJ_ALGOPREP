# Audit mẫu theo cụm — F1 RBAC AC-25–27

> 2026-10-28 · Audit **thiết kế testcase**; 0 ca thực thi. Cụm chọn: [F1_rbac_audit.md](modules/F1_identity/F1_rbac_audit.md), đối chiếu [TDD F1](../modules/F1_identity.md) và [DD API](../../03-dd/api/F1_identity.md) / [logic](../../03-dd/logic/F1_identity.md).

## Phạm vi và kiểm tra cấu trúc

| Mục | Kết quả |
| :--- | :--- |
| AC chọn | AC-25, AC-26, AC-27 |
| Case trong phạm vi | 6: AC25-01/02, AC26-01, AC27-01/02/03 |
| Trace TDD đến đúng case | 6/6 |
| 10 trường bắt buộc, ID duy nhất | 6/6, không lỗi |
| Trạng thái chạy | Chưa thực hiện |

Lệnh lặp lại: `python -X utf8 .agents/skills/algoprep-qa-audit/scripts/audit_case_cluster.py --cases 04-tdd/testcases/modules/F1_identity/F1_rbac_audit.md --tdd 04-tdd/modules/F1_identity.md --ac AC-25 --ac AC-26 --ac AC-27`.

## Đối chiếu nhánh và sửa đổi

| AC | Nhánh độc lập đã đối chiếu | Phát hiện và xử lý |
| :--- | :--- | :--- |
| 25 | Thu hồi quyền trên instance đã cache cho phép; cấp lại quyền trên instance đã cache từ chối | Ca cũ ghi “endpoint được gác quyền”, chưa có lớp/actor để quan sát. Đã dùng GET #26 với lớp/học viên UUID cụ thể. Audit phát hiện Redis pub/sub không thể bảo đảm hiệu lực ngay; chủ dự án chốt đọc `permission_version` từ PostgreSQL primary trước khi dùng cache. Hai ca giữ Redis delivery chậm và kiểm request sau PATCH vẫn thấy quyền mới. |
| 26 | Xoá ba role hệ thống | Chủ dự án chốt `422 IDT-421`; mỗi lần từ chối giữ role, quyền, assignment và không ghi audit thành công. |
| 27 | Chặn role tuỳ biến còn người gán; xoá role trống; ghi audit lỗi | Đã thay path dùng tên fixture bằng UUID thật. Chủ dự án chốt `422 IDT-422`, thành công `204` không body với một audit `ROLE_DELETED`, audit lỗi rollback `503 IDT-446`. Bổ sung ca recovery AC27-03 và trace TDD. |

## Phụ thuộc thực thi và phạm vi tiếp theo

- AC-25: mức bảo đảm thời điểm đã chốt 2026-10-28 và đồng bộ DD/TDD/cases. Khi coding, cần đo truy vấn primary/version và bảo đảm module khác dùng cùng guard Lớp 2; chưa có run evidence.
- Các AC 28–32 nằm cùng file nhưng ngoài đợt mẫu; đã audit bằng lượt chọn AC riêng trong [biên bản tiếp theo](AUDIT_F1_RBAC_AC28_32.md). Script chỉ trả cờ oracle trong phạm vi đã chọn.

## Điều chỉnh skill qua lần chạy mẫu

- Bản script đầu báo cờ oracle AC-32 dù chỉ chọn AC-25–27. Đã sửa lọc theo `--ac`, vẫn kiểm ID trùng trên toàn file; chạy lại được 6 case, 6 trace, 0 lỗi/cờ ngoài phạm vi.
- Chạy thử cùng skill trên F6 AC-33/35/36 đã phát hiện ô `Environment` rỗng ở AC36-01 mà phép đếm tên trường trước đó bỏ sót. Đã điền môi trường và chạy lại: 16 case, 16 trace, 0 lỗi. Quét cấu trúc toàn bộ corpus sau sửa: không còn ô bắt buộc rỗng.
- Chạy thử trên màn admin `ADM0501` AL-AC-03: 1 case, 1 trace, 0 lỗi; cùng lệnh và bộ trường áp dụng được cho testcase màn hình.
- Rà AC-25 cho thấy ca dùng barrier đợi Redis đã che race với yêu cầu “ngay sau PATCH”. Đã sửa skill để audit thời điểm lời hứa nghiệp vụ so với delivery bất đồng bộ, rồi thay stimulus bằng request trước khi Redis delivery.

Mẫu audit này là đầu vào cho [skill audit QA](../../.agents/skills/algoprep-qa-audit/SKILL.md). Sau khi chốt AC-25, tiếp tục các cụm theo [checklist](CHECKLIST.md); không suy kết quả kiểm thử từ độ phủ tài liệu.
