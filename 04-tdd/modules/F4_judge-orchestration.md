# Tiêu chí nghiệm thu — Module `judge-orchestration`

> Bounded Context: F4 `judge-orchestration` · DD: `03-dd/api/F4_judge-orchestration.md`, `03-dd/logic/F4_judge-orchestration.md`, `03-dd/validation/F4_judge-orchestration.md`, `03-dd/jobs/F4_judge-orchestration.md` · Cập nhật: 2026-10-23.
> Viết cùng lúc với DD theo `.claude/rules/tdd-mode.md`. Nội dung mỗi tiêu chí ở ngôn ngữ nghiệp vụ, người nghiệm thu kiểm được mà không cần đọc mã; tên endpoint, mã lỗi và bảng nằm ở cột truy vết (mục 3) chứ không nằm trong thân tiêu chí.

## 1. Phạm vi

**Trong phạm vi**: nộp bài và chạy thử; chấm từng testcase chạy hết, trạng thái và điểm tỷ lệ; kênh thời gian thực; kết quả của học viên; lịch sử và số liệu nộp bài; kết quả bài nộp cho giảng viên theo phạm vi lớp; giám sát hàng đợi, sự kiện hạ tầng, cấu hình ngôn ngữ và sandbox cho quản trị viên; sự kiện miền phát ra; cổng đọc cho module khác.

**Ngoài phạm vi**: chấm lại (đã loại, `DEC-2026-0828-remove-rejudge-scope`); luồng webhook của engine bất đồng bộ (chi tiết của adapter, chỉ có khi cắm lại Judge0); nội dung báo cáo phân tích và phỏng vấn của F5; chi tiết giao diện (thuộc `04-tdd/<màn>.md` khi có). Bốn job nền ở `03-dd/jobs/F4_judge-orchestration.md` được phủ ở đây ở **kết quả quan sát được** (AC-23, AC-24, AC-64); lịch chạy và cỡ lô là chi tiết của file job.

**Quy ước dữ liệu mẫu**: "bài X" là một bài đã xuất bản có đủ testcase mẫu và testcase ẩn; "máy chấm" là cụm go-judge; "học viên" là tài khoản vai trò học viên; "giảng viên" là tài khoản vai trò giảng viên có quyền quản lý lớp; "quản trị viên" là tài khoản vai trò quản trị.

## 2. Danh sách tiêu chí

### 2.1. Nộp bài và chạy thử

### AC-01 — Nộp bài hợp lệ nhận phản hồi ngay, không chờ chấm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên đã đăng nhập, mở bài X, ngôn ngữ Java đang bật, mã không rỗng và nhỏ hơn 64 KB. Hàng đợi chấm đang chạy |
| **Thao tác** | Học viên bấm Nộp bài |
| **Kết quả mong đợi** | Học viên nhận ngay mã bài nộp kèm trạng thái "Đang chờ", không phải đợi kết quả chấm. Bài nộp xuất hiện trong lịch sử của chính học viên. Chưa có điểm, chưa có số testcase đạt (hiển thị dấu gạch, không phải 0/0) |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:10-11]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:800] |

### AC-02 — Vượt số lượt nộp tối đa trong một giờ bị từ chối
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X không khai giới hạn riêng (dùng mặc định hệ thống 60 lượt mỗi giờ). Học viên đã nộp đúng 60 lượt cho bài X trong 60 phút gần nhất |
| **Thao tác** | Học viên nộp thêm một lượt cho bài X; sau đó thử lại với một bài Y khác |
| **Kết quả mong đợi** | Lượt thứ 61 cho bài X bị từ chối với thông báo vượt số lần cho phép trong một giờ, **không** tạo bài nộp mới. Lượt nộp cho bài Y vẫn được nhận (giới hạn tính theo từng cặp người và bài). Khi lượt nộp cũ nhất rời khỏi cửa sổ một giờ, học viên nộp lại được |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:835]; [SoT: 02-bd/database/F2_problem-bank.md:295] |

### AC-03 — Giới hạn riêng của bài thay cho mặc định
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài Z được người soạn khai tối đa 5 lượt nộp mỗi giờ. Học viên đã nộp 5 lượt cho Z trong giờ qua |
| **Thao tác** | Học viên nộp lượt thứ 6 cho Z |
| **Kết quả mong đợi** | Bị từ chối vì vượt giới hạn; giới hạn 60 mặc định **không** áp cho bài này |
| **Nguồn** | [SoT: 02-bd/database/F2_problem-bank.md:29] |

### AC-04 — Mã nguồn rỗng hoặc quá 64 KB bị máy chủ từ chối
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên (hoặc công cụ gọi thẳng vào máy chủ, bỏ qua giao diện) gửi mã nguồn |
| **Thao tác** | Gửi lần lượt: mã toàn khoảng trắng; mã dài đúng 65.536 byte; mã dài 65.537 byte; mã tiếng Việt có dấu mà số ký tự dưới 65.536 nhưng số byte vượt 65.536 |
| **Kết quả mong đợi** | Mã toàn khoảng trắng bị từ chối. Mã đúng 65.536 byte được nhận. Mã 65.537 byte bị từ chối. Mã tiếng Việt vượt 65.536 byte bị từ chối (giới hạn tính theo **byte**, không theo số ký tự). Không bài nộp nào được tạo ở các trường hợp bị từ chối. Việc này giống nhau cho cả nộp bài và chạy thử |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:24]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:832-833] |

### AC-05 — Ngôn ngữ đã tắt không nhận bài mới, bài đang chờ vẫn chạy nốt
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có 3 bài nộp Python đang chờ trong hàng đợi. Quản trị viên vừa tắt Python (Java và C++ vẫn bật) |
| **Thao tác** | Học viên nộp một bài mới bằng Python; ba bài nộp Python đang chờ tiếp tục được xử lý |
| **Kết quả mong đợi** | Bài nộp Python mới bị từ chối với thông báo ngôn ngữ đang tắt. Ba bài Python đã vào hàng đợi **vẫn được chấm tới cùng** và ra kết quả bình thường, không bị huỷ hay đổi sang lỗi hệ thống |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:74-83]; [SoT: 02-bd/screens/admin/ADM0501_language_config.md:504] |

### AC-06 — Bài chỉ hỗ trợ Standard I/O từ chối mô hình Bọc hàm (kiểm ở máy chủ)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài S chỉ hỗ trợ Standard I/O. Giao diện đã ẩn nút Bọc hàm, nhưng người dùng sửa tay tham số để chọn Bọc hàm |
| **Thao tác** | Gửi yêu cầu nộp bài (và chạy thử) cho bài S với mô hình Bọc hàm |
| **Kết quả mong đợi** | Cả hai bị máy chủ từ chối với thông báo bài chỉ hỗ trợ Standard I/O; không tạo bài nộp. Hai mô hình đều dùng được với bài hỗ trợ cả hai |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:834] |

### AC-07 — Tệp tải lên sai phần mở rộng bị từ chối
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên chọn ngôn ngữ Python và nộp nội dung từ một tệp |
| **Thao tác** | Nộp với tên tệp `main.py`; nộp lại với tên tệp `Main.java` |
| **Kết quả mong đợi** | `main.py` được nhận và bài nộp ghi nhận nguồn là tải tệp. `Main.java` bị từ chối vì phần mở rộng không khớp ngôn ngữ đang chọn. Máy chủ không lưu tên tệp |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:13-15]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:830] |

### AC-08 — Hàng đợi chấm không nhận được bài nộp: học viên được báo, bài không kẹt mãi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hàng đợi chấm đang tắt (không nhận được tin nhắn) |
| **Thao tác** | Học viên nộp một bài hợp lệ |
| **Kết quả mong đợi** | Học viên nhận thông báo lỗi để thử lại; mã đang soạn không bị mất. Lượt vừa ghi được kết thúc ngay bằng lỗi hệ thống và có thể tìm thấy trong lịch sử; không chờ năm phút. Nếu máy chấm đã nhận lượt đó thì học viên nhận mã để tiếp tục theo dõi kết quả thật; hệ thống không ghi đè tiến trình đang chấm. Việc này không làm bài nộp khác bị ảnh hưởng |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:213-232]; [SoT: 03-dd/jobs/F4_judge-orchestration.md:29-70] |

### AC-09 — Hai lượt nộp đồng thời không cùng vượt giới hạn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên đã nộp 59 lượt cho bài X trong giờ qua (còn đúng một suất). Hai thiết bị cùng gửi lượt nộp tại cùng một thời điểm |
| **Thao tác** | Cả hai yêu cầu tới máy chủ đồng thời |
| **Kết quả mong đợi** | Đúng một lượt được nhận, lượt kia bị từ chối vì vượt giới hạn; tổng số lượt trong giờ không vượt 60 |
| **Nguồn** | [SoT: Suy luận] kiểm rồi ghi phải nguyên tử, xem `03-dd/logic/F4_judge-orchestration.md` BR-JD-01 bước 4 |

### AC-10 — Chạy thử chỉ dùng testcase mẫu và không lưu gì
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có 3 testcase mẫu và 12 testcase ẩn. Học viên có mã đúng |
| **Thao tác** | Học viên bấm Chạy thử |
| **Kết quả mong đợi** | Kết quả trả ngay trong cùng lần bấm với đúng 3 testcase mẫu, mỗi testcase có kết quả đạt hoặc không đạt, thời gian, bộ nhớ và output thực tế. **Không** có testcase ẩn nào được chạy hay nhắc tới. Danh sách bài nộp của học viên không tăng thêm dòng nào, thống kê tiến độ và tỉ lệ chấp thuận không đổi |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:16]; [SoT: 02-bd/database/F4_judge-orchestration.md:46] |

### AC-11 — Chạy thử lỗi biên dịch báo đúng dòng của học viên
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên chọn mô hình Bọc hàm và viết hàm có lỗi cú pháp ở dòng 3 của phần mã họ gõ |
| **Thao tác** | Bấm Chạy thử |
| **Kết quả mong đợi** | Kết quả nêu lỗi biên dịch và chỉ dòng **3** (dòng trong mã của học viên, không phải dòng trong tệp ghép). Không chứa bất kỳ đoạn nào của phần mã khung do hệ thống sinh. Không có kết quả testcase nào |
| **Nguồn** | [SoT: 02-bd/architecture/F3_harness.md:261-282]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:799] |

### AC-12 — Chạy thử khi máy chấm không phản hồi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Máy chấm ngừng hoạt động hoặc không trả lời |
| **Thao tác** | Học viên bấm Chạy thử |
| **Kết quả mong đợi** | Sau tối đa 60 giây học viên nhận thông báo lỗi hệ thống, mã đang soạn còn nguyên, bấm Chạy thử lại được. Không có dòng nào được ghi, kể cả dòng lỗi |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:799]; [SoT: Suy luận] hạn 60 giây, xem `03-dd/api/F4_judge-orchestration.md` mục 3 |

### AC-13 — Quá nhiều lượt chạy thử đồng thời không làm nghẽn việc chấm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Đang có 8 lượt chạy thử chạy cùng lúc |
| **Thao tác** | Học viên thứ 9 bấm Chạy thử; đồng thời một bài nộp thật được gửi |
| **Kết quả mong đợi** | Lượt chạy thử thứ 9 được báo hệ thống đang bận, thử lại sau; bài nộp thật vẫn được nhận và chấm bình thường |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md] mục 3; quyết định giới hạn chạy thử 2026-10-26 |

### AC-14 — Giới hạn thời gian và bộ nhớ hiển thị đã nhân hệ số ngôn ngữ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X giới hạn 1.000 ms và 256 MB; hệ số thời gian Java 2,00, C++ 1,00, Python 3,00; hệ số bộ nhớ Java 2,00; Python vừa bị tắt |
| **Thao tác** | Học viên mở bài X |
| **Kết quả mong đợi** | Java hiển thị 2.000 ms và 512 MB; C++ hiển thị 1.000 ms và 256 MB; Python hiển thị 3.000 ms nhưng được đánh dấu đang tắt (không chọn được cho bài nộp mới). Bài không tồn tại hoặc chưa xuất bản nhận thông báo "không tìm thấy bài toán" |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:113-115]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:827] |

### 2.2. Chấm bài và trạng thái

### AC-15 — Chạy hết mọi testcase dù có testcase sai ở giữa
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có 12 testcase. Mã học viên sai ở testcase thứ 3 và thứ 8 |
| **Thao tác** | Học viên nộp và chờ chấm xong |
| **Kết quả mong đợi** | Cả 12 testcase đều được chạy (không dừng ở testcase thứ 3); kết quả có đủ 12 dòng, với 10 đạt và 2 không đạt; điểm tỷ lệ hiển thị 10 trên 12 (khoảng 0,833 trên thang 1 điểm) |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:17-33]; DEC-2026-0831-partial-score-testcase-ratio |

### AC-16 — Đúng hết là Accepted, điểm tỷ lệ bằng 1
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có 12 testcase; mã học viên đúng cả 12 |
| **Thao tác** | Nộp và chờ chấm xong |
| **Kết quả mong đợi** | Trạng thái cuối là Accepted; điểm tỷ lệ là 1 trên 1; số testcase đạt 12 trên 12; thời gian và bộ nhớ hiển thị là giá trị lớn nhất trong các testcase |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:23-30] |

### AC-17 — Trạng thái theo testcase sai đầu tiên, điểm tỷ lệ độc lập với trạng thái
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có 10 testcase. Testcase 2 quá thời gian, testcase 5 sai kết quả, testcase 7 lỗi khi chạy; còn lại đạt |
| **Thao tác** | Nộp và chờ chấm xong |
| **Kết quả mong đợi** | Trạng thái cuối là **Quá thời gian** (theo testcase sai đầu tiên theo thứ tự, là testcase 2), không phải Sai kết quả hay Lỗi thực thi. Số testcase đạt 7 trên 10, điểm tỷ lệ 0,7. Trạng thái Accepted không bị ảnh hưởng bởi điểm tỷ lệ (bài này không phải Accepted dù có 70% testcase đạt) |
| **Nguồn** | [SoT: 03-dd/logic/F4_judge-orchestration.md] BR-JD-05; quyết định lỗi đầu tiên theo thứ tự testcase 2026-10-25 |

### AC-18 — Vượt bộ nhớ được gộp vào nhãn Quá thời gian
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Mã học viên vượt giới hạn bộ nhớ ở testcase đầu tiên không đạt |
| **Thao tác** | Nộp và chờ chấm xong |
| **Kết quả mong đợi** | Trạng thái cuối của bài nộp là Quá thời gian; dòng kết quả của riêng testcase đó vẫn hiển thị là vượt bộ nhớ |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:60] |

### AC-19 — Lỗi biên dịch: không chạy testcase nào, không có điểm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Mã học viên không biên dịch được |
| **Thao tác** | Nộp và chờ chấm xong |
| **Kết quả mong đợi** | Trạng thái cuối là Lỗi biên dịch kèm thông báo lỗi đúng dòng của học viên. **Không** có testcase nào được chạy và không có bảng kết quả testcase. Điểm tỷ lệ và số testcase đạt hiển thị dấu gạch (không hiển thị 0 hay 0%) |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:28,31]; [SoT: 02-bd/screens/users/USR0201_submission_result.md:306] |

### AC-20 — Worker chết giữa chừng: chấm tiếp, không chạy lại testcase đã xong
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp đang chấm, đã xong 5 trên 12 testcase thì tiến trình chấm bị tắt đột ngột |
| **Thao tác** | Hệ thống giao lại bài nộp cho một tiến trình chấm khác |
| **Kết quả mong đợi** | Bài được chấm tiếp từ testcase thứ 6; năm testcase đầu **giữ nguyên kết quả cũ** và không được chạy lại; cuối cùng có đúng 12 dòng kết quả, không dòng nào trùng. Học viên không thấy lỗi |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:192-205] |

### AC-21 — Trạng thái cuối không bao giờ bị ghi đè
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài nộp vừa được tiến trình chấm kết thúc là Accepted đúng lúc tác vụ quét bài treo cũng chọn bài này |
| **Thao tác** | Cả hai cùng cố ghi kết quả cuối |
| **Kết quả mong đợi** | Bài nộp giữ đúng **một** trạng thái cuối (bên ghi trước thắng); bên ghi sau không ghi đè, không tạo thêm sự kiện. Không bao giờ có chuyển từ trạng thái cuối sang trạng thái khác |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:157-173,226-229] |

### AC-22 — Máy chấm lỗi hạ tầng: lỗi hệ thống, không phải lỗi của học viên
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giữa lúc chấm, máy chấm ngừng trả lời và không phục hồi sau các lần thử lại |
| **Thao tác** | Chờ hệ thống xử lý |
| **Kết quả mong đợi** | Bài nộp kết thúc ở trạng thái lỗi hệ thống (không phải Sai kết quả hay Quá thời gian), không có điểm tỷ lệ; các testcase đã chạy xong giữ nguyên kết quả. Quản trị viên thấy bài này trong danh sách lỗi của màn giám sát và trong sự kiện hạ tầng |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:175] |

### AC-23 — Bài chờ quá 5 phút bị đánh lỗi hệ thống, riêng khi tạm dừng hàng đợi thì không
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài nộp ở trạng thái chờ hoặc đang chấm đã hơn 5 phút không có cập nhật nào (tiến trình chấm đã chết, không ai nhận lại). Một nhóm bài khác đang chờ trong lúc quản trị viên tạm dừng hàng đợi hơn 5 phút |
| **Thao tác** | Chờ chu kỳ quét kế tiếp (tối đa khoảng 1 phút); sau đó quản trị viên tiếp tục hàng đợi |
| **Kết quả mong đợi** | Bài mồ côi được chuyển sang lỗi hệ thống. Các bài chờ trong lúc **đang tạm dừng** **không** bị chuyển sang lỗi; sau khi tiếp tục hàng đợi, chúng có thêm 5 phút kể từ lúc mở lại trước khi bị coi là treo, và được chấm bình thường |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:41-46]; [SoT: 02-bd/architecture/F4_judge-orchestration.md:221-225] |

### AC-24 — Bài chấm dài hợp lệ không bị quét nhầm là treo
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có 45 testcase, mỗi testcase chạy vài giây nên cả bài chấm hơn 5 phút; tiến trình chấm vẫn hoạt động bình thường |
| **Thao tác** | Học viên nộp và chờ; tác vụ quét bài treo chạy nhiều lần trong lúc chấm |
| **Kết quả mong đợi** | Bài nộp **không** bị đưa sang lỗi hệ thống; chấm xong bình thường với đủ 45 kết quả, vì mỗi testcase xong là một lần cập nhật tiến độ |
| **Nguồn** | [SoT: Suy luận] xem `03-dd/logic/F4_judge-orchestration.md` BR-JD-04 bước 10 |

### 2.3. Thời gian thực

### AC-25 — Học viên nhận từng testcase theo thứ tự ngay khi xong
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên vừa nộp bài X (12 testcase) và đang mở màn kết quả |
| **Thao tác** | Chờ chấm |
| **Kết quả mong đợi** | Học viên thấy lần lượt: "Đang biên dịch", "Đang chấm" kèm số ô testcase (12), rồi từng ô được tô màu đạt hoặc không đạt theo đúng thứ tự testcase 1 đến 12 ngay sau khi mỗi testcase xong; cuối cùng là kết quả tổng. Số đếm "đã xong" và "đạt" tăng khớp với các ô. Không có ô nào bị tô sai thứ tự |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:47-49]; [SoT: 03-dd/api/F4_judge-orchestration.md] mục 2.3, 2.4 |

### AC-26 — Chỉ nghe được tiến trình chấm của chính mình
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên A đang có bài nộp đang chấm. Học viên B đã đăng nhập |
| **Thao tác** | B cố đăng ký nghe kênh của A bằng mã bài nộp của A, hoặc đăng ký một kênh không thuộc quyền của B |
| **Kết quả mong đợi** | B không nhận được bất kỳ thông tin nào về bài nộp của A; việc đăng ký bị từ chối im lặng, không xác nhận mã bài nộp có tồn tại hay không. Người chưa đăng nhập không kết nối được |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:474,615]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:837] |

### AC-27 — Nối lại giữa chừng: dựng lại đúng trạng thái và không trùng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp đang chấm, đã xong 4 trên 12 testcase thì kết nối thời gian thực của học viên bị rớt; trong lúc rớt, 3 testcase nữa xong |
| **Thao tác** | Kết nối trở lại |
| **Kết quả mong đợi** | Học viên thấy ngay 7 ô đã tô đúng màu (4 ô đã có trước đó và 3 ô bỏ lỡ), số đếm đúng, rồi tiếp tục nhận các ô còn lại; không ô nào bị tô trùng hay tô sai. Nếu bài đã kết thúc trong lúc rớt, học viên thấy ngay kết quả cuối |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md] mục 2.5 |

### AC-28 — Mất kênh thời gian thực không làm kết luận bài trượt
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Kết nối thời gian thực không nối lại được trong lúc bài đang chấm |
| **Thao tác** | Học viên chờ |
| **Kết quả mong đợi** | Màn hỏi lại trạng thái định kỳ (3 giây một lần, tối đa 10 lần) và vẫn cập nhật được tiến trình; nếu sau đó vẫn chưa xong thì hiện dòng nhắc cập nhật đang chậm. Màn **không** kết luận bài trượt chỉ vì mất kênh; tải lại trang vẫn thấy trạng thái thật |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:472]; [SoT: 02-bd/screens/users/USR0102_problem_detail.md:801] |

### AC-29 — Bản tin thời gian thực không lộ dữ liệu testcase
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X có testcase ẩn; mã học viên sai ở một testcase ẩn |
| **Thao tác** | Học viên quan sát mọi bản tin nhận được trong lúc chấm (kể cả bằng công cụ mạng của trình duyệt) |
| **Kết quả mong đợi** | Mỗi bản tin về một testcase chỉ cho biết thứ tự và đạt hay không đạt; không có dữ liệu vào, kết quả mong đợi, output thực tế hay thông báo lỗi chạy của testcase ẩn |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:828] |

### 2.4. Xem kết quả bài nộp

### AC-30 — Xem chi tiết kết quả của bài nộp Accepted
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có một bài nộp Accepted (12 trên 12) |
| **Thao tác** | Mở trang kết quả của bài nộp |
| **Kết quả mong đợi** | Hiển thị mã nguồn đã nộp, ngôn ngữ, cách nộp, trạng thái, điểm 1 trên 1, thời gian và bộ nhớ lớn nhất, và bảng 12 testcase theo đúng thứ tự (mỗi dòng có đạt hay không, thời gian, bộ nhớ, loại Mẫu hay Ẩn). Phần kết quả này hiển thị đủ kể cả khi dịch vụ phân tích AI đang hỏng |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:274-325] |

### AC-31 — Bài sai ở testcase ẩn: chỉ lộ số thứ tự, không lộ nội dung
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên nộp bài X, sai ở testcase ẩn thứ 7 (testcase thứ 1 đến 3 là mẫu) |
| **Thao tác** | Mở trang kết quả; thử mở rộng chi tiết dòng testcase 7; quan sát toàn bộ dữ liệu máy chủ trả về cho trang |
| **Kết quả mong đợi** | Dòng testcase 7 chỉ có thứ tự 7, kết quả không đạt, thời gian và bộ nhớ. Không có dữ liệu vào, kết quả mong đợi, output thực tế hay phần so sánh khác biệt của testcase này ở bất kỳ chỗ nào trong dữ liệu trả về. Nút mở rộng ở testcase ẩn không dùng được. Với testcase mẫu thứ 2 sai, học viên **được** thấy output thực tế của chương trình mình |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:470]; [SoT: 02-bd/security/F4_judge-orchestration.md:57-59]; [SoT: README.md] F2-08 |

### AC-32 — Chỉ chủ bài nộp xem được kết quả, người khác không biết bài nộp có tồn tại
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp thuộc học viên A. Học viên B đã đăng nhập |
| **Thao tác** | B mở trang kết quả (hoặc gọi trực tiếp dữ liệu) bằng mã bài nộp của A; rồi dùng một mã bài nộp hoàn toàn không tồn tại |
| **Kết quả mong đợi** | Hai trường hợp nhận **cùng một** phản hồi "không tìm thấy bài nộp"; B không đọc được mã nguồn hay kết quả của A và không phân biệt được hai trường hợp |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:469] |

### AC-33 — Bài nộp quá 90 ngày không còn chi tiết từng testcase
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có hai bài nộp: một bài 89 ngày tuổi, một bài 91 ngày tuổi, cả hai Sai kết quả 7 trên 12 |
| **Thao tác** | Mở trang kết quả của từng bài |
| **Kết quả mong đợi** | Bài 89 ngày còn đủ bảng testcase. Bài 91 ngày **không** có bảng testcase và hiện thông báo chi tiết đã hết hạn lưu giữ (không hiện bảng rỗng), nhưng vẫn có trạng thái, điểm 7 trên 12, thời gian, bộ nhớ và mã nguồn |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:68-72] |

### AC-34 — Beats chỉ hiện khi đủ dữ liệu so sánh và khi Accepted
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài X bằng Java có 40 bài nộp Accepted; bài X bằng C++ có 6 bài nộp Accepted; học viên có một bài Accepted bằng mỗi ngôn ngữ và một bài Sai kết quả bằng Java |
| **Thao tác** | Mở trang kết quả của từng bài nộp |
| **Kết quả mong đợi** | Bài Java Accepted hiện phần trăm Beats làm tròn tới số nguyên, tính trên các bài Accepted **cùng bài và cùng ngôn ngữ** (không so với C++). Bài C++ Accepted (chỉ có 6 bài) hiện "Chưa đủ dữ liệu so sánh" thay cho số. Bài Sai kết quả hiện dấu gạch ở chỗ Beats |
| **Nguồn** | [SoT: 01-rd/req/F4_judge-orchestration.md:84-89]; [SoT: 02-bd/screens/users/USR0201_submission_result.md:298,473] |

### AC-35 — Thông báo lỗi chạy chỉ hiện khi quản trị cho phép và chỉ với testcase mẫu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Mã học viên gây lỗi khi chạy ở một testcase mẫu và một testcase ẩn. Cấu hình sandbox đang bật chế độ trả thông báo lỗi chạy cho học viên |
| **Thao tác** | Học viên mở kết quả; sau đó quản trị viên tắt chế độ này và học viên bài nộp mới |
| **Kết quả mong đợi** | Khi bật: học viên thấy đoạn thông báo lỗi chạy của testcase mẫu, **không** thấy của testcase ẩn. Khi tắt: bài nộp mới không có thông báo lỗi chạy nào để hiển thị ở cả hai loại testcase |
| **Nguồn** | [SoT: 02-bd/security/F4_judge-orchestration.md:60-63]; `[SoT: Suy luận]` chỉ trả dữ liệu testcase mẫu theo DD F4 Q6 đã chốt |

### 2.5. Lịch sử và số liệu của học viên

### AC-36 — Tab Bài nộp của một bài hiển thị 10 lượt gần nhất của chính mình
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên A có 12 lượt nộp cho bài X; học viên B có 3 lượt nộp cho bài X |
| **Thao tác** | A mở bài X, tab Bài nộp |
| **Kết quả mong đợi** | A thấy đúng 10 lượt mới nhất của mình theo thứ tự mới đến cũ, mỗi lượt có ngôn ngữ, kết quả, số testcase đạt, thời gian, bộ nhớ và thời điểm nộp; không thấy lượt nào của B. Lượt đang chờ hiện dấu gạch ở số đếm, không hiện 0/0 |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0102_problem_detail.md:502-505] |

### AC-37 — Lịch sử nộp bài có phân trang, tìm và lọc
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có 45 lượt nộp, gồm nhiều ngôn ngữ và kết quả, trên nhiều bài |
| **Thao tác** | Mở lịch sử nộp bài; chuyển trang; lọc Accepted rồi Python; tìm theo một phần tên bài; nhập giá trị lạ vào tham số lọc trên địa chỉ trang |
| **Kết quả mong đợi** | Mỗi trang 20 dòng, trang 3 có 5 dòng, tổng 45; lọc cho đúng các dòng khớp (lọc Accepted không lẫn bài đang chờ hay lỗi hệ thống); tìm theo tên không phân biệt hoa thường; giá trị lọc lạ được coi như "Tất cả" mà không báo lỗi; mở trang vượt quá tổng số trang thì hiện trang cuối. Không có lượt nộp của người khác |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0202_my_submissions.md:243-267,376] |

### AC-38 — Bài đang làm dở là bài có lượt nộp gần nhất chưa Accepted
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có bài A (nộp sai rồi nộp Accepted), bài B (nộp sai), bài C (nộp Accepted rồi nộp lại sai), bài D (nộp sai), bài E (nộp sai) |
| **Thao tác** | Mở danh sách bài; xin danh sách mặc định (3 bài) và danh sách dài hơn |
| **Kết quả mong đợi** | Bài A không có mặt. Bài B, C, D, E có mặt (bài C vì lượt nộp gần nhất không Accepted). Danh sách mặc định chỉ có 3 bài mới nhất theo lượt nộp gần nhất; danh sách dài hơn có đủ 4 bài. Không có bài của người khác |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0101_problem_list.md:647] |

### AC-39 — Số liệu cá nhân cắt ngày theo giờ Việt Nam và luôn đủ cột
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên nộp bài lúc 23:30 giờ Việt Nam hôm qua (tức đầu giờ buổi chiều UTC cùng ngày), hôm nay đã nộp 1 lượt, 3 ngày trước đó không nộp |
| **Thao tác** | Mở trang tiến độ với khoảng 7 ngày, 30 ngày và Tất cả |
| **Kết quả mong đợi** | Lượt 23:30 được tính vào đúng ngày giờ Việt Nam của nó. Chuỗi ngày bằng 2. Khoảng 7 ngày có đúng 7 cột (ngày không nộp là 0); khoảng 30 ngày có đúng 14 cột và tổng bằng số lượt trong 30 ngày, `day` là ngày cuối nhóm; Tất cả gom theo tuần ISO, `day` là thứ Hai đầu tuần theo UTC+7 và có tuần hiện tại. Học viên chưa nộp bài nào thấy chuỗi ngày 0 và toàn cột 0 |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0501_my_progress.md:305,333,593] |

### AC-40 — Khoảng thời gian lạ bị từ chối, không đoán
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Trang tiến độ chỉ có ba khoảng 7 ngày, 30 ngày, Tất cả |
| **Thao tác** | Gọi dữ liệu với khoảng "1 năm" hoặc thêm một mã người dùng khác trên địa chỉ |
| **Kết quả mong đợi** | Khoảng "1 năm" bị từ chối với thông báo khoảng thời gian không hợp lệ. Mã người dùng thừa bị bỏ qua: dữ liệu luôn của chính người đăng nhập |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0501_my_progress.md:574]; DEC-2026-0927-submission-metrics-two-ports |

### 2.6. Giảng viên đọc bài nộp của học viên trong lớp

### AC-41 — Danh sách của giảng viên chỉ gồm bài đã giao cho lớp, trong thời gian học viên thuộc lớp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên G phụ trách lớp L1. Học viên H thuộc L1. Bài P1 đã giao cho L1; bài P2 chưa giao (H tự luyện). H nộp P1 và P2 |
| **Thao tác** | G mở danh sách bài nộp |
| **Kết quả mong đợi** | G thấy lượt nộp P1 của H (kèm tên học viên, tên lớp, tên bài, ngôn ngữ, trạng thái, số testcase đạt, thời điểm nộp) và **không** thấy lượt nộp P2. Danh sách không chứa mã nguồn, thông báo lỗi biên dịch hay dữ liệu testcase. Mới nhất ở trên cùng, 20 dòng mỗi trang |
| **Nguồn** | [SoT: 02-bd/screens/teacher/INS0206_submission_management.md:206-216,545,549] |

### AC-42 — Học viên đã rời lớp: chỉ lượt nộp lúc còn trong lớp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | H rời lớp L1 vào ngày D (lớp ghi nhận ngày rời). H có lượt nộp P1 trước D và lượt nộp P1 sau D. Sau đó H xin vào lại L1 |
| **Thao tác** | G mở danh sách bài nộp trước khi H vào lại, và sau khi H vào lại |
| **Kết quả mong đợi** | Trước khi vào lại: G thấy lượt nộp trước D (dòng mang ghi chú "Đã rời lớp", tên không bấm được sang hồ sơ) và **không** thấy lượt nộp sau D. Sau khi vào lại: các lượt nộp trong khoảng đã rời cũng thuộc phạm vi (chấp nhận khoảng hở này) |
| **Nguồn** | [SoT: 02-bd/screens/teacher/INS0206_submission_management.md:212,544] |

### AC-43 — Học viên ở hai lớp cùng giao một bài: mỗi lượt nộp đúng một dòng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | H thuộc cả L1 và L2 của giảng viên G; cả hai lớp cùng giao bài P1; lần giao cho L2 mới hơn lần giao cho L1 nhưng đều trước lượt nộp |
| **Thao tác** | G mở danh sách (không lọc lớp), rồi lọc lớp L1, rồi lọc lớp L2 |
| **Kết quả mong đợi** | Mỗi lượt nộp P1 của H chỉ xuất hiện **một** lần, gắn với L2 (lớp có lần giao mới nhất không sau thời điểm nộp). Lọc L2 thấy lượt nộp; lọc L1 không thấy |
| **Nguồn** | [SoT: 02-bd/screens/teacher/INS0206_submission_management.md:213,550] |

### AC-44 — Lớp không phải của giảng viên: báo không tìm thấy, không trả danh sách rỗng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Lớp M do giảng viên khác phụ trách. Tài khoản học viên cũng thử gọi |
| **Thao tác** | G gọi danh sách bài nộp lọc theo lớp M (và theo mã lớp sai định dạng); tài khoản học viên gọi danh sách của giảng viên |
| **Kết quả mong đợi** | G nhận thông báo "không tìm thấy lớp" giống như lớp không tồn tại, không phải danh sách rỗng; học viên bị từ chối vì không có quyền |
| **Nguồn** | [SoT: 02-bd/screens/teacher/INS0206_submission_management.md:543,546] |

### AC-45 — Giảng viên xem chi tiết một bài nộp trong phạm vi lớp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | H thuộc lớp L1 của G, đã nộp bài P1 (đã giao). Một bài nộp khác thuộc lớp của giảng viên khác. Một bài nộp của H làm sau ngày H rời lớp |
| **Thao tác** | G mở chi tiết từng bài nộp; một lần mở bằng địa chỉ có cặp học viên và lớp bị sửa thành người khác |
| **Kết quả mong đợi** | Bài nộp trong phạm vi: G thấy mã nguồn, trạng thái, bảng testcase (cùng quy tắc che testcase ẩn như học viên), kèm tên học viên và lớp; **không** có chỉ số Beats. Hai bài nộp ngoài phạm vi: "không tìm thấy bài nộp" giống như bài không tồn tại. Địa chỉ bị sửa không làm trang hiện học viên hay lớp sai: tên luôn lấy từ chính bài nộp. Testcase ẩn vẫn không lộ gì |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:548-560] |

### AC-46 — Giảng viên xem lịch sử nộp của một học viên theo lớp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | H thuộc L1 của G, có 25 lượt nộp bài đã giao cho L1 và vài lượt nộp bài tự luyện. Học viên K không thuộc L1 |
| **Thao tác** | G mở lịch sử nộp của H trong L1; sau đó thử với K và với một lớp của giảng viên khác |
| **Kết quả mong đợi** | G thấy các lượt nộp bài đã giao (20 dòng ở trang đầu, tổng 25), không có bài tự luyện, không có mã nguồn; điểm tỷ lệ hiện dấu gạch với lỗi biên dịch hoặc lỗi hệ thống. Với K: báo học viên không thuộc lớp. Với lớp của người khác: báo không tìm thấy lớp |
| **Nguồn** | [SoT: 02-bd/screens/teacher/INS0204_class_student_detail.md:632-634,666] |

### AC-47 — Số liệu theo tuần của lớp chỉ tính tuần đủ và có thể cũ tối đa 15 phút
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hôm nay là thứ Tư. Lớp L1 có 20 học viên, đã giao 5 bài. Một học viên không nộp bài nào cho 5 bài này |
| **Thao tác** | Hệ thống lấy xu hướng 4 tuần gần nhất của lớp; ngay sau đó học viên có thêm một bài Accepted rồi lấy lại trong vòng 15 phút |
| **Kết quả mong đợi** | Xu hướng gồm đúng 4 tuần **đủ** gần nhất (tuần bắt đầu thứ Hai; tuần đang chạy không tính); tuần không có bài nộp vẫn có mặt với số 0 và tỉ lệ trống; học viên chưa nộp có chuỗi ngày 0 và mốc nộp gần nhất trống. Lần lấy lại trong 15 phút có thể chưa phản ánh bài mới. Kết quả chỉ chứa số đếm và mốc thời gian, không có mã nguồn hay dữ liệu testcase |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:113]; [SoT: 02-bd/screens/teacher/INS0203_class_progress.md:514] |

### 2.7. Quản trị: hàng đợi, sự kiện hạ tầng, cấu hình

### AC-48 — Ảnh chụp hàng đợi: trạng thái và độ trễ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền giám sát hàng đợi. Ngưỡng tự mở rộng là 20. Có 25 bài đang chờ; 3 bài đã bắt đầu chấm trong giờ qua với thời gian chờ 2, 4 và 6 giây |
| **Thao tác** | Mở màn giám sát hàng đợi; sau đó làm lại khi dịch vụ số liệu hạ tầng không trả lời; sau đó làm lại khi hàng đợi đang tạm dừng |
| **Kết quả mong đợi** | Trạng thái hàng đợi là "Đang dồn" kèm 25 job; độ trễ chờ trung bình 4,0 giây. Màn không hiển thị mã, tải hay thời gian hoạt động của từng worker. Khi dịch vụ số liệu hạ tầng không trả lời, trạng thái và độ trễ hàng đợi vẫn hiện theo dữ liệu đã lưu. Khi tạm dừng, trạng thái "Đang tạm dừng" được ưu tiên. Hàng đợi trống và không job nào bắt đầu trong giờ qua: độ trễ hiển thị dấu gạch. Người không có quyền giám sát bị từ chối |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:325,382-391] |

### AC-49 — Danh sách job: thứ tự chờ lâu nhất trước, job lỗi giữ có thời hạn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có job chờ 30 giây, 10 giây; job đang chấm; job lỗi hệ thống kết thúc 2 giờ trước và 25 giờ trước; thời gian giữ job lỗi là 24 giờ; số dòng mỗi trang là 20 |
| **Thao tác** | Xem lần lượt các tab Đang chờ, Lỗi, Tất cả; đổi số dòng mỗi trang thành 5 rồi xem lại |
| **Kết quả mong đợi** | Tab Đang chờ chỉ có job đang chờ, chờ lâu nhất đứng đầu. Tab Lỗi chỉ có job lỗi 2 giờ trước (job 25 giờ trước đã rời khỏi danh sách nhưng bài nộp tương ứng **vẫn còn nguyên** trong lịch sử của học viên). Tab Tất cả xếp job lỗi **cuối**. Mỗi tab kèm số lượng đúng. Sau khi đổi, mỗi trang chỉ 5 dòng ở lần tải kế tiếp |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:335-343] |

### AC-50 — Tạm dừng và tiếp tục tiêu thụ hàng đợi, có ghi nhật ký
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên đang xem màn giám sát; có 1 bài đang chấm và học viên khác sắp nộp bài |
| **Thao tác** | Quản trị viên tạm dừng hàng đợi (sau hộp xác nhận ở giao diện); học viên nộp bài trong lúc tạm dừng; quản trị viên tiếp tục hàng đợi |
| **Kết quả mong đợi** | Bài đang chấm chạy hết. Bài nộp mới được nhận và nằm ở trạng thái chờ, không bị lỗi dù tạm dừng quá 5 phút. Sau khi tiếp tục, bài chờ được chấm theo thứ tự. **Mỗi** lần đổi trạng thái tạo đúng một dòng nhật ký hệ thống có người thực hiện, giá trị cũ và mới; gọi lệnh khi trạng thái đã đúng như đích thì không tạo thêm dòng |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:644,651,675] |

### AC-51 — Hai quản trị viên đổi cùng lúc: người sau được báo, không ghi đè
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hai quản trị viên cùng mở màn giám sát, đều thấy hàng đợi đang chạy |
| **Thao tác** | Quản trị viên 1 tạm dừng; sau đó quản trị viên 2 (màn còn cũ) cũng bấm tạm dừng |
| **Kết quả mong đợi** | Lệnh của quản trị viên 2 bị từ chối với thông báo trạng thái cụm đã bị người khác thay đổi và màn tải lại; hàng đợi vẫn tạm dừng; chỉ có một dòng nhật ký |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:669] |

### AC-52 — Bật và tắt tự động mở rộng worker, có ghi nhật ký
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Tự động mở rộng đang bật |
| **Thao tác** | Quản trị viên tắt tự động mở rộng, rồi đọc lại màn giám sát |
| **Kết quả mong đợi** | Màn thể hiện tự động mở rộng đang tắt và dòng mô tả vẫn nêu ngưỡng hiện hành; một dòng nhật ký có giá trị cũ và mới. Lệnh với trạng thái mong đợi đã cũ bị từ chối như AC-51 |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:645] |

### AC-53 — Tham số hàng đợi: chỉ nhận số nguyên từ 1, không có trần
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở cửa sổ tham số hàng đợi (ngưỡng tự mở rộng, số dòng mỗi trang, thời gian giữ job lỗi, ngưỡng trần thanh độ trễ) |
| **Thao tác** | Lưu lần lượt các giá trị: 0; 1,5; để trống; chữ; 100.000; rồi lưu hợp lệ `5, 7, 48, 20` |
| **Kết quả mong đợi** | 0, 1,5, trống, chữ đều bị từ chối và không đổi gì. 100.000 được nhận (hệ thống không đặt trần nghiệp vụ). Lưu hợp lệ thành công; giá trị mới có tác dụng ở lần tải kế tiếp của bảng job và thanh độ trễ; một dòng nhật ký ghi giá trị cũ và mới của các tham số đã đổi |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:672] |

### AC-54 — Lệnh ghi quản trị bị chặn ở máy chủ với người không phải quản trị viên
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên và học viên đã đăng nhập; giao diện đã ẩn các nút này |
| **Thao tác** | Gọi thẳng các lệnh tạm dừng, đổi tự động mở rộng, đổi tham số hàng đợi và lưu cấu hình ngôn ngữ |
| **Kết quả mong đợi** | Mọi lệnh bị từ chối vì không đủ quyền, không có thay đổi và không có dòng nhật ký nào. Một tài khoản chỉ có quyền xem cấu hình ngôn ngữ (không có quyền sửa) cũng bị từ chối khi lưu |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0401_queue_monitor.md:666,673]; [SoT: 02-bd/screens/admin/ADM0501_language_config.md:491] |

### AC-55 — Danh sách sự kiện hạ tầng: hai nguồn, mức độ theo số lần thử, lọc và phân trang
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có một bản ghi sự kiện đã thử gửi 3 lần chưa được xác nhận, một bản ghi đã thử 5 lần, hai bài nộp kết thúc lỗi hệ thống (một do tiến trình chấm treo, một do máy chấm lỗi), và một sự kiện cũ hơn 90 ngày. Tổng cộng hơn 20 dòng |
| **Thao tác** | Quản trị viên mở màn sự kiện hạ tầng; lọc theo mức, dịch vụ, từ khoá, khoảng ngày (cả khoảng ngược); bấm Tải thêm |
| **Kết quả mong đợi** | Mới nhất trước, 20 dòng mỗi trang, Tải thêm nối tiếp đúng; bản ghi 3 lần là Cảnh báo, 5 lần là Lỗi (dịch vụ nguồn là hàng đợi sự kiện); bài nộp lỗi hệ thống là Lỗi (dịch vụ máy chấm); sự kiện quá 90 ngày không hiện; lọc theo mức Thông tin trả danh sách rỗng không lỗi; từ khoá tìm không phân biệt hoa thường; khoảng ngày có "từ ngày" sau "đến ngày" bị từ chối. Câu mô tả sự kiện không chứa dữ liệu người dùng |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0404_infra_events.md:230-241,353-354,366-369] |

### AC-56 — Thẻ tóm tắt hạ tầng khớp với danh sách sự kiện
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Dữ liệu như AC-55; một bài nộp lỗi hệ thống kết thúc cách đây 25 giờ |
| **Thao tác** | Xem thẻ liên kết sự kiện hạ tầng ở màn giám sát hàng đợi, rồi mở danh sách và đếm các dòng trong 24 giờ gần nhất |
| **Kết quả mong đợi** | Số cảnh báo và số lỗi trên thẻ **bằng** số dòng cùng mức trong 24 giờ gần nhất ở danh sách; sự kiện 25 giờ trước không tính vào thẻ |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0404_infra_events.md:320] |

### AC-57 — Xem cấu hình ngôn ngữ, giới hạn và sandbox trong một lần
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền xem cấu hình ngôn ngữ; có 4 bài Python đang chờ |
| **Thao tác** | Mở màn ngôn ngữ và giới hạn chấm |
| **Kết quả mong đợi** | Hiển thị đủ ba ngôn ngữ (hệ số thời gian, hệ số bộ nhớ, trạng thái bật, số job đang chờ của mỗi ngôn ngữ), bốn giới hạn mặc định, ba công tắc sandbox (chặn mạng mặc định bật chặn, giới hạn tiến trình con, trả thông báo lỗi chạy cho học viên) và hai giới hạn cho script sinh dữ liệu. Không có ô số tiến trình con (đó là hằng số của hệ thống). Người không có quyền xem bị từ chối |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0501_language_config.md:446-452] |

### AC-58 — Lưu cấu hình hợp lệ: có hiệu lực từ lúc bắt đầu chấm, có nhật ký
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hệ số thời gian của Java là 2,00. Một bài Java đang chấm dở. Một bài Java khác đã chờ trong hàng đợi |
| **Thao tác** | Quản trị viên đổi hệ số thời gian Java thành 3,00 và lưu |
| **Kết quả mong đợi** | Lần lưu thành công; một dòng nhật ký hệ thống ghi giá trị cũ và mới (chỉ trường đã đổi). Bài đang chấm dở chạy nốt với hệ số cũ; bài chờ trong hàng đợi và mọi bài nộp sau đó dùng hệ số 3,00 **tính từ lúc tiến trình chấm bắt đầu chấm bài đó**. Không cần khởi động lại dịch vụ. Lưu mà không đổi gì thì không có dòng nhật ký |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:198]; [SoT: 02-bd/screens/admin/ADM0501_language_config.md:470,476] |

### AC-59 — Không cho tắt ngôn ngữ cuối cùng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Chỉ còn Java đang bật |
| **Thao tác** | Quản trị viên tắt Java và lưu (gọi thẳng máy chủ, bỏ qua giao diện) |
| **Kết quả mong đợi** | Bị từ chối với thông báo phải giữ ít nhất một ngôn ngữ đang bật; không có thay đổi và không có dòng nhật ký |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0501_language_config.md:496] |

### AC-60 — Giới hạn hiệu dụng ngoài khoảng cho phép bị từ chối
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giới hạn thời gian mặc định 1.000 ms; hệ số Python 3,00 |
| **Thao tác** | Lưu: hệ số 0,09; hệ số 10,00; hệ số 9,99 với giới hạn mặc định 4.000 ms (cho kết quả 39.960 ms); giới hạn mặc định 1.000 ms với hệ số bộ nhớ làm bộ nhớ hiệu dụng 20 MB; rồi một giá trị hợp lệ sát biên (hệ số 0,10 và 9,99 với mặc định 1.000 ms) |
| **Kết quả mong đợi** | Các trường hợp đầu bị từ chối mà không lưu gì (hệ số ngoài 0,10 đến 9,99, thời gian hiệu dụng ngoài 100 đến 30.000 ms, bộ nhớ hiệu dụng ngoài 64 đến 1.024 MB). Giá trị sát biên hợp lệ được lưu. Giới hạn script sinh dữ liệu ngoài 1 đến 300 giây hoặc 1 đến 1.024 MB cũng bị từ chối |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0501_language_config.md:492-495,499-500] |

### AC-61 — Hai quản trị viên lưu đồng thời: người sau được báo xung đột
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hai quản trị viên cùng mở màn cấu hình |
| **Thao tác** | Quản trị viên 1 lưu thay đổi; quản trị viên 2 (màn cũ) lưu thay đổi khác |
| **Kết quả mong đợi** | Lần lưu của quản trị viên 2 bị từ chối với thông báo cấu hình đã được người khác cập nhật, yêu cầu tải lại; thay đổi của quản trị viên 1 không bị ghi đè |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0501_language_config.md:497] |

### AC-62 — Lưu cấu hình và nhật ký là một khối: nhật ký lỗi thì không lưu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Chức năng ghi nhật ký hệ thống tạm thời không dùng được |
| **Thao tác** | Quản trị viên lưu một thay đổi cấu hình hợp lệ |
| **Kết quả mong đợi** | Lần lưu thất bại và **không** có thay đổi nào được áp dụng; không có trạng thái "đã lưu nhưng thiếu dấu vết". Khi chức năng nhật ký hoạt động lại, thao tác lưu được lần nữa bình thường |
| **Nguồn** | [SoT: 02-bd/architecture/F1_identity.md:92]; [SoT: 03-dd/logic/F1_identity.md:356] |

### 2.8. Sự kiện miền và cổng cho module khác

### AC-63 — Kết thúc chấm phát sự kiện đủ trường, đánh dấu lượt nộp đầu tiên
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên chưa từng nộp bài X. Học viên nộp lần 1 (Sai kết quả), lần 2 (Accepted), một bài khác bị lỗi biên dịch |
| **Thao tác** | Chờ cả ba bài nộp kết thúc |
| **Kết quả mong đợi** | Mỗi bài nộp phát đúng một sự kiện "đã chấm" gồm người nộp, bài, mã bài nộp, ngôn ngữ, kết quả, số testcase đạt và tổng, điểm tỷ lệ, thời điểm nộp (không phải thời điểm chấm); lần nộp 1 của bài X đánh dấu là lượt đầu tiên, lần 2 thì không. Bài lỗi biên dịch có số đạt, tổng và điểm trống. Chỉ lần Accepted phát thêm sự kiện "đã Accepted" kèm người nộp, bài, mã bài nộp, ngôn ngữ. Hệ thống thống kê tiến độ của học viên cập nhật khớp |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:254-261]; [SoT: 03-dd/api/F1_identity.md:251] |

### AC-64 — Hàng đợi sự kiện hỏng không làm mất sự kiện và không chậm việc chấm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hạ tầng truyền sự kiện ngừng hoạt động 10 phút; trong lúc đó 5 bài nộp được chấm xong (có một bài Accepted) |
| **Thao tác** | Hạ tầng truyền sự kiện hoạt động trở lại |
| **Kết quả mong đợi** | Trong lúc ngừng, việc chấm và kết quả hiển thị cho học viên **không bị chậm hay lỗi**. Sau khi hoạt động lại, cả năm sự kiện "đã chấm" và sự kiện "đã Accepted" đều tới nơi nhận (không mất); điều kiện phân tích bài giải cho bài Accepted được mở. Sự kiện đã gửi rồi không gửi lại vô hạn |
| **Nguồn** | [SoT: 02-bd/architecture/F4_judge-orchestration.md:263-275]; [SoT: 03-dd/jobs/F4_judge-orchestration.md:72-98] |

### AC-65 — Nhận trùng sự kiện không làm thống kê tăng đôi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một sự kiện "đã chấm" bị gửi hai lần (xác nhận bị mất sau khi đã gửi) |
| **Thao tác** | Hệ thống thống kê tiến độ nhận cả hai bản |
| **Kết quả mong đợi** | Tổng lượt nộp, số bài Accepted, "đúng ngay lần đầu" và biểu đồ ngày của học viên chỉ tăng **một lần** |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:251]; [SoT: 03-dd/jobs/F4_judge-orchestration.md:96] |

### AC-66 — Cổng đọc cấu hình cho module bài toán
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên đặt giới hạn script sinh dữ liệu là 45 giây và 200 MB |
| **Thao tác** | Chức năng sinh testcase bằng AI của module bài toán hỏi giới hạn; sau đó quản trị viên đổi thành 60 giây |
| **Kết quả mong đợi** | Lần hỏi đầu nhận 45 giây và 200 MB; lần hỏi sau nhận 60 giây ngay, không cần khởi động lại. Module bài toán không đọc trực tiếp dữ liệu của module chấm bài |
| **Nguồn** | [SoT: 02-bd/database/F4_judge-orchestration.md:159] |

### AC-67 — Số liệu nộp bài cho trang tổng quan quản trị
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hệ thống có các bài nộp ở mọi trạng thái, gồm cả đang chờ và lỗi hệ thống |
| **Thao tác** | Trang tổng quan quản trị tải số liệu nộp bài |
| **Kết quả mong đợi** | Năm tỉ lệ kết quả (Accepted, Sai kết quả, Quá thời gian, Lỗi thực thi, Lỗi biên dịch) tính trên các bài đã có kết quả cuối, không tính bài đang chờ hay đang chấm; lỗi hệ thống không có lát riêng. Nếu module chấm bài không trả lời, trang tổng quan vẫn hiển thị các khối còn lại |
| **Nguồn** | [SoT: 02-bd/screens/admin/ADM0101_overview.md:361-363]; [SoT: 03-dd/logic/F1_identity.md:310-321] |

### 2.9. Suy giảm êm khi phân hệ AI hỏng

### AC-68 — Phân hệ AI hỏng không ảnh hưởng nộp bài, chấm bài và xem kết quả
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Phân hệ AI (phân tích bài giải, phỏng vấn giả lập) không hoạt động hoặc hết hạn mức |
| **Thao tác** | Học viên nộp bài, theo dõi chấm, xem kết quả; giảng viên xem danh sách bài nộp; quản trị viên xem hàng đợi |
| **Kết quả mong đợi** | Mọi thao tác trên hoạt động bình thường, không có thông báo lỗi liên quan đến AI; các bài Accepted vẫn ra kết quả và vẫn phát sự kiện "đã Accepted" (việc hàng đợi nhận sự kiện không chặn chấm). Chạy phân tích hay phỏng vấn là hành động riêng của học viên ở màn khác |
| **Nguồn** | [SoT: CLAUDE.md] mục Rules "The AI subsystem degrades gracefully"; [SoT: 02-bd/screens/teacher/INS0206_submission_management.md:555] |

### 2.10. Danh sách bài Accepted cho phỏng vấn giả lập (thêm 2026-10-23)

### AC-69 — Tab Bài nộp Accepted chỉ liệt kê bài Accepted của chính mình
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên A có 12 lượt nộp, 5 lượt Accepted (trên 3 bài), 7 lượt còn lại sai hoặc lỗi; học viên B có 4 lượt Accepted |
| **Thao tác** | A mở Phỏng vấn giả lập, chọn tab Bài nộp Accepted |
| **Kết quả mong đợi** | A thấy đúng 5 lượt Accepted của mình, mới đến cũ, mỗi dòng có tên bài và ngôn ngữ; không thấy lượt sai, lỗi hay đang chờ, không thấy lượt nào của B. Học viên chưa có lượt Accepted nào thấy danh sách rỗng, không báo lỗi. Danh sách không chứa mã nguồn hay dữ liệu testcase. Tham số số dòng ngoài khoảng cho phép bị từ chối |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0302_mock_interview.md:204,389,399]; [SoT: 03-dd/api/F4_judge-orchestration.md mục 24A] |

### AC-70 — Phân hệ AI đọc được bài Accepted qua cổng, không đọc được bài chưa Accepted
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài nộp Accepted và một bài nộp Wrong Answer của học viên A |
| **Thao tác** | Phân hệ AI xin ảnh chụp của từng bài để tạo báo cáo phân tích hoặc phiên phỏng vấn |
| **Kết quả mong đợi** | Bài Accepted trả mã nguồn, ngôn ngữ, bài toán, thời gian, bộ nhớ và chủ bài nộp; bài Wrong Answer bị từ chối là chưa Accepted; mã bài nộp không tồn tại bị báo không tìm thấy. Kết quả không chứa dữ liệu testcase ẩn |
| **Nguồn** | [SoT: 02-bd/architecture/F5_ai-review.md:71-82]; [SoT: 03-dd/api/F4_judge-orchestration.md mục 26.4] |

### AC-71 — Bài toán đã rút khỏi ngân hàng vẫn xem được kết quả nộp và được báo rõ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có bài nộp của bài X; sau đó bài X bị ẩn mềm (rút khỏi ngân hàng). Một bài nộp khác của bài Y còn xuất bản |
| **Thao tác** | Mở trang kết quả của từng bài nộp; lặp lại khi ngân hàng bài tạm lỗi |
| **Kết quả mong đợi** | Bài nộp của X mở được, đủ kết quả và mã nguồn, và trang báo bài đã rút khỏi ngân hàng (phân biệt được với trường hợp ngân hàng bài lỗi). Bài nộp của Y không có báo này. Khi ngân hàng bài lỗi: phần kết quả vẫn đủ, không có báo "đã rút" và không có tên bài |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:34]; [SoT: 03-dd/api/F4_judge-orchestration.md mục 8.3] |

### AC-72 — Danh sách ngôn ngữ nộp bài đang bật cho người dùng đã đăng nhập
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | PYTHON, JAVA, CPP được cấu hình; tắt JAVA bằng #22, giữ hai ngôn ngữ còn lại bật |
| **Thao tác** | STUDENT và INSTRUCTOR gọi #24; gọi lại không có JWT; thử dùng quyền đọc này để gọi #21 |
| **Kết quả mong đợi** | #24 trả chỉ PYTHON, CPP theo thứ tự cố định, không lộ giới hạn hay cấu hình; không có JWT nhận 401; quyền đọc #24 không cấp quyền quản trị #21 |
| **Nguồn** | `DEC-2026-1024-dd-student-language-contracts`; `03-dd/api/F4_judge-orchestration.md` mục 24B |

### AC-73 — Một tài khoản được chạy thử tối đa 60 lượt trong 60 phút trượt
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một tài khoản đã chạy thử thành công 60 lần trong 60 phút gần nhất; tài khoản khác còn lượt |
| **Thao tác** | Tài khoản đầu chạy thử lần thứ 61, sau đó thử lại khi lượt cũ nhất đã qua 60 phút; tài khoản kia chạy thử trong lúc đó |
| **Kết quả mong đợi** | Lần thứ 61 bị từ chối và được báo thời gian có thể thử lại. Khi lượt cũ nhất hết hạn, tài khoản đầu chạy được; tài khoản kia không bị ảnh hưởng. Lượt bị từ chối do hệ thống bận hoặc máy chấm lỗi không tiêu tốn hạn mức |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md] mục 3; quyết định chủ dự án 2026-10-26 |

### AC-74 — Đặc tả hoặc bộ testcase hỏng được báo là lỗi hệ thống
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài nộp đã được nhận, nhưng bản đặc tả hoặc bộ testcase đã ghim của bài bị thiếu hoặc hỏng |
| **Thao tác** | Hệ thống bắt đầu chấm bài đó |
| **Kết quả mong đợi** | Bài kết thúc bằng lỗi hệ thống, không bị ghi là bài làm sai hoặc lỗi biên dịch của học viên. Điểm để trống; thông tin vận hành phân biệt được lỗi dữ liệu bài với lỗi máy chấm, không lộ nội dung testcase cho học viên |
| **Nguồn** | [SoT: 03-dd/logic/F4_judge-orchestration.md] BR-JD-03, BR-JD-05, BR-JD-07 |

### AC-75 — Bài nộp chấm theo đúng bộ testcase tại thời điểm nhận bài
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A đã được nhận và đang chờ. Người soạn sửa một testcase trước khi máy chấm bắt đầu xử lý A |
| **Thao tác** | Máy chấm xử lý A; sau đó học viên gửi bài nộp B |
| **Kết quả mong đợi** | A được chấm bằng nội dung testcase cũ tại lúc nộp; B dùng nội dung mới. Việc sửa testcase không làm thay đổi kết quả của A đang chờ |
| **Nguồn** | [SoT: 03-dd/logic/F4_judge-orchestration.md] BR-JD-01, BR-JD-03 |

### AC-76 — Chế độ Bọc hàm không lộ thông báo lỗi nội bộ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài dùng chế độ Bọc hàm và chương trình chạy lỗi, có thông báo nội bộ chứa dấu vết mã khung |
| **Thao tác** | Học viên xem kết quả chạy thử và kết quả bài nộp |
| **Kết quả mong đợi** | Học viên thấy loại lỗi và dữ liệu được phép xem của testcase mẫu, nhưng không thấy thông báo lỗi nội bộ hoặc dấu vết mã khung. Testcase ẩn không lộ dữ liệu vào, kết quả mong đợi hay đầu ra |
| **Nguồn** | [SoT: 03-dd/logic/F4_judge-orchestration.md] BR-JD-04 bước 8; quyết định chủ dự án 2026-10-25 |

### AC-77 — Bật tự động mở rộng làm thay đổi khả năng nhận bài của worker
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên bật tự động mở rộng, `autoscaleJobThreshold = 20`, một consumer đang nhận bài; quan sát được `pendingCount` và số consumer mục tiêu/thực đang chạy |
| **Thao tác** | Cho `pendingCount = 20`, rồi 21 qua các lượt poll 15 giây; tiếp tục giữ backlog để chạm trần 8; đưa `pendingCount` về 0 khi có và khi không có bài đang xử lý; lặp với queue tạm dừng, tự mở rộng tắt và hai tiến trình worker cùng khởi động |
| **Kết quả mong đợi** | Bằng ngưỡng 20 không tăng. Vượt ngưỡng ở hai lượt liên tiếp mới tăng mục tiêu từ 1 lên 2; mỗi chu kỳ tiếp theo tăng tối đa 1 và không vượt 8. Rỗng bốn lượt liên tiếp (60 giây) mới giảm từng bước, chỉ dừng consumer sau khi bài đang xử lý xong; backlog ở giữa hai điều kiện giữ nguyên mục tiêu. Tạm dừng không nhận bài mới và không tự giảm mục tiêu; tiếp tục thì nhận bài trở lại. Tắt tự mở rộng đưa mục tiêu về 1 bằng cơ chế chờ bài đang chạy xong. Trong hai tiến trình chỉ một giữ khoá leader và điều khiển consumer; mất khoá thì dừng nhận bài mới |
| **Nguồn** | [SoT: 03-dd/jobs/F4_judge-orchestration.md] JOB-JD-05 |

## 3. Truy vết

**Bảng 1: Truy vết tiêu chí tới endpoint hoặc luồng (cột "Test case" điền khi có)**

| AC | Endpoint hoặc luồng | Mã lỗi kiểm | Test case | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| AC-01 | `POST /problems/{problemId}/submissions` (#3) | | [TC-F4-AC01-01](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-02 | #3 | `JDG-106` | [TC-F4-AC02-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-03 | #3 | `JDG-106` | [TC-F4-AC03-01](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-04 | #2, #3 | `JDG-V01`, `JDG-V02` | [TC-F4-AC04-01 đến -08](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-05 | #3, #2, luồng chấm job đang chờ (BR-JD-09) | `JDG-102` | [TC-F4-AC05-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-06 | #2, #3 | `JDG-103` | [TC-F4-AC06-01 đến -04](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-07 | #3 | `JDG-V06` | [TC-F4-AC07-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-08 | #6, JOB-JD-01 | `JDG-503` | [TC-F4-AC08-01 đến -02](../testcases/modules/F4_judge-orchestration/F4_submission_resilience.md) | chưa |
| AC-09 | #3 | `JDG-106` | [TC-F4-AC09-01](../testcases/modules/F4_judge-orchestration/F4_submission_input_limits.md) | chưa |
| AC-10 | `POST /problems/{problemId}/run-sample` (#2) | | [TC-F4-AC10-01 (ba Sample), TC-F4-AC73-08 (Redis lỗi)](../testcases/modules/F4_judge-orchestration/F4_run_code_limits.md) | chưa |
| AC-11 | #2 (ánh xạ lỗi biên dịch của `harness`) | | [TC-F4-AC11-01](../testcases/modules/F4_judge-orchestration/F4_run_compile_limits.md) | chưa |
| AC-12 | #2 | `JDG-501` | [TC-F4-AC12-01 (FE/timeout), TC-F4-AC73-06 (API)](../testcases/modules/F4_judge-orchestration/F4_run_code_limits.md) | chưa |
| AC-13 | #2 | `JDG-502` | [TC-F4-AC73-05](../testcases/modules/F4_judge-orchestration/F4_run_code_limits.md) | chưa |
| AC-14 | `GET /problems/{problemId}/execution-limits` (#1) | `JDG-101` | [TC-F4-AC14-01–03](../testcases/modules/F4_judge-orchestration/F4_run_compile_limits.md) | chưa |
| AC-15 | Luồng chấm (BR-JD-03, BR-JD-04) | | [TC-F4-AC15-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-16 | Luồng chấm (BR-JD-05) | | [TC-F4-AC16-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-17 | Luồng chấm (BR-JD-05 bước 2, 3) | | [TC-F4-AC17-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-18 | Luồng chấm (BR-JD-05 bước 2) | | [TC-F4-AC18-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-19 | Luồng chấm (BR-JD-03 bước 8, BR-JD-05 biến thể) | | [TC-F4-AC19-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-20 | Luồng chấm khi giao lại (BR-JD-03, BR-JD-04 bước 2) | | [TC-F4-AC20-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-21 | Luồng kết thúc (BR-JD-05 bước 6), JOB-JD-01 | | [TC-F4-AC21-01](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-22 | BR-JD-07, #17, #22 | | [TC-F4-AC22-01 đến -02](../testcases/modules/F4_judge-orchestration/F4_submission_resilience.md) | chưa |
| AC-23 | JOB-JD-01 và BR-JD-08 | | [TC-F4-AC23-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_submission_resilience.md) | chưa |
| AC-24 | BR-JD-04 bước 10, JOB-JD-01 | | [TC-F4-AC24-01](../testcases/modules/F4_judge-orchestration/F4_submission_resilience.md) | chưa |
| AC-25 | Kênh `/user/queue/submissions` (W1), #9 | | [TC-F4-AC25-01](../testcases/modules/F4_judge-orchestration/F4_submission_realtime.md) | chưa |
| AC-26 | W1 | | [TC-F4-AC26-01 đến -04](../testcases/modules/F4_judge-orchestration/F4_submission_realtime.md) | chưa |
| AC-27 | W1 và `GET /submissions/{submissionId}/progress` (#9) | | [TC-F4-AC27-01 đến -02](../testcases/modules/F4_judge-orchestration/F4_submission_realtime.md) | chưa |
| AC-28 | #9 (đường dự phòng) | | [TC-F4-AC28-01](../testcases/modules/F4_judge-orchestration/F4_submission_realtime.md) | chưa |
| AC-29 | W1, #9 | | [TC-F4-AC29-01](../testcases/modules/F4_judge-orchestration/F4_submission_realtime.md) | chưa |
| AC-30 | `GET /submissions/{submissionId}` (#5) | | [TC-F4-AC30-01–03](../testcases/modules/F4_judge-orchestration/F4_submission_detail_beats_stderr.md) | chưa |
| AC-31 | #8, #9, W1 | | [TC-F4-AC31-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_result_retention_visibility.md) | chưa |
| AC-32 | #8, #9 | `JDG-110` | [TC-F4-AC32-01](../testcases/modules/F4_judge-orchestration/F4_result_retention_visibility.md) | chưa |
| AC-33 | #8, #9, JOB-JD-04 | | [TC-F4-AC33-01 đến -04](../testcases/modules/F4_judge-orchestration/F4_result_retention_visibility.md) | chưa |
| AC-34 | #5 (BR-JD-12) | | [TC-F4-AC34-01–04](../testcases/modules/F4_judge-orchestration/F4_submission_detail_beats_stderr.md) | chưa |
| AC-35 | #5, #2 | | [TC-F4-AC35-01–06](../testcases/modules/F4_judge-orchestration/F4_submission_detail_beats_stderr.md) | chưa |
| AC-36 | `GET /problems/{problemId}/submissions/mine` (#4) | `JDG-V14` | [TC-F4-AC36-01–03](../testcases/modules/F4_judge-orchestration/F4_student_history_metrics.md) | chưa |
| AC-37 | `GET /me/submissions` (#7) | | [TC-F4-AC37-01–03](../testcases/modules/F4_judge-orchestration/F4_student_history_metrics.md) | chưa |
| AC-38 | `GET /me/in-progress-problems` (#8) | `JDG-V14` | [TC-F4-AC38-01–02](../testcases/modules/F4_judge-orchestration/F4_student_history_metrics.md) | chưa |
| AC-39 | `GET /me/metrics` (#9), cổng `GetMySubmissionMetrics` | | [TC-F4-AC39-01–03](../testcases/modules/F4_judge-orchestration/F4_student_history_metrics.md) | chưa |
| AC-40 | #9 | `JDG-V07` | [TC-F4-AC40-01–02](../testcases/modules/F4_judge-orchestration/F4_student_history_metrics.md) | chưa |
| AC-41 | `GET /instructor/submissions` (#10) | | [TC-F4-AC41-01–03](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-42 | #10 (BR-JD-10 điều kiện c) | | [TC-F4-AC42-01–02](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-43 | #10 (BR-JD-10 bước 4) | | [TC-F4-AC43-01](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-44 | #10 | `JDG-111`, `JDG-301` | [TC-F4-AC44-01–02](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-45 | `GET /instructor/submissions/{submissionId}` (#11) | `JDG-110` | [TC-F4-AC45-01–03](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-46 | `GET /instructor/classes/{classId}/students/{studentId}/submissions` (#12) | `JDG-111`, `JDG-112` | [TC-F4-AC46-01–02](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-47 | Cổng `GetClassStudentSubmissionMetrics` (P1) | | [TC-F4-AC47-01–02](../testcases/modules/F4_judge-orchestration/F4_instructor_scope_metrics.md) | chưa |
| AC-48 | `GET /admin/queue/snapshot` (#16) | `JDG-301` | [TC-F4-AC48-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | chưa |
| AC-49 | `GET /admin/queue/jobs` (#17) | | [TC-F4-AC49-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_queue_control.md) | chưa |
| AC-50 | `PUT /admin/queue/consumption` (#19) | | [TC-F4-AC50-01 đến -05](../testcases/modules/F4_judge-orchestration/F4_queue_control.md) | chưa |
| AC-51 | #16 | `JDG-201` | [TC-F4-AC51-01](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-52 | `PUT /admin/queue/autoscale` (#17) | `JDG-201` | [TC-F4-AC52-01–02](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-53 | `GET/PUT /admin/queue/settings` (#18, #19) | `JDG-V09` | [TC-F4-AC53-01–02](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-54 | #16, #17, #19, #22 | `JDG-301` | [TC-F4-AC54-01](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-55 | `GET /admin/infra-events` (#20) | `JDG-V13` | [TC-F4-AC55-01–03](../testcases/modules/F4_judge-orchestration/F4_infra_events.md) | chưa |
| AC-56 | `GET /admin/queue/infra-summary` (#15) và #20 | | [TC-F4-AC56-01](../testcases/modules/F4_judge-orchestration/F4_infra_events.md) | chưa |
| AC-57 | `GET /admin/language-config` (#21) | `JDG-301` | [TC-F4-AC57-01–02](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-58 | `PUT /admin/language-config` (#22), BR-JD-09 | | [TC-F4-AC58-01–02](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-59 | #22 | `JDG-203` | [TC-F4-AC59-01](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-60 | #22 | `JDG-V10`, `JDG-V12`, `JDG-204`, `JDG-205` | [TC-F4-AC60-01–02](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-61 | #22 | `JDG-202` | [TC-F4-AC61-01](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-62 | #16, #17, #19, #22 (cùng giao dịch với `AuditLogPort`) | | [TC-F4-AC62-01](../testcases/modules/F4_judge-orchestration/F4_admin_configuration.md) | chưa |
| AC-63 | Sự kiện `SubmissionGraded`, `SubmissionAccepted` (mục 25) | | [TC-F4-AC63-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_outbox_events.md) | chưa |
| AC-64 | JOB-JD-02, BR-JD-14 | | [TC-F4-AC64-01 đến -04](../testcases/modules/F4_judge-orchestration/F4_outbox_events.md) | chưa |
| AC-65 | Hợp đồng tiêu thụ (mục 25.4) | | [TC-F4-AC65-01](../testcases/modules/F4_judge-orchestration/F4_outbox_events.md) | chưa |
| AC-66 | Cổng `JudgeConfigReadPort` (P3) | | [TC-F4-AC66-01–02](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-67 | Cổng `GetAdminSubmissionStats` (P2) | | [TC-F4-AC67-01](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-68 | Toàn module (BR-JD-14, BR-JD-05) | | [TC-F4-AC68-01](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-69 | `GET /me/accepted-submissions` (#23) | `JDG-V14` | [TC-F4-AC69-01–02](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-70 | Cổng `SubmissionSnapshotPort.getAccepted` (P4) | | [TC-F4-AC70-01–02](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-71 | `GET /submissions/{submissionId}` (#5), trường `problem.withdrawn` | | [TC-F4-AC71-01](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-72 | `GET /languages/enabled` (#24) | | [TC-F4-AC72-01–02](../testcases/modules/F4_judge-orchestration/F4_cross_module_reads.md) | chưa |
| AC-73 | `POST /problems/{problemId}/run-sample` (#2) | `JDG-107` | [TC-F4-AC73-01 đến -08](../testcases/modules/F4_judge-orchestration/F4_run_code_limits.md) | chưa |
| AC-74 | BR-JD-03, BR-JD-05, BR-JD-07 | `SPEC_UNAVAILABLE` | [TC-F2-AC93-04, TC-F2-AC93-05, TC-F4-AC74-01](../testcases/modules/F2_problem-bank/F2_snapshot_grading.md) | chưa |
| AC-75 | BR-JD-01, BR-JD-03 | | [TC-F2-AC93-01, TC-F2-AC93-02, TC-F2-AC93-03](../testcases/modules/F2_problem-bank/F2_snapshot_grading.md) | chưa |
| AC-76 | #5, #8, BR-JD-04/11 | | [TC-F4-AC76-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_verdict_privacy_queue.md), [TC-F4-AC31-01 đến -03](../testcases/modules/F4_judge-orchestration/F4_result_retention_visibility.md) | chưa |
| AC-77 | #20, JOB-JD-05 | | [TC-F4-AC77-01 đến -10](../testcases/modules/F4_judge-orchestration/F4_worker_autoscale.md) | chưa |

Mỗi endpoint trong `03-dd/api/F4_judge-orchestration.md` (24 REST, 2 kênh STOMP, 4 cổng trong tiến trình, 2 sự kiện) có ít nhất một tiêu chí: #1 AC-14; #2 AC-10 đến AC-13; #3 AC-01 đến AC-09; #4 AC-36; #5 AC-30 đến AC-35, AC-71; #6 AC-27, AC-28; #7 AC-37; #8 AC-38; #9 AC-39, AC-40; #10 AC-41 đến AC-44; #11 AC-45; #12 AC-46; #13 AC-48; #14 AC-49; #15 AC-56; #16 AC-50, AC-51; #17 AC-52; #18, #19 AC-53; #20 AC-55; #21 AC-57; #22 AC-58 đến AC-62; W1 AC-25 đến AC-29; W2 (kênh quản trị) AC-48; P1 AC-47; P2 AC-67; P3 AC-66; P4 AC-70; #23 AC-69; #24 AC-72; hai sự kiện AC-63 đến AC-65.

## 4. Chưa chốt

Không còn quyết định chủ dự án đang chờ. Các câu Q4–Q7, Q14–Q16, Q19 và Q21–Q22 đã đóng trong DD F4; AC-73–AC-77 bổ sung các hành vi mới. Các giới hạn kiểm thử hạ tầng được liệt kê dưới đây.

**Chưa phủ được** (những gì bộ tiêu chí này cố ý không kiểm và lý do):

- **Hành vi thật của go-judge** (giới hạn cgroup, chặn mạng, dọn sandbox): cần chạy trên máy chấm thật ở giai đoạn dựng cùng `harness` (`.nexa/control/dependency-map.md` mục 4); tiêu chí ở đây chỉ kiểm hành vi quan sát được ở tầng hệ thống.
- **Số liệu worker** (mã worker, tải, uptime): phụ thuộc việc go-judge có xuất số liệu cho hệ thống đo đạc hay không (`ADM0401` Q2); AC-48 chỉ kiểm đường suy giảm khi không có.
- **Hiệu năng** (độ trễ truy vấn Beats, cửa sổ 364 ngày, thống kê lớp với lớp lớn): chưa có số đo; thuộc kiểm thử tải sau này.
- **Nhiều instance backend** cho kênh thời gian thực: bản triển khai mặc định một instance.
- **Giao diện** (xác nhận trước khi tạm dừng hoặc tắt ngôn ngữ, bố cục, toast): thuộc `04-tdd/<màn>.md`; ở đây chỉ kiểm phần máy chủ (ví dụ nhật ký được ghi, lệnh bị chặn khi gọi thẳng).
