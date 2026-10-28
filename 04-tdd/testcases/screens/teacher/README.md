# Testcase QA màn giảng viên

Thiết kế đã review 2026-10-28 theo [inventory](../AUDIT_INVENTORY.md); **chưa thực thi**. Fixture và bằng chứng theo [quy ước chung](../FIXTURE_CONVENTIONS.md).

| Màn | AC | Ca | Testcase |
| :--- | ---: | ---: | :--- |
| [INS0101](../../../screens/teacher/INS0101_overview.md) | 2 | 2 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0201](../../../screens/teacher/INS0201_class_management.md) | 4 | 8 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0202](../../../screens/teacher/INS0202_class_assignments.md) | 2 | 4 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0203](../../../screens/teacher/INS0203_class_progress.md) | 2 | 2 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0204](../../../screens/teacher/INS0204_class_student_detail.md) | 2 | 4 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0205](../../../screens/teacher/INS0205_class_detail.md) | 2 | 4 | [teacher_screens_phase4](teacher_screens_phase4.md) |
| [INS0206](../../../screens/teacher/INS0206_submission_management.md) | 3 | 4 | [teacher_screens_phase4](teacher_screens_phase4.md) |

**Tổng: 7 màn, 17 AC, 28 ca thiết kế; 0 ca thực thi.**

Review ngữ nghĩa hoàn tất 2026-10-28 cho cả 7 màn (28 ca); checker cấu trúc/trace: 0 lỗi. Nhánh lỗi, race và quyền có fixture cùng oracle riêng. Thực thi vẫn chưa chạy; xem [bản ghi audit](../../AUDIT_DOT4_SCREENS.md).
