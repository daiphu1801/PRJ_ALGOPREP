# Tiêu chí nghiệm thu — Màn `submission_result` (`USR0201`)

> Màn: `USR0201` Kết quả nộp bài · DD màn: `03-dd/screens/users/USR0201_submission_result.md` · Cập nhật: 2026-10-23.
> File này **chỉ** chứa luồng chạy qua nhiều module hoặc hành vi trình duyệt mà `AC` module không phủ (`03-dd/screens/README.md` mục 2). Phần dữ liệu của endpoint `GetSubmissionDetail`, quy tắc Beats, hết hạn 90 ngày đã có ở `04-tdd/modules/F4_judge-orchestration.md` (AC-30 đến AC-35); file này nghiệm thu **cách màn dùng** chúng.
> Viết cùng DD theo `.claude/rules/tdd-mode.md`; tên endpoint và mã lỗi nằm ở cột truy vết.

## 1. Phạm vi

**Trong phạm vi**: mở trang bài nộp đã xong và đang chấm; hợp nhất ảnh chụp với bản tin thời gian thực; ghép dữ liệu testcase mẫu từ hai module; hiển thị điểm, Beats; điều hướng và khoá nút theo trạng thái; vùng giảng viên chỉ đọc.

**Ngoài phạm vi**: thuật toán chấm, công thức Beats, ngưỡng 90 ngày (module F4); nội dung báo cáo AI (F5).

**Quy ước dữ liệu mẫu**: "bài nộp A" của học viên, "bài nộp B" của học viên khác; bài X có 2 testcase mẫu và 10 testcase ẩn.

## 2. Danh sách tiêu chí

### SR-AC-01 — Mở bài nộp đã xong: đủ nội dung, không mở kênh thời gian thực
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A đã `Accepted`, học viên là chủ |
| **Thao tác** | Học viên mở đường dẫn của bài nộp A (từ lịch sử hoặc dán liên kết); bấm "Sao chép mã" khi trình duyệt cho và khi trình duyệt từ chối |
| **Kết quả mong đợi** | Thấy ngay kết quả, số testcase đạt trên tổng, thời gian, bộ nhớ, Beats, bảng testcase, mã đã nộp; không cần chờ bản tin nào và trang không mở kết nối thời gian thực. Sao chép thành công có thông báo thành công; bị từ chối có thông báo lỗi, mã không đổi |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0201_submission_result.md:160; 02-bd/screens/users/USR0201_submission_result.md:456] |

### SR-AC-02 — Mở khi bài đang chấm: bảng lấp dần, chuyển sang kết quả cuối không cần tải lại
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A vừa nộp từ Workspace, đang `Đang chấm` với 12 testcase |
| **Thao tác** | Học viên mở trang bài nộp A và chờ chấm xong |
| **Kết quả mong đợi** | Trang hiện trạng thái hiện tại ngay, các dòng testcase lấp dần theo đúng thứ tự khi testcase xong, số đạt trên tổng tăng khớp. Trong lúc chấm cột thời gian và bộ nhớ từng dòng hiển thị dấu gạch. Khi kết thúc, trang tự chuyển sang kết quả cuối (nhãn, điểm, thời gian, bộ nhớ từng dòng) mà không cần tải lại, bảng đủ 12 dòng |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0201_submission_result.md:141; 02-bd/screens/users/USR0201_submission_result.md:452, 460; 04-tdd/modules/F4_judge-orchestration.md AC-25] |

### SR-AC-03 — Nối lại giữa chừng và mất kênh khi đang xem trang kết quả
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên đang xem bài nộp A đang chấm, đã xong 4 trên 12 testcase |
| **Thao tác** | Làm rớt đường truyền thời gian thực; (a) cho nối lại sau khi xong thêm 3 testcase; (b) giữ rớt mãi |
| **Kết quả mong đợi** | (a) Thấy 7 dòng đúng kết quả, không dòng nào trùng hoặc sai thứ tự, rồi nhận tiếp. (b) Trang hỏi lại trạng thái mỗi 3 giây tối đa 10 lần, rồi hiện dòng "cập nhật đang chậm"; không kết luận bài trượt; tải lại trang thấy đúng trạng thái |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md:146-156; 02-bd/screens/users/USR0201_submission_result.md:472; 04-tdd/modules/F4_judge-orchestration.md AC-27, AC-28] |

### SR-AC-04 — Mở rộng testcase mẫu ghép từ hai nơi; testcase ẩn không mở được
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A của bài X, `Wrong Answer` ở một testcase mẫu và một testcase ẩn |
| **Thao tác** | Học viên bấm "Chi tiết" ở dòng testcase mẫu rồi ở dòng testcase ẩn; lặp lại khi ngân hàng bài tạm lỗi |
| **Kết quả mong đợi** | Dòng mẫu mở ra ba hộp: dữ liệu vào và kết quả mong đợi (lấy từ ngân hàng bài), kết quả thực tế (lấy từ kết quả chấm, hiển thị tối đa 2.000 ký tự). Dòng ẩn: nút bị vô hiệu, bấm không mở gì, không có dữ liệu vào, mong đợi, thực tế hay chênh lệch ở bất kỳ nơi nào của trang. Khi ngân hàng bài lỗi, hộp dữ liệu vào và mong đợi hiện dấu gạch, hộp thực tế vẫn hiện |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:315, 386, 470; 04-tdd/modules/F2_problem-bank.md AC-11; 04-tdd/modules/F4_judge-orchestration.md AC-31] |

### SR-AC-05 — Lỗi biên dịch: ẩn bảng, chỉ đúng dòng của học viên
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A kết quả `Compile Error` |
| **Thao tác** | Học viên mở trang |
| **Kết quả mong đợi** | Không có bảng testcase (không phải bảng rỗng); có khối thông báo lỗi biên dịch chỉ dòng mã của học viên, không có dòng nào thuộc mã khung do hệ thống sinh; điểm và thời gian hiển thị dấu gạch; hai nút phân tích và phỏng vấn bị vô hiệu |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0201_submission_result.md:163; 02-bd/screens/users/USR0201_submission_result.md:380; 04-tdd/modules/F3_harness.md AC-14, AC-17] |

### SR-AC-06 — Bài nộp quá 90 ngày: bảng thay bằng một câu, phần còn lại vẫn đủ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp A đã 91 ngày tuổi, kết quả `Wrong Answer` 7 trên 12 |
| **Thao tác** | Học viên mở trang |
| **Kết quả mong đợi** | Banner (kết quả, 7/12), mã nguồn và các số tổng vẫn hiện; thay vì bảng testcase là một câu nói chi tiết từng testcase chỉ lưu 90 ngày; không có thông báo lỗi |
| **Nguồn** | [SoT: 03-dd/api/F4_judge-orchestration.md:510-517; 04-tdd/modules/F4_judge-orchestration.md AC-33] |

### SR-AC-07 — Không tìm thấy giống nhau cho mọi nguyên nhân; lỗi mạng khác không tìm thấy
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp B của học viên khác; một mã bài nộp không tồn tại |
| **Thao tác** | Học viên mở đường dẫn bài nộp B, rồi mã không tồn tại; sau đó mở bài nộp A khi mất mạng |
| **Kết quả mong đợi** | Hai trường hợp đầu hiện **cùng** một trang "Không tìm thấy bài nộp.", không phân biệt được bài nộp có tồn tại hay không. Mất mạng hiện lỗi tải kèm nút thử lại, **không** hiện "Không tìm thấy" |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:469; 04-tdd/modules/F4_judge-orchestration.md AC-32] |

### SR-AC-08 — Điểm và Beats hiển thị đúng điều kiện
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài nộp `Accepted` thuộc nhóm có hơn 10 bài `Accepted` cùng bài, cùng ngôn ngữ; một bài nộp `Accepted` thuộc nhóm có 3 bài; một bài `Wrong Answer`; một bài `System Error` |
| **Thao tác** | Học viên lần lượt mở bốn trang |
| **Kết quả mong đợi** | Nhóm đủ mẫu: "Nhanh hơn X%". Nhóm 3 bài: "Chưa đủ dữ liệu so sánh". `Wrong Answer`: Beats là dấu gạch, số đạt trên tổng hiển thị. `System Error`: nhãn "Lỗi hệ thống", điểm và Beats là dấu gạch; không có thông báo đổ lỗi cho học viên |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:298, 473; 04-tdd/modules/F4_judge-orchestration.md AC-34] |

### SR-AC-09 — Hành động tiếp theo và độc lập với phân hệ AI
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài nộp `Accepted` và một bài `Wrong Answer`; bài toán của bài nộp bị rút khỏi ngân hàng hoặc không đọc được; phân hệ AI hỏng |
| **Thao tác** | Mở từng trang; bấm các nút hành động, tên bài ở breadcrumb và "Làm lại bài" |
| **Kết quả mong đợi** | Trang kết quả hiển thị đầy đủ dù AI hỏng. Nút phân tích và phỏng vấn bật khi `Accepted` và dẫn tới màn tương ứng kèm mã bài nộp; khi chưa đạt thì vô hiệu và có câu giải thích bằng chữ đọc được bằng bàn phím. "Làm lại bài" đưa vào Workspace đúng bài. Bài không đọc được: nút "Làm lại" và liên kết tên bài bị vô hiệu |
| **Nguồn** | [SoT: 02-bd/screens/users/USR0201_submission_result.md:34, 404-406; 04-tdd/modules/F4_judge-orchestration.md AC-68] |

### SR-AC-10 — Vùng giảng viên: chỉ đọc, không kênh thời gian thực, gợi ý URL không đổi dữ liệu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên có quyền quản lý lớp; bài nộp của học viên H ở lớp L của giảng viên đó, đang chấm; một đường dẫn ghi học viên và lớp khác H, L |
| **Thao tác** | Giảng viên mở bài nộp từ danh sách bài nộp, từ chi tiết học viên và mở thẳng đường dẫn không có `from`; mở đường dẫn giả mạo |
| **Kết quả mong đợi** | Trang chỉ đọc: không có nút phân tích, phỏng vấn, làm lại; hiện trạng thái hiện tại và không tự cập nhật (muốn mới hơn thì tải lại); không có Beats. Breadcrumb về đúng màn đã gọi (mặc định danh sách bài nộp), mục sidebar sáng theo màn gọi tới. Thẻ ngữ cảnh luôn là H và L thật của bài nộp, không theo gợi ý trên đường dẫn. Bài nộp ngoài lớp: "Không tìm thấy bài nộp." |
| **Nguồn** | [SoT: 01-rd/screens/users/USR0201_submission_result.md:225-232; 02-bd/screens/users/USR0201_submission_result.md:549, 557-560; 04-tdd/modules/F4_judge-orchestration.md AC-45] |

## 3. Truy vết

Bảng 1 nối tiêu chí tới `AC` module và điểm kiểm ở DD màn (`T-SR-nn`).

**Bảng 1: Truy vết**

| AC | AC module liên quan | Điểm kiểm DD màn | Test case | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| SR-AC-01 | `F4 AC-30` | T-SR-01, 17 | [TC-USR0201-SRAC01-01](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-02 | `F4 AC-25` | T-SR-06, 07, 08, 22 | [TC-USR0201-SRAC02-01](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-03 | `F4 AC-27`, `AC-28` | T-SR-09, 10, 22 | [TC-USR0201-SRAC03-01](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-04 | `F2 AC-11`, `F4 AC-31` | T-SR-12, 13, 14 | [TC-USR0201-SRAC04-01](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC04-02](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-05 | `F3 AC-14`, `AC-17` | T-SR-04 | [TC-USR0201-SRAC05-01](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-06 | `F4 AC-33` | T-SR-05 | [TC-USR0201-SRAC06-01](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC06-02](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-07 | `F4 AC-32` | T-SR-02, 03 | [TC-USR0201-SRAC07-01](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC07-02](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-08 | `F4 AC-34` | T-SR-15, 16 | [TC-USR0201-SRAC08-01](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC08-02](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC08-03](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC08-04](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-09 | `F4 AC-68` | T-SR-18, 19, 20, 23 | [TC-USR0201-SRAC09-01](../../testcases/screens/users/USR0201_submission_result.md), [TC-USR0201-SRAC09-02](../../testcases/screens/users/USR0201_submission_result.md) | chưa |
| SR-AC-10 | `F4 AC-45` | T-SR-11, 21 | [TC-USR0201-SRAC10-01](../../testcases/screens/users/USR0201_submission_result.md) | chưa |

## 4. Chưa chốt

- `SR-AC-04`: ngân hàng bài trả `testcaseId` ở từng testcase mẫu để ghép với kết quả chấm (`DEC-2026-1024-dd-screens-users-core-contract-changes`, `04-tdd/modules/F2_problem-bank.md` AC-11); nghiệm thu được khi mã dựng xong.
- `SR-AC-09`, ý bài bị rút khỏi ngân hàng: nhãn "Đã rút khỏi ngân hàng" lấy từ `problem.withdrawn` của F4 mục 8 (`DEC-2026-1024-dd-screens-users-core-contract-changes`, `04-tdd/modules/F4_judge-orchestration.md` AC-71); `problem = null` (ngân hàng bài lỗi) không hiện nhãn.
- Các AC đã có testcase thiết kế trong cột "Test case"; trạng thái vẫn chưa thực hiện.
