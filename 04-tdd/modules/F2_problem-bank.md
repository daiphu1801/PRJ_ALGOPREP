# TDD — Tiêu chí nghiệm thu module `problem-bank` (F2)

> Mỗi `AC-nn` map về đúng một endpoint (`03-dd/api/F2_problem-bank.md`, ký hiệu `§n` là số mục ở file đó) hoặc một khối quy tắc `BR-nn` (`03-dd/logic/F2_problem-bank.md`).
> Viết cùng lúc với DD theo `.claude/rules/tdd-mode.md`. Quy tắc kiểm trường ở `03-dd/validation/F2_problem-bank.md`. Cập nhật: 2026-10-23.
> Ký hiệu vai trò: A1 học viên, A2 giảng viên, A3 quản trị viên. Mọi AC có nghĩa "phản hồi lỗi mang đúng `error.code`" khi nêu mã `PRB-nnn`.

## 1. Phạm vi

Tiêu chí nghiệm thu ngân hàng bài toán, testcase, danh mục, giao bài và sự kiện miền; đối chiếu `03-dd/api/F2_problem-bank.md` và các luồng DD cùng module.

## 2. Danh sách tiêu chí

## Học viên: danh sách, chi tiết, testcase mẫu

### AC-01 — Danh sách bài chỉ gồm bài đã xuất bản (§2, BR-01)
- **Tiền điều kiện**: kho có bài `PUBLISHED`, bài `UNPUBLISHED` và bài `deleted = true`.
- **Hành động**: học viên gọi `GET /problems` không tham số.
- **Kết quả mong đợi**: `200`, chỉ bài `PUBLISHED` và không `deleted`, tối đa 12 dòng; gõ đúng mã của bài nháp vào `q` cũng không thấy bài đó.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §2, BR-01; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-02 — Bộ lọc cộng dồn và tham số lạ (§2)
- **Tiền điều kiện**: có bài ở nhiều độ khó, chủ đề; học viên đã giải một số bài.
- **Hành động**: gọi với `difficulty` + `topic` + `status=SOLVED`; rồi gọi với `difficulty=khong-co`, `sort=abc`, `page=-4`.
- **Kết quả mong đợi**: lần một chỉ trả bài thoả cả ba điều kiện; lần hai trả `200`, dùng mặc định, không lỗi.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §2; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-03 — Danh sách vẫn hiện khi `identity` lỗi (§2, BR-23)
- **Tiền điều kiện**: cổng trạng thái giải của `identity` không đáp ứng.
- **Hành động**: gọi `GET /problems`.
- **Kết quả mong đợi**: `200`, `solveState = null`, `solveStateAvailable = false`, bảng đủ dữ liệu; bộ lọc `status` bị bỏ qua.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §2, BR-23; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-04 — Tỉ lệ AC chưa có dữ liệu (§2, §6)
- **Tiền điều kiện**: bài chưa ai nộp (không có dòng `problem_stats`).
- **Hành động**: gọi danh sách và chi tiết bài.
- **Kết quả mong đợi**: `acRate = null` (không phải `0`); trang vượt số trang thì trả trang cuối.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §2, §6; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-05 — Bài ngẫu nhiên (§3)
- **Tiền điều kiện**: bộ lọc `status=NOT_SOLVED` còn bài chưa giải.
- **Hành động**: gọi `GET /problems/random?status=NOT_SOLVED`; rồi gọi với bộ lọc không còn bài.
- **Kết quả mong đợi**: lần một trả `problemId` của bài chưa giải; lần hai `200 { problemId: null }`, không lỗi.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §3; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-06 — Tiến độ theo chủ đề (§4)
- **Tiền điều kiện**: chủ đề "Mảng" có 5 bài xuất bản, học viên đã giải 2; một bài khác của chủ đề đã bị rút.
- **Hành động**: gọi `GET /topics/progress`.
- **Kết quả mong đợi**: "Mảng" có `solved = 2`, `total = 5`; bài đã rút không tính vào cả hai số.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §4; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-07 — Bài tập lớp của học viên (§5)
- **Tiền điều kiện**: học viên thuộc lớp A và B; lớp A có 2 bài đang giao, 1 bài đã gỡ; lớp C không thuộc học viên.
- **Hành động**: gọi `GET /me/class-assignments`.
- **Kết quả mong đợi**: hai nhóm (A, B) theo bài giao gần nhất; bài đã gỡ và lớp C không xuất hiện; không có tham số `classId` nào được nhận.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §5; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-08 — Chi tiết bài cho màn giải (§6, BR-01)
- **Tiền điều kiện**: bài đã xuất bản có 3 ví dụ mẫu, đã có bookmark của học viên.
- **Hành động**: gọi `GET /problems/{id}/workspace`.
- **Kết quả mong đợi**: có đề, ràng buộc, ví dụ theo `position`, `bookmarked = true`, không có trường testcase, đáp án mẫu, `aiBrief`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §6; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-09 — Bài không thuộc phạm vi coi như không tồn tại (§6, §7, §8, BR-01)
- **Tiền điều kiện**: bài `UNPUBLISHED`, bài `deleted`, một id ngẫu nhiên.
- **Hành động**: học viên gọi `workspace`, `overview`, `sample-testcases` cho từng id.
- **Kết quả mong đợi**: cả ba trả `404 PRB-010` với thân phản hồi giống nhau.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §6, §7, §8, BR-01; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-10 — Tổng quan bài (§7)
- **Tiền điều kiện**: bài đã xuất bản có 6 testcase `HIDDEN` đã duyệt và 2 nháp AI chưa duyệt.
- **Hành động**: gọi `GET /problems/{id}/overview`.
- **Kết quả mong đợi**: `hiddenTestcaseCount = 6`; không có bất kỳ trường nào của testcase ẩn; ví dụ mẫu có `explanationMd`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §7; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-11 — Testcase mẫu không lộ testcase ẩn (§8, BR-13)
- **Tiền điều kiện**: bài có 2 Sample đã duyệt, 1 Sample nháp chưa duyệt, 6 Hidden; một Sample lưu `MINIO`.
- **Hành động**: học viên gọi `GET /problems/{id}/sample-testcases`.
- **Kết quả mong đợi**: đúng 2 phần tử, mỗi phần tử có mã testcase (`testcaseId`) khớp mã mà chấm bài trả cho cùng testcase; Sample `MINIO` có `inputDownloadUrl` hiệu lực ngắn và `input = null`; không có nháp, không có Hidden, không có `object_key` hay URL nào của Hidden.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §8 (`testcaseId`), BR-13; [SoT: 03-dd/logic/F2_problem-bank.md].

## Học viên: bookmark

### AC-12 — Lưu, sửa ghi chú, Hoàn tác (§10, BR-21)
- **Tiền điều kiện**: bài đã xuất bản, chưa có bookmark.
- **Hành động**: `PUT /me/bookmarks/{id}` kèm ghi chú; sửa ghi chú với `createIfMissing=false`; xoá; rồi `PUT` với `createIfMissing=true` và ghi chú cũ.
- **Kết quả mong đợi**: `201` rồi `200` (`savedAt` không đổi); sau xoá, lần sửa với `createIfMissing=false` trả `404 PRB-014` và không tạo dòng; Hoàn tác tạo lại với ngày lưu mới.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §10, BR-21; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-13 — Ghi chú: độ dài và nội dung (§10)
- **Tiền điều kiện**: có bookmark.
- **Hành động**: gửi ghi chú 1000 ký tự, 1001 ký tự, chỉ khoảng trắng.
- **Kết quả mong đợi**: 1000 được chấp nhận; 1001 trả `400 PRB-001`; chuỗi khoảng trắng lưu `NULL`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §10; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-14 — Bỏ lưu idempotent và với bài đã rút (§11)
- **Tiền điều kiện**: có bookmark; bài sau đó bị rút xuống.
- **Hành động**: `DELETE /me/bookmarks/{id}` hai lần.
- **Kết quả mong đợi**: cả hai lần `204`; bài đã rút vẫn bỏ lưu được.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §11; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-15 — Đặt trạng thái lưu bài (§12)
- **Tiền điều kiện**: bài đã xuất bản.
- **Hành động**: `PUT /me/bookmarks/{id}/state {bookmarked:true}` hai lần liên tiếp rồi `{bookmarked:false}`; thử `true` trên bài chưa xuất bản.
- **Kết quả mong đợi**: chỉ một dòng được tạo, ghi chú sẵn có không mất; `false` xoá dòng; bài chưa xuất bản trả `404 PRB-010`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §12; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-16 — Danh sách bài đã lưu riêng tư tuyệt đối (§9, BR-21)
- **Tiền điều kiện**: học viên H có 3 bookmark, trong đó 1 bài đã rút xuống; A2 và A3 có bookmark riêng.
- **Hành động**: H gọi `GET /me/bookmarks`; A2 gọi cùng endpoint kèm tham số `userId=<H>`.
- **Kết quả mong đợi**: H thấy 3 dòng (bài đã rút có `withdrawn = true`, vẫn nằm trong `savedTotal`); A2 chỉ thấy bookmark của chính mình, tham số `userId` bị bỏ qua; không có route nào cho A2/A3 đọc bookmark người khác.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §9, BR-21; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-17 — Gợi ý bài toán (§13, BR-23)
- **Tiền điều kiện**: chủ đề yếu nhất của học viên có bài chưa giải; học viên đã giải nhiều bài ở chủ đề mạnh.
- **Hành động**: mở Dashboard, xem điểm năng lực và bài gợi ý; lặp khi dữ liệu bài nộp theo chủ đề hoặc trạng thái giải tạm lỗi.
- **Kết quả mong đợi**: tối đa bảy bài; bài chưa giải thuộc chủ đề yếu đứng trước bài đã giải. Điểm năng lực hiển thị là chính dữ liệu dùng để xếp bài, không được giao diện tính lại. Chủ đề chưa có lượt nộp không bị gán điểm không. Khi thiếu nguồn xếp hạng, bài vẫn hiện theo thứ tự dự phòng và màn báo chưa đủ dữ liệu, không hiển thị điểm giả.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §13, BR-23; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-18 — Tóm tắt kho bài (§14)
- **Tiền điều kiện**: ba mức độ khó seed, ADMIN vừa thêm mức thứ tư chưa có bài.
- **Hành động**: gọi `GET /catalog/summary`.
- **Kết quả mong đợi**: `byLevel` có 4 phần tử theo `sort_order`, mức mới `count = 0`; bài `UNPUBLISHED`/`deleted` không được đếm.

## Danh mục độ khó và chủ đề

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §14; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-19 — Đọc danh mục (§15, §20)
- **Tiền điều kiện**: A1, A2, A3 đã đăng nhập.
- **Hành động**: mỗi người gọi `GET /levels` và `GET /topics`; A2 thêm `includeCounts=true`.
- **Kết quả mong đợi**: ai cũng đọc được theo `sort_order`; `problemCount` chỉ có với ADMIN, A2 nhận `null`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §15, §20; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-20 — Thêm độ khó (§16, BR-22, BR-27)
- **Tiền điều kiện**: ADMIN đăng nhập.
- **Hành động**: `POST /levels {displayName:"Rất khó"}`; rồi gọi lại với tên "rất khó".
- **Kết quả mong đợi**: `201`, `code` slug, xếp cuối, đúng một dòng nhật ký qua `AuditLogPort`; lần hai `409 PRB-022`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §16, BR-22, BR-27; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-21 — A2 không quản lý được danh mục (§16 tới §19, §21 tới §24)
- **Tiền điều kiện**: A2 có đủ `PROBLEM_AUTHORING` bốn hành động.
- **Hành động**: A2 gọi từng endpoint ghi danh mục.
- **Kết quả mong đợi**: tất cả `403 PRB-004`, không có dữ liệu đổi, không có dòng nhật ký.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §16-§19, §21-§24; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-22 — Nhật ký cùng giao dịch (§16, BR-27)
- **Tiền điều kiện**: `AuditLogPort` được giả lập để ném lỗi.
- **Hành động**: ADMIN thêm một mức độ khó.
- **Kết quả mong đợi**: lỗi trả về, không có mức mới nào được tạo (rollback).

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §16, BR-27; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-23 — Đổi tên độ khó (§17)
- **Tiền điều kiện**: mức `HARD` tên "Khó".
- **Hành động**: ADMIN `PATCH` đổi tên thành "Nâng cao"; rồi gửi lại cùng tên.
- **Kết quả mong đợi**: `code` vẫn `HARD`, một dòng nhật ký có giá trị cũ và mới; lần hai `200` không thêm nhật ký.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §17; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-24 — Sắp xếp lại độ khó (§18, §23)
- **Tiền điều kiện**: ba mức.
- **Hành động**: `PUT /levels/order` với thứ tự mới đủ id; rồi thiếu một id.
- **Kết quả mong đợi**: lần một đổi `sort_order`, cột "Độ khó" của danh sách bài sắp theo thứ tự mới; lần hai `422 PRB-049`, thứ tự cũ giữ.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §18, §23; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-25 — Xoá độ khó bị chặn (§19, BR-22)
- **Tiền điều kiện**: một mức còn 1 bài `deleted` tham chiếu; kho chỉ còn một mức khác.
- **Hành động**: ADMIN xoá mức đang được tham chiếu; sau đó xoá mức cuối cùng.
- **Kết quả mong đợi**: `422 PRB-032` với `referencingProblemCount = 1`; `422 PRB-033`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §19, BR-22; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-26 — Xoá độ khó thành công và chạy song song (§19)
- **Tiền điều kiện**: hai mức không bài, hai ADMIN cùng xoá cả hai.
- **Hành động**: hai yêu cầu xoá đồng thời.
- **Kết quả mong đợi**: đúng một yêu cầu `204`, yêu cầu kia `422 PRB-033`; còn đúng một mức.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §19; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-27 — Quản lý chủ đề (§21, §22, §24)
- **Tiền điều kiện**: ADMIN; chủ đề "Đồ thị" còn 1 bài `UNPUBLISHED` gắn vào; chủ đề "Rỗng" không bài.
- **Hành động**: thêm "Quy hoạch động"; đổi tên; xoá "Đồ thị"; xoá "Rỗng".
- **Kết quả mong đợi**: thêm `201` `code = QUY-HOACH-DONG`; đổi tên giữ `code`; xoá "Đồ thị" `422 PRB-031` kèm `referencingProblemCount = 1`; xoá "Rỗng" `204`; mỗi thao tác thành công đúng một dòng nhật ký.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §21, §22, §24; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-28 — Sắp xếp lại chủ đề (§23)
- **Tiền điều kiện**: ADMIN, danh sách chủ đề.
- **Hành động**: gửi thứ tự mới đủ id.
- **Kết quả mong đợi**: `200` trả toàn bộ danh sách đúng thứ tự mới; `ListProblemTopics` phản ánh ngay.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §23; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-29 — Slug `code` (BR-03)
- **Tiền điều kiện**: đã có bài tên "Hai số" và một bài đã `deleted` cùng tiêu đề "Two Sum".
- **Hành động**: tạo bài "Hai số", tạo bài "Two Sum", tạo chủ đề "!!!".
- **Kết quả mong đợi**: mã bài thứ hai là `HAI-SO-2`; bài "Two Sum" mới có hậu tố vì unique tính cả bài đã xoá; chủ đề toàn ký hiệu nhận mã dự phòng `TOPIC`.

## Quản lý bài toán (A2, A3)

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-03; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-30 — Danh sách quản lý theo phạm vi tác giả (§25)
- **Tiền điều kiện**: A2 soạn 3 bài, A2' soạn 2 bài, 1 bài đã `deleted`.
- **Hành động**: A2 và A3 gọi `GET /authoring/problems`.
- **Kết quả mong đợi**: A2 thấy 3 bài của mình, A3 thấy 5 bài (không có bài đã xoá); `totals` đúng theo phạm vi; sắp theo `level` đi theo `sort_order`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §25; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-31 — Tham số quản trị đọc nghiêm (§25)
- **Tiền điều kiện**: A3 đăng nhập.
- **Hành động**: gọi với `pageSize=7`; rồi `q` 101 ký tự.
- **Kết quả mong đợi**: cả hai trả `400 PRB-001`; `q` đúng 100 ký tự được chấp nhận.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §25; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-32 — Phân bố chủ đề và bài cần chú ý (§26, §27)
- **Tiền điều kiện**: 10 bài, 5 gắn "Mảng"; một bài AC 30%, một bài AC 20%, một nháp 10 ngày không sửa, một bài từng xuất bản đã rút.
- **Hành động**: gọi hai endpoint.
- **Kết quả mong đợi**: "Mảng" `count = 5`; `LOW_AC = 1` (AC đúng 30% không tính), `STALE_DRAFT = 1`, `HIDDEN_FROM_LEARNERS = 1`; không chia cho 0 khi chưa có chủ đề nào.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §26, §27; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-33 — Xuất CSV (§28)
- **Tiền điều kiện**: A3 chọn 3 bài, một tiêu đề bắt đầu bằng `=SUM(1,1)`.
- **Hành động**: `POST /authoring/problems/export` với `problemIds`.
- **Kết quả mong đợi**: CSV có dòng tiêu đề và 3 dòng, đúng 8 cột; không có đề bài, đặc tả, testcase; ô nguy hiểm được thêm `'`; gửi cả `problemIds` lẫn `filter` thì `400 PRB-001`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §28; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-34 — Xuất bản lô độc lập từng bài (§29, BR-08)
- **Tiền điều kiện**: bài P1 đủ checklist, bài P2 thiếu 3 điều kiện.
- **Hành động**: `bulk/publish [P1, P2]`.
- **Kết quả mong đợi**: `200`; P1 trong `succeededIds` (một dòng nhật ký, sự kiện `ProblemPublished`); P2 trong `failedItems` với `PRB-030`; P2 vẫn `UNPUBLISHED`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §29, BR-08; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-35 — Rút xuống không mất dữ liệu (§30, BR-08)
- **Tiền điều kiện**: bài `PUBLISHED` có lượt nộp và bookmark.
- **Hành động**: `bulk/unpublish`.
- **Kết quả mong đợi**: bài biến khỏi `GET /problems` ngay; `published_at` giữ nguyên; lượt nộp, bookmark, dòng giao bài còn nguyên; gọi lại là thành công không thêm nhật ký.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §30, BR-08; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-36 — Đổi độ khó và gán chủ đề theo lô (§31, §32)
- **Tiền điều kiện**: 3 bài; một mức vừa bị ADMIN xoá.
- **Hành động**: đổi độ khó sang mức còn tồn tại; đổi sang mức đã xoá; gán chủ đề hai lần.
- **Kết quả mong đợi**: lần một 3 bài đổi, 3 dòng nhật ký; mức đã xoá `422 PRB-039` và không bài nào đổi; gán chủ đề lần hai không nhân đôi quan hệ, không thêm nhật ký.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §31, §32; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-37 — Xoá mềm theo lô (§33, BR-09)
- **Tiền điều kiện**: bài có 50 lượt nộp, đang giao cho 2 lớp, và một bài nháp của người khác.
- **Hành động**: A3 xoá bài; A2 thử xoá bài của người khác.
- **Kết quả mong đợi**: bài `status = UNPUBLISHED`, `deleted = true`; testcase, giao bài, lượt nộp, bookmark còn nguyên; không bị từ chối vì lượt nộp hay lớp; A2 nhận `PRB-010` (bản nháp người khác) hoặc `PRB-003` (đã xuất bản) trong `failedItems`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §33, BR-09; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-38 — Lô rỗng và quá lớn (§29 tới §33)
- **Tiền điều kiện**: A3.
- **Hành động**: gọi các endpoint lô với `problemIds` rỗng; `bulk/publish` với 21 id.
- **Kết quả mong đợi**: `400 PRB-001` cho cả hai.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §29-§33; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-39 — Mở bài để soạn và quyền đọc của A2 (§34, BR-02)
- **Tiền điều kiện**: bài X của A2, bài Y `PUBLISHED` của A2', bài Z nháp của A2'.
- **Hành động**: A2 gọi `GET /authoring/problems/{id}` cho X, Y, Z.
- **Kết quả mong đợi**: X đầy đủ có nội dung testcase; Y `200` với `readOnly = true`, không nội dung testcase; Z `404 PRB-010`, thân giống id không tồn tại.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §34, BR-02; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-40 — Sửa bài người khác bị chặn (§37, §38, §39, §40, §42)
- **Tiền điều kiện**: như AC-39.
- **Hành động**: A2 gọi `PUT .../content` cho Y và Z.
- **Kết quả mong đợi**: Y `403 PRB-003`, Z `404 PRB-010`; A3 sửa được cả hai.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §37-§40, §42, BR-02; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-41 — Nhân bản (§35, §36, BR-10)
- **Tiền điều kiện**: bài nguồn có đặc tả, 6 testcase (1 `MINIO`), đáp án mẫu, 2 ví dụ.
- **Hành động**: `GET .../duplicate-source`; rồi `POST /authoring/problems` với `duplicatedFromProblemId`.
- **Kết quả mong đợi**: bước đọc không tạo bản ghi nào; bước tạo sinh bài mới `UNPUBLISHED`, mã mới có hậu tố nếu trùng, `current_testcase_set_version = 1`, `spec_version = 1`, đã sao chép đặc tả và 6 testcase (object MinIO có khoá mới), ví dụ và đáp án mẫu theo thân yêu cầu, một dòng nhật ký `PROBLEM_DUPLICATED`; A2 nhân bản bài người khác bị `403 PRB-003`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §35, §36, BR-10; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-42 — Tạo bài với giá trị mặc định (§36)
- **Tiền điều kiện**: `grading_defaults` có sẵn.
- **Hành động**: tạo bài chỉ với `title` và `statementMd`.
- **Kết quả mong đợi**: `201`; giới hạn điền từ `grading_defaults`; mức độ khó là mức đầu theo `sort_order`; `author_id` là người gọi; `maxSubmissionsPerHour = null` giữ `NULL`; hai cờ AI mặc định `true`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §36; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-43 — Lưu nội dung và khoá lạc quan (§37, BR-04)
- **Tiền điều kiện**: hai trình duyệt cùng mở bài với `updatedAt = T1`.
- **Hành động**: trình duyệt 1 lưu (`expectedUpdatedAt = T1`); trình duyệt 2 lưu với `T1`.
- **Kết quả mong đợi**: lần một `200` với `updatedAt` mới; lần hai `409 PRB-020` kèm `currentUpdatedAt`, dữ liệu lần một giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §37, BR-04; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-44 — Ví dụ mẫu và đáp án mẫu khi lưu (§37)
- **Tiền điều kiện**: bài `PUBLISHED` còn đúng 2 ví dụ; bài khác có đáp án mẫu.
- **Hành động**: gửi danh sách 1 ví dụ cho bài `PUBLISHED`; xoá chữ trong ô đáp án mẫu của bài khác; đổi mã đáp án.
- **Kết quả mong đợi**: bài `PUBLISHED` `422 PRB-053`, không đổi gì; dòng đáp án mẫu bị xoá khi ô rỗng; đổi mã đáp án không đổi `spec_version` hay `current_testcase_set_version`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §37; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-45 — Kiểm đầu vào nội dung (§36, §37)
- **Tiền điều kiện**: A3.
- **Hành động**: `title` rỗng; 256 ký tự; `timeLimitMs = 0`; `levelId` đã bị xoá.
- **Kết quả mong đợi**: ba lỗi đầu `400 PRB-001` kèm `details.fields`; `levelId` đã xoá `422 PRB-039`, không tạo bản ghi; `title` 255 ký tự được chấp nhận.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §36, §37; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-46 — Lưu đặc tả hợp lệ (§38, BR-05)
- **Tiền điều kiện**: bài chưa có đặc tả.
- **Hành động**: lưu chữ ký `twoSum(int[] nums, int target) -> int[]` với `EXACT`; rồi lưu lại đúng thân cũ; rồi chỉ đổi `stdinFormatMd`.
- **Kết quả mong đợi**: lần một `functionWrapperSupported = true`, `specVersion = 1`, phát `ProblemSpecChanged`; lần hai không đổi, không sự kiện; lần ba `specVersion = 2`; key Redis `problem:spec:<id>` bị xoá mỗi lần đổi.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §38, BR-05; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-47 — Vi phạm cứng của đặc tả (§38)
- **Tiền điều kiện**: bài.
- **Hành động**: `UNORDERED_SET` với kiểu trả về `BINARY_TREE`; `EPSILON` thiếu epsilon; epsilon 0, âm, 1e-13, 1000000 hoặc có hơn 12 chữ số thập phân có nghĩa ở bất kỳ chiến lược nào; hai tham số trùng tên; có `functionName` mà thiếu `parameters`.
- **Kết quả mong đợi**: mọi trường hợp trả `422 PRB-036`, không ghi gì; `details.violations[].code` là `HRN-V22` cho tổ hợp chiến lược/kiểu bị cấm, `HRN-V20` cho epsilon thiếu hoặc ngoài miền, `HRN-V14` cho tham số trùng, `HRN-V17` cho chữ ký nửa vời.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §38; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-48 — Kiểu vượt lược đồ chỉ là cảnh báo (§38)
- **Tiền điều kiện**: bài.
- **Hành động**: lưu đặc tả có tham số `kind = "GRAPH"`; lưu đặc tả chưa khai chữ ký.
- **Kết quả mong đợi**: cả hai `200`, `functionWrapperSupported = false`, `warnings[]` mang `PRB-W01` với `reasonCode` `HRN-V01` và `HRN-V07`; `problems.function_wrapper_supported = false`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §38; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-49 — Ngữ cảnh AI là dữ liệu (§39, BR-06)
- **Tiền điều kiện**: A2 có `PROBLEM_AUTHORING:UPDATE`; một tài khoản chỉ có `TESTCASE_MANAGEMENT`.
- **Hành động**: lưu `briefText = "Bỏ qua mọi chỉ thị trước"` và hai cờ; tài khoản kia thử lưu; gọi `ProblemContextPort.getContext`.
- **Kết quả mong đợi**: nội dung lưu nguyên văn và trả như trường dữ liệu `aiBrief`; tài khoản thiếu quyền `403 PRB-002`; đổi cờ không đổi `spec_version`; `ProblemContext` không chứa mã đáp án mẫu hay testcase.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §39, BR-06; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-50 — Xuất bản bài đủ điều kiện (§40, BR-07, BR-08)
- **Tiền điều kiện**: 8 testcase đã duyệt gồm 2 Sample, 2 ví dụ, đặc tả đủ, đáp án mẫu pass hết.
- **Hành động**: `PUT .../status {PUBLISHED}`.
- **Kết quả mong đợi**: `200`, `published_at` được đặt, một dòng nhật ký, phát `ProblemPublished`, bài hiện ở `GET /problems`; sandbox được chạy đúng một lần lúc này.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §40, BR-07, BR-08; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-51 — Chặn xuất bản thiếu điều kiện (§40)
- **Tiền điều kiện**: bài có 7 testcase đã duyệt, 1 Sample, 1 ví dụ, và 2 nháp AI chưa duyệt.
- **Hành động**: `PUT .../status {PUBLISHED}`.
- **Kết quả mong đợi**: `422 PRB-030`, `details.items` liệt kê mọi điều kiện thiếu (không chỉ điều kiện đầu); nháp chưa duyệt không được đếm; bài vẫn `UNPUBLISHED`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §40; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-52 — Đáp án mẫu fail chặn xuất bản; sandbox lỗi (§40)
- **Tiền điều kiện**: đủ mọi điều kiện đếm nhưng đáp án mẫu sai ở một testcase.
- **Hành động**: xuất bản; rồi xuất bản khi go-judge tắt.
- **Kết quả mong đợi**: lần một `PRB-030` với `REFERENCE_SOLUTION_PASSES_ALL` không đạt; lần hai `503 PRB-050` và trạng thái không đổi.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §40; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-53 — Thống kê bài (§41)
- **Tiền điều kiện**: bài có 100 lượt nộp, 40 Accepted; bài mới chưa ai nộp.
- **Hành động**: gọi `GET .../stats` cho cả hai.
- **Kết quả mong đợi**: `acRate = 40.00`; bài mới `submissionCount = 0`, `acRate = null`; A2 mở thống kê bản nháp người khác `404 PRB-010`.

## Testcase

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §41; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-54 — Thêm testcase tay và ranh giới 8 KB (§42, BR-12)
- **Tiền điều kiện**: bài `UNPUBLISHED`.
- **Hành động**: thêm testcase input đúng 8.192 byte; input 8.193 byte; input nhỏ kèm expected lớn.
- **Kết quả mong đợi**: lần một `INLINE`; lần hai `MINIO` (object ở `problems/<id>/testcases/<tc>/input.txt`); lần ba hai vế khác kiểu lưu; `origin = MANUAL`, `approved = true`, `position` là max cộng 1.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §42, BR-12; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-55 — Kiểm đầu vào testcase (§42)
- **Tiền điều kiện**: bài.
- **Hành động**: `input` rỗng; một vế 1 MB cộng 1 byte; thêm khi bài đã có 500 testcase.
- **Kết quả mong đợi**: `400 PRB-001`; `422 PRB-048`; `422 PRB-046`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §42; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-56 — Tăng phiên bản bộ testcase đúng lúc (§42, §43, §44, BR-11)
- **Tiền điều kiện**: bài `PUBLISHED`, `current_testcase_set_version = 1`.
- **Hành động**: thêm Sample; thêm Hidden; sửa Hidden đã duyệt; đổi `category`; xoá Hidden đã duyệt; sắp xếp lại.
- **Kết quả mong đợi**: với bài đang xuất bản, thêm, duyệt, sửa, xoá testcase đã duyệt (gồm mẫu và ẩn), hoặc đổi `position` qua §45, mỗi thao tác thay đổi thực sự tăng đúng một phiên bản và có bộ chấm mới bất biến trước khi công bố. `position` là một thứ tự dùng chung cho trình bày và chấm. Chỉ đổi nhãn phân loại, sửa nội dung nháp chưa duyệt, gửi lại thứ tự y hệt hoặc thao tác khi bài chưa xuất bản không tăng ngay. Tái xuất bản sau sửa phải so bộ chấm với snapshot: khác thì tăng một version, giống thì không tăng (BR-08).

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §42-§44, BR-11; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-57 — Duyệt nháp AI (§43, BR-17)
- **Tiền điều kiện**: nháp AI `HIDDEN` chưa duyệt của bài `PUBLISHED`.
- **Hành động**: `PATCH {approved:true}`; rồi `PATCH {approved:false}` trên testcase đã duyệt.
- **Kết quả mong đợi**: lần một `approved = true`, `origin` vẫn `AI`, phiên bản tăng 1, nháp được tính vào checklist; lần hai `400 PRB-001`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §43, BR-17; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-58 — Sửa testcase đổi kiểu lưu (§43)
- **Tiền điều kiện**: testcase `INLINE`.
- **Hành động**: sửa input thành 9 KB; testcase thuộc bài khác dùng id này.
- **Kết quả mong đợi**: chuyển `MINIO`, hai CHECK lưu trữ thoả; `testcaseId` của bài khác `404 PRB-011`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §43; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-59 — Xoá testcase (§44)
- **Tiền điều kiện**: testcase `MINIO`.
- **Hành động**: xoá hai lần; xoá khi MinIO lỗi lúc dọn.
- **Kết quả mong đợi**: Testcase đủ điều kiện bị xoá khỏi bài; lặp lại báo không tìm thấy. Nếu bài đang xuất bản còn đúng tám testcase được duyệt hoặc hai testcase mẫu, thao tác làm giảm dưới ngưỡng bị từ chối và bộ testcase giữ nguyên. Lỗi dọn tệp lưu trữ sau khi xoá không làm thao tác đã hoàn tất bị báo thất bại; hệ thống ghi sự kiện vận hành để xử lý.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §44; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-60 — Sắp xếp lại testcase (§45)
- **Tiền điều kiện**: bài `PUBLISHED` đủ ít nhất 8 testcase đã duyệt và 2 Sample; xét thêm bài `UNPUBLISHED` có 5 testcase.
- **Hành động**: gửi thứ tự mới chứa đủ id; gửi lại đúng thứ tự; gửi thiếu một id; thử khi ghi snapshot lỗi và khi A2 không sở hữu bài.
- **Kết quả mong đợi**: `position` đánh số lại 1..n trong một giao dịch. Đổi thứ tự thực sự trên bài `PUBLISHED` tăng phiên bản một lần, tạo snapshot mới; submission đã ghim vẫn dùng thứ tự cũ. Bài `UNPUBLISHED` và lần gửi lặp thứ tự hiện hành không tăng. Thiếu id trả `422 PRB-049`, lỗi bundle trả `503 PRB-052`, không có ghi nửa chừng; thiếu quyền trả `403 PRB-003`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §45; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-61 — Tải lên theo lô: xin URL (§46, BR-14)
- **Tiền điều kiện**: bài có 499 testcase.
- **Hành động**: xin URL cho 1 cặp; cho 2 cặp; cho 101 cặp; cho vế 65 MB.
- **Kết quả mong đợi**: 1 cặp thành công với `putUrl` hiệu lực 15 phút trỏ vào khoá do máy chủ sinh trong `problems/<id>/`; 2 cặp `422 PRB-046`; 101 cặp và vế 65 MB `422 PRB-047`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §46, BR-14; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-62 — Tải lên theo lô: xác nhận tất cả hoặc không (§47)
- **Tiền điều kiện**: phiên với 3 cặp, đã tải đủ.
- **Hành động**: xác nhận; xác nhận lần hai; thử lại với phiên thiếu một object; với phiên của người khác.
- **Kết quả mong đợi**: ba testcase hợp lệ được thêm cùng nhau; xác nhận lại không nhân đôi. Thiếu tệp hoặc phiên tải lên không hợp lệ thì không thêm testcase nào. Với bài đang xuất bản, lô có ít nhất một testcase đã duyệt, dù mẫu hay ẩn, tăng đúng một phiên bản bộ chấm và tạo snapshot mới.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §47, BR-14; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-63 — Chạy đáp án mẫu (§48, BR-15)
- **Tiền điều kiện**: bài có 8 testcase đã duyệt và một nháp chưa duyệt; đáp án mẫu đúng.
- **Hành động**: chạy không tham số; chạy với `testcaseId` của nháp; chạy với mã sai ở testcase #3.
- **Kết quả mong đợi**: lần một `allPass = true`, `total = 8`; lần hai đúng một phần tử kể cả nháp; lần ba `FAIL` `WRONG_ANSWER` ở dòng đó; phản hồi **không** có `actualOutput` hay diff, kể cả testcase Hidden.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §48, BR-15; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-64 — Chạy đáp án mẫu: lỗi và thiếu (§48)
- **Tiền điều kiện**: bài.
- **Hành động**: chạy khi chưa có đáp án mẫu và không kèm mã; mã lỗi biên dịch; go-judge tắt; bài chưa có testcase đã duyệt.
- **Kết quả mong đợi**: `422 PRB-040`; `compileError` có nội dung và mọi dòng `COMPILE_ERROR`; `503 PRB-050`; `total = 0`, `allPass = false`.

## Sinh testcase AI (F2-14)

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §48; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-65 — Sinh testcase thành công (§49, BR-16)
- **Tiền điều kiện**: có ràng buộc dữ liệu, 2 Sample tự viết đã duyệt, đáp án mẫu pass hết, `used < limit`.
- **Hành động**: `POST .../testcases/generate`.
- **Kết quả mong đợi**: `added > 0`; mọi testcase mới `origin = AI`, `approved = false`, `HIDDEN`, có `category`; output là kết quả chạy đáp án mẫu thật; `used` tăng đúng 1; một dòng `testcase_generation_runs` chứa script và hạt giống; nháp không vào checklist, không vào `loadGradingSpec`, không tăng phiên bản.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49, BR-16; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-66 — Điều kiện tiên quyết (§49)
- **Tiền điều kiện**: lần lượt: ràng buộc dữ liệu rỗng; chỉ 1 Sample tự viết; đáp án mẫu fail một testcase; chưa có đáp án mẫu.
- **Hành động**: gọi sinh.
- **Kết quả mong đợi**: `422 PRB-041` với `reasons` lần lượt `NO_CONSTRAINTS`, `NOT_ENOUGH_MANUAL_SAMPLES`, `REFERENCE_NOT_PASSING`; trường hợp cuối `422 PRB-040`; không gọi AI, không tính lượt.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-67 — Hạn mức số lần sinh (§49, §57)
- **Tiền điều kiện**: `max_ai_generations_per_problem = 2`, bài đã dùng 2 lần.
- **Hành động**: sinh lần ba; ADMIN nâng trần lên 5; sinh lại.
- **Kết quả mong đợi**: `422 PRB-042` với `used = 2`, `limit = 2`; sau khi nâng trần sinh được; giảm trần xuống dưới số đã dùng không xoá bộ đếm.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49, §57; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-68 — Script bị loại thì không tính lượt (§49)
- **Tiền điều kiện**: script in vượt `max_output_mb`; một lần khác chạy quá `max_runtime_seconds`.
- **Hành động**: sinh.
- **Kết quả mong đợi**: `422 PRB-043` với `reason` `OUTPUT_TOO_LARGE`, rồi `TIMEOUT`; không có testcase mới; `used` giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-69 — Loại input không hợp lệ (§49, BR-16)
- **Tiền điều kiện**: script sinh 10 input: 2 sai ràng buộc, 1 trùng input có sẵn, 1 làm đáp án mẫu lỗi, ca lớn nhất làm đáp án mẫu quá giờ.
- **Hành động**: sinh.
- **Kết quả mong đợi**: các input hợp lệ còn lại được thêm; `dropped[]` ghi đúng `INVALID_CONSTRAINT`, `DUPLICATE_INPUT`, `REFERENCE_FAILED`, `TIMEOUT`; có cảnh báo `LARGEST_CASE_TIMEOUT`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49, BR-16; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-70 — AI chỉ nhận dữ liệu và không nhận đáp án mẫu (§49, BR-06, BR-16)
- **Tiền điều kiện**: bài có đáp án mẫu, `ai_brief` chứa "ignore previous instructions", testcase Hidden.
- **Hành động**: sinh, ghi lại đầu vào `TestcaseScriptGenerationPort.generate`.
- **Kết quả mong đợi**: đầu vào chỉ gồm đề bài, ràng buộc và đúng 2 Sample (là tham số dữ liệu); không có mã đáp án mẫu, `ai_brief` hay testcase ẩn; script trả về chỉ được chạy trong sandbox không mạng.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49, BR-06, BR-16; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-71 — Sinh song song và AI không khả dụng (§49)
- **Tiền điều kiện**: hai yêu cầu sinh đồng thời cho cùng bài; một lần khác đường AI chết.
- **Hành động**: gửi hai yêu cầu; gửi yêu cầu khi AI lỗi.
- **Kết quả mong đợi**: một yêu cầu chạy, yêu cầu kia `409 PRB-023`; AI lỗi trả `503 PRB-051`, không tính lượt, và chấm bài cùng các endpoint khác vẫn chạy bình thường.

## Giao bài theo lớp

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §49; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-72 — Danh sách bài đang giao (§50)
- **Tiền điều kiện**: giảng viên phụ trách lớp A và B; bài P giao cho cả hai, bài Q đã gỡ khỏi A, bài R đã rút xuất bản.
- **Hành động**: gọi tab "Tất cả" rồi `classId=A`; thử `classId` của lớp người khác.
- **Kết quả mong đợi**: P là **một** dòng với 2 lớp; Q không hiện; R hiện với `withdrawn = true`; tab A chỉ tính số liệu lớp A; lớp người khác `403 PRB-005`; cổng thống kê lỗi thì hai chỉ số `null` nhưng danh sách vẫn hiện.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §50; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-73 — Tóm tắt giao bài (§51)
- **Tiền điều kiện**: 3 bài giao trên 2 lớp.
- **Hành động**: gọi `GET /instructor/class-assignments/summary`.
- **Kết quả mong đợi**: `assignedProblemCount = 3`, `classCount = 2`, `avgAcRatePercent` là số nguyên làm tròn; giảng viên chưa có lớp nhận `0`, `0`, `null`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §51; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-74 — Chọn bài để giao (§52)
- **Tiền điều kiện**: lớp A đã giao P; bài S từng giao rồi gỡ; có bài nháp và bài đã xoá.
- **Hành động**: gọi `assignable-problems` của lớp A.
- **Kết quả mong đợi**: có S và các bài xuất bản chưa giao; không có P, không có bài nháp/đã xoá; lớp không phụ trách `403 PRB-005`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §52; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-75 — Giao bài (§53, BR-18)
- **Tiền điều kiện**: 3 bài xuất bản, 1 bài `UNPUBLISHED`.
- **Hành động**: giao 4 bài vào lớp A; giao lại ngay; hai giảng viên giao đồng thời; gây lỗi ghi Outbox riêng cho một bài trong lô rồi thử lại sau phục hồi.
- **Kết quả mong đợi**: 3 bài vào `succeededIds` (3 sự kiện `ProblemAssignedToClass`), bài nháp `PRB-037` trong `failedItems`; lần giao lại vào `skippedIds`; đồng thời chỉ có một dòng hoạt động cho mỗi `(bài, lớp)`; 51 id bị `400 PRB-001`. Khi Outbox của một bài lỗi, response lô vẫn `200`, bài đó vào `failedItems` với `PRB-056`, không có assignment/event; các bài khác vẫn thành công. Thử lại sau phục hồi giao bài lỗi đúng một lần.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §53, BR-18; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-76 — Gỡ bài khỏi lớp giữ lịch sử (§54)
- **Tiền điều kiện**: bài đang giao cho lớp A, học viên đã nộp bài này.
- **Hành động**: gỡ; gỡ lần hai; thử lớp không phụ trách.
- **Kết quả mong đợi**: Bài không còn trong danh sách giao hiện hành và mẫu số tiến độ lớp từ lúc gỡ; lượt nộp, điểm và lịch sử học tập cũ vẫn xem được. Thao tác gỡ phát thay đổi một lần; lặp lại báo bài đã gỡ mà không đổi mốc cũ; giảng viên ngoài lớp bị từ chối.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §54, BR-18; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-77 — Năm bài giao nhiều lượt nộp nhất (§55)
- **Tiền điều kiện**: giảng viên có 7 bài giao.
- **Hành động**: gọi `GET /instructor/assigned-problems/top`; khi cổng thống kê lỗi.
- **Kết quả mong đợi**: đúng 5 bài xếp theo lượt nộp giảm dần; cổng lỗi trả `items = []`.

## Cấu hình

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §55; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-78 — Đọc và đổi trần sinh testcase (§56, §57)
- **Tiền điều kiện**: tài khoản có `AI_CONFIG`; tài khoản không có.
- **Hành động**: đọc; ghi 5; ghi 0 và 11; tài khoản không quyền gọi.
- **Kết quả mong đợi**: mặc định `maxPerProblem = 2`; ghi 5 thành công; 0 và 11 `400 PRB-001`, giá trị cũ giữ; tài khoản không quyền `403 PRB-002`; 1 và 10 được chấp nhận.

## Sự kiện, cổng và quy tắc xuyên module

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §56, §57; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-79 — Cập nhật `problem_stats` và khử trùng (BR-20)
- **Tiền điều kiện**: bài chưa có dòng `problem_stats`.
- **Hành động**: nhận `SubmissionGraded` `ACCEPTED`; nhận lại đúng sự kiện đó (cùng `eventId`); nhận `WRONG_ANSWER`; nhận `SYSTEM_ERROR`.
- **Kết quả mong đợi**: dòng được tạo (1, 1, 100.00); lần nhận lại không đổi gì; sau `WRONG_ANSWER` là (2, 1, 50.00); `SYSTEM_ERROR` không đổi số đếm; `ac_rate` được ghi cùng câu lệnh với hai bộ đếm.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-20; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-80 — Xử lý `ClassDeleted` (BR-19)
- **Tiền điều kiện**: lớp L có 3 dòng giao bài hoạt động và 1 dòng đã gỡ.
- **Hành động**: nhận `ClassDeleted { classId = L }` hai lần.
- **Kết quả mong đợi**: 3 dòng hoạt động có `removed_at = now()`, dòng đã gỡ giữ mốc cũ, không dòng nào bị xoá, lịch sử nộp bài còn nguyên; lần hai không đổi gì.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-19; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-81 — Cổng chấm bài (`ProblemGradingSpecPort`, BR-11, BR-13)
- **Tiền điều kiện**: bài có 8 Hidden đã duyệt, 2 Sample, 2 nháp AI; rồi bài bị rút xuống.
- **Hành động**: `judge-orchestration` gọi `loadGradingSpec`, `loadTestcaseSnapshot` cho phiên bản đã ghim, `loadSampleTestcases`.
- **Kết quả mong đợi**: metadata `testcases` chỉ có testcase đã duyệt, kèm `specVersion` và `currentTestcaseSetVersion` đồng bộ; bundle snapshot bất biến có nội dung của testcase đã duyệt và không chứa nháp; `loadSampleTestcases` chỉ Sample đã duyệt; bài đã rút trả `published = false` (nộp mới bị từ chối) nhưng lượt đã vào hàng đợi vẫn đọc được bundle đã ghim.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] `ProblemGradingSpecPort`, BR-11, BR-13; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-82 — Cổng đặc tả cho `harness` (`ProblemSpecQuery`, BR-05)
- **Tiền điều kiện**: bài có đặc tả `specVersion = 3`; bài khác chưa có.
- **Hành động**: `harness` gọi `findSpec`; đổi đặc tả; gọi lại.
- **Kết quả mong đợi**: view đủ các trường của Bảng 4 (`03-dd/api/F3_harness.md`) kèm `functionWrapperSupported`; bài chưa có đặc tả trả rỗng; sau khi đổi, cache Redis bị xoá và giá trị mới có `specVersion` lớn hơn; sự kiện `ProblemSpecChanged` mang `problemId` và `specVersion`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] `ProblemSpecQuery`, BR-05; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-83 — Cổng ngữ cảnh cho `ai-review` (`ProblemContextPort`, BR-06)
- **Tiền điều kiện**: bài có `ai_brief`, hai cờ, ràng buộc.
- **Hành động**: gọi `getContext`.
- **Kết quả mong đợi**: trả `aiBrief`, `noFullCode`, `socraticOnly` như trường dữ liệu có tên; không có mã đáp án mẫu, testcase, `object_key`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] `ProblemContextPort`, BR-06; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-84 — Cổng tên bài, chủ đề và giao bài cho module khác (`ProblemSummaryReadPort`, `ProblemTopicReadPort`, `ClassAssignmentReadPort`)
- **Tiền điều kiện**: bài đã `deleted` còn lượt nộp cũ; lớp có bài đã gỡ mềm.
- **Hành động**: `judge-orchestration` gọi `getSummaries`, `getProblemIdsByTopics`, `listAssignments`; học viên đọc `GET /topics/progress`.
- **Kết quả mong đợi**: bài đã xoá mềm vẫn trả mã và tên cho lịch sử; danh sách giao bài có cả bài đã gỡ kèm thời điểm gỡ để nơi đọc tự lọc đúng cửa sổ; quan hệ bài–chủ đề lịch sử vẫn gồm bài đã xoá để thống kê lượt nộp cũ, còn tiến độ chủ đề hiện hành chỉ tính bài `PUBLISHED AND NOT deleted`.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] `ProblemSummaryReadPort`, `ClassAssignmentReadPort`; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-85 — Chống rò rỉ testcase ẩn xuyên suốt (BR-13)
- **Tiền điều kiện**: bài có Hidden `INLINE` và `MINIO`.
- **Hành động**: học viên gọi mọi endpoint học viên (§2 tới §14); quét phản hồi và log mức `INFO`.
- **Kết quả mong đợi**: không phản hồi nào chứa input/expected-output, `object_key`, presigned URL của Hidden; log chỉ có `testcase_id` và độ dài; không DTO học viên nào có trường `input`/`expectedOutput` cho Hidden (kiểm cấu trúc).

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-13; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-86 — Quyền theo Function (mọi endpoint ghi)
- **Tiền điều kiện**: tài khoản có `PROBLEM_AUTHORING` nhưng không có `TESTCASE_MANAGEMENT`, và ngược lại; học viên.
- **Hành động**: gọi endpoint soạn bài, endpoint testcase, endpoint giao bài, endpoint cấu hình AI.
- **Kết quả mong đợi**: thiếu Function thì `403 PRB-002`; học viên không gọi được endpoint quản trị; quyền được kiểm ở máy chủ kể cả khi giao diện ẩn nút.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-02; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-87 — Cache danh sách và rút xuống tức thì (BR-25)
- **Tiền điều kiện**: danh sách đã được cache.
- **Hành động**: rút một bài xuống; gọi `GET /problems`.
- **Kết quả mong đợi**: bài biến ngay (phiên bản cache tăng); Redis chết thì danh sách vẫn trả từ DB.

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] BR-25; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-88 — Bài giao của lớp học viên giữ bài đã rút trong tiến độ

| Mục | Nội dung |
| :--- | :--- |
| Tiền đề | Học viên ở bốn lớp; một lớp có bài thường, bài đã rút nhưng giao còn hiệu lực và bài đã gỡ khỏi lớp |
| Thao tác | Gọi `GET /me/class-problems` không lọc và với `classId` của lớp; gọi với lớp người khác và UUID sai |
| Kết quả mong đợi | Trả đủ bốn lớp, không giới hạn ba nhóm; bài đã rút vẫn có dòng `withdrawn = true` và giữ trạng thái giải, bài đã gỡ vắng mặt; hai `classId` không hợp lệ cùng `404 PRB-015`; không nhận `studentId` để đọc hộ |
| Nguồn | `DEC-2026-1024-dd-student-class-contracts`; `03-dd/api/F2_problem-bank.md` mục 5A |

- **Nguồn**: [SoT: 03-dd/api/F2_problem-bank.md] §5A; [SoT: 03-dd/logic/F2_problem-bank.md].

### AC-89 — Sự kiện thay đổi bài không mất khi hàng đợi sự kiện tạm lỗi

- **Tiền điều kiện**: giảng viên sửa một bài hợp lệ; dịch vụ chuyển sự kiện sang phân hệ khác đang tạm ngừng.
- **Hành động**: lưu thay đổi bài, sau đó khôi phục dịch vụ chuyển sự kiện.
- **Kết quả mong đợi**: thay đổi bài được lưu; khi dịch vụ hoạt động lại, sự kiện của chính lần sửa ấy được chuyển đủ. Gửi lại không tạo hai lần thay đổi nghiệp vụ ở bên nhận.
- **Nguồn**: `[SoT: 03-dd/api/F2_problem-bank.md]` mục 59; `[SoT: 03-dd/jobs/F2_problem-bank.md]` JOB-PB-01.

### AC-90 — Không lưu thay đổi bài nếu không ghi nhận được sự kiện kèm theo

- **Tiền điều kiện**: một thao tác giao bài sẽ phát sự kiện cho các phân hệ khác, nhưng nơi lưu sự kiện không ghi được.
- **Hành động**: giảng viên bấm Giao bài.
- **Kết quả mong đợi**: lô trả HTTP 200 với bài lỗi trong `failedItems` mã `PRB-056`, bài không được giao một nửa; các bài khác trong lô vẫn xử lý. Khi thử lại sau khi hệ thống phục hồi, bài được giao đúng một lần và sự kiện được chuyển đi.
- **Nguồn**: `[SoT: 03-dd/logic/F2_problem-bank.md]` BR-26; `[SoT: 03-dd/jobs/F2_problem-bank.md]` JOB-PB-01.

### AC-91 — Chạy đáp án mẫu có hạn mức riêng theo tài khoản

- **Tiền điều kiện**: tác giả đã chạy đáp án mẫu của bài mười lần trong 60 giây gần nhất.
- **Hành động**: chạy thêm lần thứ mười một, rồi thử lại sau khi lượt cũ nhất hết hạn; lặp với một lượt mà máy chấm gặp lỗi hạ tầng.
- **Kết quả mong đợi**: lần thứ mười một bị từ chối và được báo thời gian thử lại; hết cửa sổ thì chạy được. Lượt gặp lỗi hạ tầng không tiêu tốn hạn mức, và hạn mức này không trừ số lượt sinh testcase AI.
- **Nguồn**: `[SoT: 03-dd/api/F2_problem-bank.md]` mục 48, Q-31.

### AC-92 — Tiến độ theo độ khó chỉ tính bài đang xuất bản

- **Tiền điều kiện**: người học đã giải bài ở nhiều mức khó; một bài sau đó bị rút khỏi ngân hàng và có một mức mới chưa có bài.
- **Hành động**: mở Dashboard và xem số đã giải theo từng mức khó.
- **Kết quả mong đợi**: mỗi mức hiện đúng số bài đang xuất bản và số bài người học đã giải trong tập đó. Bài đã rút không thuộc cả tử lẫn mẫu; mức mới hiện không trên không, không bị bỏ khỏi danh sách. Khi nguồn trạng thái giải lỗi, API trả `503`, không trả tiến độ với số đã giải giả bằng không.
- **Nguồn**: `[SoT: 03-dd/api/F2_problem-bank.md]` mục 14A.

### AC-93 — Bộ testcase đã công bố giữ nguyên cho lượt nộp cũ

- **Tiền điều kiện**: một bài đang xuất bản có bộ testcase phiên bản A; một lượt nộp đã được tiếp nhận và ghi nhận phiên bản A nhưng chưa được chấm.
- **Hành động**: tác giả sửa một testcase mẫu đã duyệt và công bố phiên bản B; máy chấm xử lý lượt nộp cũ, rồi có lượt nộp mới. Lặp lại khi bộ phiên bản A bị thiếu hoặc sai dấu kiểm toàn vẹn; khi rút bài, sửa bộ chấm rồi xuất bản lại; và khi tái xuất bản mà bộ chấm không đổi.
- **Kết quả mong đợi**: lượt cũ chỉ được chấm bằng nội dung nguyên vẹn của A; lượt mới dùng B. Nội dung A không bị ghi đè hoặc đổi khi sửa bài, kể cả sau khi testcase hiện hành bị xoá hoặc bài bị rút xuống. Thiếu hoặc hỏng A thì lượt cũ kết thúc bằng lỗi hệ thống; không chấm bằng B hay nội dung hiện hành. Rút xuống sửa rồi tái xuất bản tạo B nếu bộ chấm khác A; giữ nguyên A cho lượt cũ, phát đúng một TestcaseSetVersionBumped. Tái xuất bản với bộ chấm giống A dùng lại A, không tăng phiên bản. Lỗi tạo snapshot rollback trạng thái/version/Outbox theo BR-08.
- **Nguồn**: `[SoT: 03-dd/logic/F2_problem-bank.md]` BR-11; `[SoT: 03-dd/api/F2_problem-bank.md]` mục 40, 43, 58; `[SoT: 02-bd/database/F2_problem-bank.md]` mục 1.15.

### AC-94 — Dọn sự kiện đã phát mà không làm mất sự kiện chờ

- **Tiền điều kiện**: có sự kiện đã xác nhận phát quá 90 ngày, sự kiện đã phát chưa quá 90 ngày và sự kiện chưa phát quá 90 ngày.
- **Hành động**: chạy job dọn; lặp khi hai tiến trình cùng chạy và khi kho dữ liệu lỗi giữa một lô.
- **Kết quả mong đợi**: chỉ sự kiện đã phát qua hạn bị dọn; sự kiện chưa phát vẫn chờ gửi dù cũ bao lâu. Hai tiến trình không xoá nhầm hoặc làm hỏng sự kiện chờ; lô gặp lỗi được hoàn tác và được thử lại ở lượt sau.
- **Nguồn**: `[SoT: 03-dd/jobs/F2_problem-bank.md]` JOB-PB-02, PB-J04.

## 3. Truy vết

Bảng 1: Truy vết tiêu chí nghiệm thu sang endpoint, khối quy tắc và test. Cột "Test" điền ID test case khi `testcase-generation` chạy; "Trạng thái" cập nhật sau khi code và test.

| AC | Endpoint / khối quy tắc | Test | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-01 | §2, BR-01 | [TC-F2-AC01-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-02 | §2 | [TC-F2-AC02-01–02](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-03 | §2, BR-23 | [TC-F2-AC03-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-04 | §2, §6 | [TC-F2-AC04-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-05 | §3 | [TC-F2-AC05-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-06 | §4 | [TC-F2-AC06-01–02](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-07 | §5 | [TC-F2-AC07-01–02](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-08 | §6 | [TC-F2-AC08-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-09 | §6, §7, §8, BR-01 | [TC-F2-AC09-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-10 | §7 | [TC-F2-AC10-01](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-11 | §8 (`testcaseId`), BR-13 | [TC-F2-AC11-01–02](../testcases/modules/F2_problem-bank/F2_student_catalog_reads.md) | Chưa làm |
| AC-12 | §10, BR-21 | [TC-F2-AC12-01](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-13 | §10 | [TC-F2-AC13-01](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-14 | §11 | [TC-F2-AC14-01](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-15 | §12 | [TC-F2-AC15-01–02](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-16 | §9, BR-21 | [TC-F2-AC16-01–02](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-17 | §13, BR-23 | [TC-F2-AC17-01–02](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-18 | §14 | [TC-F2-AC18-01](../testcases/modules/F2_problem-bank/F2_bookmarks_recommendations.md) | Chưa làm |
| AC-19 | §15, §20 | [TC-F2-AC19-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-20 | §16, BR-22, BR-27 | [TC-F2-AC20-01–02](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-21 | §16-§19, §21-§24 | [TC-F2-AC21-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-22 | §16, BR-27 | [TC-F2-AC22-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-23 | §17 | [TC-F2-AC23-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-24 | §18, §23 | [TC-F2-AC24-01–02](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-25 | §19, BR-22 | [TC-F2-AC25-01–02](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-26 | §19 | [TC-F2-AC26-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-27 | §21, §22, §24 | [TC-F2-AC27-01–02](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-28 | §23 | [TC-F2-AC28-01](../testcases/modules/F2_problem-bank/F2_taxonomy_admin.md) | Chưa làm |
| AC-29 | BR-03 | [TC-F2-AC29-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-30 | §25 | [TC-F2-AC30-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-31 | §25 | [TC-F2-AC31-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-32 | §26, §27 | [TC-F2-AC32-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-33 | §28 | [TC-F2-AC33-01–02](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-34 | §29, BR-08 | [TC-F2-AC34-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-35 | §30, BR-08 | [TC-F2-AC35-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-36 | §31, §32 | [TC-F2-AC36-01–02](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-37 | §33, BR-09 | [TC-F2-AC37-01–02](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-38 | §29-§33 | [TC-F2-AC38-01](../testcases/modules/F2_problem-bank/F2_authoring_list_bulk.md) | Chưa làm |
| AC-39 | §34, BR-02 | [TC-F2-AC39-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-40 | §37-§40, §42, BR-02 | [TC-F2-AC40-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-41 | §35, §36, BR-10 | [TC-F2-AC41-01–02](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-42 | §36 | [TC-F2-AC42-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-43 | §37, BR-04 | [TC-F2-AC43-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-44 | §37 | [TC-F2-AC44-01–02](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-45 | §36, §37 | [TC-F2-AC45-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-46 | §38, BR-05 | [TC-F2-AC46-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-47 | §38 | [TC-F2-AC47-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-48 | §38 | [TC-F2-AC48-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-49 | §39, BR-06 | [TC-F2-AC49-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-50 | §40, BR-07, BR-08 | [TC-F2-AC50-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-51 | §40 | [TC-F2-AC51-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-52 | §40 | [TC-F2-AC52-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-53 | §41 | [TC-F2-AC53-01](../testcases/modules/F2_problem-bank/F2_authoring_editor_publish.md) | Chưa làm |
| AC-54 | §42, BR-12 | [TC-F2-AC54-01](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-55 | §42 | [TC-F2-AC55-01](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-56 | §42-§45, BR-11 | [TC-F2-AC56-01 đến -20](../testcases/modules/F2_problem-bank/F2_testcase_set_version.md) | Chưa làm |
| AC-57 | §43, BR-17 | [TC-F2-AC56-03 (duyệt), TC-F2-AC57-01 (từ chối hạ duyệt)](../testcases/modules/F2_problem-bank/F2_testcase_set_version.md) | Chưa làm |
| AC-58 | §43 | [TC-F2-AC58-01](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-59 | §44 | [TC-F2-AC56-06/07, TC-F2-AC59-01 đến -04](../testcases/modules/F2_problem-bank/F2_testcase_set_version.md) | Chưa làm |
| AC-60 | §45 | [TC-F2-AC56-15 đến -23](../testcases/modules/F2_problem-bank/F2_testcase_set_version.md) | Chưa làm |
| AC-61 | §46, BR-14 | [TC-F2-AC61-01](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-62 | §47, BR-14 | [TC-F2-AC62-01–02](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-63 | §48, BR-15 | [TC-F2-AC63-01–02](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-64 | §48 | [TC-F2-AC64-01–02](../testcases/modules/F2_problem-bank/F2_testcase_authoring_upload.md) | Chưa làm |
| AC-65 | §49, BR-16 | [TC-F2-AC65-01](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-66 | §49 | [TC-F2-AC66-01](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-67 | §49, §57 | [TC-F2-AC67-01](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-68 | §49 | [TC-F2-AC68-01](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-69 | §49, BR-16 | [TC-F2-AC69-01–02](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-70 | §49, BR-06, BR-16 | [TC-F2-AC70-01](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-71 | §49 | [TC-F2-AC71-01–02](../testcases/modules/F2_problem-bank/F2_ai_generation.md) | Chưa làm |
| AC-72 | §50 | [TC-F2-AC72-01–02](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-73 | §51 | [TC-F2-AC73-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-74 | §52 | [TC-F2-AC74-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-75 | §53, BR-18 | [TC-F2-AC75-01–03](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-76 | §54, BR-18 | [TC-F2-AC76-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-77 | §55 | [TC-F2-AC77-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-78 | §56, §57 | [TC-F2-AC78-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-79 | BR-20 | [TC-F2-AC79-01–02](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-80 | BR-19 | [TC-F2-AC80-01](../testcases/modules/F2_problem-bank/F2_class_assignments.md) | Chưa làm |
| AC-81 | `ProblemGradingSpecPort`, BR-11, BR-13 | [TC-F2-AC81-01–02](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-82 | `ProblemSpecQuery`, BR-05 | [TC-F2-AC82-01](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-83 | `ProblemContextPort`, BR-06 | [TC-F2-AC83-01](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-84 | `ProblemSummaryReadPort`, `ProblemTopicReadPort`, `ClassAssignmentReadPort` | [TC-F2-AC84-01–02](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-85 | BR-13 | [TC-F2-AC85-01](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-86 | BR-02 | [TC-F2-AC86-01](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-87 | BR-25 | [TC-F2-AC87-01](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-88 | §5A | [TC-F2-AC88-01–02](../testcases/modules/F2_problem-bank/F2_ports_privacy_cache.md) | Chưa làm |
| AC-89 | §59, JOB-PB-01 | [TC-F2-AC89-01 đến -04](../testcases/modules/F2_problem-bank/F2_outbox_delivery_cleanup.md) | Chưa làm |
| AC-90 | BR-26, JOB-PB-01 | [TC-F2-AC90-01](../testcases/modules/F2_problem-bank/F2_outbox_delivery_cleanup.md) | Chưa làm |
| AC-91 | §48, Q-31 | [TC-F2-AC91-01–02](../testcases/modules/F2_problem-bank/F2_reference_rate_level_progress.md) | Chưa làm |
| AC-92 | §14A | [TC-F2-AC92-01–02](../testcases/modules/F2_problem-bank/F2_reference_rate_level_progress.md) | Chưa làm |
| AC-93 | BR-11, §40, §43, §58, DB 1.15 | [TC-F2-AC93-01, -02, -03, -04, -05](../testcases/modules/F2_problem-bank/F2_snapshot_grading.md) | Chưa làm |
| AC-94 | JOB-PB-02 | [TC-F2-AC94-01 đến -03](../testcases/modules/F2_problem-bank/F2_outbox_delivery_cleanup.md) | Chưa làm |

Mọi endpoint §2 tới §57 có ít nhất một `AC`; §59, relay và dọn Outbox có AC-89–AC-90, AC-94. Snapshot được kiểm ở AC-93. DD Q-31 đã đóng và được phủ bằng AC-91; sinh testcase AI giữ trần theo bài.

## Baseline migration 2026-10-29

`DEC-2026-1029-f2-migration-readiness` đóng quyết định DDL/seed/epsilon/tag/snapshot; xem BD database F2 §7. AC-47, AC-56 và AC-93 được bổ sung nhánh epsilon và tái xuất bản; các testcase liên kết cần mở rộng tương ứng trong phase kiểm của plan migration. Giữ nguyên số AC và trạng thái Chưa làm; truy vết hiện có không đồng nghĩa nhánh mới đã có testcase hoặc đã chạy.
