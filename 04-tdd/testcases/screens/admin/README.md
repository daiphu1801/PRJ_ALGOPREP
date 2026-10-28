# Testcase QA màn quản trị

Thiết kế đã review 2026-10-28 theo [inventory](../AUDIT_INVENTORY.md); **chưa thực thi**. Fixture và bằng chứng theo [quy ước chung](../FIXTURE_CONVENTIONS.md).

| Màn | AC | Ca | Testcase |
| :--- | ---: | ---: | :--- |
| [ADM0101](../../../screens/admin/ADM0101_overview.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0201](../../../screens/admin/ADM0201_user_management.md) | 3 | 6 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0202](../../../screens/admin/ADM0202_permission_matrix.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0301](../../../screens/admin/ADM0301_ai_config.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0302](../../../screens/admin/ADM0302_ai_usage.md) | 2 | 2 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0303](../../../screens/admin/ADM0303_ai_limits.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0401](../../../screens/admin/ADM0401_queue_monitor.md) | 3 | 5 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0403](../../../screens/admin/ADM0403_system_log.md) | 3 | 5 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0404](../../../screens/admin/ADM0404_infra_events.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |
| [ADM0501](../../../screens/admin/ADM0501_language_config.md) | 3 | 3 | [admin_screens_phase4](admin_screens_phase4.md) |

**Tổng: 10 màn, 29 AC, 36 ca thiết kế; 0 ca thực thi.**

Review ngữ nghĩa hoàn tất 2026-10-28 cho cả 10 màn (36 ca); checker cấu trúc/trace: 0 lỗi. Mutation, quyền, conflict và export có oracle cụ thể. Thực thi vẫn chưa chạy; xem [bản ghi audit](../../AUDIT_DOT4_SCREENS.md).
