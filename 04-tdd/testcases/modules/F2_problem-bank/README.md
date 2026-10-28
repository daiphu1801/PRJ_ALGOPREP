# F2 problem-bank — testcase QA

| Cụm | Phạm vi | Ca |
| :--- | :--- | ---: |
| [Phiên bản bộ testcase](F2_testcase_set_version.md) | AC-56, 57, 59, 60; thay đổi bộ đã duyệt, thứ tự và snapshot | 28 |
| [Snapshot chấm F2/F4](F2_snapshot_grading.md) | F2 AC-93 và các nhánh F4 AC-74–75 dùng snapshot | 6 |
| [Outbox phát và dọn](F2_outbox_delivery_cleanup.md) | AC-89, 90, 94; relay, retry, rollback và retention | 8 |
| [Học viên đọc kho bài](F2_student_catalog_reads.md) | AC-01–11; lọc, tiến độ, lớp, workspace, Sample/Hidden | 15 |
| [Bookmark và gợi ý](F2_bookmarks_recommendations.md) | AC-12–18; ghi chú, quyền riêng tư, xếp hạng, thống kê | 10 |
| [Danh mục độ khó/chủ đề](F2_taxonomy_admin.md) | AC-19–28; quyền ADMIN, audit, thứ tự, tham chiếu | 14 |
| [Danh sách và thao tác hàng loạt](F2_authoring_list_bulk.md) | AC-29–38; danh sách, copy, bulk và quyền | 13 |
| [Biên soạn và xuất bản](F2_authoring_editor_publish.md) | AC-39–53; sửa, đặc tả, preview, publish | 17 |
| [Lưu, upload và chạy đáp án mẫu](F2_testcase_authoring_upload.md) | AC-54/55/58/61–64; storage, confirm, reference | 10 |
| [Sinh testcase AI](F2_ai_generation.md) | AC-65–71; tiên quyết, quota, lọc, bảo mật, đồng thời | 9 |
| [Giao bài và sự kiện lớp](F2_class_assignments.md) | AC-72–80; danh sách, giao/gỡ, thống kê, consumer | 13 |
| [Cổng, quyền và cache](F2_ports_privacy_cache.md) | AC-81–88; port, Hidden, Function, Redis, lớp | 11 |
| [Hạn mức và tiến độ độ khó](F2_reference_rate_level_progress.md) | AC-91–92; cửa sổ chạy reference, số liệu Dashboard | 4 |

Tổng **158 ca thiết kế** trong 13 cụm; 94/94 AC F2 có ca theo nguồn, mốc thiết kế đã đóng và chưa thực thi. Oracle phản hồi lỗi Outbox của AC-75/90 đã chốt 2026-10-27. Truy vết AC nằm trong TDD F2/F4 tương ứng; [biên bản đợt 1](../../AUDIT_DOT1.md).
