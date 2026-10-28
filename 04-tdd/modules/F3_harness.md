# TDD — Tiêu chí nghiệm thu module `harness` (F3)

> Bounded Context: F3 `harness` · DD: `03-dd/api/F3_harness.md` (hợp đồng cổng), `03-dd/logic/F3_harness.md`,
> `03-dd/validation/F3_harness.md` · Cập nhật: 2026-10-23
>
> Mỗi `AC-nn` map về một phương thức cổng (`03-dd/api/F3_harness.md` mục 1.1, ký hiệu P1 đến P5, O1, E1) hoặc một khối
> quy tắc (`BR-nn` ở `03-dd/logic/F3_harness.md`). Viết cùng lúc với DD theo `.claude/rules/tdd-mode.md`. Mã lỗi `HRN-...`
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
- **Nguồn**: `[SoT: 01-rd/req/user_stories/a1_student.md:50]`; `[SoT: 02-bd/screens/users/USR0102_problem_detail.md:697,745]`; `03-dd/logic/F3_harness.md` mục 4.6; P1.

### AC-02 — Ghi đè tên hàm chỉ đổi đúng ngôn ngữ được ghi đè
- **Tiền điều kiện**: bài có ghi đè tên hàm cho Java là `solve`.
- **Hành động**: xin mã khung bọc hàm cho ba ngôn ngữ.
- **Kết quả mong đợi**: Java có hàm `solve`; Python và C++ vẫn dùng tên suy ra từ tên chung.
- **Nguồn**: `[SoT: 02-bd/database/F2_problem-bank.md:135]`; `[SoT: .nexa/control/decision-registry.md:2666]`; `03-dd/logic/F3_harness.md` mục 2; P1.

### AC-03 — Mã khung Standard I/O luôn có, không phụ thuộc chữ ký hàm
- **Tiền điều kiện**: một bài hỗ trợ cả hai mô hình, và một bài khác chỉ hỗ trợ Standard I/O.
- **Hành động**: xin mã khung Standard I/O cho ba ngôn ngữ ở cả hai bài.
- **Kết quả mong đợi**: cả hai bài đều trả về một chương trình tối thiểu có hàm `main`, biên dịch được, không chứa mã đọc hay in theo kiểu dữ liệu của bài.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:30-34]`; `[SoT: 02-bd/architecture/F3_harness.md:223]`; `03-dd/logic/F3_harness.md` mục 4.5; P1.

### AC-04 — Máy chủ từ chối mô hình Bọc hàm cho bài chỉ hỗ trợ Standard I/O, dù giao diện đã ẩn nút
- **Tiền điều kiện**: bài có kiểu dữ liệu ngoài lược đồ nên được đánh dấu chỉ hỗ trợ Standard I/O.
- **Hành động**: gọi thẳng yêu cầu mã khung mô hình Bọc hàm (như khi người dùng sửa tay tham số trên đường dẫn), rồi gọi cổng chuẩn bị gói chấm P2 với cùng bài và mô hình.
- **Kết quả mong đợi**: cả P1 và P2 từ chối với `HRN-004`; P1 hiển thị "Bài toán này chỉ hỗ trợ mô hình Standard I/O.", không trả mã khung; P2 không sinh `PreparedProgram` hoặc payload. Yêu cầu Standard I/O cho cùng bài vẫn thành công.
- **Nguồn**: `[SoT: 02-bd/screens/users/USR0102_problem_detail.md:834]`; `03-dd/api/F3_harness.md` P2-T03, P1-T04; `HRN-004`.

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
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:263-267]`; `03-dd/logic/F3_harness.md` mục 4.3; P2.

### AC-07 — Cùng đầu vào cho cùng gói, từng byte
- **Tiền điều kiện**: một bài, một ngôn ngữ, một mã người học, một phiên bản đặc tả.
- **Hành động**: chuẩn bị gói chấm hai lần.
- **Kết quả mong đợi**: hai gói giống hệt nhau từng byte (tệp mã nguồn, lệnh, bảng ánh xạ, thông số so khớp).
- **Nguồn**: `03-dd/api/F3_harness.md` mục 2.1; P2.

### AC-08 — Mã người học phá cấu trúc không làm đổi vị trí chèn
- **Tiền điều kiện**: ba mã người học cùng số dòng: (a) vô hại; (b) có một dòng chứa nguyên văn chuỗi neo của khung (`@@APH_USER_CODE@@`) và một dòng chứa `{{FUNCTION_NAME}}`; (c) có một dấu đóng khối thừa.
- **Hành động**: chuẩn bị gói chấm cho từng mã ở ba ngôn ngữ.
- **Kết quả mong đợi**: số dòng khung đầu và vị trí các dòng khung cuối giống hệt giữa ba trường hợp; phần khung không bị nội suy bởi nội dung người học; ở (c) kết quả tối đa là lỗi biên dịch ở bước sau, không phải thay đổi logic của khung.
- **Nguồn**: `[SoT: 02-bd/security/F3_harness.md:12-32]`; `03-dd/logic/F3_harness.md` mục 4.1; P2.

### AC-09 — Chuẩn hoá xuống dòng và BOM không làm lệch số dòng
- **Tiền điều kiện**: mã người học (a) dùng xuống dòng kiểu Windows; (b) không kết thúc bằng xuống dòng; (c) có ký tự đánh dấu thứ tự byte (BOM) ở đầu.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: số dòng mã người học trong bảng ánh xạ đúng số dòng nhìn thấy; phần khung cuối luôn bắt đầu ở dòng mới, không dính vào dòng cuối của người học.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 4.2; P2.

### AC-10 — Mã nguồn không hợp lệ bị từ chối trước khi sinh
- **Tiền điều kiện**: ba mã: rỗng (chỉ khoảng trắng), 65 537 byte, chứa ký tự NUL.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: mỗi mã bị từ chối với lý do riêng; không gói nào được tạo. Mã đúng 65 536 byte được chấp nhận.
- **Nguồn**: `[SoT: 02-bd/security/F3_harness.md:53-57]`; `HRN-020`, `HRN-021`, `HRN-022`; P2.

### AC-11 — Đặc tả đổi giữa chừng được báo rõ ràng, không chấm nhầm âm thầm
- **Tiền điều kiện**: lượt nộp ghim phiên bản đặc tả 3; A2 vừa lưu lại đặc tả, nay là phiên bản 4.
- **Hành động**: chuẩn bị gói chấm với phiên bản mong đợi 3.
- **Kết quả mong đợi**: bị từ chối với lý do phiên bản đặc tả đã đổi; không có gói nào sinh theo phiên bản 4 bị gán nhầm là phiên bản 3.
- **Nguồn**: `[SoT: 02-bd/database/F2_problem-bank.md:136]`; `HRN-002`; P2. Chính sách xử lý ở bên chấm bài ghi ở mục 4.

### AC-12 — Standard I/O không có khung quanh mã người học
- **Tiền điều kiện**: bài bất kỳ, mô hình Standard I/O, mã người học 10 dòng.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: tệp mã nguồn trong gói là đúng mã người học (đã chuẩn hoá xuống dòng), không thêm dòng nào ở đầu hay cuối; bảng ánh xạ ghi 0 dòng khung đầu và 10 dòng người học; không có dòng đánh dấu kết quả.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:223,280-282]`; `03-dd/logic/F3_harness.md` mục 4.5; P2.

### AC-13 — Mỗi kiểu dữ liệu trong lược đồ đọc vào rồi in ra đúng chuỗi chuẩn ở cả ba ngôn ngữ
- **Tiền điều kiện**: với mỗi kiểu trong lược đồ (số nguyên, số nguyên dài, số thực, đúng/sai, ký tự, chuỗi, mảng một đến ba chiều, danh sách lồng, danh sách liên kết, cây nhị phân), một bài mẫu có hàm trả lại đúng tham số nhận vào.
- **Hành động**: chạy chương trình sinh ra trong môi trường chấm thật với input mẫu theo đúng định dạng, kể cả các ca rỗng (mảng rỗng, danh sách liên kết rỗng, cây rỗng).
- **Kết quả mong đợi**: output bằng đúng chuỗi in chuẩn của kiểu đó, giống nhau ở cả ba ngôn ngữ; số thực in cố định 12 chữ số thập phân; cây khác hình dạng cho chuỗi khác nhau.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:105-181]`; `03-dd/logic/F3_harness.md` mục 3.3, 3.4, 10.3; P2.

### Ánh xạ lỗi biên dịch (`map`)

### AC-14 — Java: lỗi ở dòng đầu, dòng cuối và trong khối lồng của mã người học báo đúng dòng của người học
- **Tiền điều kiện**: ba mã Java cố ý hỏng, lỗi lần lượt ở dòng 1, ở dòng cuối cùng, và ở một dòng bên trong vòng lặp lồng.
- **Hành động**: biên dịch gói chấm Java rồi dịch lỗi.
- **Kết quả mong đợi**: mỗi lần chỉ có chẩn đoán trỏ đúng dòng 1, dòng cuối, và dòng lồng **tính theo mã của người học** (không cộng số dòng khung); nội dung lỗi giữ nguyên lời của trình biên dịch.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:19-21]` (F3-11); `[SoT: 02-bd/architecture/F3_harness.md:268-279]`; `03-dd/logic/F3_harness.md` mục 6.3; P3.

### AC-15 — C++: lỗi ở dòng đầu, dòng cuối và trong khối lồng báo đúng dòng của người học
- **Tiền điều kiện**: như AC-14 với C++.
- **Hành động**: biên dịch rồi dịch lỗi.
- **Kết quả mong đợi**: như AC-14; có thêm số cột đúng (cột không bị lệch).
- **Nguồn**: như AC-14; P3.

### AC-16 — Python: lỗi cú pháp ở dòng đầu, dòng cuối và trong khối lồng báo đúng dòng của người học
- **Tiền điều kiện**: như AC-14 với Python (lỗi cú pháp và lỗi thụt lề).
- **Hành động**: kiểm cú pháp rồi dịch lỗi.
- **Kết quả mong đợi**: như AC-14.
- **Nguồn**: như AC-14; `03-dd/logic/F3_harness.md` mục 4.4; P3.

### AC-17 — Lỗi thuộc mã khung bị che hoàn toàn khỏi người học
- **Tiền điều kiện**: đầu ra biên dịch có lỗi nằm hoàn toàn trong phần khung (đầu hoặc cuối) và không có lỗi nào của người học.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học chỉ thấy một thông điệp cố định, không nội suy theo lỗi thật; thông điệp **không** chứa đoạn mã khung, tên biến hoặc hàm nội bộ, đường dẫn tệp tạm, hay số phiên bản trình biên dịch; toàn văn lỗi thật được ghi vào nhật ký nội bộ kèm mã đối chiếu, và không đi qua bất kỳ đường nào người học truy cập được.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:19-21]` (F3-12); `[SoT: 02-bd/security/F3_harness.md:59-70]`; `03-dd/logic/F3_harness.md` mục 6.3, 6.5, 8; P3.

### AC-18 — Sai chữ ký hàm hoặc thiếu hàm cho thông báo đúng chữ ký, không phải "lỗi hệ thống"
- **Tiền điều kiện**: mã người học đúng cú pháp nhưng hàm đặt sai tên (hoặc sai số tham số) ở cả ba ngôn ngữ; lỗi do trình biên dịch báo tại dòng gọi hàm của khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: thông báo yêu cầu kiểm tra tên hàm, số tham số và kiểu, kèm chữ ký đúng của bài theo ngôn ngữ đó (chữ ký này đã hiển thị cho người học ở mã khung); không hiển thị dòng mã khung.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 6.3, 6.4; P3.

### AC-19 — Thiếu hoặc thừa dấu đóng khối cho thông báo gợi ý cố định
- **Tiền điều kiện**: mã Java và C++ thiếu một dấu đóng khối ở cuối; mã có một dấu đóng khối thừa; lỗi được báo ở dòng thuộc phần khung cuối.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học thấy thông điệp cố định nhắc kiểm tra dấu đóng khối; không có số dòng trỏ vào phần khung; không lộ mã khung.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 6.3 và bảng 5; P3.

### AC-20 — Lỗi người học lẫn lỗi dây chuyền ở khung: chỉ hiện lỗi của người học
- **Tiền điều kiện**: đầu ra biên dịch có hai lỗi ở mã người học (một lỗi lặp lại hai lần) và ba lỗi ở phần khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: chỉ hai lỗi của người học được trả về, đã khử lặp và sắp theo số dòng tăng dần, tối đa mười lỗi; lỗi khung không xuất hiện (nhưng được ghi nhật ký nội bộ).
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 6.3; P3.

### AC-21 — Standard I/O: mọi lỗi quy về mã người học, kể cả lỗi "hết tệp" và thiếu `main`
- **Tiền điều kiện**: mã Standard I/O có lỗi ở dòng cuối; một mã khác thiếu dấu đóng khối nên trình biên dịch báo lỗi ở dòng ngay sau dòng cuối; một mã C++ không có hàm `main`.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: hai ca đầu báo ở dòng cuối của mã người học (không báo ngoài vùng); ca thứ ba báo "Chương trình chưa có hàm `main`". Không ca nào bị coi là lỗi hệ thống.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:280-282]`; `03-dd/logic/F3_harness.md` mục 6.3 bước 1 và 3; P3.

### AC-22 — C++: lỗi thể hiện mẫu kèm chú thích trỏ vào khung chỉ hiện phần thuộc mã người học
- **Tiền điều kiện**: mã C++ gây lỗi thể hiện mẫu; đầu ra có một lỗi chính ở dòng của người học và các dòng "required from" và chú thích trỏ vào mã khung.
- **Hành động**: dịch lỗi.
- **Kết quả mong đợi**: người học thấy lỗi chính ở dòng đã quy đổi và các chú thích thuộc vùng của họ (nếu có), không thấy dòng chú thích nào trỏ vào mã khung, không thấy đường dẫn tệp hay tên hàm nội bộ.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 6.2, 6.4, 6.5; P3.

### So khớp kết quả (`match`)

### AC-23 — `EXACT`: bằng nhau tuyệt đối, bỏ qua xuống dòng ở cuối
- **Tiền điều kiện**: bài có chiến lược so khớp `EXACT`.
- **Hành động**: so output của người học với expected-output trong ba ca: giống hệt; khác đúng một ký tự; khác đúng một dấu cách; chỉ khác số ký tự xuống dòng ở cuối.
- **Kết quả mong đợi**: ca 1 và ca 4 khớp; ca 2 và ca 3 không khớp.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:16-18]` (F3-07); `[SoT: 02-bd/architecture/F3_harness.md:239-242]`; `03-dd/logic/F3_harness.md` mục 7; P4. Quy tắc xuống dòng cuối đã đóng ở DD F3 Q12.

### AC-24 — `TRIMMED`: khác biệt khoảng trắng thừa không làm sai, khác biệt nội dung vẫn sai
- **Tiền điều kiện**: bài có chiến lược `TRIMMED`, kiểu trả về mảng nhiều chiều hoặc mô hình Standard I/O để kiểm dòng trống.
- **Hành động**: so output có khoảng trắng thừa đầu và cuối dòng, nhiều dấu cách liên tiếp giữa các số, với expected-output chuẩn; so thêm output khác một giá trị và output khác số dòng trống ở giữa.
- **Kết quả mong đợi**: ca khoảng trắng khớp; ca khác một giá trị hoặc khác số dòng trống ở giữa không khớp. Với kiểu danh sách lồng và cây nhị phân, `TRIMMED` cho cùng kết quả như `EXACT`.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:16-18]` (F3-08); `[SoT: 02-bd/architecture/F3_harness.md:243-246]`; P4.

### AC-25 — `EPSILON`: giá trị đúng sát biên được chấp nhận, vượt biên bị loại
- **Tiền điều kiện**: bài trả về số thực, `EPSILON` với sai số 0,001.
- **Hành động**: so output sai lệch đúng 0,001 và output sai lệch 0,0011 so với expected-output; so `NaN`, `Infinity` và dấu của vô cực.
- **Kết quả mong đợi**: sai lệch đúng bằng sai số thì khớp (biên được tính); sai lệch lớn hơn thì không khớp. `NaN` chỉ khớp `NaN`, vô cực chỉ khớp cùng dấu.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:16-18]` (F3-09); `[SoT: 02-bd/architecture/F3_harness.md:247-248]` ("nhỏ hơn hoặc bằng"); P4.

### AC-26 — `EPSILON` với cấu trúc lồng so từng phần tử theo vị trí; Standard I/O so theo token
- **Tiền điều kiện**: (a) danh sách số thực và mảng hai chiều số thực với `EPSILON`; (b) bài Standard I/O với `EPSILON`.
- **Hành động**: so output mà mọi phần tử nằm trong sai số; một output sai hình dạng (ít một phần tử); một output sai một phần tử vượt sai số; ở (b) output có cả số và chữ.
- **Kết quả mong đợi**: ca trong sai số khớp; sai hình dạng và sai phần tử vượt sai số không khớp; ở (b) các số so bằng sai số, các token chữ phải giống hệt.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:249-252]`; `03-dd/logic/F3_harness.md` mục 7.2, 7.3; P4.

### AC-27 — So danh sách không theo thứ tự nhưng giữ số lần lặp
- **Tiền điều kiện**: bài trả về danh sách một chiều hoặc mảng hai chiều số nguyên, chiến lược `UNORDERED_SET`.
- **Hành động**: so `[1,1,2]` với `[2,1,1]`, rồi với `[1,2]`; so tương tự với bài nhập xuất chuẩn; so mảng hai chiều đổi hàng và đổi phần tử trong một hàng.
- **Kết quả mong đợi**: hai danh sách có cùng phần tử và cùng số lần lặp được coi là khớp dù khác thứ tự; thiếu một giá trị `1` thì không khớp. Quy tắc này cũng áp cho các giá trị của bài nhập xuất chuẩn. Với mảng hai chiều, đổi thứ tự hàng được, đổi thứ tự phần tử trong hàng không được.
- **Nguồn**: `[SoT: 03-dd/logic/F3_harness.md]` mục so khớp; quyết định chủ dự án 2026-10-25.

### AC-28 — Hai cây cùng giá trị khác hình dạng không bị coi là bằng nhau
- **Tiền điều kiện**: bài trả về cây nhị phân, chiến lược `EXACT`.
- **Hành động**: so output của hai cây có cùng tập giá trị nhưng khác hình dạng; so output cây rỗng với expected cây rỗng.
- **Kết quả mong đợi**: hai cây khác hình dạng không khớp; cây rỗng khớp cây rỗng.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:175-178,239-242]`; P4.

### AC-29 — Output debug của người học không làm sai kết quả; thoát sớm không phải lỗi hệ thống
- **Tiền điều kiện**: bài mô hình Bọc hàm; mã người học in thêm vài dòng debug trước khi trả kết quả.
- **Hành động**: so output của người học với expected-output; lặp lại với một chương trình thoát sớm trước khi in kết quả.
- **Kết quả mong đợi**: ca có debug khớp nếu đáp án đúng, và phần debug được trả riêng để hiển thị ở phần console của người học; ca thoát sớm trả "không khớp", không báo lỗi hệ thống.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 5.4; P4.

### AC-30 — Kết quả so khớp không bao giờ lộ expected-output hay khác biệt; expected-output hỏng không tính là sai đáp án
- **Tiền điều kiện**: (a) một testcase ẩn mà output của người học sai; (b) expected-output không đọc được theo kiểu trả về khi dùng `EPSILON` hoặc `UNORDERED_SET`; (c) actual-output không parse được; (d) `matchSpec` cũ chứa epsilon không hợp lệ hoặc `UNORDERED_SET` với cây.
- **Hành động**: so khớp.
- **Kết quả mong đợi**: (a) kết quả chỉ là "khớp" hoặc "không khớp", không chứa expected-output, không chứa vị trí hay chi tiết khác biệt; (b) `HRN-050` để bên chấm bài xử lý, **không** trả "không khớp" gán cho người học; (c) `NO_MATCH`; (d) `HRN-051`, không có verdict. Không lộ expected-output trong các nhánh lỗi.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:226]`; `[SoT: .claude/skills/dd-generation/SKILL.md:135-136]`; `HRN-050`; P4.

### Kiểm đặc tả hàm khi A2 lưu bài (`FunctionSpecEvaluator`)

### AC-31 — Chữ ký hợp lệ với mọi kiểu trong lược đồ được chấp nhận và bảo đảm sinh được mã
- **Tiền điều kiện**: các chữ ký dùng từng kiểu của lược đồ: nguyên thuỷ, chuỗi, mảng một đến ba chiều, danh sách lồng (đến ba lớp), danh sách liên kết, cây nhị phân.
- **Hành động**: kiểm đặc tả.
- **Kết quả mong đợi**: không có vi phạm; bài được đánh dấu hỗ trợ mô hình Bọc hàm; sinh mã khung và gói chấm cho cả ba ngôn ngữ thành công cho từng chữ ký.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:11-12]` (F3-01); `[SoT: 02-bd/architecture/F3_harness.md:203-208]`; P5.

### AC-32 — Kiểu ngoài lược đồ vẫn lưu được nhưng bài chỉ còn Standard I/O, và A2 được báo lý do
- **Tiền điều kiện**: chữ ký có (a) một kiểu không có trong lược đồ; (b) danh sách thiếu kiểu phần tử; (c) tổ hợp không được hỗ trợ (ví dụ danh sách của mảng); (d) hai `LINKED_LIST` có kiểu node khác nhau trong cùng đặc tả. Đối chứng: TypeNode sai hình dạng JSON (ví dụ `ARRAY.dimensions=0`).
- **Hành động**: kiểm đặc tả khi A2 lưu.
- **Kết quả mong đợi**: (a)–(d) lưu **thành công**, bài bị đánh dấu chỉ hỗ trợ Standard I/O ngay tại lúc lưu (không đợi học viên mở bài), A2 thấy cảnh báo kèm lý do từng trường hợp. TypeNode sai hình dạng bị từ chối `HRN-V10`/`422 PRB-036`, không ghi dữ liệu.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:30-34]`; `[SoT: 02-bd/architecture/F3_harness.md:193-201]`; `[SoT: 02-bd/screens/shared/SHR0202_problem_authoring.md:411]`; P5.

### AC-33 — Ranh giới số chiều và độ sâu lồng
- **Tiền điều kiện**: chữ ký (a) mảng ba chiều; (b) mảng bốn chiều; (c) danh sách lồng ba lớp; (d) danh sách lồng bốn lớp.
- **Hành động**: kiểm đặc tả.
- **Kết quả mong đợi**: (a) và (c) được hỗ trợ; (b) và (d) bị coi ngoài lược đồ (chỉ còn Standard I/O).
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:103]`; `03-dd/validation/F3_harness.md` Bảng 1; P5.

### AC-34 — Kiểu "không hỗ trợ" không bao giờ được lưu vào đặc tả
- **Tiền điều kiện**: bài có kiểu ngoài lược đồ (AC-32).
- **Hành động**: A2 lưu rồi mở lại đặc tả đã lưu.
- **Kết quả mong đợi**: đặc tả lưu giữ đúng cấu trúc A2 đã nhập; không có giá trị thay thế kiểu "không hỗ trợ" nào được ghi vào dữ liệu; chỉ cờ "bài chỉ hỗ trợ Standard I/O" thay đổi.
- **Nguồn**: `[SoT: 02-bd/database/F3_harness.md:90]`; `[SoT: 02-bd/architecture/F3_harness.md:103]`; P5.

### AC-35 — Tên hàm và tên tham số không hợp lệ bị từ chối lúc lưu
- **Tiền điều kiện**: A2 nhập: tên bắt đầu bằng số; tên chứa ký tự đặc biệt; tên dài hơn 64 ký tự; hai tham số `maxVal` và `max_val`; tham số trùng với từ khoá của một trong ba ngôn ngữ (ví dụ `class`, `new`, `lambda`); tên hàm là từ khoá sau suy ra tên; tên bắt đầu bằng tiền tố dành riêng của khung; tên tham số `self` hoặc trùng tên hàm; 10 và 11 tham số.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: các tên sai và 11 tham số bị từ chối **không lưu**, kèm lý do và vị trí trường sai; 10 tham số được nhận. Không trường hợp nào dẫn tới mã khung không biên dịch được.
- **Nguồn**: `03-dd/validation/F3_harness.md` Bảng 2; P5.

### AC-36 — Ghi đè tên hàm sai khoá hoặc sai giá trị bị từ chối
- **Tiền điều kiện**: A2 nhập ghi đè cho ngôn ngữ không tồn tại (ví dụ `RUBY`); ghi đè Java bằng từ khoá Java hoặc tiền tố `apH`; ghi đè hợp lệ cho Python.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: các ghi đè sai bị từ chối; trường hợp hợp lệ lưu được và mã khung Python dùng tên đó.
- **Nguồn**: `[SoT: 02-bd/database/F2_problem-bank.md:135]`; `03-dd/validation/F3_harness.md` Bảng 2; P5.

### AC-37 — Tổ hợp chiến lược so khớp và kiểu không hợp lệ bị từ chối
- **Tiền điều kiện**: A2 chọn (a) `EPSILON` không nhập sai số, hoặc sai số bằng 0 hoặc âm; (b) `UNORDERED_SET` cho bài trả về cây nhị phân; (c) `UNORDERED_SET` cho bài trả về danh sách liên kết; (d) `EXACT` có lưu `epsilonValue`; (e) `EPSILON` hoặc `UNORDERED_SET` với kiểu trả về nguyên thuỷ `INT`.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: (a)–(c) bị từ chối không lưu, kèm lý do; `EPSILON` với sai số dương và `UNORDERED_SET` với danh sách một chiều lưu được. (d) lưu được nhưng P4 so EXACT, không dùng epsilon. (e) lưu được và P4 so bằng chính xác.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:257-259,300-301]`; `[SoT: 02-bd/database/F2_problem-bank.md:129]`; P5.

### AC-38 — Chưa khai chữ ký chỉ còn Standard I/O; chữ ký nửa vời bị từ chối
- **Tiền điều kiện**: (a) A2 chỉ khai định dạng stdin/stdout, không khai chữ ký; (b) A2 khai tên hàm nhưng bỏ trống kiểu trả về.
- **Hành động**: lưu đặc tả.
- **Kết quả mong đợi**: (a) lưu được, bài chỉ hỗ trợ Standard I/O; (b) bị từ chối, yêu cầu khai đủ.
- **Nguồn**: `03-dd/validation/F3_harness.md` Bảng 2 (`HRN-V07`, `HRN-V17`); hợp đồng đã chốt ở DD F3 mục 4.

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
- **Nguồn**: `[SoT: 02-bd/architecture/F2_problem-bank.md:67]`; `[SoT: 02-bd/database/F3_harness.md:49-73]`; E1.

### AC-41 — Input testcase hỏng làm chương trình báo hiệu riêng, không lộ dữ liệu, không bị tính là lỗi chạy của người học
- **Tiền điều kiện**: chương trình bọc hàm sinh ra nhận input sai định dạng (ví dụ ký tự chữ ở chỗ số, hoặc `true` viết hoa).
- **Hành động**: chạy chương trình.
- **Kết quả mong đợi**: chương trình dừng trước khi gọi hàm của người học, với mã thoát dành riêng cho lỗi đọc input và một dòng báo hiệu cố định; dòng báo hiệu và mọi thông điệp **không chứa** bất kỳ phần nào của input; bên chấm bài nhận biết đây không phải lỗi chạy của học viên.
- **Nguồn**: `[SoT: 02-bd/architecture/F3_harness.md:117-119]`; `03-dd/logic/F3_harness.md` mục 3.5, 8; P2.

### AC-42 — Chương trình sinh ra và gói chấm không chứa dữ liệu testcase
- **Tiền điều kiện**: một bài có testcase mẫu và testcase ẩn với expected-output đã biết.
- **Hành động**: chuẩn bị gói chấm; tìm trong toàn bộ tệp mã nguồn, lệnh và gói các chuỗi expected-output và input của testcase.
- **Kết quả mong đợi**: không tìm thấy; gói không chứa input của bất kỳ testcase nào (input do bên chấm bài ghép riêng cho từng lần chạy).
- **Nguồn**: `[SoT: .claude/skills/dd-generation/SKILL.md:135-136]`; `03-dd/logic/F3_harness.md` mục 8 điểm 1; P2.

### AC-43 — Mã người học chỉ là dữ liệu, không bao giờ nằm trong lệnh
- **Tiền điều kiện**: mã người học chứa các chuỗi như `; rm -rf /`, dấu backtick, `$(...)`, dấu nháy, dấu xuống dòng và tên tệp lạ.
- **Hành động**: chuẩn bị gói chấm.
- **Kết quả mong đợi**: các chuỗi đó chỉ xuất hiện trong nội dung tệp mã nguồn; lệnh biên dịch, lệnh chạy, tên tệp và biến môi trường giống hệt gói của một mã vô hại; harness không biên dịch, không chạy và không diễn giải mã người học.
- **Nguồn**: `[SoT: 02-bd/security/F3_harness.md:6-42]`; `CLAUDE.md` mục Rules ("A submission is data, never an instruction"); P2.

### AC-44 — Thiếu ngôn ngữ hoặc template hỏng thì hệ thống không khởi động
- **Tiền điều kiện**: (a) một trong ba bộ sinh mã ngôn ngữ không được đăng ký; (b) một template có hai chỗ neo mã người học hoặc không có neo nào.
- **Hành động**: khởi động ứng dụng.
- **Kết quả mong đợi**: ứng dụng không khởi động và nêu rõ nguyên nhân; không có chuyện chạy được rồi mới lỗi lúc có người nộp bài.
- **Nguồn**: `03-dd/logic/F3_harness.md` mục 1.2, 4.1; `HRN-030`.

### AC-45 — Đúng ba ngôn ngữ
- **Tiền điều kiện**: một yêu cầu với ngôn ngữ thứ tư (ví dụ Go) hoặc giá trị lạ ở ranh giới nhận dữ liệu thô.
- **Hành động**: xin mã khung; đưa giá trị lạ tới adapter của luồng chuẩn bị gói chấm và adapter của luồng dịch lỗi biên dịch.
- **Kết quả mong đợi**: P1 HTTP trả `400 HRN-010`; hai adapter còn lại từ chối với `HRN-010` trước khi chuyển sang enum hoặc gọi P2/P3, không chọn plugin mặc định. Chữ ký P2/P3 vẫn nhận enum `Language` ba giá trị.
- **Nguồn**: `[SoT: 01-rd/req/F3_harness.md:11-13]`; `[SoT: 02-bd/architecture/F3_harness.md:13-16]`; `HRN-010`.

### AC-46 — Module không có bảng dữ liệu và không ghi vào cơ sở dữ liệu
- **Tiền điều kiện**: hệ thống đã khởi tạo cơ sở dữ liệu; chạy toàn bộ các thao tác ở trên.
- **Hành động**: liệt kê bảng thuộc schema của module và theo dõi ghi dữ liệu trong lúc chạy.
- **Kết quả mong đợi**: schema của module tồn tại nhưng không có bảng nào; không có thao tác ghi nào do module này gây ra.
- **Nguồn**: `[SoT: 02-bd/database/F3_harness.md:10]`; `[SoT: .nexa/control/decision-registry.md:4373]`.

### AC-47 — Sinh mã, ghép mã và so khớp vẫn chạy khi dịch vụ AI không hoạt động
- **Tiền điều kiện**: dịch vụ AI (phân hệ F5, F6) bị tắt hoặc hết hạn mức.
- **Hành động**: thực hiện AC-01, AC-06, AC-14 và AC-23.
- **Kết quả mong đợi**: tất cả vẫn cho kết quả đúng như khi dịch vụ AI hoạt động; module không gọi và không phụ thuộc đường AI.
- **Nguồn**: `CLAUDE.md` mục Rules ("The AI subsystem degrades gracefully"); `03-dd/logic/F3_harness.md` mục 8 điểm 3.

### AC-48 — Mã khung của bài chưa xuất bản chỉ dành cho người có quyền

- **Tiền điều kiện**: một bài chưa xuất bản có tác giả A; tài khoản B không phải tác giả; quản trị viên C; một bài khác đã xuất bản; một bài đã xoá.
- **Hành động**: A, B, C lần lượt xin mã khung của bài chưa xuất bản; B xin mã khung của bài đã xuất bản; A và B xin mã khung của bài đã xoá.
- **Kết quả mong đợi**: A và C nhận được mã khung của bài chưa xuất bản; B không nhận được và không biết bài đó có tồn tại hay không. B nhận được mã khung của bài đã xuất bản. Bài đã xoá trả `404 HRN-001` như ID không tồn tại cho cả A và B; không lộ chữ ký hoặc mã khung cũ.
- **Nguồn**: `[SoT: 03-dd/api/F3_harness.md]` mục 3.1; quyết định chủ dự án 2026-10-25.

### AC-49 — Xem trước chữ ký hàm từ bản nháp chưa lưu

- **Tiền điều kiện**: người soạn đang biên soạn một bài dùng Bọc hàm, đã nhập tên hàm và các kiểu tham số nhưng chưa lưu.
- **Hành động**: xem trước chữ ký, đổi một kiểu rồi xem trước lại; thử bản nháp thiếu chữ ký và bản nháp sai kiểu.
- **Kết quả mong đợi**: ba dòng khai báo cho Java, C++ và Python đổi theo bản nháp mới, khớp với mã khung sau khi lưu. Bản nháp thiếu chữ ký không hiện dòng khai báo; bản nháp sai được báo lỗi để sửa. Body JSON UTF-8 vượt 16.384 byte trả `413 HRN-062` trước khi gọi P5. Xem trước không lưu thay đổi bài.
- **Nguồn**: `[SoT: 03-dd/api/F3_harness.md]` mục 3.7; `[SoT: 03-dd/logic/F3_harness.md]` mục xem trước chữ ký.

### AC-50 — Mảng nhiều chiều đọc kích thước từ dòng đầu

- **Tiền điều kiện**: bài Bọc hàm có tham số là mảng hai chiều.
- **Hành động**: chạy với dòng đầu `2 3` rồi hai hàng, mỗi hàng ba phần tử; chạy lại khi thiếu một hàng hoặc một phần tử.
- **Kết quả mong đợi**: dữ liệu đủ được đọc thành mảng hai hàng, ba cột. Dữ liệu thiếu bị báo đầu vào testcase không hợp lệ; hệ thống không tự suy số cột từ hàng đầu tiên.
- **Nguồn**: `[SoT: 03-dd/logic/F3_harness.md]` Q8 và mục định dạng đầu vào.

## 3. Truy vết

Bảng 1: Truy vết tiêu chí tới cổng và khối quy tắc. Cột "Test case" điền khi có test thật theo `testcase-generation`.

| AC | Cổng / luồng | Khối quy tắc | Test viewpoint ở DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| AC-01 | P1 `getStarterCode` | BR-01, BR-05 | P1-T01, P1-T03 | [TC-F3-AC01-01 đến -02](../testcases/modules/F3_harness/F3_starter_code.md) | chưa |
| AC-02 | P1 | BR-01 | P1-T07 | [TC-F3-AC02-01](../testcases/modules/F3_harness/F3_starter_code.md) | chưa |
| AC-03 | P1 | BR-04 | P1-T02 | [TC-F3-AC03-01](../testcases/modules/F3_harness/F3_starter_code.md) | chưa |
| AC-04 | P1, P2 | BR-05 | P1-T04, P2-T03 | [TC-F3-AC04-01 đến -02](../testcases/modules/F3_harness/F3_starter_code.md), [TC-F3-AC04-03](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-05 | P1 | BR-05 | P1-T05, P1-T06 | [TC-F3-AC05-01 đến -02](../testcases/modules/F3_harness/F3_starter_code.md) | chưa |
| AC-06 | P2 `prepare` | BR-03 | P2-T01 | [TC-F3-AC06-01 đến -02 (ca -02 chờ golden)](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-07 | P2 | BR-03 | P2-T02 | [TC-F3-AC07-01](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-08 | P2 | BR-02 | P2-T07 | [TC-F3-AC08-01–02](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-09 | P2 | BR-03 | P2-T08 | [TC-F3-AC09-01–03](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-10 | P2 | validation mục 5 | P2-T05, P2-T06 | [TC-F3-AC10-01–04](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-11 | P2 | BR-09 | P2-T04 | [TC-F3-AC11-01–02](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-12 | P2 | BR-04 | P2-T01 | [TC-F3-AC12-01](../testcases/modules/F3_harness/F3_prepare_source_and_version.md) | chưa |
| AC-13 | P2 (tầng chuỗi công cụ) | mục 3.3, 3.4, 10.3 | P2-T01 | [TC-F3-AC13-01–07](../testcases/modules/F3_harness/F3_type_roundtrip.md) | chưa |
| AC-14 | P3 `map` (Java) | BR-07 | mục 6.1 api | [TC-F3-AC14-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-15 | P3 (C++) | BR-07 | mục 6.1 api | [TC-F3-AC15-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-16 | P3 (Python) | BR-07 | mục 6.1 api | [TC-F3-AC16-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-17 | P3 | BR-07 | mục 6.1 api | [TC-F3-AC17-01–03](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-18 | P3 | BR-07 | mục 6.1 api | [TC-F3-AC18-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-19 | P3 | BR-07 | mục 6.1 api | [TC-F3-AC19-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-20 | P3 | BR-07 | mục 6.1 api | [TC-F3-AC20-01–02](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-21 | P3 (Standard I/O) | BR-07 | mục 6.1 api | [TC-F3-AC21-01–02](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-22 | P3 (C++ mẫu) | BR-07 | mục 6.1 api | [TC-F3-AC22-01](../testcases/modules/F3_harness/F3_compile_error_mapping.md) | chưa |
| AC-23 | P4 `match` (`EXACT`) | BR-08 | mục 6.2 api | [TC-F3-AC23-01 đến -04](../testcases/modules/F3_harness/F3_output_matching.md) | chưa |
| AC-24 | P4 (`TRIMMED`) | BR-08 | mục 6.2 api | [TC-F3-AC24-01 đến -05](../testcases/modules/F3_harness/F3_output_matching.md) | chưa |
| AC-25 | P4 (`EPSILON` biên) | BR-08 | mục 6.2 api | [TC-F3-AC25-01 đến -03](../testcases/modules/F3_harness/F3_output_matching.md) | chưa |
| AC-26 | P4 (`EPSILON` lồng, Standard I/O) | BR-08 | mục 6.2 api | [TC-F3-AC26-01 đến -07](../testcases/modules/F3_harness/F3_output_matching.md) | chưa |
| AC-27 | P4 (`UNORDERED_SET`) | BR-08 | mục 6.2 api | [TC-F3-AC27-01 đến -04](../testcases/modules/F3_harness/F3_harness_contracts.md) | chưa |
| AC-28 | P4 (cây) | BR-08 | mục 6.2 api | [TC-F3-AC28-01 đến -02](../testcases/modules/F3_harness/F3_output_matching_edge_cases.md) | chưa |
| AC-29 | P4 (dòng đánh dấu) | BR-06 | mục 6.2 api | [TC-F3-AC29-01 đến -03](../testcases/modules/F3_harness/F3_output_matching_edge_cases.md) | chưa |
| AC-30 | P4 (không lộ dữ liệu) | BR-08 | mục 6.2 api | [TC-F3-AC30-01 đến -05](../testcases/modules/F3_harness/F3_output_matching_edge_cases.md) | chưa |
| AC-31 | P5 `evaluate` | validation mục 2 | mục 6.3 api | [TC-F3-AC31-01 đến -04](../testcases/modules/F3_harness/F3_spec_type_validation.md) | chưa |
| AC-32 | P5 | validation mục 2 | mục 6.3 api | [TC-F3-AC32-01 đến -05](../testcases/modules/F3_harness/F3_spec_type_validation.md) | chưa |
| AC-33 | P5 | validation Bảng 1 | mục 6.3 api | [TC-F3-AC33-01 đến -02](../testcases/modules/F3_harness/F3_spec_type_validation.md) | chưa |
| AC-34 | P5 | validation mục 2 | mục 6.3 api | [TC-F3-AC34-01](../testcases/modules/F3_harness/F3_spec_type_validation.md) | chưa |
| AC-35 | P5 | validation Bảng 2 | mục 6.3 api | [TC-F3-AC35-01 đến -09](../testcases/modules/F3_harness/F3_spec_semantic_validation.md) | chưa |
| AC-36 | P5 | validation Bảng 2 | mục 6.3 api | [TC-F3-AC36-01 đến -04](../testcases/modules/F3_harness/F3_spec_semantic_validation.md) | chưa |
| AC-37 | P5 | validation Bảng 3 | mục 6.3 api | [TC-F3-AC37-01 đến -08](../testcases/modules/F3_harness/F3_spec_semantic_validation.md) | chưa |
| AC-38 | P5 | validation Bảng 2 | mục 6.3 api | [TC-F3-AC38-01 đến -02](../testcases/modules/F3_harness/F3_spec_semantic_validation.md) | chưa |
| AC-39 | O1 `findSpec` | BR-09 | O1-T03, P2-T09 | [TC-F3-AC39-01 đến -02](../testcases/modules/F3_harness/F3_spec_read_invalidation.md) | chưa |
| AC-40 | E1 `ProblemSpecChanged` | BR-09 | E1-T01 đến E1-T04 | [TC-F3-AC40-01 đến -04](../testcases/modules/F3_harness/F3_spec_read_invalidation.md) | chưa |
| AC-41 | P2 (chương trình sinh ra) | mục 3.5 logic | P2-T01 | [TC-F3-AC41-01 đến -02](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-42 | P2 | mục 8 logic | P2-T01 | [TC-F3-AC42-01](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-43 | P2 | BR-02, BR-06 | P2-T07 | [TC-F3-AC43-01](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-44 | khởi động | BR-02 | (không có ở DD) | [TC-F3-AC44-01 đến -03](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-45 | P1, adapter P2/P3 | API §1.2, validation Bảng 4 | `HRN-010` | [TC-F3-AC45-01 đến -03](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-46 | schema | `02-bd/database/F3_harness.md` mục 1 | (không có ở DD) | [TC-F3-AC46-01](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-47 | P1, P2, P3, P4 | mục 8 logic | (không có ở DD) | [TC-F3-AC47-01](../testcases/modules/F3_harness/F3_generated_program_safety.md) | chưa |
| AC-48 | P1, O2, GET mã khung | mục 3.1 API | P1-T08 | [TC-F3-AC48-01 đến -04](../testcases/modules/F3_harness/F3_harness_contracts.md) | chưa |
| AC-49 | P6, POST xem trước chữ ký | mục 3.7 API | `HRN-062` | [TC-F3-AC49-01 đến -08](../testcases/modules/F3_harness/F3_harness_contracts.md) | chưa |
| AC-50 | P2, P3 | Q8 logic | | [TC-F3-AC50-01 đến -03](../testcases/modules/F3_harness/F3_harness_contracts.md) | chưa |

Mỗi cổng có ít nhất một tiêu chí: P1 (AC-01 đến AC-05), P2 (AC-06 đến AC-13), P3 (AC-14 đến AC-22), P4 (AC-23 đến
AC-30), P5 (AC-31 đến AC-38), O1 (AC-39), E1 (AC-40). Số hiệu P, O, E theo `03-dd/api/F3_harness.md` Bảng 1.

## 4. Chưa chốt

Không còn quyết định chủ dự án đang chờ. Các Q2, Q3, Q4, Q8, Q12–Q14 và Q16 của DD F3 đã đóng; tiêu chí mới phải bám theo hợp đồng DD hiện hành.

## 5. Chưa phủ được

| Điều không nghiệm thu được ở đây | Vì sao | Cách phủ sau |
| :--- | :--- | :--- |
| Khớp thật giữa mẫu lỗi biên dịch trong test và đầu ra của trình biên dịch trên image go-judge | Image chưa build (`01-rd/system/SYS0401_judge_engine.md:283-295`); regex phụ thuộc phiên bản | Tầng "chuỗi công cụ" của `03-dd/logic/F3_harness.md` mục 10.3 chụp lại mẫu thật khi image sẵn sàng |
| Hành vi khi hai module chạy cùng nhau trong môi trường thật (hàng đợi, giới hạn tài nguyên) | Thuộc giai đoạn xây dựng 2 (`.nexa/control/dependency-map.md`) | Test tích hợp cùng `judge-orchestration` |
| Hiệu năng sinh mã dưới tải | Cache trong tiến trình mặc định tắt, chưa đo | Chỉ đo và thêm tiêu chí khi cần bật cache |
| Thông điệp lỗi lúc chạy (ngoại lệ trong hàm người học) có lộ mã khung không | F3 chỉ xử lý lỗi biên dịch; F4 không lưu/trả stderr của `FUNCTION_WRAPPER` | Kiểm ở F4 AC-76 theo Q14/D5 đã chốt của DD F3 |
