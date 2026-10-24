# Quy tắc nghiệp vụ liên tính năng và ngoại lệ (Cross-feature Rules & Exceptions)

> **Vai trò:** tài liệu tham chiếu **chéo** cho các quy tắc cắt ngang, ngưỡng, công thức và trạng thái ảnh hưởng nhiều tính năng hoặc cần tra cứu chéo. Không lặp lại bảng FR/NFR, danh mục tác nhân/use case, hoặc luồng chính–ngoại lệ đầy đủ của từng UC.
>
> **Nguồn:** [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx).
> **Định vị:** theo chương/mục/bảng; số trang chưa xác minh.
> **Trạng thái:** chỉ ghi nội dung có trong DOCX; không suy diễn quy tắc mới. Khi nguồn mâu thuẫn, giữ nguyên và trỏ đến mục xung đột.

## 1. Đọc gì ở đâu

| Nhu cầu | Tài liệu canonical |
|---|---|
| Phạm vi, 16 FR, 17 NFR, 14 quy tắc tóm tắt `BR-01`–`BR-14` | [Phạm vi và yêu cầu](../requirements/scope-and-requirements.md) — §3, §4, §5 |
| Xung đột nguồn `C-01`–`C-11` | [Phạm vi và yêu cầu](../requirements/scope-and-requirements.md) — §6 |
| Danh mục 19 UC, tên gọi và tác nhân | [Danh mục use case](../use-cases/catalog.md) |
| Luồng chính/ngoại lệ đầy đủ | [UC001](../use-cases/UC001-login.md) … [UC019](../use-cases/UC019-price-tax-update.md) |
| Thuộc tính, kiểu dữ liệu, enum, công thức thực thể | [Mô hình lớp và thực thể](../domain/class-model-and-entities.md) |
| Ràng buộc `RB01`–`RB66` | [CSDL và toàn vẹn](../database/schema-and-integrity.md) |
| Màn hình và screen flow | [Màn hình](../ui/screen-flow-and-screens.md) |

`BR-01`–`BR-14` trong tài liệu yêu cầu là bản tóm tắt canonical; tài liệu này **không chép lại**. Dưới đây chỉ bổ sung các quy tắc cắt ngang nhiều tính năng, kèm hệ quả và vị trí nguồn.

## 2. Quy tắc cắt ngang nhiều tính năng

| ID | Quy tắc | Tính năng bị ảnh hưởng | Định vị nguồn |
|---|---|---|---|
| XBR-01 | Thanh toán **chỉ bằng tiền mặt**; hóa đơn chỉ hoàn tất khi nhận đủ **100%** giá trị. Không công nợ, trả góp, thẻ, QR, chuyển khoản. | UC003, UC017 | Ch.1 §I.2.b; §II.3; §II.8; Q&A 1 |
| XBR-02 | Đơn giá lưu là **giá trước thuế**; thuế tính riêng theo thuế suất từng sản phẩm. Nhân viên không được sửa đơn giá, thuế suất hoặc tự nhập chiết khấu trên hóa đơn. | UC003, UC006, UC019 | Ch.1 §I.2.b; §II.3; Q&A 4; FR006–FR008 |
| XBR-03 | Đơn giá, thuế suất và khuyến mãi **tại thời điểm giao dịch** phải được lưu cùng chi tiết; thay đổi hiện tại không được làm đổi giá trị hóa đơn/phiếu cũ. | UC003, UC006, UC015–UC017, UC019 | Ch.1 §I.2.b–c; §II.3–§II.4; Ch.4 §I.3 (`HoaDon`, `ChiTietHoaDon`); `RB46`, `RB47` |
| XBR-04 | Chỉ máy **`SAN_SANG_BAN`** thuộc dòng đang kinh doanh mới được bán hoặc giao thay thế; tồn kho không âm; không hai giao dịch cùng bán một máy. | UC003, UC004, UC006, UC008, UC009, UC014 | Ch.3 §III ghi chú UC003; Ch.4 §I.4 (`TrangThaiSanPham`); `RB35`–`RB37`, `RB40` |
| XBR-05 | Đổi/trả phải gắn **một hóa đơn gốc đã thanh toán**; không xử lý hàng không xác định nguồn; tổng lượng đã đổi/trả không vượt lượng đã mua; không hoàn tất trùng một quyền đổi/trả. | UC005, UC006, UC017, UC018 | Ch.1 §I.2.c; §II.4; `RB23`, `RB52`, `RB53` |
| XBR-06 | Hoàn tiền **từ 3.000.000 VNĐ trở lên** phải được Quản lý phê duyệt **trước khi hoàn tất**; nhân viên không tự duyệt. Với phiếu đổi, kiểm tra trên khoản hoàn **sau bù trừ toàn phiếu**. | UC006, UC017, UC018 | Ch.1 §I.2.c; §II.1; §II.4; Q&A 3; `RB54`–`RB58` |
| XBR-07 | Duyệt **chỉ ghi nhận quyết định**, không tự hoàn tiền/giao máy/cập nhật tồn. Phiếu `CHO_DUYET` hoặc `TU_CHOI` không được chuyển `HOAN_TAT`; sửa nội dung sau duyệt phải xác định lại yêu cầu duyệt. | UC006, UC018 | Ch.3 §IV UC018; `RB56`–`RB58` |
| XBR-08 | Hóa đơn đã thanh toán **không được xóa/hủy/đổi cờ** để loại bỏ giao dịch; khách hủy mua phải xử lý qua đổi/trả. | UC003, UC006 | Ch.1 §I.2.b; §II.4; Q&A 5; `RB62` |
| XBR-09 | Không xóa vĩnh viễn dữ liệu có tham chiếu lịch sử: nhân viên nghỉ việc **khóa tài khoản**, sản phẩm/dòng ngừng kinh doanh **đổi trạng thái**; hồ sơ cũ vẫn giữ liên kết. | UC008, UC009, UC013 | Ch.1 §I.2.a; Q&A 7–8; `RB60`, `RB61` |
| XBR-10 | Nghiệp vụ nhiều bước (bán hàng, đổi/trả) phải nhất quán trong **cùng giao dịch CSDL**; một bước thất bại thì hoàn tác toàn bộ, không để trạng thái cập nhật một phần. | UC003, UC006 | NFR009; `RB63`, `RB64` |
| XBR-11 | Doanh thu/doanh số nội bộ tính theo **giá trị trước thuế**, đã điều chỉnh đổi/trả; tiền thuế là **chỉ tiêu riêng**. Chỉ dùng hóa đơn đã thanh toán và phiếu đổi/trả đã hoàn tất; loại phiếu nháp/chờ duyệt/hủy. | UC015, UC016, UC017 | Ch.1 §II.5; FR010, FR012, FR013; `RB65`, `RB66` |
| XBR-12 | Báo cáo tồn kho lâu ngày dùng ngưỡng tham chiếu **trên 60 ngày**; chỉ hỗ trợ quyết định, không tự tạo khuyến mãi. | UC014 | Ch.1 §II.5; FR014; Q&A 6 |
| XBR-13 | Quyền quản lý (nhân viên, giá/thuế, duyệt đổi/trả, báo cáo, sao lưu) tách khỏi quyền bán hàng; kiểm tra quyền tại nơi xử lý nghiệp vụ, không chỉ ở giao diện. | UC001, UC012, UC013, UC018, UC019 | Ch.1 §II.1; NFR002; Ch.4 §II.3.9 |
| XBR-14 | Mã định danh do **ứng dụng** sinh dạng `TIỀN_TỐ-YYYY-NNNNNN`, **bất biến, không trùng, không tái sử dụng**; cấp khi lưu mới thành công. PostgreSQL/V1 không dùng sequence, identity hay default để cấp ID. | UC003, UC006, UC007, UC009–UC013 | Ch.4 §I.3 “Quy ước chung”; `RB02`, `RB03` |
| XBR-15 | Chứng từ bán hàng/đổi/trả sau phát hành **không được tự ý tiêu hủy hoặc sửa nội dung**; phải lưu để tra cứu, bảo hành, đối soát. | UC003, UC005, UC006 | Ch.1 §I.2.f; `RB59`, `RB60` |
| XBR-16 | Mọi phép tính tiền dùng **cùng một quy tắc làm tròn**; tổng chứng từ cộng từ các khoản đã làm tròn ở chi tiết; không dùng giá trị hiển thị đã làm tròn để tính tiếp. | UC003, UC006, UC015–UC017 | NFR017; Ch.4 §I.3 “Tiền tệ”; `RB51` |

> **Bản chất của `XBR`:** đây là **chỉ mục mở rộng theo tính năng cắt ngang**, không phải phiên bản canonical riêng của các quy tắc tóm tắt `BR-01`–`BR-14` trong tài liệu yêu cầu. Khi nguồn hỗ trợ ánh xạ chính xác sang một `BR` tóm tắt thì ghi rõ; trường hợp không có, ghi “không có ánh xạ `BR` chính xác”.

### 2.1. Ánh xạ `XBR` ↔ `BR` tóm tắt (chỉ khi nguồn hỗ trợ)

| XBR | `BR` tóm tắt tương ứng | Ghi chú |
|---|---|---|
| XBR-01 | BR-01 | Thanh toán tiền mặt và hoàn tất khi trả đủ |
| XBR-02 | BR-03 | Giá trước thuế, không tự sửa giá/thuế/chiết khấu |
| XBR-03 | BR-04 | Lưu giá/thuế tại thời điểm bán; riêng phần **khuyến mãi** không có ánh xạ `BR` chính xác (Ch.4 §I.3, `RB46`) |
| XBR-04 | BR-05 | Chỉ `SAN_SANG_BAN` được bán |
| XBR-05 | BR-06 | Đổi/trả gắn hóa đơn gốc |
| XBR-06 | BR-07 | Ngưỡng phê duyệt 3.000.000 VNĐ |
| XBR-07 | — | Không có ánh xạ `BR` chính xác; trạng thái duyệt quy định tại Ch.4 §I.4 và `RB56`–`RB58` |
| XBR-08 | BR-10 | Không xóa/hủy hóa đơn đã thanh toán |
| XBR-09 | BR-13 | Không xóa vĩnh viễn dữ liệu có lịch sử |
| XBR-10 | — | Không có ánh xạ `BR` chính xác; nhất quán giao dịch thuộc NFR009 và `RB63`, `RB64` |
| XBR-11 | BR-12 | Doanh thu/doanh số theo giá trị trước thuế, thuế là chỉ tiêu riêng |
| XBR-12 | BR-11 | Ngưỡng tồn kho lâu ngày trên 60 ngày |
| XBR-13 | — | Không có ánh xạ `BR` chính xác; phân quyền thuộc Ch.1 §II.1 và NFR002 |
| XBR-14 | — | Không có ánh xạ `BR` chính xác; mã tự sinh thuộc Ch.4 §I.3 và `RB02`, `RB03` |
| XBR-15 | — | Không có ánh xạ `BR` chính xác; lưu trữ chứng từ thuộc Ch.1 §I.2.f |
| XBR-16 | — | Không có ánh xạ `BR` chính xác; làm tròn thuộc NFR017 và `RB51` |

## 3. Ngưỡng và công thức

### 3.1. Ngưỡng

| Ngưỡng | Giá trị | Tính chất | Định vị |
|---|---|---|---|
| Phê duyệt hoàn tiền | **≥ 3.000.000 VNĐ** | Bắt buộc; chặn hoàn tất | Ch.1 §I.2.c; §II.1; §II.4; Q&A 3; `RB54` |
| Thanh toán | **100%** giá trị, tiền mặt | Bắt buộc để hoàn tất hóa đơn | Ch.1 §I.2.b; §II.3; Q&A 1 |
| Tồn kho lâu ngày | **trên 60 ngày** | Tham chiếu, không phải chặn nghiệp vụ | Ch.1 §II.5; FR014; Q&A 6 |
| Thời hạn đổi/trả | Không có số ngày cụ thể trong nguồn | Phải theo chính sách đã phê duyệt | Ch.1 §I.2.c; `RB34` |
| Hệ số độ mới `kNgay`, `kSua`, `kDoiTra` | Không có giá trị trong nguồn | Đề xuất, chờ cửa hàng phê duyệt | Ch.4 §I.3 (`SanPham`) |

> **Làm rõ nguồn (không phải mâu thuẫn):** FR014 nêu ngưỡng “như trên 60 ngày” với tính chất **tham chiếu**; UC014 cho Quản lý **tự nhập ngưỡng số ngày tồn kho** và nguồn **không quy định giá trị mặc định**. Xem [Phạm vi và yêu cầu — FR014](../requirements/scope-and-requirements.md).

### 3.2. Công thức

Hai nhóm công thức cùng tồn tại; **Ch.4 mở rộng Ch.1 bằng khuyến mãi**, không thay thế định nghĩa giá trước thuế.

| Phạm vi | Công thức | Định vị |
|---|---|---|
| Ch.1 (bán hàng) | `Thành tiền trước thuế = Đơn giá trước thuế × Số lượng`; `Tiền thuế = Thành tiền trước thuế × Thuế suất / 100`; `Thành tiền sau thuế = Thành tiền trước thuế + Tiền thuế` | Ch.1 §I.2.b |
| Ch.4 (chi tiết) | `tienTruocKhuyenMai = soLuong × đơnGiáTrướcThuếTạiLúcBán`; `tienSauKhuyenMai = tienTruocKhuyenMai − tiềnKhuyếnMãiThựcTế`; `tiềnKhuyếnMãiThựcTế = làmTròn(tienTruocKhuyenMai × tyLeGiam / 100)` | Ch.4 §I.3 (`ChiTietHoaDon`) |
| Ch.4 (hóa đơn) | `tongTienTruocThue = Σ tienSauKhuyenMai`; `tongTienThue = Σ làmTròn(Sᵢ × rᵢ / 100)`; `tongTienThanhToan = tongTienTruocThue + tongTienThue` | Ch.4 §I.3 (`HoaDon`); `RB44`, `RB45` |
| Hoàn tiền | `tienHoanTra = tiềnSauKhuyếnMãiGốcCủaMáy + tiềnThuếGốcCủaMáy`; `tongTienHoanTra = Σ tienHoanTra` | Ch.4 §I.3 (`ChiTietPhieuTra`, `PhieuTra`); `RB47` |
| Chênh lệch đổi | `tienChenhLech = giáTrịMáyThayThếSauThuế − giáTrịĐãThanhToánGốc`; `tongTienChenhLech = Σ tienChenhLech`; `tongTienThuThem = max(0, tongTienChenhLech)`; khoản hoàn `= max(0, −tongTienChenhLech)` | Ch.4 §I.3 (`ChiTietPhieuDoi`, `PhieuDoi`); `RB48`, `RB49` |

> Ch.1 không nêu khuyến mãi trong công thức; Ch.4 đưa khuyến mãi vào `tienSauKhuyenMai`. Xem mục 6.

> **Quy ước `làmTròn` (đã chốt 2026-10-24, xem `C-11`):** đơn vị đồng, **0 chữ số thập phân**, chế độ `HALF_UP`, dùng `BigDecimal`; làm tròn ở cấp chi tiết rồi cộng thành tổng; DB lưu giá trị đã tròn; hiển thị dạng `1.250.000 VND`.

## 4. Trạng thái và chuyển trạng thái

| Kiểu | Giá trị hợp lệ | Ảnh hưởng liên tính năng |
|---|---|---|
| `TrangThaiSanPham` | `SAN_SANG_BAN`, `DA_BAN`, `HANG_LOI`, `DANG_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `DA_THANH_LY` | Chỉ `SAN_SANG_BAN` được bán/giao thay thế (XBR-04); máy lỗi không cộng tồn bán |
| `TrangThaiXuLyPhieu` | `NHAP`, `CHO_XU_LY`, `HOAN_TAT`, `HUY` | `HOAN_TAT` phải có `ngayHoanTat`; phiếu hủy không tính đã xử lý |
| `TrangThaiDuyet` | `KHONG_CAN_DUYET`, `CHO_DUYET`, `DA_DUYET`, `TU_CHOI` | `DA_DUYET` không đồng nghĩa đã hoàn tiền; `CHO_DUYET`/`TU_CHOI` không được `HOAN_TAT` |
| `TinhTrangHangTra` | `NGUYEN_VEN`, `HANG_LOI` | Quyết định nhập lại tồn hay tạo hồ sơ lỗi |
| `TrangThaiXuLyLoi` | `CHO_XU_LY`, `DA_GUI_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `THANH_LY` | Phải đồng bộ với trạng thái máy và tồn bán |
| `LoaiQuyen` | `NHAN_VIEN_QUAN_LY`, `NHAN_VIEN_BAN_HANG` | Phân quyền XBR-13 |

Định vị: Ch.4 §I.4; tập giá trị lặp tại Ch.4 §II.3.2 (`RB16`). Ý nghĩa từng giá trị: xem [Mô hình lớp và thực thể](../domain/class-model-and-entities.md).

## 5. Nhóm ngoại lệ cắt ngang

Các nhóm dưới đây gom ngoại lệ **xuất hiện ở nhiều UC**. Luồng chi tiết và thứ tự bước xem tệp UC tương ứng.

| ID | Nhóm ngoại lệ | Hệ quả nghiệp vụ chung | UC liên quan |
|---|---|---|---|
| EX-01 | Thiếu/sai dữ liệu đầu vào (trường bắt buộc, định dạng, số âm, ngày không hợp lệ, giá/thuế ngoài miền) | Chặn lưu; giữ dữ liệu đã nhập để sửa lại | UC001, UC007–UC014, UC017, UC019 |
| EX-02 | Xác thực/phân quyền thất bại (sai mật khẩu, tài khoản khóa, sai quyền) | Không tạo phiên hoặc chặn chức năng | UC001, UC012, UC013, UC018, UC019 |
| EX-03 | Tham chiếu không tồn tại hoặc không khớp (hóa đơn gốc, máy, chứng từ, danh mục, dòng sản phẩm) | Từ chối xử lý; không tạo bản ghi mồ côi | UC003, UC006, UC008, UC011, UC018 |
| EX-04 | Trạng thái không cho phép (máy không sẵn sàng, phiếu đã hoàn tất/hủy/đã xử lý, đã bị từ chối) | Chặn chuyển trạng thái; không ghi đè quyết định cũ | UC003, UC006, UC011, UC018 |
| EX-05 | Trùng lặp / xử lý lặp (serial, mã, tên đăng nhập, khách theo SĐT, cùng máy trong một phiếu) | Không tạo bản ghi trùng; đề nghị dùng hồ sơ hiện có | UC006, UC007–UC013 |
| EX-06 | Thiếu tồn hoặc không đủ số lượng | Không hoàn tất bán/giao thay thế; tồn không âm | UC003, UC006 |
| EX-07 | Vượt ngưỡng phê duyệt hoặc chưa có quyết định duyệt | Lưu phiếu chờ duyệt; **chưa hoàn tiền, chưa hoàn tất** | UC006, UC018 |
| EX-08 | Lỗi ghi dữ liệu / lỗi kết nối khi hoàn tất | Hoàn tác toàn bộ giao dịch; không để cập nhật một phần | UC003, UC006, UC007–UC013, UC019 |
| EX-09 | Thời gian không hợp lệ hoặc xung đột hiệu lực (khoảng ngày, thời hạn đổi/trả, chồng lấn khuyến mãi) | Chặn hoặc yêu cầu điều chỉnh trước khi lưu | UC006, UC008, UC012, UC015–UC017 |
| EX-10 | Kết xuất báo cáo thất bại | Giữ báo cáo trên màn hình; thông báo lỗi | UC017 |

## 6. Xung đột nguồn và nội dung bị cắt

Không tự dung hòa. Các xung đột phạm vi/vai trò đã được lập mã `C-01`–`C-12` tại [Phạm vi và yêu cầu §6](../requirements/scope-and-requirements.md#6-xung-đột-và-điểm-chưa-xác-nhận); dưới đây chỉ bổ sung các điểm **trực tiếp ảnh hưởng quy tắc** và lỗi văn bản.

| Mã | Nội dung | Định vị | Trạng thái |
|---|---|---|---|
| BR-C01 | Công thức Ch.1 không có khuyến mãi; Ch.4 tính `tienSauKhuyenMai` trước thuế. | Ch.1 §I.2.b vs Ch.4 §I.3 | **Đã chọn công thức Ch.4 cho hóa đơn có khuyến mãi (2026-10-24, theo quyết định `C-04`)** — khớp `RB42`–`RB46` và V1 `ChiTietHoaDon`; `làmTròn` theo `C-11` (VND, 0 chữ số thập phân, `HALF_UP`) |
| BR-C02 | Ch.1 nói “mỗi sản phẩm có một mức thuế suất”; Ch.4 đặt `thue` trên `DongSanPham`. | Ch.1 §II.3 vs Ch.4 §I.3 | Khác mức hạt dữ liệu; cần xác nhận cấp lưu thuế suất |
| BR-C03 | Ngưỡng duyệt tính trên “số tiền hoàn”; `RB54` quy định với phiếu đổi kiểm tra sau bù trừ toàn phiếu. | Ch.1 §I.2.c vs Ch.4 §II.3.7 | Cần xác nhận cách tính khi có nhiều chi tiết âm/dương |
| BR-C04 | Không có quy tắc làm tròn cụ thể (đơn vị, thứ tự) dù yêu cầu thống nhất. | NFR017; `RB51` vs toàn bộ công thức | **Đã chốt 2026-10-24:** VND, 0 chữ số thập phân, `HALF_UP`, một điểm làm tròn (`TienUtil`), tròn ở cấp chi tiết rồi cộng; DB lưu giá trị đã tròn; hiển thị dạng `1.250.000 VND`. Xem `C-11`. |
| BR-C05 | UC007, luồng 8.a.1 bị cắt giữa câu: “Phát hiện số điện thoạ”. | Ch.3 §IV UC007 | Văn bản nguồn không đầy đủ; không suy đoán |
| BR-C06 | UC017 bỏ qua bước `10.a.4` trong luồng xuất Excel. | Ch.3 §IV UC017 | Thiếu số bước; nội dung 10.a.3 → 10.a.5 vẫn đọc được |
| BR-C07 | Luồng chính nguồn của UC002 có các dòng **không được đánh số bước**, và UC003 có một dòng **thiếu nhãn số bước**; **không phải** các bước bị trùng số. Giữ nguyên luồng nguồn, không đánh số lại. Xem [UC002](../use-cases/UC002-logout.md) và [UC003](../use-cases/UC003-retail-invoice.md). | Ch.3 §IV UC002, UC003 | Lỗi trình bày/văn bản nguồn; không đổi nghĩa |
| BR-C08 | Khuyến mãi vừa bị loại khỏi phạm vi vừa có UC012 và được áp dụng ở UC003. | Ch.1 §II.6, §II.8 vs Ch.3 §III–IV | **Đã giải quyết phạm vi (2026-10-24):** khuyến mãi cơ bản thuộc phạm vi (UC012 quản lý + UC003 bước 7–8 áp dụng trên từng dòng sản phẩm); voucher/tích điểm/khuyến mãi chuyên sâu vẫn ngoài phạm vi; DOCX không sửa. Xem `C-04`. |
| BR-C09 | Tác nhân UC008–UC011 khác nhau giữa danh mục, đặc tả và mô tả tác nhân. | Ch.3 §II, §III, §IV; Ch.1 §II.1 | Mâu thuẫn ba chiều; xem `C-03`, `G-09` |
| BR-C10 | Ai được cập nhật giá/thuế: nhân viên bán hàng theo Ch.1/FR006 hay Quản lý theo UC019. | Ch.1 §II.1.b; FR006; Ch.3 §IV UC019 | Mâu thuẫn vai trò; xem `C-08`, `G-08` |
| BR-C11 | Ch.3 §IV UC009/UC010/UC012/UC013 yêu cầu **nhập mã định danh**; Ch.4 §I.3 và `RB02`/`RB03` quy định mã **tự sinh, bất biến, không tái sử dụng**. | Ch.3 §IV UC009, UC010, UC012, UC013 vs Ch.4 §I.3; Ch.4 §II.3.1 (`RB02`, `RB03`) | **Đã giải quyết cho implementation hiện tại (2026-10-20):** mã do ứng dụng sinh, giữ đúng định dạng tài liệu hóa; không dùng DB sequence/identity/default. Xung đột nguồn (bước nhập tay trong DOCX) vẫn được giữ làm bằng chứng; xem `C-12`, `G-10`. |
| BR-C12 | Mô hình văn bản `ChiTietHoaDon` ghi `sanPham: DongSanPham`, `soLuong` = 1 và các khoản dẫn xuất; `RB22` cần xác định **máy thực tế** và `RB46` cần **lưu giá/thuế/khuyến mãi tại thời điểm giao dịch**. Bằng chứng trực quan ngày 2026-10-19 (bản sao byte-identical sơ đồ lớp/EER/sơ đồ HQT CSDL): sơ đồ lớp thể hiện `hoaDon`, `sanPham: DongSanPham`, `soLuong`, `/tienTruocKhuyenMai`, `/tienSauKhuyenMai`, **không thấy trường serial/snapshot nào được vẽ trực tiếp**; EER thể hiện `maHD`, `maDongSP`, `soLuong`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, **không có cột serial/giá thực tế/thuế/khuyến mãi snapshot tường minh**. Không suy đoán thêm cột/quan hệ ngoài phần thấy được. | Ch.4 §I.3 (`ChiTietHoaDon`) vs Ch.4 §II.3.3 (`RB22`), §II.3.6 (`RB46`); ảnh [image17.png](../assets/diagrams/image17.png), [image6.png](../assets/diagrams/image6.png), [image8.png](../assets/diagrams/image8.png) | **Xung đột nguồn được giữ làm bằng chứng lịch sử; implementation hiện tại theo V1 amendment đã duyệt (2026-10-20)** — xem [domain §8](../domain/class-model-and-entities.md#8-bản-đồ-v1-đã-phê-duyệt), [database §6](../database/schema-and-integrity.md#6-bản-đồ-ddl-v1-đã-phê-duyệt); `RB22`/`RB46` giữ nguyên câu chữ. |

> **Điều hướng sổ đăng ký:** bảng nối `C-xx` ↔ `G-xx` ↔ `BR-Cxx` nằm tại [Phạm vi và yêu cầu — §7](../requirements/scope-and-requirements.md). Ô chưa có mã tương ứng được ghi “—”, nghĩa là **chưa lập hoặc chưa xác nhận**, không suy diễn quan hệ.

## 7. Bản đồ UC → quy tắc → luồng đầy đủ

| UC | Tệp luồng đầy đủ | Quy tắc chính |
|---|---|---|
| UC001 | [UC001-login.md](../use-cases/UC001-login.md) | XBR-13, EX-01, EX-02 |
| UC002 | [UC002-logout.md](../use-cases/UC002-logout.md) | XBR-13 |
| UC003 | [UC003-retail-invoice.md](../use-cases/UC003-retail-invoice.md) | XBR-01–XBR-04, XBR-08, XBR-15, XBR-16 |
| UC004 | [UC004-product-inventory-lookup.md](../use-cases/UC004-product-inventory-lookup.md) | XBR-04 |
| UC005 | [UC005-customer-transaction-lookup.md](../use-cases/UC005-customer-transaction-lookup.md) | XBR-05, XBR-15 |
| UC006 | [UC006-return-exchange.md](../use-cases/UC006-return-exchange.md) | XBR-03, XBR-05–XBR-08, XBR-10, XBR-14–XBR-16 |
| UC007 | [UC007-customer-management.md](../use-cases/UC007-customer-management.md) | XBR-14, EX-05 |
| UC008 | [UC008-product-management.md](../use-cases/UC008-product-management.md) | XBR-04, XBR-09, EX-05 |
| UC009 | [UC009-product-line-management.md](../use-cases/UC009-product-line-management.md) | XBR-04, XBR-09, XBR-14 |
| UC010 | [UC010-product-category-management.md](../use-cases/UC010-product-category-management.md) | XBR-09, XBR-14, EX-05 |
| UC011 | [UC011-defective-product-management.md](../use-cases/UC011-defective-product-management.md) | XBR-04, XBR-14, EX-03–EX-05 |
| UC012 | [UC012-promotion-management.md](../use-cases/UC012-promotion-management.md) | XBR-13, XBR-14, EX-09 |
| UC013 | [UC013-sales-staff-management.md](../use-cases/UC013-sales-staff-management.md) | XBR-09, XBR-13, XBR-14, EX-05 |
| UC014 | [UC014-inventory-report.md](../use-cases/UC014-inventory-report.md) | XBR-04, XBR-12, EX-01 |
| UC015 | [UC015-sales-by-product.md](../use-cases/UC015-sales-by-product.md) | XBR-03, XBR-11, XBR-16 |
| UC016 | [UC016-sales-by-employee.md](../use-cases/UC016-sales-by-employee.md) | XBR-03, XBR-11, XBR-16 |
| UC017 | [UC017-revenue-statistics.md](../use-cases/UC017-revenue-statistics.md) | XBR-05, XBR-06, XBR-11, XBR-16, EX-10 |
| UC018 | [UC018-return-approval.md](../use-cases/UC018-return-approval.md) | XBR-05–XBR-07, XBR-13 |
| UC019 | [UC019-price-tax-update.md](../use-cases/UC019-price-tax-update.md) | XBR-02, XBR-03, XBR-13 |

## 8. Checklist

- [x] Quy tắc cắt ngang có ID, hệ quả và định vị nguồn.
- [x] Ngưỡng 3.000.000 VNĐ, 100% tiền mặt, 60 ngày; nêu rõ tính chất từng ngưỡng.
- [x] Công thức Ch.1 và Ch.4, kèm khác biệt khuyến mãi.
- [x] 6 kiểu trạng thái và ảnh hưởng liên tính năng.
- [x] 10 nhóm ngoại lệ cắt ngang, không lặp luồng từng UC.
- [x] Bản đồ 19 UC → tệp luồng đầy đủ.
- [x] Xung đột và nội dung bị cắt được nêu đúng, có mã tham chiếu.
- [x] Không chép lại bảng FR/NFR, danh mục tác nhân/UC, đặc tả UC.
