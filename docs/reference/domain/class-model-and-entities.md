# Mô hình lớp và đặc tả thực thể (Class Diagram & Entities)

> **Trạng thái tài liệu:** Trích xuất từ báo cáo phân tích và thiết kế v12; trạng thái phê duyệt chưa được xác minh.
> **Nguồn:** `docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`.
> **Phạm vi trích xuất:** CHƯƠNG 4 › I. Thiết kế sơ đồ lớp (Class Diagram), gồm các mục 1–4.
> **Phân biệt:** Tài liệu này mô tả **thiết kế đã được ghi trong báo cáo**. Phần "Đối chiếu mã nguồn" ở cuối ghi nhận trạng thái **mã nguồn hiện tại**; mã nguồn **không** được dùng để suy diễn hay bổ sung dữ kiện còn thiếu trong DOCX.
> **Số trang:** Không nêu số trang vì chưa xác minh được; chỉ dẫn theo tiêu đề mục.
> **Hình vẽ:** Tài liệu này **không vẽ lại** sơ đồ. Bản sao byte-identical của sơ đồ lớp đã được **rà soát trực quan ngày 2026-10-19**; xem mục "Tài sản hình ảnh (bản sao byte-identical và nguồn DOCX)".
> **Bổ sung V1 (đã phê duyệt, 2026-10-19):** Mục 8 ghi bản đồ DDL V1 và các sửa đổi được phê duyệt cho `ChiTietHoaDon`, chi tiết đổi/trả và `SanPhamLoi`. Sơ đồ lớp gốc vẫn là nguồn tham chiếu; V1 bổ sung liên kết serial và snapshot, không tuyên bố các trường này đã có trong DOCX. Các bảng trích từ nguồn tại mục 3–4 và các xung đột tại mục 7 được giữ nguyên.

---

## 1. Vị trí trong báo cáo nguồn

| Mục | Tiêu đề trong DOCX | Nội dung trích xuất |
|---|---|---|
| CHƯƠNG 4 › I › 1 | Class Diagram | Chỉ có một hình; không có bảng dữ liệu văn bản. |
| CHƯƠNG 4 › I › 2 | Đặc tả Class Diagram | Tiêu đề tồn tại nhưng **không có nội dung** trong bản trích xuất. |
| CHƯƠNG 4 › I › 3 | Đặc tả chi tiết các thực thể | Quy ước chung + **14 thực thể**. |
| CHƯƠNG 4 › I › 4 | Đặc tả các kiểu liệt kê | **6 kiểu liệt kê**. |

Chỉ các mục 3 và 4 có nội dung văn bản có thể trích xuất. Mục 1 chỉ có hình; mục 2 là tiêu đề rỗng.

### Tài sản hình ảnh (bản sao byte-identical và nguồn DOCX)

- **CHƯƠNG 4 › I › 1. Class Diagram:** sơ đồ lớp được chèn dưới dạng một hình duy nhất (`word/media/image17.png`, kích thước hiển thị ≈ 6.10 in × 4.57 in trong bản DOCX). Bản sao byte-identical: [`../assets/diagrams/image17.png`](../assets/diagrams/image17.png).
- **Hình ảnh gốc vẫn nằm trong file DOCX nguồn**; bản sao byte-identical đặt trong `docs/reference/assets/diagrams/`.
- **Toàn bộ 37 ảnh sao đã được rà soát trực quan ngày 2026-10-19** (bao gồm sơ đồ lớp `image17`, EER `image6`, sơ đồ trên HQT CSDL `image8`; các bản sao byte-identical dẫn trên). Quan sát cho `ChiTietHoaDon`: sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai` và **không thấy trường serial/snapshot nào được vẽ trực tiếp**; EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai` và **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**. Đây là **xung đột thiết kế nguồn được giữ làm bằng chứng lịch sử**; V1 đã quyết định mô hình theo amendment được phê duyệt tại mục 8.2. Không suy đoán thêm cột/quan hệ ngoài phần thấy được.
- Không tái tạo hoặc vẽ lại sơ đồ trong tài liệu này.

---

## 2. Quy ước chung (CHƯƠNG 4 › I › 3 › Quy ước chung)

| Nội dung | Quy ước |
|---|---|
| Thuộc tính dẫn xuất | Thuộc tính có dấu `/` được tính từ dữ liệu khác. Chỉ cung cấp getter, không cung cấp setter để nhập trực tiếp kết quả. |
| Mã tự sinh | Sử dụng cấu trúc `TIỀN_TỐ-YYYY-NNNNNN`. `YYYY` là năm gồm 4 chữ số; `NNNNNN` là số thứ tự gồm 6 chữ số, bắt đầu từ `000001` trong từng năm và từng loại thực thể. |
| Nguyên tắc cấp mã | Mã được cấp khi lưu mới thành công, không trùng, không tái sử dụng và không thay đổi sau khi đã cấp. Việc cấp mã không phụ thuộc nhân viên đang thao tác. Số thứ tự có thể bị khuyết. |
| Thuộc tính tham chiếu | Tham chiếu đến đối tượng đã tồn tại. Ví dụ, `nhanVienLap` sử dụng nhân viên có sẵn, không tạo mã nhân viên mới khi lập hóa đơn. |
| Tiền tệ | Các số tiền sử dụng đơn vị đồng Việt Nam. Giữ nguyên kiểu dữ liệu trong sơ đồ. Các phép tính cần áp dụng cùng một quy tắc làm tròn; tổng chứng từ được cộng từ các khoản tiền đã làm tròn ở chi tiết. |
| Thuế và tỷ lệ giảm | Trong các công thức bên dưới, quy ước `thue = 10` nghĩa là thuế suất 10%; `tyLeGiam = 15` nghĩa là giảm 15%. Khi tính phải chia cho 100. |
| Dữ liệu giao dịch lịch sử | Giá bán, thuế suất và khuyến mãi dùng để tính hóa đơn, đổi hoặc trả hàng phải là dữ liệu tại thời điểm giao dịch. Không tính lại giao dịch cũ bằng giá, thuế hoặc khuyến mãi hiện tại. |
| Ngày tính tồn kho | `D` là ngày dùng để tính hoặc xem báo cáo. Khi xem hiện tại, `D` là ngày hiện tại. Số ngày được tính theo chênh lệch ngày lịch. |
| Constructor | Sơ đồ chỉ thể hiện constructor không tham số. Constructor có tham số trong phần đặc tả là **đề xuất bổ sung** để khởi tạo đối tượng hợp lệ. |
| Getter và setter | Getter đọc giá trị. Setter chỉ cập nhật thuộc tính được phép thay đổi và phải kiểm tra ràng buộc. Không cung cấp setter thông thường cho mã tự sinh, số serial và thuộc tính dẫn xuất. |
| Phương thức `toString()` | Trả về thông tin tóm tắt. Đối tượng liên quan chỉ hiển thị mã hoặc tên cần thiết, tránh gọi lặp qua toàn bộ quan hệ. |

---

## 3. Mười bốn thực thể

### 3.1. Thực thể NhanVien

**Mô tả:** Lưu thông tin nhân viên, tài khoản đăng nhập và quyền sử dụng hệ thống.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maNV` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `NVQUANLY-YYYY-NNN` hoặc `NVBANHANG-YYYY-NNN`. Tiền tố thể hiện vai trò khi tạo hồ sơ; `YYYY` là năm vào làm; `NNN` là số thứ tự nhân viên trong năm, dùng chung cho cả hai vai trò. Ví dụ: `NVQUANLY-2024-001` là nhân viên quản lý có số thứ tự 1 trong năm vào làm 2024. Khi đổi quyền, giữ nguyên mã; quyền hiện tại được xác định bằng `loaiQuyen`. |
| 1.2 | `hoTen` | String | Không rỗng sau khi loại bỏ khoảng trắng đầu và cuối. | Họ tên đầy đủ của nhân viên, có hỗ trợ tiếng Việt. |
| 1.3 | `soDienThoai` | String | Không rỗng; đúng định dạng số điện thoại được hệ thống chấp nhận. | Lưu dạng chuỗi để giữ số 0 ở đầu hoặc mã quốc gia. |
| 1.4 | `email` | String | Nếu có nhập thì phải đúng định dạng email. | Địa chỉ email liên hệ của nhân viên. |
| 1.5 | `ngayVaoLam` | LocalDate | Không rỗng. | Ngày bắt đầu làm việc; dùng xác định phần năm khi cấp mã nhân viên. |
| 1.6 | `tenDangNhap` | String | Không rỗng; duy nhất. | Tự sinh khi Quản lý thêm nhân viên theo dạng `NVQLYYYYYNNN` hoặc `NVBHYYYYNNN`. Tiền tố thể hiện vai trò khi tạo hồ sơ; `YYYY` là năm vào làm; `NNN` là số thứ tự nhân viên trong năm, dùng chung cho cả hai vai trò. Ví dụ: `NVQL2024001` là nhân viên quản lý có số thứ tự 1 trong năm vào làm 2024. Đảm bảo đồng bộ với maNV|
| 1.7 | `matKhau` | String | Không rỗng; tuân thủ quy định mật khẩu của hệ thống. | Lưu giá trị băm của mật khẩu; không hiển thị trong danh sách, nhật ký hoặc `toString()`. |
| 1.8 | `loaiQuyen` | LoaiQuyen | Không rỗng; thuộc tập giá trị `LoaiQuyen`. | Xác định quyền hiện tại của nhân viên quản lý hoặc nhân viên bán hàng. |
| 1.9 | `trangThaiHoatDong` | boolean | Chỉ nhận `true` hoặc `false`. | `true`: tài khoản được hoạt động; `false`: tài khoản bị khóa hoặc nhân viên đã nghỉ việc. Hồ sơ vẫn được giữ để tra cứu giao dịch cũ. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | Bao gồm `getMaNV()`, `getHoTen()`, `getSoDienThoai()`, `getEmail()`, `getNgayVaoLam()`, `getTenDangNhap()`, `getLoaiQuyen()` và `isTrangThaiHoatDong()`. Không cung cấp chức năng đọc mật khẩu gốc. |
| 2.2 | Setter cho các thuộc tính được cập nhật | void | Kiểm tra dữ liệu và quyền thao tác. | Không có setter thông thường cho `maNV`. Thay đổi mật khẩu phải thông qua chức năng đặt hoặc đổi mật khẩu, không nhận dữ liệu băm tùy ý từ giao diện. |
| 3 | Viết các constructor | | | |
| 3.1 | `NhanVien()` | --- | Constructor không tham số. | Khởi tạo đối tượng để nhập dữ liệu. |
| 3.2 | `NhanVien(...)` | --- | Các trường bắt buộc phải hợp lệ. | Nhận thông tin nhân viên và tài khoản; không yêu cầu nhập `maNV`. Mã được cấp khi lưu mới thành công. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không chứa mật khẩu. | Trả về mã nhân viên, họ tên, quyền và trạng thái hoạt động. |

### 3.2. Thực thể KhachHang

**Mô tả:** Lưu thông tin khách hàng để lập hóa đơn, tra cứu lịch sử mua hàng và xử lý đổi trả.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maKH` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `KH-YYYY-NNNNNN`. `KH` là khách hàng; `YYYY` là năm tạo hồ sơ; `NNNNNN` là số thứ tự hồ sơ trong năm. Ví dụ: `KH-2026-000001`. Không ghép số điện thoại vào mã để mã không thay đổi khi khách đổi số liên hệ. |
| 1.2 | `hoTen` | String | Không rỗng sau khi loại bỏ khoảng trắng đầu và cuối. | Họ tên khách hàng. |
| 1.3 | `soDienThoai` | String | Không rỗng; đúng định dạng được hệ thống chấp nhận. | Dùng tìm kiếm và liên hệ. Kiểm tra hồ sơ hiện có trước khi thêm để hạn chế tạo trùng khách hàng. |
| 1.4 | `diaChi` | String | Có thể để trống nếu nghiệp vụ không yêu cầu. | Địa chỉ liên hệ của khách hàng. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | `getMaKH()`, `getHoTen()`, `getSoDienThoai()`, `getDiaChi()`. |
| 2.2 | Setter cho thông tin khách hàng | void | Dữ liệu phải hợp lệ. | Cho phép cập nhật họ tên, số điện thoại và địa chỉ; không đổi `maKH`. |
| 3 | Viết các constructor | | | |
| 3.1 | `KhachHang()` | --- | Constructor không tham số. | Khởi tạo hồ sơ khách hàng chưa hoàn chỉnh. |
| 3.2 | `KhachHang(hoTen, soDienThoai, diaChi)` | --- | Kiểm tra các trường bắt buộc. | Không nhận mã khách hàng tự nhập. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Thông tin ngắn gọn. | Trả về mã khách hàng, họ tên và số điện thoại. |

### 3.3. Thực thể DanhMucSanPham

**Mô tả:** Phân loại các dòng sản phẩm trong hệ thống.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maDanhMuc` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `DM-YYYY-NNNNNN`. `DM` là danh mục; `YYYY` là năm tạo; `NNNNNN` là số thứ tự danh mục trong năm. Ví dụ: `DM-2026-000001`. |
| 1.2 | `tenDanhMuc` | String | Không rỗng; không trùng tên danh mục sau khi chuẩn hóa khoảng trắng và cách so sánh chữ hoa, chữ thường. | Ví dụ: Tivi, Tủ lạnh, Máy giặt. |
| 1.3 | `moTa` | String | Có thể để trống. | Mô tả phạm vi hoặc đặc điểm của danh mục. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | `getMaDanhMuc()`, `getTenDanhMuc()`, `getMoTa()`. |
| 2.2 | Setter cho tên và mô tả | void | Tên phải hợp lệ và không trùng. | Không đổi mã danh mục. Không xóa danh mục đang được dòng sản phẩm tham chiếu. |
| 3 | Viết các constructor | | | |
| 3.1 | `DanhMucSanPham()` | --- | Constructor không tham số. | Khởi tạo đối tượng danh mục. |
| 3.2 | `DanhMucSanPham(tenDanhMuc, moTa)` | --- | Tên danh mục hợp lệ. | Mã được hệ thống cấp khi lưu. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không liệt kê toàn bộ dòng sản phẩm. | Trả về mã và tên danh mục. |

### 3.4. Thực thể NhaCungCap

**Mô tả:** Lưu thông tin nhà cung cấp của từng sản phẩm theo số serial.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maNCC` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `NCC-YYYY-NNNNNN`. `NCC` là nhà cung cấp; `YYYY` là năm tạo hồ sơ; `NNNNNN` là số thứ tự nhà cung cấp trong năm. Ví dụ: `NCC-2026-000001`. |
| 1.2 | `tenNCC` | String | Không rỗng sau khi loại bỏ khoảng trắng đầu và cuối. | Tên nhà cung cấp. Không dùng tên làm mã vì tên có thể thay đổi hoặc trùng giữa các đơn vị. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | `getMaNCC()`, `getTenNCC()`. |
| 2.2 | `setTenNCC(tenNCC)` | void | Tên không rỗng. | Cho phép sửa tên; không thay đổi mã nhà cung cấp. |
| 3 | Viết các constructor | | | |
| 3.1 | `NhaCungCap()` | --- | Constructor không tham số. | Khởi tạo đối tượng nhà cung cấp. |
| 3.2 | `NhaCungCap(tenNCC)` | --- | Tên hợp lệ. | Mã được tự sinh khi lưu hồ sơ thành công. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không liệt kê toàn bộ sản phẩm đã cung cấp. | Trả về mã và tên nhà cung cấp. |

### 3.5. Thực thể DongSanPham

**Mô tả:** Lưu thông tin chung của một dòng sản phẩm. Một dòng có thể gồm nhiều sản phẩm riêng lẻ được phân biệt bằng số serial.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maDongSP` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `DSP-YYYY-NNNNNN`. `DSP` là dòng sản phẩm; `YYYY` là năm tạo hồ sơ; `NNNNNN` là số thứ tự dòng sản phẩm trong năm. Ví dụ: `DSP-2026-000001`. |
| 1.2 | `tenDongSP` | String | Không rỗng. | Tên thương mại hoặc tên model của dòng sản phẩm. |
| 1.3 | `danhMucSanPham` | DanhMucSanPham | Không rỗng; tham chiếu danh mục tồn tại. | Mỗi dòng sản phẩm thuộc một danh mục. Không tự tạo danh mục khi gán tham chiếu. |
| 1.4 | `donViTinh` | String | Không rỗng. | Ví dụ: chiếc, cái, bộ. |
| 1.5 | `soLuongTon` | int | Lớn hơn hoặc bằng 0. | Số máy thuộc dòng đang sẵn sàng bán. Đối soát bằng số `SanPham` có cùng dòng và trạng thái `SAN_SANG_BAN`. Giữ là thuộc tính lưu trữ theo sơ đồ; hệ thống cập nhật khi nghiệp vụ hoàn tất, không nhập sửa tùy ý. |
| 1.6 | `thue` | double | Từ 0 đến 100. | Thuế suất hiện hành tính theo phần trăm. Ví dụ: `10` tương ứng 10%. Thuế suất đã dùng trong giao dịch cũ phải được giữ lại theo giao dịch. |
| 1.7 | `donGiaBanTruocThue` | double | Lớn hơn hoặc bằng 0. | Giá bán cơ sở trước thuế của dòng sản phẩm, trước khi điều chỉnh theo độ mới và khuyến mãi. |
| 1.8 | `trangThaiKinhDoanh` | boolean | Chỉ nhận `true` hoặc `false`. | `true`: đang kinh doanh; `false`: ngừng kinh doanh. Ngừng kinh doanh không làm mất dữ liệu lịch sử. |
| 1.9 | `ngayNhapKho` | LocalDate | Không lớn hơn ngày hiện tại đối với hàng đã nhập thực tế. | Quy ước là ngày nhập kho lần đầu của dòng sản phẩm. Không sử dụng ngày này thay cho ngày nhập ban đầu của từng máy khi lập báo cáo tồn kho theo serial. |
| 1.10 | `ngayCapNhapCuoi` | LocalDate | Không trước ngày tạo hoặc nhập kho ban đầu. | Ngày cập nhật thông tin dòng sản phẩm gần nhất. Giữ cách đặt tên như trong sơ đồ. |
| 1.11 | `/soNgayTonKho` | int | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức đề xuất:** `soNgayTonKho = max(soNgayTonKho của các máy SAN_SANG_BAN thuộc dòng)`. Nếu không có máy sẵn sàng bán thì bằng 0. Đây là tuổi tồn kho của máy sẵn sàng bán lâu nhất, không phải tổng số ngày tồn của các máy. Sơ đồ chưa quy định cách tổng hợp nên cần thống nhất quy ước này. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Thuộc tính dẫn xuất được tính khi đọc. | Bao gồm `getSoNgayTonKho()` theo công thức trên. |
| 2.2 | Setter cho thông tin được cập nhật | void | Kiểm tra danh mục, giá, thuế và quyền thao tác. | Không có setter cho mã hoặc thuộc tính dẫn xuất. `soLuongTon` được cập nhật thông qua nghiệp vụ; giá và thuế chỉ được thay đổi bởi nhân viên có quyền. |
| 3 | Viết các constructor | | | |
| 3.1 | `DongSanPham()` | --- | Constructor không tham số. | Khởi tạo đối tượng dòng sản phẩm. |
| 3.2 | `DongSanPham(...)` | --- | Tên, danh mục, đơn vị tính, giá và thuế hợp lệ. | Không nhận mã tự sinh hoặc số ngày tồn kho. Số lượng tồn ban đầu phải khớp dữ liệu máy được tiếp nhận. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không liệt kê toàn bộ máy và khuyến mãi. | Trả về mã dòng, tên dòng, tên danh mục, giá cơ sở và số lượng tồn. |

### 3.6. Thực thể SanPham

**Mô tả:** Đại diện cho từng máy hoặc sản phẩm vật lý, được nhận diện bằng số serial và gắn với một nhà cung cấp.

**Quy ước đề xuất cho hai thuộc tính dẫn xuất mới:**

- `doMoi` được biểu diễn theo thang từ 0 đến 100; 100 là mức mới tối đa.
- `kNgay`, `kSua`, `kDoiTra` lần lượt là mức giảm độ mới do mỗi ngày tồn kho, mỗi lần sửa và mỗi lần đổi trả; đơn vị là **điểm phần trăm**.
- Các hệ số phải được cửa hàng phê duyệt. Sơ đồ chưa cung cấp giá trị, vì vậy chưa ấn định con số cụ thể.
- `giaBan` là giá trước thuế và trước khuyến mãi. `giaNhap` phục vụ theo dõi giá vốn, không tự động thay thế giá bán cơ sở.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `soSerial` | String | Không rỗng; duy nhất trong hệ thống; không thay đổi sau khi ghi nhận đúng. | Số serial do nhà sản xuất cung cấp, được nhập hoặc quét khi tiếp nhận máy. **Không tự sinh** theo cấu trúc mã nội bộ và không suy đoán ý nghĩa các ký tự của serial. |
| 1.2 | `nhaCungCap` | NhaCungCap | Không rỗng; tham chiếu nhà cung cấp tồn tại. | Nhà cung cấp đã cung ứng máy này. Mỗi máy thuộc một nhà cung cấp theo sơ đồ. |
| 1.3 | `dongSanPham` | DongSanPham | Không rỗng; tham chiếu dòng sản phẩm tồn tại. | Xác định thông tin chung, danh mục, thuế và giá bán cơ sở của máy. |
| 1.4 | `giaNhap` | double | Lớn hơn hoặc bằng 0. | Giá nhập của riêng máy, phục vụ theo dõi giá vốn. Cần sử dụng cùng quy ước về thuế khi so sánh với giá bán trước thuế. |
| 1.5 | `ngayNhapKhoBanDau` | LocalDate | Không rỗng; không lớn hơn ngày hiện tại đối với máy đã nhập thực tế. | Ngày máy được nhập vào cửa hàng lần đầu. Không đặt lại ngày này khi khách trả hàng hoặc máy quay về từ bảo hành. |
| 1.6 | `trangThai` | TrangThaiSanPham | Không rỗng; thuộc tập trạng thái được định nghĩa. | Chỉ máy ở trạng thái `SAN_SANG_BAN` và thuộc dòng đang kinh doanh mới được chọn bán. |
| 1.7 | `soLanDoiTra` | int | Lớn hơn hoặc bằng 0. | Số lần máy thực sự được nhận lại qua nghiệp vụ đổi hoặc trả đã hoàn tất. Không tăng khi chỉ tạo phiếu nháp hoặc khi phiếu bị hủy. |
| 1.8 | `soLanSua` | int | Lớn hơn hoặc bằng 0. | Số lần sửa chữa đã hoàn tất của máy. Không tăng chỉ vì tạo hồ sơ lỗi hoặc gửi bảo hành. |
| 1.9 | `/soNgayTonKho` | long | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `soNgayTonKho = max(0, số ngày từ ngayNhapKhoBanDau đến D)`. Chỉ tiêu này đo số ngày kể từ lần nhập đầu, không trừ thời gian máy đã bán hoặc gửi bảo hành vì sơ đồ chưa có đầy đủ các khoảng thời gian đó. Báo cáo tồn kho chỉ chọn những máy thuộc phạm vi tồn kho cần báo cáo. |
| 1.10 | `/doMoi` | double | Từ 0 đến 100; không nhập trực tiếp. | **Công thức đề xuất:** `doMoi = max(0, min(100, 100 − soNgayTonKho × kNgay − soLanSua × kSua − soLanDoiTra × kDoiTra))`. Các hệ số không âm và do cửa hàng phê duyệt. Ví dụ, `kSua = 5` nghĩa là mỗi lần sửa làm giảm 5 điểm độ mới, không phải giảm 5% của độ mới còn lại. |
| 1.11 | `/giaBan` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức đề xuất:** `giaBan = dongSanPham.donGiaBanTruocThue × doMoi / 100`. Đây là giá trước thuế, trước khuyến mãi. Ví dụ, giá cơ sở 10.000.000 đồng và độ mới 90 thì giá bán là 9.000.000 đồng. Công thức chưa bao gồm quy tắc giá tối thiểu theo giá nhập vì sơ đồ chưa quy định chính sách đó. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Tính thuộc tính dẫn xuất từ dữ liệu hợp lệ. | Bao gồm `getSoNgayTonKho()`, `getDoMoi()` và `getGiaBan()`. |
| 2.2 | Setter cho thông tin được cập nhật | void | Tuân thủ trạng thái và quyền thao tác. | Không có setter thông thường cho serial hoặc ba thuộc tính dẫn xuất. Số lần sửa và đổi trả được cập nhật bởi nghiệp vụ hoàn tất; không sửa tùy ý trên giao diện. |
| 3 | Viết các constructor | | | |
| 3.1 | `SanPham()` | --- | Constructor không tham số. | Khởi tạo đối tượng máy. |
| 3.2 | `SanPham(soSerial, nhaCungCap, dongSanPham, giaNhap, ngayNhapKhoBanDau, trangThai)` | --- | Serial chưa tồn tại; các tham chiếu và dữ liệu nhập kho hợp lệ. | Khi tiếp nhận máy mới chưa có lịch sử, số lần sửa và đổi trả bằng 0. Không nhận giá trị trực tiếp cho các thuộc tính dẫn xuất. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không truy xuất lặp toàn bộ đối tượng liên quan. | Trả về serial, mã dòng sản phẩm, mã nhà cung cấp, trạng thái, độ mới và giá bán hiện tại. |

### 3.7. Thực thể KhuyenMai

**Mô tả:** Lưu chương trình giảm giá áp dụng cho dòng sản phẩm trong một khoảng thời gian.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maKM` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `KM-YYYY-NNNNNN`. `KM` là khuyến mãi; `YYYY` là năm tạo chương trình; `NNNNNN` là số thứ tự chương trình trong năm. Ví dụ: `KM-2026-000001`. Năm tạo có thể khác năm bắt đầu áp dụng. |
| 1.2 | `tenKM` | String | Không rỗng. | Tên chương trình khuyến mãi. |
| 1.3 | `ngayBatDau` | LocalDate | Không rỗng; không sau `ngayKetThuc`. | Ngày bắt đầu có hiệu lực. |
| 1.4 | `ngayKetThuc` | LocalDate | Không rỗng; không trước `ngayBatDau`. | Ngày cuối cùng có hiệu lực, tính cả ngày này. |
| 1.5 | `tyLeGiam` | BigDecimal | Từ 0 đến 100. | Tỷ lệ giảm theo phần trăm. Ví dụ: `15` nghĩa là giảm 15%. Với một chương trình giảm phần trăm, tiền giảm bằng giá trị trước khuyến mãi nhân `tyLeGiam / 100`. |
| 1.6 | `moTa` | String | Có thể để trống. | Nội dung hoặc điều kiện áp dụng chương trình. |
| 1.7 | `dangApDung` | boolean | Chỉ nhận `true` hoặc `false`. | Cờ cho phép sử dụng chương trình. Chương trình thực sự có hiệu lực khi cờ bằng `true`, ngày giao dịch nằm trong thời hạn và dòng sản phẩm thuộc phạm vi áp dụng. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | Đọc mã, tên, thời gian, tỷ lệ giảm, mô tả và cờ áp dụng. |
| 2.2 | Setter cho thông tin chương trình | void | Kiểm tra thời gian, tỷ lệ giảm và quyền quản lý. | Không đổi mã. Việc cập nhật không được làm thay đổi khuyến mãi đã ghi nhận trên hóa đơn cũ. Nếu có nhiều chương trình cùng hiệu lực, phải áp dụng quy tắc kết hợp đã được phê duyệt. |
| 3 | Viết các constructor | | | |
| 3.1 | `KhuyenMai()` | --- | Constructor không tham số. | Khởi tạo chương trình khuyến mãi. |
| 3.2 | `KhuyenMai(tenKM, ngayBatDau, ngayKetThuc, tyLeGiam, moTa, dangApDung)` | --- | Khoảng thời gian và tỷ lệ giảm hợp lệ. | Mã chương trình được hệ thống cấp khi lưu mới thành công. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Thông tin ngắn gọn. | Trả về mã, tên chương trình, tỷ lệ giảm, thời hạn và cờ áp dụng. |

### 3.8. Thực thể HoaDon

**Mô tả:** Ghi nhận giao dịch bán hàng của một nhân viên cho một khách hàng.

**Quy ước tính tiền:** Với mỗi chi tiết hóa đơn `i`, gọi `rᵢ` là thuế suất tại thời điểm bán và `Sᵢ` là tiền sau khuyến mãi, trước thuế. Thuế của chi tiết bằng `Sᵢ × rᵢ / 100`.

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maHD` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `HD-YYYY-NNNNNN`. `HD` là hóa đơn; `YYYY` là năm của `ngayLap`; `NNNNNN` là số thứ tự hóa đơn trong năm. Ví dụ: `HD-2026-000001`. Không chứa mã nhân viên hoặc khách hàng để mã độc lập với các đối tượng liên quan. |
| 1.2 | `ngayLap` | LocalDate | Không rỗng. | Ngày lập hóa đơn do hệ thống ghi nhận. |
| 1.3 | `nhanVienLap` | NhanVien | Không rỗng; tham chiếu nhân viên tồn tại. | Nhân viên lập giao dịch. Giữ nguyên tham chiếu lịch sử khi nhân viên đổi quyền hoặc nghỉ việc. |
| 1.4 | `khachHang` | KhachHang | Không rỗng theo quan hệ trong sơ đồ. | Khách hàng mua hàng, được chọn từ hồ sơ hiện có hoặc tạo hồ sơ trước khi hoàn tất hóa đơn. |
| 1.5 | `trangThaiThanhToan` | boolean | Chỉ nhận `true` hoặc `false`. | `false`: chưa thanh toán; `true`: đã thanh toán thành công. Hóa đơn đã thanh toán không được hủy bằng cách đổi lại cờ này. |
| 1.6 | `/tongTienTruocThue` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienTruocThue = Σ chiTietHoaDon.tienSauKhuyenMai`. Là tổng tiền hàng sau khuyến mãi, trước thuế. |
| 1.7 | `/tongTienThue` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienThue = Σ làmTròn(Sᵢ × rᵢ / 100)`. Tính riêng từng chi tiết vì các dòng sản phẩm có thể có thuế suất khác nhau. Dùng thuế suất đã ghi nhận khi bán. |
| 1.8 | `/tongTienThanhToan` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienThanhToan = tongTienTruocThue + tongTienThue`. Đây là giá trị hóa đơn, không phải số tiền khách đưa hoặc doanh thu sau điều chỉnh đổi trả. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Các tổng tiền được tính từ chi tiết thuộc hóa đơn. | Bao gồm ba getter cho tổng tiền; không tính lại hóa đơn đã chốt bằng dữ liệu giá hiện tại. |
| 2.2 | Setter cho thông tin được phép thay đổi | void | Chỉ sửa thông tin nghiệp vụ khi hóa đơn còn được phép chỉnh sửa. | Không có setter cho mã và tổng tiền. Trạng thái thanh toán chỉ được chuyển khi nghiệp vụ thanh toán thành công. |
| 3 | Viết các constructor | | | |
| 3.1 | `HoaDon()` | --- | Constructor không tham số. | Khởi tạo hóa đơn chưa hoàn chỉnh. |
| 3.2 | `HoaDon(nhanVienLap, khachHang, ngayLap)` | --- | Nhân viên và khách hàng tồn tại. | Khởi tạo hóa đơn chưa thanh toán. Khi thanh toán phải có ít nhất một chi tiết hợp lệ. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không gọi lặp qua toàn bộ chi tiết. | Trả về mã hóa đơn, ngày lập, mã nhân viên, mã khách hàng, tổng thanh toán và trạng thái thanh toán. |

### 3.9. Thực thể ChiTietHoaDon

> **Ghi chú xung đột thiết kế nguồn (G-11 / BR-C12):** Phần văn bản dưới đây mô tả `ChiTietHoaDon` với `sanPham: DongSanPham`; `soLuong` bằng 1 theo quan hệ một chi tiết–một máy hiện tại; `tienTruocKhuyenMai` và `tienSauKhuyenMai` là thuộc tính dẫn xuất. Bản văn bản **không hiển thị** trường giá/thuế/khuyến mãi đã chốt hay tham chiếu trực tiếp tới máy vật lý, trong khi `RB22` yêu cầu xác định **máy thực tế được bán** và `RB46` yêu cầu **lưu đơn giá, thuế suất và khuyến mãi tại thời điểm giao dịch**.
>
> **Bằng chứng trực quan ngày 2026-10-19** (các bản sao byte-identical của sơ đồ lớp và EER): sơ đồ lớp thể hiện `ChiTietHoaDon` với `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai`, **không thấy trường serial/snapshot nào được vẽ trực tiếp**; EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**. Đây là **xung đột thiết kế nguồn được giữ làm bằng chứng lịch sử** với `RB22`/`RB46`; phần trích DOCX không thêm cột/quan hệ ngoài phần thấy được. **V1 đã quyết định mô hình theo amendment được phê duyệt** (liên kết serial và snapshot tại §8.2), không suy đoán thêm quan hệ khác. Liên kết: [G-11](../traceability.md), [BR-C12](../business-rules/rules-and-exceptions.md), [RB22/RB46](../database/schema-and-integrity.md).

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `hoaDon` | HoaDon | Không rỗng; tham chiếu hóa đơn tồn tại. | Hóa đơn sở hữu chi tiết. Chi tiết không tồn tại độc lập ngoài hóa đơn. Không tự sinh mã hóa đơn tại đây. |
| 1.2 | `sanPham` | DongSanPham | Không rỗng; khớp dòng sản phẩm của máy thực tế được bán. | Giữ tên và kiểu như sơ đồ. Tham chiếu dòng sản phẩm không thay thế việc xác định serial của máy bán. |
| 1.3 | `soLuong` | int | Lớn hơn 0; bằng 1 theo quan hệ một chi tiết–một máy hiện tại. | Nếu muốn một dòng chứa nhiều serial thì phải cập nhật quan hệ trong sơ đồ trước. |
| 1.4 | `/tienTruocKhuyenMai` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tienTruocKhuyenMai = soLuong × đơnGiáMáyTrướcThuếTạiLúcBán`. Đơn giá được lấy từ `SanPham.giaBan` khi chốt giao dịch, chưa áp dụng khuyến mãi. Không dùng `giaNhap` để tính tiền khách mua. |
| 1.5 | `/tienSauKhuyenMai` | double | Từ 0 đến `tienTruocKhuyenMai`; không nhập trực tiếp. | **Công thức:** `tienSauKhuyenMai = tienTruocKhuyenMai − tiềnKhuyếnMãiThựcTế`. Nếu áp dụng một chương trình giảm phần trăm: `tiềnKhuyếnMãiThựcTế = làmTròn(tienTruocKhuyenMai × tyLeGiam / 100)`. Không mặc định cộng dồn nhiều chương trình nếu chưa có quy tắc. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Hai khoản tiền được tính theo dữ liệu giao dịch. | `getHoaDon()`, `getSanPham()`, `getSoLuong()`, `getTienTruocKhuyenMai()`, `getTienSauKhuyenMai()`. |
| 2.2 | Setter cho dữ liệu chi tiết được phép sửa | void | Chỉ áp dụng trước khi giao dịch được chốt. | Không có setter cho tiền dẫn xuất. Khi đổi máy phải kiểm tra lại serial, trạng thái, giá và khuyến mãi. |
| 3 | Viết các constructor | | | |
| 3.1 | `ChiTietHoaDon()` | --- | Constructor không tham số. | Khởi tạo chi tiết hóa đơn. |
| 3.2 | `ChiTietHoaDon(hoaDon, sanPham, soLuong)` | --- | Hóa đơn và dòng sản phẩm hợp lệ; máy bán phải được liên kết. | Không tạo mã chi tiết mới vì sơ đồ không có thuộc tính mã chi tiết. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không in toàn bộ hóa đơn cha. | Trả về mã hóa đơn, mã dòng sản phẩm, serial được liên kết, số lượng và tiền sau khuyến mãi. |

### 3.10. Thực thể PhieuTra

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maPhieuTra` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `PT-YYYY-NNNNNN`. `PT` là phiếu trả; `YYYY` là năm của `ngayTao`; `NNNNNN` là số thứ tự phiếu trả trong năm. Ví dụ: `PT-2026-000001`. |
| 1.2 | `nhanVienLap` | NhanVien | Không rỗng. | Nhân viên tiếp nhận và lập phiếu trả. |
| 1.3 | `hoaDon` | HoaDon | Không rỗng; hóa đơn đã thanh toán. | Hóa đơn gốc có hàng được trả. Mọi chi tiết trả trong phiếu phải thuộc hóa đơn này. |
| 1.4 | `ngayTao` | LocalDate | Không rỗng; không trước ngày lập hóa đơn gốc. | Ngày tạo phiếu trả. |
| 1.5 | `ngayHoanTat` | LocalDate | Có thể rỗng trước khi hoàn tất; không trước `ngayTao`. | Chỉ ghi khi nghiệp vụ trả hàng và hoàn tiền đã hoàn tất. |
| 1.6 | `trangThaiDuyet` | TrangThaiDuyet | Không rỗng. | Theo dõi yêu cầu và kết quả phê duyệt. Khoản hoàn từ 3.000.000 đồng trở lên phải được quản lý duyệt trước khi hoàn tất. |
| 1.7 | `trangThaiXuLyPhieu` | TrangThaiXuLyPhieu | Không rỗng. | Theo dõi tiến trình xử lý độc lập với trạng thái duyệt. Được duyệt chưa đồng nghĩa đã hoàn tiền. |
| 1.8 | `nhanVienDuyet` | NhanVien | Có thể rỗng nếu không cần hoặc chưa duyệt. | Khi có quyết định duyệt hoặc từ chối, phải tham chiếu nhân viên có quyền quản lý. |
| 1.9 | `ngayDuyet` | LocalDate | Có thể rỗng trước quyết định; không trước `ngayTao`. | Ngày quản lý đưa ra quyết định. |
| 1.10 | `lyDoDuyet` | String | Bắt buộc khi từ chối. | Nội dung giải thích quyết định duyệt hoặc từ chối. |
| 1.11 | `tienKhachDua` | double | Lớn hơn hoặc bằng 0. | Giữ thuộc tính theo sơ đồ. Nghiệp vụ trả hàng thông thường là cửa hàng hoàn tiền nên không tự cộng hoặc trừ trường này vào tiền hoàn. Cần làm rõ mục đích sử dụng trước khi triển khai. |
| 1.12 | `/tongGiaTriGoc` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongGiaTriGoc = Σ giáTrịĐãThanhToánGốcCủaMáyĐượcTrả`. Mỗi giá trị gốc bao gồm tiền sau khuyến mãi và thuế đã ghi nhận khi bán; chỉ tính phần hàng được trả trong phiếu này. |
| 1.13 | `/tongTienHoanTra` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienHoanTra = Σ chiTietPhieuTra.tienHoanTra`. Theo quy tắc hoàn đủ giá trị đã thanh toán của hàng đủ điều kiện, khoản này bằng tổng giá trị gốc được trả. Không tự đặt thêm phí hoặc tỷ lệ khấu trừ khi tài liệu chưa quy định. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Tổng tiền lấy từ các chi tiết thuộc phiếu. | Bao gồm `getTongGiaTriGoc()` và `getTongTienHoanTra()`. |
| 2.2 | Setter cho thông tin được phép cập nhật | void | Tuân thủ quyền và tiến trình xử lý. | Không có setter cho mã hoặc tổng tiền. Phiếu đã hoàn tất không được sửa hàng trả hoặc số tiền; các thay đổi trạng thái phải thông qua nghiệp vụ tương ứng. |
| 3 | Viết các constructor | | | |
| 3.1 | `PhieuTra()` | --- | Constructor không tham số. | Khởi tạo đối tượng phiếu trả. |
| 3.2 | `PhieuTra(nhanVienLap, hoaDon, ngayTao)` | --- | Hóa đơn và nhân viên hợp lệ. | Ban đầu ở trạng thái `NHAP`; ngày hoàn tất và thông tin người duyệt chưa có. Trạng thái duyệt được xác định theo giá trị và quy tắc phê duyệt. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không in toàn bộ hóa đơn hoặc danh sách chi tiết. | Trả về mã phiếu, mã hóa đơn gốc, ngày tạo, trạng thái duyệt, trạng thái xử lý và tổng tiền hoàn. |

### 3.11. Thực thể ChiTietPhieuTra

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `phieuTra` | PhieuTra | Không rỗng. | Phiếu trả sở hữu chi tiết. Không tạo mã phiếu riêng tại chi tiết. |
| 1.2 | `tinhTrangHangTra` | TinhTrangHangTra | Không rỗng; thuộc tập giá trị được định nghĩa. | Xác định hàng còn nguyên vẹn hay hàng lỗi; phục vụ xử lý trạng thái máy sau khi nhận lại. |
| 1.3 | `chiTietHoaDon` | ChiTietHoaDon | Không rỗng; thuộc hóa đơn gốc của phiếu trả. | Xác định máy và giá trị giao dịch gốc. Không được nhận trả trùng một máy đã được đổi hoặc trả hoàn tất trước đó. |
| 1.4 | `lyDoTra` | String | Không rỗng. | Lý do khách trả hàng hoặc kết quả tiếp nhận. |
| 1.5 | `/tienHoanTra` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức cho máy đủ điều kiện hoàn đủ:** `tienHoanTra = tiềnSauKhuyếnMãiGốcCủaMáy + tiềnThuếGốcCủaMáy`. Với một chi tiết hóa đơn tương ứng một máy, đây là số tiền khách đã thanh toán cho chi tiết đó. Không tính bằng giá hiện tại hoặc giá nhập. Hàng không đủ điều kiện phải bị từ chối xử lý, không tự mặc định hoàn 0 để hoàn tất phiếu. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Tiền hoàn được tính từ giao dịch gốc. | Bao gồm `getTienHoanTra()`. |
| 2.2 | Setter cho dữ liệu chi tiết được phép sửa | void | Chỉ sửa khi phiếu còn cho phép chỉnh sửa. | Không có setter cho `tienHoanTra`. Kiểm tra lại điều kiện trả khi thay đổi chi tiết hóa đơn được tham chiếu. |
| 3 | Viết các constructor | | | |
| 3.1 | `ChiTietPhieuTra()` | --- | Constructor không tham số. | Khởi tạo chi tiết trả hàng. |
| 3.2 | `ChiTietPhieuTra(phieuTra, tinhTrangHangTra, chiTietHoaDon, lyDoTra)` | --- | Các tham chiếu và điều kiện trả hợp lệ. | Không tự sinh mã chi tiết vì sơ đồ không khai báo thuộc tính mã. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không gọi lặp phiếu cha. | Trả về mã phiếu trả, mã hóa đơn, serial máy trả, tình trạng và tiền hoàn. |

### 3.12. Thực thể PhieuDoi

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maPhieuDoi` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `PD-YYYY-NNNNNN`. `PD` là phiếu đổi; `YYYY` là năm của `ngayTao`; `NNNNNN` là số thứ tự phiếu đổi trong năm. Ví dụ: `PD-2026-000001`. |
| 1.2 | `nhanVienLap` | NhanVien | Không rỗng. | Nhân viên lập và tiếp nhận yêu cầu đổi hàng. |
| 1.3 | `hoaDon` | HoaDon | Không rỗng; hóa đơn đã thanh toán. | Hóa đơn gốc của hàng khách mang đổi. |
| 1.4 | `ngayHoanTat` | LocalDate | Có thể rỗng trước khi hoàn tất; không trước `ngayTao`. | Ngày nhận hàng cũ, giao hàng mới và xử lý tiền chênh lệch thành công. |
| 1.5 | `ngayTao` | LocalDate | Không rỗng; không trước ngày lập hóa đơn gốc. | Ngày tạo phiếu đổi. |
| 1.6 | `trangThaiDuyet` | TrangThaiDuyet | Không rỗng. | Xác định phiếu không cần duyệt, chờ duyệt, đã duyệt hoặc bị từ chối. Nếu phát sinh khoản hoàn thuộc ngưỡng phải duyệt thì phải được duyệt trước khi hoàn tất. |
| 1.7 | `trangThaiXuLyPhieu` | TrangThaiXuLyPhieu | Không rỗng. | Trạng thái thực hiện nghiệp vụ, độc lập với trạng thái phê duyệt. |
| 1.8 | `tienKhachDuaThem` | double | Lớn hơn hoặc bằng 0. | Số tiền mặt khách thực tế đưa để thanh toán khoản cần thu thêm. Khi có khoản cần thu, số tiền khách đưa phải đủ; tiền thừa trả khách bằng `tienKhachDuaThem − tongTienThuThem`. |
| 1.9 | `nhanVienDuyet` | NhanVien | Có thể rỗng trước khi duyệt hoặc khi không cần duyệt. | Khi có quyết định, người duyệt phải có quyền quản lý. |
| 1.10 | `ngayDuyet` | LocalDate | Có thể rỗng trước quyết định; không trước `ngayTao`. | Ngày ghi nhận quyết định duyệt hoặc từ chối. |
| 1.11 | `lyDoDuyet` | String | Bắt buộc khi từ chối. | Giải thích quyết định của người duyệt. |
| 1.12 | `/tongGiaTriHangDoi` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Quy ước tên:** Là tổng giá trị gốc được công nhận của hàng khách mang đổi. **Công thức:** `tongGiaTriHangDoi = Σ giáTrịĐãThanhToánGốcCủaMáyMangĐổi`. Giá trị bao gồm thuế và đã trừ khuyến mãi tại lần bán gốc. |
| 1.13 | `/tongTienChenhLech` | double | Có thể dương, âm hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienChenhLech = Σ chiTietPhieuDoi.tienChenhLech = tổngGiáTrịMáyThayThếSauThuế − tongGiaTriHangDoi`. Phải cộng cả chênh lệch âm và dương. |
| 1.14 | `/tongTienThuThem` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tongTienThuThem = max(0, tongTienChenhLech)`. Nếu tổng chênh lệch âm, số tiền cửa hàng cần hoàn là `max(0, −tongTienChenhLech)`. Không tính tổng tiền thu bằng cách cộng riêng các chi tiết dương vì sẽ bỏ qua khoản được bù trừ từ chi tiết âm. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Tổng tiền được tính từ chi tiết hợp lệ. | Bao gồm getter cho tổng giá trị hàng đổi, tổng chênh lệch và tổng tiền thu thêm. |
| 2.2 | Setter cho thông tin được phép cập nhật | void | Tuân thủ quyền, trạng thái và điều kiện đổi. | Không có setter cho mã hoặc các tổng tiền. Không sửa nội dung phiếu đã hoàn tất. |
| 3 | Viết các constructor | | | |
| 3.1 | `PhieuDoi()` | --- | Constructor không tham số. | Khởi tạo đối tượng phiếu đổi. |
| 3.2 | `PhieuDoi(nhanVienLap, hoaDon, ngayTao)` | --- | Hóa đơn gốc và nhân viên hợp lệ. | Khởi tạo trạng thái xử lý `NHAP`; chưa có ngày hoàn tất và quyết định duyệt. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không liệt kê lặp toàn bộ chi tiết. | Trả về mã phiếu, mã hóa đơn gốc, trạng thái, tổng chênh lệch và tổng tiền thu thêm. |

### 3.13. Thực thể ChiTietPhieuDoi

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `phieuDoi` | PhieuDoi | Không rỗng. | Phiếu đổi sở hữu chi tiết. |
| 1.2 | `lyDoDoi` | String | Không rỗng. | Lý do đổi máy. |
| 1.3 | `chiTietHoaDon` | ChiTietHoaDon | Không rỗng; thuộc hóa đơn gốc của phiếu đổi. | Xác định máy khách mang đổi và số tiền đã thanh toán gốc. Không cho đổi trùng máy đã được xử lý hoàn tất trước đó. |
| 1.4 | `chiTietSanPhamDoi` | SanPham | Không rỗng; khác máy mang đổi; máy thay thế phải đủ điều kiện bán. | Máy được giao cho khách để thay thế. Không cho cùng một máy thay thế xuất hiện trong nhiều chi tiết của giao dịch. |
| 1.5 | `/tienChenhLech` | double | Có thể dương, âm hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tienChenhLech = giáTrịMáyThayThếSauThuế − giáTrịĐãThanhToánGốcCủaMáyMangĐổi`. Giá trị máy thay thế được xác định tại thời điểm chốt đổi, sau khuyến mãi hợp lệ và cộng thuế. Giá trị máy cũ lấy từ giao dịch gốc. |
| 1.6 | `/tienThuThem` | double | Lớn hơn hoặc bằng 0; không nhập trực tiếp. | **Công thức:** `tienThuThem = max(0, tienChenhLech)`. Đây là khoản dương ở riêng chi tiết để tham khảo. Số tiền thực thu của cả phiếu phải dùng `PhieuDoi.tongTienThuThem` sau khi bù trừ toàn bộ chi tiết. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Tiền chênh lệch sử dụng giá trị đã chốt của giao dịch. | Bao gồm `getTienChenhLech()` và `getTienThuThem()`. |
| 2.2 | Setter cho dữ liệu chi tiết được phép sửa | void | Chỉ sửa trước khi phiếu được chốt; kiểm tra lại hàng đổi và máy thay thế. | Không có setter cho hai khoản tiền dẫn xuất. |
| 3 | Viết các constructor | | | |
| 3.1 | `ChiTietPhieuDoi()` | --- | Constructor không tham số. | Khởi tạo chi tiết phiếu đổi. |
| 3.2 | `ChiTietPhieuDoi(phieuDoi, lyDoDoi, chiTietHoaDon, chiTietSanPhamDoi)` | --- | Các tham chiếu hợp lệ; không trùng máy. | Không tự sinh mã chi tiết vì sơ đồ không khai báo mã riêng. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không gọi lặp phiếu cha. | Trả về mã phiếu đổi, serial máy cũ, serial máy thay thế, tiền chênh lệch và tiền thu thêm của chi tiết. |

**Công thức giá trị máy thay thế:**

```
giáTrịMáyThayThếSauThuế = tiềnMáyThayThếSauKhuyếnMãiTrướcThuế + tiềnThuếMáyThayThế
```

Trong đó:

```
tiềnMáyThayThếSauKhuyếnMãiTrướcThuế = giáBanTạiLúcĐổi − tiềnKhuyếnMãiHợpLệTạiLúcĐổi
tiềnThuếMáyThayThế = làmTròn(tiềnMáyThayThếSauKhuyếnMãiTrướcThuế × thuếSuấtTạiLúcĐổi / 100)
```

Các giá trị đã chốt phải được lưu theo giao dịch đổi để không bị thay đổi khi cập nhật giá hoặc thuế sau này.

### 3.14. Thực thể SanPhamLoi

| STT | Công việc | Kiểu dữ liệu | Ràng buộc | Ghi chú |
|---|---|---|---|---|
| 1 | Khai báo thuộc tính | | | |
| 1.1 | `maLoi` | String | Không rỗng; duy nhất; không thay đổi sau khi cấp. | Tự sinh theo dạng `SPL-YYYY-NNNNNN`. `SPL` là hồ sơ sản phẩm lỗi; `YYYY` là năm của `ngayGhiNhan`; `NNNNNN` là số thứ tự hồ sơ lỗi trong năm. Ví dụ: `SPL-2026-000001`. Một máy có nhiều lần phát sinh lỗi sẽ có nhiều mã hồ sơ khác nhau. |
| 1.2 | `ghiChuXuLy` | String | Có thể để trống khi mới ghi nhận. | Nội dung kiểm tra, hướng xử lý hoặc kết quả xử lý. Khi chuyển trạng thái cần ghi chú đủ để tra cứu. |
| 1.3 | `moTaLoi` | String | Không rỗng. | Mô tả biểu hiện hoặc tình trạng lỗi được phát hiện. |
| 1.4 | `ngayGhiNhan` | LocalDate | Không rỗng; không lớn hơn ngày hiện tại khi ghi nhận thực tế. | Ngày tạo hồ sơ lỗi; dùng xác định năm trong mã tự sinh. |
| 1.5 | `tinhTrangXuLy` | TrangThaiXuLyLoi | Không rỗng; thuộc tập giá trị được định nghĩa. | Theo dõi chờ xử lý, đã gửi bảo hành, đã trả nhà cung cấp hoặc thanh lý. Phải thống nhất với trạng thái thực tế của máy. |
| 1.6 | `sanPham` | DongSanPham | Không rỗng theo thuộc tính đang vẽ; dòng sản phẩm phải tồn tại. | Kiểu hiện tại chỉ xác định dòng sản phẩm. Để xử lý lỗi theo serial, phải xác định máy qua quan hệ `SanPham`; cần thống nhất lại kiểu thuộc tính hoặc cách thể hiện quan hệ trước khi triển khai. |
| 1.7 | `chiTietPhieuDoi` | ChiTietPhieuDoi | (trống trong nguồn) | |
| 1.8 | `chiTietPhieuTra` | ChiTietPhieuTra | (trống trong nguồn) | |
| 1.9 | `ngayCapNhap` | LocalDate | Không trước `ngayGhiNhan`. | Ngày cập nhật hồ sơ lỗi gần nhất. Giữ cách đặt tên trong sơ đồ. |
| 2 | Viết các phương thức getter, setter | | | |
| 2.1 | Getter cho các thuộc tính | Theo kiểu thuộc tính | Không làm thay đổi dữ liệu. | Đọc mã lỗi, mô tả, ghi chú, ngày, trạng thái và các tham chiếu đã được thống nhất. |
| 2.2 | Setter cho thông tin xử lý | void | Kiểm tra quyền và chuyển trạng thái hợp lệ. | Không đổi mã lỗi. Cập nhật tình trạng xử lý phải đồng bộ với máy liên quan; không tăng số lần sửa chỉ vì chuyển sang trạng thái gửi bảo hành. |
| 3 | Viết các constructor | | | |
| 3.1 | `SanPhamLoi()` | --- | Constructor không tham số. | Khởi tạo hồ sơ lỗi. |
| 3.2 | `SanPhamLoi(...)` | --- | Phải xác định được máy thực tế, mô tả lỗi và ngày ghi nhận. | Khởi tạo tình trạng `CHO_XU_LY`. Tham số liên quan phiếu đổi trả chỉ hoàn thiện sau khi thống nhất kiểu `PhieuDoiTra`. |
| 4 | Viết phương thức `toString()` | | | |
| 4.1 | `toString()` | String | Không gọi lặp toàn bộ máy hoặc phiếu đổi trả. | Trả về mã lỗi, serial máy qua quan hệ đã xác định, mô tả, ngày ghi nhận và tình trạng xử lý. |

---

## 4. Các kiểu liệt kê (CHƯƠNG 4 › I › 4)

> **Ghi chú kiểm chứng:** Phần văn bản DOCX định nghĩa **6 kiểu liệt kê** (đánh số 15–20); tài liệu này ghi đủ 6 kiểu theo đúng nguồn.

### 4.1. Kiểu liệt kê LoaiQuyen

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `NHAN_VIEN_QUAN_LY` | Nhân viên quản lý. | Sử dụng các chức năng quản lý được phân quyền như nhân viên, khuyến mãi, giá, thuế, duyệt đổi trả và báo cáo. |
| 2 | `NHAN_VIEN_BAN_HANG` | Nhân viên bán hàng. | Sử dụng các chức năng tác nghiệp được phân quyền như bán hàng, tra cứu, khách hàng và tiếp nhận đổi trả. |

### 4.2. Kiểu liệt kê TrangThaiSanPham

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `SAN_SANG_BAN` | Máy đủ điều kiện bán. | Được tính vào số lượng sẵn sàng bán của dòng sản phẩm. |
| 2 | `DA_BAN` | Máy đã được bán thành công. | Không được chọn bán lại khi chưa có nghiệp vụ nhận lại hợp lệ. |
| 3 | `HANG_LOI` | Máy có lỗi. | Không được chọn bán như hàng sẵn sàng bán. |
| 4 | `DANG_BAO_HANH` | Máy đang được bảo hành. | Không thuộc số lượng sẵn sàng bán. |
| 5 | `DA_TRA_NHA_CUNG_CAP` | Máy đã trả về nhà cung cấp. | Không thuộc số lượng sẵn sàng bán. |
| 6 | `DA_THANH_LY` | Máy đã được thanh lý. | Không thuộc số lượng sẵn sàng bán thông thường. |

### 4.3. Kiểu liệt kê TrangThaiXuLyPhieu

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `NHAP` | Phiếu đang được soạn. | Có thể chỉnh sửa nội dung hợp lệ; chưa ghi nhận nghiệp vụ hoàn tất. |
| 2 | `CHO_XU_LY` | Phiếu đã được gửi để xử lý. | Nếu cần duyệt thì phải chờ kết quả duyệt trước khi hoàn tất. |
| 3 | `HOAN_TAT` | Nghiệp vụ đã hoàn thành. | Đã xử lý hàng và tiền tương ứng; không cho thực hiện lại cùng nghiệp vụ. |
| 4 | `HUY` | Phiếu bị hủy. | Không phát sinh tác động như một phiếu hoàn tất. |

### 4.4. Kiểu liệt kê TrangThaiDuyet

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `KHONG_CAN_DUYET` | Phiếu không thuộc trường hợp phải duyệt. | Vẫn phải kiểm tra đầy đủ điều kiện nghiệp vụ trước khi hoàn tất. |
| 2 | `CHO_DUYET` | Đang chờ quản lý quyết định. | Chưa được hoàn tất nghiệp vụ cần phê duyệt. |
| 3 | `DA_DUYET` | Quản lý đã chấp thuận. | Không đồng nghĩa tiền đã được hoàn hoặc máy đã được giao nhận. |
| 4 | `TU_CHOI` | Quản lý không chấp thuận. | Phải lưu lý do từ chối; không hoàn tất theo nội dung đã bị từ chối. |

### 4.5. Kiểu liệt kê TinhTrangHangTra

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `NGUYEN_VEN` | Hàng trả còn nguyên vẹn. | Chỉ chuyển sang sẵn sàng bán sau khi kiểm tra đủ điều kiện. |
| 2 | `HANG_LOI` | Hàng trả có lỗi. | Không cộng ngay vào số lượng sẵn sàng bán; ghi nhận hồ sơ lỗi khi phù hợp. |

### 4.6. Kiểu liệt kê TrangThaiXuLyLoi

| STT | Giá trị | Ý nghĩa | Ghi chú |
|---|---|---|---|
| 1 | `CHO_XU_LY` | Lỗi đã được ghi nhận và đang chờ xử lý. | Chưa xác nhận đã sửa xong hoặc đã trả nhà cung cấp. |
| 2 | `DA_GUI_BAO_HANH` | Máy đã được gửi bảo hành. | Trạng thái máy tương ứng là đang bảo hành khi máy vẫn ở tiến trình này. |
| 3 | `DA_TRA_NHA_CUNG_CAP` | Máy đã được trả nhà cung cấp. | Đồng bộ trạng thái máy và số lượng sẵn sàng bán. |
| 4 | `THANH_LY` | Hồ sơ lỗi được xử lý theo hướng thanh lý. | Đồng bộ với trạng thái máy đã thanh lý khi nghiệp vụ hoàn tất. |

---

## 5. Tổng hợp công thức quan trọng

| Phạm vi | Công thức |
|---|---|
| Tiền hàng chi tiết | `tienTruocKhuyenMai = soLuong × đơnGiáMáyTrướcThuếTạiLúcBán` |
| Giảm giá chi tiết | `tiềnKhuyếnMãiThựcTế = làmTròn(tienTruocKhuyenMai × tyLeGiam / 100)`; `tienSauKhuyenMai = tienTruocKhuyenMai − tiềnKhuyếnMãiThựcTế`; ràng buộc `0 ≤ tienSauKhuyenMai ≤ tienTruocKhuyenMai` |
| Tổng hóa đơn | `tongTienTruocThue = Σ tienSauKhuyenMai`; `tongTienThue = Σ làmTròn(Sᵢ × rᵢ / 100)`; `tongTienThanhToan = tongTienTruocThue + tongTienThue` |
| Tiền hoàn trả | `tienHoanTra = tiềnSauKhuyếnMãiGốcCủaMáy + tiềnThuếGốcCủaMáy`; `tongTienHoanTra = Σ chiTietPhieuTra.tienHoanTra` |
| Chênh lệch đổi | `tienChenhLech = giáTrịMáyThayThếSauThuế − giáTrịĐãThanhToánGốcCủaMáyMangĐổi`; `tongTienChenhLech = Σ tienChenhLech`; `tongTienThuThem = max(0, tongTienChenhLech)` |
| Giá trị máy thay thế | `giáTrịMáyThayThếSauThuế = tiềnMáyThayThếSauKhuyếnMãiTrướcThuế + tiềnThuếMáyThayThế` |
| Tồn kho theo máy | `soNgayTonKho = max(0, số ngày từ ngayNhapKhoBanDau đến D)` |
| Tồn kho theo dòng (đề xuất) | `soNgayTonKho = max(soNgayTonKho của các máy SAN_SANG_BAN thuộc dòng)`; bằng 0 nếu không có máy sẵn sàng bán |
| Độ mới (đề xuất) | `doMoi = max(0, min(100, 100 − soNgayTonKho × kNgay − soLanSua × kSua − soLanDoiTra × kDoiTra))` |
| Giá bán (đề xuất) | `giaBan = dongSanPham.donGiaBanTruocThue × doMoi / 100` |

---

## 6. Đối chiếu mã nguồn hiện tại (KHÔNG dùng để bổ sung dữ kiện DOCX)

> Phần này chỉ ghi nhận khoảng cách giữa **thiết kế đã ghi trong báo cáo** và **mã nguồn hiện tại**, phục vụ đánh giá. Không có dữ kiện nào của DOCX được suy diễn từ mã nguồn.
>
> **Ảnh chụp cập nhật ngày 2026-10-20.** Các mục 3–4 vẫn là **yêu cầu/thiết kế nguồn**; mục này chỉ mô tả **implementation hiện tại**. V1 là migration đã được phê duyệt, không phải nội dung sửa lại của DOCX.

Trạng thái mã nguồn tại ngày 2026-10-20 (nhánh hiện tại):

- `Main` khởi tạo FlatLaf, lấy `DataSource` dùng chung từ `ConnectDB`, đăng ký shutdown hook, gọi `DbMigration.migrate(dataSource)` trước khi mở giao diện; nếu migration ném `RuntimeException`, ứng dụng ghi log, đóng pool và **dừng khởi động**, không mở cửa sổ.
- Sau migration thành công, `Main` mở `MainFrame` trên EDT. `MainFrame` hiện là màn hình chào chung dùng FlatLaf/MigLayout; không còn trạng thái tải hoặc danh sách `Product`.
- `pom.xml` khai báo Flyway **13.10.2** (`flyway-core`, `flyway-database-postgresql`). `DbMigration` dùng `classpath:db/migration` và `DataSource` dùng chung, không tạo pool mới.
- Đã có migration [`V1__initial_schema.sql`](../../../src/main/resources/db/migration/V1__initial_schema.sql), tạo **14 bảng** DTAQ và liên kết `KhuyenMai.maDongSP` → `DongSanPham` (RB20).
- Các lớp demo `Product` và `ProductDao`/`ProductDaoImpl`/`MainController` đã bị xóa; không còn Java class trong các thư mục `entity`, `dao`, `dao.impl`, `controller` (các thư mục rỗng có thể còn trên đĩa).
- Chưa có Java entity/enum cho 14 thực thể và 6 kiểu liệt kê của thiết kế; các nhóm package hiện có class là `config`, `exception`, `ui`, `util`, cộng `Main`.
- Ghi chú implementation: Javadoc hiện tại của `DbMigration` vẫn còn câu “no migration scripts are bundled yet”; câu này đã lỗi thời so với V1 đang có. Đây là nhận xét mã nguồn, không phải yêu cầu thiết kế.

Kết luận: mã nguồn hiện tại **chưa** hiện thực hóa mô hình lớp Java của DOCX; phần đã hiện thực là khung ứng dụng, Flyway và schema V1. Mọi phát biểu thuộc tính/phương thức/ràng buộc trong mục 3–4 vẫn lấy từ DOCX.

---

## 7. Điểm xung đột và nội dung cần làm rõ

1. **`SanPhamLoi.sanPham` — xung đột nguồn (bằng chứng lịch sử; V1 đã quyết định):** DOCX ghi kiểu `DongSanPham` trong khi ghi chú yêu cầu xử lý lỗi theo serial cần xác định máy qua `SanPham`; nguồn nêu "cần thống nhất lại kiểu thuộc tính hoặc cách thể hiện quan hệ". Đây vẫn là **xung đột của DOCX, không sửa DOCX**. V1 dùng `soSerial` bắt buộc, FK tới `SanPham` và `maPhieuTra`/`maPhieuDoi` tùy chọn, kèm trigger kiểm tra chi tiết nguồn (xem §8.4).
2. **`PhieuTra.tienKhachDua`:** nguồn nêu "Cần làm rõ mục đích sử dụng trước khi triển khai".
3. **Các công thức `soNgayTonKho` (dòng), `doMoi`, `giaBan`:** được đánh dấu "đề xuất"/"chưa quy định", chưa phải quy tắc chính thức.
4. **Các hệ số `kNgay`, `kSua`, `kDoiTra`:** chưa có giá trị; phải được cửa hàng phê duyệt.
5. **Mục CHƯƠNG 4 › I › 2 "Đặc tả Class Diagram":** rỗng trong bản trích xuất.
6. **Sơ đồ lớp:** đã rà soát trực quan ngày 2026-10-19 qua bản sao byte-identical; tài liệu vẫn không vẽ lại sơ đồ.
7. **`ChiTietHoaDon` — xung đột thiết kế nguồn (bằng chứng lịch sử; V1 đã quyết định):** phần văn bản ghi `sanPham: DongSanPham`, `soLuong` = 1 và các khoản dẫn xuất, không hiển thị trường giá/thuế/khuyến mãi đã chốt hay tham chiếu trực tiếp tới máy vật lý; `RB22` cần xác định máy thực tế và `RB46` cần lưu giá/thuế/khuyến mãi tại thời điểm giao dịch. Bằng chứng trực quan ngày 2026-10-19: sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai` và **không thấy trường serial/snapshot nào được vẽ trực tiếp**; EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai` và **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**. Phần trích DOCX không thêm cột/quan hệ ngoài phần thấy được; **V1 đã quyết định mô hình theo amendment được phê duyệt** tại §8.2. Xem ghi chú tại [§3.9](#39-thực-thể-chitiethoadon) và sổ đăng ký [G-11](../traceability.md), [BR-C12](../business-rules/rules-and-exceptions.md).

---

## 8. Bản đồ V1 (đã phê duyệt)

> **Vị trí DDL:** `src/main/resources/db/migration/V1__initial_schema.sql`. Đây là **bản sửa đổi V1 đã được phê duyệt ngày 2026-10-19**, không phải nội dung gốc của DOCX.
>
> **Quan hệ với nguồn:** sơ đồ lớp gốc (`word/media/image17.png`, bản sao `../assets/diagrams/image17.png`) và EER gốc vẫn là **nguồn tham chiếu**. V1 bổ sung liên kết serial và snapshot giá/thuế/khuyến mãi theo quyết định đã duyệt; không tuyên bố DOCX đã có các trường này. Các mục 3–4 giữ nguyên nội dung trích từ nguồn; mục 7 giữ nguyên các xung đột nguồn.

### 8.1. Mười bốn bảng và khóa chính

| Bảng | Khóa chính | Ghi chú V1 |
|---|---|---|
| `NhanVien` | `maNV` | ID chuỗi do ứng dụng cấp; `tenDangNhap` duy nhất. |
| `KhachHang` | `maKH` | ID chuỗi do ứng dụng cấp. |
| `DanhMucSanPham` | `maDanhMuc` | Tên duy nhất sau chuẩn hóa khoảng trắng và chữ hoa/thường. |
| `NhaCungCap` | `maNCC` | ID chuỗi do ứng dụng cấp. |
| `DongSanPham` | `maDongSP` | FK tới `DanhMucSanPham`; giữ `soLuongTon`, `thue`, `donGiaBanTruocThue`. |
| `SanPham` | `soSerial` | FK tới `NhaCungCap`, `DongSanPham`; có UQ `(soSerial, maDongSP)` để làm đích cho FK ghép. |
| `KhuyenMai` | `maKM` | ID chuỗi do ứng dụng cấp; FK bắt buộc `maDongSP` → `DongSanPham` (RB20); khoảng ngày và tỷ lệ giảm có CHECK. |
| `HoaDon` | `maHD` | FK tới `NhanVien`, `KhachHang`. |
| `ChiTietHoaDon` | `(maHD, soSerial)` | Khóa ghép theo hóa đơn và máy thực tế; xem 8.2. |
| `PhieuTra` | `maPhieuTra` | FK tới `NhanVien`, `HoaDon`, nhân viên duyệt. |
| `ChiTietPhieuTra` | `(maPhieuTra, maHD, soSerial)` | FK ghép tới `ChiTietHoaDon(maHD, soSerial)`. |
| `PhieuDoi` | `maPhieuDoi` | FK tới `NhanVien`, `HoaDon`, nhân viên duyệt. |
| `ChiTietPhieuDoi` | `(maPhieuDoi, maHD, soSerial)` | FK ghép tới `ChiTietHoaDon`; máy thay thế tham chiếu `SanPham`. |
| `SanPhamLoi` | `maLoi` | `soSerial` bắt buộc; `maPhieuTra`/`maPhieuDoi` tùy chọn; xem 8.4. |

Tất cả ID là `VARCHAR` do ứng dụng cấp, không có identity/sequence/default. Tên bảng và cột dùng identifier được quote để giữ đúng camelCase nguồn. Khóa ngoại dùng `ON DELETE RESTRICT`; không có cascade.

### 8.2. `ChiTietHoaDon` — sửa đổi V1 được phê duyệt

- PK `(maHD, soSerial)`; FK `maHD` tới `HoaDon`, FK ghép `(soSerial, maDongSP)` tới `SanPham(soSerial, maDongSP)`.
- Giữ `maDongSP` theo sơ đồ nguồn để bảo đảm dòng sản phẩm của chi tiết khớp dòng của máy.
- `soLuong` có CHECK bằng `1` (RB12, quan hệ một chi tiết–một máy).
- Snapshot tại thời điểm bán: `donGiaBanTaiLucBan`, `thueSuatTaiLucBan`, `maKM` (nullable), `tyLeGiamTaiLucBan` (nullable), `tienKhuyenMaiThucTe`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, `tienThue`.
- CHECK: `donGiaBanTaiLucBan >= 0`; `thueSuatTaiLucBan` trong `0..100`; `tyLeGiamTaiLucBan` nullable nhưng nếu có thì trong `0..100`; các khoản tiền `>= 0`; `tienSauKhuyenMai <= tienTruocKhuyenMai`.
- Không tính lại từ giá/thuế/khuyến mãi hiện tại; giá trị đã lưu là dữ liệu lịch sử (RB42–RB46). Việc tính toán và đối soát thuộc lớp ứng dụng.

### 8.3. Chi tiết trả và chi tiết đổi

- `ChiTietPhieuTra` tham chiếu chi tiết hóa đơn gốc bằng `(maHD, soSerial)`; PK `(maPhieuTra, maHD, soSerial)`; FK tới `PhieuTra` và `ChiTietHoaDon`.
- `ChiTietPhieuDoi` tham chiếu chi tiết hóa đơn gốc bằng `(maHD, soSerial)`; PK `(maPhieuDoi, maHD, soSerial)`; FK tới `PhieuDoi` và `ChiTietHoaDon`.
- Máy thay thế dùng cột nguồn `chiTietSanPhamDoi`, FK tới `SanPham(soSerial)`.
- UQ `(maPhieuDoi, chiTietSanPhamDoi)` không cho một máy thay thế lặp trong cùng phiếu; CHECK `chiTietSanPhamDoi <> soSerial` không cho thay thế chính máy khách mang đổi (RB25).

### 8.4. `SanPhamLoi` — liên kết nguồn và trigger

- `maLoi` là ID ứng dụng cấp; `soSerial` bắt buộc và FK tới `SanPham`.
- `maPhieuTra` và `maPhieuDoi` là FK tùy chọn tới header phiếu trả/phiếu đổi; cả hai có thể NULL khi lỗi không phát sinh từ đổi/trả. Không thêm quy tắc loại trừ lẫn nhau.
- Trigger `trg_sanphamloi_nguon_hop_le` kiểm tra: nếu có `maPhieuTra` thì phải tồn tại `ChiTietPhieuTra` cùng phiếu chứa `soSerial`; nếu có `maPhieuDoi` thì phải tồn tại `ChiTietPhieuDoi` cùng phiếu chứa serial đó ở máy trả hoặc máy thay thế. Đây là diễn giải được phê duyệt cho RB27.
- `tinhTrangXuLy` dùng đúng tập `TrangThaiXuLyLoi`; `ngayCapNhap` nếu có không trước `ngayGhiNhan`.

### 8.5. Phạm vi và giới hạn của V1

- V1 tạo đúng **14 bảng** nêu trên; không có bảng `products`, không có dữ liệu mẫu, không có baseline tự động.
- Không thêm trigger cho tồn kho, phê duyệt hoặc quy trình giao dịch; các quy tắc liên bảng đó vẫn thuộc lớp ứng dụng.
- Các tổng tiền dẫn xuất của `HoaDon`, `PhieuTra`, `PhieuDoi` và các thuộc tính dẫn xuất của `SanPham`/`DongSanPham` không được lưu trong V1; ứng dụng tính theo công thức nguồn.
- Quan hệ `KhuyenMai.maDongSP` → `DongSanPham` là quan hệ cấu trúc theo EER `image8`/sơ đồ lớp `image17` và RB20; V1 không suy ra phạm vi khuyến mãi từ quan hệ này, `C-04` vẫn mở.
- V1 đã quyết định `G-11`/`BR-C12` theo amendment được phê duyệt; xung đột DOCX vẫn được giữ làm bằng chứng lịch sử. Các xung đột `C-04` (phạm vi khuyến mãi), `C-07` (đơn vị tồn kho serial hay số lượng) và `C-11` (quy tắc làm tròn) vẫn giữ nguyên. Xem [bản đồ DDL V1](../database/schema-and-integrity.md#6-bản-đồ-ddl-v1-đã-phê-duyệt).

---

## 9. Checklist hoàn thành (file này)

- [x] Trích 14 thực thể (NhanVien → SanPhamLoi) với ràng buộc và ghi chú.
- [x] Trích 6 kiểu liệt kê (LoaiQuyen, TrangThaiSanPham, TrangThaiXuLyPhieu, TrangThaiDuyet, TinhTrangHangTra, TrangThaiXuLyLoi).
- [x] Trích "Quy ước chung".
- [x] Giữ nguyên mã lỗi/định dạng mã `TIỀN_TỐ-YYYY-NNNNNN` và ví dụ.
- [x] Ghi công thức tính tiền, thuế, hoàn tiền, chênh lệch, tồn kho, độ mới.
- [x] Chỉ tham chiếu hình sơ đồ lớp, không vẽ lại; nêu bản sao byte-identical, hình gốc còn ở nguồn, và toàn bộ 37 ảnh sao đã rà soát trực quan ngày 2026-10-19.
- [x] G-11/BR-C12 ghi bằng chứng trực quan hẹp (trường sơ đồ lớp và cột EER của `ChiTietHoaDon`), giữ xung đột DOCX làm bằng chứng lịch sử; V1 đã quyết định mô hình theo amendment được phê duyệt.
- [x] Phân biệt thiết kế báo cáo với mã nguồn hiện tại.
- [x] Không nêu số trang chưa xác minh.
