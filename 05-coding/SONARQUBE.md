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
