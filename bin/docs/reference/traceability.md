# Truy vết yêu cầu – use case – thực thể

Tài liệu kiểm tra độ phủ và các ánh xạ **có câu chữ trực tiếp** trong báo cáo v12. Nguồn chuẩn: [`docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx).

**Phương pháp và định vị:** nội dung được ghi lại từ DOCX và đối chiếu trực tiếp với bảng/tiêu đề nguồn (ngày cập nhật tài liệu: 2026-10-19); FR/NFR ở Chương 1 mục IV–V; tác nhân/danh mục/đặc tả UC ở Chương 3 mục II–IV. DOCX có `TOC` field instruction nhưng không có nội dung mục lục đã lưu trong body; do đó **không dùng số trang cho chương/mục/UC** — trang là **unverified**. Định vị dùng chương/mục/tiêu đề UC. Chỉ ghi nhận ánh xạ khi nguồn nêu trực tiếp; mọi quan hệ suy đoán đều bị đánh dấu **chưa xác nhận** và không dùng làm căn cứ triển khai.

**Liên kết:** [Phạm vi và yêu cầu](requirements/scope-and-requirements.md) · [Danh mục use case](use-cases/catalog.md) · [README](README.md)

## 1. Quy ước

- **Xác nhận:** nguồn nêu trực tiếp cặp ánh xạ (ví dụ cột “Ghi chú” của danh mục UC liệt kê thực thể).
- **Chưa xác nhận:** nguồn không nêu cặp ánh xạ; cột “ứng viên” chỉ là quan sát theo tên/chức năng, **không phải** ánh xạ chính thức.
- **Không có:** nguồn không có đối tượng tương ứng trong phạm vi đã trích.

## 2. Tổng hợp độ phủ

| Nhóm | Số lượng | Ánh xạ tường minh sang use case |
|---|---|---|
| Yêu cầu chức năng FR001–FR016 | 16 | **Không có** bảng cặp FR–UC trong nguồn |
| Yêu cầu phi chức năng NFR001–NFR017 | 17 | **Không có** bảng cặp NFR–UC trong nguồn |
| Use case UC001–UC019 | 19 | **Có** ánh xạ UC → tác nhân và UC → thực thể qua danh mục |
| Tác nhân | 02 | Nguồn Ch.3 §II |
| Câu hỏi làm rõ yêu cầu | 08 | Nguồn Ch.1 §III |

**Căn cứ nguyên văn (Ch.1 §IV):** “Để bảo đảm khả năng truy vết giữa **yêu cầu chức năng → Use Case → thiết kế và hiện thực**, các yêu cầu được đánh số tương ứng với bộ Use Case của hệ thống.” Nguồn chỉ nêu nguyên tắc chung, không cung cấp bảng cặp cụ thể; 16 FR và 19 UC không tương ứng một-một.

## 3. Truy vết yêu cầu chức năng

Nguồn FR: Chương 1 mục IV. Cột “Ứng viên theo tên/chức năng” **chưa xác nhận**.

| FR | Tên yêu cầu | Ánh xạ tường minh sang UC | Ứng viên theo tên/chức năng (chưa xác nhận) | Ghi chú |
|---|---|---|---|---|
| FR001 | Đăng nhập | Không | UC001 | Tên trùng UC001; không có cặp FR–UC trong nguồn |
| FR002 | Đăng xuất | Không | UC002 | Tên trùng UC002 |
| FR003 | Quản lý nhân viên bán hàng | Không | UC013 (một phần) | FR gồm thêm/cập nhật/tra cứu/tài khoản/khóa; đặc tả UC013 chỉ có luồng thêm |
| FR004 | Quản lý thông tin khách hàng | Không | UC005, UC007 | UC007 chỉ thêm khách hàng; UC005 tra cứu khách hàng/giao dịch |
| FR005 | Quản lý danh mục loại sản phẩm | Không | UC010 | Tên “danh mục sản phẩm”; đặc tả chỉ luồng thêm |
| FR006 | Quản lý thông tin sản phẩm và tồn kho | Không | UC004, UC008, UC009 | Trải trên tra cứu và thêm sản phẩm/dòng sản phẩm |
| FR007 | Lập hóa đơn bán lẻ | Không | UC003 | Tên trùng UC003 |
| FR008 | Xử lý hoàn trả/đổi sản phẩm | Không | UC006, UC018 | UC006 lập/xử lý phiếu; UC018 phê duyệt vượt hạn mức |
| FR009 | Xử lý giao dịch đã thanh toán nhưng khách hủy mua | Không | UC006 (hành vi) | Không có UC riêng; Ch.1 §II.4 nêu xử lý qua nghiệp vụ đổi/trả |
| FR010 | Thống kê doanh thu | Không | UC017 | Tên gần trùng UC017 |
| FR011 | Xuất báo cáo doanh thu | Không | UC017 (chức năng) | UC017 có “xuất báo cáo Excel”; không có UC riêng cho kết xuất |
| FR012 | Thống kê doanh số theo nhân viên | Không | UC016 | Tên trùng UC016 |
| FR013 | Thống kê doanh số theo sản phẩm | Không | UC015 | Tên trùng UC015 |
| FR014 | Báo cáo hàng tồn kho lâu ngày | Không | UC014 | Tên UC014 lệch “hằng ngày”/“lâu ngày”; xem C-01 |
| FR015 | Sao lưu dữ liệu | Không | **Không có** | Danh mục 19 UC không có use case sao lưu |
| FR016 | Phục hồi dữ liệu | Không | **Không có** | Danh mục 19 UC không có use case phục hồi |

## 4. Truy vết use case → tác nhân và thực thể (tường minh)

Nguồn: Chương 3 mục III (danh mục) và mục IV (tác nhân đặc tả). Đây là các ánh xạ **xác nhận**.

| UC | Tên đặc tả | Tác nhân (đặc tả) | Thực thể / ghi chú tường minh (danh mục) | Ghi chú truy vết |
|---|---|---|---|---|
| UC001 | Đăng nhập | Nhân viên quản lý; Nhân viên bán hàng | `NhanVien`, `LoaiQuyen` | Tác nhân danh mục: “Cả hai tác nhân” |
| UC002 | Đăng xuất | Nhân viên quản lý; Nhân viên bán hàng | `NhanVien` (không xóa) | — |
| UC003 | Lập hóa đơn bán lẻ | Nhân viên bán hàng | `HoaDon`, `ChiTietHoaDon`, `SanPham`; chỉ máy `SAN_SANG_BAN` được bán | Mâu thuẫn mức tồn kho với FR007 (số lượng) — xem C-07 |
| UC004 | Tra cứu sản phẩm và tồn kho | Nhân viên bán hàng | `DongSanPham`, `DanhMucSanPham`, `SanPham` | — |
| UC005 | Tra cứu khách hàng và giao dịch | Nhân viên bán hàng | `KhachHang`, `HoaDon`, `PhieuDoi`, `PhieuTra` | — |
| UC006 | Xử lý đổi trả sản phẩm | Nhân viên bán hàng | `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi`; chờ phê duyệt | Liên quan FR008, FR009 |
| UC007 | Thêm khách hàng | Nhân viên bán hàng | `KhachHang`; giữ liên kết lịch sử hóa đơn | Tên danh mục khác đặc tả — C-02 |
| UC008 | Thêm sản phẩm | Nhân viên bán hàng | `SanPham`, `DongSanPham`; `TrangThaiSanPham` | Tác nhân danh mục “Quản lý” — C-03 |
| UC009 | Thêm dòng sản phẩm | Nhân viên bán hàng | `DongSanPham.trangThaiKinhDoanh`; `SanPham` có serial | Tác nhân danh mục khác — C-03 |
| UC010 | Thêm danh mục sản phẩm | Nhân viên bán hàng | `DanhMucSanPham`, `DongSanPham`; không mất liên kết | Tác nhân danh mục khác — C-03 |
| UC011 | Thêm sản phẩm lỗi | Nhân viên bán hàng | `SanPhamLoi`, `SanPham`, `TrangThaiXuLyLoi` | Tác nhân danh mục khác — C-03 |
| UC012 | Thêm khuyến mãi | Nhân viên quản lý | `KhuyenMai`, `DongSanPham`; chỉ khi đang dùng và còn hiệu lực | Phạm vi đã chốt 2026-10-24: thuộc phạm vi — C-04 (trước đây là mâu thuẫn, nay đã giải quyết) |
| UC013 | Thêm nhân viên bán hàng | Nhân viên quản lý | `NhanVien`, `LoaiQuyen`, `trangThaiHoatDong`; không xóa lịch sử | Liên quan FR003 (một phần) |
| UC014 | Báo cáo sản phẩm tồn kho lâu ngày | Nhân viên quản lý | `DongSanPham`, `SanPham`, `ngayNhapKho`, `TrangThaiSanPham` | Liên quan FR014; mâu thuẫn tên gọi — C-01 |
| UC015 | Thống kê doanh số theo sản phẩm | Nhân viên quản lý | `HoaDon`, `ChiTietHoaDon`, `DongSanPham`; chỉ hóa đơn đã thanh toán | Liên quan FR013 |
| UC016 | Thống kê doanh số theo nhân viên | Nhân viên quản lý | `NhanVien`, `HoaDon`, `ChiTietHoaDon`; chỉ hóa đơn đã thanh toán | Liên quan FR012 |
| UC017 | Thống kê doanh thu | Nhân viên quản lý | `HoaDon`, `PhieuTra`, `PhieuDoi`; loại phiếu hủy/từ chối, tránh trùng tiền đổi/trả | Liên quan FR010, FR011 |
| UC018 | Phê duyệt yêu cầu đổi trả | Nhân viên quản lý | `PhieuTra`, `PhieuDoi`, `TrangThaiDuyet`, `TrangThaiXuLyPhieu`; chỉ hoàn tất sau khi duyệt | Liên quan FR008, BR-07 |
| UC019 | Cập nhật giá bán và thuế suất | Nhân viên quản lý | `DongSanPham`; ghi chú cần bổ sung giá/thuế chốt vào `ChiTietHoaDon` | Vai trò đã chốt 2026-10-25: **chỉ Quản lý** — C-08; ghi chú có thể đã lỗi thời so với Ch.4 |

## 5. Truy vết yêu cầu phi chức năng

Nguồn NFR: Chương 1 mục V. Nguồn **không nêu** ánh xạ NFR → UC; cột cuối chỉ ghi nội dung nguồn liên quan, không phải ánh xạ.

| NFR | Nhóm | Ánh xạ tường minh | Nội dung nguồn liên quan (không phải ánh xạ) |
|---|---|---|---|
| NFR001 | Bảo mật – Xác thực | Không | UC001; BR xác thực |
| NFR002 | Bảo mật – Phân quyền | Không | UC001; phân quyền hai tác nhân Ch.3 §II |
| NFR003 | Bảo mật dữ liệu | Không | Báo cáo, sao lưu/phục hồi chỉ Quản lý |
| NFR004 | Hiệu năng | Không | Thao tác đăng nhập, tìm kiếm, tồn kho, hóa đơn |
| NFR005 | Hiệu năng thống kê | Không | UC015–UC017 |
| NFR006 | Khả năng sử dụng | Không | Giao diện tiếng Việt |
| NFR007 | Khả năng sử dụng | Không | Thao tác tại quầy; hỗ trợ bàn phím |
| NFR008 | Tính toàn vẹn dữ liệu | Không | UC019; BR-04 bảo toàn giá/thuế |
| NFR009 | Tính nhất quán giao dịch | Không | UC003, UC006 |
| NFR010 | Độ tin cậy | Không | Thông báo lỗi, không mất dữ liệu nhập |
| NFR011 | Sao lưu và phục hồi | Không | FR015, FR016; không có UC tương ứng |
| NFR012 | Khả năng bảo trì | Không | Kiến trúc phân lớp Ch.1 §II.9 |
| NFR013 | Tính nhất quán giao diện | Không | Điều hướng màn hình chính/sidebar |
| NFR014 | Khả năng kiểm tra dữ liệu đầu vào | Không | Kiểm tra trường bắt buộc, kiểu dữ liệu |
| NFR015 | Khả năng truy vết | Không | Người thực hiện, chứng từ gốc, người phê duyệt |
| NFR016 | Khả năng mở rộng | Không | Khuyến mãi, phương thức thanh toán, báo cáo |
| NFR017 | Độ chính xác tính toán tài chính | Không | Làm tròn tiền tệ thống nhất — đã chốt VND/0 chữ số thập phân/`HALF_UP` (C-11, 2026-10-24) |

## 6. Khoảng trống và câu hỏi cần xác nhận

| # | Khoảng trống | Ảnh hưởng |
|---|---|---|
| G-01 | Không có bảng ánh xạ tường minh FR ↔ UC dù Ch.1 §IV nêu nguyên tắc truy vết | Không thể chứng minh độ phủ yêu cầu bằng nguồn |
| G-02 | Không có UC sao lưu/phục hồi cho FR015, FR016, NFR011 | Chức năng bắt buộc chưa được đặc tả ở mức use case |
| G-03 | Không có UC riêng cho kết xuất báo cáo (FR011); chỉ nằm trong UC017 | Cần xác nhận kết xuất là chức năng con hay UC độc lập |
| G-04 | Không có UC riêng cho hủy mua sau thanh toán (FR009) | Nguồn nêu xử lý qua đổi/trả nhưng không gán UC |
| G-05 | FR003/FR004/FR005/FR006 gồm cập nhật, tra cứu, xóa; đặc tả UC007–UC013 chỉ mô tả luồng thêm | Nguy cơ thiếu luồng so với yêu cầu |
| G-06 | Không có quy tắc làm tròn tiền tệ cụ thể (C-11) | Không thể kiểm chứng NFR017 |
| G-07 | Mô hình tồn kho serial so với số lượng chưa thống nhất (C-07) | Ảnh hưởng trực tiếp UC003/UC004/UC008 và FR007 |
| G-08 | Vai trò cập nhật giá/thuế chưa thống nhất (C-08) | Ảnh hưởng phân quyền UC019 và FR006 |
| G-09 | Tác nhân UC008–UC011 chưa thống nhất (C-03) | Ảnh hưởng phân quyền Ch.3 |
| G-10 | Ch.3 §IV UC009/UC010/UC012/UC013 yêu cầu **nhập mã định danh**, trong khi Ch.4 §I.3 và `RB02`/`RB03` quy định mã **tự sinh, bất biến, không tái sử dụng** (C-12, BR-C11). **Xung đột nguồn được giữ làm bằng chứng lịch sử.** | Ảnh hưởng luồng thêm dòng sản phẩm/danh mục/khuyến mãi/nhân viên và quy tắc cấp mã; **implementation hiện tại (đã duyệt 2026-10-20):** mã do ứng dụng sinh, không dùng DB sequence/identity/default. |
| G-11 | Mô hình văn bản `ChiTietHoaDon` (Ch.4 §I.3) ghi `sanPham: DongSanPham`, `soLuong` = 1, các khoản dẫn xuất, trong khi `RB22` cần xác định **máy thực tế** và `RB46` cần **lưu giá/thuế/khuyến mãi tại thời điểm giao dịch** (BR-C12). Bằng chứng trực quan ngày 2026-10-19 (bản sao byte-identical sơ đồ lớp/EER/sơ đồ HQT CSDL): sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai`, không thấy trường serial/snapshot nào được vẽ trực tiếp; EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh. **Xung đột nguồn được giữ làm bằng chứng lịch sử; implementation hiện tại theo V1 amendment đã duyệt (2026-10-20)** — xem [domain §8](domain/class-model-and-entities.md#8-bản-đồ-v1-đã-phê-duyệt), [database §6](database/schema-and-integrity.md#6-bản-đồ-ddl-v1-đã-phê-duyệt); không suy đoán thêm cột/quan hệ ngoài phần thấy được | Ảnh hưởng UC003, UC006, UC015, UC016, UC017 và thiết kế CSDL chi tiết hóa đơn |

**Câu hỏi mở:**

1. Có bảng ánh xạ FR ↔ UC nào khác ngoài báo cáo v12 không?
2. Chức năng sao lưu/phục hồi (FR015, FR016, NFR011) có được đặc tả ở tài liệu khác không?
3. Tên chuẩn của UC014 là “hằng ngày” hay “lâu ngày”?
4. Ai cập nhật giá bán/thuế suất: Quản lý hay Nhân viên bán hàng theo phê duyệt?
5. Tồn kho được quản lý theo serial từng máy hay theo số lượng sản phẩm?
6. Các UC007–UC013 có bao gồm cập nhật/tra cứu/xóa hay chỉ thêm?
7. Quy tắc làm tròn tiền tệ cụ thể để kiểm chứng NFR017 là gì?

## 7. Liên kết

- [README bộ tài liệu](README.md)
- [Phạm vi và yêu cầu](requirements/scope-and-requirements.md)
- [Danh mục use case](use-cases/catalog.md)
- [Quy tắc nghiệp vụ và ngoại lệ](business-rules/rules-and-exceptions.md)
- [Ghi chú mô hình `ChiTietHoaDon`](domain/class-model-and-entities.md) — xem G-11 / BR-C12

## 8. Điều hướng sổ đăng ký

Bảng nối `C-xx` (phạm vi/xung đột), `G-xx` (khoảng trống) và `BR-Cxx` (quy tắc/khác biệt) nằm tại [Phạm vi và yêu cầu — §7](requirements/scope-and-requirements.md). Ô chưa có mã tương ứng được ghi “—”, nghĩa là **chưa lập hoặc chưa xác nhận**, không suy diễn quan hệ.
