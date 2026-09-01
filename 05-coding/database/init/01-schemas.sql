-- AlgoPrep — khoi tao sau schema.
--
-- Script nay duoc mount vao /docker-entrypoint-initdb.d/ cua image postgres
-- (01-rd/system/environment.md muc 2) va CHI CHAY LAN DAU, khi volume con rong. Doi file nay roi
-- `docker compose up` lai se KHONG chay lai — phai `docker compose down -v`.
--
-- Sau schema, khong phai sau database: Modular Monolith dung MOT PostgreSQL instance
-- (DEC-2026-0820-architecture-baseline), ranh gioi giua cac Bounded Context la schema.
--
-- Ten schema NGAN HON ten Bounded Context o ba cho — day la cho hay sai
-- (01-rd/system/codebase_structure.md muc 3):
--   problem-bank        -> problem
--   judge-orchestration -> judge
--   ai-review           -> ai
-- Bang anh xa day du: .nexa/domain-registry.json
--
-- File nay KHONG tao bang. Bang, cot, khoa, index thuoc 02-bd/database/<module>.md, tai lieu chua
-- ton tai. Migration (Flyway/Liquibase) cung chua chot — xem 05-coding/backend/README.md.

CREATE SCHEMA IF NOT EXISTS identity;
COMMENT ON SCHEMA identity IS
  'F1 identity — xac thuc JWT, phan quyen STUDENT/INSTRUCTOR/ADMIN, tien do ca nhan';

CREATE SCHEMA IF NOT EXISTS problem;
COMMENT ON SCHEMA problem IS
  'F2 problem-bank — de bai, dac ta ham theo ngon ngu, testcase Sample/Hidden, phien ban bo testcase';

CREATE SCHEMA IF NOT EXISTS harness;
COMMENT ON SCHEMA harness IS
  'F3 harness — luoc do kieu doc lap ngon ngu, template sinh ma, anh xa loi bien dich';

CREATE SCHEMA IF NOT EXISTS judge;
COMMENT ON SCHEMA judge IS
  'F4 judge-orchestration — bai nop, ket qua tung testcase, cau hinh ngon ngu';

CREATE SCHEMA IF NOT EXISTS ai;
COMMENT ON SCHEMA ai IS
  'F5 ai-review — Solution Review mot luot, Mock Interview nhieu luot, rubric, dem token';

CREATE SCHEMA IF NOT EXISTS interview_bank;
COMMENT ON SCHEMA interview_bank IS
  'F6 interview-bank — ngan hang cau hoi ly thuyet, che do hoc, che do luyen, tien do';

-- Ung dung KHONG dung schema `public`. Xoa quyen tao doi tuong trong `public` de mot Entity thieu
-- khai bao schema se DO NGAY thay vi am tham tao bang o `public` — do la cach ranh gioi schema bi
-- pha trong im lang.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- search_path tuong minh, khong de mac dinh. Thu tu nay khong ngu y quyen truy cap: moi module chi
-- duoc cham schema cua chinh no, va dieu do do tang ung dung bao dam chu khong do search_path.
ALTER DATABASE algoprep
  SET search_path TO identity, problem, harness, judge, ai, interview_bank, public;
