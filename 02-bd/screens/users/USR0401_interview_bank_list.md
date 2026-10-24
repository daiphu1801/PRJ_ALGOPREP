# Tài liệu thiết kế cơ bản (BD) — Ngân hàng câu hỏi phỏng vấn (`USR0401`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `USR0401` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
  Slug chính tắc `interview_bank_list` không đổi.
- Màn không có màn con và **không có popup**; nó có **hai trạng thái trong cùng một màn**: `browse` (mặc
  định) và `drill` (luyện nhanh dạng thẻ) [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:97,193]. Theo
  quy ước thuật ngữ ở `02-bd/_rules/bd-template-9sheet.md:51-54`, đổi trạng thái **không** phải chuyển màn.
- Màn là gốc của cụm hai màn: màn này và `interview_question_detail` (`USR0402`) — bản xem nhanh ở cột
  phải chỉ là **bản rút gọn**, trang chi tiết đầy đủ là màn riêng
  [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:116].

> Đọc cùng `01-rd/screens/users/USR0401_interview_bank_list.md` (hành vi ở mức yêu cầu, không lặp lại ở
> đây), hai file BD module `02-bd/architecture/F6_interview-bank.md` và `02-bd/database/F6_interview-bank.md`,
> và khung điều hướng khu Người học `02-bd/screens/users/_shell.md`. **Không mô tả lại header và chân
> trang** — màn này là mục nav thứ 5 (cuối) trên thanh header ngang, không có sidebar
> [Nguồn: 02-bd/screens/users/_shell.md:60].
>
> **Phạm vi đã chốt trước khi viết BD** — không mở lại:
> - **Không có** khái niệm "bộ câu hỏi theo lớp" (F6-11 đã loại khỏi phạm vi,
>   `DEC-2026-0828-remove-per-class-interview-set`) và **không có** bảng `question_sets`
>   [Nguồn: 02-bd/database/F6_interview-bank.md:206-212].
> - **Danh mục chủ đề là dữ liệu do ADMIN quản lý** (5 chủ đề khởi tạo, không còn cố định — thay tiểu quyết
>   định 4 của `DEC-2026-0830-interview-bank-crud` bằng `DEC-2026-1001-admin-configurable-settings`), dùng chung ba màn, màn này không tự định
>   nghĩa danh sách riêng [Nguồn: 01-rd/req/F6_interview-bank.md:9-13;
>   02-bd/architecture/F6_interview-bank.md:58-66].
> - **Màn này không gọi AI.** Chế độ luyện (F6-07/F6-08 — nơi duy nhất F6 chạm LLM) nằm ở `USR0402`
>   [Nguồn: 02-bd/architecture/F6_interview-bank.md:49]. Phân hệ AI chết hoặc hết quota thì màn này hoạt
>   động **nguyên vẹn**: duyệt, lọc, xem nhanh, tự chấm, luyện nhanh đều không phụ thuộc AI
>   [Nguồn: 02-bd/architecture/F6_interview-bank.md:270-279].
> - **Không có use case sửa câu trả lời đã nộp** (`DEC-2026-0831-outside-screens-closures`) — màn này
>   không chạm `user_answers` ở bất kỳ chiều nào.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `interview-bank` (F6) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Ngân hàng câu hỏi phỏng vấn |
| Mã màn hình | `USR0401` |
| Tên vật lý (slug) | `interview_bank_list` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A1 (`STUDENT`) |
| Phiên bản | V1.15 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/21 |
| Người cập nhật | Claude |
| Ngày cập nhật | 2026/10/22 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V1.0 | Toàn bộ | Tạo mới theo mẫu 9 sheet. Ánh xạ toàn bộ trường hiển thị của prototype về cột thật trong `02-bd/database/F6_interview-bank.md`; phát hiện 5 trường prototype không có nguồn dữ liệu (mã câu hỏi `IQ-0nn`, dòng "Xuất hiện ở 6/12 buổi phỏng vấn", "Bẫy thường gặp", "Thêm vào phiên giả lập tới", tag) và chuyển thành câu hỏi mở; bổ sung hai điều khiển RD yêu cầu mà prototype thiếu (đánh dấu xem lại F6-03, liên kết mở `USR0402`) | 2026/09/21 | Nhóm phát triển AlgoPrep |
| V1.1 | Sheet 7 (mục 7.3) | Sửa tham chiếu sang `SHR0301`: endpoint `GetInterviewQuestionBankStats` không còn tồn tại ở màn quản trị (dải thẻ chỉ số đã bỏ 2026-10-01), nên bỏ câu "không dùng lại" và trỏ số dòng đúng của `ListInterviewQuestions`, `ListQuestionTopics`. Không đổi hành vi của màn này | 2026/10/01 | AI |
| V1.2 | Sheet 4, 5, 7 | Đã chốt 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`: **danh mục chủ đề do ADMIN quản lý**, không còn "5 chủ đề cố định". Menu lọc chủ đề đọc từ `ListQuestionTopics`: chip "Tất cả" cộng một chip cho mỗi chủ đề hiện có, số chip không cố định; 5 chủ đề cũ chỉ là dữ liệu khởi tạo. `ListQuestionTopics` là endpoint đọc, mọi người dùng đã xác thực gọi được; thêm/sửa/xoá chủ đề ở `SHR0301` | 2026/10/01 | AI |
| V1.3 | Sheet 8, 9 | Đổi phản hồi sau thao tác (tìm kiếm, lọc, đánh dấu, tự chấm, luyện nhanh) và `[Tiêu điểm]` kiểm nhập liệu sang toast; giữ nguyên lỗi tải theo khu vực ở EVT-1, EVT-5, EVT-6. Theo `DEC-2026-1003-toast-feedback-channel`. | 2026/10/03 | AI |
| V1.4 | Sheet 4, 5, 7 | Đồng bộ `DEC-2026-1001-admin-configurable-settings` mục (6): độ khó câu hỏi là danh mục do ADMIN quản lý (`question_levels`), không còn enum cố định. DTO `difficulty` thành `levelCode`/`levelDisplayName` đọc qua `level_id`; nhãn hiển thị lấy từ dữ liệu, bỏ ánh xạ cứng "Dễ/Trung bình/Khó"; thêm `question_levels` vào bảng liên quan và Truy cập bảng (R, qua join). Màn chỉ hiển thị nhãn, không có bộ lọc độ khó (khớp bản dựng lúc đó: chỉ có bộ lọc trạng thái và chủ đề). **Đã thay bởi V1.5**: bản dựng nay có bộ lọc độ khó. Không đổi NO | 2026/10/03 | AI |
| V1.5 | Sheet 4, 5, 6, 7, 8, 9, Câu hỏi mở; rà soát toàn bộ trích dẫn | Chủ dự án duyệt 2026-10-03: thêm **bộ lọc độ khó** phía người học. Menu lọc độ khó đặt sau chip chủ đề, mỗi mức của `question_levels` một tab cộng tab "Tất cả"; cộng dồn `VÀ` với tìm kiếm, tab trạng thái và chip chủ đề, và luyện nhanh lấy bộ thẻ từ danh sách đã lọc kể cả theo độ khó. Màn nay gọi thêm `ListQuestionLevels` (chỉ đọc, mọi người dùng đã xác thực gọi được, `02-bd/security/F6_interview-bank.md:76`). Thêm mới, không đổi số: Sheet 5 khu vực B NO 4; Sheet 6 khu vực B NO 4; Sheet 7.1 NO 15; Sheet 7.3 NO 7; Sheet 8 EVT-15; Câu hỏi mở Q10. Sửa câu "không lọc theo độ khó" ở Sheet 7.1 NO 3 và V1.4. Sửa EVT-1 (tải thêm danh mục độ khó) và Sheet 9 NO 4 (bỏ "5 mã chủ đề seed" đã lỗi thời theo V1.2). Rà lại số dòng mọi trích dẫn: `02-bd/database/F6_interview-bank.md` lệch +22 từ mục 1.1a; `02-bd/architecture/F6_interview-bank.md` lệch ở mục 2.1 trở xuống; các dòng `interview-bank-list-view.tsx` lệch +8; mục nav của màn là thứ 5 (không phải thứ 6) theo `_shell.md:60`; câu hỏi mở Q1 đã được khung khu Người học đóng (`_shell.md:131`) | 2026/10/03 | AI |
| V1.6 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở; rà lại trích dẫn `interview-bank-list-view.tsx` | Chủ dự án quyết 2026-10-18 (mở rộng quy tắc 2026-10-13 "không dải thẻ chỉ số ở màn danh sách"): **bỏ bốn thẻ chỉ số** (Tổng câu hỏi, Đã thuộc, Đang ôn, Ôn trong tuần) khỏi màn. Hàng đầu trang chỉ còn hai nút hành động, căn phải. Sheet 4: bỏ widget `widgets/recall-summary-bar`, `RecallSummaryDto` và vùng "Thanh chỉ số" thành "Hàng hành động". Sheet 5/6: xoá Khu vực A NO 1-4, đánh lại NO: "Luyện nhanh" thành NO 1, "Vào phiên giả lập" thành NO 2 (nút này đang `disabled` ở bản dựng, ghi vào ghi chú). Sheet 7: xoá `RecallSummaryDto` (NO 11, 12) và endpoint `GetMyRecallSummary` (NO 4); đánh lại NO 13-15 thành 11-13 (DTO) và NO 5-7 thành 4-6 (endpoint); `recall_ratings` vẫn đọc cho cột trạng thái và hàng đợi Luyện nhanh. Sheet 8: EVT-1, EVT-2, EVT-8, EVT-13, EVT-14, EVT-15 không còn tải hay cập nhật chỉ số; EVT-14 không còn gọi API. Câu hỏi mở: Q2 rút (chỉ số đã bỏ); thêm Q11 (RD còn yêu cầu thanh chỉ số). Không đổi số EVT, không đổi số Q | 2026/10/18 | AI |
| V1.7 | Sheet 3, 4 (4.1), 5 (Khu vực D NO 1, 4), 8 (EVT-10), Câu hỏi mở Q3, Q4, Q5, Q11 | Owner 2026/10/18 (`DEC-2026-1018-usr-open-questions-phase1-scope`, mục 1, 4, 7), mỗi mục lấy đề xuất mặc định: đóng Q11 (RD đã bỏ mục "Thanh chỉ số"; đối chiếu F6-09/F6-10, ghi F6-10 chưa có màn nhận); đóng Q5 (nút "Thêm vào phiên giả lập tới" không tồn tại, ponytail bậc 1, 2); đóng Q3 (bỏ mã `IQ-nnn` khỏi giao diện, không `display_code`); đóng Q4 (dùng `core_keywords` thay tag, không bảng tag). Trích dẫn `interview-question-list-row.tsx:60-73` và `interview-bank-list-view.tsx:364,387-397,416-424,426-450` đã đọc lại. Không đổi số Q, EVT, NO | 2026/10/18 | Claude |
| V1.8 | Sheet 3, 4, 5, 6, 7, 8, 9, Hiện trạng bản dựng (mới), Câu hỏi mở Q7 tới Q11; sửa trích dẫn mã nguồn | Owner 2026/10/18 (`DEC-2026-1018-usr-open-questions-phase2-5`, mục PHASE 3, theo đề xuất): đóng Q7 (dòng "Sẽ nhắc ôn lại sau {n} ngày" dưới khối tự chấm; thêm Sheet 5 và 6 khu vực D NO 7, sửa Sheet 7.1 NO 12), Q8 (công tắc "Chỉ câu đã đánh dấu" cạnh bốn tab trạng thái, trục độc lập; thêm Sheet 5 và 6 khu vực B NO 5, Sheet 8 EVT-16, sửa Sheet 5 khu vực C NO 1, Sheet 7.3 NO 1), Q9 (luyện nhanh cố định 10 thẻ, hằng số cấu hình), Q10 (tham số URL `level` vào hợp đồng: sửa Sheet 3 hai khối, Sheet 5 khu vực B NO 4, Sheet 8 EVT-1, EVT-9, EVT-15, Sheet 9 NO 4, cùng `USR0402` Sheet 3). Q11: F6-10 do dashboard `USR0601` nhận. Thêm mục "Hiện trạng bản dựng" (Q10 chưa làm ở prototype vì chưa bộ lọc nào đọc URL, ghi nợ). Mã nguồn view đã thêm công tắc, dòng ôn lại và đổi chú thích hằng số nên mọi số dòng `interview-bank-list-view.tsx` được đọc lại (lệch +16 từ dòng 250 trở xuống). Không đổi số NO, EVT, Q đã có; mục mới thêm cuối danh sách | 2026/10/18 | Claude |
| V1.9 | Sheet 5 (Khu vực B NO 2, D NO 5, E NO 8), Hiện trạng bản dựng, Câu hỏi mở Q6; sửa trích dẫn chéo | Owner 2026/10/18 (`DEC-2026-1018-usr-open-questions-last-four`, theo đề xuất): đóng **Q6**: ba nút tự chấm là ba mức F6-12 Biết rõ / Mơ hồ / Quên (enum `KNOWN`/`VAGUE`/`FORGOTTEN`) do **người học tự chọn, không có AI** (owner đã hỏi đây có phải AI chấm sau Accepted không; không, đó là F5 và F6-08); bộ lọc trạng thái giữ bốn tab; "Chưa xem" là không có dòng `recall_ratings`, không bao giờ là lựa chọn tự chấm; luyện nhanh dùng đủ ba nút. Ponytail bậc 1. Thêm dòng đối chiếu nhãn vào "Hiện trạng bản dựng" (đọc mã và `vi.json`: nhãn khớp, không có nợ). Làm rõ ghi chú Sheet 5 khu vực B NO 2, D NO 5, E NO 8. Do thêm hàng ở Sheet 2 và Hiện trạng, số dòng của mục Câu hỏi mở lệch +2; đã sửa trích dẫn sang các dòng này ở `USR0402` BD. Không đổi số NO, EVT, Q | 2026/10/18 | Claude |
| V1.10 | Sheet 5 (Khu vực B NO 1 đến NO 5), Sheet 6 (Khu vực B NO 2 đến NO 5), Sheet 8 (EVT-3, EVT-4, EVT-8, EVT-7, EVT-15, EVT-16), Hiện trạng bản dựng | Owner 2026/10/19 (kết quả rà soát prototype nhóm C): (1) **ba bộ lọc trạng thái, chủ đề, độ khó là menu thả xuống (`FilterMenu`)**, không phải tab/chip như bản cũ mô tả; (2) tự chấm hoặc bỏ đánh dấu khi đang lọc: **câu vừa thao tác vẫn ở lại trong danh sách và bản xem nhanh cho tới khi người học đổi bộ lọc hoặc ô tìm kiếm, hoặc tải lại danh sách**, để dòng "Sẽ nhắc ôn lại sau {n} ngày" không biến mất ngay; (3) ô tìm kiếm khớp tiêu đề, nội dung và từ khoá cốt lõi, cắt 100 ký tự, placeholder bỏ chữ "tag"; (4) thanh tiến độ luyện nhanh dùng `ProgressBar` chung; (5) ẩn khối "Ý cần nói" khi câu không có ý nào (câu nhập CSV) | 2026/10/19 | AI |
| V1.11 | Câu hỏi mở Q10 | Owner chốt 2026/10/19 (`DEC-2026-1019-usr-fe-audit-fixes`, chốt nốt câu hỏi mở còn lại sau rà prototype). Chỉ tài liệu, không đổi code, chưa duyệt hình. Q10 (bộ lọc trên tham số URL) giữ là nợ prototype, không dựng ở bước này (ponytail bậc 3). | 2026/10/19 | Claude |
| V1.12 | Sheet 3 (khối `Vào phiên giả lập`), 4 (4.4, 4.5), 5 và 6 (Khu vực A, B, C, D), 8 (EVT-10), Hiện trạng bản dựng, Câu hỏi mở Q12, Q13; sửa lại mọi trích dẫn `interview-bank-list-view.tsx` | **Chưa duyệt hình.** Dựng lại giao diện theo yêu cầu của owner 2026/10/20, chưa phải thiết kế đã duyệt: (1) thêm **tiêu đề trang và phụ đề** (`interviewBankList.title`, `.subtitle`); (2) nút "Vào phiên giả lập" **không còn `disabled`**, nay là liên kết thật (`Button asChild`, biểu tượng mic) tới `/mock-interview`; khoá i18n `startMockInterviewUnavailable` không còn được dùng (ghi nợ, xoá được); (3) ô tìm kiếm, ba menu lọc và công tắc "Chỉ câu đã đánh dấu" gom vào **một thẻ thanh công cụ nằm ngang** thay vì nằm trong thẻ danh sách; (4) thêm dòng đếm "Hiển thị {shown} / {total} câu" (`shownCount`) ở đầu thẻ danh sách, vùng cuộn tối đa khoảng `68dvh`; (5) tỉ lệ hai cột **5:6** (cột chi tiết rộng hơn), tiêu đề bản xem nhanh to và đậm hơn, khối từ khoá có nhãn "Từ khoá cốt lõi" (`quickView.keywordsLabel`), mỗi ý cần nói nằm trong một ô nền nhạt. Xoá mọi câu nói nút này `disabled` ở Sheet 5, 6, 8 và Hiện trạng bản dựng. Số dòng mọi trích dẫn view đã lệch (+26 tới +49 từ dòng 100 trở xuống) nên đã đọc lại và sửa. Không đổi số NO, EVT, Q đã có; mục mới thêm cuối danh sách. Chế độ luyện nhanh không đổi | 2026/10/21 | Claude |
| V1.13 | Sheet 4 (4.4, 4.5), 5 và 6 (Khu vực B), Hiện trạng bản dựng, Câu hỏi mở Q12; sửa lại số dòng trích dẫn `interview-bank-list-view.tsx` | **Chưa duyệt hình.** Owner 2026/10/21: thanh lọc nay dùng `FilterBar` chung của `shared/ui/data/filter-bar.tsx` (ô tìm kiếm, ba `FilterMenu`, `Toggle` làm phần tử con) thay cho thẻ `glass-card` tự dựng; hành vi lọc, giới hạn 100 ký tự, Enter báo số kết quả và ghim dòng không đổi. Đóng phần thanh lọc của Q12 (ponytail bậc 4). Các số dòng từ 339 trở xuống lệch -4 nên đã đo lại. Không đổi số NO, EVT; Q13 giữ mở (thuộc DD) | 2026/10/21 | Claude |
| V1.14 | Sheet 4 (4.1), 5 (Khu vực D NO 7), 7 (7.1 NO 5, 12), 8 (EVT-8, EVT-11), Hiện trạng bản dựng (Q7), Câu hỏi mở Q3, các dòng `[Nguồn]` | Sửa 2026-10-22 đồng bộ DB, `DEC-2026-1022-interview-bank-schema-sync` (IB-01, IB-03, IB-16): (1) tách hai tên: **"đang ôn"** (rating `VAGUE`/`FORGOTTEN`, tab và nhãn của màn) và **"đến hạn"** (`next_review_at <= now()`, F6-09 và hàng đợi Luyện nhanh), EVT-11 ghi "đến hạn" thay cho "tới hạn ôn lại"; (2) khoảng ôn lại là bảng cố định `FORGOTTEN` 1, `VAGUE` 3, `KNOWN` 7 ngày, không cộng dồn, thay "công thức spaced repetition"; bảng giả lập của prototype nay trùng đặc tả; (3) Q3 ghi `display_no` chỉ cho khu quản trị, màn học viên vẫn không hiện mã; (4) đọc lại số dòng mọi trích dẫn `02-bd/database/F6_interview-bank.md` (ví dụ recall_ratings từ `:159-177` thành `:177-205`, status `:65` thành `:66`). Không đổi RD, không đổi code | 2026/10/22 | Claude |
| V1.15 | Câu hỏi mở Q13 | Sửa 2026/10/23 (`DEC-2026-1023-open-questions-closed`): đóng Q13, `total` là tổng câu `ACTIVE` toàn ngân hàng. Không đổi giao diện | 2026/10/23 | Claude |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, màn đích
> tô tím. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng. Trạng thái `drill` vẽ
> trong cùng nút với màn chính vì **không phải một màn** — xem Phương châm tài liệu.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng khu Người học → Ngân hàng câu hỏi phỏng vấn

[Điều kiện mở] Chọn mục nav thứ 5 "Câu hỏi phỏng vấn" trên thanh header ngang
[Nguồn: 02-bd/screens/users/_shell.md:60].

[Chế độ mở] Trạng thái `browse`, không có bộ lọc nào được áp.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tải danh mục chủ đề, danh mục độ khó và trang đầu danh sách câu hỏi; cột phải hiển thị
bản xem nhanh của câu hỏi đầu danh sách. Không còn tải chỉ số ôn tập (đã bỏ 2026-10-18, owner).

[Khi huỷ] Không có.

#### Phân tích bài giải → Ngân hàng câu hỏi phỏng vấn

[Điều kiện mở] Bấm liên kết gợi ý ôn tập trên màn `solution_review` (F5-06)
[Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:131].

[Chế độ mở] Trạng thái `browse`, bộ lọc áp sẵn theo tham số nhận được.

[Thông tin truyền] `topic` (mã chủ đề, một giá trị) và `q` (từ khoá tìm kiếm) trên tham số truy vấn —
hình dạng chính xác của tham số thuộc DD `interview-bank`, RD đã đánh **[Đợi nextjs]**
[Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:118]. `solution_review` **không** gửi `level` (mã độ
khó): tham số này chỉ có khi quay lại từ `USR0402` (Câu hỏi mở Q10, đã chốt 2026-10-18).

[Giá trị trả về] Không có.

[Khi thành công] Menu lọc chủ đề và ô tìm kiếm hiển thị đúng giá trị nhận được, danh sách đã lọc sẵn, người
học không phải gõ lại.

[Khi huỷ] Không có.

#### Ngân hàng câu hỏi phỏng vấn → Chi tiết câu hỏi phỏng vấn

[Điều kiện mở] Bấm liên kết "Xem chi tiết" trong bản xem nhanh ở cột phải.

[Chế độ mở] Chế độ học (`STUDY`) của `USR0402`.

[Thông tin truyền] `questionId` của câu hỏi đang chọn, kèm nguyên trạng thái bộ lọc hiện hành (`topic`,
`q`, `status`, `level`) để màn chi tiết dựng được đường quay lại đúng danh sách đã lọc. `level` là một mã độ khó
(một giá trị), mã không khớp mã nào trong danh mục thì bỏ qua như `topic` (Câu hỏi mở Q10, đã chốt 2026-10-18).

[Giá trị trả về] Không có giá trị trả về tường minh. Khi quay lại, màn này tải lại trạng thái
tự chấm — vì người học có thể đã tự chấm hoặc nộp một lượt luyện ở màn chi tiết.

[Khi thành công] Mở màn `interview_question_detail`.

[Khi huỷ] Không có.

#### Ngân hàng câu hỏi phỏng vấn → Phỏng vấn giả lập

[Điều kiện mở] Bấm nút "Vào phiên giả lập" ở đầu trang
[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:110]. Ở bản dựng, nút là một **liên kết điều hướng thật** (`Button asChild` bọc `Link`, biểu tượng mic) tới `/mock-interview`, luôn bấm được, không còn bị khoá (2026/10/20, chưa duyệt hình)
[Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:320-325].

[Chế độ mở] Không có.

[Thông tin truyền] Không có. Màn này **không** truyền danh sách câu hỏi đã chọn sang `mock_interview` —
xem Câu hỏi mở Q5 (đã chốt 2026-10-18: không có nút "Thêm vào phiên giả lập tới", nên không có gì để truyền).

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `mock_interview` (`USR0302`), thuộc Bounded Context `ai-review` (F5).

[Khi huỷ] Không có.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Khung điều hướng khu Người học<br/>mục nav 5"] -->|"chọn Câu hỏi phỏng vấn"| main["Ngân hàng câu hỏi phỏng vấn<br/>interview_bank_list<br/>(browse / drill)"]
    review["Phân tích bài giải<br/>solution_review"] -->|"gợi ý ôn tập kèm topic, q"| main
    main -->|"Xem chi tiết + questionId"| detail["Chi tiết câu hỏi phỏng vấn<br/>interview_question_detail"]
    detail -->|"quay lại, tải lại trạng thái tự chấm"| main
    main -->|"Vào phiên giả lập"| mock["Phỏng vấn giả lập<br/>mock_interview"]

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000

    class nav,review,detail,mock source
    class main screen
```

[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:108-111; 01-rd/screens/users/USR0401_interview_bank_list.md:116,118,131; 02-bd/screens/users/_shell.md:60]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho người học duyệt toàn bộ ngân hàng câu hỏi phỏng vấn lý thuyết dùng chung của hệ
thống, tìm kiếm và lọc theo chủ đề, độ khó và trạng thái ôn tập, xem nhanh gợi ý trả lời của một câu hỏi mà
không rời danh sách, tự chấm mức độ nhớ để hệ thống xếp lịch ôn lại, và luyện nhanh dạng thẻ
[Nguồn: 01-rd/req/F6_interview-bank.md:8,34-40; 01-rd/screens/users/USR0401_interview_bank_list.md:14-16].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn, hệ thống tải song song ba nhóm dữ liệu — danh mục chủ đề, danh mục độ khó (đều đọc từ dữ liệu) và trang đầu
   danh sách câu hỏi `ACTIVE`. Màn **không còn** dải bốn chỉ số ôn tập (đã bỏ 2026-10-18, owner; RD đã sửa theo, Câu hỏi mở Q11 đã chốt). Trong lúc chờ, mỗi khu vực hiển
   thị khung chờ đúng số dòng dự kiến. Có tham số `topic`/`q`/`status`/`level` trên URL thì áp bộ lọc trước khi gọi danh
   sách.
2. **Lọc và tìm kiếm**: gõ từ khoá, chọn tab trạng thái ôn tập, chọn chip chủ đề, chọn tab độ khó. Bốn bộ lọc cộng dồn
   theo phép `VÀ`, cộng công tắc "Chỉ câu đã đánh dấu" là trục độc lập với bốn tab trạng thái (không phải tab thứ năm, Câu hỏi mở Q8)
   [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:118-145]. Prototype `09-layoutBase` chỉ có ba bộ lọc đầu
   [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:343-351]; bộ lọc độ khó là phần bổ sung đã duyệt 2026-10-03, bằng chứng là bản dựng Next.js.
3. **Xem nhanh**: bấm một dòng thì cột phải hiển thị bản rút gọn của câu hỏi đó — tiêu đề đầy đủ, ý cần
   nói, từ khoá cốt lõi. Không rời màn.
4. **Tự chấm mức độ nhớ (F6-12)**: chọn một trong ba mức ngay trong bản xem nhanh. Hệ thống ghi lại và
   tính lại lịch ôn lại đồng bộ, không qua job nền. **Hai tên riêng, không dùng lẫn (IB-03, `DEC-2026-1022-interview-bank-schema-sync` mục 2):** **"đang ôn"** = lần tự chấm gần nhất là `VAGUE` hoặc `FORGOTTEN` (tab lọc và nhãn trạng thái của màn này); **"đến hạn"** = `next_review_at <= now()` bất kể mức (danh sách F6-09 và thứ tự ưu tiên của Luyện nhanh); hai tập giao nhau nhưng không chứa nhau, ví dụ câu `KNOWN` quá hạn là "đến hạn" nhưng vẫn là "đã thuộc" ở tab (`02-bd/database/F6_interview-bank.md:194-201`)
   [Nguồn: 02-bd/architecture/F6_interview-bank.md:256-259]. Ngay dưới khối tự chấm hiện một dòng "Sẽ nhắc ôn lại sau {n} ngày" lấy từ
   `next_review_at` (Câu hỏi mở Q7, đã chốt 2026-10-18).
5. **Đánh dấu để xem lại (F6-03)**: bật hoặc tắt đánh dấu trên từng dòng câu hỏi.
6. **Luyện nhanh**: bấm "Luyện nhanh", màn chuyển sang trạng thái `drill` — lần lượt từng thẻ, hiện tiêu
   đề trước, người học tự nhớ rồi mới bấm hiện ý cần nói, tự chấm và sang thẻ kế tiếp. Bấm "Thoát" thì
   quay lại `browse` với đúng bộ lọc trước đó. Bộ thẻ lấy từ danh sách đã lọc, nên tab độ khó đang chọn cũng thu hẹp bộ thẻ; phiên luyện dài cố định 10 thẻ (hằng số cấu hình, Câu hỏi mở Q9)
   [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:151-164].
7. **Khoan sâu**: cần nội dung đầy đủ (khung trả lời chuẩn, Chế độ luyện có AI chấm) thì mở màn
   `interview_question_detail`.

[Người dùng] Người học đã đăng nhập (`STUDENT`). Toàn bộ trạng thái ôn tập và đánh dấu đều gắn
với một người dùng cụ thể, nên màn không có chế độ xem ẩn danh — xem Câu hỏi mở Q1.

[Tệp liên quan] Không có. Màn này không xuất hay nhập tệp.

[Phạm vi]
- **Không** có Chế độ luyện (soạn câu trả lời, AI chấm — F6-07/F6-08): thuộc `USR0402`
  [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:116].
- **Không** có khung trả lời chuẩn dạng STAR (F6-05): thuộc `USR0402` (cùng nguồn trên).
- **Không** có bộ câu hỏi theo lớp — đã loại khỏi phạm vi
  (`DEC-2026-0828-remove-per-class-interview-set`).
- **Không** có thao tác tạo, sửa, xoá câu hỏi: thuộc `SHR0301`/`SHR0302`, gác bởi Function
  `INTERVIEW_BANK_MANAGEMENT`.
- **Không** hiển thị câu hỏi `status = RETIRED` [Nguồn: 02-bd/database/F6_interview-bank.md:66].
- **Không** lọc câu hỏi thiếu rubric ra khỏi danh sách: câu thiếu rubric vẫn hiện đầy đủ ở Chế độ học,
  chỉ bị ẩn ở Chế độ luyện của màn `USR0402` [Nguồn: 01-rd/req/F6_interview-bank.md:62-63].

[Quyền sử dụng]
- Xem: được, với mọi tài khoản `STUDENT` đã đăng nhập. Toàn bộ ngân hàng là dùng chung cấp hệ thống,
  không chia theo lớp.
- Thêm: chỉ thêm dữ liệu **của chính người học** — một dòng đánh dấu (`bookmarks`) và một dòng trạng thái
  ôn tập (`recall_ratings`). Không thêm câu hỏi.
- Sửa: chỉ ghi đè dòng trạng thái ôn tập của chính mình.
- Xoá: chỉ gỡ đánh dấu của chính mình. Không xoá câu hỏi.

[Số bản ghi tối đa] Danh sách câu hỏi: phân trang cuộn, mỗi lần tải 20 dòng
(`[Suy luận]` — prototype giới hạn vùng cuộn 620px với 9 dòng mẫu và không cài phân trang,
09-layoutBase/Câu hỏi phỏng vấn.dc.html:131-132; 148 câu hỏi trong dữ liệu mẫu là quá nhiều để tải một
lần). Chủ đề: chip "Tất cả" cộng một chip mỗi chủ đề hiện có (prototype vẽ 5). Menu lọc độ khó: "Tất cả" cộng một tab mỗi mức hiện có trong `question_levels` (ban đầu 3 mức, số tab không cố định). Tab trạng thái: đúng 4. Bộ thẻ luyện nhanh: cố định 10
thẻ, là hằng số cấu hình giao diện, không có điều khiển chọn số thẻ (Câu hỏi mở Q9, đã chốt 2026-10-18; prototype `09-layoutBase` để mặc định 10, khoảng 5 tới 20
[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:235]) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:45-47].

### 4.2 DTO liên quan

- `InterviewQuestionSummaryDto` — một dòng trong danh sách cột trái.
- `InterviewQuestionDetailDto` — nội dung bản xem nhanh cột phải và thẻ luyện nhanh; dùng chung với
  `USR0402`.
- `QuestionTopicDto` — một chip chủ đề.
- `QuestionLevelDto` — một tab độ khó.
- ~~`RecallSummaryDto` — bốn chỉ số ôn tập đầu trang.~~ Đã bỏ 2026-10-18, owner.
- `RecallRatingDto` — một lần tự chấm mức độ nhớ.

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/F6_interview-bank.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (5)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `interview_bank.interview_questions` | [Nguồn: 02-bd/database/F6_interview-bank.md:52-76] |
| 2 | `interview_bank.question_topics` | [Nguồn: 02-bd/database/F6_interview-bank.md:8-28] |
| 3 | `interview_bank.recall_ratings` | [Nguồn: 02-bd/database/F6_interview-bank.md:177-205] |
| 4 | `interview_bank.bookmarks` | [Nguồn: 02-bd/database/F6_interview-bank.md:98-109] |
| 5 | `interview_bank.question_levels` | Chỉ đọc: qua join `level_id` để lấy nhãn độ khó, và trực tiếp (`ListQuestionLevels`) để dựng tab lọc độ khó [Nguồn: 02-bd/database/F6_interview-bank.md:30-50] |

Ba bảng còn lại của module **không** dùng ở màn này: `answer_rubrics` và `user_answers` chỉ phục vụ Chế
độ luyện ở `USR0402`; `practice_history` phục vụ `USR0601` (dashboard, F6-10; `my_progress` đã gộp vào đó, IB-04). `recall_ratings` ở màn này chỉ còn phục vụ cột
trạng thái ôn tập, tab lọc trạng thái và hàng đợi Luyện nhanh — xem mục 7.2.

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Câu hỏi phỏng vấn.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải** design
system cuối cùng. Cẩn thận phân biệt với `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html`, là prototype
của màn quản trị `SHR0301` chứ không phải màn này.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Header ngang dính trên (khung chung khu Người học) | `:46-93` | Dùng lại `02-bd/screens/users/_shell.md` mục 2, không mô tả lại |
| Vùng nội dung | `:95` | Một cột giữa trang, chiều rộng tối đa 1400px |
| Hàng tiêu đề và hành động | Prototype `:99-112` (có thêm bốn chỉ số bên trái — **đã bỏ khỏi bản dựng 2026-10-18, owner**) | Bên trái: tiêu đề trang `interviewBankList.title` và phụ đề `interviewBankList.subtitle`; bên phải: hai nút hành động, "Luyện nhanh" (biểu tượng Layers, nút viền) và "Vào phiên giả lập" (liên kết tới `/mock-interview`, biểu tượng mic, nút chính). Hàng xuống dòng khi hẹp (`flex-wrap`, `items-end`, `justify-between`) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:302-327]. **Chưa duyệt hình** (owner 2026/10/20) |
| Thanh công cụ bộ lọc (toàn chiều ngang, phía trên hai cột) | `:117-130` (prototype đặt trong cột trái) | **Thanh lọc dùng chung `FilterBar`** (`shared/ui/data/filter-bar.tsx`, hàng `flex-wrap`, không còn thẻ `glass-card` riêng) chứa: ô tìm kiếm co giãn, menu lọc trạng thái, menu lọc chủ đề, menu lọc độ khó, công tắc "Chỉ câu đã đánh dấu" — thay cho ba hàng lọc nằm trong thẻ danh sách. **Prototype `09-layoutBase` không có menu lọc độ khó và không có công tắc**, bằng chứng là bản dựng Next.js [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:329-383]. **Chưa duyệt hình** (owner 2026/10/20) |
| Cột trái — danh sách câu hỏi | `:131-145` | Thẻ không đệm; đầu thẻ là dòng đếm kết quả "Hiển thị {shown} / {total} câu" (`interviewBankList.shownCount`), dưới là vùng cuộn dọc cao tối đa khoảng `68dvh`; mỗi dòng gồm chủ đề, độ khó, nhãn trạng thái, tiêu đề, nút đánh dấu (component dùng chung `InterviewQuestionListRow`: dòng đang chọn có nền `surface-hover`, các dòng ngăn cách bằng viền dưới [Nguồn: 05-coding/frontend/src/entities/interview-question/ui/interview-question-list-row.tsx:46-53]); có trạng thái rỗng [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:386-417] |
| Cột phải — bản xem nhanh | `:148-188` | Dính trên khi cuộn (`sticky top-4`); chủ đề và độ khó, tiêu đề cỡ lớn in đậm (`text-xl font-bold`), khối "Ý cần nói" (mỗi ý một ô nền nhạt), khối "Từ khoá cốt lõi" có nhãn, khối tự chấm kèm dòng ngày ôn lại và liên kết "Xem chi tiết" [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:419-499] |
| Trạng thái `drill` | `:193-229` | Thay toàn bộ vùng nội dung: thanh tiến độ, thẻ câu hỏi, hai nút chấm |
| Chân trang (khung chung khu Người học) | Không có trong prototype này | Vẫn dựng theo `02-bd/screens/users/_shell.md` mục 3 — quyết định nhất quán ba khu, không phải kết luận rút từ prototype |

Bố cục hai cột `1fr 1fr` (`:114`) ở prototype; **bản dựng nay dùng tỉ lệ 5:6** (`lg:[grid-template-columns:minmax(0,5fr)_minmax(0,6fr)]`, cột chi tiết rộng hơn cột danh sách, dưới `lg` xếp một cột) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:385]. Tỉ lệ 5:6 và các thay đổi bố cục ở trên là **yêu cầu của owner 2026/10/20, chưa duyệt hình** (Câu hỏi mở Q12). Trạng thái `drill` thu về một cột hẹp `max-width: 780px` căn giữa (`:194`) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:226]; chế độ này không đổi. Không quy định màu sắc, khoảng cách hay typography ở BD.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/users/interview-bank-list` | Quy ước FSD của dự án |
| Khung khu Người học | Dùng lại `widgets/student-shell` | `02-bd/screens/users/_shell.md` |
| ~~Thanh chỉ số~~ | ~~`widgets/recall-summary-bar` + `entities/recall-rating`~~ — đã bỏ 2026-10-18, owner; bản dựng không có widget này (hàng tiêu đề và hành động viết trực tiếp trong view, [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:302-327]) | Không còn căn cứ |
| Bộ lọc | `features/interview-question-filter` + `entities/question-topic` + `entities/question-level` | Prototype `:117-130`; bản dựng nay dùng `FilterBar` chung (ô tìm kiếm) cùng ba `FilterMenu` và `Toggle` làm phần tử con, kèm menu độ khó và công tắc [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:329-383] |
| Danh sách | `widgets/interview-question-list` + `entities/interview-question` | Prototype `:131-145` |
| Bản xem nhanh | `widgets/interview-question-quick-view` | Prototype `:148-188` |
| Tự chấm | `features/rate-question-recall` | Prototype `:180-184` |
| Đánh dấu | `features/bookmark-question` | RD REQ-01 (F6-03), prototype không có |
| Luyện nhanh | `features/question-drill` | Prototype `:193-229` |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại. `entities/interview-question`,
`entities/question-topic` và `features/rate-question-recall` dùng chung với `USR0402`, không nhân bản.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/users/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Hàng hành động

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Hàng hành động | | | | | | | | | | | | | |
| | 1 | Luyện nhanh | `interviewBankList.header.btnStartDrill` | - | - | Button | - | - | - | I | - | `Luyện nhanh {số} câu` | Chuyển màn sang trạng thái `drill`<br>[Nguồn giá trị] Số thẻ cố định 10, là hằng số cấu hình giao diện, không có điều khiển chọn số thẻ (Câu hỏi mở Q9, đã chốt 2026-10-18); nhãn hiển thị số nhỏ hơn giữa 10 và số câu khớp bộ lọc [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:45-47,318]. Prototype `09-layoutBase` để mặc định 10, khoảng 5 tới 20 [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:235]<br>[EVT liên quan] EVT-11 |
| | 2 | Vào phiên giả lập | `interviewBankList.header.btnMockInterview` | - | - | Link | - | - | - | I | - | `Vào phiên giả lập` | Liên kết điều hướng tới `/mock-interview` (màn `USR0302`), kiểu nút chính, kèm biểu tượng mic; **luôn bấm được**<br>[Nguồn giá trị] Nhãn tĩnh i18n `interviewBankList.startMockInterview` [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:320-325; 05-coding/frontend/messages/vi.json:1928]<br>[EVT liên quan] EVT-10 |
| | 3 | Tiêu đề trang | `interviewBankList.title` | - | - | Label | String | - | - | O | `Ngân hàng câu hỏi` | - | Tiêu đề `h1` của màn ở trạng thái `browse`, đặt bên trái hàng đầu trang<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:304; 05-coding/frontend/messages/vi.json:1974]. Yêu cầu của owner 2026/10/20, **chưa duyệt hình**<br>[EVT liên quan] - |
| | 4 | Phụ đề trang | `interviewBankList.subtitle` | - | - | Label | String | - | - | O | `Ôn lý thuyết phỏng vấn theo chủ đề, tự chấm mức nhớ và luyện nhanh bằng thẻ.` | - | Câu mô tả ngắn dưới tiêu đề<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:305-307; 05-coding/frontend/messages/vi.json:1975]. **Chưa duyệt hình**<br>[EVT liên quan] - |

Đã bỏ 2026-10-18, owner: bốn item chỉ số (Tổng câu hỏi `interviewBankList.stats.totalQuestions`, Đã thuộc `...stats.known`, Đang ôn `...stats.reviewing`, Ôn trong tuần `...stats.drilledThisWeek`) cùng khỏi Sheet 5, 6, 7. Các NO còn lại đã đánh lại: Luyện nhanh NO 1, Vào phiên giả lập NO 2 (trước là NO 5, NO 6); tiêu đề và phụ đề trang thêm cuối khu vực là NO 3, NO 4 (2026/10/21, không đổi số NO cũ). Bản dựng: hàng đầu trang gồm khối tiêu đề bên trái và nhóm hai nút bên phải [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:302-327]. Nút "Vào phiên giả lập" **đã là liên kết thật** tới `/mock-interview` từ 2026/10/20 (trước đó là nút `disabled` kèm ghi chú `startMockInterviewUnavailable`; khoá này không còn được dùng, ghi nợ ở "Hiện trạng bản dựng") [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:320-325].

### Khu vực B — Bộ lọc

Từ 2026/10/20 (owner, **chưa duyệt hình**) năm item của khu vực này (ô tìm kiếm, ba menu lọc, công tắc) nằm chung trong **một thanh lọc nằm ngang dựng bằng `FilterBar` chung** phía trên hai cột, thứ tự trái sang phải đúng NO 1 tới NO 5; không còn ba hàng lọc nằm trong thẻ danh sách [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:329-383]. Chỉ đổi vị trí, không đổi hành vi hay NO.

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bộ lọc | | | | | | | | | | | | | |
| | 1 | Ô tìm kiếm | `interviewBankList.filter.searchBox` | `interview_questions` | `title`, `content_markdown`, `core_keywords` | TextBox | String | 100 | - | I/O | Rỗng, hoặc tham số `q` trên URL | - | Tìm theo tiêu đề, nội dung và từ khoá cốt lõi (F6-02)<br>[Nguồn giá trị] Người học nhập; giá trị khởi tạo lấy từ tham số `q` khi đến từ `solution_review`<br>[EVT liên quan] EVT-2 |
| | 2 | Menu lọc trạng thái ôn tập | `interviewBankList.filter.statusTabs` | `recall_ratings` | `rating` | List | Enum | - | - | I/O | `Tất cả` | - | Bốn lựa chọn trong menu thả xuống: Tất cả / Chưa xem / Đang ôn / Đã thuộc [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:305-310]<br>[Công thức] `Chưa xem` là **không có** dòng `recall_ratings` cho cặp người dùng và câu hỏi; `Đang ôn` là `rating IN ('VAGUE','FORGOTTEN')`; `Đã thuộc` là `rating = 'KNOWN'`. "Chưa xem" chỉ là **giá trị của bộ lọc và của nhãn trạng thái**, **không bao giờ** là một lựa chọn tự chấm (ba nút tự chấm ở Khu vực D NO 5 và E NO 8 là `KNOWN`/`VAGUE`/`FORGOTTEN`). Đã chốt 2026-10-18 (owner), Câu hỏi mở Q6 [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:339-352]<br>[EVT liên quan] EVT-3 |
| | 3 | Menu lọc chủ đề | `interviewBankList.filter.topicChips` | `question_topics` | `code`, `display_name`, `sort_order` | List | List | - | - | I/O | `Tất cả` | - | Chip "Tất cả" cộng một chip cho mỗi chủ đề trong `question_topics`, sắp theo `sort_order`; số chip không cố định vì ADMIN thêm/xoá chủ đề được (`DEC-2026-1001-admin-configurable-settings`)<br>[Nguồn giá trị] Kết quả gọi `ListQuestionTopics`; nhãn lấy `display_name`, giá trị lọc lấy `code`. Giá trị khởi tạo lấy từ tham số `topic` khi đến từ `solution_review`<br>[EVT liên quan] EVT-1, EVT-4 |
| | 4 | Menu lọc độ khó | `interviewBankList.filter.levelTabs` | `question_levels` | `code`, `display_name`, `sort_order` | List | List | - | - | I/O | `Tất cả` | - | Tab "Tất cả" cộng một tab cho mỗi mức trong `question_levels`, sắp theo `sort_order`, đặt sau chip chủ đề; số tab không cố định vì ADMIN thêm/xoá mức được (`DEC-2026-1001-admin-configurable-settings` mục 6). Chọn một mức tại một thời điểm, cộng dồn `VÀ` với ba bộ lọc còn lại<br>[Nguồn giá trị] Kết quả gọi `ListQuestionLevels`; nhãn lấy `display_name`, giá trị lọc lấy `code` (khớp `interview_questions.level_id`). Giá trị khởi tạo lấy từ tham số `level` trên URL (một mã độ khó; không khớp mã nào trong danh mục thì bỏ qua và giữ "Tất cả", như `topic`); `solution_review` không gửi tham số này, chỉ có khi quay lại từ `USR0402`. Đã chốt 2026-10-18 (owner), Câu hỏi mở Q10. **Bản dựng chưa đọc URL cho bất kỳ bộ lọc nào** (xem "Hiện trạng bản dựng") [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:90-95,365-376]<br>[EVT liên quan] EVT-1, EVT-15 |
| | 5 | Công tắc "Chỉ câu đã đánh dấu" | `interviewBankList.filter.bookmarkedOnly` | `bookmarks` | `user_id`, `question_id` | Toggle | Boolean | - | - | I/O | Tắt | Bật / Tắt | Lọc danh sách chỉ còn câu hỏi người học đã đánh dấu (F6-03). Đứng cạnh bốn tab trạng thái nhưng là **trục độc lập**, không phải tab thứ năm: đánh dấu và trạng thái ôn tập lọc chéo được, cộng dồn `VÀ` với bốn bộ lọc còn lại. Đã chốt 2026-10-18 (owner), Câu hỏi mở Q8<br>[Nguồn giá trị] Câu hỏi có dòng `bookmarks` khớp `(user_id, question_id)` thì qua bộ lọc; truyền lên `ListInterviewQuestions` dưới dạng cờ `bookmarkedOnly` [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:95,124,377-382]<br>[EVT liên quan] EVT-16 |

### Khu vực C — Danh sách câu hỏi

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Danh sách câu hỏi | | | | | | | | | | | | | |
| | 1 | Danh sách câu hỏi | `interviewBankList.list` | `interview_questions` | - | List | List | - | - | O | Rỗng | - | Danh sách câu hỏi `ACTIVE` khớp cả bốn bộ lọc (tìm kiếm, trạng thái, chủ đề, độ khó) và công tắc "Chỉ câu đã đánh dấu" khi đang Bật, cuộn tải thêm 20 dòng mỗi lần<br>[Nguồn giá trị] Kết quả gọi `ListInterviewQuestions`<br>[EVT liên quan] EVT-1, EVT-2, EVT-3, EVT-4, EVT-5, EVT-15, EVT-16 |
| | 2 | Chủ đề | `interviewBankList.list.col.topic` | `question_topics` | `display_name` | ListColumn | String | - | - | O | - | Chữ hoa | Chủ đề của câu hỏi, lấy qua `interview_questions.topic_id`<br>[Nguồn giá trị] Cột `question_topics.display_name`<br>[EVT liên quan] - |
| | 3 | Độ khó | `interviewBankList.list.col.difficulty` | `question_levels`, `interview_questions` | `display_name` | ListColumn | String | - | - | O | - | Nhãn `display_name`, badge | Mức độ khó của câu hỏi (F6-01), lấy qua `interview_questions.level_id`<br>[Nguồn giá trị] Cột `question_levels.display_name`, **đọc từ dữ liệu** do ADMIN quản lý ở `SHR0301`, không đi qua khoá i18n (khoá `...col.difficulty` chỉ còn là nhãn tiêu đề cột nếu có). Ba dòng seed `EASY`/`MEDIUM`/`HARD` ("Dễ"/"Trung bình"/"Khó") chỉ là dữ liệu thường. Màu badge: dòng seed giữ màu cũ, dòng ADMIN thêm mới dùng màu trung tính — **không có cột màu** trong `question_levels`, nên màu là quy ước của giao diện, không phải dữ liệu [Nguồn: 05-coding/frontend/src/entities/interview-question/ui/interview-question-list-row.tsx:36,57]<br>[EVT liên quan] - |
| | 4 | Trạng thái ôn tập | `interviewBankList.list.col.recallStatus` | `recall_ratings` | `rating` | Badge | Enum | - | - | O | `Chưa xem` | Nhãn tiếng Việt | Trạng thái ôn tập của **chính người học** với câu hỏi đó<br>[Công thức] Không có dòng `recall_ratings` thành "Chưa xem"; `KNOWN` thành "Đã thuộc"; `VAGUE` và `FORGOTTEN` thành "Đang ôn"<br>[EVT liên quan] EVT-8, EVT-13 |
| | 5 | Tiêu đề câu hỏi | `interviewBankList.list.col.title` | `interview_questions` | `title` | ListColumn | String | - | - | O | - | - | Tiêu đề câu hỏi, hiển thị tối đa hai dòng<br>[Nguồn giá trị] Cột `title`<br>[EVT liên quan] EVT-6 |
| | 6 | Đánh dấu xem lại | `interviewBankList.list.col.btnBookmark` | `bookmarks` | `user_id`, `question_id` | Toggle | Boolean | - | - | I/O | Tắt | Bật / Tắt | Đánh dấu câu hỏi để xem lại (F6-03). **Prototype không có điều khiển này** — BD bổ sung vì RD đưa F6-03 vào phạm vi màn [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:126]<br>[Nguồn giá trị] Có dòng `bookmarks` khớp `(user_id, question_id)` thì Bật<br>[EVT liên quan] EVT-7 |
| | 7 | Trạng thái rỗng | `interviewBankList.list.emptyState` | - | - | Label | String | - | - | O | Không có câu hỏi khớp bộ lọc. | - | Câu thông báo khi bộ lọc không khớp dòng nào<br>[Nguồn giá trị] Nhãn tĩnh i18n [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:143]<br>[EVT liên quan] - |
| | 8 | Dòng đếm kết quả | `interviewBankList.shownCount` | - | - | Label | String | - | - | O | `Hiển thị 0 / 0 câu` | `Hiển thị {shown} / {total} câu` | Dòng đầu thẻ danh sách cho biết số câu đang hiển thị trên tổng số câu<br>[Công thức] `shown` là số câu khớp bộ lọc hiện hành (kể cả câu đang được ghim, EVT-8); `total` là tổng câu của nguồn dữ liệu. Ở bản dựng `total` là số câu của mảng mock đã tải [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:387-392; 05-coding/frontend/messages/vi.json:1976]. Khi có phân trang máy chủ, `total` phải là tổng số bản ghi `ACTIVE` do `ListInterviewQuestions` trả về, **không** phải số dòng đã tải (Câu hỏi mở Q13)<br>[EVT liên quan] EVT-1, EVT-2, EVT-3, EVT-4, EVT-15, EVT-16 |

### Khu vực D — Bản xem nhanh

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bản xem nhanh | | | | | | | | | | | | | |
| | 1 | Chủ đề và độ khó | `interviewBankList.quickView.meta` | `question_topics`, `question_levels` | `display_name`, `display_name` | Label | String | - | - | O | - | `{chủ đề} · {độ khó}` | Nhãn phân loại của câu hỏi đang chọn; độ khó hiển thị dạng chữ thường, không badge<br>[Nguồn giá trị] Hai cột `display_name` đã nêu (chủ đề qua `topic_id`, độ khó qua `level_id`). Prototype còn ghép thêm mã dạng `IQ-014` [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:406] — **không có cột tương ứng**, BD bỏ khỏi thiết kế, đã chốt 2026-10-18 (owner), không thêm `display_code` — Câu hỏi mở Q3. Bản dựng đúng như vậy [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:422-429]<br>[EVT liên quan] EVT-6 |
| | 2 | Tiêu đề đầy đủ | `interviewBankList.quickView.title` | `interview_questions` | `title` | Label | String | - | - | O | - | - | Tiêu đề câu hỏi đang chọn, không cắt dòng; hiển thị cỡ lớn, in đậm (`text-xl font-bold`) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:430-432]<br>[Nguồn giá trị] Cột `title`<br>[EVT liên quan] EVT-6 |
| | 3 | Ý cần nói | `interviewBankList.quickView.outline` | `interview_questions` | `suggested_approach` | List | String | - | - | O | - | Danh sách đánh số hai chữ số | Gợi ý hướng tiếp cận (F6-04), hiển thị dạng danh sách đánh số, mỗi ý nằm trong một ô nền nhạt (`bg-surface-hover`, bo góc) dưới nhãn khối `interviewBankList.quickView.outlineLabel` "Ý cần nói" [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:434-453]<br>[Nguồn giá trị] Cột `suggested_approach`, kiểu TEXT dạng Markdown — mỗi mục danh sách trong Markdown thành một dòng [Nguồn: 02-bd/database/F6_interview-bank.md:62]<br>[EVT liên quan] EVT-6 |
| | 4 | Từ khoá cốt lõi | `interviewBankList.quickView.keywords` | `interview_questions` | `core_keywords` | List | List | - | - | O | Rỗng | Chip | Từ khoá kỹ thuật cốt lõi cần nêu (F6-06), hiển thị dưới nhãn khối **"Từ khoá cốt lõi"** (`interviewBankList.quickView.keywordsLabel`, thêm 2026/10/20, chưa duyệt hình) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:455-468; 05-coding/frontend/messages/vi.json:1956]<br>[Nguồn giá trị] Cột `core_keywords` [Nguồn: 02-bd/database/F6_interview-bank.md:64]. Prototype gọi khối này là "tag" với nội dung lẫn cả bối cảnh phỏng vấn ("Hỏi ở vòng 1") [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:244] — đã chốt 2026-10-18 (owner): dùng `core_keywords` thay tag, không thêm bảng tag, nhãn bối cảnh là dữ liệu mẫu — Câu hỏi mở Q4. Bản dựng render chip từ `coreKeywords` [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:460-466]<br>[EVT liên quan] EVT-6 |
| | 5 | Mức độ nắm | `interviewBankList.quickView.recallLevels` | `recall_ratings` | `rating` | List | Enum | - | - | I/O | Mức hiện hành, không có thì không chọn mức nào | - | Ba nút tự chấm mức độ nhớ (F6-12): Biết rõ / Mơ hồ / Quên<br>[Nguồn giá trị] Enum `rating` của `recall_ratings` [Nguồn: 02-bd/database/F6_interview-bank.md:184]. Prototype `09-layoutBase` dùng ba nhãn khác (Chưa xem / Đang ôn / Đã thuộc) [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:413-418] — BD dùng ba mức của F6-12, **do người học tự chọn, không có AI tham gia** (đã chốt 2026-10-18, owner, Câu hỏi mở Q6). Bản dựng đúng như vậy [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:214-220,474-480]<br>[EVT liên quan] EVT-8 |
| | 6 | Xem chi tiết | `interviewBankList.quickView.linkDetail` | - | - | Link | - | - | - | I | - | - | Mở màn `interview_question_detail` (`USR0402`) cho câu hỏi đang chọn. **Prototype không có liên kết này** — BD bổ sung vì cấu trúc hai màn được chốt sau khi prototype dựng xong [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:116]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-9 |
| | 7 | Dòng ngày ôn lại kế tiếp | `interviewBankList.quickView.nextReview` | `recall_ratings` | `next_review_at` | Label | String | - | - | O | Ẩn | `Sẽ nhắc ôn lại sau {n} ngày` | Hiện ngay dưới khối "Mức độ nắm" (NO 5) khi câu hỏi đang chọn đã có mức tự chấm; cho người học biết lần tự chấm vừa rồi có tác dụng gì. Đã chốt 2026-10-18 (owner), Câu hỏi mở Q7<br>[Nguồn giá trị] `n` là số ngày từ bây giờ tới `recall_ratings.next_review_at` máy chủ trả về trong `RecallRatingDto` (Sheet 7.1 NO 12). **Prototype Next.js tra bảng hằng số** theo mức tự chấm (Quên 1 ngày, Mơ hồ 3 ngày, Biết rõ 7 ngày), **đúng bảng cố định đã chốt ở DB** (IB-16); chỉ khác là chưa có máy chủ nên `n` không lấy từ `next_review_at` thật [Nguồn: 05-coding/frontend/src/entities/interview-question/model/types.ts:62-71; 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:481-487]<br>[EVT liên quan] EVT-8 |

### Khu vực E — Trạng thái luyện nhanh

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Trạng thái luyện nhanh | | | | | | | | | | | | | |
| | 1 | Bộ đếm thẻ | `interviewBankList.drill.counter` | - | - | Label | String | - | - | O | `Thẻ 1 / {tổng}` | `Thẻ {n} / {tổng}` | Vị trí thẻ hiện tại trong phiên luyện<br>[Công thức] Số thứ tự thẻ đang xem trên tổng số thẻ của phiên [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:424]<br>[EVT liên quan] EVT-11, EVT-13 |
| | 2 | Thanh tiến độ | `interviewBankList.drill.progressBar` | - | - | ProgressBar | Number | - | - | O | 0 | Phần trăm | Tiến độ của phiên luyện<br>[Công thức] Số thứ tự thẻ chia tổng số thẻ, làm tròn phần trăm [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:425]<br>[EVT liên quan] EVT-11, EVT-13 |
| | 3 | Thoát | `interviewBankList.drill.btnExit` | - | - | Button | - | - | - | I | - | - | Quay lại trạng thái `browse`, giữ nguyên bộ lọc trước khi vào luyện<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-14 |
| | 4 | Phân loại thẻ | `interviewBankList.drill.cardMeta` | `question_topics`, `question_levels` | `display_name`, `display_name` | Label | String | - | - | O | - | `{chủ đề} · {độ khó}` | Nhãn phân loại của thẻ đang xem; độ khó là chữ thường<br>[Nguồn giá trị] Hai cột `display_name` đã nêu (qua `topic_id` và `level_id`)<br>[EVT liên quan] EVT-13 |
| | 5 | Tiêu đề thẻ | `interviewBankList.drill.cardTitle` | `interview_questions` | `title` | Label | String | - | - | O | - | - | Tiêu đề câu hỏi trên thẻ, căn giữa<br>[Nguồn giá trị] Cột `title`<br>[EVT liên quan] EVT-13 |
| | 6 | Ý cần nói trên thẻ | `interviewBankList.drill.cardOutline` | `interview_questions` | `suggested_approach` | List | String | - | - | O | Ẩn | Danh sách đánh số hai chữ số | Cùng nội dung với bản xem nhanh, **không** kèm mục nào khác<br>[Nguồn giá trị] Cột `suggested_approach`<br>[EVT liên quan] EVT-12 |
| | 7 | Hiện hoặc ẩn ý cần nói | `interviewBankList.drill.btnReveal` | - | - | Button | - | - | - | I | - | `Xem ý cần nói` / `Ẩn đáp án` | Đảo trạng thái lộ đáp án của thẻ<br>[Nguồn giá trị] Nhãn tĩnh i18n, đổi theo trạng thái [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:432]<br>[EVT liên quan] EVT-12 |
| | 8 | Tự chấm trên thẻ | `interviewBankList.drill.recallLevels` | `recall_ratings` | `rating` | List | Enum | - | - | I | - | - | Ba nút tự chấm giống khu vực D, bấm xong chuyển sang thẻ kế tiếp. Prototype `09-layoutBase` chỉ có hai nút ("Cần ôn lại", "Đã thuộc") [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:223-224]; đã chốt 2026-10-18 (owner, Q6): luyện nhanh dùng **đủ ba nút** để một lần bấm luôn xác định được `interval_days`. Bản dựng dùng cùng `RecallLevelPicker` với bản xem nhanh [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:288-293]<br>[Nguồn giá trị] Enum `rating` của `recall_ratings`<br>[EVT liên quan] EVT-13 |

[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:99-112,117-130,131-145,148-188,193-229; 02-bd/database/F6_interview-bank.md:8-28,52-76,98-109,177-205; 01-rd/screens/users/USR0401_interview_bank_list.md:44-73]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Hàng hành động

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Hàng hành động | | | | |
| | 1 | Luyện nhanh | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải danh sách, và không kích hoạt khi bộ lọc hiện hành (kể cả tab độ khó) không khớp câu hỏi nào. |
| | 2 | Vào phiên giả lập | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`.<br>[Điều kiện kích hoạt] Luôn kích hoạt: là liên kết điều hướng, không phụ thuộc việc tải danh sách hay bộ lọc. |
| | 3 | Tiêu đề trang | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`; trạng thái `drill` thay toàn bộ vùng nội dung và không có tiêu đề này [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:222-226,302-308]. |
| | 4 | Phụ đề trang | Điều kiện | [Điều kiện hiển thị] Như NO 3. |

Bốn item chỉ số đã bỏ 2026-10-18, owner (xem Sheet 5 khu vực A). Nút "Vào phiên giả lập" **luôn kích hoạt** từ 2026/10/20 (liên kết tới `/mock-interview`), không còn trạng thái bị khoá.

### Khu vực B — Bộ lọc

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bộ lọc | | | | |
| | 1 | Ô tìm kiếm | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`.<br>[Tự động đặt] Có tham số `q` trên URL thì đặt sẵn giá trị đó ngay khi khởi tạo màn, trước khi gọi danh sách. |
| | 2 | Menu lọc trạng thái ôn tập | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`. Luôn đủ 4 tab, không ẩn tab nào kể cả khi không có câu hỏi ở trạng thái đó. |
| | 3 | Menu lọc chủ đề | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`. Trong lúc tải danh mục chủ đề hiển thị khung chờ cho dải chip (số chip theo danh mục hiện có, mặc định giả định 6 khi chưa tải).<br>[Tự động đặt] Có tham số `topic` trên URL thì chọn sẵn chip tương ứng; mã chủ đề không khớp chủ đề nào trong danh mục đã tải thì bỏ qua tham số và giữ "Tất cả". |
| | 4 | Menu lọc độ khó | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`. Trong lúc tải danh mục độ khó hiển thị khung chờ; danh mục tải lỗi hoặc rỗng thì chỉ còn tab "Tất cả" và danh sách vẫn xem được.<br>[Tự động đặt] Mặc định "Tất cả" mỗi lần vào màn. Mức đang chọn bị ADMIN xoá giữa chừng thì lần tải danh mục kế tiếp đưa tab về "Tất cả" `[SoT: Suy luận]` — bản dựng chưa xử lý tình huống này vì dữ liệu mẫu cố định. |
| | 5 | Công tắc "Chỉ câu đã đánh dấu" | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`, cạnh bốn tab trạng thái.<br>[Tự động đặt] Mặc định Tắt mỗi lần vào màn. Gỡ đánh dấu một câu khi công tắc đang Bật thì dòng đó **vẫn ở lại** trong danh sách cho tới khi người học đổi bộ lọc hoặc ô tìm kiếm (EVT-7, EVT-8). |

### Khu vực C — Danh sách câu hỏi

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Danh sách câu hỏi | | | | |
| | 1 | Danh sách câu hỏi | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`. Trong lúc tải trang đầu hiển thị khung chờ đúng 9 dòng. |
| | 2 | Chủ đề | Có | - |
| | 3 | Độ khó | Có | - |
| | 4 | Trạng thái ôn tập | Có | [Tự động đặt] Đổi ngay sau khi người học tự chấm câu hỏi đó, kể cả khi tự chấm từ bản xem nhanh hoặc từ thẻ luyện nhanh. |
| | 5 | Tiêu đề câu hỏi | Có | - |
| | 6 | Đánh dấu xem lại | Có | [Điều kiện kích hoạt] Kích hoạt sau khi tải xong dòng đó; trong lúc chờ phản hồi của một lần bật hoặc tắt thì không kích hoạt lại chính nút đó. |
| | 7 | Trạng thái rỗng | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi đã tải xong và không có dòng nào khớp bộ lọc; khi đó ẩn toàn bộ vùng danh sách (dòng đếm kết quả ở đầu thẻ vẫn hiện, ghi 0 câu) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:387-395]. |
| | 8 | Dòng đếm kết quả | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse`, luôn hiện ở đầu thẻ danh sách kể cả khi kết quả rỗng.<br>[Tự động đặt] Cập nhật ngay mỗi khi bộ lọc, ô tìm kiếm hoặc việc ghim câu vừa thao tác (EVT-8) làm đổi số dòng. |

### Khu vực D — Bản xem nhanh

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bản xem nhanh | | | | |
| | 1 | Chủ đề và độ khó | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `browse` và khi đã có một câu hỏi được chọn. Danh sách rỗng thì cả khối xem nhanh hiển thị câu hướng dẫn chọn câu hỏi. |
| | 2 | Tiêu đề đầy đủ | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 3 | Ý cần nói | Điều kiện | [Điều kiện hiển thị] Như NO 1; câu hỏi có `suggested_approach` rỗng thì ẩn cả khối, không hiển thị tiêu đề khối trống. |
| | 4 | Từ khoá cốt lõi | Điều kiện | [Điều kiện hiển thị] Như NO 1; `core_keywords` rỗng thì ẩn cả khối, gồm cả nhãn "Từ khoá cốt lõi" [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:455-468]. |
| | 5 | Mức độ nắm | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang chờ phản hồi của lần tự chấm trước.<br>[Tự động đặt] Mức đang lưu được chọn sẵn; chấm lại thì mức mới thay mức cũ, không cộng dồn. |
| | 6 | Xem chi tiết | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 7 | Dòng ngày ôn lại kế tiếp | Điều kiện | [Điều kiện hiển thị] Như NO 1, và chỉ khi câu hỏi đang chọn đã có dòng `recall_ratings` (vừa tự chấm, hoặc đã có mức từ trước); chưa tự chấm thì ẩn.<br>[Tự động đặt] Cập nhật ngay sau mỗi lần tự chấm, lấy giá trị máy chủ trả về. |

### Khu vực E — Trạng thái luyện nhanh

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Trạng thái luyện nhanh | | | | |
| | 1 | Bộ đếm thẻ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`. |
| | 2 | Thanh tiến độ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`.<br>[Tự động đặt] Cập nhật cùng lúc với bộ đếm thẻ. |
| | 3 | Thoát | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`. |
| | 4 | Phân loại thẻ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`. |
| | 5 | Tiêu đề thẻ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`. |
| | 6 | Ý cần nói trên thẻ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Xem ý cần nói" trên chính thẻ đang xem.<br>[Tự động đặt] Sang thẻ kế tiếp thì tự thu lại, thẻ mới luôn bắt đầu ở trạng thái chưa lộ đáp án. |
| | 7 | Hiện hoặc ẩn ý cần nói | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`.<br>[Tự động đặt] Nhãn đổi giữa "Xem ý cần nói" và "Ẩn đáp án" theo trạng thái lộ đáp án. |
| | 8 | Tự chấm trên thẻ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `drill`.<br>[Điều kiện kích hoạt] Kích hoạt kể cả khi chưa lộ đáp án — người học được quyền tự chấm mà không cần xem gợi ý.<br>[Tự động đặt] Bấm xong thì tự chuyển sang thẻ kế tiếp; thẻ cuối cùng thì kết thúc phiên và quay về `browse`. |

[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:97,142-144,193,209,422-425,434-435; 02-bd/database/F6_interview-bank.md:185-187]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `InterviewQuestionSummaryDto` | `id` | UUID | `interview_questions` | `id` | - | Không | [Nguồn] Phản hồi của `ListInterviewQuestions`<br>[Đích] Tham số của `GetInterviewQuestion`, `RateQuestionRecall`, `ToggleQuestionBookmark` và của màn `USR0402`. Không hiển thị trên màn. |
| 2 | `InterviewQuestionSummaryDto` | `topicCode`, `topicDisplayName` | Enum, String | `question_topics` | `code`, `display_name` | Danh sách "Chủ đề" | Có | [Nguồn] Join qua `interview_questions.topic_id`<br>[Chuyển đổi] Hiển thị `display_name`, lọc bằng `code`. |
| 3 | `InterviewQuestionSummaryDto` | `levelCode`, `levelDisplayName` | String, String | `question_levels` | `code`, `display_name` | Danh sách "Độ khó"; Xem nhanh và Luyện nhanh "Chủ đề và độ khó" | Có | [Nguồn] Join qua `interview_questions.level_id` (thay trường `difficulty` kiểu Enum cũ, theo `DEC-2026-1001-admin-configurable-settings` mục (6) và [Nguồn: 02-bd/database/F6_interview-bank.md:30,59])<br>[Chuyển đổi] Đọc `display_name` từ `question_levels`, không còn ánh xạ cứng `EASY`/`MEDIUM`/`HARD` sang "Dễ"/"Trung bình"/"Khó". Màn hiển thị nhãn này và dùng `levelCode` để so khớp với tab độ khó (Sheet 5 khu vực B NO 4); danh sách mức đầy đủ lấy từ `ListQuestionLevels` (NO 13), mở cho mọi người dùng đã xác thực [Nguồn: 02-bd/architecture/F6_interview-bank.md:99]. |
| 4 | `InterviewQuestionSummaryDto` | `title` | String | `interview_questions` | `title` | Danh sách "Tiêu đề câu hỏi" | Có | - |
| 5 | `InterviewQuestionSummaryDto` | `recallRating` | Enum nullable | `recall_ratings` | `rating` | Danh sách "Trạng thái ôn tập" | Có | [Nguồn] Left join `recall_ratings` theo `(user_id hiện hành, question_id)`<br>[Chuyển đổi] Rỗng thành "Chưa xem"; `KNOWN` thành "Đã thuộc"; `VAGUE` và `FORGOTTEN` thành "Đang ôn". Nhãn này **chỉ** suy từ `rating`, không xét `next_review_at`: "đang ôn" khác "đến hạn" (`02-bd/database/F6_interview-bank.md:194-201`, IB-03). |
| 6 | `InterviewQuestionSummaryDto` | `bookmarked` | Boolean | `bookmarks` | `user_id`, `question_id` | Danh sách "Đánh dấu xem lại" | Có | [Nguồn] Tồn tại dòng `bookmarks` khớp cặp khoá thì `true`<br>[Đích] Tham số của `ToggleQuestionBookmark`; cũng là cơ sở của công tắc "Chỉ câu đã đánh dấu" (Sheet 5 khu vực B NO 5). |
| 7 | `InterviewQuestionDetailDto` | `suggestedApproach` | String | `interview_questions` | `suggested_approach` | Xem nhanh "Ý cần nói", Luyện nhanh "Ý cần nói trên thẻ" | Có | [Chuyển đổi] Markdown, tách mục danh sách thành các dòng đánh số hai chữ số khi hiển thị. |
| 8 | `InterviewQuestionDetailDto` | `coreKeywords` | List | `interview_questions` | `core_keywords` | Xem nhanh "Từ khoá cốt lõi" | Có | [Chuyển đổi] Mỗi phần tử là một chip. |
| 9 | `InterviewQuestionDetailDto` | `sampleAnswerFramework`, `followUpQuestions` | String, List | `interview_questions` | `sample_answer_framework`, `follow_up_questions` | - | Không | [Đích] Chỉ màn `USR0402` dùng — DTO dùng chung nhưng màn này **không** hiển thị, đúng phạm vi "bản xem nhanh rút gọn" [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:116]. |
| 10 | `QuestionTopicDto` | `code`, `displayName`, `sortOrder` | String, String, Number | `question_topics` | `code`, `display_name`, `sort_order` | Bộ lọc "Menu lọc chủ đề" | Có | [Nguồn] Phản hồi của `ListQuestionTopics`, đọc từ dữ liệu do ADMIN quản lý (không còn đúng 5 dòng seed); `code` là slug, không còn enum<br>[Chuyển đổi] Giao diện tự thêm chip "Tất cả" ở đầu, không phải một dòng dữ liệu. |
| 11 | `RecallRatingDto` | `questionId`, `rating` | UUID, Enum | `recall_ratings` | `question_id`, `rating` | Xem nhanh "Mức độ nắm", Luyện nhanh "Tự chấm trên thẻ" | Có | [Nguồn] Mức người học chọn<br>[Đích] Tham số của `RateQuestionRecall`. |
| 12 | `RecallRatingDto` | `intervalDays`, `nextReviewAt` | Number, Date | `recall_ratings` | `interval_days`, `next_review_at` | Xem nhanh "Dòng ngày ôn lại kế tiếp" | Có | [Nguồn] Máy chủ tra **bảng cố định** `FORGOTTEN` 1 ngày, `VAGUE` 3 ngày, `KNOWN` 7 ngày, không cộng dồn theo lịch sử, `next_review_at = rated_at + interval_days` (`DEC-2026-1022-interview-bank-schema-sync` mục 5, `02-bd/database/F6_interview-bank.md:185-186`); công thức x2 trần 60 ngày đã bỏ<br>[Đích] Giao diện hiển thị `nextReviewAt` thành "Sẽ nhắc ôn lại sau {n} ngày" (Sheet 5 khu vực D NO 7; Câu hỏi mở Q7 đã chốt 2026-10-18); `intervalDays` chỉ trả về, không hiển thị. |
| 13 | `QuestionLevelDto` | `code`, `displayName`, `sortOrder` | String, String, Number | `question_levels` | `code`, `display_name`, `sort_order` | Bộ lọc "Menu lọc độ khó" | Có | [Nguồn] Phản hồi của `ListQuestionLevels`, đọc từ dữ liệu do ADMIN quản lý; `code` là slug, không còn enum. Endpoint chỉ đọc, mọi người dùng đã xác thực gọi được [Nguồn: 02-bd/security/F6_interview-bank.md:76]<br>[Chuyển đổi] Giao diện tự thêm tab "Tất cả" ở đầu, không phải một dòng dữ liệu. Không gồm số câu hỏi tham chiếu (`questionCount`) như `QuestionLevelOptionDto` của `SHR0301` — học viên không cần. |

### 7.2 Truy cập bảng dữ liệu (5)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Câu hỏi phỏng vấn | `interview_bank.interview_questions` | `InterviewQuestionRepository` | R | Đọc danh sách đã lọc và đọc nội dung một câu hỏi | `ListInterviewQuestions`: R<br>`GetInterviewQuestion`: R. Luôn kèm điều kiện `status = 'ACTIVE'` |
| 2 | Danh mục chủ đề | `interview_bank.question_topics` | `QuestionTopicRepository` | R | Đọc danh mục chủ đề cho chip lọc và cho nhãn chủ đề của từng dòng | `ListQuestionTopics`: R. Màn này không ghi; ADMIN thêm/đổi tên/sắp xếp/xoá chủ đề ở `SHR0301` [Nguồn: 02-bd/database/F6_interview-bank.md:8-28] |
| 3 | Trạng thái ôn tập | `interview_bank.recall_ratings` | `RecallRatingRepository` | C, R, U | Đọc trạng thái ôn tập của người học, ghi mức tự chấm mới | `ListInterviewQuestions`: R (cột trạng thái, tab lọc trạng thái, hàng đợi Luyện nhanh)<br>`RateQuestionRecall`: C, U — chưa có dòng thì tạo, đã có thì ghi đè tại chỗ [Nguồn: 02-bd/database/F6_interview-bank.md:187-192] |
| 4 | Đánh dấu xem lại | `interview_bank.bookmarks` | `BookmarkRepository` | C, R, D | Đọc trạng thái đánh dấu, thêm và gỡ đánh dấu | `ListInterviewQuestions`: R<br>`ToggleQuestionBookmark`: C khi bật, D khi tắt — bookmark là nhị phân, không có trường nào để sửa [Nguồn: 02-bd/database/F6_interview-bank.md:107-108] |
| 5 | Danh mục độ khó | `interview_bank.question_levels` | `QuestionLevelRepository` | R | Đọc nhãn độ khó của từng dòng, bản xem nhanh và thẻ luyện nhanh; đọc danh mục cho tab lọc độ khó | `ListQuestionLevels`: R<br>`ListInterviewQuestions`, `GetInterviewQuestion`: R, qua join. Màn này không ghi; ADMIN quản lý độ khó ở `SHR0301` [Nguồn: 02-bd/database/F6_interview-bank.md:30-50] |

Màn này **không** chạm `user_answers`, `answer_rubrics`, `practice_history`. Riêng `user_answers` là bảng
bất biến, không có đường ghi nào từ màn này (`DEC-2026-0831-outside-screens-closures`).

`[Suy luận]` — tên repository do BD này đề xuất, DD module `interview-bank` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/F6_interview-bank.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListInterviewQuestions` | Tìm kiếm, lọc (từ khoá, trạng thái ôn tập, chủ đề, độ khó, cờ `bookmarkedOnly`) và phân trang danh sách câu hỏi; cũng dùng để lấy bộ thẻ luyện nhanh bằng cách thêm thứ tự ưu tiên câu đến hạn (`next_review_at <= now()`) và giới hạn số dòng | `interview-bank` |
| 2 | `ListQuestionTopics` | Tải danh mục chủ đề (đọc từ dữ liệu) cho chip lọc | `interview-bank` |
| 3 | `GetInterviewQuestion` | Tải nội dung một câu hỏi cho bản xem nhanh và cho thẻ luyện nhanh; dùng chung với màn `USR0402` | `interview-bank` |
| 4 | `RateQuestionRecall` | Ghi một lần tự chấm mức độ nhớ và tính lại lịch ôn lại (F6-12) | `interview-bank` |
| 5 | `ToggleQuestionBookmark` | Bật hoặc tắt đánh dấu xem lại một câu hỏi (F6-03) | `interview-bank` |
| 6 | `ListQuestionLevels` | Tải danh mục độ khó (đọc từ dữ liệu) cho tab lọc độ khó; chỉ đọc, mọi người dùng đã xác thực gọi được | `interview-bank` |

Endpoint NO 1, NO 2 và NO 6 **dùng lại đúng tên nghiệp vụ** của màn quản trị `SHR0301`
[Nguồn: 02-bd/screens/shared/SHR0301_interview_question_management.md:721-722,729] — cùng một việc, khác
phạm vi dữ liệu theo quyền, không đặt tên thứ hai. Không có endpoint riêng cho luyện nhanh: RD đã chốt
phiên luyện lấy theo đúng bộ lọc đang áp [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:117],
nên NO 1 đã đủ.

Dải bốn chỉ số ôn tập của màn này (cùng endpoint `GetMyRecallSummary` và `RecallSummaryDto`) **đã bỏ 2026-10-18, owner**: màn danh sách không còn dải thẻ chỉ số, xem Câu hỏi mở Q11. Màn quản trị `SHR0301` cũng không có endpoint thống kê nào để dùng lại (`GetInterviewQuestionBankStats` đã bỏ ngày 2026-10-01 [Nguồn: 02-bd/screens/shared/SHR0301_interview_question_management.md:737]).

[Nguồn: 02-bd/database/F6_interview-bank.md:8-28,52-76,98-109,177-205]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Đổi giữa hai trạng
> thái `browse` và `drill` **không** phải chuyển màn.

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì tải danh mục chủ đề, danh mục độ khó và trang đầu danh sách. | Không | Có | `ListQuestionTopics`, `ListQuestionLevels`, `ListInterviewQuestions` | [Các bước]<br>1. Kiểm tra người dùng đã đăng nhập.<br>2. Đọc tham số `topic`, `q`, `status`, `level` trên URL và đặt vào bộ lọc trước khi gọi (mã không khớp thì bỏ qua).<br>3. Hiển thị khung chờ cho ba khu vực, tải song song.<br>4. Chọn sẵn dòng đầu tiên của danh sách và tải bản xem nhanh của nó.<br>[Khi thành công] Danh sách, bộ lọc chủ đề, độ khó và bản xem nhanh hiển thị đầy đủ. Không còn dải bốn chỉ số (đã bỏ 2026-10-18, owner).<br>[Khi lỗi] Hiển thị lỗi tại đúng khu vực tải thất bại; các khu vực tải được vẫn hiển thị bình thường. Danh mục chủ đề lỗi thì chỉ còn chip "Tất cả", danh mục độ khó lỗi thì chỉ còn tab "Tất cả"; danh sách vẫn xem được. |
| 2 | Nhập liệu | Tìm kiếm theo từ khoá | Gõ vào ô tìm kiếm. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Chờ người học ngừng gõ rồi mới gọi, không gọi theo từng ký tự.<br>2. Tải lại danh sách từ trang đầu với từ khoá mới.<br>3. Ghi từ khoá vào tham số `q` trên URL để nút quay lại của trình duyệt khôi phục đúng bộ lọc.<br>[Khi thành công] Danh sách đổi. Nhấn Enter trong ô tìm kiếm thì toast thông tin báo số câu hỏi tìm thấy (hoặc không có câu nào); gõ dở thì không toast [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:205-212].<br>[Khi lỗi] Giữ nguyên danh sách cũ và hiển thị toast lỗi. |
| 3 | Nút | Chọn trạng thái ôn tập | Mở menu lọc trạng thái và chọn một trong bốn lựa chọn. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt tab được chọn, tải lại danh sách từ trang đầu.<br>2. Ghi vào tham số `status` trên URL.<br>[Khi thành công] Danh sách chỉ còn câu hỏi ở trạng thái tương ứng; không khớp dòng nào thì hiện trạng thái rỗng.<br>[Khi lỗi] Trả tab về giá trị trước đó và hiển thị toast lỗi. |
| 4 | Nút | Chọn chủ đề | Mở menu lọc chủ đề và chọn một chủ đề hoặc "Tất cả". | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt chủ đề được chọn (chỉ chọn được **một** chủ đề tại một thời điểm), tải lại danh sách từ trang đầu.<br>2. Ghi vào tham số `topic` trên URL.<br>[Khi thành công] Danh sách chỉ còn câu hỏi thuộc chủ đề đó.<br>[Khi lỗi] Trả chip về giá trị trước đó và hiển thị toast lỗi. |
| 5 | Màn hình | Cuộn tải thêm | Cuộn tới cuối vùng danh sách. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Gọi trang kế tiếp với đúng bộ lọc hiện hành.<br>2. Nối thêm vào cuối danh sách, giữ nguyên vị trí cuộn và câu hỏi đang chọn.<br>[Khi thành công] Thêm tối đa 20 dòng.<br>[Khi lỗi] Giữ nguyên các dòng đã tải, hiển thị lỗi ở cuối danh sách kèm khả năng thử lại. |
| 6 | Danh sách | Chọn một dòng câu hỏi | Bấm vào một dòng trong danh sách. | Không | Có | `GetInterviewQuestion` | [Các bước]<br>1. Đánh dấu dòng đang chọn.<br>2. Tải nội dung câu hỏi cho bản xem nhanh.<br>[Khi thành công] Cột phải hiển thị tiêu đề đầy đủ, ý cần nói, từ khoá và mức tự chấm hiện hành; danh sách không bị cuộn lại đầu.<br>[Khi lỗi] Cột phải hiển thị lỗi, danh sách bên trái giữ nguyên. |
| 7 | Nút | Bật hoặc tắt đánh dấu xem lại | Bấm nút đánh dấu trên một dòng. | Không | Có | `ToggleQuestionBookmark` | [Các bước]<br>1. Đảo trạng thái đánh dấu ngay trên giao diện.<br>2. Gọi máy chủ ghi nhận.<br>[Khi thành công] Giữ nguyên trạng thái đã đảo, hiện toast thành công "Đã lưu câu hỏi." hoặc "Đã bỏ lưu câu hỏi." [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:196-203].<br>[Khi lỗi] Trả trạng thái về như cũ và hiển thị toast lỗi. Đây là thao tác chỉ chạm dữ liệu của chính người học, không đổi nội dung câu hỏi. |
| 8 | Nút | Tự chấm mức độ nhớ | Bấm một trong ba mức trong bản xem nhanh. | Không | Có | `RateQuestionRecall` | [Các bước]<br>1. Gửi mức tự chấm của câu hỏi đang chọn.<br>2. Máy chủ ghi đè dòng `recall_ratings` và đặt lại `interval_days` theo bảng cố định 1/3/7 ngày, `next_review_at` (không cộng dồn).<br>3. **Giữ câu vừa chấm trong danh sách và bản xem nhanh** dù bộ lọc đang áp sẽ loại nó (câu này được ghim cho tới khi đổi bộ lọc, ô tìm kiếm hoặc tải lại danh sách; owner 2026/10/19), để người học kịp đọc dòng ngày ôn lại. Cập nhật nhãn trạng thái của dòng tương ứng (không còn chỉ số ôn tập để cập nhật, đã bỏ 2026-10-18, owner); hiện dòng "Sẽ nhắc ôn lại sau {n} ngày" dưới khối tự chấm từ `next_review_at` máy chủ trả về (Câu hỏi mở Q7).<br>[Khi thành công] Mức mới được chọn sẵn, nhãn trạng thái trên danh sách đổi theo, dòng ngày ôn lại cập nhật, hiện toast thành công "Đã ghi nhận mức độ nhớ." [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:190-194].<br>[Khi lỗi] Giữ nguyên mức cũ và hiển thị toast lỗi; **không** chặn các thao tác khác của màn. |
| 9 | Liên kết | Mở chi tiết câu hỏi | Bấm "Xem chi tiết" trong bản xem nhanh. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `interview_question_detail` kèm `questionId` và trạng thái bộ lọc hiện hành (`topic`, `q`, `status`, `level`).<br>[Khi thành công] Mở màn `USR0402`. Màn này không giữ thay đổi chưa lưu nào — mọi thao tác (tự chấm, đánh dấu) đều đã ghi ngay khi bấm, nên **không** hỏi xác nhận trước khi rời màn.<br>[Thông báo hoàn tất] Không có. |
| 10 | Liên kết | Vào phiên giả lập | Bấm liên kết "Vào phiên giả lập" (luôn bấm được). | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `mock_interview` (`USR0302`), đường dẫn `/mock-interview` [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:320-325].<br>[Khi thành công] Mở màn `USR0302`. Không truyền câu hỏi nào sang, xem Câu hỏi mở Q5 (đã chốt 2026-10-18, không có nút "Thêm vào phiên giả lập tới"). Không có thay đổi chưa lưu nên không hỏi xác nhận. |
| 11 | Nút | Bắt đầu luyện nhanh | Bấm "Luyện nhanh". | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Lấy bộ thẻ theo **đúng bộ lọc đang áp**, ưu tiên câu hỏi **đến hạn** (`next_review_at <= now()`, không xét mức tự chấm) rồi tới câu chưa từng tự chấm, giới hạn 10 thẻ (hằng số cấu hình, Câu hỏi mở Q9) [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:117,133].<br>2. Chuyển sang trạng thái `drill`, hiển thị thẻ đầu tiên ở trạng thái chưa lộ đáp án.<br>[Khi thành công] Thanh tiến độ ở 1 trên tổng số thẻ. Bộ lọc hiện hành được giữ lại để khôi phục khi thoát.<br>[Khi lỗi] Không chuyển trạng thái, giữ nguyên `browse` và hiển thị toast lỗi. |
| 12 | Nút | Hiện hoặc ẩn ý cần nói | Bấm "Xem ý cần nói" hoặc "Ẩn đáp án". | Không | Không | - | [Các bước]<br>1. Đảo trạng thái lộ đáp án của thẻ đang xem.<br>[Khi thành công] Ý cần nói hiện ra hoặc thu lại, nhãn nút đổi theo. Không gọi máy chủ vì nội dung thẻ đã tải sẵn. |
| 13 | Nút | Tự chấm trên thẻ và sang thẻ kế | Bấm một trong ba mức ở cuối thẻ. | Không | Có | `RateQuestionRecall` | [Các bước]<br>1. Gửi mức tự chấm của thẻ hiện tại.<br>2. Chuyển sang thẻ kế tiếp, thu ý cần nói về trạng thái chưa lộ.<br>3. Cập nhật bộ đếm và thanh tiến độ.<br>[Khi thành công] Hết thẻ cuối cùng thì kết thúc phiên, quay về `browse` với đúng bộ lọc trước đó, kèm toast thành công "Đã ôn xong {n} thẻ." [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:170-181].<br>[Khi lỗi] **Vẫn chuyển sang thẻ kế tiếp** để không chặn nhịp luyện, hiển thị một toast cảnh báo rằng lần tự chấm chưa ghi được — mất một lần tự chấm nhẹ hơn nhiều so với kẹt cả phiên luyện `[SoT: Suy luận]`. |
| 14 | Nút | Thoát luyện nhanh | Bấm "Thoát". | Không | Không | - | [Các bước]<br>1. Quay về trạng thái `browse` với đúng bộ lọc trước khi vào luyện. Không gọi máy chủ: bốn chỉ số mà trước đây phải tải lại đã bỏ 2026-10-18, owner [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:166-168].<br>[Khi thành công] Danh sách hiển thị lại kèm nhãn trạng thái đã cập nhật.<br>[Khi xác nhận] Không hỏi xác nhận: mỗi lần tự chấm đã ghi ngay khi bấm, thoát giữa chừng không mất dữ liệu nào. |
| 15 | Nút | Chọn độ khó | Mở menu lọc độ khó và chọn một mức hoặc "Tất cả". | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt mức được chọn (chỉ chọn được **một** mức tại một thời điểm), tải lại danh sách từ trang đầu với `levelCode`, cộng dồn `VÀ` với từ khoá, tab trạng thái và chip chủ đề đang áp.<br>2. Cột phải: câu đang chọn còn trong danh sách mới thì giữ, không còn thì chuyển sang dòng đầu của danh sách đã lọc [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:147-148]. Danh sách rỗng thì cột phải hiện câu hướng dẫn chọn câu hỏi.<br>3. Ghi mã độ khó vào tham số `level` trên URL (Câu hỏi mở Q10, đã chốt 2026-10-18); bản dựng hiện chưa ghi URL cho bộ lọc nào, xem "Hiện trạng bản dựng".<br>[Khi thành công] Danh sách chỉ còn câu hỏi thuộc mức đó; không khớp dòng nào thì hiện trạng thái rỗng và nút "Luyện nhanh" không kích hoạt. Bộ thẻ luyện nhanh bấm sau đó lấy từ danh sách đã lọc này (EVT-11) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:118-145,151-164,365-376].<br>[Khi lỗi] Trả tab về giá trị trước đó và hiển thị toast lỗi. |
| 16 | Nút | Bật hoặc tắt công tắc "Chỉ câu đã đánh dấu" | Bấm công tắc cạnh bốn tab trạng thái. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt cờ `bookmarkedOnly`, tải lại danh sách từ trang đầu, cộng dồn `VÀ` với từ khoá, tab trạng thái, chip chủ đề và tab độ khó đang áp.<br>2. Cột phải: câu đang chọn còn trong danh sách mới thì giữ, không còn thì chuyển sang dòng đầu; danh sách rỗng thì hiện câu hướng dẫn chọn câu hỏi.<br>[Khi thành công] Danh sách chỉ còn câu hỏi đã đánh dấu khi công tắc Bật; không khớp dòng nào thì hiện trạng thái rỗng và nút "Luyện nhanh" không kích hoạt. Bộ thẻ luyện nhanh bấm sau đó lấy từ danh sách đã lọc này (EVT-11) [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:118-145,151-164,377-382].<br>[Khi lỗi] Trả công tắc về giá trị trước đó và hiển thị toast lỗi. |

[Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:109-110,121,124,128,133,183,185,197,221,223-224,422-425,434-435; 01-rd/screens/users/USR0401_interview_bank_list.md:117,131,133; 02-bd/architecture/F6_interview-bank.md:256-259]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/interview_bank_list.md`, không lặp lại ở
> đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Yêu cầu đăng nhập | [Nội dung kiểm] Người dùng chưa đăng nhập thì không vào được màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ, không chỉ ẩn giao diện. | Lỗi | Chưa có mã thông báo | Nội dung "Vui lòng đăng nhập để tiếp tục." Lý do bắt buộc đăng nhập: trạng thái ôn tập và đánh dấu đều gắn với một `user_id` cụ thể. Khung khu Người học đã chốt bắt buộc đăng nhập cho mọi tính năng [Nguồn: 02-bd/screens/users/_shell.md:131] — Câu hỏi mở Q1 đã đóng. | EVT-1 | 1 |
| 2 | Kiểm quyền | Quyền sở hữu dữ liệu ôn tập | [Nội dung kiểm] Mọi thao tác đọc và ghi trạng thái ôn tập, đánh dấu chỉ áp dụng cho `user_id` của phiên đăng nhập hiện hành; không nhận `user_id` từ phía giao diện.<br>[Nơi thực thi] Máy chủ. | Lỗi | Mã lỗi trong phản hồi | Nội dung "Bạn không có quyền thực hiện thao tác này." Cùng nguyên tắc quyền sở hữu dữ liệu đã chốt cho module [Nguồn: 02-bd/architecture/F6_interview-bank.md:265-268]. | EVT-7, EVT-8, EVT-13, EVT-16 | 1 |
| 3 | Kiểm nhập liệu | Độ dài từ khoá tìm kiếm | [Nội dung kiểm] Từ khoá dài quá 100 ký tự thì cắt bớt và không gọi máy chủ với phần dư.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Viền ô tìm kiếm + toast. | Cảnh báo | Chưa có mã thông báo | Nội dung "Từ khoá tìm kiếm tối đa 100 ký tự." Giới hạn là `[Suy luận]` — prototype không đặt `maxlength`, nhưng ô tìm kiếm không có lý do nhận chuỗi dài hơn tiêu đề câu hỏi. | EVT-2 | 1 |
| 4 | Kiểm nhập liệu | Tham số lọc trên URL | [Nội dung kiểm] Tham số `topic` không khớp mã chủ đề nào trong danh mục đã tải, `level` không khớp mã độ khó nào trong danh mục đã tải, hoặc `status` không khớp 4 giá trị hợp lệ, thì bỏ qua tham số đó và dùng giá trị mặc định, **không** báo lỗi chặn màn.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Menu lọc chủ đề, tab độ khó và tab trạng thái. | Cảnh báo | Chưa có mã thông báo | Nội dung "Bộ lọc trong đường dẫn không hợp lệ, đã bỏ qua." Màn đến từ `solution_review` nên tham số có thể lệch khi hai module đổi độc lập [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:118]. | EVT-1 | 2 |
| 5 | Kiểm nghiệp vụ | Chỉ hiển thị câu hỏi đang dùng | [Nội dung kiểm] Câu hỏi có `status = 'RETIRED'` không xuất hiện ở bất kỳ danh sách, bản xem nhanh hay thẻ luyện nhanh nào.<br>[Nơi thực thi] Máy chủ, ở tầng truy vấn. | Lỗi | Mã lỗi trong phản hồi | Nội dung "Câu hỏi không còn khả dụng." Xoá là xoá mềm, không cascade [Nguồn: 02-bd/database/F6_interview-bank.md:66]. Áp cả khi người học mở lại một đường dẫn cũ trỏ vào câu hỏi đã ngừng dùng. | EVT-1, EVT-6, EVT-11 | 1 |
| 6 | Kiểm nghiệp vụ | Bộ lọc không còn câu hỏi nào khi luyện nhanh | [Nội dung kiểm] Bộ lọc hiện hành không khớp câu hỏi nào thì không cho vào trạng thái `drill`.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Nút "Luyện nhanh" + toast. | Cảnh báo | Chưa có mã thông báo | Nội dung "Không có câu hỏi nào khớp bộ lọc để luyện." Nút đã không kích hoạt sẵn theo Sheet 6; kiểm này phòng trường hợp danh sách rỗng đi ngay sau khi bấm. | EVT-11 | 1 |
| 7 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác đó, giữ nguyên dữ liệu đang hiển thị, không rời màn.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." Lỗi của EVT-1 hiện ở đúng khu vực tải hỏng (nội dung thay chỗ); lỗi của các EVT còn lại hiện bằng toast. Riêng EVT-13 vẫn cho đi tiếp sang thẻ kế, xem Sheet 8. | EVT-1, EVT-2, EVT-3, EVT-4, EVT-5, EVT-6, EVT-7, EVT-8, EVT-11, EVT-13, EVT-15, EVT-16 | 1 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

**Không có mục kiểm nào liên quan tới AI trên màn này** — màn không gọi `AnswerFeedbackPort`, nên phân hệ
AI chết hoặc hết quota không sinh thêm một nhánh lỗi nào ở đây
[Nguồn: 02-bd/architecture/F6_interview-bank.md:270-279].

[Nguồn: 02-bd/architecture/F6_interview-bank.md:265-268,270-279; 02-bd/database/F6_interview-bank.md:66]

---

## Hiện trạng bản dựng

> Thêm 2026-10-18, cập nhật 2026-10-21 (số dòng `view` và các dòng dựng lại 2026/10/20, **chưa duyệt hình**). Đối chiếu `05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx` ("view") và `05-coding/frontend/src/entities/interview-question/model/types.ts` ("types") với `DEC-2026-1018-usr-open-questions-phase1-scope` và `DEC-2026-1018-usr-open-questions-phase2-5`. Mã đã được sửa trong cùng lượt này cho Q7, Q8, Q9; Q10 chưa làm được ở prototype (xem dòng Q10 của bảng). Việc ghi vào `06-plan/PROTOTYPE_DEBT.md` do bên điều phối làm, file đó ngoài phạm vi lần sửa này.

| Điểm quyết định | Bản dựng thực tế | Khớp? |
| :--- | :--- | :-: |
| Q7: dòng "Sẽ nhắc ôn lại sau {n} ngày" | Hiện dưới khối tự chấm khi câu đang chọn có mức tự chấm [view:481-487]; `n` tra bảng (Quên 1, Mơ hồ 3, Biết rõ 7) [types:62-71], **trùng bảng cố định ở DB** (IB-16). Không có `next_review_at` thật vì chưa có máy chủ | Khớp bảng; thiếu máy chủ |
| Q8: công tắc "Chỉ câu đã đánh dấu" | `Toggle` dùng chung, state `bookmarkedOnly` [view:95,377-382], lọc theo bản đồ đánh dấu hiện hành [view:124] | Khớp |
| Q9: luyện nhanh cố định 10 thẻ | Hằng số `DRILL_SIZE = 10` [view:45-47], dùng cắt bộ thẻ [view:159] và nhãn nút [view:318]; không có điều khiển chọn số thẻ | Khớp |
| Q10: tham số `level` trên URL | View không dùng `useSearchParams` hay `useRouter`: mọi bộ lọc (`query`, `status`, `topic`, `level`, `bookmarkedOnly`) là `useState` cục bộ [view:90-95]; liên kết sang chi tiết là `/interview-bank/${code}` không mang tham số lọc [view:488-491]. `topic`, `q`, `status` cũng chưa đọc từ URL, nên theo quyết định của owner `level` chưa làm riêng | Lệch (nợ prototype, cả bốn tham số) |
| Q6: ba nút tự chấm | `RecallLevelPicker` render đúng ba nút theo `RECALL_LEVELS = ["known", "vague", "forgotten"]` (`05-coding/frontend/src/entities/interview-question/model/types.ts:60`; `05-coding/frontend/src/entities/interview-question/ui/recall-level-picker.tsx:33`), dùng ở bản xem nhanh [view:474-480] và thẻ luyện nhanh [view:288-293]; nhãn `recall` = Biết rõ / Mơ hồ / Quên (`vi.json:1942-1947`). Bốn lựa chọn lọc trạng thái = Tất cả / Chưa xem / Đang ôn / Đã thuộc (`vi.json:1933-1938`; view:339-352). "Chưa xem" còn có nhãn `recall.unrated` chỉ dùng cho badge trạng thái ở danh sách [view:219-220], không có trong bộ nút. Không có lời gọi AI nào | Khớp |
| Ô tìm kiếm | `TextField` với `maxLength = 100` [view:50,336], khớp `question`, `content`, `coreKeywords` (không phân biệt hoa thường) [view:126-132]; placeholder "Tìm theo câu hỏi, nội dung hoặc từ khoá" (`vi.json:1931`) | Khớp (2026/10/19) |
| Ba bộ lọc | Ba `FilterMenu` dạng menu thả xuống cho trạng thái, chủ đề, độ khó [view:339-376]; owner chốt giữ dạng này 2026/10/19, BD và RD đã sửa theo | Khớp (đã sửa tài liệu) |
| Tự chấm hoặc bỏ đánh dấu khi đang lọc | State `pinned` gắn với chữ ký bộ lọc [view:103-111]: câu vừa thao tác ở lại trong danh sách và bản xem nhanh cho tới khi đổi bộ lọc hoặc ô tìm kiếm | Khớp (2026/10/19) |
| Câu không có ý cần nói (nhập CSV) | Khối "Ý cần nói" ẩn khi mảng rỗng, ở cả bản xem nhanh [view:434] lẫn thẻ luyện nhanh [view:263] | Khớp |
| Nút "Vào phiên giả lập" | **Không còn `disabled`** (sửa 2026/10/21): `Button asChild` bọc `Link href="/mock-interview"`, biểu tượng `Mic`, nhãn `startMockInterview` [view:320-325]. Route `/mock-interview` đã có ở prototype (`06-plan/PROTOTYPE_DEBT.md:1033`) | Khớp (chưa duyệt hình) |
| Tiêu đề và phụ đề trang | `h1` `t("title")` và `p` `t("subtitle")` ở đầu hàng [view:303-308]; `vi.json:1974-1975` | Khớp (yêu cầu owner 2026/10/20, chưa duyệt hình) |
| Thanh công cụ lọc ngang | `FilterBar` chung (`shared/ui/data/filter-bar.tsx`) chứa ô tìm kiếm, ba `FilterMenu`, `Toggle` [view:329-383]. Owner chốt 2026/10/21 dùng `FilterBar` chung thay thanh tự dựng (Q12); hành vi lọc và xử lý Enter giữ nguyên | Khớp (chưa duyệt hình); Q12 phần thanh lọc đã chốt |
| Dòng đếm kết quả | `t("shownCount", { shown: filtered.length, total: page.questions.length })` ở đầu thẻ danh sách [view:387-392]; vùng cuộn `max-h-[68dvh]` [view:393]. `total` là độ dài mảng mock, chưa phải tổng từ máy chủ | Khớp (nợ: xem Q13) |
| Tỉ lệ hai cột 5:6 và bản xem nhanh | `lg:[grid-template-columns:minmax(0,5fr)_minmax(0,6fr)]` [view:385]; tiêu đề `text-xl font-bold` [view:430-432]; nhãn khối từ khoá `quickView.keywordsLabel` [view:455-459]; ý cần nói trong ô nền nhạt [view:441-444]. Dòng danh sách (`InterviewQuestionListRow`) **không** có "nền nhạt + viền" từng dòng: dòng chọn có nền `surface-hover`, các dòng ngăn bằng viền dưới [`interview-question-list-row.tsx:46-53`] | Khớp phần bản xem nhanh; yêu cầu "list rows are tinted outline rows" của owner **chưa kiểm chứng được** từ mã (Q12) |
| Khoá i18n `startMockInterviewUnavailable` | Vẫn còn trong `vi.json:1929` nhưng không còn được view dùng (grep `startMockInterviewUnavailable` trong `interview-bank-list-view.tsx` không có kết quả) | Nợ: xoá được ở `vi.json` và `en.json` |
| Kiểm thử | `interview-bank-list-view.test.tsx:23-107` là smoke test: danh sách, tìm kiếm, lọc độ khó, vào luyện nhanh, ghim câu vừa chấm. **Chưa có** ca nào cho tiêu đề, liên kết `/mock-interview`, dòng đếm, nhãn từ khoá | Nợ: bổ sung khi viết `04-tdd/interview_bank_list.md` |

**Nợ prototype cho `06-plan/PROTOTYPE_DEBT.md`** (bên điều phối ghi): (1) `USR0401` chưa đọc `topic`/`q`/`status`/`level` từ URL và chưa truyền sang `USR0402`; làm một lượt cho cả bốn tham số khi dựng route thật; (2) dòng ngày ôn lại dùng bảng hằng số giả lập thay `next_review_at`; (3) công tắc "Chỉ câu đã đánh dấu" lọc trên bản đồ đánh dấu trong bộ nhớ, không gọi `ListInterviewQuestions`; (4) xoá khoá i18n `startMockInterviewUnavailable` ở `vi.json` và `en.json` (không còn dùng từ 2026/10/20); (5) `06-plan/PROTOTYPE_DEBT.md:886` còn ghi nút "Vào phiên giả lập" "đang khoá" và chưa có route `mock_interview`, đã lỗi thời (file đó ngoài phạm vi lần sửa này); (6) `total` ở dòng đếm là độ dài mảng mock, thay bằng tổng từ `ListInterviewQuestions` khi có API.

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~**Màn có xem được khi chưa đăng nhập không?**~~ **ĐÃ ĐÓNG 2026-09-22**: khung khu Người học chốt bắt buộc đăng nhập cho mọi tính năng, chưa đăng nhập chuyển hướng về `auth` (`DEC-2026-0922-users-and-admin-conflict-resolutions`) [Nguồn: 02-bd/screens/users/_shell.md:131]. | Khung khu Người học từng để ngỏ câu này và nêu đích danh `interview_bank_list`; nay đã đóng. | Giữ phương án đã chốt: bắt buộc đăng nhập. Nhãn trạng thái từng dòng, tự chấm và đánh dấu đều là dữ liệu của một người dùng cụ thể; làm bản khách chỉ để xem danh sách trần sẽ phải dựng thêm một nhánh giao diện riêng mà không có yêu cầu nào đòi. | Chủ dự án |
| Q2 | ~~**Chỉ số "Ôn trong tuần" đếm thiếu.**~~ **RÚT 2026-10-18**: chỉ số "Ôn trong tuần" đã bỏ cùng bốn thẻ chỉ số của màn này (owner, mở rộng quy tắc 2026-10-13), nên câu hỏi không còn đối tượng. Câu hỏi gốc: `recall_ratings` ghi đè tại chỗ, chỉ giữ `rated_at` của lần chấm mới nhất [Nguồn: 02-bd/database/F6_interview-bank.md:187-192], nên một câu chấm ba lần trong tuần chỉ đếm là một. | Không còn chỉ số nào trên màn này cần đếm theo `rated_at`. | Không cần hành động ở màn này. Nếu sau này một màn khác (ví dụ `my_progress`, F6-10) cần số lượt ôn theo tuần thì mới đặt lại câu hỏi bảng lịch sử `recall_rating_history` [Nguồn: 02-bd/database/F6_interview-bank.md:203-204]; việc đó thuộc màn đó. | Chủ dự án |
| Q3 | ~~**Mã câu hỏi dạng `IQ-014` hiển thị ở bản xem nhanh lấy từ đâu?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất: bỏ mã khỏi giao diện, không thêm cột `display_code`.** Ponytail bậc 1, 2 (không yêu cầu nghiệp vụ, không nguồn dữ liệu), `DEC-2026-1018-usr-open-questions-phase1-scope` mục (7). Bản dựng không hiển thị mã: dòng danh sách chỉ có chủ đề, độ khó, nhãn trạng thái, tiêu đề [Nguồn: 05-coding/frontend/src/entities/interview-question/ui/interview-question-list-row.tsx:60-73]; bản xem nhanh chỉ có chủ đề · độ khó, tiêu đề [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:422-432]. (Trường `code` trong mã nguồn là khoá nội bộ dùng làm `key` và đường dẫn `/interview-bank/{code}`, không render ra chữ [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:399,489].) Câu hỏi gốc: Prototype ghép mã này vào nhãn phân loại [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:406], nhưng `interview_questions` chỉ có `id` kiểu UUID [Nguồn: 02-bd/database/F6_interview-bank.md:56, trạng thái lúc hỏi; nay có thêm `display_no`, xem ghi chú cuối dòng]. | Hai nguồn đối lập: prototype có mã người đọc được; BD database không có cột nào tương ứng. | Bỏ mã khỏi giao diện ở bản đầu. Thêm một cột mã hiển thị (kiểu `display_code` tự tăng) là một migration cộng một quy tắc sinh mã, chỉ để làm đẹp một nhãn — chưa có yêu cầu nghiệp vụ nào cần trích dẫn câu hỏi theo mã. Khi nào cần (ví dụ giảng viên muốn đọc mã cho học viên) thì thêm, lúc đó đủ dữ kiện để chọn định dạng đúng. **Cập nhật 2026-10-22 (IB-01, `DEC-2026-1022-interview-bank-schema-sync` mục 1):** DB đã thêm `interview_questions.display_no INT UNIQUE` (số hiệu `IQ-%03d` từ sequence máy chủ, `02-bd/database/F6_interview-bank.md:57`) **chỉ cho khu quản trị và giảng viên**; màn học viên này **vẫn không hiện mã**, không đọc `display_no`, không dùng làm tham số API hay route (`id` là khoá). Quyết định bỏ mã khỏi giao diện học viên giữ nguyên; khi dựng route thật, prototype khoá theo `id` thay vì `code` (nợ prototype ở báo cáo, không ghi ở đây). | Chủ dự án |
| Q4 | ~~**"Tag" của prototype và `core_keywords` của BD database có phải một thứ không?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất: dùng `core_keywords` thay tag, không thêm bảng tag; nhãn bối cảnh như "Hỏi ở vòng 1" là dữ liệu mẫu của prototype.** Ponytail bậc 1, 2, `DEC-2026-1018-usr-open-questions-phase1-scope` mục (7). Bản dựng render `core_keywords` thành chip trong bản xem nhanh, ẩn khi rỗng [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:455-468]; không có khối tag nào khác. Câu hỏi gốc: Prototype dùng tag chứa cả bối cảnh phỏng vấn ("Hỏi ở vòng 1", "Vòng senior", "Vòng culture fit") lẫn từ khoá kỹ thuật ("Hash map", "Redis") [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:244,259,294]; `core_keywords` theo F6-06 chỉ là "từ khoá kỹ thuật cốt lõi **cần nêu** khi trả lời" [Nguồn: 02-bd/database/F6_interview-bank.md:64]. | Hai khái niệm chồng lấn một phần: một cái là gợi ý nội dung trả lời, một cái là nhãn phân loại để tìm kiếm. Màn soạn thảo `SHR0302` chỉ có 4 nhóm trường, không có nhóm "tag". | Dùng `core_keywords` cho cả hai vai trò ở bản đầu, và **không** thêm bảng tag riêng. Nhãn bối cảnh phỏng vấn ("Hỏi ở vòng 1") là dữ liệu mẫu của prototype, không có yêu cầu nào trong F6 đòi phân loại theo vòng phỏng vấn. Cần phân loại theo vòng thì đó là một trường phân loại mới, phải sửa cả `SHR0302`. | Chủ dự án + DD `interview-bank` |
| Q5 | ~~**Nút "Thêm vào phiên giả lập tới" có thật không?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất: nút không tồn tại, bỏ.** Ponytail bậc 1, 2 (không mã `Fx-nn`, không bảng lưu hàng đợi), không thêm phụ thuộc F6 sang F5, `DEC-2026-1018-usr-open-questions-phase1-scope` mục (4). Nút "Vào phiên giả lập" ở đầu trang giữ nguyên (Sheet 5 Khu vực A NO 2); bản dựng không có nút "Thêm vào phiên..." ở bản xem nhanh [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:419-499]. Câu hỏi gốc: Prototype có nút này, nhưng phần cài đặt của chính prototype chỉ đánh dấu câu hỏi là "đang ôn" chứ không thêm vào đâu cả [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:185,420]. Không có bảng nào lưu "hàng đợi câu hỏi cho phiên giả lập tới", và `mock_interview` thuộc Bounded Context `ai-review` (F5), không phải F6. | Nút này là một điểm nối liên module chưa từng được thiết kế ở BD nào của F5 hay F6. Ghép vào sẽ tạo phụ thuộc mới từ F6 sang F5, trong khi kiến trúc đang chốt là F6 độc lập hoàn toàn [Nguồn: 02-bd/architecture/F6_interview-bank.md:127-129]. | Bỏ nút khỏi thiết kế — BD này đã không đưa vào Sheet 5. Giữ nút "Vào phiên giả lập" ở đầu trang là đủ. Muốn có thật thì phải mở một quyết định về việc F5 nhận danh sách câu hỏi khởi tạo từ đâu, và đó là thay đổi phạm vi của `mock_interview`. | Chủ dự án |
| Q6 | ~~**Ba mức tự chấm dùng nhãn nào — nhãn của F6-12 hay nhãn của prototype?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** `DEC-2026-1018-usr-open-questions-last-four`. Ponytail bậc 1 (F6-12 đã nêu đích danh ba mức). Ba nút tự chấm là Biết rõ / Mơ hồ / Quên (`KNOWN`/`VAGUE`/`FORGOTTEN`), **do người học chọn, không có AI**: owner hỏi đây có phải AI chấm sau khi bài đạt Accepted không, câu trả lời là không (AI chỉ ở F5 phân tích bài giải và phỏng vấn giả lập, và F6-08 chấm câu trả lời gõ ở `USR0402`); giải thích xong owner giữ tự chấm thuần và bác ý gợi ý mức bằng AI. Bộ lọc trạng thái giữ bốn tab (Tất cả, Chưa xem, Đang ôn, Đã thuộc); "Chưa xem" là không có dòng `recall_ratings`, không phải lựa chọn tự chấm; luyện nhanh dùng cả ba nút. Bản dựng khớp (xem "Hiện trạng bản dựng"). Sheet 5 khu vực B NO 2, D NO 5, E NO 8. Câu hỏi gốc: F6-12 chốt ba mức Biết rõ / Mơ hồ / Quên [Nguồn: 01-rd/req/F6_interview-bank.md:34-37], khớp enum `KNOWN`/`VAGUE`/`FORGOTTEN` [Nguồn: 02-bd/database/F6_interview-bank.md:184]. Prototype dùng ba nhãn khác: Chưa xem / Đang ôn / Đã thuộc [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:413-418], và ở trạng thái luyện nhanh chỉ còn hai nút [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:223-224]. | Hai nguồn đối lập về nhãn, và prototype gộp hai khái niệm khác nhau làm một: "bộ lọc trạng thái" (bốn giá trị, trong đó "Chưa xem" là **không có** dòng dữ liệu) và "mức tự chấm" (ba giá trị, luôn có dòng dữ liệu). | Tách hai khái niệm, đúng như BD này đã viết: bộ lọc giữ bốn tab của prototype; nút tự chấm dùng ba mức của F6-12. "Chưa xem" không thể là một lựa chọn tự chấm — không ai tự chấm mình là chưa xem một câu vừa đọc xong. Trạng thái luyện nhanh cũng dùng đủ ba nút để một lần bấm luôn xác định được `interval_days` theo công thức đã chốt [Nguồn: 02-bd/architecture/F6_interview-bank.md:247-254]; hai nút sẽ bỏ phí mức `VAGUE`. | Chủ dự án |
| Q7 | ~~**Có hiển thị ngày ôn lại kế tiếp sau khi tự chấm không?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** `DEC-2026-1018-usr-open-questions-phase2-5` (PHASE 3). Ponytail bậc 3 (người học thấy ngay tác dụng của lần tự chấm khi demo). Thêm Sheet 5 và 6 khu vực D NO 7; Sheet 7.1 NO 12 nay hiển thị. Bản dựng dùng quy tắc giả lập (Quên 1, Mơ hồ 3, Biết rõ 7 ngày) [Nguồn: 05-coding/frontend/src/entities/interview-question/model/types.ts:62-71; 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:481-487]. Câu hỏi gốc: `recall_ratings.next_review_at` đã có sẵn và máy chủ trả về, nhưng prototype không vẽ chỗ nào hiển thị nó. | Không có yêu cầu nào trong F6-09/F6-12 đòi hiển thị ngày cụ thể; RD chỉ nói hệ thống "tự xếp lịch". | Hiện một dòng ngắn dạng "Sẽ nhắc ôn lại sau {n} ngày" ngay dưới khối tự chấm. Rẻ (dữ liệu đã có trong phản hồi), và nó là phản hồi duy nhất cho người học biết hành động tự chấm vừa rồi có tác dụng gì — không có nó thì ba nút kia trông như một nhãn trang trí. | Chủ dự án |
| Q8 | ~~**Bộ lọc "Đã đánh dấu" (F6-03) có cần một tab riêng không?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** `DEC-2026-1018-usr-open-questions-phase2-5` (PHASE 3). Ponytail bậc 3 (người học bấm lọc câu đã đánh dấu khi demo). Công tắc "Chỉ câu đã đánh dấu" cạnh bốn tab, không phải tab thứ năm; thêm Sheet 5 và 6 khu vực B NO 5, Sheet 8 EVT-16. Bản dựng [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:95,124,377-382]. Câu hỏi gốc: BD đã bổ sung nút đánh dấu trên từng dòng, nhưng chưa có đường nào để lọc ra các câu đã đánh dấu — mà đánh dấu mà không lọc lại được thì gần như vô dụng. | Prototype không có cả nút lẫn bộ lọc; RD đưa F6-03 vào phạm vi màn [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:126] nhưng không mô tả cách xem lại. | Thêm một công tắc "Chỉ câu đã đánh dấu" cạnh bốn tab trạng thái, chứ **không** thêm tab thứ năm — đánh dấu và trạng thái ôn tập là hai trục độc lập, gộp vào cùng một dải tab sẽ không lọc chéo được. Nếu chủ dự án đồng ý, BD này cập nhật Sheet 5 khu vực B thêm một item. | Chủ dự án |
| Q9 | ~~**Số thẻ của một phiên luyện nhanh là cố định 10 hay cho người học chọn?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** `DEC-2026-1018-usr-open-questions-phase2-5` (PHASE 3, bộ thẻ cố định 10). Ponytail bậc 5 (có mặc định hợp lý nên là hằng số, không thêm điều khiển). Bản dựng đã là hằng số `DRILL_SIZE = 10` [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:45-47]. Câu hỏi gốc: Prototype để nó là một tham số cấu hình (mặc định 10, khoảng 5 tới 20) chứ không phải điều khiển trên màn [Nguồn: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:235]. | Tham số cấu hình của bản mẫu không nói được ý định sản phẩm: nó có thể là một hằng số, cũng có thể là dấu vết của một điều khiển chưa dựng. | Cố định 10 ở bản đầu, ghi thành hằng số cấu hình giao diện. Thêm một ô chọn số thẻ là thêm một điều khiển và một trạng thái cần nhớ, đổi lấy rất ít. | Chủ dự án |
| Q10 | ~~**Bộ lọc độ khó có đi theo tham số URL và đường quay lại từ `USR0402` không?**~~ **ĐÃ CHỐT 2026-10-18 (owner), theo đề xuất.** `DEC-2026-1018-usr-open-questions-phase2-5` (PHASE 3). Ponytail bậc 4 (dùng lại cơ chế tham số URL đã có của `topic`). Tham số `level` (một mã độ khó) vào hợp đồng, bỏ qua nếu không khớp như `topic`, cũng đọc khi quay lại từ `USR0402`; `solution_review` không gửi. Đã sửa Sheet 3, Sheet 5 khu vực B NO 4, Sheet 8 EVT-1, EVT-9, EVT-15, Sheet 9 NO 4 của màn này và Sheet 3 của `USR0402`. **Prototype chưa làm (owner chốt lại 2026-10-19: giữ là nợ, làm một lượt cho cả bốn tham số khi dựng route thật, ponytail bậc 3):** view không đọc bộ lọc nào từ URL, ghi nợ ở mục "Hiện trạng bản dựng" [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:90-95,488-491]. Câu hỏi gốc: Hợp đồng tham số hiện chỉ có `topic`, `q`, `status` (Sheet 3, mục "Ngân hàng câu hỏi phỏng vấn → Chi tiết câu hỏi phỏng vấn"); bản dựng không ghi tab độ khó lên URL và không truyền gì sang màn chi tiết [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:90-95,488-491]. | Thêm `level` vào hợp đồng là đổi tên tham số dùng chung hai màn (`USR0402` Sheet 3) nên chưa tự quyết trong BD này. Hậu quả nếu không thêm: người học quay lại từ màn chi tiết thì mất tab độ khó đang chọn. | Thêm tham số `level` (một mã độ khó) vào hợp đồng, cùng quy tắc bỏ qua nếu không khớp như `topic`; `solution_review` không cần gửi. Sửa đồng thời `USR0402` Sheet 3 và Sheet 9 NO 4 của màn này. Duyệt rồi mới sửa. | Chủ dự án |
| Q11 | ~~**RD còn yêu cầu thanh chỉ số mà bản dựng đã bỏ.**~~ **ĐÃ CHỐT 2026-10-18 (owner), RD đã sửa:** RD `USR0401` đã bỏ mục "Thanh chỉ số" (nay là ghi chú đầu mục "Trạng thái `browse`" và các khoản đánh lại 1-3), `DEC-2026-1018-usr-open-questions-phase1-scope` mục (1), ponytail bậc 1. **Đối chiếu F6-09/F6-10 còn được đáp ứng ở đâu:** F6-09 "câu cần ôn lại" còn qua tab lọc "Đang ôn" (Sheet 5 Khu vực B NO 2) và hàng đợi Luyện nhanh (Sheet 8 EVT-11); "lịch sử các lượt" của một câu hỏi ở `USR0402` khu vực E [Nguồn: 02-bd/screens/users/USR0402_interview_question_detail.md:226]. **F6-10 "tỉ lệ hoàn thành theo chủ đề" do dashboard `USR0601` nhận** (owner chốt 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`, mục FOLLOW-UPS: dashboard `USR0601` takes F6-10; ponytail bậc 4, dùng `practice_history` đã có). Màn này không hiển thị F6-10. Trước quyết định này `USR0402` Q5 nói `my_progress` tổng hợp theo chủ đề nhưng `my_progress` đã gộp vào `USR0601` [Nguồn: 01-rd/screens/users/USR0601_dashboard.md:18] và RD/BD của `USR0601` chưa có mã `F6-nn` nào; việc ghi F6-10 vào RD/BD của `USR0601` do bản sửa của chính màn đó làm, không thuộc BD này. Câu hỏi gốc: RD mục "Trạng thái `browse`" khoản 1 mô tả "Thanh chỉ số — Tổng câu hỏi, Đã thuộc, Đang ôn, Ôn trong tuần" [Nguồn: 01-rd/screens/users/USR0401_interview_bank_list.md:44-46]. Chủ dự án quyết 2026-10-18 bỏ bốn thẻ này khỏi màn (mở rộng quy tắc 2026-10-13 "không dải thẻ chỉ số ở màn danh sách"); BD này và bản dựng đã theo quyết định đó [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:302-327]. | Không còn: RD `USR0401` đã sửa theo, quyết định có trong `DEC-2026-1018-usr-open-questions-phase1-scope` mục (1) và `DEC-2026-1018-usr-open-questions-phase2-5` (F6-10 thuộc dashboard). | Còn lại: đồng bộ DD `interview-bank` (nếu có `GetMyRecallSummary`) và `04-tdd` khi các file đó được viết. | Chủ dự án |
| Q12 | **ĐÃ CHỐT PHẦN THANH LỌC 2026/10/21 (owner): dùng `FilterBar` chung, không dựng thanh riêng; ponytail bậc 4 (dùng lại).** Phần còn lại vẫn mở. **Giao diện dựng lại 2026/10/20 (tiêu đề và phụ đề, thanh công cụ lọc ngang, tỉ lệ 5:6, bản xem nhanh) đã được duyệt hình chưa; dòng danh sách có phải "nền nhạt + viền" không?** Mới là yêu cầu của owner, **chưa duyệt hình**. Mã dựng dòng danh sách ở entity dùng chung chỉ có nền `surface-hover` cho dòng chọn và viền dưới [Nguồn: 05-coding/frontend/src/entities/interview-question/ui/interview-question-list-row.tsx:46-53]; nếu owner muốn dòng nền nhạt có viền riêng thì phải sửa entity, ảnh hưởng `interview-question-management`. | Owner chưa duyệt ảnh chụp; mô tả "tinted outline rows" không khớp mã đọc được. | Chụp Playwright thật ở 1280px, sáng và tối, rồi duyệt. Thanh lọc đã chuyển sang `FilterBar` chung theo quyết định 2026/10/21; còn lại chờ owner duyệt ảnh và chốt dòng danh sách. | Chủ dự án |
| Q13 | **`total` trong "Hiển thị {shown} / {total} câu" là gì khi có phân trang máy chủ?** Bản dựng lấy độ dài mảng mock [Nguồn: 05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx:387-392]; RD/BD chốt danh sách cuộn tải 20 dòng mỗi lần (Sheet 4.1 Số bản ghi tối đa). | `ListInterviewQuestions` chưa có DD nên chưa biết có trả tổng số bản ghi khớp bộ lọc hay không. | `total` là tổng câu `ACTIVE` của toàn ngân hàng (không áp bộ lọc), `shown` là số khớp bộ lọc; `ListInterviewQuestions` cần trả cả hai số, ghi vào DD `interview-bank`. `[SoT: Suy luận]` — ngữ nghĩa "tổng" do BD đề xuất, owner chưa chốt. **ĐÃ CHỐT 2026-10-23 (chủ dự án, `DEC-2026-1023-open-questions-closed`):** `total` là tổng câu `ACTIVE` của toàn ngân hàng, không áp bộ lọc; `shown` là số khớp bộ lọc; `ListInterviewQuestions` trả cả hai số (ghi vào DD `interview-bank`). | Đã đóng |

---

> [Nội bộ] Tiêu chí nghiệm thu `AC-nn` của màn này viết ở `04-tdd/interview_bank_list.md` cùng lúc với
> DD màn hình, không viết trong file BD.
