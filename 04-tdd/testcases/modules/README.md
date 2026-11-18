# Testcase QA theo module

Mỗi thư mục ứng với một bounded context trong `04-tdd/modules/`; mỗi file bên trong là một cụm rủi ro. ID testcase và AC giữ nguyên khi di chuyển file. Trạng thái thiết kế và thực thi được theo dõi ở [kế hoạch chung](../README.md).

| Module | Cụm | Ca thiết kế | Mục lục |
| :--- | ---: | ---: | :--- |
| F1 identity | 11 | 165 | [F1](F1_identity/README.md) |
| F5 AI review | 7 | 144 | [F5](F5_ai-review/README.md) |
| F6 interview bank | 7 | 110 | [F6](F6_interview-bank/README.md) |
| F2 problem-bank | 14 | 171 | [F2](F2_problem-bank/README.md) |
| F3 harness | 11 | 145 | [F3](F3_harness/README.md) |
| F4 judge-orchestration | 16 | 173 | [F4](F4_judge-orchestration/README.md) |

Tổng F1–F4: **51 cụm, 641 ca thiết kế**. F5/F6: **14 cụm, 254 ca thiết kế**. Toàn bộ F1–F6 có **895 ca**, chưa có run record. F1 đã đóng audit thiết kế; F5/F6 đã review đủ cụm, còn một quyết định tràn mảng AI ảnh hưởng hai ca, xem [checklist](../CHECKLIST.md).

Bổ sung 2026-11-11: F2 AC-95 thêm13 ca thiết kế ở cụm defaults, tổng F2 hiện171/14 cụm, chưa có run record cho13 ca mới. Các tổng gộp895/641 ở trên giữ mốc baseline trước bổ sung, chưa phải kiểm kê lại toàn bộ F1/F5/F6 đang thay đổi.
