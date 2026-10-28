# Tiêu chí nghiệm thu — Màn `problem_detail` (`USR0102`)

> Màn: `USR0102` Workspace giải bài · DD màn: `03-dd/screens/users/USR0102_problem_detail.md` · Cập nhật: 2026-10-23.
> File này **chỉ** chứa các luồng chạy qua nhiều module hoặc hành vi trình duyệt mà `AC` của module không phủ (`03-dd/screens/README.md` mục 2). Hành vi một module đã có `AC` ở `04-tdd/modules/F2_problem-bank.md`, `F3_harness.md`, `F4_judge-orchestration.md`, `F5_ai-review.md` thì không lặp lại; mục 3 chỉ trỏ tới.
> Viết cùng DD theo `.claude/rules/tdd-mode.md`. Nội dung ở ngôn ngữ nghiệp vụ; tên endpoint, mã lỗi nằm ở cột truy vết.

## 1. Phạm vi

**Trong phạm vi**: mở màn khi một module phụ lỗi; chọn ngôn ngữ, mô hình và nháp mã; chặn gửi ở trình duyệt; nộp bài, theo dõi từng testcase, nối lại giữa chừng và mất kênh; hiện liên kết đi tiếp khi AI sống hay chết; chạy thử; lưu bài.

**Ngoài phạm vi**: nội dung bản tin và mã lỗi (`F4`); sinh mã khung (`F3`); thuật toán chấm (`F4`); báo cáo phân tích (`F5`); màu sắc, bố cục chi tiết.

**Quy ước dữ liệu mẫu**: "bài X" là bài đã xuất bản có testcase mẫu và testcase ẩn (12 testcase, trong đó 2 mẫu); "học viên" là tài khoản học viên đã đăng nhập; "đường truyền" là kết nối thời gian thực của trình duyệt.

## 2. Danh sách tiêu chí

### PD-AC-01 — Một module phụ lỗi không chặn soạn mã và nộp bài
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X đã xuất bản. Lần lượt: dịch vụ lịch sử nộp lỗi, dịch vụ giới hạn thực thi lỗi, dịch vụ phân tích AI lỗi (mỗi lần một, rồi cả ba cùng lúc) |
| **Thao tác** | Học viên mở bài X |
| **Kết quả mong đợi** | Đề bài, mã khung và hai nút Run Code, Submit vẫn dùng được trong mọi trường hợp. Lịch sử nộp lỗi thì tab Bài nộp báo lỗi riêng kèm nút thử lại, hai tab còn lại bình thường. Giới hạn thực thi lỗi thì dòng giới hạn biến mất, không hiện con số thay thế. AI lỗi thì tab Solution Review biến mất mà không có thông báo lỗi. Chỉ khi chính đề bài không tải được mới có vùng lỗi toàn màn |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:793, 841] |

### PD-AC-02 — Cặp (ngôn ngữ, mô hình): nút Bọc hàm, mã khung và nháp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X hỗ trợ cả hai mô hình; bài Y chỉ hỗ trợ Standard I/O. Học viên đã gõ mã Java Bọc hàm cho bài X |
| **Thao tác** | Chuyển sang C++, rồi quay lại Java; chuyển sang "Tải file" rồi sang "Có hàm main"; mở bài Y với lựa chọn đã nhớ là Bọc hàm; chặn bộ nhớ trình duyệt rồi gõ mã |
| **Kết quả mong đợi** | Quay lại Java thấy đúng mã đã gõ, không mất. Mỗi cặp có nháp riêng; cặp chưa có nháp hiện mã khung của cặp đó. Đổi mô hình không đổi phương thức nhập mã. Bài Y không có nút Bọc hàm, luôn Standard I/O dù lựa chọn đã nhớ. Khi trình duyệt chặn lưu thì vẫn gõ được và **không** hiện nhãn "Đã lưu trên trình duyệt này". Đổi tab thông tin không làm đổi mã đang soạn |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:645-646, 793-798; 04-tdd/modules/F3_harness.md AC-01, AC-04] |

### PD-AC-03 — Nộp bài: ô tô theo thứ tự, không lộ nội dung testcase ẩn trên màn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X (12 testcase). Mã của học viên sai ở testcase ẩn số 7 |
| **Thao tác** | Học viên bấm Submit và theo dõi đến khi chấm xong, rồi mở màn phụ liệt kê testcase |
| **Kết quả mong đợi** | Nhãn "Đang chờ", "Đang biên dịch", "Đang chấm" lần lượt hiện; 12 ô tô dần theo thứ tự 1 đến 12; chấm đủ 12 testcase, không dừng ở testcase sai. Trong lúc chấm, điều khiển chọn ngôn ngữ, mô hình, phương thức nhập và ô soạn mã đều bị khoá, Submit và Run Code không bấm được. Mỗi testcase ẩn chỉ hiện "Đạt" hoặc "Không đạt"; không nơi nào trên màn (ô, chú giải, màn phụ, thông báo, văn bản đọc cho trình đọc màn hình) chứa dữ liệu vào, kết quả mong đợi hay output của testcase ẩn |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:671, 806, 831; 04-tdd/modules/F4_judge-orchestration.md AC-15, AC-25, AC-29] |

### PD-AC-04 — Nối lại giữa chừng và mở lại màn khi bài còn đang chấm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp 12 testcase đang chấm, đã xong 4; đường truyền rớt; trong lúc rớt xong thêm 3 |
| **Thao tác** | (a) Đường truyền trở lại. (b) Học viên tải lại trang trong lúc bài vẫn đang chấm. (c) Bài kết thúc trong lúc đường truyền rớt |
| **Kết quả mong đợi** | (a) Thấy ngay 7 ô đúng màu rồi nhận tiếp các ô còn lại, không ô nào bị tô trùng hay sai thứ tự. (b) Màn tiếp tục theo dõi đúng lượt nộp đang chạy và Submit vẫn khoá cho đến khi lượt đó kết thúc. (c) Thấy ngay kết quả cuối, không cần tải lại |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md:146-156; 04-tdd/modules/F4_judge-orchestration.md AC-27] |

### PD-AC-05 — Mất đường truyền hoàn toàn: hỏi lại định kỳ, không kết luận trượt, không mất mã
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài đang chấm; đường truyền thời gian thực không nối lại được |
| **Thao tác** | Học viên chờ |
| **Kết quả mong đợi** | Màn tự hỏi lại trạng thái mỗi 3 giây, tối đa 10 lần, và vẫn cập nhật nếu có kết quả. Hết 10 lần mà chưa xong thì hiện một dòng "cập nhật đang chậm"; màn **không** kết luận bài trượt. Mã nguồn trong ô soạn còn nguyên. Tải lại trang thấy đúng trạng thái thật |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:804; 04-tdd/modules/F4_judge-orchestration.md AC-28] |

### PD-AC-06 — Khi chấm xong: lịch sử tự làm mới, liên kết đi tiếp đúng điều kiện
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X; học viên nộp ba lần với kết quả: sai, quá thời gian, đúng. Phân hệ AI hoạt động ở lượt một, hỏng ở lượt hai |
| **Thao tác** | Quan sát sau mỗi lượt kết thúc; bấm một dòng trong tab Bài nộp |
| **Kết quả mong đợi** | Tab Bài nộp tự có thêm dòng mới, không cần làm mới. Có thông báo kết quả (thành công cho đúng, lỗi kèm số testcase đạt cho còn lại). Liên kết "Xem kết quả chấm" hiện sau **mọi** lượt đã kết thúc. Hai liên kết "Phân tích bài giải" và "Phỏng vấn giả lập" chỉ hiện khi lượt đúng và AI không hỏng. Bấm một dòng lịch sử mở màn kết quả của lượt đó |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:674-676, 804, 814; 04-tdd/modules/F4_judge-orchestration.md AC-68] |

### PD-AC-07 — Chạy thử không để lại dấu vết và chỉ lộ testcase mẫu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X (2 mẫu, 10 ẩn); tab Bài nộp có 3 lượt |
| **Thao tác** | Học viên bấm Run Code với mã sai ở một testcase mẫu; sau đó với mã không biên dịch được |
| **Kết quả mong đợi** | Hiện đúng 2 testcase mẫu với dữ liệu vào, kết quả thực tế, kết quả mong đợi, thời gian, bộ nhớ; mở sẵn testcase sai đầu tiên. Không lượt nào trong tab Bài nộp được thêm; tiến độ cá nhân không đổi. Không có thông tin nào về 10 testcase ẩn ngoài con số 10 ở câu hướng dẫn. Mã không biên dịch được thì hiện thông báo lỗi chỉ đúng dòng của học viên, không chứa dòng mã khung |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:560, 802; 04-tdd/modules/F4_judge-orchestration.md AC-10, AC-11] |

### PD-AC-08 — AI hỏng: chỉ mất phần AI
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Phân hệ AI không phản hồi hoặc hết hạn mức |
| **Thao tác** | Học viên mở bài, soạn mã, chạy thử, nộp bài đến khi `Accepted` |
| **Kết quả mong đợi** | Mọi bước đọc đề, soạn mã, chạy thử, nộp, xem kết quả chạy bình thường. Tab Solution Review và hai liên kết sang phân tích, phỏng vấn không hiện, và không có thông báo lỗi nào về AI |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:841; 04-tdd/modules/F4_judge-orchestration.md AC-68; 04-tdd/modules/F3_harness.md AC-47] |

### PD-AC-09 — Lưu bài lạc quan và hoàn nguyên khi lỗi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X chưa lưu |
| **Thao tác** | Học viên bấm "Lưu bài" khi máy chủ nhận; rồi bấm "Bỏ lưu" khi máy chủ báo lỗi |
| **Kết quả mong đợi** | Nhãn đổi ngay sang "Bỏ lưu" kèm thông báo "Đã lưu bài" có hành động xem bài đã lưu; bài hiện ở danh sách bài đã lưu. Khi lỗi, nhãn quay lại trạng thái cũ và có thông báo lỗi; mã đang soạn không mất. Bấm hai lần liên tiếp không tạo hai bản ghi |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:595, 807; 04-tdd/modules/F2_problem-bank.md AC-15] |

### PD-AC-10 — Chặn gửi ở trình duyệt, máy chủ vẫn là nơi kiểm thật
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên ở màn bài X |
| **Thao tác** | Bấm Run Code hoặc Submit với mã toàn khoảng trắng; với mã đúng 64 KB và 64 KB cộng một byte (có chữ tiếng Việt); chọn tệp sai đuôi ngôn ngữ; chọn tệp lớn hơn 64 KB; chọn "Tải file" mà chưa chọn tệp |
| **Kết quả mong đợi** | Mỗi trường hợp bị chặn sớm với một thông báo, không gọi máy chủ, mã trong ô soạn không đổi. Đúng 64 KB được gửi; hơn một byte bị chặn (đếm theo byte, không theo ký tự). Tệp sai đuôi hoặc quá lớn bị từ chối trước khi đọc nội dung và tệp đã tải trước đó được giữ. Khi một kiểm tra phía trình duyệt bị bỏ qua mà máy chủ vẫn từ chối thì màn hiện thông báo tương ứng, không mất mã |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:833-836; 04-tdd/modules/F4_judge-orchestration.md AC-04, AC-07] |

### PD-AC-11 — Hoàn tác mã trước khi áp dụng gợi ý AI
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có mã đang soạn, sau đó xác nhận áp dụng mã gợi ý từ báo cáo phân tích |
| **Thao tác** | Mở Workspace và bấm Hoàn tác; thử lại khi bộ nhớ trình duyệt không cho ghi |
| **Kết quả mong đợi** | Workspace hiện mã gợi ý và cho khôi phục đúng bản mã trước khi áp dụng bằng một nút. Nếu không sao lưu được bản cũ, mã gợi ý không ghi đè và người học ở lại màn phân tích; mã đang soạn không mất |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0102_problem_detail.md] BR-SC-10 |

### PD-AC-12 — Kéo vùng soạn mã vẫn giữ bố cục hợp lệ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên đang giải bài trên màn có vùng đề và vùng soạn mã |
| **Thao tác** | Kéo vạch chia tới hai đầu giới hạn, tải lại trang rồi đổi sang màn hình hẹp |
| **Kết quả mong đợi** | Tỉ lệ hai vùng được giữ trong khoảng cho phép và nhớ ở trình duyệt; đề và ô soạn luôn còn diện tích sử dụng được, không biến mất do kéo quá xa |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0102_problem_detail.md] EVT-20, T-PD-29 |

### PD-AC-13 — Hết lượt chạy thử vẫn giữ mã đang soạn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học đã dùng hết 60 lượt Chạy thử trong 60 phút gần nhất và còn mã đang soạn. |
| **Thao tác** | Bấm Chạy thử thêm một lần, chờ tới lúc được thử lại. |
| **Kết quả mong đợi** | Màn báo thời gian chờ theo phản hồi máy chủ, không thêm kết quả chạy giả và không xoá mã hoặc bản nháp. Sau khi hết hạn, người học có thể chạy thử lại; lượt nộp bài vẫn dùng hạn mức riêng. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0102_problem_detail.md] T-PD-16A; [SoT: 03-dd/api/F4_judge-orchestration.md] §5 |

## 3. Truy vết

Bảng 1 nối tiêu chí của màn tới `AC` module đã phủ phần nghiệp vụ tương ứng và tới điểm kiểm thử ở DD màn (`T-PD-nn`). Cột "Test case" điền khi có.

**Bảng 1: Truy vết**

| AC | AC module liên quan | Điểm kiểm DD màn | Test case | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| PD-AC-01 | `F2 AC-08`, `F4 AC-68` | T-PD-01, 03, 04, 05 | [TC-USR0102-PDAC01-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-02 | `F3 AC-01`, `F3 AC-04` | T-PD-06 đến 10 | [TC-USR0102-PDAC02-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-03 | `F4 AC-15`, `AC-25`, `AC-29` | T-PD-17, 19, 24, 25, 30 | [TC-USR0102-PDAC03-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-04 | `F4 AC-27` | T-PD-21, 23 | [TC-USR0102-PDAC04-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-05 | `F4 AC-28` | T-PD-22 | [TC-USR0102-PDAC05-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-06 | `F4 AC-68`, `F5 AC-16` | T-PD-20, 27, 28, 30 | [TC-USR0102-PDAC06-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-07 | `F4 AC-10`, `AC-11` | T-PD-14, 15 | [TC-USR0102-PDAC07-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-08 | `F4 AC-68`, `F3 AC-47` | T-PD-04, 27 | [TC-USR0102-PDAC08-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-09 | `F2 AC-15` | T-PD-26 | [TC-USR0102-PDAC09-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-10 | `F4 AC-04`, `AC-07` | T-PD-11, 12, 13 | [TC-USR0102-PDAC10-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-11 | BR-SC-10, `USR0301` EVT-6 | Chưa có điểm kiểm DD riêng | [TC-USR0102-PDAC11-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-12 | EVT-20 | T-PD-29 | [TC-USR0102-PDAC12-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |
| PD-AC-13 | `F4 AC-73` | T-PD-16A | [TC-USR0102-PDAC13-01](../../testcases/screens/users/USR0102_problem_detail.md) | Chưa thực hiện |

## 4. Chưa chốt

Không còn quyết định chủ dự án chờ chốt cho màn này. Mở lại lúc đang chấm và khoá điều khiển đã có hợp đồng DD; ô soạn mã ở chế độ chỉ đọc và lịch sử nháp là việc cần dựng khi viết code. Cột test case được điền khi tạo kiểm thử tự động.
