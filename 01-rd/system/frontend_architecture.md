# Kiến Trúc Frontend (Frontend Architecture)

Tài liệu này đặc tả kiến trúc Next.js theo Feature-Sliced Design (FSD) cho AlgoPrep: công nghệ, phân tầng,
tiêu chí đặt một component vào tầng nào, cách xử lý hai kênh realtime, và quy chuẩn bảo mật phía client.

Nguyên lý FSD và lý do chọn ở `01-rd/overview/overview.md` mục 1.H. Backend đối ứng ở `backend_architecture.md`.

> **Trạng thái 2026-08-20 (cập nhật 2026-08-24):** `05-coding/frontend/` còn rỗng. Đây là **thiết kế phải theo**,
> chưa phải mô tả mã đang có. Dự án có `09-layoutBase/` (31 màn prototype tĩnh `.dc.html`) làm căn cứ đối chiếu UX
> và bố cục màn cho BD/DD.

---

## 1. Công nghệ frontend

Nguồn: `README.md` mục 5 và `DEC-2026-0820-stack-versions`.

| Hạng mục | Lựa chọn | Ghi chú |
| :--- | :--- | :--- |
| Framework | **Next.js 16.x**, App Router | React Server Components cho phần đọc nhiều (đề bài, danh sách bài toán) |
| Ngôn ngữ | **TypeScript**, chế độ `strict` | Không `any` ngầm; lược đồ kiểu của F3 phải mô hình hoá được bằng union kiểu |
| Style | **Tailwind CSS** | Chưa có design token dự án; primitive tự dựng trong `shared/ui` |
| Soạn thảo mã | **Monaco Editor** | Nạp động (dynamic import), chỉ trên màn chi tiết bài toán |
| Trạng thái từ server | **TanStack Query** | Cache, đồng bộ, `invalidate` sau khi nộp bài. [SoT: Suy luận — chưa chốt ở `README.md`] |
| Trạng thái giao diện | **Zustand** | Trạng thái nội bộ: ngôn ngữ đang chọn trong editor, bố cục panel. [SoT: Suy luận] |
| Realtime chấm bài | **WebSocket (STOMP)** | Trạng thái từng testcase theo topic của bài nộp (`README.md` mục 4, F4) |
| Realtime AI | **SSE** | Stream phản hồi phỏng vấn (`README.md` mục 4, F5.2) |
| Kiểm thử | **Vitest** (unit) · **Playwright** (E2E) | `README.md` mục 5 |

**Ba thư viện phải quyết định bằng nhu cầu thật, không quyết trước:** thư viện render Markdown và LaTeX (đề
bài dùng cả hai), thư viện làm sạch HTML phía client, và thư viện biểu đồ cho trang tiến độ. Ghi ở đây để
không ai tưởng chúng đã được chốt. [SoT: Suy luận]

**Đa ngôn ngữ (i18n): ĐÃ CHỐT có song ngữ Việt/Anh (vi/en)** theo `DEC-2026-0824-i18n-vi-en` (đồng bộ
`overview.md` mục 3). Giao diện hỗ trợ chuyển đổi ngôn ngữ (`data-ui-lang`, `data-lang="vi|en"`), nội dung đề bài
do giảng viên soạn giữ nguyên ngôn ngữ gốc. Chi tiết cấu trúc định tuyến và khung dịch i18n chốt tại
`02-bd/screens/`.

---

## 2. Phân tầng FSD

```text
frontend/src/
│
├── app/                          # TẦNG 1: Cấu hình toàn cục và định tuyến Next.js
│   ├── providers/                #   QueryClientProvider, AuthProvider, ErrorBoundary gốc
│   ├── (routes)/                 #   Route App Router — chỉ lắp view vào, không chứa logic
│   └── globals.css
│
├── views/                        # TẦNG 2: Bố cục hoàn chỉnh của MỘT màn hình
│   ├── problem-list/             #   một slice cho mỗi màn trong 01-rd/screens/
│   ├── problem-detail/           #   tên slice = slug của màn, dấu `_` đổi thành `-`
│   ├── submission-result/
│   ├── solution-review/
│   ├── mock-interview/
│   ├── interview-bank/
│   ├── my-progress/
│   ├── auth/
│   └── admin/                    #   khu vực quản trị, mỗi màn một slice con
│
├── widgets/                      # TẦNG 3: Tổ hợp UI lớn ghép từ nhiều feature
│   ├── app-shell/                #   header, điều hướng chính
│   ├── code-workspace/           #   khung editor + thanh chọn ngôn ngữ + panel kết quả
│   ├── testcase-panel/           #   bảng kết quả từng testcase, cập nhật realtime
│   └── interview-console/        #   khung hội thoại phỏng vấn + panel rubric
│
├── features/                     # TẦNG 4: Hành động của người dùng
│   ├── run-code/                 #   chạy thử với testcase Sample
│   ├── submit-solution/          #   nộp bài + theo dõi trạng thái qua WebSocket
│   ├── request-solution-review/  #   gọi F5.1, hiển thị báo cáo
│   ├── conduct-mock-interview/   #   phiên F5.2 qua SSE
│   ├── answer-interview-question/#   chế độ luyện của F6
│   ├── auth-by-credentials/      #   đăng nhập, làm mới token
│   └── author-problem/           #   soạn đề và tải testcase (vai trò INSTRUCTOR)
│
├── entities/                     # TẦNG 5: Thực thể nghiệp vụ — kiểu dữ liệu và UI thuần
│   ├── problem/                  #   ProblemCard, DifficultyBadge, TopicTag, kiểu Problem
│   ├── submission/               #   StatusBadge, VerdictLabel, kiểu Submission
│   ├── testcase/                 #   TestcaseRow — Hidden chỉ hiện trạng thái và chỉ số
│   ├── interview-question/
│   ├── interview-session/
│   └── user/                     #   store người dùng hiện tại, kiểu Role
│
└── shared/                       # TẦNG 6: Kỹ thuật và UI dùng chung, KHÔNG biết nghiệp vụ
    ├── ui/                       #   Button, Modal, Skeleton, Tabs, CodeBlock
    ├── api/                      #   HTTP client, interceptor, ánh xạ mã lỗi
    ├── realtime/                 #   client STOMP và client SSE
    ├── lib/                      #   hàm thuần: cn(), formatDuration()
    └── config/                   #   biến môi trường
```

> **Quy tắc vàng của FSD.**
> 1. Chỉ import một slice qua Public API của nó — file `index.ts` ở gốc slice
>    (ví dụ `import { ProblemCard } from '@/entities/problem'`), không trỏ sâu vào file bên trong.
> 2. Import chỉ đi **một hướng, xuống dưới**: `app → views → widgets → features → entities → shared`.
>    Tầng dưới không bao giờ biết tầng trên. Cùng tầng thì không import lẫn nhau.

**Tên slice `views` khớp slug màn hình.** Mỗi màn trong `01-rd/screens/` có đúng một slice `views` cùng tên,
`_` đổi thành `-`. Nhờ vậy `grep` một tên là ra cả chuỗi `01-rd → 02-bd → 03-dd → code` — đúng luật chống
trôi của `CLAUDE.md` mục Process.

**Hai màn dễ bị gộp, đừng gộp:** `problem-detail` là nơi đọc đề và viết mã; `submission-result` là kết quả
của một bài nộp cụ thể (có URL riêng để chia sẻ và để mở lại từ trang tiến độ). Gộp làm một thì không mở lại
được một bài nộp cũ.
**Và hai luồng AI là hai slice riêng:** `solution-review` (một lượt, tĩnh) khác `mock-interview` (nhiều lượt,
stream, có trạng thái phiên). Chúng chỉ giống nhau ở chỗ cùng mở ra sau `Accepted`.

### 2.A. Đặt một component vào tầng nào

Chiều import thì ESLint chặn được. Còn "component này thuộc tầng nào" thì máy không trả lời được — dưới đây
là tiêu chí cho người và cho agent.

**Tiêu chí duy nhất: component có biết về nghiệp vụ không.**

| Component | Tầng | Vì sao |
| :--- | :--- | :--- |
| Chỉ nhận `children` và prop trình bày (`variant`, `size`) | `shared/ui` | Xoá sạch nghiệp vụ AlgoPrep nó vẫn dùng được |
| Nhận kiểu nghiệp vụ (`Problem`, `Submission`, `InterviewQuestion`) | `entities/<domain>/ui` | Chỉ có nghĩa trong miền đó |
| Gắn với một hành động của người dùng | `features/<action>/ui` | Vòng đời theo tính năng, không theo thực thể |
| Ghép nhiều feature thành một khối lớn của màn | `widgets/<name>` | Là tổ hợp, không phải hành động đơn |

**"Dùng ở nhiều nơi" KHÔNG phải tiêu chí.** `ProblemCard` xuất hiện ở danh sách bài toán, trang tiến độ và
màn bài tập theo lớp, nhưng nó nhận `Problem` nên vẫn ở `entities/problem/ui`. Số nơi dùng nói lên mức tái
sử dụng; tầng nói lên **mức phụ thuộc nghiệp vụ**. Hai chuyện khác nhau.

Phép thử nhanh khi phân vân: *xoá sạch nghiệp vụ AlgoPrep, chỉ giữ khung Next.js — component này còn biên
dịch và còn ý nghĩa không?* Còn thì `shared`; không còn thì `entities` hoặc `features`.

**Ranh giới ngược lại:** thẻ HTML thô vẫn dùng thẳng khi nó chỉ làm bố cục cục bộ (`<div className="flex
gap-4">`). Chỉ những phần tử được chuẩn hoá về mặt trình bày mới đóng gói thành primitive trong `shared/ui` —
đừng kéo bố cục của một widget lên `shared`.

**Đổi tầng là thay đổi có chủ ý**, không phải hệ quả phụ của một commit tính năng. Thăng lên `shared` chỉ khi
component đã hết phụ thuộc nghiệp vụ (thường là tách làm hai: phần trình bày lên `shared/ui`, phần biết dữ
liệu ở lại). Phát hiện thứ gì trong `shared` chỉ một nơi dùng **và** biết nghiệp vụ thì giáng nó xuống.

### 2.B. Ba trường hợp riêng của AlgoPrep

* **Monaco Editor đặt ở `shared/ui`, không ở `features`.** Bản thân editor không biết `Problem` là gì — nó
  nhận mã, ngôn ngữ, và callback. Phần biết nghiệp vụ (mã mẫu theo ngôn ngữ của bài toán, chọn ngôn ngữ
  trong ba ngôn ngữ hỗ trợ) nằm ở `widgets/code-workspace`. **Bắt buộc nạp động** — Monaco rất nặng và chỉ
  cần trên một màn.
* **Client STOMP và client SSE đặt ở `shared/realtime`.** Chúng là hạ tầng truyền tin, không biết nghiệp vụ.
  Việc *diễn giải* message (message này là kết quả testcase thứ ba, đã sai) thuộc `features/submit-solution`.
* **Render đề bài (Markdown + LaTeX) đặt ở `entities/problem/ui`.** Nó nhận nội dung đề bài — một kiểu
  nghiệp vụ — nên không thuộc `shared`.

---

## 3. Kiến trúc dữ liệu và khả năng chịu lỗi

* **Data Mapper là bắt buộc (Anti-Corruption Layer phía client).** Mọi phản hồi từ `shared/api` phải đi qua
  một mapper (đặt trong `model/` hoặc `api/` của entity/feature) để chuyển DTO thành kiểu dữ liệu của
  frontend trước khi vào store: chuỗi ISO thành `Date`, giá trị thiếu thành mặc định rõ ràng. Backend đổi
  hình dạng phản hồi thì chỉ mapper phải sửa, không phải mọi component.
* **Error Boundary và Suspense theo từng widget.** `code-workspace`, `testcase-panel`, `interview-console`
  mỗi cái bọc riêng. Panel kết quả AI lỗi thì editor và bảng testcase vẫn dùng được — đây là **suy giảm có
  kiểm soát hiện lên giao diện**, đối ứng với ràng buộc ở `overview.md` mục 1.J.
* **Trạng thái realtime phải chịu được mất kết nối.** WebSocket đứt trong lúc chấm là chuyện thường. Bắt
  buộc có đường lùi: kết nối lại kèm lấy lại trạng thái hiện tại của bài nộp bằng một lần gọi REST. Chỉ dựa
  vào push là sẽ có người thấy bài nộp treo ở `PENDING` trong khi backend đã chấm xong.
* **Không đoán trạng thái cuối ở client.** Trạng thái tổng của bài nộp do backend quyết; client hiển thị,
  không tự suy ra từ các testcase đã nhận.
* **Kết quả AI dạng cấu trúc phải được kiểm ở biên.** F5.1 trả JSON theo lược đồ; client kiểm lược đồ đó
  trước khi render. JSON sai hình dạng thì hiện thông báo lỗi thân thiện, không làm sập cả màn hình.

---

## 4. Quy chuẩn bảo mật frontend

* **Hai token, mỗi cái một chỗ.**
  - **Access Token** giữ trong bộ nhớ ứng dụng, **không** đặt vào `localStorage`. Mã độc XSS đọc được
    `localStorage`.
  - **Refresh Token** nằm trong cookie `Secure`, `HttpOnly`, có `SameSite` — JavaScript không đọc được
    (`README.md` mục 4, F1).
  - Interceptor của HTTP client bắt `401`, gọi endpoint làm mới token bằng cookie, rồi phát lại request. Phải
    chống lặp: một lần làm mới thất bại thì đăng xuất, không thử vô hạn.
* **XSS ở hai chỗ cụ thể của AlgoPrep.** Đề bài Markdown do A2 soạn, và phản hồi AI (được sinh từ dữ liệu
  người dùng). Cả hai đều phải làm sạch trước khi render; ưu tiên **làm sạch phía server** rồi client làm
  sạch lần nữa như lớp phòng thủ thứ hai. Không bao giờ `dangerouslySetInnerHTML` với nội dung chưa làm sạch.
* **Content-Security-Policy.** Cấu hình qua HTTP header. Lưu ý riêng của dự án: Monaco Editor dùng web
  worker, và WebSocket cần `connect-src` cho scheme `wss:` — hai thứ này làm CSP của AlgoPrep không giống
  CSP mặc định của một trang tĩnh, phải viết có ý thức chứ không sao chép.
* **Không hiển thị message lỗi thô từ backend.** Backend trả **mã lỗi ổn định**; frontend ánh xạ mã lỗi
  thành thông báo cho người dùng. Mã lỗi chưa có bản dịch thì dùng thông báo chung.
* **Testcase ẩn.** Giao diện chỉ được hiện **trạng thái và chỉ số** của testcase `Hidden`, không hiện input
  và không hiện diff (`README.md` mục 4, F2). Đây là ràng buộc phải kiểm bằng test, vì nó rất dễ vỡ khi ai
  đó thêm một trường debug vào bảng kết quả.
* **Không tin phân quyền phía client.** Ẩn nút của `INSTRUCTOR`/`ADMIN` là chuyện trải nghiệm; quyền thật do
  backend kiểm.

---

## 5. Cổng chất lượng

Mọi thay đổi trong `05-coding/frontend/` phải qua **`pnpm check`** (typecheck + lint + Vitest) trước khi báo
xong; thay đổi luồng người dùng thì thêm `pnpm e2e` (`CLAUDE.md` mục Rules, `environment.md` mục 2.C).
**Báo PASS không kèm output lệnh thì không tính.**

Chiều import của FSD do `eslint-plugin-boundaries` chặn thật. Còn tiêu chí đặt tầng ở mục 2.A thì **người và
agent thi hành, máy không bắt được** — đừng trông chờ `pnpm lint` phát hiện một component đặt sai tầng.

---

## 6. Điều tài liệu này KHÔNG chốt
 
| Chưa chốt | Sẽ chốt ở |
| :--- | :--- |
| Danh sách màn hình chính thức và slug của chúng | `01-rd/screens/` (đã có hạt giống ở `system_survey.md` mục 7 và 31 prototype ở `09-layoutBase/`) |
| Bố cục và luồng tương tác từng màn | `02-bd/screens/<screen>.md` theo prototype `09-layoutBase/` |
| Hợp đồng API mà từng màn gọi | `03-dd/api/` — `screens/` chỉ liên kết tới, không định nghĩa lại |
| Hệ thống thiết kế, design token | Chưa chọn — chốt ở `02-bd/screens/` |
| Chế độ theme (Dark/Light) | **Đã chốt 2026-08-24 — hỗ trợ cả hai, Light là mặc định** (`DEC-2026-0824-dark-light-theme`) |
| Thư viện Markdown + LaTeX, sanitizer, biểu đồ | Khi dựng màn chi tiết bài toán và trang tiến độ |
| Đa ngôn ngữ giao diện (i18n) | **Đã chốt 2026-08-24 — có song ngữ vi/en** (`DEC-2026-0824-i18n-vi-en`), chi tiết chốt ở `02-bd/screens/` |
