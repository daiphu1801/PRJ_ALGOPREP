# TDD màn `USR0105` — Trang đầu Workspace

> DD: `03-dd/screens/users/USR0105_workspace_home.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Danh sách đang làm dở và điều hướng

- **Tiền điều kiện:** F4 có hơn 12 bài đang làm dở và một bài không có metadata do F2 lỗi.
- **Hành động:** mở Workspace Home, chọn xem danh mục và một thẻ đang làm dở.
- **Kết quả mong đợi:** màn gọi F4 #8 với `limit=12`, tối đa 12 thẻ hợp lệ; thẻ thiếu metadata không dựng link; xem danh mục tới `/problems`, chọn thẻ tới `/workspace/{problemId}`.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0105_workspace_home.md] mục 3, 7.

### AC-SC-02 — Lỗi và rỗng

- **Tiền điều kiện:** F4 lần lượt trả danh sách rỗng và lỗi mạng.
- **Hành động:** mở màn rồi bấm Thử lại sau lỗi.
- **Kết quả mong đợi:** rỗng hiện câu hướng dẫn; lỗi có vùng retry, không hiện danh sách mock cũ; retry gọi lại #8 và thay vùng lỗi khi thành công.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0105_workspace_home.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0105_workspace_home.md) mục 3, 7 | [TC-USR0105-SCAC01-01](../../testcases/screens/users/USR0105_workspace_home.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0105_workspace_home.md) mục 3, 7 | [TC-USR0105-SCAC02-01](../../testcases/screens/users/USR0105_workspace_home.md) | Chưa thực hiện |
