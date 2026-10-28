# Tiêu chí nghiệm thu — Màn USR0401 — Ngân hàng câu hỏi phỏng vấn

> DD: `03-dd/screens/users/USR0401_interview_bank_list.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### IL-AC-01 — Danh sách và bộ lọc hoạt động khi một danh mục lỗi

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học mở ngân hàng câu hỏi; danh mục chủ đề hoặc độ khó có thể lỗi. |
| **Hành động** | Tìm kiếm, lọc trạng thái, chủ đề, độ khó và chỉ câu đã đánh dấu. |
| **Kết quả mong đợi** | Danh sách vẫn xem được khi một danh mục phụ lỗi; các bộ lọc có sẵn cộng dồn và đổi bộ lọc về trang đầu. Giá trị lạ trên đường dẫn được dọn. Bộ lọc câu đã đánh dấu được giữ trên đường dẫn khi mở chi tiết rồi quay lại. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0401_interview_bank_list.md] mục 3, 7 |

### IL-AC-02 — Chọn câu và tải thêm không làm mất ngữ cảnh

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có nhiều trang; người học đã chọn một câu. |
| **Hành động** | Tải thêm, đổi lựa chọn nhanh và thử lại khi trang kế hoặc câu đã chọn lỗi. |
| **Kết quả mong đợi** | Các dòng đã tải vẫn còn nếu tải trang kế lỗi; chi tiết chỉ hiện câu được chọn sau cùng. Câu đã bị xoá rời danh sách và màn chọn câu còn lại; lỗi đổi lọc giữ danh sách cũ. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0401_interview_bank_list.md] mục 3, 7 |

### IL-AC-03 — Đánh dấu và tự chấm cập nhật đúng câu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học đang xem một câu chưa đánh dấu và chưa tự chấm. |
| **Hành động** | Đánh dấu, tự chấm, rồi lặp thao tác nhanh hoặc gặp lỗi mạng. |
| **Kết quả mong đợi** | Dấu và mức ôn cập nhật cho đúng câu, có ngày ôn lại dễ hiểu. Lỗi hoàn lại trạng thái cũ và báo rõ; hai lần bấm nhanh không đảo sai. Câu vừa chấm khi lọc Chưa xem vẫn ở lại cho tới khi đổi bộ lọc. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0401_interview_bank_list.md] mục 3, 7 |

### IL-AC-04 — Luyện nhanh đi qua các thẻ và thoát được

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bộ lọc đang cho ít nhất một câu; sau đó thử trường hợp rỗng hoặc lỗi. |
| **Hành động** | Bắt đầu Luyện nhanh, tự chấm từng thẻ, thoát giữa chừng hoặc hoàn thành. |
| **Kết quả mong đợi** | Các câu được chọn đúng bộ lọc; số thẻ không quá mười. Chấm xong chuyển sang thẻ kế; lỗi chấm báo cảnh báo nhưng không giữ người học ở thẻ lỗi. Hết thẻ trở về danh sách; thoát giữa chừng không hỏi xác nhận. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0401_interview_bank_list.md] mục 3, 7 |

### IL-AC-05 — Lối vào phỏng vấn giả lập luôn dùng được

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học ở danh sách câu hỏi, kể cả khi bộ lọc không còn câu nào |
| **Hành động** | Bấm Vào phiên giả lập |
| **Kết quả mong đợi** | Màn mở trang phỏng vấn giả lập; trạng thái rỗng của danh sách câu hỏi không khoá lối đi này và không tự tạo phiên trước khi người học chọn nguồn phỏng vấn |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0401_interview_bank_list.md] EVT-10, T-IL-21 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| IL-AC-01 | Mục 3, 7; điểm kiểm DD: T-IL-01–06, T-IL-20 | [TC-USR0401-ILAC01-01](../../testcases/screens/users/USR0401_interview_bank_list.md) | Chưa thực hiện |
| IL-AC-02 | Mục 3, 7; điểm kiểm DD: T-IL-07–09 | [TC-USR0401-ILAC02-01](../../testcases/screens/users/USR0401_interview_bank_list.md) | Chưa thực hiện |
| IL-AC-03 | Mục 3, 7; điểm kiểm DD: T-IL-10–14 | [TC-USR0401-ILAC03-01](../../testcases/screens/users/USR0401_interview_bank_list.md) | Chưa thực hiện |
| IL-AC-04 | Mục 3, 7; điểm kiểm DD: T-IL-15–19 | [TC-USR0401-ILAC04-01](../../testcases/screens/users/USR0401_interview_bank_list.md) | Chưa thực hiện |
| IL-AC-05 | EVT-10; điểm kiểm DD: T-IL-21 | [TC-USR0401-ILAC05-01](../../testcases/screens/users/USR0401_interview_bank_list.md) | Chưa thực hiện |

