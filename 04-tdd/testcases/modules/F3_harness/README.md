# F3 harness — testcase QA

| Cụm | Phạm vi | Ca |
| :--- | :--- | ---: |
| [Hợp đồng harness](F3_harness_contracts.md) | AC-27, 48–50; tập không thứ tự, quyền mã khung, preview, dữ liệu mảng | 19 |
| [So khớp output](F3_output_matching.md) | AC-23–26; EXACT, TRIMMED, EPSILON | 19 |
| [Cây, debug và expected lỗi](F3_output_matching_edge_cases.md) | AC-28–30; hình dạng cây, sentinel, bảo mật dữ liệu | 10 |
| [Lược đồ kiểu và sinh mã](F3_spec_type_validation.md) | AC-31–34; kiểu hợp lệ, ngoài lược đồ và ranh giới | 12 |
| [Tên và quy tắc liên trường](F3_spec_semantic_validation.md) | AC-35–38; định danh, override, chiến lược, chữ ký thiếu | 23 |
| [Đọc đặc tả và vô hiệu cache](F3_spec_read_invalidation.md) | AC-39–40; reader lỗi và event phiên bản | 6 |
| [Chương trình sinh và an toàn](F3_generated_program_safety.md) | AC-41–47; input hỏng, dữ liệu ẩn, startup và AI lỗi | 12 |
| [Mã khung khởi đầu](F3_starter_code.md) | AC-01–05; ba ngôn ngữ, Standard I/O, lỗi đặc tả | 8 |
| [Ghép mã và ghim đặc tả](F3_prepare_source_and_version.md) | AC-04, 06–12; từ chối Wrapper, vị trí chèn, golden, chuẩn hoá, spec version | 16 |
| [Đọc/in TypeNode](F3_type_roundtrip.md) | AC-13; scalar, mảng, list, linked list, cây trên ba ngôn ngữ | 7 |
| [Ánh xạ lỗi compiler](F3_compile_error_mapping.md) | AC-14–22; dòng/cột, lỗi khung, chữ ký, Standard I/O và note C++ | 13 |

Tổng **145 ca thiết kế**; chưa thực thi. Sáu oracle liên quan F2/F3/F4 đã được chủ dự án chốt ngày 2026-10-27; ca AC-06-02 có [handoff golden P2](F3_P2_golden_handoff.md) cho mốc sau codegen. Truy vết AC nằm trong TDD F3; [biên bản đợt 1](../../AUDIT_DOT1.md).
