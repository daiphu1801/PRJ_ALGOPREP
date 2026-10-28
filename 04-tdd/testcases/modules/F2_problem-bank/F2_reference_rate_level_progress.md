# Test case QA — F2: hạn mức chạy đáp án và tiến độ theo độ khó

> 4 ca thiết kế cho TDD F2 AC-91–92; DD API §48, §14A, Q-31. Chưa thực thi. Cố định clock T0, cô lập Redis theo tài khoản.

### TC-F2-AC91-01 — Lượt thứ 11 bị chặn, cửa sổ trượt mở lại
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-91; DD §48, Q-31 |
| Kind / priority | Boundary, P0 |
| Environment | Reference-run API, Redis, sandbox, clock |
| Preconditions | A có đúng 10 lần chạy được nhận tại T0-59s đến T0-1s, X có reference và testcase; B không có lượt. |
| Test data | A chạy reference của X ở T0, B chạy cùng X ở T0; dịch tới T0+2s để mốc T0-59s hết cửa sổ 60 giây rồi A chạy lại. Redis seed 10 mốc A: T0-59s, T0-9s..T0-1s. |
| Steps | A chạy lần 11 tại T0, B chạy, dịch clock tới sau khi mốc cũ nhất hết hạn rồi A chạy lại. |
| Expected | A lần 11 `429 PRB-055` có thời gian thử lại; B chạy được vì quota riêng; A chạy được sau cửa sổ, không vượt 10 mốc còn hiệu lực. Không thay đổi quota AI của X. |
| Cleanup | Dọn Redis/fixture |
| Evidence | ba response, timestamp mốc. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC91-02 — Lỗi hạ tầng hoàn mốc, quota AI độc lập
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-91; DD §48, Q-31 |
| Kind / priority | Recovery, P0 |
| Environment | Reference-run API, sandbox fault injection, Redis, DB |
| Preconditions | A có 9 mốc trong cửa sổ, X AI used=1/limit=2. |
| Test data | Redis seed 9 mốc A tại T0-9s..T0-1s; request chạy reference của X tại T0 khi go-judge lỗi, lặp cùng request sau phục hồi; request sinh AI cho X tạo một ứng viên hợp lệ. |
| Steps | Tắt go-judge, chạy reference; khôi phục, chạy lại; sinh testcase AI hợp lệ. |
| Expected | Lỗi `503 PRB-050` và mốc lỗi được hoàn; lần sau chạy được là mốc thứ 10; sinh AI dùng quota AI riêng, reference-run mốc vẫn 10 và AI used tăng theo quy tắc added>0. |
| Cleanup | Khôi phục sandbox |
| Evidence | responses, Redis, AI used diff. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC92-01 — Mẫu số chỉ gồm bài published, mức trống vẫn hiện
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-92; DD §14A |
| Kind / priority | Data aggregation, P0 |
| Environment | Dashboard API, problem DB, solved-status port |
| Preconditions | Mức Easy: P1/P2 published, W withdrawn; S giải P1 và W. Mức Medium: P3 published, S chưa giải. Mức Hard mới: không bài. |
| Test data | Token S; bài P1/P2 ở Easy, W ở Easy nhưng `UNPUBLISHED`, P3 ở Medium, Hard không có bài; solved-status port trả `[P1,W]` cho S. Gọi `GET /me/levels/progress` theo DD §14A. |
| Steps | GET tiến độ theo độ khó của S; đối chiếu từng mức. |
| Expected | Easy total=2, solved=1; Medium total=1, solved=0; Hard total=0, solved=0 và vẫn có dòng. W không trong cả tử/mẫu. |
| Cleanup | Dọn fixture |
| Evidence | response và bảng bài/trạng thái giải. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC92-02 — Nguồn trạng thái giải lỗi không thành số không
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-92; DD §14A |
| Kind / priority | Recovery, P0 |
| Environment | Dashboard API, solved-status port fault injection |
| Preconditions | Cùng fixture TC-F2-AC92-01; cổng trạng thái giải unavailable. |
| Test data | Cùng token S và bốn bài P1/P2/W/P3 của TC-F2-AC92-01; lần đầu solved-status port ném timeout, lần sau trả `[P1,W]`. |
| Steps | GET tiến độ theo độ khó của S khi port lỗi; khôi phục port rồi GET lại. |
| Expected | Lần lỗi `503` theo DD §14A.3, không trả payload tiến độ hay số solved=0 giả cho Easy/Medium/Hard. Sau khi port phục hồi, GET trả lại các số của TC-F2-AC92-01. |
| Cleanup | Khôi phục port |
| Evidence | response, fault trace. |
| Design review | Đã review; chưa thực thi |
