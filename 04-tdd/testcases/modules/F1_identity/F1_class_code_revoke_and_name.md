# Testcase QA F1 — thu hồi mã mời, tên lớp duy nhất và ca hồi quy về lớp

> Phạm vi: `DEC-2026-1126-class-invite-revoke`, `DEC-2026-1126-class-name-unique`, hộp thoại xoá lớp có số liệu. Oracle: `03-dd/api/F1_identity.md` #20, #21, #25, #78 và mục cuối "Bổ sung 2026-11-26"; `03-dd/logic/F1_identity.md` BR-09, BR-22, BR-29. Plan: `06-plan/nexa-plan/261125-2330-lop-hoc-xu-ly-case-can-thiet.md` mục 4. Khác với các file cùng thư mục, các ca dưới đây **đã có test chạy được**; cột "Test thật" ghi tên test, cột "Trạng thái" ghi kết quả lần chạy ngày 2026-11-26.

Bảng 1: Ca kiểm của phần lớp học và nơi chúng được thực hiện.

| ID | Ca | Kết quả mong đợi | Test thật | Trạng thái |
| :-: | :--- | :--- | :--- | :--- |
| CL-01 | Thu hồi một mã còn hạn, rồi học viên nhập mã đó | `422 IDT-410`, không ghi danh, không lộ việc mã từng tồn tại | `ClassManagementFlowTest.revokingACodeStopsItWorkingKeepsTheOthersAndARepeatIsHarmless` | PASS |
| CL-02 | Thu hồi một mã, mã khác của cùng lớp còn hạn | Mã khác vẫn dùng được, #24 chỉ còn mã khác | cùng test CL-01 | PASS |
| CL-03 | Thu hồi mã của lớp không thuộc giảng viên; id sai dạng; thiếu ô quyền `UPDATE` | `404 IDT-414`, không xoá gì; thiếu quyền `403 IDT-445` | `ClassManagementFlowTest.aClassOfSomeoneElseOrWithABadIdIs404Idt414OnEveryEndpoint`, `...everyInstructorEndpointNeedsItsOwnCell...`; `InstructorClassHandlersTest.revokingACodeNeedsOwnership...` | PASS |
| CL-04 | Thu hồi mã đã thu hồi; mã của lớp khác qua đường dẫn của lớp này | `204`, mã lớp khác còn nguyên | cùng test CL-01; `ClassManagementFlowTest.aCodeOfAnotherClassIsNeverRevokedThroughThisClassPath` | PASS |
| CL-05 | Tạo lớp trùng tên (khác hoa thường, thừa khoảng trắng) của cùng giảng viên | `409 IDT-417`, không tạo lớp | `ClassManagementFlowTest.anInstructorCannotOwnTwoClassesWithTheSameNameButTwoInstructorsCan` | PASS |
| CL-06 | Hai giảng viên cùng đặt một tên | `201` cho cả hai | cùng test CL-05 | PASS |
| CL-07 | Đổi tên lớp thành tên lớp khác của mình; PATCH giữ nguyên tên của chính lớp | `409 IDT-417` ở ca đầu; `200` ở ca sau | cùng test CL-05 | PASS |
| CL-08 | Tạo hai lớp cùng tên cùng lúc | Chỉ một lớp được tạo | **Chưa có test song song.** Bảo đảm bằng chỉ mục `classes_instructor_name_uk` đã chạy trong test CL-05 (vi phạm chỉ mục thành `409`) | Chưa kiểm song song |
| CL-09 | Hộp thoại xoá lớp có học viên và bài giao | Ghi đúng số học viên và số bài giao | Vitest `class-management-view.test.tsx` "tells what the delete takes with it..." (dữ liệu giả); số học viên đã đủ từ `studentCount` của #19 | PASS (dữ liệu giả) |
| CL-10 | Nhập mã sai dạng: chữ thường, có khoảng trắng, thiếu hoặc thừa ký tự | Chuẩn hoá hoặc `422 IDT-410`, không lỗi máy chủ | `StudentClassFlowTest.expiredAndMalformedCodesAre422Idt410AndEnrolNobody` | PASS |
| CL-11 | Một học viên bấm "Tham gia" hai lần | Một dòng ghi danh, `joined_at` giữ nguyên | `StudentClassFlowTest.joinEnrolsOnceAndIsIdempotentKeepingJoinedAt` | PASS. Hai học viên khác nhau cùng lúc: chưa có test song song |
| CL-12 | Học viên bị gỡ rồi vào lại bằng mã | Dùng lại dòng cũ, `left_at` về rỗng | `StudentClassFlowTest.rejoinAfterLeavingReactivatesTheSameRowAndKeepsJoinedAt` | PASS |
| CL-13 | Xoá lớp đang có bài giao | Ghi danh và mã mời mất theo, sự kiện `ClassDeleted` phát một lần; lượt nộp không đụng | `ClassManagementFlowTest.deleteRemovesTheClassWithItsCodesAndEnrolmentsThenAnnouncesIt`; phía F2 gỡ mềm bài giao: test consumer của problem-bank | PASS (hai phía kiểm riêng) |
| CL-14 | Học viên đã xoá hoặc bị khoá tài khoản nằm trong lớp | Vẫn trong danh sách và sĩ số, có `accountInactive = true`; sĩ số không đổi (`DEC-2026-1126-roster-inactive-account-label`) | `ClassManagementFlowTest.aStudentWhoseAccountIsNoLongerActiveStaysInTheRosterWithAFlag`; Vitest `class-detail-real.test.tsx` "marks a learner whose account is no longer active..." | PASS |
| CL-15 | Học viên nhập mã của lớp mà giảng viên không còn `ACTIVE` | `422 IDT-410`, không ghi danh; giảng viên `ACTIVE` lại thì mã dùng được (`DEC-2026-1126-class-join-needs-active-instructor`) | `StudentClassFlowTest.aCodeOfAClassWhoseInstructorIsNoLongerActiveIs422Idt410AndEnrolsNobody` | PASS |

Ghi chú CL-14 (đã chốt 2026-11-26): trước đó chỉ là quan sát từ code. Job ẩn danh không đổi `class_enrollments` và danh sách chỉ lọc `left_at IS NULL`, nên học viên đã xoá tài khoản vẫn nằm trong sĩ số và mẫu số tỉ lệ hoàn thành dưới tên "Người dùng đã xoá". Chủ dự án chọn giữ cách tính và chỉ ghi nhãn (phương án C). Chưa có test cho một tài khoản đã **ẩn danh hoá** thật (sau 30 ngày); test dùng tài khoản `DEACTIVATED` do chính chủ xoá, cùng luồng truy vấn.

Bảng 2: Ca cho thấy test frontend đã thêm.

| Ca | Test | Trạng thái |
| :--- | :--- | :--- |
| Thu hồi mã: huỷ thì không đổi, xác nhận thì mã biến mất và mã khác còn | `class-detail-view.test.tsx` "revokes one invite code after a confirmation..." | PASS |
| Lỗi tên trùng `IDT-417` khi tạo và khi đổi tên, hộp thoại giữ nguyên | `class-management-real.test.tsx` "reads a name clash (IDT-417)..." | PASS |
