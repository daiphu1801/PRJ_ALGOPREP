# Kế hoạch cải tiến sequence diagram theo kiến trúc AlgoPrep

**Trạng thái 2026-10-28:** Hoàn tất migration 63/63 trang gốc thành 94 trang mới (A1 36, A2 15, A3 30, shared 13). Mỗi trang có spec, `.drawio` và PNG 2x. Bộ kiểm migration: 0 lỗi; catalog Markdown/HTML khớp 113 mã chức năng và 182 sơ đồ. Một số trang nhiều lifeline cần khổ A3 khi in.
**Phạm vi gốc:** 63 trang sequence hiện có: A1 `user/` 21 trang (7 luồng), A2 `gv/` 12 trang (7 luồng), A3 `admin/` 24 trang (10 luồng), và `shared/` 6 trang (2 luồng dùng chung A2/A3). Tác vụ tự động được mô tả ở nơi chúng thực sự khởi phát (hàng đợi, lịch chạy, sự kiện), không giả làm thao tác của actor người dùng.

## Mục tiêu và nguyên tắc

- Giữ câu chuyện nghiệp vụ đọc được bằng tiếng Việt; đồng thời chỉ rõ **ai thực hiện mỗi bước và ranh giới kiến trúc nào được đi qua**. Tên lifeline có thể là tên lớp/interface kỹ thuật; nhãn mũi tên dùng lời tự nhiên.
- Bám `01-rd/system/SYS0101_backend_architecture.md`: modular monolith, bốn tầng `presentation / application / domain / infrastructure`, CQRS, ports and adapters; không áp mẫu MVC `Controller → Service → Repository → DB` cho mọi luồng.
- Đường ghi điển hình: actor → controller → cổng vào/use case (có thể ghi ở stereotype hoặc lời gọi) → command handler → domain khi có bất biến nghiệp vụ → output port (`*Repository`, publisher, gateway) → adapter → hệ ngoài/DB. Đường đọc: controller → query handler → read port → persistence adapter → DB/read model; không ép đi qua aggregate.
- Chỉ tách lifeline cho một port/adapter khi ranh giới đó giải thích một quyết định quan trọng của luồng. Tránh vẽ mọi interface và mọi phương thức vào một hình; nếu vượt ngân sách 10 lifeline hoặc hình khó đọc, tách theo pha nghiệp vụ và liên kết các trang.
- Tên cụ thể lấy từ DD/BD và code hiện có; lớp chưa hiện thực phải ghi **thiết kế dự kiến** hoặc `[SoT: Suy luận]` trong spec. DB là nguồn kiểm tra dữ liệu/trạng thái; sequence chỉ ghi trường có ý nghĩa với lời gọi, không thay ERD hay class diagram để liệt kê mọi cột.
- Giữ `covers` và truy vết yêu cầu; không biến mã chức năng đã loại thành luồng đang hoạt động. Mọi thay đổi về phạm vi hình phải cập nhật catalog.

## Phase 0 — Chốt baseline và bảng truy vết

- [x] Lập manifest baseline 63 trang hiện có với actor, luồng, số trang, `covers`, ghi chú nguồn và ánh xạ đầu ra; hai mươi mốt trang đã cập nhật trạng thái.
- [ ] Phân loại đủ 63 trang thành đường ghi/đọc/bất đồng bộ và xác nhận nguồn RD/BD/DD/DB hiện hành.
- [ ] Đối chiếu tài liệu hiện hành và các quyết định mới với spec cũ; đánh dấu chỗ sai hoặc cũ, đặc biệt `go-judge`, hàng đợi, outbox, sự kiện miền và quyền sở hữu schema.
- [ ] Phân biệt tên đã có trong code, tên chốt ở DD và tên suy luận. Backend hiện là scaffold, nên không gắn nhãn lớp nghiệp vụ là “đã hiện thực”.
- [ ] Chốt quy tắc trang: giữ cùng một nhóm luồng gốc; tách/gộp trang chỉ khi giúp đọc tốt hơn và có ánh xạ trang cũ → trang mới.

**Self-test:** 63/63 trang có dòng manifest; mọi `covers` và mọi trang gốc có điểm đến; không có nguồn dẫn tới file không tồn tại.

## Phase 1 — Cải tiến skill drawio cho sequence

- [x] Sửa phần sequence của `.claude/skills/drawio-uml-architect/SKILL.md`; thêm `references/sequence-architecture.md` cho `command`, `query`, `async`, `cross-module`.
- [x] Quy định cách hiện input port, handler, domain model, output port, adapter và datastore theo đúng vị trí Repository/ReadPort.
- [x] Quy định luồng xuyên module đi qua cổng do module gọi sở hữu hoặc sự kiện miền.
- [x] Quy định bất đồng bộ và thời điểm trả lời; pilot A1 đã tách commit trước hàng đợi.
- [x] Giữ nhãn tiếng Việt và thêm quy tắc nhãn lifeline ngắn có ánh xạ tên đầy đủ trong spec.
- [x] Chạy `skill-creator/scripts/quick_validate.py` với `PYTHONUTF8=1`: hợp lệ.
- [x] Thử quy tắc mới trên đường đọc tiến độ lớp: bỏ bảng tính sẵn đã bị DD loại, thêm QueryHandler/ReadPort/Adapter và cổng F1→F2/F4.
- [x] Tạo [chú giải chung](CHU_GIAI.md) cho vai trò lifeline, port/adapter và kiểu mũi tên.

**Self-test:** Người vẽ có thể quyết định lifeline nào cần hiện mà không đoán kiến trúc; ba mẫu thử không tạo lời gọi trực tiếp Controller/Handler → DB hoặc phụ thuộc chéo module sai luật. Skill vẫn áp dụng được cho sơ đồ use case/class/ERD hiện có.

## Phase 2 — Pilot ba actor và shared

- [x] A1: đã vẽ lại đủ ba trang gốc thành 1a/1b, 2a/2b và 3a/3b: lưu bài, hàng đợi, worker, biên dịch, chấm từng ca, realtime và chốt kết quả.
- [x] A2: đã vẽ lại đủ ba trang tạo lớp, tạo mã mời và ghi danh; luồng đọc tiến độ/hồ sơ lớp cũng đã cập nhật.
- [x] A3: vẽ lại **thêm tài khoản** thành hai trang: lưu tài khoản/audit và gửi email.
- [x] Shared: vẽ lại đủ ba trang **quản lý testcase** thành 8 hình: cấp URL tạm, upload trực tiếp, xác nhận và lưu lô, sửa testcase, tạo snapshot, AI sinh script, chạy đáp án mẫu và lưu nháp. Đã sửa sai khác lớn giữa spec cũ và DD mới về MinIO, điều kiện tăng phiên bản và AI.
- [x] Đã mở xem 22 PNG pilot; các nhãn lifeline dài được rút ngắn và ánh xạ lại trong spec. Hình nhiều lifeline cần khổ A3 khi in. Có [gallery review](README.md).

**Self-test:** Lint 0 `FAIL`; mở được `.drawio`; PNG rõ chữ tiếng Việt và không đè nhãn; không có lời gọi tắt xuyên tầng; thứ tự request/response/async đúng nguồn. Nếu pilot không qua, sửa skill trước khi nhân rộng.

## Phase 3 — A1: Sinh viên/người dùng (21 trang gốc, 7 luồng)

- [x] Tài khoản: đăng ký/đăng nhập, liên kết OAuth, quên mật khẩu OTP — chỉ rõ controller, handler, port xác thực/lưu dữ liệu, adapter email/OAuth/Redis khi có.
- [x] Nhóm đăng ký/đăng nhập đã thay 3 trang cũ thành 8 trang: tạo tài khoản chờ xác minh, phát/nhập OTP email, rate limit, kiểm trạng thái, cấp phiên và xoay vòng refresh token. Actor ở cột trái nhất theo skill.
- [x] Nhóm OAuth đã thay 3 trang cũ thành 4 trang: khởi tạo trạng thái, kiểm callback, lấy hồ sơ có email đã xác minh, liên kết tài khoản và cấp phiên. Chính sách trùng email với tài khoản tự đăng ký đang chờ xác minh cần chốt trong DD.
- [x] Nhóm quên mật khẩu đã thay 3 trang cũ thành 6 trang: tìm tài khoản, giới hạn và phát OTP, ghi audit/gửi thư, xác minh mã riêng trước khi nhập mật khẩu, kiểm lại mã, ghi mật khẩu/thu hồi phiên. Owner đã chốt 2026-10-27: tài khoản chỉ OAuth nhận email hướng dẫn, không phát OTP; DB khoá dòng và kiểm mốc đổi mật khẩu để chống dùng lại mã, lỗi DB giữ mã và TTL gốc. DD/BD và sơ đồ đã đồng bộ.
- [x] Phân tích bài giải AI (F5.1) đã thay 3 trang cũ thành 3 trang: kiểm bài đạt và sở hữu, ưu tiên báo cáo/bản dùng lại trước cổng chi phí, tách cổng F4/F2, gọi AI an toàn và ghi kết quả cùng sự kiện.
- [x] Phỏng vấn giả lập (F5.2) đã thay 3 trang cũ thành 5 trang: ba lối vào, câu mở đầu đồng bộ trước khi tạo phiên, từng lượt SSE và lịch sử bền vững, chấm kết thúc sớm theo bốn tiêu chí.
- [ ] Nộp bài và xem kết quả thời gian thực — dùng mẫu pilot; kiểm tra trạng thái, lưu bài, hàng đợi, xử lý đủ testcase và WebSocket.
- [x] Ôn tập/luyện tập AI (F6) — 3 trang gốc thành 4 trang: đọc dữ liệu soạn sẵn, kiểm hạn mức/ngân sách, adapter AI và lưu lượt luyện, tự chấm và tiến độ theo chủ đề. Nhánh ngân sách khóa không lưu câu trả lời; AI lỗi vẫn lưu lượt thất bại.
- [ ] Sinh spec, `.drawio`, PNG cho từng trang và cập nhật manifest/covers ngay khi xong luồng.

**Self-test A1:** 21 trang gốc đều có trang thay thế; kết quả trả về trước/sau xử lý nền đúng; không lộ testcase ẩn; AI không được gọi trước bước kiểm điều kiện cần thiết.

## Phase 4 — Shared và A2: Giảng viên (6 + 12 trang gốc)

- [x] Hoàn thiện shared: soạn đề/đặc tả hàm (3 trang gốc thành 5 trang), quản lý testcase (3 trang gốc thành 8 trang); đã kiểm ranh giới F2/F3, yêu cầu xuất bản và sự kiện sau commit.
- [x] Hoàn thiện A2: tạo/sửa/xóa lớp, tạo mã mời, gỡ học viên, giao/gỡ bài, xem tiến độ và bài nộp (12 trang gốc thành 15 trang mới).
- [ ] Tách rõ command path và query path; dữ liệu bài nộp/tiến độ từ context khác phải đi qua read port hoặc event/projection theo BD/DD.

**Self-test A2/shared:** 18 trang gốc đều có trang thay thế; owner của lớp và quyền soạn đề đúng; trang shared truy vết được từ cả A2 lẫn A3; không có application gọi thẳng persistence của module khác.

## Phase 5 — A3: Quản trị viên (24 trang gốc, 10 luồng)

- [x] Tài khoản và phân quyền: thêm tài khoản, đặt lại mật khẩu, khóa/mở khóa, đổi vai trò, đổi ma trận quyền; thể hiện kiểm quyền, transaction và audit ở owner đúng.
- [x] Cấu hình và vận hành: ngôn ngữ/giới hạn, prompt AI, hạn mức AI, ngân sách token, giám sát hàng đợi; tách command/query và job/consumer khi cần.
- [x] Với hình nhiều nhánh hoặc nhiều nguồn dữ liệu, tách thành trang “cấu hình”, “áp dụng”, “giám sát” thay vì nén chữ hoặc vẽ một Service làm hết.

**Self-test A3:** 24 trang gốc đều có trang thay thế; thao tác cấu hình có nơi lưu/đọc và cơ chế áp dụng rõ; giám sát hàng đợi không bị vẽ như trực tiếp sửa DB; không phục hồi chức năng đã loại.

## Phase 6 — Kiểm tra chéo và bàn giao

- [ ] Chạy `gen_drawio.py`, `render_drawio.py`, `lint_drawio.py` cho mọi spec mới; xử lý mọi `FAIL` và giải thích `WARN` còn lại.
- [ ] Soát bằng máy: đủ spec/drawio/png, `covers` hợp lệ, không có literal `?` thay dấu tiếng Việt, không có tên lifeline trôi giữa các trang của cùng use case, không có trang gốc thiếu ánh xạ.
- [ ] Soát bằng mắt toàn bộ PNG ở cỡ xem/in dự kiến; kiểm mũi tên, alt/loop, số thứ tự, chữ, và đường đi qua các ranh giới thật.
- [ ] Chạy `check_catalog.py`; cập nhật README gallery và một file chú giải chung. Mỗi spec chỉ giữ ghi chú riêng: điều đã gộp/lược bỏ, nguồn và phần suy luận.
- [ ] Đối chiếu lần cuối với class diagram và DB: tên aggregate/port, trạng thái, dữ liệu quan trọng, owner schema. Ghi sai khác BD/DD chưa giải quyết thành issue rõ ràng, không tự chọn một bên.
- [ ] Xem `git diff` để giữ nguyên thay đổi không liên quan đang có trong workspace. Nếu có commit sau này, chạy GitNexus `detect_changes()` trước commit.

**Điều kiện hoàn thành:** 63/63 trang gốc có trang thay thế hoặc quyết định gộp có truy vết; mọi sơ đồ mới lint 0 `FAIL`, có PNG đã kiểm bằng mắt; các luồng đọc/ghi/bất đồng bộ và ranh giới module khớp kiến trúc; catalog và gallery đồng bộ; người đọc phân biệt được lớp đã hiện thực với lớp thiết kế dự kiến.

## Kết quả hoàn tất 2026-10-28

- [x] Cả 63 trang baseline có ánh xạ thay thế trong manifest, đủ spec, bản vẽ và PNG.
- [x] Rà soát lại nguồn DD và quyết định owner cho các luồng A1, A2, A3 và shared.
- [x] Kiểm tra migration toàn bộ: 63/63 trang gốc, 94/94 trang mới, 0 lỗi.
- [x] Đối chiếu catalog Markdown/HTML: 113 mã chức năng, 182 sơ đồ.
- [ ] Khi đưa vào báo cáo in, chọn khổ A3 cho các sơ đồ có cảnh báo bề ngang A4.
