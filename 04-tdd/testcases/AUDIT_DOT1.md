# Biên bản đóng mốc thiết kế testcase QA — đợt 1

Ngày rà: 2026-10-27. Phạm vi: F2 problem-bank, F3 harness, F4 judge-orchestration. Đây là **đóng thiết kế có điều kiện** để chuyển cho coding và chuẩn bị chạy; không phải nghiệm thu phần mềm hoặc báo cáo test pass.

> Cập nhật phạm vi 2026-10-29: quyết định `DEC-2026-1029-f2-migration-readiness` sửa hợp đồng epsilon và tái xuất bản snapshot F2, mở rộng AC-47/56/93. Biên bản này giữ kết luận trên baseline 2026-10-27; chưa xác nhận thiết kế testcase hoặc thực thi cho các nhánh mới. Bổ sung ca theo plan `06-plan/261029-1100-f2-db-migration.md` trước khi đóng kiểm nghiệp vụ.

## Kết quả đối chiếu

| Kiểm | Kết quả |
| :--- | :--- |
| Hồ sơ | 40 file cụm: F2 13, F3 11, F4 16; 476 ca theo vị trí file: F2 158, F3 145, F4 173 |
| AC → ca theo nguồn từng ca | F2 94/94, F3 50/50, F4 77/77 = 221/221 AC có ít nhất một ca; các ca dùng chung ghi mọi AC liên quan trong `AC / DD source` |
| ID và định dạng | 476 ID duy nhất; mọi ca có AC/DD source, Test data, Steps, Expected, Cleanup và Evidence (một số ca F2 gộp hai ô cuối) |
| Ưu tiên | P0 248, P1 225, P2 3; 475 ca đã review đầy đủ theo trạng thái tài liệu, AC06-02 review kịch bản nhưng còn đối chứng byte |
| Thực thi | 0 run record; không có ca nào được đánh dấu Đạt/Trượt |

Lượt audit cuối bổ sung 5 ca F2 cho AC57/59 (hạ duyệt, xoá nháp, xoá lặp, hai ngưỡng xuất bản và lỗi dọn MinIO) cùng 2 ca F4 cho AC10/12 (ba Sample không chạy Hidden/không ghi bài nộp và timeout giữ mã trong editor). Trace TDD, mục lục và checklist đã đồng bộ. Các lượt trước đã bổ sung nhánh F3 P1–P5, F2 quyền/Hidden/Outbox, F4 thống kê và sáu oracle được chủ dự án chốt.

## Ca dùng chung và ID ổn định

- F2 AC57 dùng ca duyệt `TC-F2-AC56-03` và ca từ chối `TC-F2-AC57-01`. F2 AC59 dùng hai ca xoá hợp lệ `TC-F2-AC56-06/07` cùng `TC-F2-AC59-01..04`. F2 AC60 dùng `TC-F2-AC56-15..23` vì thay đổi `position` liên quan phiên bản bộ chấm AC56.
- F4 AC13 dùng `TC-F4-AC73-05`; F4 AC75 dùng ba ca snapshot `TC-F2-AC93-01..03` trong cụm F2/F4. Ca `TC-F4-AC74-01` nằm cùng file snapshot F2/F4, nên thống kê **theo thư mục** khác thống kê theo tiền tố ID một ca.
- Ba ID cũ `TC-F2-AC56-21/22/23` chỉ truy nguồn F2 AC60. Giữ nguyên ID đã phát hành để không gãy liên kết; bảng trace AC60 và ma trận cụm dẫn đến đúng ba ca này. Không xem tên tiền tố ID là nguồn AC duy nhất.

## Ngoại lệ sau coding

`TC-F3-AC06-02` đã có sáu command, fixture `U7`, oracle cấu trúc và [handoff P2-T01](modules/F3_harness/F3_P2_golden_handoff.md). Codegen P2 chưa có implementation, nên sáu tệp golden/hash chưa thể tạo hoặc review. Điều kiện **so byte** của ca này còn mở; khi coding xong phải tạo, duyệt diff/hash rồi chạy ca. Không điền hash giả và không ghi Đạt/Trượt trước run record.

Các ca API, DB, Outbox, queue, sandbox, đồng thời và E2E vẫn cần build/hạ tầng thật. Test FE với mock nếu thực hiện sớm phải có run record riêng và không dùng làm bằng chứng cho hợp đồng backend. [Checklist](CHECKLIST.md) giữ hai cổng sau coding chưa đánh dấu.
