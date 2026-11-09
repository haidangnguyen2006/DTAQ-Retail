# W3 — Tuấn Anh: UC016 Thống kê doanh số theo nhân viên + xuất Excel

> **Branch:** `feature/anh-w3` · **Wave:** W3 (08/11–11/11)
> **Căn cứ:** [Phân công §3.3, §6](work-assignment-4-members.md) · [guide-tuan-anh.md §5.4](guide-tuan-anh.md) · [UC016](../use-cases/UC016-sales-by-employee.md) · XBR-03, XBR-11, XBR-16, RB65, RB66, EX-10

## 1. Kết quả

| Hạng mục W3 | Trạng thái |
|---|---|
| UC016: lọc kỳ + phạm vi nhân viên → số hóa đơn, doanh số trước thuế, điều chỉnh hàng trả, sau điều chỉnh | Xong, **chờ đăng ký trang** (§2) |
| Đối chiếu một nhân viên (luồng 10.a): hóa đơn + phiếu trả dùng để tính | Xong |
| Xuất Excel đúng báo cáo đang hiển thị, lỗi ghi tệp vẫn giữ báo cáo | Xong |
| `mvn test` | Xanh: 398 test, 0 lỗi, 0 bỏ qua (DB phát triển, seed V2 + V4) |

## 2. Việc cần Đăng làm trong `AppNavigator`

Trong `managerPages(...)`, tạo trang và thêm vào `List.of(...)` trả về:

```java
SalesByEmployeePage salesByEmployee = new SalesByEmployeePage(new ThongKeNhanVienController(
    new ThongKeNhanVienDaoImpl(dataSource), new NhanVienDaoImpl(dataSource)));
```

Chưa áp dụng thì mục "Thống kê doanh số theo nhân viên" vẫn hiện `PlaceholderPage`.

## 3. File thay đổi (tất cả là file mới)

| File | Nội dung |
|---|---|
| `dao/ThongKeNhanVienDao.java` | DAO chỉ đọc + record `DongThongKeNhanVien`, `HoaDonDoiChieu`, `DieuChinhDoiChieu` |
| `dao/impl/ThongKeNhanVienDaoImpl.java` | 3 câu `SELECT`, không ghi dữ liệu |
| `controller/ThongKeNhanVienController.java` | Kiểm quyền Quản lý, tổng hợp, đối chiếu, xuất Excel |
| `ui/page/report/SalesByEmployeePage.java` | Màn hình UC016 + hộp thoại đối chiếu |
| `test/.../dao/impl/ThongKeNhanVienDaoImplTest.java` | 6 test trên dữ liệu mẫu |
| `test/.../controller/ThongKeNhanVienControllerTest.java` | 7 test (fake DAO + tệp Excel thật trong thư mục tạm) |

Không sửa file nào có sẵn. Dùng nguyên `DateRange`, `KyBaoCaoFilter`, `ExcelExporter` của Đăng; không đụng DAO hóa đơn/phiếu của Đăng.

## 4. Đối chiếu UC016

| Bước | Hành vi | Vị trí |
|---|---|---|
| 2–4 | Bộ lọc: phạm vi nhân viên (tất cả hoặc một người, gồm cả tài khoản đã khóa) + kỳ `dd/MM/yyyy` + "Áp dụng" | `SalesByEmployeePage` |
| 5 / 5.a | Kỳ trống, sai định dạng, ngày không có thật hoặc đảo ngược → lỗi ngay dưới ô ngày, không truy vấn, giữ kết quả cũ | `KyBaoCaoFilter` + `DateRange` |
| 6–8 | Hóa đơn `trangThaiThanhToan = TRUE`, `ngayLap` trong kỳ, nhóm theo `nhanVienLap`; số HĐ = `COUNT(DISTINCT maHD)`; doanh số = `SUM(tienSauKhuyenMai)` | `CTE_BAN` |
| 9 | Trừ giá trị trước thuế của máy trên phiếu trả `HOAN_TAT` có `ngayHoanTat` trong kỳ, lấy từ chi tiết hóa đơn gốc, quy về nhân viên lập hóa đơn gốc | `CTE_TRA` |
| 6.a | Không có phát sinh → "Không có phát sinh", KPI về 0, tắt nút xuất | `render` |
| 10 | Bảng theo nhân viên + 4 KPI tổng | `render` |
| 10.a | Nút "Đối chiếu" hoặc nhấp đúp → hóa đơn và phiếu trả đã dùng để tính | `doiChieu`, `DoiChieuDialog` |

## 5. Vì sao code như vậy

- **Công thức điều chỉnh bám guide §5.4.** Đặc tả UC016 ghi rõ nguồn *không* quy định công thức điều chỉnh. Guide chốt: "phiếu `HOAN_TAT` trong kỳ → trừ giá trị hàng trả (snapshot HĐ gốc), quy về nhân viên lập HĐ gốc". Code làm đúng như vậy, theo các quy tắc sau:
  - **Chỉ phiếu hoàn tất** (RB65, XBR-11): phiếu `NHAP`, `CHO_XU_LY`, `HUY` không được tính. Test `loaiPhieuChuaHoanTat` chạy trên tháng 3 của dữ liệu mẫu, nơi có 2 phiếu chờ xử lý và 1 phiếu hủy.
  - **Theo ngày hoàn tất phiếu**, không theo ngày lập hóa đơn: hàng trả làm giảm doanh số của kỳ phát sinh việc trả. Ví dụ tháng 7: hai phiếu trả trên hóa đơn tháng 6 làm `NVBANHANG-2026-002` có 0 hóa đơn, doanh số sau điều chỉnh âm (test `dieuChinhQuyVeNhanVienLapHoaDonGoc`).
  - **Quy về người lập hóa đơn gốc, không phải người lập phiếu.** Trong dữ liệu mẫu, `PT-2026-000007` do `NVBANHANG-2026-003` lập nhưng bị trừ vào `NVBANHANG-2026-002`, người lập `HD-2026-000024`.
  - **Giá trị trừ là `tienSauKhuyenMai` của đúng serial trên `ChiTietHoaDon` gốc**, cùng thước đo "trước thuế" với doanh số (BR-12). Không dùng giá hay thuế hiện tại (XBR-03).
- **Phiếu đổi không điều chỉnh doanh số.** Guide §5.4 chỉ nói "giá trị hàng trả". Schema `PhieuDoi` không có cột tiền hoàn hay chênh lệch, và dữ liệu mẫu là đổi cùng model với `tienKhachDuaThem = 0`, nên không có giá trị nào để trừ. Xem §7 điểm 1.
- **Không làm tròn lại** (C-11, XBR-16): mọi số là tổng hoặc hiệu của giá trị đã làm tròn sẵn trong CSDL, dùng `BigDecimal`. Tổng cộng ở KPI và Excel là tổng các dòng, không tính lại từ đầu. Hiển thị bằng `TienUtil.dinhDang` (KPI) và `MoneyRenderer` (bảng).
- **Một CTE cho bán, một CTE cho trả, rồi `LEFT JOIN` vào `NhanVien`.** Tách hai CTE để `JOIN` chi tiết hóa đơn không nhân bản tiền trả lên theo số dòng bán. Nhân viên chỉ có hàng trả mà không bán trong kỳ vẫn xuất hiện. Lọc `CAST(? AS VARCHAR) IS NULL` để PostgreSQL suy được kiểu tham số khi xem tất cả.
- **Nhân viên đã khóa vẫn hiện** kèm nhãn "(đã khóa)": báo cáo là lịch sử, không lọc theo `trangThaiHoatDong` (test `nhanVienDaKhoaVanHien`).
- **Đối chiếu khớp đúng số liệu:** `hoaDonTrongKy`/`dieuChinhTrongKy` dùng cùng điều kiện với `thongKe`. Test `moiDongKhopDoiChieu` kiểm tra mọi nhân viên của cả năm: số HĐ, doanh số và điều chỉnh đều bằng tổng của danh sách đối chiếu.
- **Excel xuất từ đối tượng `BaoCao` đang hiển thị**, không truy vấn lại, đúng quyết định "xuất từ kết quả đang hiển thị" (D3).
  - Cột Excel lấy từ cùng một hằng số `TIEU_DE_COT` với màn hình, kèm dòng "Tổng cộng"; tiêu đề tệp ghi rõ kỳ.
  - Lỗi ghi tệp (ví dụ đích là thư mục, hoặc tệp đang mở) được chuyển thành thông báo tiếng Việt, và báo cáo trên màn hình giữ nguyên (EX-10).
  - Cách chọn tệp (`JFileChooser`, tự thêm `.xlsx`) theo cùng mẫu với `LongStockReportPage` của Tài.
- **Kiểm tra quyền ở controller** (NFR002, XBR-13): mọi method ném `SecurityException` nếu phiên không phải Quản lý.
- **Truy vấn chạy ngoài EDT**; bộ đếm yêu cầu bỏ qua kết quả cũ khi bấm "Áp dụng" liên tiếp.

## 6. Kiểm thử

| Nội dung | Test |
|---|---|
| 1 nhân viên có 2 hóa đơn + 1 phiếu trả hoàn tất (quý 1, `NVBANHANG-2025-001`) | `haiHoaDonMotPhieuTra` |
| Mọi dòng khớp danh sách đối chiếu | `moiDongKhopDoiChieu` |
| Hàng trả trên hóa đơn ngoài kỳ → quy về người lập hóa đơn gốc | `dieuChinhQuyVeNhanVienLapHoaDonGoc` |
| Phiếu chờ xử lý hoặc hủy không được tính | `loaiPhieuChuaHoanTat` |
| Lọc một nhân viên; kỳ rỗng; kỳ đảo ngược bị từ chối | `locNhanVienVaKyRong`, `kyRong`, `kyKhongHopLe` |
| Nhân viên đã khóa vẫn hiện lịch sử (khóa tạm rồi khôi phục) | `nhanVienDaKhoaVanHien` |
| Tổng cộng, phạm vi nhân viên, phân quyền | `ThongKeNhanVienControllerTest` |
| Excel: đúng tiêu đề cột, đúng dòng, có dòng tổng; không ghi được tệp → trả lỗi, không ném | `xuatExcel`, `xuatExcelLoiGhiTep` |

**Chạy thử giao diện trên cửa sổ thật** với DB phát triển, bằng chương trình kiểm tra riêng không nằm trong repo; chương trình tự đăng ký trang vào `AppShellFrame`. Có đối chiếu ảnh chụp. Các luồng đã chạy:

- kỳ mặc định 7 ngày;
- quý 1 (9 nhân viên, khớp DAO);
- kỳ đảo ngược → báo lỗi, giữ kết quả cũ;
- năm 2030 → "Không có phát sinh", nút xuất bị tắt;
- lọc một nhân viên;
- hộp thoại đối chiếu (2 hóa đơn, 1 phiếu trả, sau điều chỉnh 11.200.000 VND);
- tháng 7 (hàng trả quy về người lập hóa đơn tháng 6);
- xuất Excel thành công qua `JFileChooser`;
- xuất vào một thư mục → hộp thoại "Xuất Excel thất bại", báo cáo vẫn còn;
- phiên nhân viên bán hàng bị từ chối.

`git diff --check` sạch.

## 7. Điểm cần Đăng xem

1. **Phiếu đổi có điều chỉnh doanh số không?** Hiện không điều chỉnh, vì guide chỉ nói "hàng trả" và schema không có giá trị chênh lệch. Guide của Tài (UC017) ghi "phiếu đổi chỉ trừ chênh lệch đã hoàn". Nếu nhóm muốn UC016 tính tương tự thì cần chốt "chênh lệch đã hoàn" lấy từ cột nào, vì `PhieuDoi` chỉ có `tienKhachDuaThem`.
2. **Mốc thời gian của điều chỉnh** đang là `ngayHoanTat` của phiếu trả, theo cách hiểu "phiếu `HOAN_TAT` trong kỳ" của guide. Nên thống nhất cùng mốc với UC015 (Quý) và UC017 (Tài) để ba báo cáo khớp nhau.
3. **Đăng ký trang** trong `managerPages` (§2).
4. **Chưa đối chiếu Figma** cho `SalesByEmployeePage`; màn hình dựng theo UI kit và bố cục của `LongStockReportPage`.

## 8. Việc tiếp theo (W4, theo guide)

Rà soát UC001/UC002 (phiên, tài khoản khóa, thông báo tiếng Việt), bổ sung test và review chéo.
