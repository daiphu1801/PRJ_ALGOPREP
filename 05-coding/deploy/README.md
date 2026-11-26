# Triển khai AlgoPrep lên một VPS

Hướng dẫn deploy lần đầu bằng Docker Compose, dùng image đã được Release kiểm tra và đẩy lên GHCR. Phạm vi là demo nhóm nhỏ trên một máy. Kế hoạch tổng thể: `06-plan/nexa-plan/261125-1649-trien-khai-cicd-vps.md`. Tổng quan CI/CD: `12-knowledge/ci-cd-algoprep.md`.

## 1. Điều kiện trước khi bắt đầu

| Thứ cần có | Ghi chú |
| --- | --- |
| `images.env` | Artifact `release-evidence` của workflow **Release images** (chạy trên `main`). Chứa 3 dòng `FRONTEND_IMAGE`, `BACKEND_IMAGE`, `JUDGE_IMAGE` dạng `...@sha256:...`. Chưa có thì chưa deploy được |
| VPS Linux amd64 | Ubuntu 24.04, tối thiểu 2 vCPU và 8 GiB RAM để chạy thử. Cấu hình này chưa được đo, chỉ là điểm xuất phát |
| Tên miền trỏ về IP của VPS | Caddy cần tên miền thật để tự cấp HTTPS (xem mục 2) |
| Cổng 80 và 443 mở | Chỉ hai cổng này ra Internet. PostgreSQL, Redis, RabbitMQ, MinIO và go-judge nằm trong mạng Docker, không mở cổng |
| Host dùng cgroup | go-judge chạy `privileged` và mount `/sys/fs/cgroup` của host. Ubuntu 24.04 mặc định dùng cgroup v2, đúng yêu cầu |

## 2. Chọn nơi chạy (tối ưu chi phí với GitHub Student Pack)

Các ưu đãi bên dưới lấy từ trang [GitHub Student Developer Pack](https://education.github.com/pack). Điều khoản thay đổi theo thời gian và theo quốc gia, nên **kiểm tra lại trên trang chính thức và trong tài khoản của bạn trước khi dựa vào**.

| Lựa chọn | Điểm mạnh | Điểm cần kiểm tra |
| --- | --- | --- |
| **Azure for Students** | Credit cho sinh viên, không cần thẻ tín dụng (theo trang Pack) | Số credit, thời hạn, và danh sách vùng/cỡ VM được phép trong gói. Đưa VM về `Deallocated` khi không dùng để credit không bị trừ. Đĩa và IP tĩnh vẫn tính tiền |
| **DigitalOcean** | Droplet có IP cố định, giao diện đơn giản | Trang Pack ghi credit có thời hạn 1 năm. Cần xác nhận số tiền hiện tại và có yêu cầu thẻ hay không |
| Tên miền | Pack có ưu đãi tên miền miễn phí một năm | Chưa xác nhận được nhà cung cấp. Xem tên trong tài khoản Pack |

Gợi ý: nếu chỉ cần demo theo buổi, dùng Azure và tắt VM giữa các buổi. Nếu cần link chạy liên tục, dùng DigitalOcean cho đến khi hết credit.

**Chưa có tên miền?** Có thể dùng tên miền theo IP như `20-1-2-3.sslip.io` (dịch vụ sslip.io trả về IP nằm trong tên). Caddy vẫn xin được chứng chỉ Let's Encrypt cho tên này. Cách này **chưa được thử trong repo**; dùng được cho demo, không nên dùng lâu dài.

## 3. Chuẩn bị VPS

Đăng nhập SSH, rồi cài Docker Engine kèm Compose v2 theo [hướng dẫn chính thức](https://docs.docker.com/engine/install/ubuntu/). Kiểm tra:

```bash
docker --version
docker compose version
```

Mở tường lửa chỉ cho SSH, 80 và 443 (cả firewall của nhà cung cấp lẫn `ufw` nếu có bật). Không mở 5432, 6379, 5672, 15672, 9000, 9001, 5050.

Lấy mã nguồn, vì Compose tham chiếu tương đối tới `deploy/Caddyfile` và `database/init`. Chỉ cần thư mục `05-coding`:

```bash
git clone --depth 1 https://github.com/daiphu1801/PRJ_ALGOPREP.git
cd PRJ_ALGOPREP/05-coding
```

Nên checkout đúng commit đã phát hành (`release.json` trong artifact ghi commit), vì `database/init` và `deploy/` phải khớp với image.

## 4. Điền cấu hình

```bash
cp deploy/env.example deploy/.env
chmod 600 deploy/.env
```

Mở `deploy/.env` và điền:

| Biến | Cách lấy |
| --- | --- |
| `APP_HOST`, `APP_ORIGIN` | Tên miền, và `https://` cộng tên miền đó |
| `FRONTEND_IMAGE`, `BACKEND_IMAGE`, `JUDGE_IMAGE` | Copy từ `images.env` |
| `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `MINIO_ROOT_PASSWORD` | Tạo ngẫu nhiên: `openssl rand -base64 24` |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Cặp khóa RSA, xem bên dưới. Phải **giữ cố định**: đổi khóa thì mọi phiên đăng nhập hiện có mất hiệu lực |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản admin đầu tiên, cần khi khởi động lần đầu chưa có admin |
| `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | Dịch vụ gửi mail thật, cần cho OTP đăng ký. Cổng mặc định 587 với STARTTLS bắt buộc |
| `OAUTH_*`, `OPENROUTER_API_KEY`, `AI_*` | Chỉ điền khi bật tính năng tương ứng (đăng nhập GitHub/Google, AI). Có biến thì có tính năng, để trống thì tắt |
| `JUDGE_WORKERS`, `JUDGE_PARALLELISM`, `DB_POOL_SIZE` | Giữ mặc định (2, 2, 18) cho lần đầu rồi đo |

Tạo cặp khóa JWT:

```bash
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

Compose chấp nhận giá trị nhiều dòng đặt trong dấu nháy đơn trong `.env`. Dán nguyên nội dung hai file PEM vào `JWT_PRIVATE_KEY='...'` và `JWT_PUBLIC_KEY='...'`, rồi **xóa hai file `.pem`** khỏi VPS hoặc cất vào nơi lưu bí mật. `.env` và các khóa không bao giờ được vào Git (`*.env` đã nằm trong `.gitignore`).

Nếu package GHCR để private, đăng nhập trước: `docker login ghcr.io` bằng tài khoản có quyền đọc package.

## 5. Chạy lần đầu

Chạy từ thư mục `05-coding`:

```bash
docker compose --env-file deploy/.env -f deploy/compose.yml config --quiet
docker compose --env-file deploy/.env -f deploy/compose.yml pull
docker compose --env-file deploy/.env -f deploy/compose.yml up -d --wait --wait-timeout 240
docker compose --env-file deploy/.env -f deploy/compose.yml ps
curl --fail https://<APP_HOST>/healthz
```

Lần khởi động đầu Flyway tự chạy migration trên PostgreSQL trống. Nếu `up --wait` quá hạn, xem log dịch vụ lỗi: `docker compose --env-file deploy/.env -f deploy/compose.yml logs --tail=100 backend`.

## 6. Kiểm tra sau deploy

Đây là các bước bắt buộc trước khi mời người khác vào. Tick từng bước và ghi lại kết quả.

- [ ] `/healthz` trả 200 và `"status":"UP"` qua HTTPS với chứng chỉ hợp lệ
- [ ] Đăng ký tài khoản mới, nhận OTP qua email thật và đăng nhập được
- [ ] Đăng nhập bằng tài khoản admin trong `.env`
- [ ] Nộp một bài bằng cả Java, C++ và Python, nhận verdict cuối theo thời gian thực (WebSocket)
- [ ] Tải lên và tải xuống một file qua URL MinIO đã ký
- [ ] Nếu bật AI: một cuộc hội thoại stream từng phần (SSE) qua Caddy

## 7. Sao lưu và khôi phục

Làm trước mỗi lần nâng cấp, và **thử khôi phục ít nhất một lần bằng dữ liệu thử** trước khi tin vào bản sao lưu. Lưu bản sao lưu ra ngoài VPS.

```bash
# PostgreSQL
docker compose --env-file deploy/.env -f deploy/compose.yml exec -T postgres \
  pg_dump -U algoprep -d algoprep --format=custom > algoprep-$(date +%F).dump

# Khôi phục (vào database trống hoặc database thử)
docker compose --env-file deploy/.env -f deploy/compose.yml exec -T postgres \
  pg_restore -U algoprep -d algoprep --clean --if-exists < algoprep-YYYY-MM-DD.dump

# MinIO: sao lưu cả volume dữ liệu
docker run --rm -v algoprep-deploy_minio-data:/data -v "$PWD":/backup alpine \
  tar -czf /backup/minio-$(date +%F).tgz -C /data .
```

Tên volume phụ thuộc tên project Compose (`algoprep-deploy`); kiểm tra bằng `docker volume ls` nếu lệnh MinIO báo không thấy volume. Backup file cũng chứa dữ liệu người dùng, nên cần bảo vệ như `.env`.

## 8. Nâng cấp và quay lui

Nâng cấp: lấy `images.env` của bản phát hành mới, sao lưu (mục 7), thay 3 dòng `*_IMAGE` trong `deploy/.env`, rồi chạy lại `pull` và `up -d --wait`.

Quay lui: đặt lại 3 digest của bản tốt gần nhất và chạy lại `pull` cộng `up`. **Quay lui image không đảo ngược migration.** Nếu bản mới đã đổi schema, bản cũ có thể không chạy được với schema đó. Khi đó phải khôi phục PostgreSQL từ bản sao lưu trước nâng cấp. Vì vậy hãy luôn lưu lại digest của mỗi bản đã deploy và sao lưu trước khi nâng cấp.

## 9. Giới hạn đã biết

- go-judge chạy `privileged` cùng máy với ứng dụng. Chỉ phù hợp demo nhóm nhỏ. Muốn mở đăng ký công khai thì tách go-judge ra máy riêng trước.
- Một backend chứa API, worker, outbox và maintenance. Chưa tách.
- Khả năng chịu 120 VU chưa được đo trên VPS này. Đừng công bố con số trước khi đo theo `05-coding/k6/README.md`. Mốc cũ (2,12 đến 2,64 bài/s trên máy 8 nhân vật lý) chỉ để so sánh, một VPS 2 vCPU gần như chắc chắn thấp hơn.
- Image judge được build lại lúc Release, không lấy từ CI. Smoke chấm thật trong Release là chỗ kiểm tra nó.
