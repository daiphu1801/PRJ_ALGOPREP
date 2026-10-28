# Test case QA — F4: chi tiết bài nộp, Beats và stderr

> 13 ca thiết kế cho AC-30, AC-34–35; chưa thực thi. Nguồn: TDD F4 AC-30, 34–35; DD F4 API §5, §8; logic BR-JD-11/12. Fixture độc lập giữa các ca; mọi request dùng JWT của chủ bài nộp. Đối chiếu JSON thô trước khi xem giao diện để phát hiện dữ liệu bị lộ dù UI che.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 30 | Accepted 12/12, AI lỗi, problem-bank lỗi | TC-F4-AC30-01–03 | Đã review | Chưa thực hiện |
| 34 | Công thức, ngưỡng 9/10, bài sai | TC-F4-AC34-01–04 | Đã review | Chưa thực hiện |
| 35 | Sample/Hidden, tắt cấu hình khi lưu/đọc, wrapper, cắt đoạn, Run Code | TC-F4-AC35-01–06 | Đã review | Chưa thực hiện |

## Fixture chung

- Bài `P-X` đã xuất bản, `STANDARD_IO`, có snapshot 12 testcase theo `position=1..12`: 1–3 là Sample, 4–12 là Hidden. Bài nộp `S-A` của học viên A, Java, `ACCEPTED`, 12/12, `score_ratio=1.000`, `runtime_ms=100` và `memory_kb=2048`; nguồn lưu là `class Main { public static void main(String[] a) {} }`. `finished_at` đã có, chi tiết chưa hết hạn 90 ngày. Testcase thứ i có `testcase_order=i`; giá trị runtime/memory từng dòng được cố định riêng khi cần đối chiếu MAX.
- Dùng `P-Y` và học viên B làm dữ liệu ngoài phạm vi. Sau mỗi ca, hoàn nguyên cấu hình `return_stderr_to_student`, dịch vụ giả lập và xoá riêng các submission/testcase fixture. Chụp request/response, hàng DB liên quan, ảnh màn và bản cấu hình trong bằng chứng.

### TC-F4-AC30-01 — Accepted hiển thị đủ kết quả theo thứ tự

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-30; DD F4 API §8.3/8.7 T-GD-01; logic BR-JD-11 |
| Kind / priority | Normal, P0: kết quả chấm là dữ liệu cốt lõi |
| Environment | API #5, DB judge, FE `USR0201`, F2 Sample read port |
| Preconditions | `S-A` có 12 dòng chi tiết; dòng 1 có 12 ms/512 KB, dòng 12 có 100 ms/2048 KB, các dòng giữa thấp hơn maxima; `beatsSampleSize=6` để phần Beats không cản kiểm kết quả |
| Test data | `GET /submissions/{S-A}` bằng JWT A |
| Steps | Gọi API, mở trang kết quả, đối chiếu từng thứ tự với snapshot/DB; kiểm payload thô. |
| Expected | HTTP 200; `sourceCode` bằng nguồn đã nộp, `language=JAVA`, `submissionModel=STANDARD_IO`, `status=ACCEPTED`, `passedCount=totalCount=12`, `scoreRatio=1.000`, `runtimeMs=100`, `memoryKb=2048`; `testcaseDetail={available:true,unavailableReason:null,items:[12 dòng]}` với `order=1..12`, mỗi dòng có verdict AC và số runtime/memory đúng DB; 1–3 `visibility=SAMPLE`, 4–12 `visibility=HIDDEN` và không có ID/input/expected/output/stderr của Hidden. FE hiển thị trạng thái, điểm 1/1, nguồn, ngôn ngữ, cách nộp, maxima và 12 dòng đúng thứ tự. |
| Cleanup | Xoá fixture S-A và 12 kết quả |
| Evidence | JSON đầy đủ, DB theo `testcase_order`, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC30-02 — Dịch vụ phân tích AI lỗi vẫn đọc được kết quả

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-30; DD F4 API §8; TDD yêu cầu chi tiết độc lập AI |
| Kind / priority | Degraded, P1: lỗi phụ trợ không che kết quả chấm |
| Environment | API #5, FE `USR0201`, AI service có thể cưỡng bức lỗi |
| Preconditions | `S-A` như fixture; AI service trả 503 trong suốt request và tải trang |
| Test data | `GET /submissions/{S-A}`; mở trang kết quả |
| Steps | Cưỡng bức AI 503; gọi API và tải trang; so với bản khi AI hoạt động. |
| Expected | API #5 vẫn 200, các trường bài nộp và 12 dòng testcase như TC-F4-AC30-01; FE vẫn hiển thị kết quả/nguồn, không biến thành màn lỗi hay mất bảng do AI 503. Không có thay đổi DB bài nộp. |
| Cleanup | Khôi phục AI, xoá fixture |
| Evidence | Log lỗi AI, response #5, ảnh màn, DB trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC30-03 — Problem summary lỗi không làm mất chi tiết

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-30; DD F4 API §8.3/8.7 T-GD-06 |
| Kind / priority | Degraded, P1: giữ quyền truy cập kết quả đã lưu |
| Environment | API #5, `ProblemSummaryReadPort` có thể cưỡng bức lỗi |
| Preconditions | `S-A` như fixture; cổng summary bài P-X trả lỗi, dữ liệu judge còn |
| Test data | `GET /submissions/{S-A}` |
| Steps | Cưỡng bức cổng summary lỗi; gọi API; so trường bài nộp/chi tiết với DB. |
| Expected | HTTP 200, `problem=null`; `submissionId`, source, status, điểm, maxima và 12 dòng vẫn đúng; không thay `status` thành SYSTEM_ERROR và không xoá dữ liệu judge. |
| Cleanup | Khôi phục cổng, xoá fixture |
| Evidence | Fault injection, JSON, DB |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC34-01 — Beats dùng cùng bài/ngôn ngữ và làm tròn nửa lên

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-34; DD F4 logic BR-JD-12; API §8.3 |
| Kind / priority | Normal + boundary, P1: không đếm sai tập so sánh |
| Environment | API #5, DB judge, FE `USR0201` |
| Preconditions | Trên P-X/JAVA có đúng 40 submission ACCEPTED của nhiều user, gồm S-A 100 ms; 19 submission khác runtime >=100 ms (gồm ít nhất một bằng 100), 20 submission khác <100 ms. Thêm 6 ACCEPTED P-X/CPP, 5 ACCEPTED P-Y/JAVA và 2 WRONG_ANSWER P-X/JAVA; các tập này không thuộc mẫu. |
| Test data | GET S-A; công thức `ROUND(19*100/40)=48` theo half-up |
| Steps | Gọi API, xem Beats ở trang; truy vấn DB xác nhận từng tập và số dòng >=100 ms. |
| Expected | `beatsSampleSize=40`, `beatsPercent=48`; FE hiện 48%; mẫu số có S-A nhưng tử số loại chính S-A; không cộng 6 CPP, 5 P-Y hay 2 bài sai. Không lưu/chỉnh `beatsPercent` trong DB khi đọc. |
| Cleanup | Xoá các submission fixture |
| Evidence | SQL đếm và runtime, response, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC34-02 — Chín bài Accepted chưa đủ mẫu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-34; DD F4 BR-JD-12 ngưỡng 10; API §8.7 T-GD-08 |
| Kind / priority | Boundary, P1: không hiển thị tỷ lệ khi mẫu nhỏ |
| Environment | API #5, FE `USR0201`, DB judge |
| Preconditions | Trên P-X/CPP có đúng 9 bài ACCEPTED tính cả bài đang xem; 40 bài P-X/JAVA tồn tại nhưng khác ngôn ngữ |
| Test data | GET bài CPP của A |
| Steps | Gọi API, mở trang, đếm mẫu trong DB. |
| Expected | `beatsSampleSize=9`, `beatsPercent=null`; FE hiện “Chưa đủ dữ liệu so sánh”, không hiện 0%, không lấy 40 bài Java. |
| Cleanup | Xoá fixture CPP |
| Evidence | DB count, JSON, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC34-03 — Đúng mười bài bắt đầu tính Beats

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-34; DD F4 BR-JD-12 ngưỡng 10 |
| Kind / priority | Boundary, P1: kiểm dấu so sánh tại ngưỡng |
| Environment | API #5, DB judge |
| Preconditions | P-X/CPP có đúng 10 ACCEPTED gồm bài của A ở 100 ms; 5 bài khác >=100 ms và 4 bài <100 ms |
| Test data | GET bài của A |
| Steps | Gọi API; đối chiếu truy vấn tập so sánh. |
| Expected | `beatsSampleSize=10`, `beatsPercent=50` (`5*100/10`); không còn thông báo thiếu dữ liệu. |
| Cleanup | Xoá fixture CPP |
| Evidence | DB count, JSON |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC34-04 — Bài không Accepted không có mẫu Beats

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-34; DD F4 BR-JD-12; API §8.3 |
| Kind / priority | Abnormal, P1: không gán xếp hạng cho bài sai |
| Environment | API #5, FE `USR0201` |
| Preconditions | A có submission `WRONG_ANSWER` P-X/JAVA; cùng bài/ngôn ngữ có 40 bài ACCEPTED khác |
| Test data | GET bài WRONG_ANSWER |
| Steps | Gọi API và xem màn. |
| Expected | `beatsPercent=null`, `beatsSampleSize=null`; FE hiện dấu gạch `-`, không hiện thông báo thiếu dữ liệu và không tính số 40 cho bài sai. |
| Cleanup | Xoá fixture |
| Evidence | JSON, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-01 — Chỉ Sample Standard I/O lỗi mới có stderr

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 logic BR-JD-04/11; API §8.3 |
| Kind / priority | Security, P0: stderr Hidden có thể lộ input |
| Environment | Worker thật/giả lập có kiểm soát, DB judge, API #5, FE `USR0201` |
| Preconditions | Cấu hình `return_stderr_to_student=true`; bài STANDARD_IO có Sample order 1 và Hidden order 2 cùng verdict RE; stderr phát ra lần lượt `SAMPLE_ERR_41` và `HIDDEN_SECRET_73` |
| Test data | Nộp nguồn gây RE ở cả hai testcase, chờ chấm cuối |
| Steps | Nộp, kiểm DB sau chấm, gọi #5, xem JSON thô và màn. |
| Expected | Dòng Sample có `stderrSnippet=SAMPLE_ERR_41` và màn hiện đoạn này; DB Sample lưu snippet. DB Hidden `stderr_snippet=NULL`; dòng Hidden trong JSON chỉ có order/verdict/runtime/memory, không có marker Hidden ở bất kỳ trường nào; FE không hiện marker Hidden. Không có input/expected của hai ca trong API F4. |
| Cleanup | Hoàn nguyên cấu hình, xoá fixture |
| Evidence | Cấu hình, DB hai dòng, JSON và ảnh màn; tìm marker trong toàn payload |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-02 — Tắt cấu hình trước lượt nộp mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 logic BR-JD-04/11; API §8.3 |
| Kind / priority | Security, P0: cấu hình tắt phải chặn dữ liệu từ lúc lưu |
| Environment | Admin sandbox settings, worker, DB, API #5 |
| Preconditions | Admin đặt `return_stderr_to_student=false` trước khi nộp; cùng nguồn và testcase như TC-F4-AC35-01 |
| Test data | Submission mới `S-OFF` |
| Steps | Xác nhận phiên bản cấu hình worker nhận; nộp S-OFF, chờ chấm, đọc DB và JSON. |
| Expected | Cả Sample/Hidden có `stderr_snippet=NULL` trong DB; dòng Sample trả `stderrSnippet=null`, dòng Hidden không có trường `stderrSnippet` theo BR-JD-11. Hai marker không xuất hiện trong JSON hay màn. Verdict RE vẫn được ghi; không mất kết quả chấm vì tắt stderr. |
| Cleanup | Khôi phục cấu hình, xoá S-OFF |
| Evidence | Cấu hình trước nộp, DB, response, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-03 — Function Wrapper không lưu stack trace dù cấu hình bật

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 logic BR-JD-04 bước 8, BR-JD-11; API §8.3 |
| Kind / priority | Security, P0: bảo vệ mã khung của Function Wrapper |
| Environment | Bài FUNCTION_WRAPPER hợp lệ, worker, DB, API #5 |
| Preconditions | `return_stderr_to_student=true`; Sample gặp RE với stderr chứa marker `FRAME_TRACE_97` từ khung |
| Test data | Bài nộp wrapper của A |
| Steps | Nộp, chờ chấm, kiểm DB và GET #5 bằng JWT A. |
| Expected | Verdict RE còn; `stderr_snippet=NULL` ở DB, `stderrSnippet=null` trong response; marker và stack trace khung không có trong JSON/màn. |
| Cleanup | Xoá fixture, khôi phục cấu hình |
| Evidence | DB, JSON, ảnh màn và log nội bộ đã che dữ liệu nhạy cảm |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-04 — Cắt stderr Sample ở 4096 ký tự, Accepted không lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 logic BR-JD-04 bước 8, BR-JD-11; API §8.3 |
| Kind / priority | Boundary + security, P1: giới hạn dữ liệu trả về |
| Environment | Worker, DB, API #5 |
| Preconditions | STANDARD_IO, `return_stderr_to_student=true`; Sample 1 RE phát 4100 ký tự ASCII: 4096 `A` rồi `TAIL`; Sample 2 AC phát `AC_MARKER` lên stderr |
| Test data | Submission có hai dòng Sample trên |
| Steps | Chạy chấm, đọc DB và GET #5; đếm ký tự snippet, tìm marker. |
| Expected | Dòng 1 DB/API có đúng 4096 ký tự `A`, không chứa `TAIL`; dòng 2 `stderr_snippet=NULL` và `stderrSnippet=null`, không có `AC_MARKER` trong payload. Cả hai verdict và runtime/memory vẫn đúng. |
| Cleanup | Xoá fixture, khôi phục cấu hình |
| Evidence | DB length, JSON length, log cấu hình |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-05 — Tắt cấu hình sau khi chấm vẫn chặn stderr khi đọc

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 logic BR-JD-11; API §8.3 |
| Kind / priority | Security + recovery, P0: không trả lại dữ liệu khi chính sách hiện hành đã tắt |
| Environment | Admin sandbox settings, API #5, DB judge |
| Preconditions | S-OLD là bài STANDARD_IO có Sample RE đã chấm lúc `return_stderr_to_student=true`; DB có `stderr_snippet=OLD_SAMPLE_ERR`; JWT của chủ bài hợp lệ |
| Test data | GET S-OLD khi bật; sau đó admin đổi cấu hình thành false và GET cùng ID lần nữa |
| Steps | Lưu JSON lần đầu; tắt cấu hình, xác nhận API đọc cấu hình mới; gọi lại #5, kiểm JSON thô và DB. |
| Expected | Lần đầu Sample trả `stderrSnippet=OLD_SAMPLE_ERR`; lần sau cùng dòng trả `stderrSnippet=null`, không có marker ở bất kỳ chỗ nào trong JSON/màn, dù DB vẫn giữ snippet cũ. Verdict và các số chấm không đổi; thao tác đọc không sửa DB. |
| Cleanup | Khôi phục cấu hình, xoá S-OLD |
| Evidence | Hai cấu hình, hai response, DB trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC35-06 — Run Code chỉ trả stderr của Sample Standard I/O

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-35; DD F4 API §5.3; logic BR-JD-02/11 |
| Kind / priority | Security, P0: đường chạy thử không được kéo Hidden vào response |
| Environment | API #2 `POST /problems/{problemId}/run-sample`, sandbox settings, bài P-X |
| Preconditions | P-X có Sample order 1 gây RE và phát `RUN_SAMPLE_ERR`, Hidden order 4 có input/expected marker `RUN_HIDDEN_88`; `return_stderr_to_student=true` |
| Test data | Run Code STANDARD_IO với nguồn gây lỗi Sample; lặp lại sau khi tắt cấu hình |
| Steps | Gọi #2 khi bật, kiểm toàn response; tắt cấu hình, gọi lại cùng nguồn và kiểm toàn response. |
| Expected | Cả hai lần `results[]` chỉ chứa testcase Sample; khi bật, dòng RE Sample có `stderrSnippet=RUN_SAMPLE_ERR`; khi tắt, cùng dòng có `stderrSnippet=null`. Không có ID, input, expected, output hay marker của Hidden trong bất kỳ response nào; Run Code không tạo submission hoặc kết quả chấm bền vững. |
| Cleanup | Khôi phục cấu hình, dọn dữ liệu Run Code tạm nếu có |
| Evidence | Hai request/response, cấu hình, DB kiểm không có submission mới |
| Design review | Đã review; chưa thực thi |
