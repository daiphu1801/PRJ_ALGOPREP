# BD — Kiến trúc module `harness` (F3)

> Trạng thái: **chốt sơ bộ 2026-08-24** — mới ghi chủ đích kiến trúc, chưa triển khai chi tiết (interface,
> registry, hợp đồng dữ liệu từng phương thức). Triển khai sâu chuyển sang `03-dd/logic/harness.md` theo
> đúng thứ tự BD → DD của `CLAUDE.md` mục Process.

## 1. Bối cảnh

`01-rd/req/req.md` mục F3 khoá cứng đúng ba ngôn ngữ nộp bài: Java, C++, Python
(`01-rd/req/req.md:92-104`). Không mở rộng thêm ngôn ngữ ở tầng yêu cầu — xem quyết định giữ nguyên phạm vi
tại `06-plan/PROTOTYPE_DEBT.md` mục 1.1 (chốt 2026-08-24, chọn Phương án A: bỏ tính năng "Thêm ngôn ngữ"
khỏi giao diện Admin).

## 2. Chủ đích kiến trúc — sơ bộ

Dù phạm vi nghiệp vụ dừng ở ba ngôn ngữ, module sinh mã bọc hàm (`harness`) nên tổ chức theo
**Open-Closed Principle** thông qua **Strategy/Plugin pattern nội bộ**, thay vì rẽ nhánh `if/switch-case`
cứng theo tên ngôn ngữ:

- Mỗi ngôn ngữ (Java, C++, Python) là một plugin độc lập, cùng tuân theo một interface chung (tên gọi và
  chữ ký cụ thể chốt ở DD), tối thiểu bao gồm các trách nhiệm đã liệt kê ở `req.md` F3-01 tới F3-12:
  parse/khai báo kiểu (F3-01), sinh mã đọc input (F3-02), gọi hàm người dùng (F3-03), sinh mã in kết quả
  (F3-04), tiêm mã người dùng (F3-05), đóng gói theo định dạng adapter (F3-06), so khớp kết quả
  (F3-07 tới F3-10), ánh xạ lỗi biên dịch về đúng dòng người dùng (F3-11), che giấu lỗi thuộc mã harness
  (F3-12).
- Một registry nội bộ tra cứu plugin theo mã ngôn ngữ, thay cho `switch(language)` rải rác trong logic F4
  hoặc F5.
- Đây là **quyết định chất lượng kỹ thuật nội bộ của module `harness`**, không phơi ra giao diện Admin,
  không cần bản ghi quyết định (`DEC-`) mới, không đổi phạm vi đã khoá ở mục 1.

## 3. Không chọn (đã cân nhắc và loại)

- **Admin tự định nghĩa ngôn ngữ mới qua UI** (template engine + type mapping JSON + regex lỗi biên dịch tự
  nhập): rủi ro bảo mật — cho phép định nghĩa mã bọc hàm tuỳ ý ngoài kiểm soát của sandbox go-judge; đồng
  thời đòi Admin (actor A3) phải hiểu sâu lập trình, không khớp vai trò A3 trong RD.
- **Mở toàn bộ ngôn ngữ có sẵn của Judge0** (60+ ngôn ngữ): dựa trên giả định Judge0 là engine chính, trong
  khi `DEC-2026-0823-go-judge-default-engine` đã chốt go-judge là engine mặc định, Judge0 chỉ còn là
  adapter thay thế hợp lệ.

## 4. Việc còn mở — chuyển sang DD

- Tên và chữ ký cụ thể của interface plugin, cơ chế đăng ký registry, vị trí đặt code (Maven module nào)
  — viết ở `03-dd/logic/harness.md` khi vào giai đoạn DD của module `harness`.
- Test golden-file so khớp mã sinh ra cho cả ba ngôn ngữ đã có yêu cầu ở `req.md:260` — DD cần chỉ rõ cách
  tổ chức test theo từng plugin.

## 5. Tham chiếu

- `01-rd/req/req.md:92-104` — đặc tả F3-01 tới F3-13.
- `06-plan/PROTOTYPE_DEBT.md` mục 1.1 — quyết định giữ khoá 3 ngôn ngữ, Phương án A.
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`.
