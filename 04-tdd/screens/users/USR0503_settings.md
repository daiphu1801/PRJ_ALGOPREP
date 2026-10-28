# TDD màn `USR0503` — Thiết lập

> DD: `03-dd/screens/users/USR0503_settings.md` · Cập nhật: 2026-10-26.

Các AC này chỉ kiểm phối hợp F1, F4 và lưu trên thiết bị. Nghiệp vụ API dùng `04-tdd/modules/F1_identity.md` AC-44, AC-48 và `04-tdd/modules/F4_judge-orchestration.md` AC-72.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Danh sách ngôn ngữ bật giới hạn lựa chọn mặc định

- **Tiền điều kiện:** F1 #14 trả `defaultLanguage=JAVA`; F4 #24 chỉ trả PYTHON và CPP vì JAVA vừa bị tắt.
- **Hành động:** mở `/settings`, thử lưu mà chưa chọn lại, sau đó chọn CPP và lưu.
- **Kết quả mong đợi:** giá trị JAVA cũ được hiển thị với thông báo cần chọn lại, không bị mất âm thầm; Select chỉ cho PYTHON, CPP; lưu CPP qua F1 #15 thành công. Khi F4 lỗi, Select khoá nhưng các nhóm cài đặt khác vẫn dùng được.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0503_settings.md] mục 3, 7.

### AC-SC-02 — Lưu thiết bị và tài khoản giữ dirty theo nguồn

- **Tiền điều kiện:** thay cỡ chữ và `defaultLanguage`; F1 #15 lỗi, lưu cục bộ thành công.
- **Hành động:** bấm Lưu rồi dùng “Lưu và thoát”.
- **Kết quả mong đợi:** cỡ chữ đã lưu không còn dirty; `defaultLanguage` vẫn dirty, không rời trang khi F1 lỗi; thử lại F1 thành công mới xoá dirty tài khoản và điều hướng. Không gọi endpoint preferences cũ của F5.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0503_settings.md] mục 3, 7.

### AC-SC-03 — Xuất dữ liệu có hạn mức và không khoá loại tệp khác

- **Tiền điều kiện:** người học có dữ liệu bài nộp và transcript; một loại tệp vừa được xuất trong thời gian giới hạn.
- **Hành động:** xuất lại cùng loại, thử xuất loại còn lại, rồi thử lại sau thời gian chờ.
- **Kết quả mong đợi:** lần xuất quá sớm được báo thời gian thử lại; nút của cùng loại khoá khi đang tải, loại khác vẫn dùng được. Tệp trả về được tải xuống, không in dữ liệu nhạy cảm lên trang. Hết thời gian chờ thì xuất lại được.
- **Nguồn:** [SoT: 03-dd/screens/users/USR0503_settings.md] BR-SC-02, SC-04, SC-05.

### AC-SC-04 — Lỗi xuất transcript và lỗi tải cài đặt phục hồi riêng

- **Tiền điều kiện:** nguồn transcript hoặc nguồn cài đặt tạm lỗi.
- **Hành động:** bấm Xuất transcript, sau đó bấm Thử lại ở phần cài đặt.
- **Kết quả mong đợi:** lỗi xuất transcript không khoá nút vĩnh viễn; người học có thể thử lại. Lỗi tải cài đặt chỉ ảnh hưởng phần tương ứng, và Thử lại chỉ tải lại phần đã lỗi, không ghi đè các thay đổi chưa lưu.
- **Nguồn:** [SoT: 03-dd/screens/users/USR0503_settings.md] mục 3, SC-09.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0503_settings.md) mục 3, 7 | [TC-USR0503-SCAC01-01](../../testcases/screens/users/USR0503_settings.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0503_settings.md) mục 3, 7 | [TC-USR0503-SCAC02-01](../../testcases/screens/users/USR0503_settings.md) | Chưa thực hiện |
| AC-SC-03 | BR-SC-02; điểm kiểm DD: SC-04, SC-05 | [TC-USR0503-SCAC03-01](../../testcases/screens/users/USR0503_settings.md) | Chưa thực hiện |
| AC-SC-04 | Mục 3, EVT-9/15; điểm kiểm DD: SC-09 | [TC-USR0503-SCAC04-01](../../testcases/screens/users/USR0503_settings.md) | Chưa thực hiện |
