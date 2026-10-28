# Audit theo skill — F1 RBAC AC-28–32

> 2026-10-28 · Cụm tiếp theo sau [audit mẫu AC-25–27](AUDIT_SAMPLE_F1_RBAC_AC25_27.md). Đây là review thiết kế, **0 ca thực thi**.

Nguồn đối chiếu: [TDD F1](../modules/F1_identity.md) AC-28–32; [DD API F1](../../03-dd/api/F1_identity.md) #31, #36–40; [DD logic F1](../../03-dd/logic/F1_identity.md) BR-13–16/19; [13 ca F1 RBAC](modules/F1_identity/F1_rbac_audit.md).

## Kết quả cụm

| Mục | Kết quả |
| :--- | :--- |
| AC/case | 5 AC, 13 case đã gắn trace TDD đúng heading |
| Kiểm cấu trúc | 0 lỗi trường/ID/link; 0 cờ oracle sau khi sửa |
| Nhánh đã rà | AC-28 quyền cuối/còn quyền khác; AC-29 tự khoá có/không xác nhận; AC-30 ADMIN cuối và tranh chấp; AC-31 đọc audit theo role; AC-32 CSV/GET che dữ liệu |
| Trạng thái | Cụm đã review thiết kế; chưa chạy API/DB/Redis/CSV |

## Sửa đổi có tác động đến oracle

- AC-28-02 từng nói actor U_ADMIN_A còn quyền nhờ role ADMIN khác, dù A chỉ gán `R_ADMIN_ALT`. Đã sửa: A bị `403 IDT-445` sau khi tự tắt ô, U_ADMIN_B gán `R_ADMIN` mới thực hiện được lệnh khôi phục. Ca kiểm cả permission version và hai audit đúng thứ tự.
- AC-29/30: thay path target dùng tên fixture bằng UUID cụ thể; AC30-03 dùng `acknowledgeSelfImpact=true` đã chốt; AC30-04 nêu hai request/status body và barrier sau xác thực, trước lock.
- AC-31: chủ dự án chốt non-ADMIN đọc audit trả `403 IDT-447` từ guard vai trò Lớp 1, không nhầm với thiếu ô `Function:Action`.
- AC-32: chủ dự án chốt `content` CSV là câu mô tả từ `actionType`/`safeParams` đã che; `Accept-Language: vi|en`, mặc định `vi`. Ca không còn đòi xuất cấu trúc raw JSON. Đổi khoảng ngày export chỉ gồm ngày 26 để audit của chính lần xuất ở ngày 27 không làm lệch số dòng kỳ vọng.

## Cải tiến skill khi áp dụng

Lần chạy này làm rõ một bước review lặp lại: kiểm actor thực sự gán role/owner nào **sau** mutation, thay vì suy quyền từ một role khác còn tồn tại. [Skill audit](../../.agents/skills/algoprep-qa-audit/SKILL.md) đã thêm kiểm quan hệ actor → role → quyền; bộ quét cũng nhận diện thêm các cụm từ oracle chưa xác định. Chạy lại AC-28–32: 13 case, 13 trace, 0 lỗi và 0 cờ.

Các ca này vẫn cần build/hạ tầng để ghi run record. Phạm vi F1 AC-33–80 sẽ được audit theo các cụm tiếp theo trong [checklist](CHECKLIST.md).
