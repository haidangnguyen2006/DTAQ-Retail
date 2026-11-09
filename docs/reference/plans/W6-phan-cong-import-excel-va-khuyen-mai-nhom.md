---
title: "WAVE 6 — PHÂN CÔNG & HƯỚNG DẪN"
subtitle: "Nhập Excel theo định dạng chuẩn và khuyến mãi nhiều dòng sản phẩm — DTAQ Retail"
date: "2026-11-09"
lang: vi-VN
---

**Dự án:** DTAQ Retail · **Phiên bản:** 1.4 · **Trạng thái:** Đã chốt toàn bộ quyết định · **Ngày lập:** 2026-11-09

# Mục lục

- [0. Tóm tắt điều hành](#0-tóm-tắt-điều-hành)
- [1. Căn cứ, phạm vi và cách đọc](#1-căn-cứ-phạm-vi-và-cách-đọc)
- [2. Quyết định đã chốt](#2-quyết-định-đã-chốt)
- [3. Phạm vi và nguyên tắc triển khai](#3-phạm-vi-và-nguyên-tắc-triển-khai)
- [4. Phân công tổng quan](#4-phân-công-tổng-quan)
- [5. Chi tiết từng thành viên](#5-chi-tiết-từng-thành-viên)
- [6. Hợp đồng liên thành viên](#6-hợp-đồng-liên-thành-viên)
- [7. Định dạng Excel chuẩn](#7-định-dạng-excel-chuẩn)
- [8. Thiết kế khuyến mãi nhiều dòng sản phẩm](#8-thiết-kế-khuyến-mãi-nhiều-dòng-sản-phẩm)
- [9. Lộ trình Wave 6](#9-lộ-trình-wave-6)
- [10. Kiểm thử và Definition of Done](#10-kiểm-thử-và-definition-of-done)
- [11. Rủi ro và điểm chặn](#11-rủi-ro-và-điểm-chặn)
- [12. Danh sách file dự kiến](#12-danh-sách-file-dự-kiến)
- [13. Checklist bàn giao](#13-checklist-bàn-giao)
- [14. Nhật ký thay đổi](#14-nhật-ký-thay-đổi)
- [Phụ lục A — Hiện trạng mã nguồn đã đối chiếu](#phụ-lục-a--hiện-trạng-mã-nguồn-đã-đối-chiếu)
- [Phụ lục B — Truy vết UC/RB](#phụ-lục-b--truy-vết-ucrb)
- [Phụ lục C — File dùng chung cần tạo trước](#phụ-lục-c--file-dùng-chung-cần-tạo-trước)
- [Phụ lục D — Trạng thái quyết định](#phụ-lục-d--trạng-thái-quyết-định)

# 0. Tóm tắt điều hành

Wave 6 bổ sung hai nhóm tính năng:

1. **Nhập dữ liệu từ Excel theo định dạng chuẩn** cho ba trang: Quản lý khuyến mãi, Quản lý sản phẩm, Quản lý dòng sản phẩm. Mỗi trang có **Tải mẫu**, **Nhập Excel**, xem trước, báo lỗi theo dòng/cột và chỉ ghi khi toàn bộ tệp hợp lệ.
2. **Khuyến mãi nhiều dòng sản phẩm**: một chương trình khuyến mãi có thể áp cho nhiều danh mục và nhiều dòng sản phẩm. Người dùng chọn dòng cụ thể hoặc chọn nhiều danh mục kèm điều kiện `tồn >= N ngày`; hệ thống query ra các dòng thỏa điều kiện và lưu thành **một chương trình + nhiều chi tiết khuyến mãi**.

| Trường | Giá trị |
|---|---|
| Dự án | DTAQ Retail |
| Đợt | **Wave 6** (theo yêu cầu chủ dự án; bộ tài liệu hiện tại không có Wave 5) |
| Phiên bản tài liệu | 1.3 — ngày 2026-11-09 |
| Trạng thái | **Đã chốt D18–D28 và Q3/Q5/Q7/Q8/Q9** |
| Phạm vi phiên này | Chỉ viết tài liệu phân công và hướng dẫn; **không sửa mã nguồn** |
| Thành viên tham gia | **Đăng**, **Tài**, **Quý**; Tuấn Anh không có hạng mục bắt buộc |
| Căn cứ chính | `AGENTS.md`; `docs/reference/README.md` §5.1; `work-assignment-4-members.md` v3.7; UC003/UC008/UC009/UC012; schema V1–V14; mã nguồn `develop` |
| Tài liệu liên quan | [Phân công 4 thành viên](work-assignment-4-members.md) · [guide-tai.md](guide-tai.md) · [guide-quy.md](guide-quy.md) · [guide-tuan-anh.md](guide-tuan-anh.md) |

> **Thay đổi schema đã được duyệt:** thêm bảng `ChiTietKhuyenMai` để một chương trình khuyến mãi có nhiều dòng sản phẩm. `KhuyenMai` trở thành bảng đầu chương trình; `maDongSP` chuyển sang `ChiTietKhuyenMai` theo **hai giai đoạn**: V15 (additive, giữ `KhuyenMai.maDongSP`) rồi V16 (bỏ cột sau khi mã của Tài merge). Xem [Ghi chú triển khai thực tế — bàn giao cho Tài](#ghi-chú-triển-khai-thực-tế--bàn-giao-cho-tài).

## 0.1. Kết quả bàn giao mong đợi

| # | Kết quả | Người chính |
|---|---|---|
| 1 | Migration V15: bảng `ChiTietKhuyenMai`, backfill dữ liệu cũ (**additive — giữ** `KhuyenMai.maDongSP`); V16 bỏ cột sau khi mã Tài merge | Đăng |
| 2 | Thư viện đọc/ghi Excel, hộp thoại xem trước dùng chung | Đăng |
| 3 | UC003 áp dụng đúng khuyến mãi qua bảng chi tiết, giữ snapshot hóa đơn | Đăng, Tài |
| 4 | Tạo/xem trước khuyến mãi nhiều dòng; nhập Excel khuyến mãi | Tài |
| 5 | Nhập Excel sản phẩm (serial) | Quý |
| 6 | Nhập Excel dòng sản phẩm | Quý |
| 7 | Test JUnit 5, `mvn test` xanh, báo cáo cuối đợt | Cả ba |

# 1. Căn cứ, phạm vi và cách đọc

## 1.1. Nguồn sự thật

- Yêu cầu nghiệp vụ và use case: `docs/reference/requirements/`, `docs/reference/use-cases/`. Báo cáo DOCX gốc vẫn là nguồn tham chiếu cuối cùng và **không bị sửa**.
- Quyết định implementation: `docs/reference/README.md` §5.1 và D1–D17 trong `work-assignment-4-members.md`.
- Mã nguồn `develop` là hiện trạng kỹ thuật, không phải nguồn nghiệp vụ.
- Kiến trúc: `ui → controller → dao → dao/impl → entity`; không SQL trong UI; tiền qua `BigDecimal`/`TienUtil`; mã qua `MaGenerator`.

## 1.2. Vì sao gọi là Wave 6

Chủ dự án yêu cầu đặt tên đợt này là **Wave 6**. Bộ tài liệu hiện tại chưa có Wave 5; tài liệu không khẳng định đã có Wave 5. Đây là cách đánh số theo yêu cầu.

## 1.3. Ngoài phạm vi

- Nhập Excel cho danh mục, khách hàng, nhân viên, nhà cung cấp, hóa đơn, phiếu trả/đổi.
- Cập nhật/upsert bản ghi khuyến mãi đã có bằng Excel.
- Import CSV; chỉ `.xlsx`.
- Khuyến mãi theo từng serial; Wave 6 vẫn theo dòng sản phẩm.
- Voucher, tích điểm, khuyến mãi chuyên sâu.
- Thay đổi quy tắc làm tròn tiền (C-11), quyền sửa giá/thuế (C-08), đổi/trả (D17).
- Thiết kế lại giao diện ngoài nút nhập và vùng điều kiện.

## 1.4. Mâu thuẫn nguồn cần theo dõi

| Mã | Nội dung | Ảnh hưởng Wave 6 | Cách xử lý |
|---|---|---|---|
| C-04 | Chương 1 loại trừ phân hệ khuyến mãi nhưng UC012/UC003 vẫn mô tả | Khuyến mãi nhiều dòng nằm trong phạm vi đã chốt | Giữ quyết định 2026-10-24 |
| C-07 | Nguồn mô tả tồn theo số lượng và theo serial | Điều kiện ngày tồn dùng `SanPham` | Giữ mô hình serial V1 |
| C-08 | Chỉ Quản lý sửa giá/thuế | Nhập dòng sản phẩm có giá/thuế | Q5: cả 2 actor; import chỉ tạo mới |
| C-11 | Làm tròn VND, 0 chữ số, HALF_UP | Không tính lại tiền khi nhập | Không đổi |
| D7/C-12 | Mã do ứng dụng sinh | Template không có cột mã | `MaGenerator` sinh khi lưu |
| D16/RB37 | `soLuongTon` là bộ đếm dẫn xuất | Nhập sản phẩm không nhập tồn | Trigger + controller tính lại |
| D17 | Đổi hàng không hoàn tiền | Không liên quan trực tiếp | Không đổi |

# 2. Quyết định đã chốt

Toàn bộ quyết định dưới đây đã được chủ dự án trả lời ngày **2026-11-09**. Không còn điểm chặn trước khi code.

| Mã | Nội dung | Quyết định |
|---|---|---|
| **D18** | Schema khuyến mãi | ✅ **Thêm bảng `ChiTietKhuyenMai`**; `KhuyenMai` là đầu chương trình; `maDongSP` rời `KhuyenMai` theo 2 bước: **V15 additive giữ cột**, **V16 bỏ cột** sau khi mã Tài merge. |
| **D19** | Điều kiện tạo khuyến mãi | ✅ Một chương trình có **nhiều danh mục** và nhiều dòng sản phẩm. Điều kiện danh mục/ngày tồn là optional; có thể chọn dòng cụ thể + ngày, hoặc nhiều danh mục + ngày, hoặc nhiều danh mục không ngày. |
| **D20** | Ngưỡng tuổi tồn | ✅ `>= N`: `ngayNhapKhoBanDau <= ngayXet - N`; tính từ máy `SAN_SANG_BAN` còn tồn. |
| **D21** | Xung đột | ✅ Kiểm tra **từng dòng sản phẩm** trong chi tiết; nếu một dòng xung đột thì chặn cả chương trình. |
| **D22** | Lỗi nhập Excel | ✅ Có bất kỳ lỗi thì **chặn toàn bộ tệp**; không ghi dòng nào; xuất báo cáo lỗi. |
| **D23** | Template khuyến mãi | ✅ Một chương trình gồm nhiều dòng Excel; dùng cột **`Mã nhóm`** để gộp các dòng cùng một chương trình. |
| **D24** | Quyền nhập Excel | ✅ **Cả hai actor** được nhập; được **bật áp dụng ngay** theo giá trị trong tệp (Q9). Đây là ngoại lệ so với tác nhân UC012, đã được chủ dự án chấp thuận. |
| **D25** | Thư viện dùng chung | ✅ Chỉ dùng POI 5.4.1; chỉ Đăng tạo `ExcelImporter`, `ExcelTemplate`, `ImportPreviewDialog`. |
| **D26** | Nhập sản phẩm | ✅ Một transaction; tồn kho tính lại; chỉ nhận trạng thái `SAN_SANG_BAN`. |
| **D27** | Snapshot lịch sử | ✅ Hóa đơn giữ `maKM`, `tyLeGiamTaiLucBan`, tiền tại thời điểm bán; không tính lại hóa đơn cũ. |
| **D28** | Bảng chi tiết | ✅ Một `KhuyenMai` có nhiều `ChiTietKhuyenMai`; một chi tiết thuộc đúng một khuyến mãi; chi tiết gồm `maKM` và `maDongSP`. |

**Chốt bổ sung:**

| Mã | Nội dung | Quyết định |
|---|---|---|
| **Q3** | Nhiều danh mục | ✅ Một chương trình có nhiều danh mục; lưu qua nhiều dòng chi tiết (mỗi dòng sản phẩm một chi tiết). |
| **Q5** | Nhập dòng sản phẩm có giá/thuế | ✅ Cho **cả 2 actor**; import chỉ tạo mới, không sửa dòng cũ. |
| **Q7** | Vi phạm khóa ngoại | ✅ **Chặn**; không ghi bản ghi vi phạm; báo lỗi dòng/cột. |
| **Q8** | Template `.xlsx` | ✅ **Commit** ba template tĩnh vào `docs/reference/templates/`. |
| **Q9** | Nhân viên bán hàng nhập khuyến mãi | ✅ Được **bật áp dụng ngay** theo cột `Đang áp dụng` trong tệp; không ép `false`. |

# 3. Phạm vi và nguyên tắc triển khai

## 3.1. Trong phạm vi

| Nhóm | Hạng mục | Kết quả |
|---|---|---|
| Schema | `ChiTietKhuyenMai` | Một chương trình nhiều dòng; migration + backfill |
| Nhập Excel | Khuyến mãi | Một hoặc nhiều dòng Excel gộp thành một chương trình qua `Mã nhóm` |
| Nhập Excel | Sản phẩm | Nhập serial, kiểm tra NCC/dòng/ngày/trạng thái, tồn đúng RB37 |
| Nhập Excel | Dòng sản phẩm | Nhập dòng mới, danh mục/ĐVT/giá/thuế, tồn ban đầu 0 |
| Khuyến mãi | Tạo theo điều kiện | Chọn dòng hoặc nhiều danh mục + ngày; query ra N dòng; lưu 1 chương trình + N chi tiết |
| Khuyến mãi | Áp dụng khi bán | UC003 dùng `findHieuLuc` qua bảng chi tiết; snapshot không đổi |

## 3.2. Ngoài phạm vi

- Sửa/xóa hàng loạt chi tiết khuyến mãi sau khi tạo; UI chỉ tạo và bật/dừng.
- Import cập nhật khuyến mãi đã có.
- Khuyến mãi theo từng serial.
- Rule engine nhiều điều kiện OR phức tạp; Wave 6 hỗ trợ nhiều danh mục + ngày.
- Lịch sử nhập Excel.
- Thay đổi UI ngoài phạm vi nêu trên.

## 3.3. Nguyên tắc bắt buộc

1. Không SQL trong UI.
2. Tiền dùng `BigDecimal`, không `double`.
3. Mã do `MaGenerator` sinh trong transaction.
4. Tạo chương trình + chi tiết phải nằm trong một transaction.
5. Không hard-code credential; không log dữ liệu nhạy cảm.
6. Không sửa file của người khác; `pom.xml`, migration, util dùng chung do Đăng sở hữu.
7. Không tuyên bố đã kiểm tra Figma/DOCX nếu chưa thực sự mở.

# 4. Phân công tổng quan

## 4.1. Bảng phân công

| Thành viên | Vai trò Wave 6 | Hạng mục chính | Ước lượng |
|---|---|---|---|
| **Đăng** | Hạ tầng dùng chung + migration + đăng ký trang | Migration V15; `ExcelImporter`; `ExcelTemplate`; `ImportPreviewDialog`; `AppNavigator`/`NavigationModel`; rà soát UC003 | ~32 giờ |
| **Tài** | UC012 + schema/entity/DAO + UI + nhập Excel khuyến mãi | `ChiTietKhuyenMai`; `KhuyenMai`; DAO; controller; dialog; trang; import; test | ~48 giờ |
| **Quý** | UC008 + UC009 + nhập Excel sản phẩm/dòng sản phẩm | Hàm nhập hàng loạt; UI; validate; test; RB37 | ~32 giờ |
| **Tuấn Anh** | Không có hạng mục bắt buộc | Review chéo UC003/bảo mật tệp nếu rảnh | Không tính |

Tổng ước lượng: **~112 giờ**, chưa gồm chờ duyệt/sửa lỗi.

## 4.2. Vì sao ba thành viên

- **Tài** sở hữu UC012 và `KhuyenMaiDao`, trực tiếp làm schema chi tiết, controller, UI và import.
- **Quý** sở hữu UC008/UC009, làm hai trang nhập còn lại.
- **Đăng** sở hữu `pom.xml`, migration, `ExcelExporter`, `AppNavigator`, `NavigationModel`; là điểm chặn hạ tầng.
- **Tuấn Anh** không có file bắt buộc phải sửa; có thể review.

## 4.3. Cân tải

- Đăng là điểm chặn: migration + importer/template phải xong trước W6.2.
- Tài nặng nhất: schema, DAO, controller, UI, import.
- Quý có hai luồng nhập tương tự, dùng chung `ImportPreviewDialog`.
- Review chéo ở W6.3–W6.4.

# 5. Chi tiết từng thành viên

## Ghi chú triển khai thực tế — bàn giao cho Tài

> **Trạng thái thực tế (2026-11-09):** `V15__chi_tiet_khuyen_mai.sql` đã được hiện thực theo hướng **cộng thêm (additive)**. V15 **không** bỏ `KhuyenMai.maDongSP` và **không** bỏ FK `fk_khuyenmai_dongsanpham`; mã hiện tại vẫn chạy bình thường. Việc bỏ cột được tách sang **migration V16**, chỉ thực hiện **sau khi** mã của Tài đã merge.

### Trình tự bắt buộc (3 bước)

| Bước | Ai | Nội dung | Trạng thái |
|---|---|---|---|
| 1 | Đăng | **V15 additive**: tạo `ChiTietKhuyenMai`, backfill từ `KhuyenMai.maDongSP`, tạo index tra cứu. Giữ nguyên `KhuyenMai.maDongSP`, FK `fk_khuyenmai_dongsanpham` và index cũ. | ✅ Đã xong |
| 2 | Tài | Cập nhật entity/DAO/controller/UI để đọc–ghi qua `ChiTietKhuyenMai` (danh sách chi tiết). | ⏳ Chờ merge |
| 3 | Đăng (hoặc Tài) | Thêm **V16** để bỏ `KhuyenMai.maDongSP`, FK `fk_khuyenmai_dongsanpham` và index cũ trên cột này. | ⛔ Chỉ sau bước 2 |

### Hợp đồng schema của V15 (đã hiện thực)

```sql
CREATE TABLE "ChiTietKhuyenMai" (
  "maKM" VARCHAR NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  CONSTRAINT pk_chitietkhuyenmai PRIMARY KEY ("maKM", "maDongSP"),
  CONSTRAINT fk_chitietkhuyenmai_khuyenmai FOREIGN KEY ("maKM")
    REFERENCES "KhuyenMai" ("maKM") ON DELETE CASCADE,
  CONSTRAINT fk_chitietkhuyenmai_dongsanpham FOREIGN KEY ("maDongSP")
    REFERENCES "DongSanPham" ("maDongSP") ON DELETE RESTRICT
);

INSERT INTO "ChiTietKhuyenMai" ("maKM", "maDongSP")
SELECT "maKM", "maDongSP" FROM "KhuyenMai";

CREATE INDEX idx_ctkm_dong_san_pham
  ON "ChiTietKhuyenMai" ("maDongSP", "maKM");
```

- PK chi tiết: `("maKM", "maDongSP")` — một dòng sản phẩm chỉ xuất hiện một lần trong một chương trình.
- FK `"maKM"` → `"KhuyenMai"("maKM")` **ON DELETE CASCADE**; FK `"maDongSP"` → `"DongSanPham"("maDongSP")` **ON DELETE RESTRICT**.
- Backfill 1-1: mỗi `KhuyenMai` cũ trở thành một header + đúng một chi tiết.
- Index `(maDongSP, maKM)` phục vụ tra cứu theo dòng sản phẩm (`findHieuLuc`, `existsChongLan`).

### V15 cố ý KHÔNG làm

- Không `ALTER TABLE "KhuyenMai" DROP CONSTRAINT fk_khuyenmai_dongsanpham;`
- Không `ALTER TABLE "KhuyenMai" DROP COLUMN "maDongSP";`
- Không đổi `ChiTietHoaDon.maKM`, không đổi mapper hóa đơn/trả/đổi.

Hai lệnh `DROP` trên thuộc **V16**, không thuộc V15.

### Phạm vi Tài cần cập nhật ở bước 2

| Khu vực | Việc cần làm |
|---|---|
| `entity/KhuyenMai.java` | Thêm `List<ChiTietKhuyenMai> chiTiet`; giữ constructor cũ `(..., DongSanPham)` cho mapper hóa đơn (tạo đúng một chi tiết). |
| `entity/ChiTietKhuyenMai.java` | Entity mới: `maKM` + `DongSanPham` (hoặc `maDongSP`). |
| `dao/impl/KhuyenMaiDaoImpl.java` | `findHieuLuc` join `ChiTietKhuyenMai`; `findByDongSanPham`; `insert` header + từng chi tiết (`insert(Connection, KhuyenMai)`); `existsChongLan` kiểm tra **từng chi tiết**; `findDongTheoDieuKien` hỗ trợ **nhiều danh mục**. |
| `controller/KhuyenMaiController.java` | Tạo 1 header + N chi tiết trong một transaction; validate; import gộp `Mã nhóm`. |
| `ui/page/promotion/PromotionsPage.java` | Một dòng/chương trình; cột số dòng lấy từ `chiTiet`; không còn gọi `dongSanPham()`. |

### Điều kiện để chạy V16

1. Mã của Tài đã merge và không còn đọc `KhuyenMai.maDongSP`.
2. `findHieuLuc`, `findByDongSanPham`, `existsChongLan`, `insert`, `findDongTheoDieuKien` đã dùng `ChiTietKhuyenMai`.
3. `PromotionsPage`/`KhuyenMaiController` không còn phụ thuộc cột cũ.
4. Test hồi quy UC003 xanh trên CSDL đã có dữ liệu backfill.

V16 khi đó gồm: `ALTER TABLE "KhuyenMai" DROP CONSTRAINT fk_khuyenmai_dongsanpham;` → `DROP INDEX` cũ (nếu có) → `ALTER TABLE "KhuyenMai" DROP COLUMN "maDongSP";`.

## 5.1. Đăng — hạ tầng dùng chung, migration, đăng ký trang

### 5.1.1. Hạng mục

| Mã | Hạng mục | File dự kiến | Kết quả |
|---|---|---|---|
| D6-01 | Migration V15 | `src/main/resources/db/migration/V15__chi_tiet_khuyen_mai.sql` | Tạo `ChiTietKhuyenMai`; backfill; **giữ** `KhuyenMai.maDongSP`; index/FK chi tiết (bỏ cột là V16) |
| D6-02 | Thư viện đọc Excel | `util/ExcelImporter.java` | Đọc `.xlsx`, validate, trả lỗi dòng/cột |
| D6-03 | Thư viện ghi template | `util/ExcelTemplate.java` | Sinh 3 template + sheet hướng dẫn |
| D6-04 | Hộp thoại xem trước | `ui/component/ImportPreviewDialog.java` | Xem trước/lỗi/nút nhập dùng chung |
| D6-05 | Đăng ký trang/menu | `ui/AppNavigator.java`, `ui/shell/NavigationModel.java` | Nhân viên bán hàng vào trang nhập; ẩn hành động Quản lý |
| D6-06 | Rà soát UC003 | `BanHangController` (không sửa logic) | `findHieuLuc` qua bảng chi tiết vẫn trả đúng |
| D6-07 | Hợp đồng/review/test | Tài liệu + test | Chốt API, review chéo, `mvn test` xanh |

### 5.1.2. Migration V15 — DDL và backfill

```sql
CREATE TABLE "ChiTietKhuyenMai" (
  "maKM" VARCHAR NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  CONSTRAINT pk_chitietkhuyenmai PRIMARY KEY ("maKM", "maDongSP"),
  CONSTRAINT fk_chitietkhuyenmai_khuyenmai FOREIGN KEY ("maKM")
    REFERENCES "KhuyenMai" ("maKM") ON DELETE CASCADE,
  CONSTRAINT fk_chitietkhuyenmai_dongsanpham FOREIGN KEY ("maDongSP")
    REFERENCES "DongSanPham" ("maDongSP") ON DELETE RESTRICT
);

-- Chuyển dữ liệu cũ: mỗi KhuyenMai hiện tại thành một chi tiết
INSERT INTO "ChiTietKhuyenMai" ("maKM", "maDongSP")
SELECT "maKM", "maDongSP" FROM "KhuyenMai";

-- V15 DỪNG Ở ĐÂY: KHÔNG bỏ cột, KHÔNG bỏ FK.
-- Hai lệnh dưới đây thuộc V16, chỉ chạy sau khi mã của Tài merge:
--   ALTER TABLE "KhuyenMai" DROP CONSTRAINT fk_khuyenmai_dongsanpham;
--   ALTER TABLE "KhuyenMai" DROP COLUMN "maDongSP";

CREATE INDEX idx_ctkm_dong_san_pham
  ON "ChiTietKhuyenMai" ("maDongSP", "maKM");
```

**Lưu ý:**

- Chạy backup trước migration.
- V15 là **additive**: `KhuyenMai.maDongSP` và FK `fk_khuyenmai_dongsanpham` vẫn còn sau V15.
- Việc bỏ `KhuyenMai.maDongSP` là **V16** (xem [Ghi chú triển khai thực tế — bàn giao cho Tài](#ghi-chú-triển-khai-thực-tế--bàn-giao-cho-tài)).
- V15 là bản kế tiếp sau V14; kiểm tra nhánh khác không dùng trùng số.
- Không sửa V1–V14.
- `ChiTietHoaDon.maKM` vẫn tham chiếu `KhuyenMai`; không đổi.
- Nếu CSDL dev đã có V15 khác, tạo lại CSDL hoặc `flyway repair` theo quy trình nhóm.
- Rollback: phục hồi backup; không viết migration down.

### 5.1.3. Hợp đồng `ExcelImporter`

```java
public final class ExcelImporter {
  public static ImportResult doc(File file, ImportSpec spec);
  public static void xuatBaoCaoLoi(File file, ImportResult result);

  public record ImportSpec(String tenSheet, List<CotNhap> cot, int soDongToiDa) {}
  public record CotNhap(
      String tieuDe, KieuDuLieu kieu, boolean batBuoc, List<String> giaTriChoPhep) {}
  public enum KieuDuLieu { CHUOI, SO_NGUYEN, SO_THAP_PHAN, NGAY, LOGIC, ENUM }
  public record ImportRow(int soDongExcel, Map<String, Object> giaTri) {}
  public record ImportError(int soDongExcel, String cot, String maLoi, String thongBao) {}
  public record ImportResult(List<ImportRow> dong, List<ImportError> loi, int tongSoDong) {
    public boolean hopLe() { return loi.isEmpty(); }
  }
}
```

Quy ước:

- Chỉ đọc sheet `DuLieu`; bỏ qua `HuongDan`.
- Tiêu đề dòng 1; dữ liệu từ dòng 2.
- Thiếu cột bắt buộc → lỗi cấp tệp; cột lạ → cảnh báo, bỏ qua.
- Chỉ `.xlsx`; từ chối `.xls`, `.xlsm`, macro, liên kết ngoài.
- Ô công thức: chỉ đọc cache; không đánh giá.
- Giới hạn 5.000 dòng, 10 MB, 50 cột; tối đa 200 lỗi hiển thị.
- Không log nội dung ô; chỉ log tên tệp/số dòng/số lỗi.

### 5.1.4. Hợp đồng `ExcelTemplate`

- `ExcelTemplate.xuat(File file, ImportSpec spec, List<Object[]> viDu)`.
- Mỗi tệp có `DuLieu` + `HuongDan`.
- Tiêu đề in đậm, nền xám, khóa dòng 1; ngày `dd/MM/yyyy`; số `#,##0`.
- Không chứa mã định danh/công thức/dữ liệu thật.
- Ba template tĩnh được commit: `docs/reference/templates/mau_nhap_khuyen_mai.xlsx`, `mau_nhap_san_pham.xlsx`, `mau_nhap_dong_san_pham.xlsx`.
  - Trạng thái thực tế 2026-11-09: cả ba tệp **đã tồn tại** tại `docs/reference/templates/` (mỗi tệp có sheet `DuLieu` tiêu đề in đậm + dòng mẫu và sheet `HuongDan` mô tả cột).

### 5.1.5. Hợp đồng `ImportPreviewDialog`

- Đầu vào: tiêu đề, `ImportResult`, cột xem trước, số dòng tối đa.
- Hiển thị tổng/hợp lệ/lỗi; bảng dữ liệu; bảng lỗi (dòng, cột, mã, thông báo).
- Nút: Chọn tệp khác, Xuất lỗi, Hủy, Nhập N dòng.
- Chỉ bật Nhập khi `hopLe()` và có ít nhất 1 dòng.
- Đọc/ghi trong `SwingWorker`; không Hủy khi đã bắt đầu ghi CSDL.
- Lỗi ghi CSDL: giữ dialog, không mất dữ liệu xem trước.

### 5.1.6. UC003 — ảnh hưởng của bảng chi tiết

- `BanHangController` vẫn gọi `findHieuLuc(maDongSP, ngày)`; không đổi controller.
- `KhuyenMaiDaoImpl.findHieuLuc` đổi truy vấn để join `ChiTietKhuyenMai`.
- Hóa đơn vẫn lưu `maKM` (đầu chương trình), `tyLeGiamTaiLucBan`, tiền tại thời điểm bán.
- Không tính lại hóa đơn cũ khi chi tiết/chương trình thay đổi.
- Test hồi quy: một chương trình nhiều chi tiết, bán một dòng thuộc chi tiết → áp đúng; dòng không thuộc → không áp.

### 5.1.7. Điều kiện hoàn thành của Đăng

- Migration V15 chạy sạch; backfill đủ; `KhuyenMai.maDongSP` **vẫn còn** sau V15 (V16 mới bỏ, sau khi Tài merge).
- Importer/template/preview dùng được cho 3 trang.
- Nhân viên bán hàng vào trang nhập nhưng không thấy nút tạo/bật/dừng.
- UC003 không hồi quy; `mvn test` xanh.

## 5.2. Tài — UC012, schema chi tiết, DAO, UI, nhập Excel

### 5.2.1. Hạng mục

| Mã | Hạng mục | File dự kiến | Kết quả |
|---|---|---|---|
| T6-01 | Entity chi tiết | `entity/ChiTietKhuyenMai.java`, `entity/KhuyenMai.java` | `KhuyenMai` có `List<ChiTietKhuyenMai>`; bỏ `maDongSP` khỏi đầu chương trình |
| T6-02 | DAO | `dao/KhuyenMaiDao.java`, `dao/impl/KhuyenMaiDaoImpl.java` | `findHieuLuc` join chi tiết; `findByDongSanPham`; `insert` header + details; `existsChongLan` |
| T6-03 | Controller | `controller/KhuyenMaiController.java` | Query nhiều danh mục/ngày; tạo 1 header + N details; validate; quyền |
| T6-04 | UI tạo | `ui/page/promotion/AddPromotionDialog.java` | Multi-select danh mục; ngày optional; dòng cụ thể; xem trước |
| T6-05 | Trang quản lý | `ui/page/promotion/PromotionsPage.java` | Một dòng/chương trình; cột số dòng; nhập Excel; ẩn hành động Quản lý |
| T6-06 | Nhập Excel | `KhuyenMaiController`/service | Gộp `Mã nhóm`; 1 header + N details; all-or-nothing |
| T6-07 | Test/tài liệu | Test + UC012 | Materialize, FK, xung đột, rollback, quyền |

### 5.2.2. Entity

**`ChiTietKhuyenMai`:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `maKM` | `String` | FK tới `KhuyenMai` |
| `dongSanPham` | `DongSanPham` | Dòng sản phẩm áp dụng |

**`KhuyenMai`:**

- Bỏ `DongSanPham dongSanPham`.
- Thêm `List<ChiTietKhuyenMai> chiTiet`.
- Giữ constructor cũ `(..., DongSanPham)` để các mapper hóa đơn/trả/đổi hiện có không gãy; constructor này tạo một `ChiTietKhuyenMai` duy nhất.
- Thêm constructor đầy đủ với `List<ChiTietKhuyenMai>`.
- `equals/hashCode/toString` cập nhật.
- `PromotionsPage` dùng `chiTiet` để hiển thị số dòng; không gọi `dongSanPham()` như cũ.

### 5.2.3. DAO

**`findHieuLuc(maDongSP, ngày)`:**

```sql
SELECT km.*
FROM "KhuyenMai" km
JOIN "ChiTietKhuyenMai" ct ON ct."maKM" = km."maKM"
WHERE ct."maDongSP" = ?
  AND km."dangApDung" = true
  AND ? BETWEEN km."ngayBatDau" AND km."ngayKetThuc"
ORDER BY km."tyLeGiam" DESC, km."ngayBatDau" DESC, km."maKM" ASC
LIMIT 1;
```

**`findByDongSanPham(maDongSP)`:** join chi tiết, trả các chương trình có dòng đó.

**`findById`/`findAll`:** nạp kèm danh sách `ChiTietKhuyenMai` (query phụ theo `maKM` hoặc join) để UI hiển thị số dòng; không dùng N+1 quá mức, có thể batch theo danh sách `maKM`.

**`insert(KhuyenMai)`:**

- Insert header `KhuyenMai`.
- Insert từng `ChiTietKhuyenMai`.
- Bên gọi sở hữu transaction; thêm `insert(Connection, KhuyenMai)` để controller dùng chung transaction.
- Nếu chi tiết rỗng → lỗi.
- Nếu một chi tiết vi phạm FK → rollback toàn bộ (Q7).

**`update`:** cập nhật header; nếu có chi tiết mới, xóa/ghi lại chi tiết trong cùng transaction. UI Wave 6 chưa mở sửa; vẫn xử lý đúng nếu được gọi.

**`existsChongLan(maDongSP, start, end, exclude)`:**

```sql
SELECT EXISTS (
  SELECT 1
  FROM "KhuyenMai" km
  JOIN "ChiTietKhuyenMai" ct ON ct."maKM" = km."maKM"
  WHERE ct."maDongSP" = ?
    AND km."dangApDung" = true
    AND NOT (km."ngayKetThuc" < ? OR km."ngayBatDau" > ?)
    AND (? IS NULL OR km."maKM" <> ?)
);
```

**`findDongTheoDieuKien(maDongSP, List<String> maDanhMucs, Integer soNgay, LocalDate ngay)`:**

- Nếu `maDongSP` có giá trị: chỉ xét dòng đó.
- Nếu không: lọc `d.maDanhMuc IN (...)` (nhiều danh mục) và/hoặc ngày。
- Chỉ dòng đang kinh doanh.
- Ngày tồn tính từ `SanPham.ngayNhapKhoBanDau`.
- Trả `List<DongSanPham>`.

### 5.2.4. Controller — tạo một chương trình, nhiều chi tiết

`themMoiTheoDieuKien(tenKM, moTa, tyLeGiam, ngayBatDau, ngayKetThuc, maDongSP, List<String> maDanhMucs, Integer soNgayTonToiThieu, boolean dangApDung)`.

| Trường | Quy tắc | Thông báo |
|---|---|---|
| `tenKM` | Không rỗng, <= 200 | “Tên chương trình khuyến mãi không được để trống” |
| `tyLeGiam` | 0–100, tối đa 2 chữ số thập phân | “Tỷ lệ giảm giá phải nằm trong khoảng 0% đến 100%” |
| Ngày | Bắt đầu <= kết thúc | “Ngày bắt đầu không được sau ngày kết thúc” |
| Mục tiêu | Có dòng cụ thể **hoặc** ít nhất một danh mục **hoặc** ngày | “Chọn ít nhất một đối tượng hoặc điều kiện” |
| Tổ hợp | Dòng + danh mục không hợp lệ; dòng + ngày hợp lệ; nhiều danh mục + ngày hợp lệ | “Tổ hợp điều kiện không hợp lệ” |
| Danh mục | Tất cả phải tồn tại | “Danh mục áp dụng không tồn tại” |
| Dòng | Nếu có phải tồn tại | “Dòng sản phẩm áp dụng không tồn tại” |
| Kết quả query | Ít nhất 1 dòng | “Không có dòng sản phẩm nào thỏa điều kiện” |
| Xung đột | Kiểm tra từng dòng; một dòng xung đột → chặn cả chương trình | “Có dòng đã có chương trình khác trong cùng thời gian” |
| Quyền tạo/sửa/bật | Chỉ Quản lý | “Chỉ Quản lý được quản lý chương trình khuyến mãi” |
| Quyền nhập Excel | Cả 2 actor; được bật ngay theo tệp (Q9) | “Đã nhập N chương trình” |

Luồng lưu:

1. Validate.
2. Query `findDongTheoDieuKien(...)`.
3. Nếu rỗng → lỗi, không tạo.
4. Kiểm tra `existsChongLan` từng dòng; nếu có xung đột → dừng.
5. Mở transaction; sinh `maKM`; tạo header + danh sách chi tiết; `khuyenMaiDao.insert(conn, km)`.
6. Commit; nếu lỗi → rollback toàn bộ (kể cả FK).
7. Trả `KetQua<KhuyenMai>` kèm số chi tiết.

**Một dòng cụ thể:** tạo header + 1 chi tiết.

**Nhiều danh mục:** tạo header + N chi tiết (mỗi dòng sản phẩm một chi tiết).

**Bật/dừng áp dụng:** `batDauApDung(maKM)` phải kiểm tra xung đột cho **tất cả chi tiết** trước khi bật; nếu một dòng xung đột thì không bật. `dungApDung` chỉ đổi cờ header.

**Import:** gộp theo `Mã nhóm`; mỗi nhóm tạo một header + N chi tiết; toàn bộ tệp một transaction; FK violation → rollback cả tệp.

### 5.2.5. UI

`AddPromotionDialog`:

- `PromotionFormData` gồm `maDongSP` (nullable), `List<String> maDanhMuc`, `Integer soNgayTonToiThieu`.
- Chọn dòng cụ thể (có thể bỏ trống).
- Chọn **nhiều danh mục** bằng danh sách checkbox/multi-select; nếu chọn dòng thì khóa danh mục.
- Nhập `Tồn từ N ngày` (optional).
- Xem trước: “N dòng sản phẩm sẽ được thêm vào chương trình”; bảng mã dòng/tên/danh mục/tồn/số máy đủ điều kiện.
- 0 dòng → lỗi, không lưu.
- Lưu: xác nhận “Tạo 1 chương trình với N dòng sản phẩm”.

`PromotionsPage`:

- Một dòng = một chương trình (header).
- Cột: mã, tên, tỷ lệ, ngày, trạng thái, **số dòng áp dụng**.
- Có thể mở dialog chi tiết để xem danh sách dòng (tùy chọn).
- Nút Tải mẫu/Nhập Excel cho cả 2 actor; nút Thêm/Bật/Dừng chỉ Quản lý.
- Bộ lọc theo dòng/category có thể dựa trên chi tiết; nếu phức tạp, lọc theo tên/trạng thái.

### 5.2.6. Nhập Excel khuyến mãi

Template gồm cột `Mã nhóm` để gộp nhiều dòng thành một chương trình.

| Cột | Quy tắc |
|---|---|
| `Mã nhóm` | Bắt buộc; chỉ dùng để gộp trong tệp; không lưu DB |
| `Tên chương trình` | Bắt buộc; các dòng cùng nhóm phải giống nhau |
| `Mô tả` | Tùy chọn; cùng nhóm phải giống nhau |
| `Tỷ lệ giảm (%)` | Bắt buộc; cùng nhóm phải giống nhau |
| `Ngày bắt đầu`, `Ngày kết thúc` | Bắt buộc; cùng nhóm phải giống nhau |
| `Đang áp dụng` | Bắt buộc; cùng nhóm phải giống nhau |
| `Mã dòng sản phẩm` | Bắt buộc; mỗi dòng là một chi tiết; phải tồn tại |

- Mỗi nhóm phải có ít nhất 1 dòng; một `maDongSP` không được lặp trong cùng nhóm (trùng PK chi tiết).
- Nếu một mã dòng không tồn tại (FK) → chặn cả tệp (Q7).
- Nếu một dòng xung đột → chặn cả tệp.
- Toàn bộ tệp một transaction; lỗi bất kỳ → rollback (D22).
- Nhân viên bán hàng được nhập và bật ngay theo tệp (Q9).

### 5.2.7. Điều kiện hoàn thành của Tài

- Schema chi tiết chạy; dữ liệu cũ được backfill.
- Tạo được chương trình một dòng, nhiều danh mục, nhiều dòng.
- Xem trước đúng số dòng; 0 dòng bị chặn.
- Một header + N details; FK violation rollback.
- `findHieuLuc` áp đúng cho UC003; hóa đơn cũ không đổi.
- Nhập Excel gộp nhóm đúng; all-or-nothing; cả 2 actor.
- Test xanh; báo cáo đầy đủ.

## 5.3. Quý — nhập Excel sản phẩm và dòng sản phẩm

### 5.3.1. Hạng mục

| Mã | Hạng mục | File | Kết quả |
|---|---|---|---|
| Q6-01 | Nhập hàng loạt sản phẩm | `controller/SanPhamController.java` | `nhapDanhSach`; một transaction; tính lại tồn |
| Q6-02 | UI nhập sản phẩm | `ui/page/product/ProductPage.java` | Tải mẫu/Nhập Excel/xem trước |
| Q6-03 | Nhập hàng loạt dòng sản phẩm | `controller/DongSanPhamController.java` | `nhapDanhSach`; sinh mã; một transaction |
| Q6-04 | UI nhập dòng sản phẩm | `ui/page/product/ProductLinesPage.java` | Tải mẫu/Nhập Excel/xem trước |
| Q6-05 | Test | Controller + DB | Trùng/FK/ngày/trạng thái/rollback/RB37 |

### 5.3.2. Nhập sản phẩm

- Cột: `Số serial`, `Mã nhà cung cấp`, `Mã dòng sản phẩm`, `Giá nhập`, `Ngày nhập kho ban đầu`, `Trạng thái`.
- Chỉ `SAN_SANG_BAN` (D26).
- Serial duy nhất trong tệp và CSDL; NCC/dòng phải tồn tại (Q7); ngày không tương lai; giá >= 0.
- Một transaction; insert qua `SanPhamDao`; tính lại `soLuongTon`; rollback toàn bộ nếu lỗi.
- Không có cột tồn; không nhập `soLanDoiTra`/`soLanSua`.

### 5.3.3. Nhập dòng sản phẩm

- Cột: `Tên dòng sản phẩm`, `Mã danh mục`, `Đơn vị tính`, `Giá bán trước thuế`, `Thuế suất (%)`, `Đang kinh doanh`.
- Danh mục phải tồn tại (Q7); ĐVT `Chiếc`/`Cái`; giá >= 0; thuế 0–100.
- `maDongSP` do `MaGenerator` sinh; `soLuongTon = 0`; ngày hiện tại.
- Trùng tên chuẩn hóa trong tệp: chặn; trùng với dữ liệu cũ: cảnh báo (Q7 không áp dụng cho tên, chỉ FK).
- Một transaction; rollback toàn bộ.

### 5.3.4. UI

- Nút Tải mẫu/Nhập Excel cho cả 2 actor.
- Đọc/ghi ngoài EDT; dùng `ImportPreviewDialog`.
- Lỗi hiển thị dòng/cột; không mất dữ liệu xem trước.

## 5.4. Tuấn Anh

Không có hạng mục bắt buộc. Nếu rảnh: review hồi quy UC003, bảo mật tệp, thông báo lỗi.

# 6. Hợp đồng liên thành viên

## 6.1. Bảng hợp đồng

| Hạng mục | Chủ sở hữu | API/kiểu dữ liệu | Người dùng | Chốt khi |
|---|---|---|---|---|
| Migration V15 | Đăng | Bảng `ChiTietKhuyenMai` (additive); giữ `KhuyenMai.maDongSP`; V16 bỏ cột sau khi Tài merge | Tài, Đăng | W6.0 |
| `ExcelImporter` | Đăng | `doc(File, ImportSpec)`, `ImportResult` | Quý, Tài | W6.0 |
| `ExcelTemplate` | Đăng | `xuat(File, ImportSpec, List<Object[]>)` | Quý, Tài | W6.0 |
| `ImportPreviewDialog` | Đăng | Hộp thoại dùng chung | Quý, Tài | W6.1 |
| `ChiTietKhuyenMai`, `KhuyenMai` | Tài | Entity header + details | Đăng, Tài | W6.0 |
| `KhuyenMaiDao` | Tài | `findHieuLuc`, `findByDongSanPham`, `insert(Connection, KhuyenMai)`, `existsChongLan`, `findDongTheoDieuKien` | Đăng, Tài | W6.0 |
| `KhuyenMaiController` | Tài | `themMoiTheoDieuKien`, `xemTruoc`, `nhapDanhSach` | Page của Tài | W6.1 |
| `NavigationModel` | Đăng | Thêm `PROMOTIONS` cho nhân viên bán hàng | Tài, Đăng | W6.1 |
| `SanPhamController.nhapDanhSach` | Quý | `KetQua<NhapKetQua>` | Page của Quý | W6.1 |
| `DongSanPhamController.nhapDanhSach` | Quý | `KetQua<NhapKetQua>` | Page của Quý | W6.1 |
| `ExcelExporter` | Đăng | Giữ nguyên; xuất báo cáo lỗi | Tài, Quý | Đã có |

## 6.2. Quy tắc

1. Chốt API ở W6.0; không đổi chữ ký sau W6.1 nếu chưa thống nhất.
2. Phương thức mới trên DAO nên có `default` nếu có thể, để fake cũ không gãy.
3. Constructor cũ của `KhuyenMai` giữ nguyên cho mapper hóa đơn; thêm constructor mới cho details.
4. Page không gọi trực tiếp `ExcelImporter` để ghi CSDL; chỉ đọc tệp, map DTO, gọi controller.
5. Controller không import POI; chỉ `util` dùng POI.
6. Không sửa `AppNavigator` nếu không phải Đăng.
7. Không sửa migration đã chạy; thay đổi schema mới phải V15+.

## 6.3. Mã lỗi nhập liệu

Mã lỗi được chia làm hai nhóm: mã do `ExcelImporter` sinh khi đọc/kiểm tra tệp, và mã nghiệp vụ do controller/import service sinh khi đối chiếu CSDL hoặc ghi dữ liệu.

### 6.3.1. Mã do `ExcelImporter` trả về

| Mã | Ý nghĩa | Ví dụ |
|---|---|---|
| `TEP_KHONG_TON_TAI` | Không tìm thấy tệp | “Tệp không tồn tại: maux.xlsx” |
| `TEP_KHONG_HOP_LE` | Không phải `.xlsx` | “Chỉ hỗ trợ tệp Excel định dạng .xlsx” |
| `TEP_RONG` | Tệp rỗng (0 byte) | “Tệp Excel rỗng: maux.xlsx” |
| `TEP_QUA_LON` | > 10 MB | “Tệp vượt quá dung lượng cho phép 10 MB” |
| `LOI_DOC_TEP` | Lỗi I/O hoặc tệp hỏng | “Không thể đọc tệp Excel: maux.xlsx” |
| `SHEET_KHONG_TON_TAI` | Thiếu sheet dữ liệu | “Không tìm thấy sheet: DuLieu” |
| `THIEU_TIEU_DE` | Không có dòng tiêu đề | “Tệp không có dòng tiêu đề” |
| `QUA_NHIEU_COT` | > 50 cột | “Tệp có quá 50 cột” |
| `THIEU_COT` | Thiếu cột bắt buộc | “Thiếu cột bắt buộc: Mã dòng sản phẩm” |
| `QUA_NHIEU_DONG` | > 5.000 dòng (hoặc giới hạn `soDongToiDa`) | “Tệp vượt quá 5000 dòng dữ liệu cho phép” |
| `THIEU_GIA_TRI` | Ô bắt buộc trống | “Giá trị bắt buộc không được để trống” |
| `SAI_KIEU` | Sai kiểu số/ngày/boolean | “Dòng 7, cột Giá nhập: Giá trị không phải số thập phân: abc” |
| `SAI_GIA_TRI` | Giá trị ngoài danh sách cho phép (enum) | “Dòng 2: Giá trị không nằm trong danh sách cho phép: XYZ” |

- `SAI_KIEU` bao phủ cả số nguyên/số thập phân, ngày (`dd/MM/yyyy` hoặc `yyyy-MM-dd`) và boolean (`Có/Không/TRUE/FALSE/1/0`).
- `SAI_GIA_TRI` chỉ dùng cho cột `ENUM` có `giaTriChoPhep`.
- Importer hiện **không** sinh các mã `FILE_*`, `SAI_NGAY`, `SAI_SO`, `SAI_ENUM`, `LOI_CONG_THUC`; các mã đó chỉ từng là đề xuất và không còn hiệu lực.

### 6.3.2. Mã nghiệp vụ do controller/import service trả về

Các mã dưới đây do phần nghiệp vụ (controller/import service của Tài và Quý) sinh sau khi `ExcelImporter` đã trả tệp hợp lệ.

| Mã | Ý nghĩa | Ví dụ |
|---|---|---|
| `TRUNG_TRONG_FILE` | Trùng khóa trong tệp | “Dòng 9: serial trùng dòng 4” |
| `DA_TON_TAI` | Đã có trong CSDL | “Dòng 6: serial đã tồn tại” |
| `KHONG_TIM_THAY` | FK không tồn tại | “Dòng 8: mã dòng sản phẩm không tồn tại” |
| `XUNG_DOT_THOI_GIAN` | Chồng lấn thời gian khuyến mãi | “Dòng 10: trùng thời gian với KM-…” |
| `LOI_GHI_CSDL` | Lỗi ghi/transaction | “Không thể ghi dữ liệu, đã hoàn tác” |

# 7. Định dạng Excel chuẩn

## 7.1. Quy ước chung

| Hạng mục | Quy ước |
|---|---|
| Định dạng | `.xlsx`; không `.xls`, `.xlsm`, CSV |
| Sheet dữ liệu | `DuLieu` |
| Dòng tiêu đề | Dòng 1; không merge |
| Dữ liệu | Từ dòng 2; bỏ dòng trống |
| Ngày | Ô ngày hoặc `dd/MM/yyyy`; chấp nhận `yyyy-MM-dd` nếu rõ |
| Số | Ô số; thập phân dùng `.`; không phân cách nghìn trong ô text |
| Boolean | `Có/Không`, `TRUE/FALSE`, `1/0` |
| Enum | Nhãn tiếng Việt hoặc tên enum |
| Công thức | Chỉ đọc cache; không chạy macro |
| Giới hạn | 5.000 dòng; 10 MB; 50 cột |
| Mã định danh | Không có trong template; app sinh khi lưu |

## 7.2. Template khuyến mãi

| # | Tiêu đề | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Mã nhóm | Chuỗi | Có | Chỉ dùng để gộp; không lưu DB |
| 2 | Tên chương trình | Chuỗi | Có | Các dòng cùng nhóm phải giống nhau |
| 3 | Mô tả | Chuỗi | Không | Cùng nhóm phải giống nhau |
| 4 | Tỷ lệ giảm (%) | Số thập phân | Có | 0–100; cùng nhóm phải giống nhau |
| 5 | Ngày bắt đầu | Ngày | Có | `dd/MM/yyyy` |
| 6 | Ngày kết thúc | Ngày | Có | >= bắt đầu |
| 7 | Đang áp dụng | Boolean | Có | Có/Không |
| 8 | Mã dòng sản phẩm | Chuỗi | Có | Mỗi dòng là một chi tiết; phải tồn tại |

**Quy tắc:**

- Mỗi `Mã nhóm` = một chương trình; nhóm có thể có nhiều dòng sản phẩm.
- Các dòng cùng nhóm phải cùng tên/mô tả/tỷ lệ/ngày/trạng thái; nếu khác → lỗi.
- Ít nhất 1 dòng mỗi nhóm; một `maDongSP` không được lặp trong cùng nhóm.
- FK sai → chặn cả tệp (Q7).
- Trùng/xung đột → chặn cả tệp (D21/D22).
- Toàn bộ tệp một transaction; lỗi bất kỳ → rollback.
- Nhân viên bán hàng được nhập và bật ngay (Q9).

**Ví dụ:**

| Mã nhóm | Tên chương trình | Tỷ lệ | Bắt đầu | Kết thúc | Áp dụng | Mã dòng sản phẩm |
|---|---|---|---|---|---|---|
| KM01 | Ưu đãi đồ gia dụng | 10 | 01/12/2026 | 31/12/2026 | Có | DSP-2026-000003 |
| KM01 | Ưu đãi đồ gia dụng | 10 | 01/12/2026 | 31/12/2026 | Có | DSP-2026-000005 |
| KM02 | Giảm giá máy lọc nước | 5 | 01/12/2026 | 15/12/2026 | Có | DSP-2026-000007 |

## 7.3. Template sản phẩm

| # | Tiêu đề | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Số serial | Chuỗi | Có | Duy nhất trong tệp/CSDL |
| 2 | Mã nhà cung cấp | Chuỗi | Có | Phải tồn tại |
| 3 | Mã dòng sản phẩm | Chuỗi | Có | Phải tồn tại/đang kinh doanh |
| 4 | Giá nhập | Số thập phân | Có | >= 0 |
| 5 | Ngày nhập kho ban đầu | Ngày | Có | Không tương lai |
| 6 | Trạng thái | Enum | Không | Mặc định `Sẵn sàng bán`; chỉ nhận `SAN_SANG_BAN` |

**Ví dụ:**

| Số serial | Mã NCC | Mã dòng | Giá nhập | Ngày nhập | Trạng thái |
|---|---|---|---|---|---|
| SP-TEST-0001 | NCC-2026-000001 | DSP-2026-000003 | 4500000 | 01/11/2026 | Sẵn sàng bán |

## 7.4. Template dòng sản phẩm

| # | Tiêu đề | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Tên dòng sản phẩm | Chuỗi | Có | 1–200; không trùng chuẩn hóa trong tệp |
| 2 | Mã danh mục | Chuỗi | Có | Phải tồn tại |
| 3 | Đơn vị tính | Enum | Có | `Chiếc`/`Cái` |
| 4 | Giá bán trước thuế | Số thập phân | Có | >= 0 |
| 5 | Thuế suất (%) | Số thập phân | Có | 0–100 |
| 6 | Đang kinh doanh | Boolean | Không | Mặc định Có |

**Ví dụ:**

| Tên dòng | Mã danh mục | ĐVT | Giá trước thuế | Thuế | Kinh doanh |
|---|---|---|---|---|---|
| Máy lọc nước RO | DM-2026-000001 | Chiếc | 4500000 | 10 | Có |

## 7.5. Quy trình nhập

1. Tải mẫu → mở kiểm tra.
2. Điền dữ liệu, giữ sheet/tiêu đề.
3. Nhập Excel → chọn tệp.
4. Hệ thống đọc, validate, mở `ImportPreviewDialog`.
5. Có lỗi → xem/xuất lỗi, sửa tệp.
6. Hợp lệ → Nhập N dòng → một transaction → toast → tải lại.
7. Lỗi ghi CSDL → rollback, giữ dialog.

# 8. Thiết kế khuyến mãi nhiều dòng sản phẩm

## 8.1. Schema trước và sau

| Khía cạnh | Hiện tại | Wave 6 |
|---|---|---|
| `KhuyenMai` | Có `maDongSP` | Bỏ `maDongSP` (ở **V16**, sau khi mã Tài merge); chỉ còn thông tin đầu chương trình |
| `ChiTietKhuyenMai` | Không có | `maKM`, `maDongSP`; PK (`maKM`,`maDongSP`) |
| Quan hệ | 1 KM → 1 dòng | 1 KM → N chi tiết; 1 chi tiết → 1 KM, 1 dòng |
| Resolver | `findHieuLuc` theo `KhuyenMai.maDongSP` | Join `ChiTietKhuyenMai` |
| Snapshot | `ChiTietHoaDon.maKM` | Không đổi |

## 8.2. Ngữ nghĩa điều kiện

- Một chương trình có thể áp cho **nhiều danh mục** và nhiều dòng.
- Điều kiện danh mục và ngày là optional; phải có ít nhất một mục tiêu.
- Dòng cụ thể + ngày: hợp lệ.
- Dòng cụ thể + danh mục: không hợp lệ.
- Nhiều danh mục + ngày: hợp lệ.
- Tuổi tồn `>= N`, tính từ `SanPham.ngayNhapKhoBanDau` của máy `SAN_SANG_BAN`.
- Ngày xét truyền từ UI/UC003; không dùng `CURRENT_DATE`.
- Dòng ngừng kinh doanh bị loại.

## 8.3. Luồng tạo

1. Quản lý mở Thêm khuyến mãi.
2. Chọn dòng cụ thể **hoặc** nhiều danh mục.
3. Nhập ngày tồn (optional).
4. Xem trước: N dòng sẽ được thêm.
5. Nhập tên/tỷ lệ/thời gian/trạng thái.
6. Lưu: validate, query, kiểm tra xung đột từng dòng, tạo 1 header + N details trong một transaction.
7. 0 dòng → lỗi, không lưu.
8. Một dòng xung đột → rollback cả chương trình.

## 8.4. Ví dụ materialize

Chọn hai danh mục **Đồ gia dụng**, **Thiết bị văn phòng** + ngày tồn **30**:

1. Query ra các dòng thuộc hai danh mục, có ít nhất một máy `SAN_SANG_BAN` đủ 30 ngày.
2. Tạo 1 `KhuyenMai` header (một `maKM`).
3. Tạo N `ChiTietKhuyenMai`, mỗi chi tiết gắn một `maDongSP`.
4. Kết quả: 1 chương trình, N dòng sản phẩm.

Ví dụ dữ liệu:

| maKM | tenKM | maDongSP (chi tiết) |
|---|---|---|
| KM-2026-000101 | Ưu đãi hàng tồn | DSP-2026-000003 |
| KM-2026-000101 | Ưu đãi hàng tồn | DSP-2026-000005 |
| KM-2026-000101 | Ưu đãi hàng tồn | DSP-2026-000008 |

## 8.5. Áp dụng khi bán (UC003)

- `BanHangController` gọi `findHieuLuc(maDongSP, ngày)`.
- DAO join `ChiTietKhuyenMai`; trả header tốt nhất.
- Hóa đơn lưu `maKM` header, tỷ lệ và tiền snapshot.
- Không tính lại hóa đơn cũ.
- Không cần đổi `BanHangController`, chỉ đổi DAO.

## 8.6. Xung đột

| Trường hợp | Kết quả |
|---|---|
| Hai chương trình cùng một dòng, ngày giao nhau, cùng active | Chặn |
| Một dòng trong chương trình mới xung đột | Chặn cả chương trình |
| Chương trình inactive | Không chặn |
| Ngày kết thúc cũ = ngày bắt đầu mới - 1 | Không chặn |
| Ngày kết thúc cũ = ngày bắt đầu mới | Chặn |

## 8.7. Migration/rollback

- V15 tạo bảng chi tiết và backfill từ `KhuyenMai.maDongSP`; **giữ nguyên** cột `maDongSP` và FK cũ (additive).
- V16 (follow-up, chỉ sau khi mã Tài merge) mới bỏ `maDongSP`, FK `fk_khuyenmai_dongsanpham` và index cũ.
- Backup trước; rollback bằng backup.
- Dữ liệu cũ: mỗi khuyến mãi thành header + 1 detail.
- `ChiTietHoaDon.maKM` vẫn hợp lệ.

## 8.8. Báo cáo

- UC015/UC016/UC017 đọc snapshot; không cần join chi tiết.
- Muốn thống kê hiệu quả, join `ChiTietKhuyenMai` theo `maKM` để biết dòng áp dụng.

# 9. Lộ trình Wave 6

Giả định W4 kết thúc 14/11; nếu trượt, dời tương ứng.

| Chặng | Thời gian | Đăng | Tài | Quý | Cổng |
|---|---|---|---|---|---|
| W6.0 | 15/11 | Chốt API; migration; khung importer/template | Entity/DAO contract; khung chi tiết | DTO nhập; validate | G1: hợp đồng đóng băng |
| W6.1 | 16–17/11 | Migration chạy thử; importer/template/preview | Entity/DAO/controller; test DAO | Tách validate; nhập hàng loạt | G2: migration + đọc mẫu |
| W6.2 | 18–19/11 | NavigationModel/AppNavigator | UI nhiều danh mục; import khuyến mãi | UI nhập sản phẩm/dòng | G3: nhập mẫu + tạo KM |
| W6.3 | 20–21/11 | Test hồi quy UC003 | Test chi tiết/xung đột/import | Test RB37/rollback | G4: test xanh |
| W6.4 | 22/11 | `mvn test`; báo cáo | Sửa lỗi; UC012 | Sửa lỗi; báo cáo | G5: nghiệm thu |

Thứ tự phụ thuộc:

1. V15 + entity → DAO → controller → UI.
2. Importer/template → UI nhập.
3. PreviewDialog → cả ba trang.
4. UC003 test sau khi DAO chi tiết ổn định.

# 10. Kiểm thử và Definition of Done

## 10.1. Ma trận kiểm thử

| Khu vực | Kiểm thử | Loại | Người | Kỳ vọng |
|---|---|---|---|---|
| Migration | V15 trên CSDL sạch/có dữ liệu cũ | DB | Đăng | Backfill đủ; cột `maDongSP` vẫn còn (V16 mới bỏ) |
| Schema | 1 KM + N chi tiết; FK | DB | Đăng | Đúng quan hệ |
| Importer | Thiếu cột/sai kiểu/ngày/boolean/enum | Unit | Đăng | Lỗi dòng/cột |
| Importer | Tệp rỗng/quá dòng/quá MB/`.xlsm` | Unit | Đăng | Lỗi cấp tệp |
| Template | Sinh 3 mẫu; mở lại | Unit | Đăng | Hợp lệ |
| Roundtrip | Tải mẫu → điền → nhập | Integration | Cả ba | Ghi đúng |
| Khuyến mãi nhập | Gộp `Mã nhóm`; header/detail | Controller | Tài | 1 header + N detail |
| Khuyến mãi nhập | FK sai/xung đột/một dòng lỗi | Controller | Tài | Rollback cả tệp |
| Khuyến mãi nhập | Nhân viên bán hàng nhập, bật ngay | Controller | Tài | Đúng Q9 |
| Query điều kiện | Nhiều danh mục + ngày; N-1/N/N+1 | DAO | Tài | Đúng dòng |
| Materialize | 3 dòng → 1 header + 3 detail | Controller+DB | Tài | Đúng |
| Materialize | 0 dòng | Controller | Tài | Lỗi |
| Overlap | Một dòng xung đột | DAO | Tài | Rollback cả chương trình |
| UC003 | Một chương trình nhiều chi tiết | Controller+DB | Đăng | Áp đúng; snapshot |
| UI | Trang quản lý; quyền 2 actor | Unit/UI | Tài | Đúng quyền |
| Sản phẩm nhập | Serial/FK/ngày/trạng thái | Controller | Quý | Lỗi; rollback |
| Sản phẩm nhập | `soLuongTon` = số máy sẵn bán | DB | Quý | RB37 |
| Dòng sản phẩm nhập | Danh mục/ĐVT/giá/thuế | Controller | Quý | Lỗi; rollback |
| Hồi quy | `mvn test` | Build | Đăng | Xanh |

## 10.2. Cách chạy

```bash
cp db.properties.example db.properties
mvn -q clean test
mvn -q clean compile exec:java -Dexec.mainClass=com.dtaq.retail.Main
```

- Test DB dùng `Assumptions.assumeTrue(ConnectDB.isAvailable())`.
- Không dùng dữ liệu thật; dùng seed V4 hoặc dữ liệu test.
- Test nhập tạo workbook bằng POI trong `util` test.

## 10.3. Definition of Done

1. Đúng phạm vi; không thêm tính năng ngoài yêu cầu.
2. Đúng kiến trúc; không SQL trong UI; POI chỉ trong `util`.
3. Migration V15 chạy sạch; dữ liệu cũ backfill đủ.
4. 1 chương trình + N chi tiết; FK đúng; không chi tiết mồ côi.
5. Tạo chương trình + chi tiết trong một transaction; rollback có test.
6. RB37 đúng sau nhập sản phẩm.
7. Mã qua `MaGenerator`; không nhận mã từ Excel.
8. Tiền `BigDecimal`; không tính lại hóa đơn cũ.
9. Quyền tạo/sửa/bật thủ công chỉ Quản lý; nhập Excel cho cả 2 actor, được bật ngay (Q9).
10. Lỗi tiếng Việt; không lộ stack trace/SQL/credential.
11. Test xanh; báo cáo đầy đủ.

# 11. Rủi ro và điểm chặn

| Rủi ro | Ảnh hưởng | Giảm thiểu | Chủ |
|---|---|---|---|
| Migration V15 sai/trùng version | App không khởi động | Kiểm tra V14; backup; không sửa V1–V14 | Đăng |
| Backfill thiếu | Mất liên kết KM-dòng | Test đếm trước/sau; rollback nếu lệch | Đăng |
| FK chi tiết sai | Dữ liệu mồ côi | FK DB + validate; rollback | Tài |
| Nhiều danh mục query sai | Thiếu/thừa dòng | Test N-1/N/N+1; preview = lưu | Tài |
| Một dòng xung đột | Tạo một phần | Kiểm tra trước; rollback cả chương trình | Tài |
| 0 dòng | Không có chi tiết | Chặn lưu | Tài |
| Nhập 5.000 dòng | Chậm/OOM | Giới hạn; ngoài EDT; tiến trình | Đăng/Quý/Tài |
| Công thức/macro | Bảo mật | Chỉ `.xlsx`; đọc cache; từ chối `.xlsm` | Đăng |
| C-08 | Nhân viên bán hàng tạo dòng có giá | Q5: cả 2 actor; chỉ tạo mới | Chủ dự án |
| Q9 | Nhân viên bán hàng bật ngay | Đã chấp thuận ngoại lệ UC012; ghi rõ | Chủ dự án |
| Xung đột kiểm tra không khóa | Hai người tạo cùng lúc | Ghi nhận; có thể khóa transaction sau | Tài |
| Hai phiên sinh mã | Trùng PK | `MaGenerator` MAX; PK chặn; retry tương lai | Đăng |

# 12. Danh sách file dự kiến

## 12.1. Tạo mới

| File | Người | Mục đích |
|---|---|---|
| `src/main/resources/db/migration/V15__chi_tiet_khuyen_mai.sql` | Đăng | Bảng chi tiết + backfill (additive, **không** bỏ cột) |
| `src/main/resources/db/migration/V16__bo_khuyenmai_madongsp.sql` | Tài (follow-up) | Bỏ `KhuyenMai.maDongSP`, FK + index cũ — **chỉ sau khi mã Tài merge** |
| `src/main/java/com/dtaq/retail/util/ExcelImporter.java` | Đăng | Đọc/validate Excel |
| `src/main/java/com/dtaq/retail/util/ExcelTemplate.java` | Đăng | Sinh template |
| `src/main/java/com/dtaq/retail/ui/component/ImportPreviewDialog.java` | Đăng | Xem trước/lỗi |
| `src/main/java/com/dtaq/retail/entity/ChiTietKhuyenMai.java` | Tài | Entity chi tiết |
| `docs/reference/templates/mau_nhap_khuyen_mai.xlsx` | Đăng | Template tĩnh (commit — Q8) |
| `docs/reference/templates/mau_nhap_san_pham.xlsx` | Đăng | Template tĩnh (commit — Q8) |
| `docs/reference/templates/mau_nhap_dong_san_pham.xlsx` | Đăng | Template tĩnh (commit — Q8) |
| Test: importer/template/materialize | Cả ba | Kiểm thử |

## 12.2. Sửa đổi

| File | Người | Nội dung |
|---|---|---|
| `entity/KhuyenMai.java` | Tài | Thêm `List<ChiTietKhuyenMai>`; giữ constructor cũ; bỏ `maDongSP` **sau V16** |
| `dao/KhuyenMaiDao.java` | Tài | `findHieuLuc` join chi tiết; `insert` header+details; `existsChongLan`; `findDongTheoDieuKien` |
| `dao/impl/KhuyenMaiDaoImpl.java` | Tài | Cài đặt các truy vấn/transaction |
| `controller/KhuyenMaiController.java` | Tài | Tạo 1 header + N details; validate; import |
| `ui/page/promotion/AddPromotionDialog.java` | Tài | Multi-select danh mục; ngày; dòng; xem trước |
| `ui/page/promotion/PromotionsPage.java` | Tài | Một dòng/chương trình; cột số dòng; nhập; quyền |
| `ui/AppNavigator.java` | Đăng | Wiring/đăng ký trang |
| `ui/shell/NavigationModel.java` | Đăng | `PROMOTIONS` cho nhân viên bán hàng |
| `controller/SanPhamController.java` | Quý | Nhập hàng loạt |
| `controller/DongSanPhamController.java` | Quý | Nhập hàng loạt |
| `ui/page/product/ProductPage.java` | Quý | Nút nhập/tải mẫu |
| `ui/page/product/ProductLinesPage.java` | Quý | Nút nhập/tải mẫu |
| Test hiện có | Cả ba | Cập nhật mapper/constructor |
| `docs/reference/use-cases/UC012-promotion-management.md` | Tài | Ghi đề xuất sau duyệt |
| `docs/reference/README.md` §5.1 | Đăng/chủ dự án | Ghi D18–D28 |

## 12.3. Không sửa

- `pom.xml`: POI 5.4.1 đã có.
- `db.properties`: không commit.
- `V1`–`V14`: không sửa.
- `V15`: **không sửa** sau khi đã chạy; V15 là additive, giữ `KhuyenMai.maDongSP`.
- `V16`: viết mới ở follow-up, không gộp vào V15.
- `PageId`, `AppShellFrame`, `Sidebar`: không đổi.
- `BanHangController`: không đổi logic; chỉ DAO đổi.
- Báo cáo DOCX gốc: không sửa.

# 13. Checklist bàn giao

- [ ] D18–D28 và Q3/Q5/Q7/Q8/Q9 đã chốt.
- [ ] Hợp đồng API W6.0 đã chốt.
- [ ] Migration V15 chạy sạch; backfill đủ; V15 **giữ** `KhuyenMai.maDongSP`.
- [ ] Migration V16 (follow-up) bỏ `KhuyenMai.maDongSP` sau khi mã Tài merge.
- [ ] `ExcelImporter`, `ExcelTemplate`, `ImportPreviewDialog` dùng được.
- [ ] Ba template được commit/review (tệp đã có tại `docs/reference/templates/`).
- [ ] Ba trang có Tải mẫu/Nhập Excel; lỗi theo dòng/cột.
- [ ] Tạo được 1 chương trình + N chi tiết; 0 dòng bị chặn.
- [ ] Một chi tiết xung đột → rollback cả chương trình.
- [ ] UC003 áp đúng; hóa đơn cũ không đổi.
- [ ] Nhập sản phẩm giữ RB37; nhập dòng sản phẩm tồn 0.
- [ ] Nhập all-or-nothing; FK violation bị chặn.
- [ ] Cả 2 actor nhập và bật ngay được (Q9).
- [ ] `mvn test` xanh; báo cáo file/UC/test/điểm mở.

# 14. Nhật ký thay đổi

| Ngày | Phiên bản | Nội dung |
|---|---|---|
| 2026-11-09 | 1.0 | Lập tài liệu Wave 6 |
| 2026-11-09 | 1.1 | Chốt điều kiện optional, `>= N`, all-or-nothing, 2 actor, chỉ `SAN_SANG_BAN` |
| 2026-11-09 | 1.2 | Chuyển sang Markdown, xóa DOCX |
| 2026-11-09 | 1.3 | Chốt Q3/Q5/Q7/Q8/Q9: thêm bảng `ChiTietKhuyenMai`, 1 KM nhiều chi tiết; nhiều danh mục; cả 2 actor; chặn FK; commit template; bật ngay |
| 2026-11-09 | 1.4 | Ghi nhận triển khai thực tế: V15 additive (giữ `KhuyenMai.maDongSP`); V16 bỏ cột sau khi mã Tài merge; template đã commit (Q8) |

# Phụ lục A — Hiện trạng mã nguồn đã đối chiếu

Kiểm tra ngày 2026-11-09 trên `develop`; không sửa mã nguồn.

| Khu vực | Hiện trạng | Ảnh hưởng Wave 6 |
|---|---|---|
| `entity/KhuyenMai.java` | Có `DongSanPham` bắt buộc | Bỏ `maDongSP`; thêm details |
| `dao/KhuyenMaiDao.java` | `findHieuLuc` theo dòng | Join `ChiTietKhuyenMai` |
| `dao/impl/KhuyenMaiDaoImpl.java` | `SELECT_BASE` join `DongSanPham` | Đổi query sang chi tiết |
| `controller/KhuyenMaiController.java` | `themMoi` một dòng | Tạo header + N details |
| `ui/page/promotion/PromotionsPage.java` | Một dòng/dòng sản phẩm | Một dòng/chương trình; cột số dòng |
| `ui/page/promotion/AddPromotionDialog.java` | Chọn một dòng | Multi-select danh mục + ngày |
| `controller/BanHangController.java` | Gọi `findHieuLuc` | Không đổi |
| `controller/SanPhamController.java` | Transaction riêng | Cần hàm nhập hàng loạt |
| `dao/impl/SanPhamImpl.java` | Insert + trigger | Tái sử dụng |
| `controller/DongSanPhamController.java` | Validate ĐVT/giá | Tái sử dụng |
| `util/MaGenerator.java` | MAX trên connection | Sinh mã trong transaction |
| `util/ExcelExporter.java` | POI | Giữ nguyên |
| `pom.xml` | POI 5.4.1 | Không thêm |

# Phụ lục B — Truy vết UC/RB

| Tính năng | UC/RB | Ghi chú |
|---|---|---|
| Khuyến mãi nhiều dòng | UC012; UC003 bước 7–8 | C-04 trong phạm vi |
| Chi tiết khuyến mãi | D18/D28 | 1 KM → N chi tiết |
| Snapshot | RB46; D27 | Không tính lại hóa đơn cũ |
| Xung đột | D11; quy tắc 10.b | Kiểm tra từng dòng |
| Nhập sản phẩm | UC008; RB37; D16 | Tồn serial |
| Nhập dòng sản phẩm | UC009; C-03; C-08 | Q5 |
| Mã | D7/C-12 | Không nhập mã |
| Tiền | C-11 | Không đổi |
| Đổi/trả | D17 | Không đổi |
| Quyền | XBR-13; UC012 | Tạo tay Quản lý; import 2 actor, bật ngay (Q9) |

# Phụ lục C — File dùng chung cần tạo trước

Ba template tĩnh `.xlsx` đã được tạo trong `docs/reference/templates/`; các file mã nguồn dùng chung khác vẫn theo thứ tự dưới đây.

| Thứ tự | File/API | Người tạo | Vì sao cần trước |
|---|---|---|---|
| 1 | `V15__chi_tiet_khuyen_mai.sql` | Đăng | Bảng chi tiết additive; V16 (bỏ cột) là follow-up sau Tài |
| 2 | `ExcelImporter.java` + `ImportSpec`/`ImportResult` | Đăng | Chặn 3 luồng nhập |
| 3 | `ExcelTemplate.java` | Đăng | Chặn nút Tải mẫu |
| 4 | `ImportPreviewDialog.java` | Đăng | Chặn UI nhập |
| 5 | Ba template `.xlsx` | Đăng | Review/commit |
| 6 | `NavigationModel` + quyền trang | Đăng/Tài | Nhân viên bán hàng nhập |
| 7 | Cập nhật README §5.1 | Đăng/chủ dự án | Ghi quyết định |

# Phụ lục D — Trạng thái quyết định

| Mã | Trạng thái |
|---|---|
| D18 | ✅ Thêm `ChiTietKhuyenMai` (V15 additive, giữ `maDongSP`); bỏ `KhuyenMai.maDongSP` bằng **V16** follow-up sau khi mã Tài merge |
| D19 | ✅ Nhiều danh mục/dòng; điều kiện optional |
| D20 | ✅ `>= N` |
| D21 | ✅ Kiểm tra từng dòng; xung đột chặn cả chương trình |
| D22 | ✅ Chặn toàn bộ tệp |
| D23 | ✅ Template có `Mã nhóm` |
| D24 | ✅ Cả 2 actor; bật ngay (Q9) |
| D25 | ✅ POI 5.4.1; chỉ Đăng tạo util |
| D26 | ✅ Chỉ `SAN_SANG_BAN`; RB37 |
| D27 | ✅ Snapshot hóa đơn |
| D28 | ✅ 1 KM → N chi tiết |
| Q3 | ✅ Nhiều danh mục |
| Q5 | ✅ Cả 2 actor |
| Q7 | ✅ Chặn FK |
| Q8 | ✅ Commit template |
| Q9 | ✅ Bật ngay |

---

**Hết tài liệu Wave 6 — phiên bản 1.4.**
