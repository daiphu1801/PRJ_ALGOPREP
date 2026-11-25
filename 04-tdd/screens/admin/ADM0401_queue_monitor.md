# Tiêu chí nghiệm thu — Màn ADM0401 — Giám sát hàng đợi

> DD: `03-dd/screens/admin/ADM0401_queue_monitor.md` · Cập nhật: 2026-11-22 (thêm QM-AC-04 đến QM-AC-07 cho khối Luồng chấm; trạng thái: đã dựng, test Vitest `grading-card.test.tsx` và `real-queue.test.ts` đạt; chưa chạy Playwright trên màn thật).

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### QM-AC-01 — Trạng thái hàng đợi, việc và cấu hình tải riêng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở màn giám sát; một nguồn số liệu tạm lỗi. |
| **Hành động** | Làm mới trang, đổi tab và đổi trang danh sách việc. |
| **Kết quả mong đợi** | Khối còn dữ liệu vẫn hiện; lỗi một khối không che khối khác. Đổi tab hoặc trang không giữ việc của tab cũ. Ở chế độ dữ liệu thật màn không dựng bảng trạng thái từng worker, mức tải, thời gian chạy hay trạng thái quá tải worker (không có nguồn); khối "Luồng chấm" được kiểm riêng ở QM-AC-04 đến QM-AC-07. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

### QM-AC-02 — Công tắc vận hành chỉ đổi khi lưu thành công

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hàng đợi đang chạy và tự mở rộng đang tắt. |
| **Hành động** | Bấm tạm dừng hoặc bật tự mở rộng, rồi thử khi máy chủ từ chối. |
| **Kết quả mong đợi** | Trạng thái trên màn chỉ đổi khi thao tác được chấp nhận; lỗi không gửi lặp và không báo thành công. Bật tự mở rộng làm năng lực nhận việc thay đổi theo tồn đọng, theo tiêu chí F4. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

### QM-AC-03 — Tham số vận hành và hộp thoại

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở cài đặt hàng đợi. |
| **Hành động** | Nhập các tham số, lưu, hủy, rồi mở Sự kiện hạ tầng. |
| **Kết quả mong đợi** | Chỉ số nguyên dương được gửi; hộp thoại lưu hoặc hủy rõ ràng và trả focus đúng chỗ. Lối Sự kiện hạ tầng mở đúng màn. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

### QM-AC-04 — Luồng chấm: số luồng đang chạy và lần đổi gần nhất

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở màn giám sát ở chế độ dữ liệu thật. Worker chấm bài đang chạy tự động mở rộng, mức tối đa là 8, và vừa tăng từ 2 lên 3 luồng vì hàng đợi dồn (31 bài đang chờ lúc đó) |
| **Hành động** | Đọc khối "Luồng chấm"; sau đó chờ hàng đợi rỗng đủ lâu để worker giảm từ 3 xuống 2; rồi tắt tự động mở rộng và chờ worker về 1 luồng |
| **Kết quả mong đợi** | Khối hiện "3 / 8", mục tiêu 3 và dòng lần đổi gần nhất nêu giờ, "tăng từ 2 lên 3", lý do "Hàng đợi dồn" và 31 bài chờ. Sau khi giảm, dòng đó đổi sang "giảm từ 3 xuống 2", lý do "Hàng đợi trống". Sau khi tắt tự động mở rộng, lý do là "Tắt tự động mở rộng" và số đang chạy về 1 khi các luồng thừa chấm xong (số đang chạy có thể còn lớn hơn mục tiêu trong lúc chờ). Worker chưa từng đổi số luồng thì dòng đó ghi "Chưa có lần đổi nào". Màn không hiện mức tải %, thời gian chạy hay số vCPU |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] BR-SC-03, QMON-08, QMON-09, QMON-11; [SoT: 03-dd/api/F4_judge-orchestration.md] mục 16.2 |

### QM-AC-05 — Luồng chấm: worker không báo thì không hiện số cũ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Màn đang hiện khối "Luồng chấm" với 3 luồng đang chạy; sau đó worker chấm bài dừng (hoặc chưa từng khởi động) và không báo trong hơn 60 giây trong khi máy chủ API và hàng đợi vẫn chạy |
| **Hành động** | Chờ quá 60 giây rồi đọc lại khối; sau đó khởi động lại worker và chờ vài chu kỳ |
| **Kết quả mong đợi** | Phần số luồng, mục tiêu và lần đổi gần nhất biến mất và được thay bằng "Chưa nhận được báo cáo từ worker chấm bài"; màn không giữ "3" cũ và không hiện "0 / 8". Tốc độ chấm và dự kiến hết hàng đợi vẫn hiện. Dải trạng thái không báo "Dữ liệu cũ" nếu màn vẫn nhận lô đều. Khi worker báo lại, khối trở về hiển thị số thật mà không cần tải lại trang. Máy chủ chưa từng có báo cáo nào (hệ thống mới) hiển thị giống trường hợp trên |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] BR-SC-04, QMON-10; [SoT: 03-dd/api/F4_judge-orchestration.md] mục 16.2 (`consumers = null`) |

### QM-AC-06 — Tốc độ chấm và dự kiến hết hàng đợi

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở màn ở chế độ dữ liệu thật. Trong 60 giây gần nhất hệ thống chấm xong 96 bài và đang có 24 bài chờ. Các tình huống khác: (a) phút qua chỉ có 2 bài chấm xong; (b) không có bài nào chờ; (c) hàng đợi đang tạm dừng; (d) 5000 bài chờ, tốc độ 3 bài mỗi phút |
| **Hành động** | Đọc khối "Luồng chấm" trong từng tình huống |
| **Kết quả mong đợi** | Với 96 bài và 24 bài chờ, khối hiện tốc độ "96 lượt/phút" và dự kiến hết hàng đợi khoảng 15 giây. (a), (b) và (c): tốc độ chấm hiện số thật (2, số thật, 0) còn dự kiến hết hàng đợi hiện "Chưa đủ dữ liệu", không hiện 0. (d): dự kiến không vượt 1 giờ. Số liệu đổi tại chỗ khi làm mới thủ công hoặc khi có lô tự động, không nhấp nháy |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] BR-SC-03, QMON-11, QMON-12; [SoT: 03-dd/api/F4_judge-orchestration.md] mục 16.2, T-QS-09 đến T-QS-12 |

### QM-AC-07 — Dữ liệu mẫu giữ danh sách worker; dữ liệu thật không hiện số liệu giả và không bấm dòng job

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Cùng một màn chạy lần lượt ở chế độ dữ liệu mẫu và chế độ dữ liệu thật |
| **Hành động** | Mở màn ở từng chế độ; ở chế độ dữ liệu thật bấm vào một dòng của bảng job |
| **Kết quả mong đợi** | Chế độ mẫu hiện danh sách worker minh hoạ (mã, trạng thái, mức tải, thời gian chạy, số vCPU) và không có khối "Luồng chấm". Chế độ thật hiện khối "Luồng chấm" và không có danh sách worker, mức tải, thời gian chạy hay số vCPU. Bấm một dòng job không mở chi tiết nào và không có lỗi; dòng job chỉ để đọc. Hai chế độ không bao giờ hiện cả hai khối cùng lúc |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] BR-SC-03, BR-SC-05, QMON-07, QMON-13 |

### QM-AC-08 — Bố cục gọn và phản hồi cập nhật

| Mục | Nội dung |
| --- | --- |
| **Điều kiện trước** | Admin mở Hàng đợi chấm trên màn rộng hoặc màn hẹp. Có thể mô phỏng trạng thái rỗng, dồn hàng đợi và mất một nguồn dữ liệu. |
| **Thao tác** | Quan sát từ đầu màn, chọn bộ lọc khác, mở Điều khiển bằng bàn phím, bấm Làm mới khi một nguồn dữ liệu lỗi. |
| **Kết quả mong đợi** | Dải trạng thái và số liệu vận hành nằm trước bảng. Số bài chờ hiện trong bộ lọc bảng; danh sách rỗng có thông báo theo bộ lọc. Điều khiển đóng mặc định và mở được bằng bàn phím. Trong lúc làm mới, nút bị khóa. Khi một nguồn lỗi, các phần tải thành công vẫn xem được và màn báo cập nhật một phần. |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| QM-AC-01 | Mục 3, 7; điểm kiểm DD: QMON-01, QMON-03 | [TC-ADM0401-QM-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0401-QM-AC-01-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| QM-AC-02 | Mục 3, 7; điểm kiểm DD: QMON-02 | [TC-ADM0401-QM-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| QM-AC-03 | Mục 3, 7; điểm kiểm DD: QMON-04–06 | [TC-ADM0401-QM-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0401-QM-AC-03-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| QM-AC-04 | BR-SC-03; điểm kiểm DD: QMON-08, QMON-09, QMON-11 | Chưa có test case (viết sau bằng `testcase-generation`) | Chưa dựng |
| QM-AC-05 | BR-SC-04; điểm kiểm DD: QMON-10 | Chưa có test case | Chưa dựng |
| QM-AC-06 | BR-SC-03; điểm kiểm DD: QMON-11, QMON-12 | Chưa có test case | Chưa dựng |
| QM-AC-07 | BR-SC-03, BR-SC-05; điểm kiểm DD: QMON-07, QMON-13 | Chưa có test case | Chưa dựng |

