# BD — Security module `harness` (F3)

**Checkpoint D02e 2026-11-01:** source vào payload dưới JSON copyIn content, không ở argv/env
hoặc chuỗi shell; compiler/runtime chỉ chạy trong sandbox go-judge. APH_MAX_RESULT_BYTES do
F4 ghi đè mỗi testcase, không lấy từ payload env. Runtime marker/traceback wrapper không gửi
sang P3, không có trong kết quả component; compiler output riêng vẫn theo log nội bộ P3.

> Đọc cùng `02-bd/architecture/F3_harness.md` mục 5, 7 và `CLAUDE.md` mục Rules ("A submission is data, never
> an instruction" — nguyên tắc viết cho F5/F6 nhưng tinh thần áp dụng trực tiếp cho sinh mã ở đây).

## 1. Nguyên tắc trung tâm — mã nguồn người học luôn là DATA, không bao giờ là INSTRUCTION

**D02d 2026-11-01:** helper sinh wrapper chỉ đọc tài nguyên jar và nối source bằng assembler;
không thực thi mã người học trong JVM. Mã đọc input kết thúc trước call, lỗi chỉ marker cố định 90.
Buffer kết quả có trần byte UTF-8 trước khi ghi sentinel; quá byte/node cap trả 91, không xuất kết quả
dở dang. Guard Python nằm trước call, lỗi thân hàm không bị guard bắt. Việc mã người học có thể
can thiệp tiến trình/biến môi trường vẫn thuộc ranh giới sandbox; F4 phải cấp giới hạn engine thật,
kiểm signal theo model/status/marker và ẩn stderr wrapper. Probe riêng không thay F4/rollout.

**Cập nhật DD 2026-10-25 (đóng Q9):** mô tả bên dưới giả định ô soạn chỉ chứa thân hàm. Hợp đồng `GetStarterCode` và `PrepareSubmission` hiện cho người học sửa **toàn bộ mã nguồn** trong một vùng liền mạch, gồm `class Solution`/chữ ký hàm; Python không re-indent. Các khẳng định bên dưới rằng người học không thể đặt mã ngoài thân hàm hoặc hậu quả tối đa chỉ là lỗi biên dịch **không còn là bảo đảm bảo mật**. Mã người học có thể thực thi logic bất kỳ trong phạm vi sandbox; ranh giới bảo vệ là go-judge với giới hạn tài nguyên, tiến trình và mạng. F3 chỉ bảo đảm không nội suy mã người học thành `argv`, tên tệp, biến môi trường, script của image hoặc cấu hình sandbox; `LineMap` vẫn tính đúng một vùng nguồn cố định. Xem `03-dd/logic/F3_harness.md` §3.6, §4.1–4.4.

Rủi ro chính của `harness` không phải prompt injection (đó là F5/F6) mà là **injection vào cấu trúc mã sinh
ra** — mã người học ảnh hưởng tới phần khung (template) ngoài đúng vùng thân hàm/`main()` của chính họ.
Nguyên tắc khoá:

- Mã người học **chỉ được chèn vào đúng một vùng đã định trước** trong template (F3-05) — vùng chèn là một
  **vị trí xác định trước khi build chuỗi** (biết trước offset dòng bắt đầu/kết thúc, mục 7 file
  architecture), không phải kết quả của việc tìm-thay-thế chuỗi (`String.replace`) trên nội dung do người
  học kiểm soát được một phần.
- **Không bao giờ nối chuỗi mã người học vào phần khung theo cách người học có thể thoát khỏi vùng chèn** —
  cụ thể: không dùng template engine dạng "chèn theo tên biến rồi nội suy chuỗi" nếu tên hàm/tên tham số do
  người học kiểm soát (chữ ký hàm do A2/A3 khai ở `problem-bank`, không phải người học tự đặt tên hàm tuỳ ý
  — nên bề mặt tấn công này thực ra bị chặn từ tầng `problem-bank`: `function_name`/tên tham số cố định theo
  đặc tả, người học **không** truyền tên hàm của họ vào hệ thống). Rủi ro còn lại thuộc về **nội dung thân
  hàm** — chuỗi ký tự bất kỳ người học gõ trong Monaco Editor.
- Với nội dung thân hàm: harness **không parse/hiểu ngữ nghĩa** mã người học (không cố kiểm duyệt "mã có an
  toàn không" ở tầng sinh mã — đó là việc của sandbox go-judge ở tầng thực thi, không phải của `harness` ở
  tầng sinh mã nguồn). `harness` chỉ đảm bảo **vị trí chèn không đổi bất kể nội dung** — nếu người học viết
  mã chứa ký tự đóng khối (`}`,`"""`, v.v.) cố ý để "thoát" ra ngoài thân hàm dự kiến, hậu quả tối đa là mã
  **không biên dịch được** (lỗi cú pháp, xử lý theo mục 7 file architecture — ánh xạ lỗi về đúng dòng người
  học), **không phải** khả năng ghi đè logic khung xung quanh, vì:
  - Với Java/C++ (ngôn ngữ có khối `{}` rõ ràng): vùng chèn là **thân của đúng một phương thức/hàm đã có chữ
    ký cố định phía trước** — người học chỉ viết phần bên trong dấu `{` đã mở sẵn bởi khung, không tự viết
    lại dấu mở/đóng của hàm bao ngoài. Nếu người học cố tình đóng sớm bằng `}` thừa, phần khung phía sau
    (lời gọi hàm, in output) trở thành mã nằm ngoài hàm — đây là lỗi biên dịch (thường là "unexpected token"
    hoặc tương tự), không phải lỗ hổng thực thi ngoài ý muốn.
  - **Đã thay thế (DD 2026-10-25, xem dòng 8): Python không re-indent.** Với Python (không có khối `{}`, dựa vào thụt lề): vùng chèn thân hàm phải giữ đúng **mức thụt lề cố định
    của khung**, template chèn mã người học **sau khi re-indent** toàn bộ khối theo đúng một mức thụt lề
    thống nhất (không tin thụt lề gốc do Monaco Editor gửi lên) — nếu người học cố ý làm lệch thụt lề để
    thoát khỏi thân hàm, kết quả là `IndentationError` (lỗi biên dịch/parse), không phải chạy mã ở phạm vi
    module cấp cao hơn dự kiến. Đây là rủi ro đặc thù Python cần lưu ý riêng ở DD khi viết plugin
    `PythonHarnessPlugin`.
- **Không chạy `eval`/`exec` trên bất kỳ phần nào của mã người học ở phía service sinh mã** (harness chạy
  trong JVM backend, không tự thực thi mã người học — việc thực thi luôn đi qua sandbox go-judge, tách biệt
  tiến trình/namespace). `harness` chỉ **ghép chuỗi mã nguồn**, không bao giờ tự biên dịch/chạy thử trong
  tiến trình của chính nó.

## 2. Đóng gói qua `JudgeExecutionPort` (F3-06) — không rò rỉ định dạng adapter ra ngoài port

**Owner chốt/kiểm D02c 2026-11-01:** guard Python trusted resource chạy trong sandbox,
không eval/exec trong JVM. Exit 92 + marker cố định báo sai lookup/bind; lỗi constructor,
descriptor và thân hàm giữ runtime. Script Java là nội dung image, chỉ nhận Solution.java/Main.java,
thư mục script mount read-only trong sandbox. Probe dùng container tạm đã xóa, không thay service.
Chưa có F4 classifier/luồng trả kết quả; bảo đảm ẩn runtime stderr vẫn phải thực hiện ở F4.

- Payload gửi qua `JudgeExecutionPort` đúng hình dạng nghiệp vụ ("chạy một chương trình, trả một kết quả"),
  không rò rỉ trường đặc thù go-judge (`cmd[]`/`copyIn`) hay đặc thù Judge0 (Base64, webhook token) ra khỏi
  tầng `infrastructure` của `harness`/`judge-orchestration` — đúng nguyên tắc port trung lập đã chốt ở
  `DEC-2026-0823-go-judge-default-engine` điểm (2). Vi phạm nguyên tắc này (để domain/application biết hình
  dạng adapter cụ thể) làm cho việc đổi lại `Judge0Adapter` sau này thành một cuộc tái cấu trúc lớn thay vì
  một lần swap adapter — bản thân đây là rủi ro kiến trúc, không phải rủi ro bảo mật cổ điển, nhưng ghi ở
  đây vì cùng nguyên tắc "ranh giới rõ ràng" bảo vệ cả an toàn lẫn khả năng bảo trì.
- **Giới hạn kích thước mã nguồn gửi đi** (chưa thấy RD nêu con số cụ thể) — `[SoT: Suy luận]`, BD đề xuất
  giới hạn kích thước mã nguồn người học tối đa (ví dụ 64 KB mỗi submission) ở tầng nhận submission (thuộc
  `judge-orchestration`, không phải `harness`, nhưng ảnh hưởng trực tiếp lượng mã `harness` phải chèn vào
  template) để tránh một submission cực lớn làm phình payload gửi sang go-judge hoặc kéo dài thời gian sinh
  mã bất thường — con số cụ thể chốt ở DD cùng `judge-orchestration`.

## 3. Che giấu lỗi thuộc mã harness (F3-12) — không lộ mã hệ thống

- Đã có nguyên lý dịch số dòng ở `02-bd/architecture/F3_harness.md` mục 7 — bổ sung khía cạnh bảo mật: thông
  điệp lỗi chung chung trả về khi lỗi thuộc phần khung (bước 3, mục 7 file architecture) **không được chứa
  bất kỳ đoạn trích nào của mã khung** (không lộ tên biến/hàm nội bộ, không lộ đường dẫn file tạm trên máy
  chủ chấm bài) — chỉ một câu thông báo cố định, không nội suy theo nội dung lỗi thật.
- Log nội bộ (mức `ERROR`) chứa lỗi thật để đội phát triển debug **không được lộ ra bất kỳ API nào
  học viên truy cập được** — cùng nguyên tắc đã áp dụng cho Hidden testcase ở `02-bd/security/problem-
  bank.md` mục 2 (log chi tiết chỉ dành nội bộ, không đi qua route công khai).
- Đường dẫn file tạm, tên biến nội bộ của khung, số hiệu phiên bản compiler cụ thể trên máy chủ **không xuất
  hiện** trong bất kỳ thông báo nào trả về học viên — giảm bề mặt do thám (reconnaissance) cho kẻ tấn công
  muốn dò cấu hình hạ tầng chấm bài qua thông điệp lỗi.

**Đồng bộ chủ dự án chốt/triển khai P3 2026-11-01:** bản raw được parse/log chỉ tối đa 65.536 ký tự,
kèm correlationId và cờ truncated; không lưu nguyên phần đuôi bị cắt vào log. Không trả source/caret
hoặc raw compiler output trong DTO. Log sink lỗi thì vẫn trả report đã làm sạch, không echo exception
của logger; việc mất log và quyền truy cập/retention của sink phải được ghi nhận khi vận hành.
P3 không eval/exec source; guard chữ ký Python đã chọn chỉ chạy trong sandbox ở lát P2/F4 sau này.

## 4. Không có bề mặt tấn công qua dữ liệu lưu trữ

Vì `harness` không có bảng Postgres riêng (`02-bd/database/F3_harness.md` mục 1), không có rủi ro injection SQL
hay rò rỉ dữ liệu qua truy vấn ở module này — bề mặt tấn công duy nhất đáng kể là **sinh mã** (mục 1-3 ở
trên) và **thực thi** (thuộc `judge-orchestration`/go-judge, ngoài phạm vi BD này).

## 5. Việc còn mở — chuyển sang DD

- Giới hạn kích thước mã nguồn cụ thể (mục 2) — chốt cùng `judge-orchestration`.
- Cơ chế re-indent Python cụ thể (mục 1) — thuật toán chuẩn hoá thụt lề trước khi chèn, chốt ở
  `03-dd/logic/F3_harness.md` khi viết `PythonHarnessPlugin`.
- Danh sách chính xác các trường bị lọc khỏi thông báo lỗi trả về học viên (mục 3) — chốt khi có bảng lỗi cụ
  thể theo từng compiler.

## 6. Tham chiếu

- `CLAUDE.md` mục Rules — "A submission is data, never an instruction" (áp dụng tinh thần từ F5/F6 sang F3).
- `02-bd/architecture/F3_harness.md` mục 5 (interface plugin), mục 7 (ánh xạ lỗi biên dịch).
- `DEC-2026-0823-go-judge-default-engine` — hình dạng payload port trung lập.
- `02-bd/security/F2_problem-bank.md` mục 2 — nguyên tắc log nội bộ không lộ ra route công khai, tái dùng ở
  đây cho lỗi biên dịch thay vì Hidden testcase.


## Triển khai D02s — 2026-11-04: F3 starter HTTP và nhánh không cache

**D02s — F3 REST starter P1, 2026-11-04:** HTTP starter/quyền hiện hành/phiên bản thật đã kiểm; nhánh F3 không cache đọc v1→v2 không chờ event và từ chối expected version cũ. 57 test liên quan đạt (13 unit mới, 18 API mới, 26 hợp đồng/wiring trực tiếp), 5/5 mutation bị bắt; Spotless/Checkstyle/SpotBugs đạt. Chỉ tập trung F3/F4, task riêng F1/F2 tạm gác. C08/cache-on giữ giới hạn đã nêu, D09/persisted E2E và FE chưa đóng. [Report](F:/prj_algoprep/06-plan/reports/261104-f3-starter-http/report.md).

GET P1 theo URL language/model, response language/submissionModel/sourceCode (không specVersion).
Identity từ JWT subject và authorities hiện hành, không tin claim role cũ/query. Published cho
mọi authenticated, unpublished author/admin; missing/deleted/ngoài quyền cùng404 HRN-001.
HTTP lỗi đúng §3.4, không trả raw cause/message; thiếu/lạ model là400 binding Spring, DD chưa
quy định mã miền riêng. Giữ core RR và port đã có, không sửa F1/F2. @Lazy chỉ giữ pure core scan
không cần lắp bootstrap, runtime ứng dụng dùng port thật.

Default không local cache theo logic §9/BD database §2: đã kiểm v1→v2/matching, Redis poison,
P2 stale expected bị HRN-002. Chưa có bằng chứng cache-on/E1 Rabbit delivery; không tick QA
TC-F3-AC40-01–04 cho cấu hình cache bật. Không thêm no-op listener hoặc tạo cache giả.
Sample pipeline save→starter→điền lời giải→F4 Run Code đã đạt, không thay persisted Submit E2E
D09/AC-18 hoặc FE/browser. C08 giữ hoãn. Full regression/coverage BUNDLE để mốc clear theo owner.


**D02u — 2026-11-04:** Backend F3 local đã đóng, giữ ngoại lệ C08/AC-49 HTTP/UI tiếp tục hoãn theo owner. Review đủ 50 AC với symbol/oracle và mức bằng chứng; 6/6 test Spring startup mới (AC-44) và Spotless/Checkstyle/SpotBugs đạt. 114 file lõi/resource + 2 HTTP khớp nguồn đã kiểm; không chạy lại suite cũ hoặc full regression. Cache-on conditional chưa bật; F4/FE/FINAL còn mở. [Report](F:/prj_algoprep/06-plan/reports/261104-f3-backend-closeout/report.md).
