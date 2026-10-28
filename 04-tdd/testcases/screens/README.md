# Testcase QA theo màn hình — đợt 4

Các nhóm dưới đây là **tài liệu thiết kế**, chưa có ca nào được thực thi. TDD từng màn đã có link tới testcase; kết quả FE mock và tích hợp sẽ ghi run record riêng khi có build và môi trường.

| Nhóm | Màn | AC có trace | Ca thiết kế | Mục lục |
| :--- | ---: | ---: | ---: | :--- |
| Shared | 7 | 24/24 | 53 | [Shared](shared/README.md) |
| Users | 16 | 82/82 | 95 | [Users](users/README.md) |
| Teacher | 7 | 17/17 | 28 | [Teacher](teacher/README.md) |
| Admin | 10 | 29/29 | 36 | [Admin](admin/README.md) |
| **Tổng** | **40** | **152/152** | **212** | |

Đã đóng review thiết kế 2026-10-28 theo [inventory 40 màn](AUDIT_INVENTORY.md) và [biên bản đợt 4](../AUDIT_DOT4_SCREENS.md); 212 ca/212 trace, không lỗi cấu trúc. [Quy ước fixture](FIXTURE_CONVENTIONS.md) dùng khi materialize seed và fault controls sau coding. Chưa thực thi; FE mock không chứng minh quyền hoặc giao dịch backend.
