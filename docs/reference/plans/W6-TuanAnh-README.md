# W6 — Tuấn Anh · Kiểm tra dữ liệu nhập trên giao diện

**Nhánh làm việc** `feature/anh-w6`  
**Phạm vi** D1–D5 và N1–N3 theo `anh-w6-plan.md`.  
**Căn cứ** [Phân công nhóm §5.4](W6-phan-cong-import-excel-va-khuyen-mai-nhom.md#54-tuấn-anh) và [báo cáo phân tích, thiết kế gốc](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx).

Phân công chung ghi Tuấn Anh không có hạng mục bắt buộc. Phần việc này đến từ lời giao bổ sung của trưởng nhóm, được chép trong plan cá nhân.

> còn TA thì wave này ông sửa lại lỗi validate form input trên UI, đừng refactor UI gì hết, sửa lại những bug validate form như mấy chỗ nào chọn date thì không được nhập chữ hay số, buộc phải nhập ngày hoặc chọn trên calendar, quét hết nhan.

## 1. Thay đổi cần review

| Mục | Màn hình / đặc tả | Trước | Sau |
|---|---|---|---|
| D1 | `DatePicker` dùng chung | Ô nhập nhận ký tự tự do và không báo lỗi khi rời ô | Chỉ nhận tiền tố ngày hợp lệ. Gõ `09112026` thành `09/11/2026`. Dán được `ddMMyyyy` và ISO hợp lệ. Ngày không tồn tại báo lỗi khi rời ô. Sửa nội dung hoặc chọn trên lịch sẽ xóa lỗi cũ. |
| D2 | Quản lý sản phẩm · [UC008 – Thêm sản phẩm](../use-cases/UC008-product-management.md), RB29 | Ngày sai bị đọc thành `null` rồi dùng ngày mặc định hoặc ngày cũ | Chặn trước khi gọi controller và đưa focus về ô ngày. Khi chủ động để trống, controller vẫn lấy hôm nay lúc thêm hoặc giữ ngày cũ lúc sửa, theo plan. |
| D3 | Ghi nhận sản phẩm lỗi · [UC011 – Thêm sản phẩm lỗi](../use-cases/UC011-defective-product-management.md) | Ô văn bản ISO thiếu lịch | Dùng `DatePicker`, hiện hôm nay mặc định. Ngày sai hoặc trong tương lai bị chặn. Để trống dùng hôm nay. |
| D4 | Chọn kỳ doanh thu · [UC017 – Thống kê doanh thu](../use-cases/UC017-revenue-statistics.md) | Spinner có thể áp dụng ngày cũ nếu phần vừa gõ không hợp lệ | Đọc trực tiếp hai ô ngày. Chặn ngày trống, ngày sai và kỳ đảo ngược. Cả hai ô có lịch. |
| D5 | Thêm/sửa nhân viên · [UC013 – Thêm nhân viên bán hàng](../use-cases/UC013-sales-staff-management.md) | Thông báo khi lưu chưa phân biệt bỏ trống và sai định dạng | Ô trống nhắc nhập hoặc chọn ngày. Ngày sai dùng thông báo chuẩn. Quy tắc ngày vào làm vẫn do controller kiểm tra. |
| N1 | Sao lưu · [FR015](../backup-restore-workflows.md) | Ô chu kỳ nhận chữ | Chỉ nhận tối đa 3 chữ số. Kiểm tra 1–168 giờ khi lưu. |
| N2 | Sao lưu · [FR015](../backup-restore-workflows.md) | Ô giờ nhận chữ. Formatter mặc định chấp nhận `24:00` và chuyển thành `00:00` | Gõ/dán `0830` thành `08:30`. Khi lưu, kiểm tra chặt giờ 00–23 và phút 00–59, dùng thông báo hiện có. |
| N3 | [UC014 – Báo cáo sản phẩm tồn kho lâu ngày](../use-cases/UC014-inventory-report.md) | Ô ngưỡng nhận chữ | Chỉ nhận tối đa 6 chữ số. Kiểm tra số nguyên dương khi áp dụng. |

Bộ lọc kỳ báo cáo dùng chung ở UC015 và UC016 tự nhận bộ lọc ký tự từ `DatePicker`. Bước áp dụng ở hai màn này vẫn đọc `dd/MM/yyyy` đủ hai chữ số ngày/tháng qua `DateRange`. Ở các màn đọc trực tiếp qua `DatePicker.getDate()`, ngày ngắn như `1/2/2026` được chấp nhận.

Đặc tả UC008 nêu trường hợp thiếu thông tin bắt buộc hoặc ngày nhập kho sai. Plan cá nhân chốt giữ cách xử lý hiện tại khi để trống ngày nhập kho. Guard trong bản này chỉ xử lý giá trị ngày có nội dung nhưng không hợp lệ.

### Điều chỉnh khi kiểm thử

- `PeriodDialog` dùng thông báo ngắn riêng dưới từng ô. Với thông báo dài theo plan, ô ngày còn lại chỉ hiển thị 22 px trong phép thử. Sau khi rút gọn, hai ô còn 236 px và 178 px khi ô đầu báo lỗi. Giữ bố cục hai cột và chiều rộng hộp thoại 480.
- `BackupPage` chuyển formatter hiện có sang `ResolverStyle.STRICT` để thực thi giới hạn giờ trong plan. Giá trị cấu hình vẫn hiển thị theo `HH:mm`.
- Không truy cập được frame Figma qua công cụ hiện có. Đã mở [ảnh Thống kê doanh thu từ tài liệu gốc](../assets/screens/image14.png) và dựa vào các component Swing hiện có để đối chiếu phần giao diện.

## 2. Tệp trong phạm vi

| Tệp | Lý do thay đổi |
|---|---|
| `ui/component/DatePicker.java` | D1. Bộ lọc thao tác gõ/dán, lỗi khi rời ô, API đọc trạng thái và bật/tắt lịch. |
| `ui/page/product/ProductPage.java` | D2. Guard ở đầu `luuMay()`. Tách commit để dễ tích hợp với Quý. |
| `ui/page/defective/AddDefectiveProductDialog.java` | D3. Đổi riêng ô ngày phát hiện sang lịch dùng chung. |
| `ui/page/report/PeriodDialog.java` | D4. Thay hai spinner bằng lịch và kiểm tra nội dung trước khi xác nhận. |
| `ui/page/employee/AddEmployeeDialog.java` | D5. Thống nhất thông báo ngày vào làm. |
| `ui/component/InputFilters.java` | P2. Bộ lọc số nguyên và giờ/phút. |
| `ui/page/backup/BackupPage.java` | N1–N2. Gắn bộ lọc và kiểm tra giờ hợp lệ lúc lưu. |
| `ui/page/report/LongStockReportPage.java` | N3. Gắn bộ lọc cho ngưỡng ngày. |
| `DatePickerTest.java`, `InputFiltersTest.java` trong `src/test/java/com/dtaq/retail/ui/component/` | Sáu test JUnit cho thao tác nhập, dán, xóa, rời ô và bật/tắt. |
| `docs/reference/plans/W6-TuanAnh-README.md` | Bàn giao kết quả và các điểm cần review. |

Các đường dẫn bắt đầu bằng `ui/` nằm dưới `src/main/java/com/dtaq/retail/`.

## 3. Kiểm thử

| Kiểm tra | Kết quả |
|---|---|
| `mvn -q -DskipTests compile` | Exit 0. Maven dùng JDK 25 với `--release 21` theo `pom.xml`. |
| Suite tập trung với `-Djava.awt.headless=true` | Exit 0. 18 test chạy, gồm 6 test mới. `UiLayoutSmokeTest` cần màn hình và không chạy trong lượt headless. |
| `mvn -q clean test` với màn hình và PostgreSQL 18.3 tạm, locale `C.UTF-8`, Flyway V1–V15 | Exit 0. **665 test, 0 failures, 0 errors, 0 skipped** theo báo cáo Surefire. Có 4 ca `UiLayoutSmokeTest`. Ca `SanPhamControllerTest.capNhatMay_thanhCong` cũng qua với trạng thái mã nguồn hiện tại. |
| Smoke test Swing với cửa sổ thật | Mở lịch, chọn “Hôm nay”, chặn kỳ trống/sai/đảo ngược, nhận kỳ đúng. Hộp thoại ghi nhận sản phẩm lỗi chặn ngày sai và tương lai, dùng hôm nay khi để trống. |

```text
Successfully applied 15 migrations to schema "public", now at version v15
Date error visible width 236 / preferred 134, date fields 236 px and 178 px
PeriodDialog + AddDefectiveProductDialog smoke OK
```

Lượt đầu trên CSDL tạm rỗng gặp lỗi bảng chưa tồn tại. Lượt có migration nhưng locale `C` gặp `DanhMucControllerTest.themMoi_loiTrungTen:107 expected: <false> but was: <true>`. Đối chiếu SQL cho thấy `lower('MỤC')` trả `mỤc` với locale `C` và `mục` với `C.UTF-8`. Lượt cuối chạy trên CSDL tạm mới, áp dụng migration trước và dùng locale `C.UTF-8`. Cấu hình CSDL trong workspace được giữ nguyên.

### Lệnh chạy lại

```bash
mvn -q -DskipTests compile
mvn -q -Djava.awt.headless=true -Dtest='DatePickerTest,InputFiltersTest,KyBaoCaoFilterTest,LongStockThresholdTest,UiLayoutSmokeTest,ProductPageTest,DefectiveProductsPageTest' test
mvn -q clean test
```

Lượt toàn bộ cần CSDL thử nghiệm đã chạy migration và môi trường có màn hình để chạy cả test bố cục.

### Checklist kiểm tra trên ứng dụng

| Màn | Thao tác và kết quả mong đợi |
|---|---|
| Quản lý sản phẩm | Gõ chữ bị chặn. Gõ `31022026`, Tab ra thấy lỗi, bấm Lưu không ghi. Chọn lại bằng lịch, hoặc dán `2026-11-01` thành `01/11/2026`. |
| Thêm/sửa nhân viên | Ngày sai báo lỗi dưới ô. Xóa trống rồi Lưu thấy thông báo yêu cầu nhập hoặc chọn ngày vào làm. |
| Ghi nhận sản phẩm lỗi | Ngày mặc định là hôm nay. Chọn ngày tương lai hoặc nhập ngày sai thì không gửi biểu mẫu. Xóa trống gửi ngày hôm nay. |
| Thống kê doanh thu | Mở lịch từ hai ô. Ngày trống/sai giữ hộp thoại mở. Từ ngày lớn hơn đến ngày báo lỗi ở ô Đến ngày. Cùng ngày được áp dụng. |
| Thống kê theo sản phẩm/nhân viên | Chữ bị chặn. Dùng định dạng `dd/MM/yyyy` đủ hai chữ số ngày và tháng khi áp dụng kỳ. |
| Sao lưu | `0830` thành `08:30`. `24:00` và `25:00` báo lỗi khi lưu. Chu kỳ chỉ nhận tối đa 3 chữ số và chỉ lưu từ 1 đến 168. |
| Báo cáo tồn lâu ngày | Ngưỡng chỉ nhận tối đa 6 chữ số. Áp dụng ngưỡng trống hoặc 0 báo lỗi như trước. |

Smoke test đã kiểm trực tiếp hai hộp thoại nêu trên. Checklist đầy đủ qua các màn của ứng dụng chưa được thực hiện thủ công.

## 4. Bàn giao

| Người | Nội dung |
|---|---|
| Tài | `AddPromotionDialog` hiện có hai ô ngày ISO nhập tự do và báo sai ngày kết thúc ở ô bắt đầu. Khi làm UC012, có thể dùng `DatePicker` cho cả hai ô, báo lỗi đúng trường và giữ kiểm tra thứ tự ngày. Ô tỷ lệ cần xử lý số thập phân theo UC012. |
| Quý | Giữ guard đầu `ProductPage.luuMay()` cùng các nút import/tải mẫu khi tích hợp. Guard cần commit riêng theo plan. Chưa có hash vì Tuấn Anh sẽ tự chạy Git. |
| Quý | `ProductLinesPage` và `PriceTaxPage` dùng ô tiền cho thuế suất. Ở `ProductLinesPage`, giá trị thuế thập phân được nạp qua `toPlainString()`; ví dụ `8.5` có thể thành `85` qua bộ lọc ô tiền. Ghi nhận để review riêng. |
| Đăng | `BackupPage` gắn bộ lọc cho hai ô cấu hình và bật kiểm tra chặt `HH:mm`. Có thể dùng các ca N1–N2 trong checklist để review. |
