# REST starter P1 — F3

`GET /api/v1/harness/problems/{problemId}/starter-code?language=JAVA&model=FUNCTION_WRAPPER`.

JWT bắt buộc. language nhận JAVA/CPP/PYTHON, model nhận FUNCTION_WRAPPER/STANDARD_IO. model là tên query theo URL DD; submissionModel là tên field response. Response chỉ language, submissionModel, sourceCode; specVersion vẫn ở kết quả usecase nội bộ.

Bài published: mọi tài khoản đăng nhập. Bài unpublished: tác giả/admin. Deleted/missing/ngoài quyền cùng 404 HRN-001; principal lấy từ subject, category lấy authorities hiện hành do converter kiểm DB, không tin query caller/baseCategory hoặc claim ADMIN cũ. Không có quyền Function riêng cho API này theo DD.

HRN-010 là 400 cho ngôn ngữ thiếu/lạ. Thiếu/lạ model là 400 binding chuẩn Spring; DD chưa quy định mã miền riêng nên không dùng HRN-031 hệ thống cho lỗi binding. HRN-004/005 là 422, HRN-003 là 503, HRN-006/030/031 là 500. Adapter che toàn bộ raw message/cause/upstream data và không trả starter một phần. 401/403 dùng hạ tầng xác thực hiện có.

P1 kiểm O2 rồi O1 trong read-only REPEATABLE_READ đã có. F3 không giữ cache đặc tả; F2 reader hiện tại đọc DB trực tiếp, không tin key Redis problem:spec. Metadata/phiên bản thay đổi được đọc ở lần gọi sau; P2 từ chối expectedSpecVersion cũ. Không thêm subscriber no-op, không lưu/sinh mã từ sự kiện. Listener/cache-on chỉ cần khi có quyết định bật local cache sau đo hiệu năng; không coi các QA cache-on là PASS ở cấu hình hiện tại.

Không ghi domain DB/audit/outbox và không chạy source trong JVM. Kiểm nối starter→hoàn thiện lời giải→Run Code dùng F4 sandbox; starter Python có pass nên bản thân starter chưa phải lời giải đúng. FE starter/mock, preview C08, persisted grading D09 và nghiệm thu F3/F4 tổng theo checklist riêng.
