# Tiêu chí nghiệm thu — Màn SHR0101 — Xác thực

> DD: `03-dd/screens/shared/SHR0101_auth.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AUTH-AC-01 — Đăng ký và xác thực email trước khi vào hệ thống

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người mới có email chưa dùng. |
| **Hành động** | Đăng ký, thử mật khẩu sai quy tắc, rồi đăng nhập trước khi xác thực email. |
| **Kết quả mong đợi** | Đăng ký hợp lệ chuyển sang bước nhập mã email; không tự tạo phiên. Dữ liệu sai bị chỉ rõ; tài khoản chưa xác thực được hướng dẫn hoàn tất xác thực, không tạo phiên đăng nhập. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0101_auth.md] mục 3, 7 |

### AUTH-AC-02 — Đăng nhập và đường trở lại an toàn

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có tài khoản hợp lệ và một đường trở lại đã chọn. |
| **Hành động** | Đăng nhập thường hoặc liên kết, thử đường ngoài hệ thống và đường thuộc vai trò khác. |
| **Kết quả mong đợi** | Đăng nhập thành công đi đúng khu hoặc đường nội bộ có quyền. Đường không an toàn bị bỏ; lỗi xác thực hoặc gọi lại sai không giữ phiên giả. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0101_auth.md] mục 3, 7 |

### AUTH-AC-03 — Quên mật khẩu xác minh mã trước khi đặt lại

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng yêu cầu đặt lại mật khẩu bằng email. |
| **Hành động** | Gửi yêu cầu với email có hoặc không có tài khoản, nhập mã sai hoặc hết hạn, gửi lại rồi đặt mật khẩu mới. |
| **Kết quả mong đợi** | Hai loại email cho cùng thông báo; mã được kiểm trước khi hiện bước mật khẩu mới. Gửi lại chịu thời gian chờ; đặt lại thành công trở về Đăng nhập và không tự tạo phiên. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0101_auth.md] mục 3, 7 |

### AUTH-AC-04 — Huỷ tự xoá yêu cầu đăng nhập lại

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Tài khoản tự xoá vẫn trong thời gian ân hạn; người dùng xác minh lại thông tin đăng nhập. |
| **Hành động** | Chọn Huỷ yêu cầu xoá rồi thử dùng lại vé khôi phục. |
| **Kết quả mong đợi** | Tài khoản được khôi phục một lần; người dùng được mời đăng nhập lại. Vé cũ không thể dùng tiếp và thao tác khôi phục không tạo phiên. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0101_auth.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AUTH-AC-01 | Mục 3, 7; điểm kiểm DD: SH-01–03 | [TC-SHR0101-AUTHAC01-01](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC01-02](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC01-03](../../testcases/screens/shared/SHR0101_auth.md) | Chưa thực hiện |
| AUTH-AC-02 | Mục 3, 7; điểm kiểm DD: SH-04–05, SH-09 | [TC-SHR0101-AUTHAC02-01](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC02-02](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC02-03](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC02-04](../../testcases/screens/shared/SHR0101_auth.md) | Chưa thực hiện |
| AUTH-AC-03 | Mục 3, 7; điểm kiểm DD: SH-06–08 | [TC-SHR0101-AUTHAC03-01](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC03-02](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC03-03](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC03-04](../../testcases/screens/shared/SHR0101_auth.md) | Chưa thực hiện |
| AUTH-AC-04 | Mục 3, 7; điểm kiểm DD: SH-10 | [TC-SHR0101-AUTHAC04-01](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC04-02](../../testcases/screens/shared/SHR0101_auth.md), [TC-SHR0101-AUTHAC04-03](../../testcases/screens/shared/SHR0101_auth.md) | Chưa thực hiện |

