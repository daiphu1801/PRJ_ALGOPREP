# Kế hoạch class diagram theo actor (A1-A4)

**Trạng thái 2026-10-26:** Hoàn tất 26 nhóm use case thành 28 sơ đồ theo actor (A1 11, A2 8, A3 4, A4 5), thêm 5 mô hình lớp dùng chung và giữ 2 sơ đồ F3 mẫu. Bảng truy vết đạt 118/118 tiêu chí; 33 sơ đồ mới lint 0 FAIL. PNG xuất 2x để xem màn hình/in A3. Cảnh báo cỡ chữ khi ép A4 và chênh lệch BD/DD về ẩn danh tài khoản được ghi trong [hướng dẫn review](README.md) và [ghi chú phạm vi](SCOPE_NOTES.md).

> Trình tự triển khai: chốt phạm vi và nguồn → A1 → A2 → A3 → A4 → kiểm tra chéo và bàn giao. Các bước tạo spec, sinh hình, render và lint ở cuối tài liệu là cổng chất lượng lặp lại cho **từng** sơ đồ, không chờ tới khi vẽ xong cả bốn actor.

## Mục tiêu và quy ước

- Vẽ class diagram draw.io cho toàn bộ chức năng **còn hiệu lực** của A1 (sinh viên), A2 (giảng viên), A3 (quản trị viên), A4 (hệ thống tự động).
- Truy vết 1:1 nghĩa là mỗi chức năng/tiêu chí còn hiệu lực trong user story có đúng một dòng đối chiếu tới ít nhất một class diagram và lớp/phương thức thể hiện nó. Một class diagram có thể bao phủ nhiều chức năng liên quan; một chức năng đi qua nhiều package có thể tham chiếu nhiều sơ đồ.
- Class diagram mô tả lớp, interface, thành viên và quan hệ trong thiết kế/mã nguồn. Actor xác định phạm vi thư mục và bảng truy vết, không vẽ hình actor trong class diagram.
- Nguồn ưu tiên: `01-rd/req/user_stories/a{1..4}_*.md` và `01-rd/req/F*.md` cho phạm vi; `03-dd/logic/F*.md`, `03-dd/api/F*.md`, `05-coding/` cho tên lớp, thành viên và quan hệ. Mọi suy luận phải ghi `[SoT: Suy luận]` trong spec và bảng truy vết.
- Tách theo một package hoặc một use case nếu vượt 14 lớp hoặc 22 quan hệ; không thu nhỏ chữ để nhét thêm. Giữ các sơ đồ F3 đã có làm nguồn tham chiếu, không coi là đã bao phủ actor nếu chưa lập được dòng truy vết.

## Phase 0 — Chốt phạm vi và nguồn sự thật

- [x] Lập danh sách story còn hiệu lực của A1-A4; đánh dấu rõ story đã loại (`US-A2-04`, `US-A3-05`) và chức năng đã khai tử trong `SCOPE_NOTES.md`.
- [x] Đối chiếu mã `Fx-nn`; bổ sung F1-32 đã chốt vào catalog, loại mã đề xuất F1-33/F1-34 khỏi `covers`.
- [x] Lập `actor_coverage.csv`: 118 tiêu chí, có actor, story, mã chức năng, nguồn `file:line`, nhóm sơ đồ dự kiến và trạng thái.
- [x] Phân biệt trạng thái hiện thực: backend mới có 9 file Java sản phẩm ngoài `package-info.java`; các lớp nghiệp vụ cần lấy từ DD hoặc ghi suy luận. Xem `SCOPE_NOTES.md`.

**Self-test:** Không còn tiêu chí còn hiệu lực nào thiếu dòng; không có mã đã khai tử được tính là đang triển khai; mọi dòng có nguồn tồn tại và mức độ bằng chứng.

## Phase 1 — Thiết kế bộ sơ đồ

- [x] Gom thành 26 nhóm use case dự kiến trong `diagram_manifest.csv`: A1 9, A2 8, A3 4, A4 5. Thư mục theo actor: A1 `user/`, A2 `gv/`, A3 `admin/`, A4 `system/`.
- [x] Với từng sơ đồ, xác định lớp/interface/enum, các thành viên có ý nghĩa thiết kế, quan hệ và bội số từ DD/code.
- [x] Ghi danh sách chức năng được biểu diễn trong `covers`; không gán `covers` cho một mã chỉ được nhắc trong ghi chú.
- [x] Kiểm tra trước ngân sách: tối đa 14 lớp kể cả `externals`, 22 quan hệ; mỗi hình đúng một package hoặc một use case.
- [x] Soát riêng quyền của màn hình shared: chức năng chung của A2 và A3 phải có dòng truy vết ở cả hai actor.

**Self-test:** Từ `actor_coverage.csv` có thể đi tới sơ đồ và lớp/phương thức; từ mỗi `covers` có thể đi ngược về story hoặc yêu cầu gốc. Không có lớp, phương thức hay quan hệ vô nguồn.

## Phase 2 — A1: Sinh viên / người dùng cuối

- [x] Chia theo luồng nghiệp vụ: tài khoản và hồ sơ; tìm/lưu bài toán và lớp học; chạy thử/nộp bài/xem kết quả; phân tích AI và phỏng vấn; ngân hàng câu hỏi và tiến độ.
- [x] Với từng luồng, đối chiếu story A1 và các mã F1-F6 với DD rồi vẽ lớp backend thực sự tham gia; lớp dùng chung được tham chiếu, không tạo bản sao sai khác theo actor.
- [x] Sinh spec, `.drawio`, PNG cho từng sơ đồ; cập nhật dòng truy vết A1 ngay khi hoàn thành hình đó.

**Self-test A1:** Mọi tiêu chí A1 còn hiệu lực có điểm đến cụ thể trong bảng truy vết; tên/phương thức có nguồn hoặc nhãn suy luận; các ràng buộc như không lộ testcase ẩn và chỉ phân tích bài Accepted thể hiện đúng nơi sở hữu logic.

## Phase 3 — A2: Giảng viên

- [x] Chia theo luồng: soạn/xuất bản bài toán; testcase và AI sinh testcase; lớp học/giao bài/tiến độ/bài nộp; bảo trì ngân hàng câu hỏi.
- [x] Kiểm tra quyền và phạm vi tác giả/lớp trong các lớp dịch vụ; chức năng màn hình shared với A3 phải truy vết cho cả hai actor.
- [x] Sinh spec, `.drawio`, PNG và cập nhật từng dòng A2.

**Self-test A2:** Không có chức năng đã loại (`US-A2-04`) được vẽ như đang hoạt động; quan hệ giữa quản lý nội dung, lớp học và chấm bài bám đúng ranh giới F1/F2/F4/F6.

## Phase 4 — A3: Quản trị viên

- [x] Chia theo luồng: tài khoản và ma trận quyền; nhật ký; giám sát máy chấm/cấu hình ngôn ngữ; cấu hình, sử dụng và ngân sách AI.
- [x] Đối chiếu riêng các quyền quản trị, audit và `F1-32` với nguồn mới nhất trước khi chốt sơ đồ.
- [x] Sinh spec, `.drawio`, PNG và cập nhật từng dòng A3.

**Self-test A3:** Không đưa luồng chấm lại hàng loạt đã loại (`US-A3-05`) vào hình; mỗi thao tác cấu hình/quản trị có chủ sở hữu và quan hệ audit đúng DD.

## Phase 5 — A4: Hệ thống tự động

- [x] Chia theo xử lý nền: harness đa ngôn ngữ; worker chấm bài và trạng thái realtime; bảo vệ testcase ẩn; AI review/phỏng vấn; đối chiếu câu trả lời ngân hàng câu hỏi.
- [x] Tái sử dụng hai sơ đồ F3 mẫu sau khi đối chiếu lại với DD; bổ sung sơ đồ theo package/use case nếu chưa phủ hết A4.
- [x] Sinh spec, `.drawio`, PNG và cập nhật từng dòng A4.

**Self-test A4:** Thể hiện đúng hàng đợi bất đồng bộ, chấm hết testcase, chống ghi trùng, ranh giới dữ liệu ẩn và luồng AI suy giảm có kiểm soát; không biến tác vụ tự động thành thao tác của người dùng.

## Cổng thực thi lặp lại cho từng sơ đồ — Tạo artifact bằng skill drawio

- [x] Tạo `<ten>.spec.json` bên cạnh từng sơ đồ tại `08-diagram/class/<actor>/`.
- [x] Dùng `class.legend: "shared"` và [chú giải chung](CHU_GIAI.md); chỉ để ghi chú riêng của sơ đồ trong `notes[]`. Chỉ dùng `"inline"` nếu PNG được gửi độc lập.
- [x] Chạy `.claude/skills/drawio-uml-architect/scripts/gen_drawio.py` để sinh `<ten>.drawio`; không viết XML bằng tay.
- [x] Chạy `render_drawio.py` để sinh `<ten>.png` và mở xem từng ảnh.
- [x] Sửa lỗi hình bằng cách sửa spec rồi sinh lại: chữ bị cắt, nhãn đè đường, mũi tên chồng, lớp lạc cụm, hình quá rộng.
- [x] Ghi trong `notes` phần đã gộp/lược bỏ (getter, setter, plumbing) và mọi `[SoT: Suy luận]`.

**Self-test:** Mỗi spec sinh lại được `.drawio` hợp lệ; đủ bộ ba spec/drawio/png; bố cục đọc được ở kích thước xuất báo cáo (A4 hoặc A3 được ghi rõ).

## Phase 6 — Kiểm tra 1:1 và nghiệm thu toàn bộ

- [x] Chạy `lint_drawio.py` cho **từng** `.drawio`: 0 `FAIL`; xử lý hoặc ghi rõ mọi `WARN`.
- [x] Chạy `check_catalog.py`; nếu `covers` làm thay đổi catalog, dùng `--fix`, sau đó chạy lại để bảo đảm đối chiếu sạch.
- [x] Kiểm tra tự động: mọi dòng active trong `actor_coverage.csv` có sơ đồ tồn tại và lớp/phương thức được nêu có trong spec; mọi `covers` hợp lệ; không thiếu actor hay story.
- [x] Kiểm tra bằng mắt toàn bộ PNG, đặc biệt đường quan hệ, nhãn, bội số, dấu `interface`/`abstract` và chữ tiếng Việt.
- [x] Soát `git diff` để chắc chắn chỉ có artifact, bảng truy vết và cập nhật catalog liên quan; chạy `detect_changes()` trước khi commit nếu có commit.

**Điều kiện hoàn thành:** A1-A4 đều đạt 100% tiêu chí còn hiệu lực được truy vết, toàn bộ sơ đồ qua lint 0 `FAIL`, mọi ảnh đã xem bằng mắt, catalog đồng bộ, phần suy luận/giới hạn được ghi rõ. Bàn giao đường dẫn spec, drawio, PNG và hướng dẫn mở bằng diagrams.net hoặc VS Code Draw.io Integration.
