# Tiêu chí nghiệm thu — Màn `USR0301` Phân tích bài giải (xuyên module)

> Màn: `USR0301` · DD màn: `03-dd/screens/users/USR0301_solution_review.md` · Cập nhật: 2026-10-24.
> File này chỉ chứa phần mà `04-tdd/modules/F5_ai-review.md` **không** phủ (`04-tdd/modules/F5_ai-review.md:10` ghi giao diện thuộc `04-tdd/<màn>.md`): luồng đi qua màn hình, Workspace (tầng nháp trình duyệt) và hai module `ai-review`, `judge-orchestration`. Hành vi của máy chủ (sinh báo cáo, cache, hạn mức, ngân sách, lỗi AI) đã ở `AC-01` đến `AC-14` của module F5; DD màn ghi `AC-nn` đó ở bảng điểm kiểm thử (mục 7). Mã tiêu chí dùng tiền tố `SR` để không trùng `AC-nn` của module.

## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SR-01 — Lối đi từ báo cáo sang các màn liên quan
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Học viên A đang xem báo cáo của bài nộp X (đã Accepted) |
| **Hành động** | A bấm mục "Kết quả nộp bài" hoặc mã bài nộp ở đường dẫn phân cấp; sau đó quay lại báo cáo và bấm "Phỏng vấn về bài này" |
| **Kết quả mong đợi** | Hai mục đường dẫn đều về trang kết quả của bài nộp X. Nút phỏng vấn mở màn phỏng vấn giả lập qua đường dẫn mang đúng mã bài nộp X (màn phỏng vấn chọn sẵn bài X, xem `AC-MI-01`) |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0301_solution_review.md:463-464] |

### AC-SR-02 — Không tự gọi lại ngầm khi chưa có báo cáo
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Bài nộp X chưa có báo cáo; AI giả lỗi ở lần gọi đầu |
| **Hành động** | A mở báo cáo (nhận lỗi tạm thời); rời sang tab khác rồi quay lại; mất mạng rồi có lại mạng; **không** bấm Thử lại |
| **Kết quả mong đợi** | Hệ thống chỉ gọi AI đúng một lần cho tới khi A bấm Thử lại; không có thông báo lỗi thứ hai tự xuất hiện; không tính thêm vào giới hạn mỗi giờ. Khi A bấm Thử lại một lần, chỉ có đúng một lần gọi mới |
| **Nguồn** | [SoT: 03-dd/api/F5_ai-review.md:206] (không thử lại tự động); [SoT: 02-bd/screens/users/USR0301_solution_review.md:483] |

### AC-SR-03 — Áp bản mã đề xuất vào Workspace phải có xác nhận
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Báo cáo của bài nộp X có mã đề xuất; Workspace của bài X (cùng ngôn ngữ và mô hình nộp) đang có nháp của A |
| **Hành động** | (a) A bấm "Áp dụng vào Workspace" rồi chọn "Huỷ". (b) A bấm lại, rồi chọn "Ghi đè & Mở Workspace" |
| **Kết quả mong đợi** | (a) Nháp của A không đổi, A ở lại báo cáo. (b) Nháp của bài X được thay bằng mã đề xuất, A được đưa tới Workspace của bài X, thấy mã đề xuất đã nạp sẵn; có một thông báo thành công. Báo cáo của bài nộp không có nút áp dụng khi hướng giải đã tối ưu (không có mã đề xuất) |
| **Nguồn** | [SoT: 01-rd/req/F5_ai-review.md] F5-26; [SoT: 02-bd/screens/users/USR0301_solution_review.md:465-467, 481] |

### AC-SR-04 — Không ghi đè khi không giữ được bản cũ hoặc không lưu được
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Như `AC-SR-03`; trình duyệt chặn lưu trữ cục bộ (hoặc đầy) |
| **Hành động** | A xác nhận ghi đè |
| **Kết quả mong đợi** | A nhận thông báo không lưu được; nháp cũ **nguyên vẹn**; A ở lại báo cáo, không bị chuyển màn. Khi cơ chế lịch sử nháp có mặt (điều kiện tốt nghiệp, DD màn mục 8): bản nháp cũ được giữ lại với nhãn "Bản trước khi áp dụng AI Diff" trước khi ghi đè, A hoàn tác được bằng một nút bấm trong Workspace; không giữ được bản cũ thì không ghi đè |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0301_solution_review.md:482]; [SoT: 02-bd/screens/users/USR0301_solution_review.md:516] |

### AC-SR-05 — Báo cáo khi thiếu mã đã nộp, và bản so sánh không có dòng giả
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | (a) Báo cáo có mã đề xuất nhưng chưa tải được mã đã nộp. (b) Báo cáo của bài nộp B trúng bộ nhớ đệm từ bài nộp A, hai mã chỉ khác khoảng trắng cuối dòng và kiểu xuống dòng |
| **Hành động** | A mở báo cáo trong từng trường hợp |
| **Kết quả mong đợi** | (a) Báo cáo vẫn hiện đủ phần phân tích; khối mã đã nộp và bản so sánh được ẩn kèm một câu giải thích; nút áp dụng khoá. (b) Bản so sánh không hiện các cặp dòng "bỏ, thêm" trông giống hệt nhau chỉ vì khoảng trắng cuối dòng |
| **Nguồn** | [SoT: Suy luận] từ [SoT: 03-dd/logic/F5_ai-review.md:77] (chuẩn hoá mã khi tra bộ nhớ đệm) và [SoT: 03-dd/api/F5_ai-review.md:263] (báo cáo không mang mã nguồn) |

### AC-SR-06 — Chờ giới hạn tần suất: có lý do, có đếm ngược, không bấm sớm được
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Giới hạn phân tích là 10 lần mỗi giờ và A đã dùng hết (hoặc A vừa sinh một báo cáo mới trong thời gian chờ 20 giây); bài nộp X chưa có báo cáo và mã chưa có trong bộ nhớ đệm |
| **Hành động** | A mở báo cáo của bài nộp X; chờ hết thời gian đếm ngược; bấm "Thử lại" |
| **Kết quả mong đợi** | A thấy một khối thông báo nêu lý do (đã yêu cầu quá nhiều hoặc cần chờ giữa hai yêu cầu) kèm số giây còn phải chờ, chỉ có chữ và đồng hồ; nút "Thử lại" bị tắt cho tới khi hết giờ, không có lối tắt nào khác. Khối này khác khối "hết ngân sách" (không báo là bị khoá) và khác khối "hệ thống AI đang bận". Hết giờ thì nút mở; bấm Thử lại thì gọi lại đúng một lần và (khi còn hạn mức) nhận báo cáo |
| **Nguồn** | [SoT: 03-dd/api/F5_ai-review.md:274-275]; [SoT: 04-tdd/modules/F5_ai-review.md:58-72] (AC-06, AC-07); `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim` |

### AC-SR-07 — Báo cáo của bài đã rút vẫn mở và báo rõ trạng thái bài
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có báo cáo phân tích của một bài nộp Accepted; bài toán sau đó bị rút khỏi ngân hàng |
| **Hành động** | Mở lại báo cáo phân tích |
| **Kết quả mong đợi** | Báo cáo vẫn xem được và có nhãn bài đã rút. Liên kết về đề bài không đưa tới trang không còn mở được; nội dung báo cáo và kết quả chấm không bị xoá |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0301_solution_review.md] BR-SC-05, TP-301-24 |

### AC-SR-08 — Giao diện tiếng Anh có đủ nhãn cho luồng phân tích
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học chọn tiếng Anh và có báo cáo phân tích |
| **Hành động** | Mở báo cáo, xem các trạng thái chờ, lỗi và hộp xác nhận áp dụng mã |
| **Kết quả mong đợi** | Tất cả nhãn, thông báo và nút trong luồng phân tích hiện bằng tiếng Anh, không lộ khoá dịch hoặc trộn tiếng Việt ở các phần mới |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0301_solution_review.md] mục 6, TP-301-22 |

## 3. Truy vết

Bảng 1 truy vết tiêu chí tới điểm kiểm thử của DD màn (cột "Test case" điền khi có).

**Bảng 1: Truy vết tiêu chí tới DD màn**

| AC | DD màn (`03-dd/screens/users/USR0301_solution_review.md`) | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SR-01 | mục 2 (EVT-2, EVT-3), BR-SC-06; TP-301-12, TP-301-13 | [TC-USR0301-SRAC01-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-02 | BR-SC-01, BR-SC-02; TP-301-10 | [TC-USR0301-SRAC02-01](../../testcases/screens/users/USR0301_solution_review.md), [TC-USR0301-SRAC02-02](../../testcases/screens/users/USR0301_solution_review.md), [TC-USR0301-SRAC02-03](../../testcases/screens/users/USR0301_solution_review.md), [TC-USR0301-SRAC02-04](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-03 | BR-SC-03; TP-301-14, TP-301-15 | [TC-USR0301-SRAC03-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-04 | BR-SC-03 bước 2, 3; TP-301-16, TP-301-17 | [TC-USR0301-SRAC04-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-05 | BR-SC-04, mục 3 (một phần thất bại); TP-301-18, TP-301-19 | [TC-USR0301-SRAC05-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-06 | mục 3 (chờ giới hạn tần suất), BR-SC-02 bước 4; TP-301-07, TP-301-23 | [TC-USR0301-SRAC06-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-07 | BR-SC-05; TP-301-24 | [TC-USR0301-SRAC07-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |
| AC-SR-08 | Mục 6; TP-301-22 | [TC-USR0301-SRAC08-01](../../testcases/screens/users/USR0301_solution_review.md) | chưa |

## 4. Chưa chốt

| Mục | Ai quyết | Ghi chú |
| :--- | :--- | :--- |
| Sao lưu bản cũ trước khi áp dụng | Đã đóng ở `USR0102` BR-SC-10 | AC-SR-04 và PD-AC-11 kiểm sao lưu, hoàn tác và nhánh ghi thất bại; hàm cụ thể dựng khi code |
| Nhãn bài đã rút | Đã chốt 2026-10-24 | AC-SR-07 phủ TP-301-24 |
| Trạng thái chờ giới hạn tần suất | Đã chốt 2026-10-24 | AC-SR-06 phủ nhánh màn, module F5 AC-06/07 phủ máy chủ |
