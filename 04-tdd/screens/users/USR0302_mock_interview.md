# Tiêu chí nghiệm thu — Màn `USR0302` Phỏng vấn giả lập (xuyên module)

> Màn: `USR0302` · DD màn: `03-dd/screens/users/USR0302_mock_interview.md` · Cập nhật: 2026-10-24.
> File này chỉ chứa phần mà `04-tdd/modules/F5_ai-review.md` **không** phủ: luồng đi qua nhiều module (`judge-orchestration`, `interview-bank`, `identity`, `ai-review`) và hành vi quan sát được trên màn. Hành vi máy chủ của phiên (mở phiên, luồng SSE, ba giai đoạn, nối lại, gợi ý, kết thúc, phiên bỏ trống, khoá ngân sách) đã ở `AC-18` đến `AC-37`, `AC-53`, `AC-57` của module F5; DD màn ghi `AC-nn` đó ở bảng điểm kiểm thử (mục 7). Mã tiêu chí dùng tiền tố `MI` để không trùng `AC-nn` của module.

## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-MI-01 — Đi từ bài nộp tới phiên phỏng vấn và chọn nguồn bài nộp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Học viên A có các bài nộp Accepted, trong đó bài X không nằm trong nhóm bài nộp gần đây nhất mà tab hiển thị |
| **Hành động** | (a) A mở phiên từ màn kết quả hoặc màn phân tích của bài X. (b) A mở mục "Phỏng vấn giả lập" trên thanh điều hướng và chọn tab "Bài nộp Accepted" |
| **Kết quả mong đợi** | (a) Màn phỏng vấn mở ở tab "Bài nộp Accepted" với bài X đã được chọn sẵn (kể cả khi bài X không nằm trong nhóm gần đây); nút Bắt đầu mở được ngay. (b) Tab hiện các bài nộp Accepted của chính A, chỉ có tên bài và nhãn ngôn ngữ, không hiện mã nội bộ; chưa có bài nào thì có thông báo rỗng. Bài nộp của người khác không bao giờ xuất hiện |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0302_mock_interview.md:93-94, 400, 446] (Q8); [SoT: 03-dd/api/F4_judge-orchestration.md:1171-1220] |

### AC-MI-02 — Tham số phiên tự luyện lấy mặc định từ cài đặt
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | A đã đặt trong cài đặt: mức Senior, 16 lượt, tắt gợi ý |
| **Hành động** | A mở màn phỏng vấn ở tab "Tự chọn chủ đề" |
| **Kết quả mong đợi** | Mức, số lượt, công tắc gợi ý đã mang sẵn giá trị trong cài đặt. A đổi tại màn thì chỉ áp cho phiên này, cài đặt không đổi. Chưa đặt gì thì dùng mặc định Middle, 12 lượt, bật gợi ý. Ở tab "Bài nộp Accepted" ba điều khiển này không hiện |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0302_mock_interview.md:174] (REQ-11); [SoT: 03-dd/api/F1_identity.md:54] |

### AC-MI-03 — Mở phiên từ kho câu hỏi và từ chủ đề; một danh sách lỗi không chặn tab khác
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Kho câu hỏi và danh mục chủ đề có dữ liệu; có tình huống danh sách bài nộp Accepted không tải được |
| **Hành động** | (a) A chọn một câu hỏi ở tab "Kho câu hỏi" rồi bắt đầu. (b) A chọn các chủ đề ở tab "Tự chọn chủ đề" rồi bắt đầu; thử chọn nhiều hơn 10 chủ đề. (c) Mở tab "Bài nộp Accepted" khi danh sách đó lỗi |
| **Kết quả mong đợi** | (a) Phiên mở mà không cần bài nộp nào; AI chất vấn dựa trên đúng câu hỏi đã chọn. (b) Phiên mở với các chủ đề đã chọn; chủ đề thứ 11 không chọn được và có thông báo giới hạn 10; chưa chọn chủ đề nào thì không bắt đầu được. (c) Tab đó hiện lỗi kèm Thử lại; hai tab còn lại dùng bình thường. Liên kết "Xem toàn bộ N câu hỏi" mở danh sách ngân hàng câu hỏi và N là tổng số câu hỏi đang hoạt động |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0302_mock_interview.md:172] (REQ-09); [SoT: 02-bd/screens/users/USR0302_mock_interview.md:95, 429]; [SoT: 03-dd/api/F6_interview-bank.md:137-142] |

### AC-MI-04 — Mở lại kết quả cũ, luyện tập lại, phiên đã đóng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | A có một phiên đã kết thúc có điểm (P1), một phiên đã đóng do bỏ trống chưa đủ lượt (P2) |
| **Hành động** | (a) A mở P1 từ trang tiến độ. (b) A bấm "Luyện tập lại". (c) A mở P2. (d) Phiên đang chạy bị tác vụ nền đóng trong lúc A đang trả lời, A gửi tiếp |
| **Kết quả mong đợi** | (a) Hiện bảng điểm của P1 ở chế độ chỉ xem, không có thông báo "vừa kết thúc"; có nhãn "phản hồi để học, không phải điểm chính thức". (b) Về màn chọn nguồn, tải lại trang không đưa A trở về phiên cũ. (c) Về màn chọn nguồn kèm thông báo phiên đã đóng do bỏ trống, **không** có màn bảng điểm. (d) Nếu phiên đủ lượt và đã được chấm tự động thì A thấy bảng điểm; nếu chưa đủ lượt thì về màn chọn nguồn kèm thông báo như (c) |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0302_mock_interview.md:447] (Q9); [SoT: 02-bd/screens/users/USR0501_my_progress.md:557]; [SoT: 04-tdd/modules/F5_ai-review.md:302-308] (AC-36) |

### AC-MI-05 — Phiên từ bài nộp không có tổng số lượt
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | A mở phiên từ bài nộp Accepted (không có giới hạn số lượt riêng) |
| **Hành động** | A trả lời vài lượt |
| **Kết quả mong đợi** | Màn hiện "Lượt n" **không** kèm tổng, không có thanh tiến độ theo lượt, mục "Lượt còn lại" hiện dấu gạch; phiên vẫn tự dẫn tới bước chấm điểm khi hệ thống báo nên kết thúc. Màn không hiện con số nào mà hệ thống không trả |
| **Nguồn** | [SoT: 03-dd/api/F5_ai-review.md:498] (`maxTurns` null); [SoT: 04-tdd/modules/F5_ai-review.md:166-172] (AC-19) |

### AC-MI-06 — Kết thúc sớm: huỷ popup và phiên chưa có lượt nào
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Phiên đang chạy: (a) đã trả lời 2 lượt; (b) chưa trả lời lượt nào |
| **Hành động** | A bấm "Kết thúc sớm" rồi chọn "Tiếp tục phỏng vấn"; sau đó bấm lại và xác nhận |
| **Kết quả mong đợi** | Chọn "Tiếp tục" thì phiên không đổi, không có yêu cầu chấm điểm nào. (a) Xác nhận thì phiên được chấm và A thấy bảng điểm. (b) Popup nói rõ chưa có gì để chấm; xác nhận chỉ đưa A về màn chọn nguồn, **không** có lỗi hiển thị và không có bảng điểm |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0302_mock_interview.md:414-416]; [SoT: 04-tdd/modules/F5_ai-review.md:286-292] (AC-34) |

### AC-MI-07 — Mất mạng giữa lượt không làm nhân đôi câu trả lời hay mất bản nháp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền điều kiện** | Phiên đang chạy; A gửi một câu trả lời |
| **Hành động** | (a) Mất mạng trước khi hệ thống xác nhận đã nhận câu trả lời. (b) Mất mạng khi AI đang trả lời, rồi có mạng lại sau vài giây. (c) AI lỗi giữa chừng, A bấm "Thử lại lượt này" |
| **Kết quả mong đợi** | (a) Câu trả lời trở lại ô nhập, có thông báo chưa gửi được; không tự gửi lại; không có hai bản câu trả lời. (b) A thấy câu trả lời đầy đủ của AI (hoặc trạng thái "người phỏng vấn đang trả lời" rồi đầy đủ sau ít giây); nếu chậm quá 60 giây chỉ có dòng "đang chậm hơn thường lệ", **không** báo lỗi. (c) Câu trả lời của A chỉ có một bản trong hội thoại; số lượt tăng 1 khi AI trả lời xong |
| **Nguồn** | [SoT: 03-dd/api/F5_ai-review.md:160-172] (mục 2.4); [SoT: 04-tdd/modules/F5_ai-review.md:246-252] (AC-29) |

## 3. Truy vết

Bảng 1 truy vết tiêu chí tới điểm kiểm thử của DD màn (cột "Test case" điền khi có).

**Bảng 1: Truy vết tiêu chí tới DD màn**

| AC | DD màn (`03-dd/screens/users/USR0302_mock_interview.md`) | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-MI-01 | BR-SC-02 bước 1; TP-302-04, TP-302-06 | [TC-USR0302-MIAC01-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-02 | BR-SC-02 bước 4, 5; TP-302-05 | [TC-USR0302-MIAC02-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-03 | BR-SC-02 bước 6, 7; Bảng 3.3; TP-302-07, TP-302-08 | [TC-USR0302-MIAC03-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-04 | BR-SC-02 bước 3, BR-SC-09; TP-302-02, TP-302-34, TP-302-35, TP-302-36 | [TC-USR0302-MIAC04-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-05 | BR-SC-01 bước 2; TP-302-42 | [TC-USR0302-MIAC05-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-06 | BR-SC-08 bước 2; TP-302-29, TP-302-31 | [TC-USR0302-MIAC06-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |
| AC-MI-07 | BR-SC-05; TP-302-21 | [TC-USR0302-MIAC07-01](../../testcases/screens/users/USR0302_mock_interview.md) | chưa |

## 4. Chưa chốt

Không còn quyết định chủ dự án đang chờ cho các AC của màn này. DD màn mục 8 đã chốt Q-302-4 (mở phiên cũ qua `/mock-interview?sessionId={id}` ở chế độ chỉ xem), Q-302-5 (ẩn ba tham số ở tab Bài Accepted), Q-302-6 (banner "phản hồi để học") và Q-302-9 (bỏ các khối không có nguồn dữ liệu). Chất lượng nội dung câu hỏi AI theo cấp độ cần kiểm tay khi nối nhà cung cấp thật, theo giới hạn của TDD F5; đây không phải quyết định nghiệp vụ đang chờ.
