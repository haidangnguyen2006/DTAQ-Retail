# Thiết kế cơ sở dữ liệu và ràng buộc toàn vẹn (Schema & Integrity)

> **Trạng thái tài liệu:** Trích xuất từ báo cáo phân tích và thiết kế v12; trạng thái phê duyệt chưa được xác minh.
> **Nguồn:** `docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`.
> **Phạm vi trích xuất:** CHƯƠNG 4 › II. Thiết kế cơ sở dữ liệu (mục 1–3).
> **Phân biệt:** Nội dung dưới đây là **thiết kế ghi trong báo cáo**. Mã nguồn hiện tại **không** được dùng để suy diễn lược đồ bảng hay ràng buộc còn thiếu trong DOCX.
> **Số trang:** Không nêu vì chưa xác minh; chỉ dẫn theo tiêu đề mục.
> **Hình vẽ:** Tài liệu này **không vẽ lại** sơ đồ EER hay sơ đồ trên HQT CSDL.
> **Bổ sung V1 (đã phê duyệt, 2026-10-19):** Mục 6 ghi bản đồ DDL V1 cho 14 bảng, khóa và ràng buộc. EER/sơ đồ HQT CSDL gốc vẫn là nguồn tham chiếu; V1 bổ sung liên kết serial và snapshot giá/thuế/khuyến mãi theo quyết định đã duyệt, không tuyên bố các cột này đã có trong DOCX.

---

## 1. Cấu trúc mục trong báo cáo nguồn

| Mục | Tiêu đề | Nội dung trích xuất |
|---|---|---|
| CHƯƠNG 4 › II › 1 | Sơ đồ thực thể và mối kết hợp (EER) | Chỉ có hình; không có bảng dữ liệu văn bản. |
| CHƯƠNG 4 › II › 2 | Sơ đồ trên HQT CSDL | Chỉ có hình; không có bảng dữ liệu văn bản. |
| CHƯƠNG 4 › II › 3 | Các ràng buộc toàn vẹn trong CSDL | Văn bản + 9 nhóm ràng buộc, mã `RB01`–`RB66`. |

### Tài sản hình ảnh (bản sao byte-identical và nguồn DOCX)

- **CHƯƠNG 4 › II › 1. Sơ đồ thực thể và mối kết hợp (EER):** một hình duy nhất (`word/media/image6.png`, hiển thị ≈ 6.10 in × 3.99 in). Bản sao byte-identical: [`../assets/diagrams/image6.png`](../assets/diagrams/image6.png).
- **CHƯƠNG 4 › II › 2. Sơ đồ trên HQT CSDL:** một hình duy nhất (`word/media/image8.png`, hiển thị ≈ 6.10 in × 4.36 in). Bản sao byte-identical: [`../assets/diagrams/image8.png`](../assets/diagrams/image8.png).
- **Hình ảnh gốc vẫn nằm trong file DOCX nguồn**; các bản sao byte-identical đặt trong `docs/reference/assets/diagrams/`.
- **Toàn bộ 37 ảnh sao đã được rà soát trực quan ngày 2026-10-19** (bao gồm sơ đồ lớp `image17`, EER `image6`, sơ đồ trên HQT CSDL `image8`; các bản sao byte-identical dẫn trên). Quan sát cho `ChiTietHoaDon`: EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai` và **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**; sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai` và **không thấy trường serial/snapshot nào được vẽ trực tiếp**. Đây là **xung đột thiết kế nguồn được giữ làm bằng chứng lịch sử**; V1 đã quyết định mô hình theo amendment được phê duyệt tại 6.2. Không suy đoán thêm cột/quan hệ ngoài phần thấy được.
- Tài liệu này không tái tạo hoặc vẽ lại hai sơ đồ.

**Hệ quả:** Mặc dù hai sơ đồ đã được rà soát trực quan ngày 2026-10-19, tài liệu này **không** khẳng định danh sách bảng, cột, khóa ngoại hay kiểu dữ liệu đầy đủ; các ràng buộc dưới đây là ngôn ngữ nghiệp vụ trong DOCX.

---

## 2. Ràng buộc toàn vẹn trong CSDL (`RB01`–`RB66`)

> Phần mở đầu của DOCX: *"Các ràng buộc toàn vẹn được xây dựng nhằm bảo đảm dữ liệu của hệ thống quản lý siêu thị điện máy DTAQ luôn hợp lệ, nhất quán và có thể truy vết. Các ràng buộc được áp dụng khi thêm, sửa, xóa dữ liệu và khi hoàn tất các giao dịch bán hàng, đổi hàng hoặc trả hàng."*

### 2.1. Ràng buộc về khóa và tính duy nhất (CHƯƠNG 4 › II › 3 › 3.1)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB01 | Các bảng dữ liệu | Mỗi bảng phải có khóa chính để phân biệt từng bản ghi. Khóa chính không được nhận giá trị NULL, không được trùng và không được thay đổi tùy ý sau khi đã được tham chiếu. |
| RB02 | NhanVien, KhachHang, DanhMucSanPham, DongSanPham, NhaCungCap, KhuyenMai, HoaDon, PhieuTra, PhieuDoi, SanPhamLoi | Các mã định danh tương ứng phải duy nhất trong từng bảng. Mã được tự sinh theo định dạng đã quy định, không tái sử dụng mã của bản ghi đã xóa hoặc chứng từ đã hủy. |
| RB03 | Các mã tự sinh | Cấu trúc mã phải gồm tiền tố, năm và số thứ tự theo định dạng `TIỀN_TỐ-YYYY-NNNNNN`. Năm và tiền tố phải phù hợp với loại thực thể tại thời điểm cấp mã. Việc cấp mã đồng thời phải bảo đảm không trùng. |
| RB04 | SanPham | `soSerial` không được rỗng và phải duy nhất trong bảng sản phẩm. Mỗi serial đại diện cho một máy vật lý. Máy được nhận lại qua đổi/trả vẫn sử dụng hồ sơ và serial đã có, không tạo bản ghi sản phẩm mới cho cùng máy. |
| RB05 | NhanVien | `tenDangNhap` không được rỗng và phải duy nhất theo quy tắc chuẩn hóa tên đăng nhập của hệ thống. Tài khoản đã bị khóa vẫn giữ tên đăng nhập để bảo toàn hồ sơ. |
| RB06 | DanhMucSanPham | `tenDanhMuc` không được rỗng và không được trùng sau khi chuẩn hóa khoảng trắng và cách so sánh chữ hoa, chữ thường. |
| RB07 | ChiTietHoaDon, ChiTietPhieuTra, ChiTietPhieuDoi | Mỗi chi tiết phải có khóa xác định duy nhất. Trong cùng chứng từ không được ghi lặp cùng máy hoặc cùng chi tiết giao dịch gốc cho một lần xử lý. Một serial có thể xuất hiện trong các giao dịch bán khác nhau nếu trước đó đã được nhận lại hợp lệ; không đặt ràng buộc duy nhất toàn bộ lịch sử bán trên serial. |

### 2.2. Ràng buộc về miền giá trị (CHƯƠNG 4 › II › 3 › 3.2)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB08 | Các trường thông tin bắt buộc | Các trường bắt buộc như họ tên nhân viên, tên đăng nhập, họ tên khách hàng, tên danh mục, tên dòng sản phẩm, tên nhà cung cấp và tên khuyến mãi không được NULL hoặc chỉ chứa khoảng trắng. |
| RB09 | NhanVien, KhachHang | Số điện thoại được lưu bằng kiểu chuỗi để giữ số 0 đầu hoặc mã quốc gia; phải đúng định dạng hệ thống chấp nhận. Email, nếu được nhập, phải đúng định dạng. |
| RB10 | NhanVien | `matKhau` không được rỗng. Giá trị lưu trữ phải là giá trị băm mật khẩu, không lưu mật khẩu gốc. |
| RB11 | DongSanPham, SanPham | `soLuongTon`, `soLanDoiTra` và `soLanSua` phải là số nguyên lớn hơn hoặc bằng 0. |
| RB12 | ChiTietHoaDon | `soLuong` phải là số nguyên dương. Theo quan hệ một chi tiết hóa đơn gắn với một máy trong thiết kế hiện tại, `soLuong` phải bằng 1. |
| RB13 | Các trường giá và tiền | Giá nhập, giá bán cơ sở, giá bán, tiền trước và sau khuyến mãi, tiền thuế, tiền hoàn và tiền khách đưa phải lớn hơn hoặc bằng 0. Các trường tiền chênh lệch được phép âm vì có thể phát sinh khoản cửa hàng phải hoàn cho khách. |
| RB14 | DongSanPham, KhuyenMai | Theo quy ước lưu tỷ lệ phần trăm, `thue` và `tyLeGiam` phải nằm trong khoảng từ 0 đến 100. Ví dụ, giá trị `10` tương ứng 10%. |
| RB15 | SanPham | `doMoi` phải nằm trong khoảng từ 0 đến 100; `soNgayTonKho` không được âm. Giá trị phải được tính theo công thức nghiệp vụ đã được phê duyệt. |
| RB16 | Các trường trạng thái | Quyền nhân viên, trạng thái sản phẩm, trạng thái duyệt, trạng thái xử lý phiếu, tình trạng hàng trả và trạng thái xử lý lỗi chỉ được nhận các giá trị trong tập liệt kê tương ứng. |
| RB17 | Các trường logic | `trangThaiHoatDong`, `trangThaiKinhDoanh`, `trangThaiThanhToan` và `dangApDung` chỉ nhận hai giá trị đúng hoặc sai; không để NULL. |

**Các tập giá trị trạng thái được quy định như sau:**

| Thuộc tính hoặc kiểu trạng thái | Tập giá trị hợp lệ |
|---|---|
| LoaiQuyen | `NHAN_VIEN_QUAN_LY`, `NHAN_VIEN_BAN_HANG`. |
| TrangThaiSanPham | `SAN_SANG_BAN`, `DA_BAN`, `HANG_LOI`, `DANG_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `DA_THANH_LY`. |
| TrangThaiXuLyPhieu | `NHAP`, `CHO_XU_LY`, `HOAN_TAT`, `HUY`. |
| TrangThaiDuyet | `KHONG_CAN_DUYET`, `CHO_DUYET`, `DA_DUYET`, `TU_CHOI`. |
| TinhTrangHangTra | `NGUYEN_VEN`, `HANG_LOI`. |
| TrangThaiXuLyLoi | `CHO_XU_LY`, `DA_GUI_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `THANH_LY`. |

### 2.3. Ràng buộc toàn vẹn tham chiếu (CHƯƠNG 4 › II › 3 › 3.3)

| Mã ràng buộc | Quan hệ áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB18 | DongSanPham – DanhMucSanPham | Mỗi dòng sản phẩm phải thuộc một danh mục đã tồn tại. Không được lưu dòng sản phẩm tham chiếu đến danh mục không có trong CSDL. |
| RB19 | SanPham – DongSanPham – NhaCungCap | Mỗi máy phải thuộc một dòng sản phẩm và một nhà cung cấp đã tồn tại. |
| RB20 | KhuyenMai – DongSanPham | Chương trình khuyến mãi được gán cho dòng sản phẩm phải tham chiếu đúng đối tượng tồn tại. Một chương trình không được gắn với dòng sản phẩm không có trong CSDL. |
| RB21 | HoaDon – NhanVien – KhachHang | Mỗi hóa đơn phải tham chiếu đến nhân viên lập và khách hàng đã tồn tại. |
| RB22 | ChiTietHoaDon – HoaDon – SanPham | Mỗi chi tiết hóa đơn phải thuộc một hóa đơn và xác định được máy thực tế được bán. Dòng sản phẩm ghi tại chi tiết phải khớp dòng sản phẩm của máy đó. |
| RB23 | PhieuTra, PhieuDoi – HoaDon – NhanVien | Mỗi phiếu phải tham chiếu đến hóa đơn gốc và nhân viên lập đã tồn tại. Hóa đơn gốc phải là hóa đơn đã thanh toán. |
| RB24 | ChiTietPhieuTra, ChiTietPhieuDoi – ChiTietHoaDon | Chi tiết hóa đơn được đổi hoặc trả phải thuộc đúng hóa đơn gốc của phiếu. Không được đưa chi tiết từ hóa đơn khác vào cùng phiếu. |
| RB25 | ChiTietPhieuDoi – SanPham | Máy thay thế phải tồn tại, khác máy khách mang đổi và không được dùng lặp trong nhiều chi tiết của cùng giao dịch. |
| RB26 | Phiếu đổi/trả – NhanVien duyệt | Khi phiếu có quyết định duyệt hoặc từ chối, nhân viên duyệt phải tồn tại và có quyền quản lý tại thời điểm quyết định. Việc đổi quyền sau này không làm mất thông tin người đã duyệt. |
| RB27 | SanPhamLoi – SanPham và chứng từ liên quan | Mỗi hồ sơ lỗi phải xác định được một máy cụ thể. Nếu lỗi phát sinh từ đổi/trả thì chứng từ liên quan phải tồn tại và phải liên quan đúng máy đó. |

### 2.4. Ràng buộc về thời gian (CHƯƠNG 4 › II › 3 › 3.4)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB28 | KhuyenMai | `ngayBatDau ≤ ngayKetThuc`. Chương trình chỉ được áp dụng khi `dangApDung = true`, ngày giao dịch nằm trong thời hạn và dòng sản phẩm thuộc phạm vi áp dụng. |
| RB29 | SanPham | Ngày nhập kho ban đầu không được lớn hơn ngày ghi nhận nhập kho thực tế. Ngày này không được đặt lại khi máy được trả về hoặc nhận lại sau bảo hành. |
| RB30 | PhieuTra, PhieuDoi | Ngày tạo phiếu không được trước ngày lập hóa đơn gốc. Ngày duyệt và ngày hoàn tất, nếu có, không được trước ngày tạo phiếu. |
| RB31 | Phiếu cần phê duyệt | Khi phiếu đã hoàn tất và thuộc trường hợp phải duyệt, phải có quyết định chấp thuận trước khi hoàn tất; `ngayDuyet ≤ ngayHoanTat`. |
| RB32 | Phiếu đổi/trả | Phiếu ở trạng thái `HOAN_TAT` phải có `ngayHoanTat`. Phiếu chưa hoàn tất không được ghi ngày hoàn tất như một giao dịch đã thực hiện thành công. |
| RB33 | SanPhamLoi, DongSanPham | Ngày cập nhật cuối không được trước ngày ghi nhận hoặc ngày khởi tạo tương ứng. |
| RB34 | Điều kiện đổi/trả | Thời điểm xử lý phải đáp ứng thời hạn đổi/trả theo chính sách được phê duyệt. Không tự cố định số ngày khi chính sách chưa quy định cụ thể. |

### 2.5. Ràng buộc về bán hàng và tồn kho (CHƯƠNG 4 › II › 3 › 3.5)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB35 | Bán hàng và giao máy thay thế | Chỉ được xuất bán hoặc giao thay thế máy có trạng thái `SAN_SANG_BAN` và thuộc dòng sản phẩm đang kinh doanh. |
| RB36 | Tồn kho | Số lượng bán hoặc giao thay thế không được vượt số lượng sẵn sàng bán. Tồn kho sau giao dịch không được âm. |
| RB37 | DongSanPham – SanPham | `soLuongTon` của dòng sản phẩm phải bằng số máy thuộc dòng có trạng thái `SAN_SANG_BAN`, theo quy ước tồn sẵn sàng bán của hệ thống. |
| RB38 | Hoàn tất hóa đơn | Hóa đơn được thanh toán phải có ít nhất một chi tiết hợp lệ và khách đã thanh toán đủ. Khi giao dịch thành công, các máy bán phải chuyển sang `DA_BAN` và tồn sẵn sàng bán được cập nhật tương ứng. |
| RB39 | Nhận hàng đổi/trả | Hàng nhận lại chỉ được chuyển sang `SAN_SANG_BAN` khi đã kiểm tra đủ điều kiện. Hàng lỗi phải được ghi nhận đúng trạng thái, không tự động cộng vào tồn sẵn sàng bán. |
| RB40 | Xử lý đồng thời | Hai giao dịch không được cùng xuất bán một máy đang sẵn sàng bán. Trạng thái máy và tồn kho phải được kiểm tra lại trong giao dịch ghi nhận kết quả. |
| RB41 | Số lần đổi/trả và sửa chữa | `soLanDoiTra` chỉ tăng khi việc nhận lại máy hoàn tất. `soLanSua` chỉ tăng khi sửa chữa hoàn tất. Không tăng các giá trị này khi mới lập phiếu, gửi duyệt, gửi bảo hành hoặc hủy xử lý. |

### 2.6. Ràng buộc tính toán và bảo toàn giá trị giao dịch (CHƯƠNG 4 › II › 3 › 3.6)

> Gọi `Sᵢ` là tiền hàng của chi tiết `i` sau khuyến mãi, trước thuế; `rᵢ` là thuế suất thực tế của chi tiết tại thời điểm giao dịch.

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB42 | ChiTietHoaDon | `tienTruocKhuyenMai = soLuong × đơnGiáTrướcThuếTạiLúcBán`. Đơn giá phải được xác định theo chính sách giá hợp lệ, không do nhân viên bán hàng sửa tùy ý trên hóa đơn. |
| RB43 | ChiTietHoaDon | `tienSauKhuyenMai = tienTruocKhuyenMai − tiềnKhuyếnMãiThựcTế`; phải thỏa mãn `0 ≤ tienSauKhuyenMai ≤ tienTruocKhuyenMai`. Tiền giảm phải tuân thủ chương trình và quy tắc kết hợp khuyến mãi đã được phê duyệt. |
| RB44 | HoaDon | `tongTienTruocThue = Σ Sᵢ`; `tongTienThue = Σ tiềnThuếChiTiếtᵢ`, trong đó tiền thuế chi tiết được tính từ `Sᵢ × rᵢ / 100` theo quy tắc làm tròn thống nhất. |
| RB45 | HoaDon | `tongTienThanhToan = tongTienTruocThue + tongTienThue`. Nếu lưu các tổng tiền trong CSDL thì các tổng này phải khớp dữ liệu chi tiết. |
| RB46 | Dữ liệu lịch sử | Đơn giá, thuế suất và khuyến mãi thực tế đã dùng phải được lưu theo giao dịch. Thay đổi giá, thuế hoặc chương trình hiện tại không được làm thay đổi giá trị hóa đơn đã thanh toán. |
| RB47 | ChiTietPhieuTra, PhieuTra | Tiền hoàn được xác định từ giá trị thực tế đã thanh toán của phần hàng đủ điều kiện trả, bao gồm thuế và sau khuyến mãi. `tongTienHoanTra = Σ tienHoanTra` của các chi tiết; không tính lại bằng giá hiện tại. |
| RB48 | ChiTietPhieuDoi | `tienChenhLech = giáTrịMáyThayThếSauThuế − giáTrịThanhToánGốcCủaMáyMangĐổi`; `tienThuThem = max(0, tienChenhLech)`. |
| RB49 | PhieuDoi | `tongTienChenhLech = Σ tienChenhLech`; `tongTienThuThem = max(0, tongTienChenhLech)`. Nếu tổng chênh lệch âm, tiền cần hoàn bằng `max(0, −tongTienChenhLech)`. Phải bù trừ cả chi tiết âm và dương trước khi xác định khoản thu hoặc hoàn của phiếu. |
| RB50 | Thuộc tính dẫn xuất | Các thuộc tính dẫn xuất phải được tính từ dữ liệu nguồn theo công thức đã phê duyệt. Nếu lưu kết quả để phục vụ tra cứu thì phải có cơ chế cập nhật, đối soát để không lệch dữ liệu nguồn. Công thức còn được ghi là đề xuất chưa được xem là quy tắc chính thức. |
| RB51 | Các phép tính tiền | Sử dụng cùng quy tắc về độ chính xác và làm tròn trong lưu trữ, hiển thị và báo cáo. Không dùng giá trị hiển thị đã bị làm tròn ở bước trung gian để tiếp tục tính toán. Các cột tiền và tỷ lệ trong CSDL nên sử dụng kiểu số thập phân chính xác với độ chính xác phù hợp. |

> **Ghi chú xung đột thiết kế nguồn (G-11 / BR-C12):** `RB22` yêu cầu mỗi chi tiết hóa đơn xác định được **máy thực tế được bán** và `RB46` yêu cầu **lưu đơn giá, thuế suất và khuyến mãi tại thời điểm giao dịch**, trong khi phần văn bản mô tả `ChiTietHoaDon` tại Ch.4 §I.3 chỉ hiển thị `sanPham: DongSanPham`, `soLuong` = 1 và các khoản dẫn xuất. **Bằng chứng trực quan ngày 2026-10-19**: EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**; sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai`, **không thấy trường serial/snapshot nào được vẽ trực tiếp**. Đây là **xung đột thiết kế nguồn được giữ làm bằng chứng lịch sử**; phần trích DOCX không thêm cột/quan hệ ngoài phần thấy được. **V1 đã quyết định mô hình theo amendment được phê duyệt** tại 6.2. Xem [ghi chú mô hình tại domain](../domain/class-model-and-entities.md), [G-11](../traceability.md), [BR-C12](../business-rules/rules-and-exceptions.md).

### 2.7. Ràng buộc về đổi/trả và phê duyệt (CHƯƠNG 4 › II › 3 › 3.7)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB52 | Xử lý đổi/trả | Chỉ được đổi hoặc trả máy đã bán trên hóa đơn gốc và còn đủ điều kiện xử lý. Tổng số lượng đã xử lý hoàn tất không được vượt số lượng đã mua. |
| RB53 | Chống xử lý trùng | Không được hoàn tất nhiều phiếu đổi hoặc trả cho cùng một quyền đổi/trả phát sinh từ một lần bán. Phiếu đã hủy không được tính là đã xử lý hoàn tất; máy được bán lại hợp lệ tạo ra giao dịch gốc mới để kiểm tra. |
| RB54 | Ngưỡng phê duyệt | Khoản tiền cửa hàng phải hoàn từ 3.000.000 đồng trở lên phải được quản lý phê duyệt trước khi giao dịch hoàn tất. Với phiếu đổi, kiểm tra trên khoản hoàn sau khi bù trừ chênh lệch toàn phiếu. |
| RB55 | Thông tin phê duyệt | Khi `trangThaiDuyet` là `DA_DUYET` hoặc `TU_CHOI`, phải có người duyệt và ngày duyệt. Trường hợp từ chối phải có lý do. |
| RB56 | Quan hệ giữa duyệt và xử lý | Phiếu `CHO_DUYET` hoặc `TU_CHOI` không được chuyển sang `HOAN_TAT`. Phiếu không cần duyệt chỉ được hoàn tất khi đáp ứng các điều kiện nghiệp vụ còn lại. |
| RB57 | Tác động của quyết định duyệt | Việc duyệt chỉ ghi nhận quyết định; không tự động hoàn tiền, giao máy hoặc cập nhật tồn kho như một giao dịch đã hoàn tất. |
| RB58 | Thay đổi sau khi duyệt | Nếu sửa nội dung ảnh hưởng đến máy, điều kiện xử lý hoặc tiền hoàn của phiếu đã duyệt nhưng chưa hoàn tất, phải kiểm tra và xác định lại yêu cầu phê duyệt. Không sử dụng quyết định cũ cho nội dung đã thay đổi. |
| RB59 | Phiếu đã hoàn tất hoặc hủy | Phiếu đã hoàn tất không được sửa trực tiếp nội dung làm thay đổi kết quả giao dịch hoặc thực hiện lại tác động kho, tiền. Phiếu đã hủy không được tiếp tục xử lý như phiếu đang chờ. |

### 2.8. Ràng buộc bảo toàn lịch sử và tính nhất quán giao dịch (CHƯƠNG 4 › II › 3 › 3.8)

| Mã ràng buộc | Đối tượng áp dụng | Nội dung ràng buộc |
|---|---|---|
| RB60 | Dữ liệu được tham chiếu | Không được xóa bản ghi nếu việc xóa làm mất liên kết với hóa đơn, phiếu đổi/trả hoặc hồ sơ lỗi cần lưu giữ. Không xóa dây chuyền dữ liệu giao dịch lịch sử khi xóa dữ liệu danh mục. |
| RB61 | NhanVien, DongSanPham | Nhân viên nghỉ việc được khóa tài khoản; dòng sản phẩm ngừng kinh doanh được đổi trạng thái. Các giao dịch lịch sử vẫn giữ tham chiếu đến hồ sơ cũ. |
| RB62 | HoaDon đã thanh toán | Không được xóa, hủy trực tiếp hoặc đổi lại trạng thái chưa thanh toán để loại bỏ giao dịch. Khi khách không tiếp tục mua phải xử lý bằng nghiệp vụ đổi/trả phù hợp. |
| RB63 | Giao dịch bán hàng | Lưu hóa đơn, chi tiết, giá và thuế lịch sử, trạng thái máy và tồn kho phải được thực hiện trong cùng một giao dịch CSDL. Nếu một bước thất bại thì toàn bộ thay đổi của giao dịch phải được hoàn tác. |
| RB64 | Giao dịch đổi/trả | Lưu phiếu, chi tiết, kết quả xử lý, dữ liệu tiền, trạng thái máy và tồn kho phải được cập nhật nhất quán trong cùng giao dịch CSDL. Không để tồn tại trạng thái chỉ hoàn thành một phần dữ liệu. |
| RB65 | Thống kê, báo cáo | Chỉ sử dụng hóa đơn đã thanh toán và phiếu đổi/trả đã hoàn tất để ghi nhận kết quả thực tế. Không cộng phiếu nháp, phiếu chờ duyệt hoặc phiếu hủy như giao dịch thành công. |
| RB66 | Doanh thu và truy vết | Doanh thu nội bộ được tổng hợp từ giá trị trước thuế và điều chỉnh theo đổi/trả hợp lệ; tiền thuế được theo dõi riêng. Giao dịch phải giữ được nhân viên thực hiện và chứng từ gốc, tránh ghi nhận trùng giá trị khi đổi hàng. |

### 2.9. Cơ chế thực hiện các ràng buộc (CHƯƠNG 4 › II › 3 › 3.9)

| Nhóm ràng buộc | Cơ chế thực hiện |
|---|---|
| Khóa chính và tính duy nhất | Sử dụng PRIMARY KEY, UNIQUE và cơ chế cấp mã an toàn khi có nhiều thao tác đồng thời. |
| Giá trị bắt buộc và miền dữ liệu | Sử dụng kiểu dữ liệu phù hợp, NOT NULL và CHECK cho các điều kiện kiểm tra được trên từng bản ghi. Trường chuỗi bắt buộc phải được kiểm tra thêm điều kiện không chỉ chứa khoảng trắng. |
| Toàn vẹn tham chiếu | Sử dụng FOREIGN KEY; hạn chế xóa bản ghi đang được tham chiếu và bảo toàn chứng từ lịch sử. |
| Tổng tiền, số lượng và quy tắc liên bảng | Kiểm tra bằng thủ tục lưu trữ, trigger hoặc lớp xử lý nghiệp vụ trong giao dịch CSDL. Không chỉ kiểm tra tại giao diện. |
| Chống bán hoặc đổi/trả trùng | Kết hợp giao dịch CSDL với cơ chế khóa hoặc kiểm soát đồng thời phù hợp; kiểm tra lại dữ liệu trước khi ghi nhận hoàn tất. |
| Quyền cập nhật dữ liệu | Kiểm tra người thực hiện tại nơi xử lý nghiệp vụ; chỉ cho phép thay đổi giá, thuế và phê duyệt theo quyền đã được cấp. |

---

## 3. Ngưỡng nghiệp vụ then chốt xuất hiện trong ràng buộc CSDL

- **Ngưỡng phê duyệt đổi/trả (RB54):** khoản tiền cửa hàng phải hoàn **từ 3.000.000 đồng trở lên** phải được quản lý phê duyệt trước khi giao dịch hoàn tất. Với phiếu đổi, kiểm tra trên khoản hoàn sau khi bù trừ chênh lệch toàn phiếu.
- **Tính bù trừ toàn phiếu (RB49):** tổng tiền thu/hoàn của phiếu đổi phải tính sau khi bù trừ cả chênh lệch âm và dương của mọi chi tiết.
- **Bảo toàn giá trị lịch sử (RB46, RB47, RB60):** đơn giá, thuế suất, khuyến mãi đã dùng được lưu theo giao dịch; thay đổi hiện tại không làm đổi giá trị quá khứ.

---

## 4. Ảnh chụp mã nguồn ngày 2026-10-19 (không dùng để bổ sung dữ kiện DOCX)

> Ảnh chụp tại ngày **2026-10-19**. Chỉ ghi nhận những gì đọc được từ đúng tệp/dòng được dẫn; không suy diễn lược đồ hoặc ràng buộc từ mã nguồn. Ảnh chụp này được lấy **trước khi V1 được thêm**; DDL V1 hiện có tại `src/main/resources/db/migration/V1__initial_schema.sql` và được mô tả ở mục 6.

| Quan sát trong ảnh chụp | Bằng chứng |
|---|---|
| Thực thể hiện có là `Product`, một `record` gồm `long id`, `String name`, `BigDecimal price`, `int quantity`. | `src/main/java/com/dtaq/retail/entity/Product.java:5-7` |
| Các hằng số SQL hiện có đọc/ghi bảng tên `products`: SELECT đọc `id, name, price, quantity`; INSERT ghi `name, price, quantity`. | `src/main/java/com/dtaq/retail/dao/impl/ProductDaoImpl.java:19-21` |
| Không tìm thấy tệp `.sql` hoặc `.ddl` nào trong dự án. | Tìm kiếm toàn dự án ngày 2026-10-19 với mẫu `**/*.sql`, `**/*.ddl`: không có kết quả. |

Hệ quả trong phạm vi ảnh chụp:

- Tại thời điểm ảnh chụp: không có định nghĩa `CREATE TABLE`, khóa ngoại, `CHECK`, thủ tục lưu trữ hoặc trigger cho các ràng buộc `RB01`–`RB66`.
- Tên bảng `products` và danh sách cột chỉ xuất hiện trong chuỗi SQL; không có DDL xác nhận kiểu dữ liệu, khóa chính hoặc khóa ngoại.
- Tại thời điểm ảnh chụp: không có DDL để đối chiếu kiểu dữ liệu/khóa với `RB01`–`RB66`; quan sát hình ảnh (EER, sơ đồ HQT CSDL) được ghi riêng ở mục “Tài sản hình ảnh”, không dùng bổ sung dữ kiện mã nguồn.

---

## 5. Checklist hoàn thành (file này)

- [x] Trích mục 1 "Sơ đồ thực thể và mối kết hợp (EER)" dưới dạng tham chiếu hình, không vẽ lại.
- [x] Trích mục 2 "Sơ đồ trên HQT CSDL" dưới dạng tham chiếu hình, không vẽ lại.
- [x] Trích đủ 66 ràng buộc `RB01`–`RB66` theo 9 nhóm 3.1–3.9.
- [x] Giữ nguyên mã ràng buộc, đối tượng áp dụng và nội dung ràng buộc.
- [x] Trích bảng tập giá trị trạng thái hợp lệ.
- [x] Trích bảng cơ chế thực hiện ràng buộc.
- [x] Nêu rõ bản sao byte-identical của hai sơ đồ, hình gốc còn ở nguồn, và đã rà soát trực quan ngày 2026-10-19.
- [x] Phân biệt thiết kế báo cáo với mã nguồn hiện tại.
- [x] Không nêu số trang chưa xác minh.

---

## 6. Bản đồ DDL V1 (đã phê duyệt)

> **Vị trí:** `src/main/resources/db/migration/V1__initial_schema.sql` (Flyway versioned migration V1).
>
> **Trạng thái:** Đây là **bản sửa đổi V1 đã được phê duyệt ngày 2026-10-19**, không phải DDL gốc của DOCX. EER (`word/media/image6.png`) và sơ đồ trên HQT CSDL (`word/media/image8.png`) vẫn là **nguồn tham chiếu**; V1 bổ sung liên kết serial và snapshot giá/thuế/khuyến mãi theo quyết định đã duyệt. Các ràng buộc `RB01`–`RB66` ở mục 2 vẫn giữ nguyên nội dung nguồn và các xung đột đã nêu.

### 6.1. Mười bốn bảng, khóa và ràng buộc chính

| Bảng | Khóa chính | Khóa ngoại / duy nhất / CHECK chính |
|---|---|---|
| `NhanVien` | `maNV` | UQ `tenDangNhap`; CHECK `LoaiQuyen`; chuỗi bắt buộc không rỗng. |
| `KhachHang` | `maKH` | CHECK họ tên và số điện thoại không rỗng. |
| `DanhMucSanPham` | `maDanhMuc` | UQ biểu thức `lower(btrim(tenDanhMuc))`; CHECK tên không rỗng. |
| `NhaCungCap` | `maNCC` | CHECK tên không rỗng. |
| `DongSanPham` | `maDongSP` | FK `maDanhMuc` → `DanhMucSanPham`; CHECK `soLuongTon >= 0`, `thue` trong `0..100`, `donGiaBanTruocThue >= 0`, thứ tự ngày cập nhật. |
| `SanPham` | `soSerial` | UQ `(soSerial, maDongSP)`; FK `maNCC` → `NhaCungCap`, `maDongSP` → `DongSanPham`; CHECK `giaNhap >= 0`, số lần đổi/trả và sửa `>= 0`, CHECK `TrangThaiSanPham`. |
| `KhuyenMai` | `maKM` | FK bắt buộc `maDongSP` → `DongSanPham` (RB20); CHECK `ngayBatDau <= ngayKetThuc`, `tyLeGiam` trong `0..100`. |
| `HoaDon` | `maHD` | FK `nhanVienLap` → `NhanVien`, `khachHang` → `KhachHang`. |
| `ChiTietHoaDon` | `(maHD, soSerial)` | FK `maHD` → `HoaDon`; FK ghép `(soSerial, maDongSP)` → `SanPham`; FK `maKM` → `KhuyenMai`; CHECK `soLuong = 1`; CHECK snapshot; xem 6.2. |
| `PhieuTra` | `maPhieuTra` | FK `nhanVienLap`/`nhanVienDuyet` → `NhanVien`, `hoaDon` → `HoaDon`; CHECK trạng thái, ngày, thông tin duyệt, tiền khách đưa. |
| `ChiTietPhieuTra` | `(maPhieuTra, maHD, soSerial)` | FK `maPhieuTra` → `PhieuTra`; FK ghép `(maHD, soSerial)` → `ChiTietHoaDon`; CHECK `TinhTrangHangTra`, lý do không rỗng. |
| `PhieuDoi` | `maPhieuDoi` | FK `nhanVienLap`/`nhanVienDuyet` → `NhanVien`, `hoaDon` → `HoaDon`; CHECK trạng thái, ngày, thông tin duyệt, tiền khách đưa thêm. |
| `ChiTietPhieuDoi` | `(maPhieuDoi, maHD, soSerial)` | FK `maPhieuDoi` → `PhieuDoi`; FK ghép `(maHD, soSerial)` → `ChiTietHoaDon`; FK `chiTietSanPhamDoi` → `SanPham`; UQ `(maPhieuDoi, chiTietSanPhamDoi)`; CHECK máy thay thế khác máy trả. |
| `SanPhamLoi` | `maLoi` | FK `soSerial` → `SanPham`; FK tùy chọn `maPhieuTra` → `PhieuTra`, `maPhieuDoi` → `PhieuDoi`; CHECK `TrangThaiXuLyLoi`, ngày cập nhật; trigger nguồn; xem 6.4. |

Tất cả khóa ngoại dùng `ON DELETE RESTRICT`; không có `ON DELETE CASCADE`. Tên bảng/cột dùng identifier được quote để giữ đúng camelCase nguồn.

### 6.2. `ChiTietHoaDon` — cấu trúc và snapshot

| Cột | Kiểu | Ràng buộc V1 |
|---|---|---|
| `maHD` | `VARCHAR` | NOT NULL; phần PK; FK tới `HoaDon`. |
| `soSerial` | `VARCHAR` | NOT NULL; phần PK; thành phần FK ghép tới `SanPham`. |
| `maDongSP` | `VARCHAR` | NOT NULL; giữ từ sơ đồ nguồn; thành phần FK ghép `(soSerial, maDongSP)` tới `SanPham(soSerial, maDongSP)`. |
| `soLuong` | `INTEGER` | NOT NULL; CHECK bằng `1` (RB12). |
| `donGiaBanTaiLucBan` | `NUMERIC` | NOT NULL; CHECK `>= 0`; snapshot tại thời điểm bán. |
| `thueSuatTaiLucBan` | `NUMERIC` | NOT NULL; CHECK `0..100`; snapshot thuế suất. |
| `maKM` | `VARCHAR` | NULL; FK tới `KhuyenMai`. |
| `tyLeGiamTaiLucBan` | `NUMERIC` | NULL; nếu có thì CHECK `0..100`; snapshot tỷ lệ giảm. |
| `tienKhuyenMaiThucTe` | `NUMERIC` | NOT NULL; CHECK `>= 0`; snapshot số tiền giảm thực tế. |
| `tienTruocKhuyenMai` | `NUMERIC` | NOT NULL; CHECK `>= 0`; snapshot tiền trước khuyến mãi. |
| `tienSauKhuyenMai` | `NUMERIC` | NOT NULL; CHECK `>= 0` và `<= tienTruocKhuyenMai`; snapshot tiền sau khuyến mãi, trước thuế. |
| `tienThue` | `NUMERIC` | NOT NULL; CHECK `>= 0`; snapshot tiền thuế của chi tiết. |

`SanPham` có UQ `(soSerial, maDongSP)` để làm đích cho FK ghép; điều này bảo đảm `maDongSP` lưu trên chi tiết khớp dòng của máy thực tế (RB22). Các giá trị snapshot không được tính lại từ giá/thuế/khuyến mãi hiện tại (RB42–RB46); việc tính và đối soát thuộc lớp ứng dụng.

### 6.3. Chi tiết trả và chi tiết đổi

- `ChiTietPhieuTra` và `ChiTietPhieuDoi` tham chiếu **chi tiết hóa đơn gốc** bằng khóa ghép `(maHD, soSerial)`, không chỉ bằng `maHD` hoặc `maDongSP`.
- `ChiTietPhieuTra` PK `(maPhieuTra, maHD, soSerial)`; `ChiTietPhieuDoi` PK `(maPhieuDoi, maHD, soSerial)`.
- `ChiTietPhieuDoi.chiTietSanPhamDoi` là serial máy thay thế, FK tới `SanPham(soSerial)`. UQ `(maPhieuDoi, chiTietSanPhamDoi)` chặn dùng lặp một máy thay thế trong cùng phiếu; CHECK `chiTietSanPhamDoi <> soSerial` chặn thay thế chính máy khách mang đổi (RB25).
- Các điều kiện "hóa đơn gốc đã thanh toán", "đúng hóa đơn gốc", "không xử lý trùng" và tổng số lượng đã đổi/trả vẫn là kiểm tra liên bảng của ứng dụng (RB23, RB24, RB52–RB53), không được mô phỏng bằng trigger trong V1.

### 6.4. `SanPhamLoi` và trigger nguồn

`SanPhamLoi` lưu `maLoi` (PK do ứng dụng cấp), `soSerial` bắt buộc (FK tới `SanPham`), `maPhieuTra` và `maPhieuDoi` tùy chọn (FK tới header phiếu trả/phiếu đổi). Cả hai FK nguồn có thể NULL khi hồ sơ lỗi không phát sinh từ đổi/trả; V1 **không** thêm quy tắc loại trừ lẫn nhau.

Trigger `trg_sanphamloi_nguon_hop_le` chạy trước `INSERT` hoặc khi cập nhật `soSerial`/`maPhieuTra`/`maPhieuDoi`:

- Nếu `maPhieuTra` khác NULL: phải tồn tại `ChiTietPhieuTra` cùng `maPhieuTra` và chứa `soSerial`.
- Nếu `maPhieuDoi` khác NULL: phải tồn tại `ChiTietPhieuDoi` cùng `maPhieuDoi` chứa `soSerial` ở **máy trả** (`soSerial`) hoặc **máy thay thế** (`chiTietSanPhamDoi`).
- Không có trigger cho tồn kho, phê duyệt hoặc quy trình giao dịch.

Đây là diễn giải được phê duyệt cho RB27; không thay đổi nội dung RB27 trong mục 2.

### 6.5. Ranh giới và dữ liệu không có trong V1

- Đúng **14 bảng** ở 6.1; không có bảng `products`, không có dữ liệu mẫu/seed, không có baseline tự động.
- Mọi ID là `VARCHAR` do ứng dụng cấp; không có identity, sequence hoặc default cho ID.
- Ngày dùng `DATE`, cờ dùng `BOOLEAN`, số đếm dùng `INTEGER`; tiền và tỷ lệ dùng `NUMERIC` chính xác, không đặt precision/scale tùy ý.
- Không lưu các tổng tiền dẫn xuất của `HoaDon`, `PhieuTra`, `PhieuDoi` hoặc các thuộc tính dẫn xuất `soNgayTonKho`/`doMoi`/`giaBan`; ứng dụng tính theo công thức nguồn.
- Quan hệ `KhuyenMai.maDongSP` → `DongSanPham` là quan hệ cấu trúc theo EER `image8`/sơ đồ lớp `image17` và RB20; phạm vi khuyến mãi **đã chốt là thuộc phạm vi** ngày 2026-10-24 (`C-04`), V1 giữ nguyên cấu trúc này.
- V1 đã quyết định `G-11`/`BR-C12` theo amendment được phê duyệt; xung đột DOCX vẫn được giữ làm bằng chứng lịch sử. `C-04` (phạm vi khuyến mãi) đã giải quyết ngày 2026-10-24; các xung đột `C-07` (đơn vị tồn kho serial hay số lượng) và `C-11` (quy tắc làm tròn) vẫn giữ nguyên. Xem [mô hình lớp V1](../domain/class-model-and-entities.md#8-bản-đồ-v1-đã-phê-duyệt).

### 6.6. Kiểm chứng V1

- Kiểm tra tĩnh: 14 câu lệnh `CREATE TABLE`, không có `products`, không có câu lệnh `INSERT` dữ liệu/seed.
- Đã chạy `psql -X -v ON_ERROR_STOP=1` trên PostgreSQL 16.13 disposable: thành công.
- Đã chạy Flyway 13.10.2 trên CSDL disposable: lần đầu áp dụng V1 (`migrationsExecuted=1`), lần hai không còn migration (`migrationsExecuted=0`).
- Đã kiểm tra trigger: hồ sơ lỗi có serial không thuộc chi tiết trả bị từ chối; hồ sơ lỗi có serial là máy trả hoặc máy thay thế của phiếu đổi được chấp nhận; chi tiết đổi dùng lại chính máy trả bị CHECK từ chối.
- Đã kiểm tra FK `KhuyenMai.maDongSP` → `DongSanPham`: giá trị hợp lệ được chấp nhận; `maDongSP` không tồn tại bị từ chối bởi `fk_khuyenmai_dongsanpham`.
