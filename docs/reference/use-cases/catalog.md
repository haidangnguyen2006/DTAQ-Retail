# Danh mục use case

Danh mục 19 use case của hệ thống, ghi lại từ báo cáo v12. Nguồn chuẩn: [`docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx).

**Phương pháp và định vị:** nội dung được ghi lại từ DOCX và đối chiếu trực tiếp với bảng danh mục cùng tiêu đề đặc tả (ngày cập nhật tài liệu: 2026-10-19). Danh mục (ID, tên, mô tả ngắn, chức năng, ghi chú) lấy từ Chương 3 mục III; đặc tả lấy từ Chương 3 mục IV. DOCX có `TOC` field instruction nhưng không có nội dung mục lục đã lưu trong body; do đó **không dùng số trang cho chương/mục/UC** — trang là **unverified**. Định vị dùng chương/mục và tiêu đề UC. Nội dung giữ nguyên câu chữ nguồn, kể cả lỗi chính tả.

**Canonical:** đặc tả đầy đủ của mỗi use case (mục đích, mô tả, tiền điều kiện, hậu điều kiện, luồng sự kiện chính/phụ) là **canonical trong tệp UC tương ứng**. Mục 2 dưới đây chỉ giữ trường danh mục, định vị nguồn và liên kết tới tệp canonical; không lặp lại thân đặc tả.

**Liên kết:** [Phạm vi và yêu cầu](../requirements/scope-and-requirements.md) · [Truy vết](../traceability.md) · [README](../README.md)

## 1. Đăng ký use case

“Tên danh mục” và “Tác nhân danh mục” lấy nguyên văn từ Ch.3 §III; “Tên đặc tả” lấy từ tiêu đề mục đặc tả; “Tác nhân đặc tả” lấy từ trường **Tác nhân** của đặc tả. Mỗi ID liên kết tới tệp đặc tả UC tương ứng.

| ID | Tên danh mục (Ch.3 §III) | Tên đặc tả (Ch.3 §IV) | Tác nhân danh mục | Tác nhân đặc tả | Mô tả ngắn (danh mục) |
|---|---|---|---|---|---|
| [UC001](UC001-login.md) | Đăng nhập | Đăng nhập | Cả hai tác nhân | Nhân viên quản lý; Nhân viên bán hàng | Xác thực nhân viên và xác định quyền sử dụng. |
| [UC002](UC002-logout.md) | Đăng xuất | Đăng xuất | Cả hai tác nhân | Nhân viên quản lý; Nhân viên bán hàng | Kết thúc phiên làm việc hiện tại. |
| [UC003](UC003-retail-invoice.md) | Lập hóa đơn bán lẻ | Lập hóa đơn bán lẻ | Bán hàng | Nhân viên bán hàng | Ghi nhận giao dịch bán máy cho khách hàng. |
| [UC004](UC004-product-inventory-lookup.md) | Tra cứu sản phẩm và tồn kho | Tra cứu sản phẩm và tồn kho | Bán hàng | Nhân viên bán hàng | Tìm dòng sản phẩm và từng máy theo serial. |
| [UC005](UC005-customer-transaction-lookup.md) | Tra cứu khách hàng và giao dịch | Tra cứu khách hàng và giao dịch | Bán hàng | Nhân viên bán hàng | Xem thông tin khách và các giao dịch liên quan. |
| [UC006](UC006-return-exchange.md) | Xử lý đổi trả sản phẩm | Xử lý đổi trả sản phẩm | Bán hàng | Nhân viên bán hàng | Lập và xử lý hồ sơ đổi hoặc trả theo hóa đơn gốc. |
| [UC007](UC007-customer-management.md) | Quản lý khách hàng | Thêm khách hàng | Bán hàng | Nhân viên bán hàng | Lưu thông tin khách phục vụ bán hàng. |
| [UC008](UC008-product-management.md) | Quản lý sản phẩm | Thêm sản phẩm | Quản lý | Nhân viên bán hàng | Quản lý từng máy và quá trình tiếp nhận hàng. |
| [UC009](UC009-product-line-management.md) | Quản lý dòng sản phẩm | Thêm dòng sản phẩm | Quản lý | Nhân viên bán hàng | Quản lý thông tin chung của các máy cùng dòng. |
| [UC010](UC010-product-category-management.md) | Quản lý danh mục sản phẩm | Thêm danh mục sản phẩm | Quản lý | Nhân viên bán hàng | Tổ chức các dòng sản phẩm theo danh mục. |
| [UC011](UC011-defective-product-management.md) | Quản lý sản phẩm lỗi | Thêm sản phẩm lỗi | Quản lý | Nhân viên bán hàng | Theo dõi máy lỗi và tiến độ xử lý. |
| [UC012](UC012-promotion-management.md) | Quản lý khuyến mãi | Thêm khuyến mãi | Quản lý | Nhân viên quản lý | Thiết lập chương trình giảm giá cho dòng sản phẩm. |
| [UC013](UC013-sales-staff-management.md) | Quản lý nhân viên bán hàng | Thêm nhân viên bán hàng | Quản lý | Nhân viên quản lý | Quản lý hồ sơ và tài khoản nhân viên bán hàng. |
| [UC014](UC014-inventory-report.md) | Báo cáo sản phẩm tồn kho **hằng ngày** | Báo cáo sản phẩm tồn kho **lâu ngày** | Quản lý | Nhân viên quản lý | Theo dõi lượng máy tồn theo ngày. |
| [UC015](UC015-sales-by-product.md) | Thống kê doanh số theo sản phẩm | Thống kê doanh số theo sản phẩm | Quản lý | Nhân viên quản lý | Đánh giá kết quả bán theo dòng sản phẩm. |
| [UC016](UC016-sales-by-employee.md) | Thống kê doanh số theo nhân viên | Thống kê doanh số theo nhân viên | Quản lý | Nhân viên quản lý | Đánh giá kết quả bán của từng nhân viên. |
| [UC017](UC017-revenue-statistics.md) | Thống kê doanh thu | Thống kê doanh thu | Quản lý | Nhân viên quản lý | Tổng hợp doanh thu trong kỳ và xuất báo cáo. |
| [UC018](UC018-return-approval.md) | Phê duyệt yêu cầu đổi trả | Phê duyệt yêu cầu đổi trả | Quản lý | Nhân viên quản lý | Quyết định chấp nhận hoặc từ chối phiếu. |
| [UC019](UC019-price-tax-update.md) | Cập nhật giá bán và thuế suất | Cập nhật giá bán và thuế suất | Quản lý | Nhân viên quản lý | Điều chỉnh thông tin giá và thuế của dòng sản phẩm. |

## 2. Trường danh mục và định vị theo use case

Mỗi mục chỉ ghi trường **Chức năng** và **Ghi chú** lấy từ danh mục Ch.3 §III cùng **Định vị**. Mục đích, mô tả, tiền điều kiện, hậu điều kiện và hai luồng sự kiện đầy đủ nằm trong tệp UC canonical được liên kết ở cuối mỗi mục. Trường “Ghi chú” của danh mục là nguồn ánh xạ tường minh tới thực thể và tác nhân (xem [traceability.md](../traceability.md)).

### UC001 — Đăng nhập

- **Chức năng (danh mục):** Kiểm tra tên đăng nhập, mật khẩu, trạng thái hoạt động và loại quyền.
- **Ghi chú (danh mục):** Cả hai tác nhân; NhanVien, LoaiQuyen.
- **Định vị:** Ch.3 §IV, `### UC001 – Đăng nhập`. Đặc tả đầy đủ: [UC001-login.md](UC001-login.md).

### UC002 — Đăng xuất

- **Chức năng (danh mục):** Hủy phiên đăng nhập, trở về màn hình đăng nhập.
- **Ghi chú (danh mục):** Cả hai tác nhân; không xóa NhanVien.
- **Định vị:** Ch.3 §IV, `### UC002 – Đăng xuất`. Bảng đặc tả ghi tiêu đề “Use case: Đăng xuất” (không kèm mã; mã nằm ở tiêu đề mục). Nguồn có lỗi chính tả “đẻ”, “hiển thi”; tài liệu giữ nguyên. Đặc tả đầy đủ: [UC002-logout.md](UC002-logout.md).

### UC003 — Lập hóa đơn bán lẻ

- **Chức năng (danh mục):** Chọn khách, dòng sản phẩm và serial đủ điều kiện; áp dụng khuyến mãi; tính tiền, thuế; thanh toán, phát hành hoặc hủy hóa đơn nháp.
- **Ghi chú (danh mục):** Bán hàng; HoaDon, ChiTietHoaDon, SanPham. Chỉ máy SAN_SANG_BAN được bán.
- **Định vị:** Ch.3 §IV, `### UC003 – Lập hóa đơn bán lẻ`. Đặc tả đầy đủ: [UC003-retail-invoice.md](UC003-retail-invoice.md).

### UC004 — Tra cứu sản phẩm và tồn kho

- **Chức năng (danh mục):** Tra cứu mẫu, danh mục, số lượng tồn và trạng thái máy; kiểm tra khả năng đáp ứng bán hàng.
- **Ghi chú (danh mục):** Bán hàng; DongSanPham, DanhMucSanPham, SanPham.
- **Định vị:** Ch.3 §IV, `### UC004 – Tra cứu sản phẩm và tồn kho`. Đặc tả đầy đủ: [UC004-product-inventory-lookup.md](UC004-product-inventory-lookup.md).

### UC005 — Tra cứu khách hàng và giao dịch

- **Chức năng (danh mục):** Tra cứu khách, lịch sử mua hàng, hóa đơn, phiếu đổi và phiếu trả.
- **Ghi chú (danh mục):** Bán hàng; KhachHang, HoaDon, PhieuDoi, PhieuTra.
- **Định vị:** Ch.3 §IV, `### UC005 – Tra cứu khách hàng và giao dịch`. Đặc tả đầy đủ: [UC005-customer-transaction-lookup.md](UC005-customer-transaction-lookup.md).

### UC006 — Xử lý đổi trả sản phẩm

- **Chức năng (danh mục):** Kiểm tra điều kiện, số lượng đã đổi/trả; ghi tình trạng máy, lý do; lập chi tiết phiếu, tính hoàn tiền hoặc chênh lệch.
- **Ghi chú (danh mục):** Bán hàng; PhieuTra, ChiTietPhieuTra, PhieuDoi, ChiTietPhieuDoi; chờ phê duyệt.
- **Định vị:** Ch.3 §IV, `### UC006 – Xử lý đổi trả sản phẩm`. Đặc tả đầy đủ: [UC006-return-exchange.md](UC006-return-exchange.md).

### UC007 — Thêm khách hàng (danh mục: Quản lý khách hàng)

- **Chức năng (danh mục):** Thêm khách hàng, cập nhật họ tên, số điện thoại và địa chỉ.
- **Ghi chú (danh mục):** Bán hàng; KhachHang. Giữ liên kết lịch sử hóa đơn.
- **Định vị:** Ch.3 §IV, `### UC007 – Thêm khách hàng`. Đặc tả đầy đủ: [UC007-customer-management.md](UC007-customer-management.md).

### UC008 — Thêm sản phẩm (danh mục: Quản lý sản phẩm)

- **Chức năng (danh mục):** Thêm máy theo dòng sản phẩm, ghi serial duy nhất và ngày nhập; cập nhật thông tin và trạng thái máy.
- **Ghi chú (danh mục):** Quản lý; SanPham, DongSanPham. Trạng thái máy dùng TrangThaiSanPham.
- **Định vị:** Ch.3 §IV, `### UC008 – Thêm sản phẩm`. Đặc tả đầy đủ: [UC008-product-management.md](UC008-product-management.md).

### UC009 — Thêm dòng sản phẩm (danh mục: Quản lý dòng sản phẩm)

- **Chức năng (danh mục):** Thêm, cập nhật tên dòng, danh mục và thông tin kinh doanh; ngừng hoặc kinh doanh lại dòng sản phẩm.
- **Ghi chú (danh mục):** Quản lý; DongSanPham.trangThaiKinhDoanh. SanPham đại diện từng máy có serial.
- **Định vị:** Ch.3 §IV, `### UC009 – Thêm dòng sản phẩm`. Đặc tả đầy đủ: [UC009-product-line-management.md](UC009-product-line-management.md).

### UC010 — Thêm danh mục sản phẩm (danh mục: Quản lý danh mục sản phẩm)

- **Chức năng (danh mục):** Thêm, cập nhật, xóa danh mục; kiểm tra dòng sản phẩm đang tham chiếu trước khi xóa.
- **Ghi chú (danh mục):** Quản lý; DanhMucSanPham, DongSanPham. Không làm mất liên kết dữ liệu.
- **Định vị:** Ch.3 §IV, `### UC010 – Thêm danh mục sản phẩm`. Đặc tả đầy đủ: [UC010-product-category-management.md](UC010-product-category-management.md).

### UC011 — Thêm sản phẩm lỗi (danh mục: Quản lý sản phẩm lỗi)

- **Chức năng (danh mục):** Ghi nhận lỗi, ngày phát hiện và máy liên quan; cập nhật chờ xử lý, gửi bảo hành, trả nhà cung cấp hoặc thanh lý.
- **Ghi chú (danh mục):** Quản lý; SanPhamLoi, SanPham, TrangThaiXuLyLoi; liên kết chi tiết đổi/trả khi phát sinh.
- **Định vị:** Ch.3 §IV, `### UC011 – Thêm sản phẩm lỗi`. Đặc tả đầy đủ: [UC011-defective-product-management.md](UC011-defective-product-management.md).

### UC012 — Thêm khuyến mãi (danh mục: Quản lý khuyến mãi)

- **Chức năng (danh mục):** Tạo chương trình; khai báo thời gian, loại và giá trị giảm; gắn dòng sản phẩm; kích hoạt hoặc dừng áp dụng.
- **Ghi chú (danh mục):** Quản lý; KhuyenMai, DongSanPham. Chỉ áp dụng khi đang dùng và còn hiệu lực.
- **Định vị:** Ch.3 §IV, `### UC012 – Thêm khuyến mãi`. Xem mâu thuẫn phạm vi khuyến mãi tại [C-04](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case). Đặc tả đầy đủ: [UC012-promotion-management.md](UC012-promotion-management.md).

### UC013 — Thêm nhân viên bán hàng (danh mục: Quản lý nhân viên bán hàng)

- **Chức năng (danh mục):** Thêm nhân viên và tài khoản; tra cứu, cập nhật thông tin; khóa đăng nhập khi nghỉ việc.
- **Ghi chú (danh mục):** Quản lý; NhanVien, LoaiQuyen, trangThaiHoatDong. Không xóa lịch sử giao dịch.
- **Định vị:** Ch.3 §IV, `### UC013 – Thêm nhân viên bán hàng`. Đặc tả đầy đủ: [UC013-sales-staff-management.md](UC013-sales-staff-management.md).

### UC014 — Báo cáo sản phẩm tồn kho lâu ngày (danh mục: … hằng ngày)

- **Chức năng (danh mục):** Tổng hợp số lượng theo dòng sản phẩm và trạng thái máy; đối chiếu lượng tồn và máy sẵn sàng bán.
- **Ghi chú (danh mục):** Quản lý; DongSanPham, SanPham, ngayNhapKho, TrangThaiSanPham.
- **Định vị:** Ch.3 §IV, `### UC014 – Báo cáo sản phẩm tồn kho lâu ngày`. **Mâu thuẫn tên gọi:** danh mục Ch.3 §III ghi “hằng ngày”, trong khi tiêu đề đặc tả chi tiết và FR014 ghi “lâu ngày”; xem [C-01](../requirements/scope-and-requirements.md#c-01--tiêu-đề-uc014). Đặc tả đầy đủ: [UC014-inventory-report.md](UC014-inventory-report.md).

### UC015 — Thống kê doanh số theo sản phẩm

- **Chức năng (danh mục):** Lọc kỳ thống kê; tổng hợp số lượng bán, tiền hàng theo dòng sản phẩm; đối chiếu đổi/trả.
- **Ghi chú (danh mục):** Quản lý; HoaDon, ChiTietHoaDon, DongSanPham; chỉ lấy hóa đơn đã thanh toán.
- **Định vị:** Ch.3 §IV, `### UC015 – Thống kê doanh số theo sản phẩm`. Đặc tả đầy đủ: [UC015-sales-by-product.md](UC015-sales-by-product.md).

### UC016 — Thống kê doanh số theo nhân viên

- **Chức năng (danh mục):** Lọc kỳ; nhóm hóa đơn theo người lập; tổng hợp số lượng giao dịch và giá trị bán, đối chiếu đổi/trả.
- **Ghi chú (danh mục):** Quản lý; NhanVien, HoaDon, ChiTietHoaDon; chỉ lấy hóa đơn đã thanh toán.
- **Định vị:** Ch.3 §IV, `### UC016 – Thống kê doanh số theo nhân viên`. Đặc tả đầy đủ: [UC016-sales-by-employee.md](UC016-sales-by-employee.md).

### UC017 — Thống kê doanh thu

- **Chức năng (danh mục):** Tổng hợp hóa đơn đã thanh toán, tiền hoàn trả và chênh lệch đổi đã hoàn tất; xuất báo cáo Excel.
- **Ghi chú (danh mục):** Quản lý; HoaDon, PhieuTra, PhieuDoi. Loại phiếu hủy/từ chối; tránh tính trùng tiền đổi/trả.
- **Định vị:** Ch.3 §IV, `### UC017 – Thống kê doanh thu`. Đặc tả đầy đủ: [UC017-revenue-statistics.md](UC017-revenue-statistics.md).

### UC018 — Phê duyệt yêu cầu đổi trả

- **Chức năng (danh mục):** Kiểm tra phiếu, hóa đơn và máy; ghi người duyệt, ngày duyệt, ý kiến; cập nhật trạng thái duyệt và xử lý.
- **Ghi chú (danh mục):** Quản lý; PhieuTra, PhieuDoi, TrangThaiDuyet, TrangThaiXuLyPhieu. Chỉ hoàn tất sau khi được duyệt.
- **Định vị:** Ch.3 §IV, `### UC018 – Phê duyệt yêu cầu đổi trả`. Đặc tả đầy đủ: [UC018-return-approval.md](UC018-return-approval.md).

### UC019 — Cập nhật giá bán và thuế suất

- **Chức năng (danh mục):** Cập nhật donGiaBanTruocThue và thue; dùng giá trị mới cho giao dịch phát sinh sau cập nhật.
- **Ghi chú (danh mục):** Quản lý; DongSanPham. Cần bổ sung giá/thuế chốt vào ChiTietHoaDon để bảo toàn lịch sử khi đổi giá.
- **Định vị:** Ch.3 §IV, `### UC019 – Cập nhật giá bán và thuế suất`. Đặc tả đầy đủ: [UC019-price-tax-update.md](UC019-price-tax-update.md).

## 3. Khác biệt tên gọi và tác nhân (đã kiểm chứng)

### 3.1. Tên gọi

| UC | Tên danh mục | Tên đặc tả | Trạng thái |
|---|---|---|---|
| UC007 | Quản lý khách hàng | Thêm khách hàng | Mâu thuẫn |
| UC008 | Quản lý sản phẩm | Thêm sản phẩm | Mâu thuẫn |
| UC009 | Quản lý dòng sản phẩm | Thêm dòng sản phẩm | Mâu thuẫn |
| UC010 | Quản lý danh mục sản phẩm | Thêm danh mục sản phẩm | Mâu thuẫn |
| UC011 | Quản lý sản phẩm lỗi | Thêm sản phẩm lỗi | Mâu thuẫn |
| UC012 | Quản lý khuyến mãi | Thêm khuyến mãi | Mâu thuẫn |
| UC013 | Quản lý nhân viên bán hàng | Thêm nhân viên bán hàng | Mâu thuẫn |
| UC014 | Báo cáo sản phẩm tồn kho hằng ngày | Báo cáo sản phẩm tồn kho lâu ngày | Mâu thuẫn (cần chốt tên chuẩn) |

Các UC còn lại (UC001–UC006, UC015–UC019) có tên danh mục trùng tên đặc tả.

### 3.2. Tác nhân

| UC | Tác nhân danh mục | Tác nhân đặc tả | Trạng thái |
|---|---|---|---|
| UC008 | Quản lý | Nhân viên bán hàng | Mâu thuẫn |
| UC009 | Quản lý | Nhân viên bán hàng | Mâu thuẫn |
| UC010 | Quản lý | Nhân viên bán hàng | Mâu thuẫn |
| UC011 | Quản lý | Nhân viên bán hàng | Mâu thuẫn |

Các UC còn lại nhất quán (UC001–UC007: “Cả hai tác nhân”/“Bán hàng” ↔ Nhân viên quản lý/Nhân viên bán hàng; UC012–UC019: “Quản lý” ↔ Nhân viên quản lý). Xem thêm [C-03](../requirements/scope-and-requirements.md#c-03--tác-nhân-uc008uc011-không-thống-nhất).

### 3.3. Biến thể tiêu đề bảng đặc tả trong nguồn

Tiêu đề ô đầu của bảng đặc tả Ch.3 §IV **không đồng nhất** giữa các UC. Ví dụ: UC001 ghi “Use case: UC001 – Đăng nhập” (có mã), trong khi UC002 chỉ ghi “Use case: Đăng xuất” (không kèm mã; mã nằm ở tiêu đề mục `### UC002 – Đăng xuất`). Tài liệu này **giữ nguyên tiêu đề nguồn theo từng UC**, không chuẩn hóa hay viết lại; xem trường “Tiêu đề bảng (nguyên văn)” trong từng tệp UC.

## 4. Thực thể được nêu tường minh theo use case

Các tên dưới đây lấy nguyên văn từ cột “Ghi chú” của danh mục; là ánh xạ tường minh trong nguồn.

| UC | Thực thể và ghi chú |
|---|---|
| UC001 | `NhanVien`, `LoaiQuyen` |
| UC002 | `NhanVien` (ghi chú: không xóa NhanVien) |
| UC003 | `HoaDon`, `ChiTietHoaDon`, `SanPham`; chỉ máy `SAN_SANG_BAN` được bán |
| UC004 | `DongSanPham`, `DanhMucSanPham`, `SanPham` |
| UC005 | `KhachHang`, `HoaDon`, `PhieuDoi`, `PhieuTra` |
| UC006 | `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi`; chờ phê duyệt |
| UC007 | `KhachHang`; giữ liên kết lịch sử hóa đơn |
| UC008 | `SanPham`, `DongSanPham`; trạng thái máy dùng `TrangThaiSanPham` |
| UC009 | `DongSanPham.trangThaiKinhDoanh`; `SanPham` đại diện từng máy có serial |
| UC010 | `DanhMucSanPham`, `DongSanPham`; không làm mất liên kết dữ liệu |
| UC011 | `SanPhamLoi`, `SanPham`, `TrangThaiXuLyLoi`; liên kết chi tiết đổi/trả khi phát sinh |
| UC012 | `KhuyenMai`, `DongSanPham`; chỉ áp dụng khi đang dùng và còn hiệu lực |
| UC013 | `NhanVien`, `LoaiQuyen`, `trangThaiHoatDong`; không xóa lịch sử giao dịch |
| UC014 | `DongSanPham`, `SanPham`, `ngayNhapKho`, `TrangThaiSanPham` |
| UC015 | `HoaDon`, `ChiTietHoaDon`, `DongSanPham`; chỉ lấy hóa đơn đã thanh toán |
| UC016 | `NhanVien`, `HoaDon`, `ChiTietHoaDon`; chỉ lấy hóa đơn đã thanh toán |
| UC017 | `HoaDon`, `PhieuTra`, `PhieuDoi`; loại phiếu hủy/từ chối, tránh tính trùng tiền đổi/trả |
| UC018 | `PhieuTra`, `PhieuDoi`, `TrangThaiDuyet`, `TrangThaiXuLyPhieu`; chỉ hoàn tất sau khi được duyệt |
| UC019 | `DongSanPham`; ghi chú “Cần bổ sung giá/thuế chốt vào ChiTietHoaDon để bảo toàn lịch sử khi đổi giá” |

> **Lưu ý:** ghi chú UC019 nêu “cần bổ sung” trong khi thiết kế Ch.4 đã có các trường tiền/thuế trên `ChiTietHoaDon`. Đây là ghi chú của nguồn, chưa được xác nhận là còn hiệu lực hay đã lỗi thời.

## 5. Liên kết

- [README bộ tài liệu](../README.md)
- [Phạm vi và yêu cầu](../requirements/scope-and-requirements.md)
- [Truy vết yêu cầu – use case – thực thể](../traceability.md)
- [Quy tắc nghiệp vụ liên tính năng](../business-rules/rules-and-exceptions.md)
