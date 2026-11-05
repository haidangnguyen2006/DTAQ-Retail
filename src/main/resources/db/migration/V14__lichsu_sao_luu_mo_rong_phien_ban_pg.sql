-- DTAQ Retail - V14 mở rộng cột phiên bản PostgreSQL của lịch sử sao lưu
--
-- "pg_dump --version" trên Ubuntu dài hơn 50 ký tự, ví dụ
-- "pg_dump (PostgreSQL) 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)" (56 ký tự), làm mọi lần ghi lịch sử
-- sao lưu thất bại với lỗi 22001. Chỉ đổi kiểu cột; không đổi dữ liệu hay nghiệp vụ.
ALTER TABLE "LichSuSaoLuu" ALTER COLUMN "phienBanPg" TYPE TEXT;
