# anh-w6-plan — Sửa lỗi validate form input trên UI (Tuấn Anh, Wave 6)

> File này là **bản giao việc tự đủ ngữ cảnh** cho một model khác thực thi. Model thực thi KHÔNG có
> lịch sử hội thoại trước đó — mọi thứ cần biết nằm trong file này. Đọc hết trước khi sửa code.
> File này bị git ignore (`anh-w6-plan.md` trong `.gitignore`), **không commit file này**.

---

## 0. TL;DR cho model thực thi

- Bạn đóng vai **Tuấn Anh** (git user `nghoangtuanhanhdev`), làm trên nhánh **`feature/anh-w6`** (đã checkout sẵn).
- Nhiệm vụ duy nhất (do trưởng nhóm Đăng giao): **sửa lỗi validate form input trên UI**, đặc biệt các ô
  **ngày**: không được gõ chữ / số lung tung, **buộc phải nhập đúng một ngày hợp lệ hoặc chọn trên lịch**.
  "Quét hết" các form.
- **KHÔNG refactor UI** (không đổi layout, màu, font, bố cục, tên nút, thứ tự trường, không đổi kiến trúc).
  Chỉ sửa đúng chỗ validate. Thay một ô text ngày bằng component `DatePicker` *đã có sẵn trong dự án*
  được coi là sửa validate (vì yêu cầu là "chọn trên calendar"), không phải refactor.
- **KHÔNG đụng phần của người khác trong W6** (Tài: khuyến mãi; Quý: import Excel sản phẩm/dòng SP;
  Đăng: migration/Excel util/navigation). Danh sách file cấm ở §3.
- Thứ tự làm: P1 (bắt buộc) → P2 (nên làm) → Test → Báo cáo → Commit. Chi tiết ở §5–§9.

---

## 1. Bối cảnh dự án

- **DTAQ Retail**: ứng dụng desktop quản lý siêu thị điện máy (đồ án môn Phát triển ứng dụng, IUH).
- Stack: **Java 21**, Swing + **FlatLaf** + **MigLayout**, PostgreSQL (JDBC + HikariCP), Flyway, JUnit 5, Maven.
  Không có thư viện date-picker ngoài (không có JCalendar/LGoodDatePicker) — **không được thêm dependency**,
  không sửa `pom.xml`.
- Gói gốc: `src/main/java/com/dtaq/retail/`
  - `ui/component/` — UI kit dùng chung (`FormField`, `DatePicker`, `ModalDialog`, `KyBaoCaoFilter`, …)
  - `ui/page/<module>/` — các trang/dialog
  - `ui/format/` — định dạng hiển thị (`DateFormats.DATE` = `dd/MM/yyyy`, `MoneyFormat`)
  - `util/DateRange.java` — parse kỳ báo cáo `dd/MM/uuuu` STRICT
  - `controller/` — nghiệp vụ, KHÔNG import `javax.swing`
- Quy ước code: thụt lề **2 space**, tên biến/hàm/Javadoc/thông báo lỗi **tiếng Việt có dấu** (theo code hiện có,
  ví dụ `ngayPhatHien`, `kiemTraDinhDang`), hiển thị ngày theo **`dd/MM/yyyy`**, tuần bắt đầu thứ Hai.
- Tài liệu dự án: `AGENTS.md` (quy tắc cho AI agent — đọc §3 Scope Control), `docs/reference/README.md`,
  phân công W6: `docs/reference/plans/W6-phan-cong-import-excel-va-khuyen-mai-nhom.md`.

### 1.1. Lời giao việc gốc của trưởng nhóm (nguyên văn)

> "còn TA thì wave này ông sửa lại lỗi validate form input trên UI, đừng refactor UI gì hết, sửa lại những bug
> validate form như mấy chỗ nào chọn date thì không được nhập chữ hay số, buộc phải nhập ngày hoặc chọn trên
> calendar, quét hết nhan."

Diễn giải đã chốt:
1. Ô ngày: chặn ký tự chữ ngay khi gõ/dán; chỉ cho phép chuỗi có dạng ngày `dd/MM/yyyy` (tự chèn `/`);
   rời ô mà chưa phải ngày hợp lệ → hiện lỗi đỏ dưới ô; bấm lưu với ngày sai → **chặn lưu**, không được
   âm thầm thay bằng ngày khác.
2. Mọi ô ngày phải có nút lịch (dùng `DatePicker`).
3. Ô số thuần (số giờ, số ngày) chặn chữ khi gõ (P2).

### 1.2. Vị trí của Tuấn Anh trong tài liệu W6

Tài liệu phân công W6 (§4.1, §5.4) ghi Tuấn Anh "không có hạng mục bắt buộc". Trưởng nhóm đã giao bổ sung
qua chat (lời nhắn ở §1.1). Vì vậy:
- Không sửa tài liệu W6 của nhóm (`W6-phan-cong-...md`) — đó là file Đăng quản lý.
- Ghi kết quả vào báo cáo riêng `docs/reference/plans/W6-TuanAnh-README.md` (§8).

---

## 2. Trạng thái git hiện tại (đọc kỹ để không commit nhầm)

- Nhánh: `feature/anh-w6`, đã up to date với `origin/feature/anh-w6`. Nhánh chính tích hợp: `develop`.
- Working tree có sẵn thay đổi **KHÔNG phải của task này**:
  - `M src/test/java/com/dtaq/retail/controller/SanPhamControllerTest.java` — liên quan việc Q6-06 của Quý.
    **Không sửa, không revert, không stage.**
  - `?? setup-db.sh` — script cục bộ. **Không stage.**
  - `M .gitignore` — người lập plan đã thêm dòng `anh-w6-plan.md`. **Không stage** trừ khi người dùng bảo commit.
- ⇒ Khi commit **chỉ `git add` đúng từng file** liệt kê ở §9. **Cấm** `git add -A`, `git add .`, `git commit -a`.
- Không push, không tạo PR trừ khi người dùng yêu cầu.

---

## 3. Ranh giới sở hữu file (KHÔNG LÀM LAN SANG PHẦN NGƯỜI KHÁC)

### 3.1. File đang được người khác sửa trong W6 — CẤM SỬA

| File | Chủ | Lý do |
|---|---|---|
| `ui/page/promotion/AddPromotionDialog.java` | Tài | Tài đang viết lại dialog (multi danh mục). Có bug ngày → chỉ **ghi chú bàn giao** (§8.2), không sửa. |
| `ui/page/promotion/PromotionsPage.java` | Tài | W6 Tài |
| `controller/KhuyenMaiController.java`, `dao/**KhuyenMai*`, `entity/KhuyenMai*.java`, `entity/ChiTietKhuyenMai.java` | Tài | W6 Tài |
| `ui/page/product/ProductLinesPage.java` | Quý | W6 Quý (nút nhập/tải mẫu) |
| `controller/SanPhamController.java`, `controller/DongSanPhamController.java` | Quý | W6 Quý |
| `src/test/.../SanPhamControllerTest.java` | Quý | Đang có thay đổi chưa commit |
| `util/ExcelImporter.java`, `util/ExcelTemplate.java`, `ui/component/ImportPreviewDialog.java` | Đăng | W6 Đăng |
| `ui/AppNavigator.java`, `ui/shell/NavigationModel.java`, `PageId`, `AppShellFrame`, `Sidebar` | Đăng | W6 Đăng |
| `util/DateRange.java`, `ui/component/KyBaoCaoFilter.java` | Đăng | Đã đúng (STRICT `dd/MM/uuuu`), **hưởng lợi tự động** từ sửa `DatePicker`, không cần sửa |
| `pom.xml`, `db/migration/V*.sql`, `db.properties` | Đăng | Không sửa |

### 3.2. File ĐƯỢC sửa trong task này

| File | Chủ gốc | Mức sửa |
|---|---|---|
| `ui/component/DatePicker.java` | **Tuấn Anh** (tác giả) | Thêm mặt nạ nhập + kiểm tra khi rời ô + vài API nhỏ |
| `ui/page/report/PeriodDialog.java` | **Tuấn Anh** (tác giả) | Thay `JSpinner` ngày → `DatePicker` |
| `ui/page/employee/AddEmployeeDialog.java` | **Tuấn Anh** (UC013) | Chỉnh thông báo lỗi ngày cho thống nhất |
| `ui/page/defective/AddDefectiveProductDialog.java` | Tài (không thuộc W6 của Tài) | Chỉ ô "Ngày phát hiện": text ISO → `DatePicker` |
| `ui/page/product/ProductPage.java` | Quý + Tuấn Anh (Quý chỉ thêm nút import ở header trong W6) | **Chỉ thêm 5 dòng guard** đầu hàm `luuMay()`; commit riêng; báo Quý |
| `ui/component/InputFilters.java` (MỚI) | Tuấn Anh | Helper lọc ký tự số / giờ (P2) |
| `ui/page/backup/BackupPage.java` | Đăng (không thuộc W6 của Đăng) | P2: 2 dòng gắn filter, không đổi gì khác |
| `ui/page/report/LongStockReportPage.java` | Tài (không thuộc W6 của Tài) | P2: 1 dòng gắn filter |
| Test mới: `src/test/java/com/dtaq/retail/ui/component/DatePickerTest.java`, `InputFiltersTest.java` | Tuấn Anh | Mới |
| `docs/reference/plans/W6-TuanAnh-README.md` (MỚI) | Tuấn Anh | Báo cáo |
| `.gitignore` | — | Đã thêm `anh-w6-plan.md` (người lập plan đã làm) |

Nếu trong lúc làm phát hiện thêm bug ở file thuộc §3.1 → **không sửa**, chỉ ghi vào báo cáo §8.2.

---

## 4. Kết quả quét UI (đã đối chiếu mã nguồn ngày 2026-11-09)

Đã grep toàn bộ `src/main/java/com/dtaq/retail/ui` theo: `DatePicker`, `LocalDate.parse`, `JSpinner`,
`SpinnerDateModel`, `Integer.parseInt`, `new BigDecimal(`, `FormField.text(` có nhãn ngày/số.

### 4.1. Ô nhập ngày

| # | Vị trí | Hiện trạng | Bug | Ưu tiên |
|---|---|---|---|---|
| D1 | `ui/component/DatePicker.java` | `JTextField` tự do + popup lịch; `getDate()` parse STRICT `d/M/uuuu`, trả `null` nếu sai | Gõ được chữ (`abc`), số bất kỳ (`12345678901`); không báo lỗi khi rời ô; mọi màn dùng `DatePicker` đều bị | **P1** |
| D2 | `ui/page/product/ProductPage.java` (`dateField`, "Ngày nhập kho"; hàm `luuMay()` ~dòng 237) | `LocalDate ngayNhap = dateField.getDate();` rồi gọi controller | Gõ ngày sai → `getDate()` = `null` → controller **âm thầm** dùng hôm nay (thêm mới) hoặc giữ ngày cũ (cập nhật) → người dùng tưởng đã lưu ngày mình gõ | **P1** |
| D3 | `ui/page/defective/AddDefectiveProductDialog.java` (`ngayPhatHienField`, dòng 28, 47, 89–119) | `FormField.text("Ngày phát hiện (YYYY-MM-DD)")`, `LocalDate.parse(ISO)` | Ô chữ tự do, định dạng ISO khác chuẩn `dd/MM/yyyy` của app, không có lịch | **P1** |
| D4 | `ui/page/report/PeriodDialog.java` (dùng bởi `RevenueStatisticsPage.openPeriodDialog()`) | `JSpinner` + `SpinnerDateModel` + `DateEditor("dd/MM/yyyy")` | Gõ được chữ; gõ sai rồi bấm "Áp dụng" → spinner **âm thầm** dùng giá trị cũ (chưa commitEdit), không báo lỗi; không có lịch | **P1** |
| D5 | `ui/page/employee/AddEmployeeDialog.java` (`ngayVaoLam`, dòng 47, 147–153, 172–176) | Dùng `DatePicker`, có kiểm tra khi rời ô và khi lưu | Hưởng lợi từ D1. Chỉ cần thống nhất thông báo; ô trống báo "phải theo dạng dd/MM/yyyy" chưa chuẩn | P1 (nhỏ) |
| D6 | `ui/component/KyBaoCaoFilter.java` (dùng ở `SalesByProductPage`, `SalesByEmployeePage`, …) | Dùng `DatePicker` + `DateRange.parse` STRICT | Hưởng lợi từ D1. **Không sửa file này.** | — |
| D7 | `ui/page/promotion/AddPromotionDialog.java` (dòng 34–35, 67–68, 125–131) | 2 ô `FormField.text` ISO `YYYY-MM-DD`, `LocalDate.parse` | Cùng loại bug D3; ngoài ra lỗi ngày kết thúc sai lại hiện ở ô ngày bắt đầu | **Không sửa — file của Tài (§3.1)**. Ghi bàn giao §8.2 |

### 4.2. Ô số / giờ (P2)

| # | Vị trí | Hiện trạng | Đề xuất |
|---|---|---|---|
| N1 | `ui/page/backup/BackupPage.java` `intervalField` "Chu kỳ (giờ)" (dòng 123) | Text tự do, kiểm tra 1–168 khi bấm lưu | Gắn `InputFilters.chiChoSo(field, 3)` |
| N2 | `ui/page/backup/BackupPage.java` `runTimeField` "Giờ chạy tự động" `HH:mm` (dòng 122) | Text tự do, `LocalTime.parse` khi lưu | Gắn `InputFilters.gioPhut(field)` |
| N3 | `ui/page/report/LongStockReportPage.java` `thresholdField` "Ngưỡng số ngày tồn" (dòng 52) | Text tự do, `parseThreshold` regex `\d{1,6}` | Gắn `InputFilters.chiChoSo(field, 6)` |
| N4 | `AddPromotionDialog.tyLeField` | Text tự do, BigDecimal | **Không sửa (Tài)** — ghi bàn giao |
| N5 | `PriceTaxPage.taxField`, `ProductLinesPage.taxField` dùng `FormField.money` cho thuế % | Có phân cách hàng nghìn, không cho số lẻ; nếu DB có thuế `8.5` thì `toPlainString()` → `85` | **Không sửa** (ProductLinesPage là file Quý W6; seed hiện tại toàn số nguyên). Chỉ ghi nhận rủi ro trong báo cáo |

Các form khác đã quét và **không cần sửa**: `AddCustomerDialog`, `CategoryDialog`, `ChangePasswordDialog`,
`LoginFrame`, `ForgotPasswordFrame`, `ReturnsPage`, `RetailInvoicePage` (tiền dùng `FormField.money` đã lọc số),
`UpdateDefectiveStatusDialog`, `ApprovalDecisionDialog` (đều là ô chữ tự do đúng nghĩa hoặc đã có kiểm tra).

---

## 5. P1 — Sửa `DatePicker` (gốc của mọi ô ngày)

File: `src/main/java/com/dtaq/retail/ui/component/DatePicker.java`

### 5.1. Hành vi mong muốn (spec)

1. **Mặt nạ khi gõ/dán** (DocumentFilter gắn vào `text`):
   - Chỉ chấp nhận chuỗi kết quả khớp **tiền tố ngày hợp lệ**:
     `^(\d{0,2}|\d{1,2}/\d{0,2}|\d{1,2}/\d{1,2}/\d{0,4})$` (cho phép `1/2/2026` lẫn `01/02/2026`; chặn `/` đứng đầu).
   - **Tự chèn `/`**: khi gõ 1 chữ số ở **cuối** chuỗi và chuỗi hiện tại khớp `\d{2}` hoặc `\d{1,2}/\d{2}`
     → chèn `"/" + chữ số` (gõ `09112026` liền tay thành `09/11/2026`).
   - **Dán 8 chữ số** `ddMMyyyy` (chuỗi kết quả khớp `\d{8}` và đoạn dán dài > 1) → đổi thành `dd/MM/yyyy`.
   - **Dán ISO** `yyyy-M-d` (chuỗi kết quả khớp `(\d{4})-(\d{1,2})-(\d{1,2})`) → nếu là ngày có thật thì đổi
     sang `dd/MM/yyyy`, ngược lại từ chối.
   - Khoảng trắng đầu/cuối đoạn chèn bị bỏ (`strip()`).
   - Mọi trường hợp khác (chữ cái, ký tự lạ, quá 10 ký tự, `-` ngoài mẫu ISO…) → **từ chối**, kêu bíp bằng
     `UIManager.getLookAndFeel().provideErrorFeedback(text)`. Nội dung ô giữ nguyên.
   - **Xóa (`remove`) luôn được phép** (để Backspace qua dấu `/` không bị kẹt) — chuỗi sau khi xóa chỉ gồm số
     và `/`, sai thì bắt ở bước 2.
2. **Kiểm tra khi rời ô** (FocusListener): nếu ô **không trống** và `getDate() == null` → `field.setError(LOI_DINH_DANG)`.
   Bỏ qua nếu `e.isTemporary()` hoặc popup lịch đang mở. **Không tự xóa lỗi** ở đây (để không ghi đè lỗi nghiệp vụ
   do màn hình đặt, ví dụ lỗi ngày vào làm trong `AddEmployeeDialog`).
3. **Xóa lỗi khi người dùng sửa nội dung** (DocumentListener `insertUpdate/removeUpdate` → `field.setError(null)`).
4. `setDate(...)` (gọi `text.setText`) đi qua filter: chuỗi `dd/MM/yyyy` hợp lệ → được nhận. Không được làm hỏng
   `setDate` hiện có.
5. Giữ nguyên toàn bộ giao diện: placeholder `dd/MM/yyyy`, nút lịch trailing, Alt+↓, popup, nút "Hôm nay".

### 5.2. API công khai cần thêm (giữ nguyên các API cũ: `field()`, `textField()`, `getDate()`, `setDate()`, `setError()`, `getText()`)

```java
/** Thông điệp chuẩn khi ô ngày có nội dung nhưng không phải ngày hợp lệ. */
public static final String LOI_DINH_DANG = "Ngày không hợp lệ. Nhập theo dạng dd/MM/yyyy hoặc chọn trên lịch";

/** {@code true} nếu ô đang trống. */
public boolean isBlank()

/** {@code true} nếu ô có nội dung nhưng không đọc được thành ngày có thật. */
public boolean hasInvalidText()   // !isBlank() && getDate() == null

/** Hiện lỗi định dạng nếu {@link #hasInvalidText()}; trả {@code true} nếu ô trống hoặc hợp lệ. Không xóa lỗi sẵn có. */
public boolean kiemTraDinhDang()

/** Bật/tắt cả ô nhập và nút mở lịch. */
public void setEnabled(boolean enabled)
```

### 5.3. Cài đặt tham chiếu (làm theo, có thể chỉnh nhỏ cho biên dịch được)

- Đổi biến cục bộ `moLich` trong constructor thành field `private final JButton moLichButton` để `setEnabled` dùng được.
  **Không đổi** icon, tooltip, style của nút.
- Hàm thuần (không Swing) để test dễ — đặt `static` package-private:

```java
private static final Pattern TIEN_TO_NGAY =
    Pattern.compile("\\d{0,2}|\\d{1,2}/\\d{0,2}|\\d{1,2}/\\d{1,2}/\\d{0,4}");
private static final Pattern CAN_CHEN_GACH = Pattern.compile("\\d{2}|\\d{1,2}/\\d{2}");
private static final Pattern TAM_CHU_SO = Pattern.compile("\\d{8}");
private static final Pattern NGAY_ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

/**
 * Tính nội dung mới của ô ngày khi chèn/thay {@code chen} vào {@code hienTai} tại {@code viTri}
 * (thay {@code doDai} ký tự). Trả {@code null} nếu phải từ chối thao tác.
 */
static String noiDungSauKhiGo(String hienTai, int viTri, int doDai, String chen) {
  String doanChen = chen == null ? "" : chen.strip();
  String ketQua = hienTai.substring(0, viTri) + doanChen + hienTai.substring(viTri + doDai);
  if (ketQua.isEmpty() || TIEN_TO_NGAY.matcher(ketQua).matches()) {
    return ketQua;
  }
  // Gõ liền tay: tự chèn dấu "/" sau ngày và sau tháng.
  if (doDai == 0 && viTri == hienTai.length() && doanChen.length() == 1
      && Character.isDigit(doanChen.charAt(0)) && CAN_CHEN_GACH.matcher(hienTai).matches()) {
    return hienTai + "/" + doanChen;
  }
  // Dán 8 chữ số ddMMyyyy.
  if (doanChen.length() > 1 && TAM_CHU_SO.matcher(ketQua).matches()) {
    return ketQua.substring(0, 2) + "/" + ketQua.substring(2, 4) + "/" + ketQua.substring(4);
  }
  // Dán ngày ISO yyyy-M-d.
  Matcher iso = NGAY_ISO.matcher(ketQua);
  if (iso.matches()) {
    try {
      return DateFormats.formatDate(LocalDate.of(
          Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3))));
    } catch (DateTimeException e) {
      return null;
    }
  }
  return null;
}
```

- DocumentFilter (lớp lồng `private final class MatNaNgay extends DocumentFilter`):

```java
@Override
public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
    throws BadLocationException {
  replace(fb, offset, 0, string, attr);
}

@Override
public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
  super.remove(fb, offset, length); // luôn cho xóa
}

@Override
public void replace(FilterBypass fb, int offset, int length, String chen, AttributeSet attrs)
    throws BadLocationException {
  String hienTai = fb.getDocument().getText(0, fb.getDocument().getLength());
  String doanChen = chen == null ? "" : chen.strip();
  String moi = noiDungSauKhiGo(hienTai, offset, length, chen);
  if (moi == null) {
    UIManager.getLookAndFeel().provideErrorFeedback(text);
    return;
  }
  String duKien = hienTai.substring(0, offset) + doanChen + hienTai.substring(offset + length);
  if (moi.equals(duKien)) {
    super.replace(fb, offset, length, doanChen, attrs);          // giữ vị trí con trỏ tự nhiên
  } else if (moi.equals(hienTai + "/" + doanChen) && length == 0 && offset == hienTai.length()) {
    super.replace(fb, offset, 0, "/" + doanChen, attrs);         // tự chèn "/"
  } else {
    super.replace(fb, 0, fb.getDocument().getLength(), moi, attrs); // dán 8 số / ISO
  }
}
```
  Lưu ý: `UIManager.getLookAndFeel()` có thể là `null` trong test headless rất sớm — bọc
  `LookAndFeel laf = UIManager.getLookAndFeel(); if (laf != null) laf.provideErrorFeedback(text);`.

- Trong constructor (sau khi tạo `field`):

```java
((AbstractDocument) text.getDocument()).setDocumentFilter(new MatNaNgay());
text.getDocument().addDocumentListener(new DocumentListener() {
  @Override public void insertUpdate(DocumentEvent e) { field.setError(null); }
  @Override public void removeUpdate(DocumentEvent e) { field.setError(null); }
  @Override public void changedUpdate(DocumentEvent e) { }
});
text.addFocusListener(new FocusAdapter() {
  @Override
  public void focusLost(FocusEvent e) {
    if (!e.isTemporary() && !popup.isVisible()) {
      kiemTraDinhDang();
    }
  }
});
```
  ⚠ Thứ tự: `field` là `final` và được gán trong constructor — đăng ký listener **sau** dòng `field = new FormField(label, text);`.
  `chon(LocalDate)` hiện đã gọi `field.setError(null)` — giữ nguyên.

- Cập nhật Javadoc lớp: "Ô ngày có lịch bật ra …; chỉ nhận chữ số và dấu '/', tự chèn '/', báo lỗi khi rời ô nếu ngày không có thật."
- Import cần thêm: `java.time.DateTimeException`, `java.util.regex.Matcher`, `java.util.regex.Pattern`,
  `java.awt.event.FocusAdapter`, `java.awt.event.FocusEvent`, `javax.swing.LookAndFeel`, `javax.swing.UIManager`,
  `javax.swing.event.DocumentEvent`, `javax.swing.event.DocumentListener`, `javax.swing.text.AbstractDocument`,
  `javax.swing.text.AttributeSet`, `javax.swing.text.BadLocationException`, `javax.swing.text.DocumentFilter`.

### 5.4. Ảnh hưởng tự động

Sau 5.x, các màn sau được chặn chữ ngay mà không cần sửa file: `KyBaoCaoFilter` (báo cáo theo SP, theo NV, …),
`AddEmployeeDialog`, `ProductPage`. Test `KyBaoCaoFilterTest` hiện chỉ `setText("01/01/2026")`/`"31/01/2026"` → vẫn qua filter.
Nếu có test nào khác `setText` chuỗi ngày sai (chữ) vào `DatePicker` rồi kỳ vọng lỗi → chạy lại test và sửa
**test** cho phù hợp (chuỗi chữ giờ bị chặn từ đầu, ô sẽ trống), chỉ khi test đó thuộc phạm vi được sửa;
nếu là test của người khác → dừng lại và báo người dùng.

---

## 6. P1 — Sửa từng màn hình

### 6.1. `ui/page/product/ProductPage.java` — chặn lưu khi ngày sai (D2)

Chỉ thêm guard ở **đầu** hàm `private void luuMay()` (trước dòng `LocalDate ngayNhap = dateField.getDate();`):

```java
if (dateField.hasInvalidText()) {
  dateField.setError(DatePicker.LOI_DINH_DANG);
  dateField.textField().requestFocusInWindow();
  return;
}
```
- Ô trống: giữ hành vi cũ (controller tự lấy hôm nay khi thêm / giữ ngày cũ khi cập nhật) — **không đổi**.
- Không sửa bất kỳ dòng nào khác của `ProductPage` (Quý đang thêm nút Nhập Excel/Tải mẫu ở header).
- Commit **riêng** (§9) để dễ rebase nếu đụng thay đổi của Quý; ghi trong báo cáo cần báo Quý.

### 6.2. `ui/page/defective/AddDefectiveProductDialog.java` — ô "Ngày phát hiện" (D3)

1. Thay khai báo dòng 28:
   ```java
   private final DatePicker ngayPhatHienPicker = new DatePicker("Ngày phát hiện");
   ```
   (import `com.dtaq.retail.ui.component.DatePicker`). Nhãn bỏ "(YYYY-MM-DD)" vì định dạng giờ là `dd/MM/yyyy`
   như toàn app; placeholder do `DatePicker` lo.
2. Constructor: `ngayPhatHienField.setText(LocalDate.now().toString());` → `ngayPhatHienPicker.setDate(LocalDate.now());`
   và `getBody().add(ngayPhatHienField, ...)` → `getBody().add(ngayPhatHienPicker.field(), "growx, wrap, gapbottom 12");`
   (giữ nguyên constraint, giữ nguyên vị trí giữa ô mô tả và ô ghi chú).
3. `submit()`: bỏ `String ngayStr = ...` và khối `try { LocalDate.parse(ngayStr) } catch (DateTimeParseException …)`; thay bằng:
   ```java
   ngayPhatHienPicker.setError(null);
   ...
   LocalDate ngayPhatHien = LocalDate.now();
   if (!ngayPhatHienPicker.isBlank()) {
     LocalDate ngay = ngayPhatHienPicker.getDate();
     if (ngay == null) {
       ngayPhatHienPicker.setError(DatePicker.LOI_DINH_DANG);
       coLoi = true;
     } else if (ngay.isAfter(LocalDate.now())) {
       ngayPhatHienPicker.setError("Ngày phát hiện không được sau ngày hiện tại");
       coLoi = true;
     } else {
       ngayPhatHien = ngay;
     }
   }
   ```
   Giữ nguyên: ô trống → mặc định hôm nay; thông điệp "không được sau ngày hiện tại"; record `NewDefectiveForm`; các trường khác.
4. Xóa import thừa `java.time.format.DateTimeParseException` nếu không còn dùng.
5. Không đổi `setBusy`, `setErrorMessage`, tiêu đề, InfoCallout, nút.

### 6.3. `ui/page/report/PeriodDialog.java` — thay JSpinner bằng DatePicker (D4)

```java
private final DatePicker fromPicker = new DatePicker("Từ ngày");
private final DatePicker toPicker = new DatePicker("Đến ngày");
```
- Constructor: `fromPicker.setDate(from); toPicker.setDate(to);` và `row.add(fromPicker.field()); row.add(toPicker.field());`
  (giữ layout `"[grow,fill]16[grow,fill]"`, tiêu đề, phụ đề, độ rộng 480, nút Hủy/Áp dụng như cũ).
- Xóa `newDateSpinner`, `toLocalDate`, field `fromSpinner/toSpinner/fromField/toField`, hằng `DATE_PATTERN`,
  import `ZoneId`, `Date`, `JSpinner`, `SpinnerDateModel`, `FormField` (nếu không còn dùng).
- `submit()`:
  ```java
  fromPicker.setError(null);
  toPicker.setError(null);
  LocalDate from = docNgay(fromPicker, "Vui lòng nhập hoặc chọn ngày bắt đầu");
  LocalDate to = docNgay(toPicker, "Vui lòng nhập hoặc chọn ngày kết thúc");
  if (from == null || to == null) {
    return;
  }
  if (from.isAfter(to)) {
    toPicker.setError("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");   // giữ nguyên câu cũ
    return;
  }
  dispose();
  onConfirm.accept(from, to);
  ```
  với helper:
  ```java
  /** Ngày hợp lệ trong ô, hoặc {@code null} sau khi đã hiện lỗi dưới ô. */
  private static LocalDate docNgay(DatePicker picker, String loiTrong) {
    if (picker.isBlank()) {
      picker.setError(loiTrong);
      return null;
    }
    LocalDate ngay = picker.getDate();
    if (ngay == null) {
      picker.setError(DatePicker.LOI_DINH_DANG);
    }
    return ngay;
  }
  ```
- Chữ ký constructor `PeriodDialog(Frame, LocalDate, LocalDate, BiConsumer<LocalDate, LocalDate>)` **giữ nguyên**
  (được gọi từ `RevenueStatisticsPage.openPeriodDialog()` — không sửa file đó).

### 6.4. `ui/page/employee/AddEmployeeDialog.java` — thống nhất thông báo (D5)

- Trong `onBlur(ngayVaoLam.textField(), ...)` (≈ dòng 147–153): thay chuỗi `"Ngày không hợp lệ, nhập theo dạng dd/MM/yyyy"`
  bằng `DatePicker.LOI_DINH_DANG`. Logic còn lại giữ nguyên (`NhanVienController.kiemTraNgayVaoLam(ngay)`).
- Trong `submit()` (≈ dòng 172–176) thay khối `if (ngay == null)` bằng:
  ```java
  if (ngay == null) {
    ngayVaoLam.field().setError(ngayVaoLam.isBlank()
        ? "Vui lòng nhập hoặc chọn ngày vào làm" : DatePicker.LOI_DINH_DANG);
    return;
  }
  ```
- Không đổi gì khác.

---

## 7. P2 — Chặn chữ ở ô số/giờ

### 7.1. File mới `src/main/java/com/dtaq/retail/ui/component/InputFilters.java`

```java
package com.dtaq.retail.ui.component;

/** Bộ lọc ký tự cho ô nhập: chặn ký tự sai ngay khi gõ/dán. Không thay đổi giao diện ô. */
public final class InputFilters {
  private InputFilters() {}

  /** Chỉ cho chữ số, tối đa {@code doDaiToiDa} ký tự. */
  public static void chiChoSo(JTextField o, int doDaiToiDa)

  /** Giờ dạng HH:mm: chỉ số và ':', tự chèn ':' sau 2 chữ số giờ, tối đa 5 ký tự. */
  public static void gioPhut(JTextField o)

  /** Hàm thuần để test: kết quả hợp lệ hay không (dùng chung bởi filter). */
  static boolean laSoHopLe(String s, int doDaiToiDa)            // s.matches("\\d{0," + max + "}")
  static String noiDungGioPhutSauKhiGo(String hienTai, int viTri, int doDai, String chen) // null = từ chối
}
```
- Cùng kiểu cài đặt với `MatNaNgay` ở §5.3 (`AbstractDocument#setDocumentFilter`, `remove` luôn cho, từ chối thì
  `provideErrorFeedback` có kiểm tra `null`, `strip()` đoạn chèn).
- Giờ: tiền tố hợp lệ `^(\d{0,2}|\d{1,2}:\d{0,2})$`; gõ 1 chữ số ở cuối khi hiện tại khớp `\d{2}` → tự chèn `:`;
  dán `HHmm` 4 số → `HH:mm`. Việc giờ ≤ 23 / phút ≤ 59 vẫn do `LocalTime.parse` sẵn có ở `BackupPage` kiểm tra khi lưu.

### 7.2. Gắn filter (mỗi chỗ đúng 1 dòng, đặt ngay sau dòng tạo field)

- `ui/page/backup/BackupPage.java`, trong `buildConfigCard()` sau 2 dòng tạo field (≈ dòng 122–123) và **trước** `napLaiCauHinh();`:
  ```java
  InputFilters.gioPhut(runTimeField.getTextField());
  InputFilters.chiChoSo(intervalField.getTextField(), 3);
  ```
  Kiểm tra: `napLaiCauHinh()` gán giờ dạng `HH:mm` và chu kỳ là số nguyên → phải vẫn hiển thị đúng
  (đọc hàm `napLaiCauHinh` để chắc chắn định dạng; nếu giờ hiển thị kiểu khác `HH:mm` thì KHÔNG gắn `gioPhut`, ghi vào báo cáo).
- `ui/page/report/LongStockReportPage.java`, ngay sau khai báo/ trong constructor trước `thresholdField.setText("60");` (≈ dòng 74):
  ```java
  InputFilters.chiChoSo(thresholdField.getTextField(), 6);
  ```
- Import `com.dtaq.retail.ui.component.InputFilters`. Không đổi gì khác trong hai file.

---

## 8. Báo cáo `docs/reference/plans/W6-TuanAnh-README.md`

Viết tiếng Việt, cùng phong cách với `docs/reference/plans/W4-TuanAnh-README.md` (đọc file đó để bắt chước cấu trúc). Gồm:

### 8.1. Nội dung chính
- Nguồn giao việc (trích lời trưởng nhóm §1.1), phạm vi, nguyên tắc "không refactor UI".
- Bảng các bug đã sửa (D1–D5, N1–N3) — vị trí, triệu chứng trước, hành vi sau.
- Danh sách file đã sửa + lý do được phép sửa (bảng §3.2).
- Cách kiểm thử (test tự động + checklist tay §10.2) và kết quả thật (dán output `mvn`, ghi rõ test nào fail và vì sao nếu có).

### 8.2. Bàn giao cho thành viên khác (không tự sửa)
- **Tài — `AddPromotionDialog`**: 2 ô ngày bắt đầu/kết thúc đang là `FormField.text` ISO `YYYY-MM-DD` + `LocalDate.parse`:
  gõ được chữ, không có lịch, khác định dạng `dd/MM/yyyy`; lỗi ngày kết thúc sai lại hiện ở ô ngày bắt đầu.
  Đề xuất khi Tài viết lại dialog W6: dùng `new DatePicker("Ngày bắt đầu")`/`"Ngày kết thúc"`, đọc bằng
  `getDate()`, báo `DatePicker.LOI_DINH_DANG` đúng ô bị sai, giữ kiểm tra bắt đầu ≤ kết thúc. Ô `tyLeField`
  có thể dùng `InputFilters` (cần cho phép thập phân → Tài tự quyết).
- **Quý — `ProductPage`**: đã thêm guard 5 dòng đầu `luuMay()` (commit riêng, nêu hash). Khi merge nhánh Quý nếu xung đột thì giữ cả hai.
- **Quý — thuế %** ở `ProductLinesPage`/`PriceTaxPage` dùng `FormField.money` (phân cách nghìn, không số lẻ;
  `BigDecimal.toPlainString()` của thuế lẻ như `8.5` sẽ thành `85`). Chỉ là rủi ro, seed hiện tại toàn số nguyên.
- **Đăng** — `BackupPage` chỉ thêm 2 dòng filter (nếu đã làm P2).

---

## 9. Commit (chỉ khi mọi test liên quan xanh hoặc đã giải thích rõ)

Quy ước commit dự án: Conventional Commits, mô tả tiếng Anh ngắn, ví dụ `feat(ui): …`, `fix(ui): …`, `docs(plans): …`.
Mỗi commit kết thúc bằng dòng trống + `Co-Authored-By` nếu môi trường yêu cầu (theo hướng dẫn của công cụ đang dùng).

Thứ tự đề xuất (stage đúng file, kiểm tra `git status` trước mỗi commit):
1. `fix(ui): enforce date mask and validation in DatePicker`
   — `ui/component/DatePicker.java`, `src/test/.../ui/component/DatePickerTest.java`
2. `fix(ui): use DatePicker for defective date and report period`
   — `ui/page/defective/AddDefectiveProductDialog.java`, `ui/page/report/PeriodDialog.java`, `ui/page/employee/AddEmployeeDialog.java`
3. `fix(ui): block saving product with invalid stock-in date`
   — **chỉ** `ui/page/product/ProductPage.java`
4. `fix(ui): restrict numeric and time inputs to valid characters` (P2)
   — `ui/component/InputFilters.java`, `src/test/.../ui/component/InputFiltersTest.java`, `ui/page/backup/BackupPage.java`, `ui/page/report/LongStockReportPage.java`
5. `docs(plans): add Tuan Anh W6 validation report`
   — `docs/reference/plans/W6-TuanAnh-README.md`

Tuyệt đối không stage: `SanPhamControllerTest.java`, `setup-db.sh`, `anh-w6-plan.md`, `db.properties`, `target/`.
Không push / không mở PR nếu người dùng chưa yêu cầu.

---

## 10. Kiểm thử

### 10.1. Test tự động (JUnit 5, headless — xem mẫu `src/test/java/com/dtaq/retail/ui/component/KyBaoCaoFilterTest.java`)

`src/test/java/com/dtaq/retail/ui/component/DatePickerTest.java` (cùng package để gọi hàm package-private), `@DisplayName` tiếng Việt:

| Ca | Thao tác | Kỳ vọng |
|---|---|---|
| Chặn chữ | `picker.textField().setText("abc")` | `getText()` = `""` |
| Chặn chữ lẫn số | `setText("12a")` | `""` (cả chuỗi bị từ chối vì `setText` thay toàn bộ) |
| Ngày đủ | `setText("09/11/2026")` | `getDate()` = 2026-11-09 |
| Ngày ngắn | `setText("1/2/2026")` | `getDate()` = 2026-02-01 |
| Tự chèn `/` | gõ lần lượt từng ký tự `"0","9","1","1","2","0","2","6"` bằng `doc.insertString(doc.getLength(), c, null)` | text = `"09/11/2026"` |
| Dán 8 số | `setText("09112026")` | `"09/11/2026"` |
| Dán ISO | `setText("2026-11-09")` | `"09/11/2026"` |
| ISO sai | `setText("2026-02-30")` | `""` |
| Quá dài | đang `"09/11/2026"`, `insertString(10, "1")` | không đổi |
| Ngày không có thật | `setText("31/02/2026")` | text giữ nguyên (qua mặt nạ), `getDate()` null, `hasInvalidText()` true, `kiemTraDinhDang()` false |
| Ô trống | `setText("")` | `isBlank()` true, `hasInvalidText()` false, `kiemTraDinhDang()` true |
| setDate | `setDate(LocalDate.of(2026,1,5))` | text `"05/01/2026"` |
| Xóa lỗi khi sửa | `setError("x")` rồi `setText("0")` | dòng lỗi của field trở về `" "` (đọc qua cây component như KyBaoCaoFilterTest tìm `JLabel`, hoặc chỉ kiểm tra `getClientProperty(FlatClientProperties.OUTLINE)` của `textField()` là `null`) |
| Hàm thuần | `DatePicker.noiDungSauKhiGo(...)` các ca: `("",0,0,"a")`→null; `("09",2,0,"1")`→`"09/1"`; `("09/11",5,0,"2")`→`"09/11/2"`; `("/",…)` đầu chuỗi → null | như mô tả |

`InputFiltersTest.java`: `chiChoSo` chặn `"a"`, `"1.5"`, quá độ dài; nhận `"168"`. `gioPhut`: gõ `"0","8","3","0"` → `"08:30"`; dán `"0830"` → `"08:30"`; chặn `"ab"`.

Nếu môi trường test headless làm `UIManager.getLookAndFeel()` gây lỗi → đã có kiểm tra `null` (§5.3).

Lệnh chạy (từ thư mục gốc dự án, nơi có `pom.xml`):
```bash
mvn -q -Dtest='DatePickerTest,InputFiltersTest,KyBaoCaoFilterTest,LongStockThresholdTest,UiLayoutSmokeTest,ProductPageTest,DefectiveProductsPageTest' test
```
```bash
mvn -q test
```
- Toàn bộ suite cần PostgreSQL + `db.properties` (không commit). Nếu test DB lỗi do môi trường, hoặc
  `SanPhamControllerTest.capNhatMay_thanhCong` fail (đó là việc Q6-06 của Quý, file đang có thay đổi chưa commit),
  **không sửa** — ghi nguyên văn vào báo cáo và báo người dùng.
- Biên dịch phải sạch: `mvn -q -DskipTests compile`.

### 10.2. Kiểm thử tay (chạy app)

```bash
mvn -q clean compile exec:java -Dexec.mainClass=com.dtaq.retail.Main
```
Tài khoản seed: quản lý `NVQL2026001`, bán hàng `NVBH2026001`, mật khẩu demo `12345678`.

| Màn | Bước | Kỳ vọng |
|---|---|---|
| Quản lý sản phẩm (ProductPage) | Gõ `abc` vào "Ngày nhập kho" | Không hiện ký tự, bíp |
| | Gõ `31022026` | Thành `31/02/2026`; Tab ra → lỗi đỏ "Ngày không hợp lệ…"; bấm Lưu → không lưu, focus về ô ngày |
| | Bấm icon lịch chọn ngày | Ô điền `dd/MM/yyyy`, lỗi biến mất; lưu được |
| | Dán `2026-11-01` | Thành `01/11/2026` |
| Nhân viên → Thêm/Sửa | Ô "Ngày vào làm": chữ bị chặn; ngày sai → lỗi khi rời ô; xóa trống + Lưu → "Vui lòng nhập hoặc chọn ngày vào làm" | |
| Sản phẩm lỗi → Ghi nhận | Ô "Ngày phát hiện" có icon lịch, mặc định hôm nay `dd/MM/yyyy`; chữ bị chặn; ngày tương lai → lỗi cũ; để trống → lưu với hôm nay | |
| Thống kê doanh thu → nút chọn kỳ (PeriodDialog) | Có icon lịch; gõ sai → lỗi dưới ô, không đóng dialog; từ > đến → lỗi ở ô "Đến ngày" | |
| Báo cáo theo SP / theo NV (KyBaoCaoFilter) | Chữ bị chặn; ngày sai + Áp dụng → lỗi như cũ | |
| Sao lưu (BackupPage) | "Giờ chạy": gõ `0830` → `08:30`, chữ bị chặn; "Chu kỳ": chữ bị chặn, tối đa 3 số; lưu `25:00` → lỗi "Giờ phải có dạng HH:mm" như cũ | |
| Báo cáo tồn lâu ngày | "Ngưỡng số ngày tồn": chữ bị chặn | |
| Toàn bộ | So sánh giao diện trước/sau | **Không thay đổi** bố cục/màu/kích thước (trừ ô Ngày phát hiện và PeriodDialog có thêm icon lịch) |

---

## 11. Definition of Done

- [ ] Không còn ô ngày nào (ngoài `AddPromotionDialog` của Tài) cho gõ chữ; mọi ô ngày có nút lịch.
- [ ] Ngày sai không bao giờ bị âm thầm thay bằng ngày khác khi lưu (ProductPage, PeriodDialog).
- [ ] Không sửa file nào trong §3.1; diff chỉ gồm file ở §3.2.
- [ ] Không đổi layout/màu/font/text nút (trừ nhãn "Ngày phát hiện" bỏ "(YYYY-MM-DD)").
- [ ] `mvn -q -DskipTests compile` sạch; test mới + test UI liên quan xanh; kết quả `mvn -q test` ghi vào báo cáo.
- [ ] Báo cáo `W6-TuanAnh-README.md` có phần bàn giao cho Tài/Quý/Đăng.
- [ ] Commit theo §9, không stage file ngoài phạm vi, không push nếu chưa được yêu cầu.

## 12. Những điều KHÔNG làm

- Không thêm thư viện, không sửa `pom.xml`, migration, `db.properties`.
- Không đổi `DateRange`, `KyBaoCaoFilter`, `DateFormats`, `FormField` (trừ khi bắt buộc để biên dịch — khi đó dừng lại hỏi).
- Không chuyển controller sang import Swing; không đưa logic nghiệp vụ mới vào UI.
- Không "dọn dẹp", đổi tên, format lại code ngoài các dòng cần sửa.
- Không sửa tài liệu W6 của nhóm; không sửa `AGENTS.md`.
- Không tuyên bố đã chạy/kiểm tra thứ gì nếu chưa thực sự chạy.
