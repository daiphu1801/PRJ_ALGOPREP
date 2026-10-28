# Test case QA — F2: giao bài và sự kiện lớp

> 13 ca thiết kế cho AC-72–80; TDD F2, DD API §50–55, BR-18–20. Chưa thực thi. Dùng đồng hồ T0 cố định; mỗi ca chụp assignment, submission, Outbox và thống kê trước/sau.

### TC-F2-AC72-01 — Danh sách tất cả gộp bài theo lớp, giữ bài rút
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-72; DD §50 |
| Kind / priority | Normal, P0 |
| Environment | API, DB, stats port |
| Preconditions | I phụ trách A/B; P giao A/B; Q đã gỡ A; R còn giao A nhưng withdrawn; P có số liệu A=2 lượt, B=3 lượt. |
| Test data | Token I; `GET /instructor/class-assignments` và cùng route với `classId=A`; seed P có hai assignment active A/B, Q có `removed_at=T0-1d`, R active A và `UNPUBLISHED`. |
| Steps | GET danh sách tất cả và `classId=A`; kiểm rows và thống kê. |
| Expected | Tất cả: P đúng một dòng với A/B, không Q, R có withdrawn=true. Lọc A: P chỉ mang chỉ số lớp A (2), R còn hiện; không cộng B vào A. |
| Cleanup | Dọn fixture |
| Evidence | hai response và DB rows. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC72-02 — Lớp ngoài phạm vi và thống kê lỗi
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-72; DD §50 |
| Kind / priority | Security + recovery, P0 |
| Environment | API, stats port fault injection |
| Preconditions | I phụ trách A, J phụ trách C; A có P đang giao. |
| Test data | Token I; request `classId=C` rồi `classId=A`; stats port ném timeout chỉ ở request A, assignment P/A vẫn active. |
| Steps | I gọi `classId=C`; cho stats port lỗi rồi I gọi `classId=A`. |
| Expected | C trả `403 PRB-005`, không lộ bài; A vẫn trả P và hai chỉ số thống kê `null`, không đổi thành 0. |
| Cleanup | Khôi phục port |
| Evidence | responses và trace. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC73-01 — Summary đếm bài riêng và làm tròn tỷ lệ
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-73; DD §51 |
| Kind / priority | Normal + empty, P1 |
| Environment | API, stats port |
| Preconditions | I có P1/P2/P3 là 3 bài duy nhất đang giao trong 2 lớp; P1 AC%=50, P2 AC%=51, P3 chưa có lượt nộp; J không có lớp. |
| Test data | Token I và J gọi `GET /instructor/class-assignments/summary`; P1/P2 có stats `50`/`51`, P3 stats null, P1 được giao cả hai lớp nhưng chỉ tính một bài. |
| Steps | GET summary bằng I và J. |
| Expected | I `assignedProblemCount=3,classCount=2,avgAcRatePercent=51` từ (50+51)/2=50.5 làm tròn; P3 không vào trung bình. J `0,0,null`. Không nhân đôi bài giao hai lớp. |
| Cleanup | Dọn fixture |
| Evidence | hai response, dữ liệu stats đầu vào. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC74-01 — Danh sách có thể giao bỏ bài hiện hành và bài không xuất bản
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-74; DD §52 |
| Kind / priority | Normal + security, P0 |
| Environment | API, DB |
| Preconditions | I phụ trách A; P đang giao A; S từng giao rồi gỡ; T published chưa giao; U unpublished; V deleted; C lớp của J. |
| Test data | Token I; `GET /instructor/classes/{A}/assignable-problems` và cùng route với C; P active A, S có `removed_at=T0-1d`, T/U/V chưa giao. |
| Steps | GET assignable A và C bằng I. |
| Expected | A có S,T; không P,U,V. C `403 PRB-005`, không lộ bài của J. |
| Cleanup | Dọn fixture |
| Evidence | responses và assignment rows. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC75-01 — Giao lô thành công từng phần, thử lại bỏ qua
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-75; DD §53, BR-18 |
| Kind / priority | Normal + idempotency, P0 |
| Environment | API, DB, Outbox |
| Preconditions | I phụ trách A; P1–P3 published, U unpublished; chưa có assignment; Outbox N. |
| Test data | Token I; hai POST cùng body `{problemIds:[P1,P2,P3,U]}` vào lớp A; Outbox seed rỗng cho bốn bài. |
| Steps | Giao [P1,P2,P3,U]; gọi lại cùng danh sách. |
| Expected | Lần đầu succeededIds đúng P1–P3, U trong failedItems với `PRB-037`, thêm 3 assignment hoạt động và 3 `ProblemAssignedToClass`; lần hai P1–P3 ở skippedIds, không thêm assignment/event. |
| Cleanup | Dọn fixture |
| Evidence | hai response, DB/Outbox diff. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC75-02 — Hai giảng viên tranh cùng cặp bài/lớp và trần 50 ID
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-75; DD §53, BR-18 |
| Kind / priority | Concurrency + boundary, P0 |
| Environment | API, DB, Outbox |
| Preconditions | I/J đều có quyền giao vào A theo fixture; P published; 51 UUID khác nhau. |
| Test data | Hai POST đồng thời của I/J với `{problemIds:[P]}` vào lớp A; request biên của I có `problemIds=[P1..P51]` gồm 51 UUID phân biệt, chưa giao. |
| Steps | I/J giao P đồng thời; gửi request 51 ID; kiểm row/event. |
| Expected | Chỉ một assignment hoạt động cho (P,A), một event giao có hiệu lực; yêu cầu 51 ID `400 PRB-001`, không tạo dòng nào từ request ấy. |
| Cleanup | Dọn fixture |
| Evidence | response pair, DB uniqueness/Outbox. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC75-03 — Outbox lỗi ở một bài chỉ rollback bài đó
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-75; DD API §53 PB53-R1, logic BR-18/BR-26 |
| Kind / priority | Recovery, P0: không có assignment thiếu event và lô không mất các bài khác |
| Environment | API, PostgreSQL và Outbox writer fault injection theo `aggregateId` |
| Preconditions | I phụ trách lớp A; P1, P2, P3 đều `PUBLISHED`, chưa giao; Outbox writer chỉ ném lỗi khi ghi `ProblemAssignedToClass` cho P2 |
| Test data | `POST /instructor/classes/{A}/assignments` với `problemIds=[P1,P2,P3]`; sau đó request chỉ `[P2]` khi đã gỡ fault |
| Steps | Gọi lô đầu; kiểm response, `class_assignments`, Outbox của từng bài. Gỡ fault, gọi lại P2 và kiểm cùng các bảng. |
| Expected | Lô đầu `200`: `succeededIds=[P1,P3]`, `skippedIds=[]`, `failedItems` có đúng một mục `id=P2`, `code=PRB-056`, `message` là chuỗi không rỗng và không chứa lỗi SQL/stack trace; P1/P3 mỗi bài có đúng một assignment hoạt động và một Outbox event; P2 không có assignment hoặc event. Retry P2 trả `200`, `succeededIds=[P2]`, `failedItems=[]`, tạo đúng một assignment và một event; P1/P3 không đổi. Không có event phát từ giao dịch P2 đã rollback. |
| Cleanup | Gỡ fault; xoá fixture assignment/Outbox theo môi trường thử nghiệm |
| Evidence | Hai request/response, transaction trace và DB/Outbox diff theo từng `problemId` |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |

### TC-F2-AC76-01 — Gỡ bài giữ lịch sử và mốc gỡ khi retry
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-76; DD §54, BR-18 |
| Kind / priority | Idempotency, P0 |
| Environment | API, DB, progress/stats port, Outbox |
| Preconditions | P giao A, học viên có submission điểm 80 tại T0-1d; I phụ trách A; J không phụ trách; removed_at NULL. |
| Test data | Token I/J; `DELETE /instructor/classes/{A}/assignments/{P}` tại T0 và T0+1m; submission của học viên S trên P có `score=80`, `submittedAt=T0-1d`. |
| Steps | I gỡ tại T0; I gỡ lại T0+1m; J thử gỡ; đọc tiến độ và lịch sử. |
| Expected | Gỡ đầu đổi removed_at=T0 và phát thay đổi một lần; tiến độ hiện hành bỏ P khỏi mẫu số, lịch sử submission/điểm còn xem được. Lặp báo đã gỡ, giữ T0, không phát lại; J bị từ chối theo DD §54. |
| Cleanup | Dọn fixture |
| Evidence | ba response, rows, Outbox, tiến độ/lịch sử. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC77-01 — Top 5 theo lượt nộp và cổng thống kê lỗi
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-77; DD §55 |
| Kind / priority | Normal + recovery, P1 |
| Environment | API, stats port stub |
| Preconditions | I có 7 bài giao P1–P7; số lượt tương ứng 7,6,5,4,3,2,1. |
| Test data | Token I; `GET /instructor/assigned-problems/top`; stats port trả `{P1:7,P2:6,P3:5,P4:4,P5:3,P6:2,P7:1}`, lần hai ném timeout. |
| Steps | GET top; cho stats port lỗi rồi GET lại. |
| Expected | Lần đầu đúng P1–P5 theo thứ tự; lần lỗi `items=[]`, không hiển thị số 0 giả cho từng bài. |
| Cleanup | Khôi phục port |
| Evidence | hai response. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC78-01 — Đọc/đổi trần AI và kiểm biên quyền
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-78; DD §56–57 |
| Kind / priority | Boundary + security, P0 |
| Environment | Config API, DB |
| Preconditions | Trần mặc định=2; ADMIN A có AI_CONFIG; B không có. |
| Test data | Token A/B; request đọc cấu hình và các body ghi `{maxPerProblem:5}`, `{maxPerProblem:0}`, `{maxPerProblem:11}`, `{maxPerProblem:1}`, `{maxPerProblem:10}` theo DD §56–57. |
| Steps | A đọc, ghi lần lượt 5,0,11,1,10; B đọc/ghi; đọc cuối. |
| Expected | Mặc định 2; 5,1,10 được nhận/lưu; 0,11 `400 PRB-001`, không đổi giá trị trước; B `403 PRB-002` ở thao tác được bảo vệ; giá trị cuối=10. |
| Cleanup | Khôi phục trần |
| Evidence | responses và audit/config diff. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC79-01 — Khử trùng event và loại SYSTEM_ERROR khỏi tỷ lệ
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-79; DD BR-20 |
| Kind / priority | Event idempotency, P0 |
| Environment | Consumer, DB |
| Preconditions | P chưa có problem_stats; events e1 ACCEPTED, e2 WRONG_ANSWER, e3 SYSTEM_ERROR có ID khác. |
| Test data | Bốn delivery cho cùng `problemId=P`: e1 `eventId=E1,verdict=ACCEPTED`; phát lại E1; e2 `E2,WRONG_ANSWER`; e3 `E3,SYSTEM_ERROR`, mỗi submissionId khác nhau trừ delivery lặp. |
| Steps | Phát e1, phát lại e1, phát e2, e3; đọc row sau từng lần. |
| Expected | Sau e1 (1,1,100.00); e1 lặp không đổi; sau e2 (2,1,50.00); e3 không đổi. `ac_rate` cập nhật nguyên tử cùng hai counter, không có trạng thái trung gian sai. |
| Cleanup | Dọn fixture |
| Evidence | event IDs, row snapshots, transaction trace. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC79-02 — Hai consumer cùng event không tăng hai lần
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-79; DD BR-20 |
| Kind / priority | Concurrency, P0 |
| Environment | Hai consumer, DB |
| Preconditions | P có stats (2,1,50.00), event e4 ACCEPTED chưa xử lý. |
| Test data | Hai consumer nhận cùng phong bì `eventId=E4,problemId=P,verdict=ACCEPTED,submissionId=S4` tại T0; bảng consumed_events chưa có E4. |
| Steps | Đưa cùng e4 tới hai consumer đồng thời, chờ commit. |
| Expected | Row cuối (3,2,66.67), eventId chỉ được ghi xử lý một lần; không có trạng thái (4,3). |
| Cleanup | Dọn fixture |
| Evidence | consumer log, DB/processed-event rows. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC80-01 — Xoá lớp gỡ mềm một lần và giữ lịch sử
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-80; DD BR-19 |
| Kind / priority | Event idempotency, P0 |
| Environment | Consumer, DB |
| Preconditions | L có 3 assignment active, một dòng removed_at=T0-1d; có submission cũ. |
| Test data | Hai delivery cùng `eventId=E-CLASS-DELETE`, payload `{classId:L}` tại T0 và T0+1m; bốn assignment L/P1..P4, P4 đã `removed_at=T0-1d`, P1..P3 active. |
| Steps | Gửi `ClassDeleted{classId:L}` hai lần tại T0 và T0+1m; đọc rows/history. |
| Expected | Ba dòng active thành removed_at=T0; dòng cũ giữ T0-1d; không xoá vật lý, submission/điểm còn nguyên; lần hai không thay mốc hoặc phát thay đổi nghiệp vụ. |
| Cleanup | Dọn fixture |
| Evidence | event IDs và DB before/after. |
| Design review | Đã review; chưa thực thi |
