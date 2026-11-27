# SonarQube cho AlgoPrep

Workflow `.github/workflows/sonarqube.yml` phân tích hai project trên SonarQube Server hoặc Community Build: `algoprep-backend` (Maven đa module) và `algoprep-frontend` (Next.js/TypeScript). Backend dùng báo cáo JaCoCo XML sinh bởi `mvn install`; frontend dùng LCOV sinh bởi Vitest. Quality Gate thất bại sẽ làm job tương ứng thất bại.

## Kích hoạt

1. Tạo hai project trên SonarQube với đúng project key `algoprep-backend` và `algoprep-frontend`. Tạo token có quyền phân tích cả hai project.
2. Trong GitHub repository, đặt secret `SONAR_TOKEN`, variable `SONAR_HOST_URL` (URL mà GitHub runner truy cập được), và variable `SONAR_ENABLED=true`.
3. Chạy workflow **SonarQube** bằng `workflow_dispatch` để xác nhận kết nối và Quality Gate. Sau đó workflow chạy mỗi lần push lên `main`.

Máy chạy cục bộ có thể dùng các lệnh tương đương sau, sau khi đặt `SONAR_HOST_URL` và `SONAR_TOKEN` trong môi trường:

```sh
cd 05-coding/backend
./mvnw -B install org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar -Dsonar.projectKey=algoprep-backend -Dsonar.qualitygate.wait=true

cd ../frontend
pnpm exec vitest run --coverage
sonar-scanner -Dsonar.qualitygate.wait=true
```

Quality Gate và Quality Profile được quản lý trên SonarQube. Bản Community Build chỉ phân tích nhánh chính; nếu cần phân tích pull request và trang trí PR, dùng edition hỗ trợ tính năng đó. Không đưa token vào file cấu hình hoặc URL công khai.

## Quyết định rà Issue và Quality Gate `AlgoPrep`

Đánh dấu "Accepted" và "False positive" chỉ nằm trong cơ sở dữ liệu của SonarQube, nên mất khi server bị dựng lại. Repo giữ bản gốc ở `sonar/triage.json` (mỗi mục gồm quy tắc, tệp, hash của dòng bị báo, số Issue đã rà, quyết định và lý do) cùng gate `AlgoPrep` (coverage dưới 90 là lỗi, cộng các điều kiện của `Sonar way`).

Sau khi phân tích cả hai project, áp lại bằng tài khoản quản trị (`SONAR_LOGIN`, `SONAR_PASSWORD` trong `.env`; token phân tích không có quyền đánh dấu):

```sh
cd 05-coding
node --env-file=.env sonar/apply-triage.mjs          # chạy thử, không ghi gì
node --env-file=.env sonar/apply-triage.mjs --apply  # đánh dấu, tạo và gán gate
```

Một mục chỉ khớp khi dòng mã bị báo còn nguyên và số Issue không vượt số đã rà. Dòng đã đổi, hoặc có thêm một dòng giống hệt, thì Issue vẫn mở và script in ra để rà. Việc khớp chỉ dựa trên dòng bị báo, giống cách SonarQube tự theo dõi Issue: sửa cách dựng một chuỗi SQL ở dòng khác không làm Issue mở lại, nên vẫn phải xem khi review mã. Thêm mục mới vào `triage.json` là một quyết định chấp nhận rủi ro, cần ghi rõ lý do.
