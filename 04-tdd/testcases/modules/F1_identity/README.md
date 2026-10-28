# Testcase QA F1 — identity

Nguồn: [TDD F1](../../../modules/F1_identity.md), DD F1 API/logic/validation/jobs. Đây là tài liệu thiết kế, chưa có run record.

| Cụm | AC đã có ca | Ca | Trạng thái |
| :--- | :--- | ---: | :--- |
| [Đăng ký, đăng nhập và phiên](F1_auth_session.md) | AC-01–11 | 14 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Hồ sơ và vòng đời tài khoản](F1_profile_lifecycle.md) | AC-12–18b | 12 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Ghi danh lớp và nhãn học viên](F1_class_membership_status.md) | AC-19–24, gồm AC-20b | 12 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Biên và lỗi xác thực](F1_auth_failure_boundaries.md) | Bổ sung AC-01, 04, 05, 06, 09, 11 | 12 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [RBAC, quản trị và audit](F1_rbac_audit.md) | AC-25–32 | 19 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Read model và số liệu giảng viên](F1_extension_a_metrics.md) | Một phần AC-33–55 | 15 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Xác thực và khôi phục mở rộng](F1_extension_a_auth_lifecycle.md) | Một phần AC-33–55 | 22 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Lớp, cài đặt và giới hạn mở rộng](F1_extension_a_classes_admin.md) | Một phần AC-33–55 | 10 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Xuất dữ liệu và số liệu](F1_extension_b_privacy_metrics.md) | Một phần AC-56–78 | 22 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Lớp, đăng nhập liên kết và hồ sơ](F1_extension_b_class_auth.md) | Một phần AC-56–78 | 16 | Đã audit thiết kế 2026-10-28; chưa thực thi |
| [Quyền quản trị và audit mở rộng](F1_extension_b_admin_security.md) | Một phần AC-56–78 | 11 | Đã audit thiết kế 2026-10-28; chưa thực thi |

Đã có **165 ca/80 AC** theo [checklist](../../CHECKLIST.md). [Inventory nhánh nền tảng](F1_foundation_branch_inventory.md) và bảng nhánh trong các file mở rộng ghi phần đã phủ cùng ngoại lệ. Đã đối chiếu 11 cụm với AC/DD và quyết định hiện hành; 165/165 ca qua checker cấu trúc/trace. Các giới hạn đặc tả và cổng sau coding ghi tại [audit đợt 2](../../AUDIT_DOT2_F1.md); chưa có run record.
