# BD — Storage module `problem-bank` (F2)

> Module duy nhất cùng `identity` viết trong đợt BD lần đầu **có** file `storage` — `identity` không cần
> vì không lưu file lớn; `problem-bank` bắt buộc phải có do F2-07 (bộ testcase lớn lưu MinIO)
> [SoT: `.nexa/control/dependency-map.md` mục 2].

## 1. Ranh giới PostgreSQL vs MinIO

Quyết định thuộc nhóm bốn vùng "đắt nếu sai" của `bd-generation` (mục "Large-testcase storage split") —
chốt ở đây, không để ngỏ tới DD.

| Tiêu chí | PostgreSQL (`testcases.input_inline`/`expected_output_inline`) | MinIO (`input_object_key`/`expected_output_object_key`) |
| :--- | :--- | :--- |
| Ngưỡng kích thước | ≤ 8 KB mỗi phía (input hoặc expected-output tính riêng) | > 8 KB |
| Lý do ngưỡng | `[SoT: Suy luận]` — không có con số trong RD; 8 KB chọn vì đủ chứa phần lớn testcase thuật toán nhập bằng tay (mảng vài trăm phần tử) mà không phình bảng `testcases`, đồng thời tránh TOAST của Postgres phải nén/tách hàng loạt hàng nhỏ | Testcase sinh ngẫu nhiên cho stress-test (mảng 10^5-10^6 phần tử), hoặc input/output nhiều dòng của bài đồ thị lớn — đúng nhóm "bộ lớn" mà F2-07 nhắc tới |
| Truy cập lúc chấm bài | Đọc thẳng từ cột TEXT, không có round-trip mạng | `judge-orchestration` cần tải nội dung trước khi gửi vào `JudgeExecutionPort` — round-trip qua MinIO, cần cache tầng ứng dụng nếu bài được chấm nhiều lần liên tiếp (đề xuất Redis cache ngắn hạn theo `object_key`, TTL vài phút, không bắt buộc ở BD) |

`input_storage`/`expected_output_storage` là hai cột enum độc lập trên cùng một `testcase` — input nhỏ
nhưng expected-output lớn (hoặc ngược lại) là tình huống thật (bài "đếm số lượng" input ngắn nhưng output
là một danh sách dài), nên không dùng chung một cờ `storage_mode` cho cả cặp.

## 2. Cấu trúc bucket

Một bucket cho toàn hệ thống thay vì một bucket/module, vì `problem-bank` là module duy nhất hiện tại cần
MinIO trong sáu Bounded Context (mục hạ tầng ở `dependency-map.md` — chỉ `problem-bank` liệt kê MinIO):

```
algoprep-testcases/
  problems/<problem_id>/
    testcases/<testcase_id>/input.txt
    testcases/<testcase_id>/expected_output.txt
```

`[SoT: Suy luận]` — layout object-key trên là đề xuất BD, RD không đặc tả cấu trúc bucket. Chọn phân cấp
theo `problem_id` trước để: (a) xoá/nhân bản một bài (F2-16) có thể liệt kê theo prefix
`problems/<problem_id>/` mà không cần quét toàn bucket, (b) dễ áp policy lifecycle theo bài nếu về sau cần
dọn testcase của bài đã xoá mềm quá lâu.

Không tạo bucket riêng cho `is_ai_generated_draft = true` (F2-14) — testcase nháp AI sinh vẫn nằm cùng cây
thư mục bài toán đó, phân biệt bằng cột `is_ai_generated_draft` trong Postgres, không phải bằng vị trí lưu
trữ, để nhân bản/xuất bản không phải di chuyển object giữa hai vị trí.

## 3. Chính sách presigned URL

| Thao tác | Ai được phép | Thời hạn URL | Ghi chú |
| :--- | :--- | :--- | :--- |
| Tải lên testcase theo lô (F2-07) | Vai trò có `TESTCASE_MANAGEMENT:CREATE`/`UPDATE` qua ma trận F1-10 (tham chiếu, không thiết kế lại ở đây) | 15 phút, `[SoT: Suy luận]` | Presigned PUT, client (trình duyệt admin) tải thẳng lên MinIO, backend chỉ ký URL và ghi lại `object_key` sau khi tải xong (xác nhận qua callback hoặc polling `HEAD` — chốt ở DD) |
| Tải xuống input/expected-output để hiển thị lại cho Admin xem/sửa | Cùng quyền `TESTCASE_MANAGEMENT` | 5 phút, `[SoT: Suy luận]` | Chỉ Admin/Instructor xem được nội dung testcase Hidden — học viên **không bao giờ** nhận presigned URL trỏ tới input/expected-output của Hidden testcase, dù trực tiếp hay gián tiếp qua log lỗi (F2-08) |
| `judge-orchestration` đọc input để chấm | Backend-to-backend (service credential nội bộ, không phải presigned URL cấp cho client) | Không áp dụng thời hạn kiểu presigned — dùng credential MinIO nội bộ của service | Tránh lộ URL tạm thời ra ngoài qua log/network trace của trình duyệt học viên |

Học viên **không bao giờ** là bên nhận presigned URL của Hidden testcase trong bất kỳ luồng nào — kể cả
luồng chấm bài (kết quả trả về chỉ là verdict + chỉ số testcase sai, F2-08). Presigned URL cho Sample
testcase (nếu cần hiển thị input mẫu dạng file lớn — hiếm, vì Sample thường đủ nhỏ để `INLINE`) mới cấp
cho học viên, thời hạn ngắn tương tự.

## 4. Retention

- Testcase của bài `deleted = true`: **không xoá object MinIO ngay** — theo nguyên tắc "ẩn mềm, dữ liệu
  không mất" áp dụng xuyên suốt RD (F1-16, F2-15). Dọn dẹp vật lý (nếu cần) là một job định kỳ riêng,
  ngoài phạm vi BD lần này — không có yêu cầu RD nào đòi xoá vật lý.
- Testcase nháp AI sinh (F2-14) không dùng để chấm (`is_ai_generated_draft = true`) không có retention
  riêng — theo vòng đời chung của bài toán, vì Admin có thể quay lại xác nhận muộn.

## 5. Việc còn mở — chuyển sang DD

- Cơ chế xác nhận tải lên MinIO hoàn tất (callback MinIO event hay polling từ backend) — mục 3.
- Có cần cache tầng ứng dụng (Redis) cho nội dung testcase MinIO khi `judge-orchestration` chấm lặp lại
  cùng bài nhiều lần liên tiếp hay không — mục 1.
- Ngưỡng 8 KB, thời hạn presigned URL (15 phút/5 phút) là đề xuất BD, chưa có xác nhận chủ dự án — có thể
  điều chỉnh ở DD mà không đổi kiến trúc.
