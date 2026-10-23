# TDD — Tiêu chí nghiệm thu module `harness` (F3)

> Bounded Context: F3 `harness` · DD: `03-dd/api/harness.md` (hợp đồng cổng), `03-dd/logic/harness.md`,
> `03-dd/validation/harness.md` · Cập nhật: 2026-10-23
>
> Mỗi `AC-nn` map về một phương thức cổng (`03-dd/api/harness.md` mục 1.1, ký hiệu P1 đến P5, O1, E1) hoặc một khối
> quy tắc (`BR-nn` ở `03-dd/logic/harness.md`). Viết cùng lúc với DD theo `.claude/rules/tdd-mode.md`. Mã lỗi `HRN-...`
> chỉ xuất hiện ở dòng **Nguồn** để truy vết; phần thân mô tả điều người dùng quan sát được.

## 1. Phạm vi

Module `harness` là thư viện tính toán chạy trong cùng tiến trình, không có bảng dữ liệu và không có giao diện riêng ngoài
một thao tác lấy mã khung. Bộ tiêu chí này nghiệm thu **hợp đồng** của nó: sinh mã khung, ghép mã người học và đóng gói,
dịch lỗi biên dịch về dòng của người học, so khớp kết quả, và kiểm đặc tả hàm khi A2 lưu bài.

Ngoài phạm vi (thuộc module khác): gửi gói sang engine chấm, hàng đợi, trạng thái nộp bài, giới hạn thời gian và bộ nhớ
(`judge-orchestration`); lưu đặc tả và testcase, hiển thị cảnh báo ở màn soạn bài (`problem-bank`, màn `problem_authoring`);
giao diện chọn mô hình ở màn `problem_detail` (tiêu chí màn nằm ở file màn, không lặp ở đây).

Ba ngôn ngữ nộp bài là đúng Java, C++, Python (`CLAUDE.md`, mục Locked stack). Mọi tiêu chí nói "ba ngôn ngữ" nghĩa là cả ba.

## 2. Danh sách tiêu chí

### Mã khung (`GetStarterCode`)

### AC-01 — Mã khung bọc hàm đúng chữ ký và đúng cách đặt tên của từng ngôn ngữ
- **Tiền điều kiện**: bài `twoSum` có chữ ký chung: tên hàm `two_sum`, trả về mảng một chiều số nguyên, hai tham số `nums` (mảng số nguyên) và `target` (số nguyên); bài hỗ trợ mô hình Bọc hàm.
- **Hành động**: xin mã khung bọc hàm cho từng ngôn ngữ.
- **Kết quả mong đợi**: Python có hàm `two_sum`; Java và C++ có hàm `twoSum`; mỗi bản có đúng một hàm, đủ hai tham số theo thứ tự, kiểu đúng ngôn ngữ; không có mã đọc input hay in output (phần đó bị ẩn); mã khung biên dịch được ngay khi chưa sửa gì. Nếu chữ ký dùng danh sách liên kết hoặc cây nhị phân, mã khung có chú thích mô tả các trường của node.
- **Nguồn**: `[SoT: 01-rd/req/user_stories/a1_student.md:50]`; `[SoT: 02-bd/screens/users/USR0102_problem_detail.md:697,745]`; `03-dd/logic/harness.md` mục 4.6; P1.

### AC-02 — Ghi đè tên hàm chỉ đổi đúng ngôn ngữ được ghi đè
- **Tiền điều kiện**: bài có ghi đè tên hàm cho Java là `solve`.
- **Hành động**: xin mã khung bọc hàm cho ba ngôn ngữ.
- **Kết quả mong đợi**: Java có hàm `solve`; Python và C++ vẫn dùng tên suy ra từ tên chung.
- **Nguồn**: `[SoT: 02-bd/database/problem-bank.md:135]`; `[SoT: .nexa/control/decision-registry.md:2666]`; `03-dd/logic/harness.md` mục 2; P1.

### AC-03 — Mã khung Standard I/O luôn có, không phụ thuộc chữ ký hàm
- **Tiền điều kiện**: một bài hỗ trợ cả hai mô hình, và một bài khác chỉ hỗ trợ Standard I/O.
- **Hành động**: xin mã khung Standard I/O cho ba ngôn ngữ ở cả hai bài.
- **Kết quả mong đợi**: cả hai bài đều trả về một chương trình tối thiểu có hàm `main`, biên dịch được, không chứa mã đọc hay in theo kiểu dữ liệu của bài.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:30-34]`; `[SoT: 02-bd/architecture/harness.md:223]`; `03-dd/logic/harness.md` mục 4.5; P1.

### AC-04 — Máy chủ từ chối mô hình Bọc hàm cho bài chỉ hỗ trợ Standard I/O, dù giao diện đã ẩn nút
- **Tiền điều kiện**: bài có kiểu dữ liệu ngoài lược đồ nên được đánh dấu chỉ hỗ trợ Standard I/O.
- **Hành động**: gọi thẳng yêu cầu mã khung mô hình Bọc hàm (như khi người dùng sửa tay tham số trên đường dẫn).
- **Kết quả mong đợi**: bị từ chối với thông báo "Bài toán này chỉ hỗ trợ mô hình Standard I/O."; không có mã nào được trả về. Yêu cầu Standard I/O cho cùng bài vẫn thành công.
- **Nguồn**: `[SoT: 02-bd/screens/users/USR0102_problem_detail.md:834]`; `HRN-004`; P1.

### AC-05 — Bài không có đặc tả hoặc chưa khai chữ ký không cho ra mã khung rỗng
- **Tiền điều kiện**: (a) mã bài không tồn tại; (b) bài tồn tại nhưng chưa khai chữ ký hàm.
- **Hành động**: xin mã khung bọc hàm cho từng trường hợp.
- **Kết quả mong đợi**: (a) báo không tìm thấy; (b) báo bài chưa khai chữ ký; cả hai **không** trả về một đoạn mã trống hoặc mã khung đoán.
- **Nguồn**: `HRN-001`, `HRN-005`; P1.

### Ghép mã và đóng gói (`prepare`)

### AC-06 — Mã người học nằm đúng vị trí đã biết, bảng ánh xạ dòng khớp
- **Tiền điều kiện**: bài có chữ ký hàm; mã người học dài 7 dòng.
- **Hành động**: chuẩn bị gói chấm bọc hàm cho từng ngôn ngữ.
- **Kết quả mong đợi**: trong tệp mã nguồn đã ghép, dòng đầu của mã người học nằm ngay sau phần khung đầu; dòng cuối của mã người học là dòng thứ 7 của vùng đó; dòng liền sau là dòng đầu của phần khung cuối. Bảng ánh xạ dòng ghi đúng số dòng khung đầu và số dòng mã người học (7), đúng với tệp thật.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:263-267]`; `03-dd/logic/harness.md` mục 4.3; P2.

### AC-07 — Cùng đầu vào cho cùng gói, từng byte
- **Tiền điều kiện**: một bài, một ngôn ngữ, một mã người học, một phiên bản đặc tả.
- **Hành động**: chuẩn bị gói chấm hai lần.
- **Kết quả mong đợi**: hai gói giống hệt nhau từng byte (tệp mã nguồn, lệnh, bảng ánh xạ, thông số so khớp).
- **Nguồn**: `03-dd/api/harness.md` mục 2.1; P2.

### AC-08 — Mã người học phá cấu trúc không làm đổi vị trí chèn
- **Tiền điều kiện**: ba mã người học cùng số dòng: (a) vô hại; (b) có một dòng chứa nguyên văn chuỗi neo của khung (`@@APH_USER_CODE@@`) và một dòng chứa `{{FUNCTION_NAME}}`; (c) có một dấu đóng khối thừa.
- **Hành động**: chuẩn bị gói chấm cho từng mã ở ba ngôn ngữ.
- **Kết quả mong đợi**: số dòng khung đầu và vị trí các dòng khung cuối giống hệt giữa ba trường hợp; phần khung không bị nội suy bởi nội dung người học; ở (c) kết quả tối đa là lỗi biên dịch ở bước sau, không phải thay đổi logic của khung.
- **Nguồn**: `[SoT: 02-bd/security/harness.md:12-32]`; `03-dd/logic/harness.md` mục 4.1; P2.

### AC-09 — Chuẩn hoá xuống dòng và BOM không làm lệch số dòng
- **Tiền điều kiện**: mã người học (a) dùng xuống dòng kiểu Windows; (b) không kết thúc bằng xuống dòng; (c) có ký tự đánh dấu thứ tự byte (BOM) ở đầu.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: số dòng mã người học trong bảng ánh xạ đúng số dòng nhìn thấy; phần khung cuối luôn bắt đầu ở dòng mới, không dính vào dòng cuối của người học.
- **Nguồn**: `03-dd/logic/harness.md` mục 4.2; P2.

### AC-10 — Mã nguồn không hợp lệ bị từ chối trước khi sinh
- **Tiền điều kiện**: ba mã: rỗng (chỉ khoảng trắng), 65 537 byte, chứa ký tự NUL.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: mỗi mã bị từ chối với lý do riêng; không gói nào được tạo. Mã đúng 65 536 byte được chấp nhận.
- **Nguồn**: `[SoT: 02-bd/security/harness.md:53-57]`; `HRN-020`, `HRN-021`, `HRN-022`; P2.

### AC-11 — Đặc tả đổi giữa chừng được báo rõ ràng, không chấm nhầm âm thầm
- **Tiền điều kiện**: lượt nộp ghim phiên bản đặc tả 3; A2 vừa lưu lại đặc tả, nay là phiên bản 4.
- **Hành động**: chuẩn bị gói chấm với phiên bản mong đợi 3.
- **Kết quả mong đợi**: bị từ chối với lý do phiên bản đặc tả đã đổi; không có gói nào sinh theo phiên bản 4 bị gán nhầm là phiên bản 3.
- **Nguồn**: `[SoT: 02-bd/database/problem-bank.md:136]`; `HRN-002`; P2. Chính sách xử lý ở bên chấm bài ghi ở mục 4.

### AC-12 — Standard I/O không có khung quanh mã người học
- **Tiền điều kiện**: bài bất kỳ, mô hình Standard I/O, mã người học 10 dòng.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: tệp mã nguồn trong gói là đúng mã người học (đã chuẩn hoá xuống dòng), không thêm dòng nào ở đầu hay cuối; bảng ánh xạ ghi 0 dòng khung đầu và 10 dòng người học; không có dòng đánh dấu kết quả.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:223,280-282]`; `03-dd/logic/harness.md` mục 4.5; P2.

### AC-13 — Mỗi kiểu dữ liệu trong lược đồ đọc vào rồi in ra đúng chuỗi chuẩn ở cả ba ngôn ngữ
- **Tiền điều kiện**: với mỗi kiểu trong lược đồ (số nguyên, số nguyên dài, số thực, đúng/sai, ký tự, chuỗi, mảng một đến ba chiều, danh sách lồng, danh sách liên kết, cây nhị phân), một bài mẫu có hàm trả lại đúng tham số nhận vào.
- **Hành động**: chạy chương trình sinh ra trong môi trường chấm thật với input mẫu theo đúng định dạng, kể cả các ca rỗng (mảng rỗng, danh sách liên kết rỗng, cây rỗng).
- **Kết quả mong đợi**: output bằng đúng chuỗi in chuẩn của kiểu đó, giống nhau ở cả ba ngôn ngữ; số thực in cố định 12 chữ số thập phân; cây khác hình dạng cho chuỗi khác nhau.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:105-181]`; `03-dd/logic/harness.md` mục 3.3, 3.4, 10.3; P2.

### Ánh xạ lỗi biên dịch (`map`)

### AC-14 — Java: lỗi ở dòng đầu, dòng cuối và trong khối lồng của mã người học báo đúng dòng của người học
- **Tiền điều kiện**: ba mã Java cố ý hỏng, lỗi lần lượt ở dòng 1, ở dòng cuối cùng, và ở một dòng bên trong vòng lặp lồng.
- **Hành động**: biên dịch gói chấm Java rồi dịch lỗi.
- **Kết quả mong đợi**: mỗi lần chỉ có chẩn đoán trỏ đúng dòng 1, dòng cuối, và dòng lồng **tính theo mã của người học** (không cộng số dòng khung); nội dung lỗi giữ nguyên lời của trình biên dịch.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:19-21]` (F3-11); `[SoT: 02-bd/architecture/harness.md:268-279]`; `03-dd/logic/harness.md` mục 6.3; P3.

### AC-15 — C++: lỗi ở dòng đầu, dòng cuối và trong khối lồng báo đúng dòng của người học
- **Tiền điều kiện**: như AC-14 với C++.
- **Hành động**: biên dịch rồi dịch lỗi.
- **Kết quả mong đợi**: như AC-14; có thêm số cột đúng (cột không bị lệch).
- **Nguồn**: như AC-14; P3.

### AC-16 — Python: lỗi cú pháp ở dòng đầu, dòng cuối và trong khối lồng báo đúng dòng của người học
- **Tiền điều kiện**: như AC-14 với Python (lỗi cú pháp và lỗi thụt lề).
- **Hành động**: kiểm cú pháp rồi dịch lỗi.
- **Kết quả mong đợi**: như AC-14.
- **Nguồn**: như AC-14; `03-dd/logic/harness.md` mục 4.4; P3.

### AC-17 — Lỗi thuộc mã khung bị che hoàn toàn khỏi người học
- **Tiền điều kiện**: đầu ra biên dịch có lỗi nằm hoàn toàn trong phần khung (đầu hoặc cuối) và không có lỗi nào của người học.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học chỉ thấy một thông điệp cố định, không nội suy theo lỗi thật; thông điệp **không** chứa đoạn mã khung, tên biến hoặc hàm nội bộ, đường dẫn tệp tạm, hay số phiên bản trình biên dịch; toàn văn lỗi thật được ghi vào nhật ký nội bộ kèm mã đối chiếu, và không đi qua bất kỳ đường nào người học truy cập được.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:19-21]` (F3-12); `[SoT: 02-bd/security/harness.md:59-70]`; `03-dd/logic/harness.md` mục 6.3, 6.5, 8; P3.

### AC-18 — Sai chữ ký hàm hoặc thiếu hàm cho thông báo đúng chữ ký, không phải "lỗi hệ thống"
- **Tiền điều kiện**: mã người học đúng cú pháp nhưng hàm đặt sai tên (hoặc sai số tham số) ở cả ba ngôn ngữ; lỗi do trình biên dịch báo tại dòng gọi hàm của khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: thông báo yêu cầu kiểm tra tên hàm, số tham số và kiểu, kèm chữ ký đúng của bài theo ngôn ngữ đó (chữ ký này đã hiển thị cho người học ở mã khung); không hiển thị dòng mã khung.
- **Nguồn**: `03-dd/logic/harness.md` mục 6.3, 6.4; P3.

### AC-19 — Thiếu hoặc thừa dấu đóng khối cho thông báo gợi ý cố định
- **Tiền điều kiện**: mã Java và C++ thiếu một dấu đóng khối ở cuối; mã có một dấu đóng khối thừa; lỗi được báo ở dòng thuộc phần khung cuối.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học thấy thông điệp cố định nhắc kiểm tra dấu đóng khối; không có số dòng trỏ vào phần khung; không lộ mã khung.
- **Nguồn**: `03-dd/logic/harness.md` mục 6.3 và bảng 5; P3.

### AC-20 — Lỗi người học lẫn lỗi dây chuyền ở khung: chỉ hiện lỗi của người học
- **Tiền điều kiện**: đầu ra biên dịch có hai lỗi ở mã người học (một lỗi lặp lại hai lần) và ba lỗi ở phần khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: chỉ hai lỗi của người học được trả về, đã khử lặp và sắp theo số dòng tăng dần, tối đa mười lỗi; lỗi khung không xuất hiện (nhưng được ghi nhật ký nội bộ).
- **Nguồn**: `03-dd/logic/harness.md` mục 6.3; P3.

### AC-21 — Standard I/O: mọi lỗi quy về mã người học, kể cả lỗi "hết tệp" và thiếu `main`
- **Tiền điều kiện**: mã Standard I/O có lỗi ở dòng cuối; một mã khác thiếu dấu đóng khối nên trình biên dịch báo lỗi ở dòng ngay sau dòng cuối; một mã C++ không có hàm `main`.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: hai ca đầu báo ở dòng cuối của mã người học (không báo ngoài vùng); ca thứ ba báo "Chương trình chưa có hàm `main`". Không ca nào bị coi là lỗi hệ thống.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:280-282]`; `03-dd/logic/harness.md` mục 6.3 bước 1 và 3; P3.

### AC-22 — C++: lỗi thể hiện mẫu kèm chú thích trỏ vào khung chỉ hiện phần thuộc mã người học
- **Tiền điều kiện**: mã C++ gây lỗi thể hiện mẫu; đầu ra có một lỗi chính ở dòng của người học và các dòng "required from" và chú thích trỏ vào mã khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học thấy lỗi chính ở dòng đã quy đổi và các chú thích thuộc vùng của họ (nếu có), không thấy dòng chú thích nào trỏ vào mã khung, không thấy đường dẫn tệp hay tên hàm nội bộ.
- **Nguồn**: `03-dd/logic/harness.md` mục 6.2, 6.4, 6.5; P3.

### So khớp kết quả (`match`)

### AC-23 — `EXACT`: bằng nhau tuyệt đối, bỏ qua xuống dòng ở cuối
- **Tiền điều kiện**: bài có chiến lược so khớp `EXACT`.
- **Hành động**: so output của người học với expected-output trong ba ca: giống hệt; khác đúng một ký tự; khác đúng một dấu cách; chỉ khác số ký tự xuống dòng ở cuối.
- **Kết quả mong đợi**: ca 1 và ca 4 khớp; ca 2 và ca 3 không khớp.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:16-18]` (F3-07); `[SoT: 02-bd/architecture/harness.md:239-242]`; `03-dd/logic/harness.md` mục 7; P4. Việc xuống dòng cuối đang chờ chủ dự án xác nhận (mục 4).

### AC-24 — `TRIMMED`: khác biệt khoảng trắng thừa không làm sai, khác biệt nội dung vẫn sai
- **Tiền điều kiện**: bài có chiến lược `TRIMMED`, kiểu trả về mảng nhiều chiều.
- **Hành động**: so output có khoảng trắng thừa đầu và cuối dòng, nhiều dấu cách liên tiếp giữa các số, với expected-output chuẩn; so thêm một output khác một giá trị.
- **Kết quả mong đợi**: ca khoảng trắng khớp; ca khác một giá trị không khớp. Với kiểu danh sách lồng và cây nhị phân, `TRIMMED` cho cùng kết quả như `EXACT`.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:16-18]` (F3-08); `[SoT: 02-bd/architecture/harness.md:243-246]`; P4.

### AC-25 — `EPSILON`: giá trị đúng sát biên được chấp nhận, vượt biên bị loại
- **Tiền điều kiện**: bài trả về số thực, `EPSILON` với sai số 0,001.
- **Hành động**: so output sai lệch đúng 0,001 và output sai lệch 0,0011 so với expected-output.
- **Kết quả mong đợi**: sai lệch đúng bằng sai số thì khớp (biên được tính); sai lệch lớn hơn thì không khớp.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:16-18]` (F3-09); `[SoT: 02-bd/architecture/harness.md:247-248]` ("nhỏ hơn hoặc bằng"); P4.

### AC-26 — `EPSILON` với cấu trúc lồng so từng phần tử theo vị trí; Standard I/O so theo token
- **Tiền điều kiện**: (a) danh sách số thực và mảng hai chiều số thực với `EPSILON`; (b) bài Standard I/O với `EPSILON`.
- **Hành động**: so output mà mọi phần tử nằm trong sai số; một output sai hình dạng (ít một phần tử); một output sai một phần tử vượt sai số; ở (b) output có cả số và chữ.
- **Kết quả mong đợi**: ca trong sai số khớp; sai hình dạng và sai phần tử vượt sai số không khớp; ở (b) các số so bằng sai số, các token chữ phải giống hệt.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:249-252]`; `03-dd/logic/harness.md` mục 7.2, 7.3; P4.

### AC-27 — `UNORDERED_SET`: thứ tự khác vẫn khớp, thiếu hoặc thừa phần tử không khớp
- **Tiền điều kiện**: bài trả về danh sách một chiều số nguyên, chiến lược `UNORDERED_SET`.
- **Hành động**: so output cùng tập phần tử nhưng thứ tự khác; output thiếu một phần tử; output thừa một phần tử; so với bài Standard I/O dùng cùng chiến lược.
- **Kết quả mong đợi**: ca khác thứ tự khớp; ca thiếu và ca thừa không khớp; Standard I/O so tập các token.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:16-18]` (F3-10); `[SoT: 02-bd/architecture/harness.md:253-259]`; P4. Trường hợp phần tử trùng lặp đang chờ quyết (mục 4).

### AC-28 — Hai cây cùng giá trị khác hình dạng không bị coi là bằng nhau
- **Tiền điều kiện**: bài trả về cây nhị phân, chiến lược `EXACT`.
- **Hành động**: so output của hai cây có cùng tập giá trị nhưng khác hình dạng; so output cây rỗng với expected cây rỗng.
- **Kết quả mong đợi**: hai cây khác hình dạng không khớp; cây rỗng khớp cây rỗng.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:175-178,239-242]`; P4.

### AC-29 — Output debug của người học không làm sai kết quả; thoát sớm không phải lỗi hệ thống
- **Tiền điều kiện**: bài mô hình Bọc hàm; mã người học in thêm vài dòng debug trước khi trả kết quả.
- **Hành động**: so output của người học với expected-output; lặp lại với một chương trình thoát sớm trước khi in kết quả.
- **Kết quả mong đợi**: ca có debug khớp nếu đáp án đúng, và phần debug được trả riêng để hiển thị ở phần console của người học; ca thoát sớm trả "không khớp", không báo lỗi hệ thống.
- **Nguồn**: `03-dd/logic/harness.md` mục 5.4; P4.

### AC-30 — Kết quả so khớp không bao giờ lộ expected-output hay khác biệt; expected-output hỏng không tính là sai đáp án
- **Tiền điều kiện**: (a) một testcase ẩn mà output của người học sai; (b) một testcase có expected-output không đọc được theo kiểu trả về khi bài dùng `EPSILON`.
- **Hành động**: so khớp.
- **Kết quả mong đợi**: (a) kết quả chỉ là "khớp" hoặc "không khớp", không chứa expected-output, không chứa vị trí hay chi tiết khác biệt; (b) báo lỗi dữ liệu testcase để bên chấm bài xử lý, **không** trả "không khớp" gán cho người học.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:226]`; `[SoT: .claude/skills/dd-generation/SKILL.md:135-136]`; `HRN-050`; P4.

### Kiểm đặc tả hàm khi A2 lưu bài (`FunctionSpecEvaluator`)

### AC-31 — Chữ ký hợp lệ với mọi kiểu trong lược đồ được chấp nhận và bảo đảm sinh được mã
- **Tiền điều kiện**: các chữ ký dùng từng kiểu của lược đồ: nguyên thuỷ, chuỗi, mảng một đến ba chiều, danh sách lồng (đến ba lớp), danh sách liên kết, cây nhị phân.
- **Hành động**: kiểm đặc tả.
- **Kết quả mong đợi**: không có vi phạm; bài được đánh dấu hỗ trợ mô hình Bọc hàm; sinh mã khung và gói chấm cho cả ba ngôn ngữ thành công cho từng chữ ký.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:11-12]` (F3-01); `[SoT: 02-bd/architecture/harness.md:203-208]`; P5.

### AC-32 — Kiểu ngoài lược đồ vẫn lưu được nhưng bài chỉ còn Standard I/O, và A2 được báo lý do
- **Tiền điều kiện**: chữ ký có (a) một kiểu không có trong lược đồ; (b) danh sách thiếu kiểu phần tử; (c) tổ hợp không được hỗ trợ (ví dụ danh sách của mảng).
- **Hành động**: kiểm đặc tả khi A2 lưu.
- **Kết quả mong đợi**: lưu **thành công**, bài bị đánh dấu chỉ hỗ trợ Standard I/O ngay tại lúc lưu (không đợi học viên mở bài), A2 thấy cảnh báo kèm lý do từng trường hợp.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:30-34]`; `[SoT: 02-bd/architecture/harness.md:193-201]`; `[SoT: 02-bd/screens/shared/SHR0202_problem_authoring.md:411]`; P5.

### AC-33 — Ranh giới số chiều và độ sâu lồng
- **Tiền điều kiện**: chữ ký (a) mảng ba chiều; (b) mảng bốn chiều; (c) danh sách lồng ba lớp; (d) danh sách lồng bốn lớp.
- **Hành động**: kiểm đặc tả.
- **Kết quả mong đợi**: (a) và (c) được hỗ trợ; (b) và (d) bị coi ngoài lược đồ (chỉ còn Standard I/O).
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:103]`; `03-dd/validation/harness.md` Bảng 1; P5.

### AC-34 — Kiểu "không hỗ trợ" không bao giờ được lưu vào đặc tả
- **Tiền điều kiện**: bài có kiểu ngoài lược đồ (AC-32).
- **Hành động**: A2 lưu rồi mở lại đặc tả đã lưu.
- **Kết quả mong đợi**: đặc tả lưu giữ đúng cấu trúc A2 đã nhập; không có giá trị thay thế kiểu "không hỗ trợ" nào được ghi vào dữ liệu; chỉ cờ "bài chỉ hỗ trợ Standard I/O" thay đổi.
- **Nguồn**: `[SoT: 02-bd/database/harness.md:90]`; `[SoT: 02-bd/architecture/harness.md:103]`; P5.

### AC-35 — Tên hàm và tên tham số không hợp lệ bị từ chối lúc lưu
- **Tiền điều kiện**: A2 nhập: tên bắt đầu bằng số; tên chứa ký tự đặc biệt; tên dài hơn 64 ký tự; hai tham số `maxVal` và `max_val`; tham số trùng với từ khoá của một trong ba ngôn ngữ (ví dụ `class`, `new`, `lambda`); tên bắt đầu bằng tiền tố dành riêng của khung.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: từng trường hợp bị từ chối **không lưu**, kèm lý do và vị trí trường sai; không trường hợp nào dẫn tới mã khung không biên dịch được.
- **Nguồn**: `03-dd/validation/harness.md` Bảng 2; P5.

### AC-36 — Ghi đè tên hàm sai khoá hoặc sai giá trị bị từ chối
- **Tiền điều kiện**: A2 nhập ghi đè cho ngôn ngữ không tồn tại (ví dụ `RUBY`); ghi đè Java bằng từ khoá Java; ghi đè hợp lệ cho Python.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: hai trường hợp đầu bị từ chối; trường hợp hợp lệ lưu được và mã khung Python dùng tên đó.
- **Nguồn**: `[SoT: 02-bd/database/problem-bank.md:135]`; `03-dd/validation/harness.md` Bảng 2; P5.

### AC-37 — Tổ hợp chiến lược so khớp và kiểu không hợp lệ bị từ chối
- **Tiền điều kiện**: A2 chọn (a) `EPSILON` không nhập sai số, hoặc sai số bằng 0 hoặc âm; (b) `UNORDERED_SET` cho bài trả về cây nhị phân; (c) `UNORDERED_SET` cho bài trả về danh sách liên kết.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: cả ba bị từ chối không lưu, kèm lý do; `EPSILON` với sai số dương và `UNORDERED_SET` với danh sách một chiều lưu được.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:257-259,300-301]`; `[SoT: 02-bd/database/problem-bank.md:129]`; P5.

### AC-38 — Chưa khai chữ ký chỉ còn Standard I/O; chữ ký nửa vời bị từ chối
- **Tiền điều kiện**: (a) A2 chỉ khai định dạng stdin/stdout, không khai chữ ký; (b) A2 khai tên hàm nhưng bỏ trống kiểu trả về.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: (a) lưu được, bài chỉ hỗ trợ Standard I/O; (b) bị từ chối, yêu cầu khai đủ.
- **Nguồn**: `03-dd/validation/harness.md` Bảng 2 (`HRN-V07`, `HRN-V17`); đề xuất của DD, đang chờ xác nhận (mục 4).

### Đọc đặc tả, sự kiện, an toàn và tính độc lập

### AC-39 — Đọc đặc tả thất bại tạm thời cho lỗi rõ ràng, không kết quả nửa vời
- **Tiền điều kiện**: bên cung cấp đặc tả lỗi hoặc quá thời gian.
- **Hành động**: xin mã khung hoặc chuẩn bị gói chấm.
- **Kết quả mong đợi**: báo lỗi tạm thời, có thể thử lại; không có mã khung hay gói nào được trả về.
- **Nguồn**: `HRN-003`; O1.

### AC-40 — Sau khi A2 đổi đặc tả, lần sinh mã kế tiếp dùng đặc tả mới
- **Tiền điều kiện**: bài đã được sinh mã khung một lần; sau đó A2 đổi tên hàm chung và lưu.
- **Hành động**: xin mã khung và chuẩn bị gói chấm ngay sau lần lưu.
- **Kết quả mong đợi**: cả hai dùng tên hàm mới; không có lần sinh nào dùng tên cũ. Việc nhận thông báo đặc tả đổi hai lần hay nhận thông báo cũ hơn không gây lỗi và không xoá nhầm đặc tả mới.
- **Nguồn**: `[SoT: 02-bd/architecture/problem-bank.md:67]`; `[SoT: 02-bd/database/harness.md:49-73]`; E1.

### AC-41 — Input testcase hỏng làm chương trình báo hiệu riêng, không lộ dữ liệu, không bị tính là lỗi chạy của người học
- **Tiền điều kiện**: chương trình bọc hàm sinh ra nhận input sai định dạng (ví dụ ký tự chữ ở chỗ số, hoặc `true` viết hoa).
- **Hành động**: chạy chương trình.
- **Kết quả mong đợi**: chương trình dừng trước khi gọi hàm của người học, với mã thoát dành riêng cho lỗi đọc input và một dòng báo hiệu cố định; dòng báo hiệu và mọi thông điệp **không chứa** bất kỳ phần nào của input; bên chấm bài nhận biết đây không phải lỗi chạy của học viên.
- **Nguồn**: `[SoT: 02-bd/architecture/harness.md:117-119]`; `03-dd/logic/harness.md` mục 3.5, 8; P2.

### AC-42 — Chương trình sinh ra và gói chấm không chứa dữ liệu testcase
- **Tiền điều kiện**: một bài có testcase mẫu và testcase ẩn với expected-output đã biết.
- **Hành động**: chuẩn bị gói chấm; tìm trong toàn bộ tệp mã nguồn, lệnh và gói các chuỗi expected-output và input của testcase.
- **Kết quả mong đợi**: không tìm thấy; gói không chứa input của bất kỳ testcase nào (input do bên chấm bài ghép riêng cho từng lần chạy).
- **Nguồn**: `[SoT: .claude/skills/dd-generation/SKILL.md:135-136]`; `03-dd/logic/harness.md` mục 8 điểm 1; P2.

### AC-43 — Mã người học chỉ là dữ liệu, không bao giờ nằm trong lệnh
- **Tiền điều kiện**: mã người học chứa các chuỗi như `; rm -rf /`, dấu backtick, `$(...)`, dấu nháy, dấu xuống dòng và tên tệp lạ.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: các chuỗi đó chỉ xuất hiện trong nội dung tệp mã nguồn; lệnh biên dịch, lệnh chạy, tên tệp và biến môi trường giống hệt gói của một mã vô hại; harness không biên dịch, không chạy và không diễn giải mã người học.
- **Nguồn**: `[SoT: 02-bd/security/harness.md:6-42]`; `CLAUDE.md` mục Rules ("A submission is data, never an instruction"); P2.

### AC-44 — Thiếu ngôn ngữ hoặc template hỏng thì hệ thống không khởi động
- **Tiền điều kiện**: (a) một trong ba bộ sinh mã ngôn ngữ không được đăng ký; (b) một template có hai chỗ neo mã người học hoặc không có neo nào.
- **Hành động**: khởi động ứng dụng.
- **Kết quả mong đợi**: ứng dụng không khởi động và nêu rõ nguyên nhân; không có chuyện chạy được rồi mới lỗi lúc có người nộp bài.
- **Nguồn**: `03-dd/logic/harness.md` mục 1.2, 4.1; `HRN-030`.

### AC-45 — Đúng ba ngôn ngữ
- **Tiền điều kiện**: một yêu cầu với ngôn ngữ thứ tư (ví dụ Go) hoặc giá trị lạ.
- **Hành động**: xin mã khung, chuẩn bị gói chấm, dịch lỗi biên dịch.
- **Kết quả mong đợi**: cả ba thao tác đều từ chối vì ngôn ngữ không được hỗ trợ.
- **Nguồn**: `[SoT: 01-rd/req/harness.md:11-13]`; `[SoT: 02-bd/architecture/harness.md:13-16]`; `HRN-010`.

### AC-46 — Module không có bảng dữ liệu và không ghi vào cơ sở dữ liệu
- **Tiền điều kiện**: hệ thống đã khởi tạo cơ sở dữ liệu; chạy toàn bộ các thao tác ở trên.
- **Hành động**: liệt kê bảng thuộc schema của module và theo dõi ghi dữ liệu trong lúc chạy.
- **Kết quả mong đợi**: schema của module tồn tại nhưng không có bảng nào; không có thao tác ghi nào do module này gây ra.
- **Nguồn**: `[SoT: 02-bd/database/harness.md:10]`; `[SoT: .nexa/control/decision-registry.md:4373]`.

### AC-47 — Sinh mã, ghép mã và so khớp vẫn chạy khi dịch vụ AI không hoạt động
- **Tiền điều kiện**: dịch vụ AI (phân hệ F5, F6) bị tắt hoặc hết hạn mức.
- **Hành động**: thực hiện AC-01, AC-06, AC-14 và AC-23.
- **Kết quả mong đợi**: tất cả vẫn cho kết quả đúng như khi dịch vụ AI hoạt động; module không gọi và không phụ thuộc đường AI.
- **Nguồn**: `CLAUDE.md` mục Rules ("The AI subsystem degrades gracefully"); `03-dd/logic/harness.md` mục 8 điểm 3.

## 3. Truy vết

Bảng 1: Truy vết tiêu chí tới cổng và khối quy tắc. Cột "Test case" điền khi có test thật theo `testcase-generation`.

| AC | Cổng / luồng | Khối quy tắc | Test viewpoint ở DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| AC-01 | P1 `getStarterCode` | BR-01, BR-05 | P1-T01, P1-T03 | (điền khi có TC) | chưa |
| AC-02 | P1 | BR-01 | P1-T07 | (điền khi có TC) | chưa |
| AC-03 | P1 | BR-04 | P1-T02 | (điền khi có TC) | chưa |
| AC-04 | P1 | BR-05 | P1-T04 | (điền khi có TC) | chưa |
| AC-05 | P1 | BR-05 | P1-T05, P1-T06 | (điền khi có TC) | chưa |
| AC-06 | P2 `prepare` | BR-03 | P2-T01 | (điền khi có TC) | chưa |
| AC-07 | P2 | BR-03 | P2-T02 | (điền khi có TC) | chưa |
| AC-08 | P2 | BR-02 | P2-T07 | (điền khi có TC) | chưa |
| AC-09 | P2 | BR-03 | P2-T08 | (điền khi có TC) | chưa |
| AC-10 | P2 | validation mục 5 | P2-T05, P2-T06 | (điền khi có TC) | chưa |
| AC-11 | P2 | BR-09 | P2-T04 | (điền khi có TC) | chưa |
| AC-12 | P2 | BR-04 | P2-T01 | (điền khi có TC) | chưa |
| AC-13 | P2 (tầng chuỗi công cụ) | mục 3.3, 3.4, 10.3 | P2-T01 | (điền khi có TC) | chưa |
| AC-14 | P3 `map` (Java) | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-15 | P3 (C++) | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-16 | P3 (Python) | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-17 | P3 | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-18 | P3 | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-19 | P3 | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-20 | P3 | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-21 | P3 (Standard I/O) | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-22 | P3 (C++ mẫu) | BR-07 | mục 6.1 api | (điền khi có TC) | chưa |
| AC-23 | P4 `match` (`EXACT`) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-24 | P4 (`TRIMMED`) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-25 | P4 (`EPSILON` biên) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-26 | P4 (`EPSILON` lồng, Standard I/O) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-27 | P4 (`UNORDERED_SET`) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-28 | P4 (cây) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-29 | P4 (dòng đánh dấu) | BR-06 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-30 | P4 (không lộ dữ liệu) | BR-08 | mục 6.2 api | (điền khi có TC) | chưa |
| AC-31 | P5 `evaluate` | validation mục 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-32 | P5 | validation mục 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-33 | P5 | validation Bảng 1 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-34 | P5 | validation mục 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-35 | P5 | validation Bảng 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-36 | P5 | validation Bảng 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-37 | P5 | validation Bảng 3 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-38 | P5 | validation Bảng 2 | mục 6.3 api | (điền khi có TC) | chưa |
| AC-39 | O1 `findSpec` | BR-09 | O1-T03, P2-T09 | (điền khi có TC) | chưa |
| AC-40 | E1 `ProblemSpecChanged` | BR-09 | E1-T01 đến E1-T04 | (điền khi có TC) | chưa |
| AC-41 | P2 (chương trình sinh ra) | mục 3.5 logic | P2-T01 | (điền khi có TC) | chưa |
| AC-42 | P2 | mục 8 logic | P2-T01 | (điền khi có TC) | chưa |
| AC-43 | P2 | BR-02, BR-06 | P2-T07 | (điền khi có TC) | chưa |
| AC-44 | khởi động | BR-02 | (không có ở DD) | (điền khi có TC) | chưa |
| AC-45 | P1, P2, P3 | mục 1.2 logic | (không có ở DD) | (điền khi có TC) | chưa |
| AC-46 | schema | `02-bd/database/harness.md` mục 1 | (không có ở DD) | (điền khi có TC) | chưa |
| AC-47 | P1, P2, P3, P4 | mục 8 logic | (không có ở DD) | (điền khi có TC) | chưa |

Mỗi cổng có ít nhất một tiêu chí: P1 (AC-01 đến AC-05), P2 (AC-06 đến AC-13), P3 (AC-14 đến AC-22), P4 (AC-23 đến
AC-30), P5 (AC-31 đến AC-38), O1 (AC-39), E1 (AC-40). Số hiệu P, O, E theo `03-dd/api/harness.md` Bảng 1.

## 4. Chưa chốt

Không có tiêu chí giữ chỗ cho các mục dưới đây; khi chủ dự án quyết, bổ sung `AC-nn` mới ở cuối file, không đánh số lại.

| Mục | Ai quyết | Ghi chú |
| :--- | :--- | :--- |
| `EXACT` có bỏ qua xuống dòng ở cuối hay không (AC-23 đang dùng "có") | Chủ dự án | `03-dd/logic/harness.md` mục 12, Q12 |
| `UNORDERED_SET` là tập hay đa tập (phần tử trùng lặp) | Chủ dự án | Q13; BD ghi "tập hợp" nhưng không nói về trùng lặp. Test đối kháng "tập có phần tử trùng" chờ quyết |
| Chính sách khi đặc tả đổi giữa lúc nộp và lúc chấm (AC-11 chỉ nghiệm thu phía `harness`) | `judge-orchestration` DD, chủ dự án | `03-dd/api/harness.md` Q3 |
| Chưa khai chữ ký có tự đặt bài về Standard I/O hay không (AC-38a) | Chủ dự án | `03-dd/logic/harness.md` Q16 |
| Định dạng đầu vào mảng nhiều chiều (dòng tiêu đề kích thước) | Chủ dự án và DD `problem-bank` | Q8; ảnh hưởng cách viết testcase |
| Lỗi lúc chạy có phải dịch dòng, bỏ mã khung không | Chủ dự án | Q14; không có AC cho tới khi quyết, đề xuất D5 |
| Ai được xin mã khung và có kiểm bài đã xuất bản | Chủ dự án | `03-dd/api/harness.md` Q2 |
| Hình dạng chính xác của gói gửi go-judge | Hai DD `harness` và `judge-orchestration` | `03-dd/api/harness.md` Q4 |

## 5. Chưa phủ được

| Điều không nghiệm thu được ở đây | Vì sao | Cách phủ sau |
| :--- | :--- | :--- |
| Khớp thật giữa mẫu lỗi biên dịch trong test và đầu ra của trình biên dịch trên image go-judge | Image chưa build (`01-rd/system/SYS0401_judge_engine.md:283-295`); regex phụ thuộc phiên bản | Tầng "chuỗi công cụ" của `03-dd/logic/harness.md` mục 10.3 chụp lại mẫu thật khi image sẵn sàng |
| Hành vi khi hai module chạy cùng nhau trong môi trường thật (hàng đợi, giới hạn tài nguyên) | Thuộc giai đoạn xây dựng 2 (`.nexa/control/dependency-map.md`) | Test tích hợp cùng `judge-orchestration` |
| Hiệu năng sinh mã dưới tải | Cache trong tiến trình mặc định tắt, chưa đo | Chỉ đo và thêm tiêu chí khi cần bật cache |
| Thông điệp lỗi lúc chạy (ngoại lệ trong hàm người học) có lộ mã khung không | BD chỉ giao lỗi biên dịch cho module này | Q14, đề xuất D5 |
