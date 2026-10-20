# Phân luồng màn hình và thiết kế màn hình (Screen Flow & Screens)

> **Trạng thái tài liệu:** Trích xuất từ báo cáo phân tích và thiết kế v12; trạng thái phê duyệt chưa được xác minh.
> **Nguồn:** `docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`.
> **Phạm vi trích xuất:** CHƯƠNG 4 › III. Phân luồng màn hình của ứng dụng (Screen Flow); CHƯƠNG 4 › IV. Thiết kế màn hình của ứng dụng. Bổ sung tham chiếu biểu mẫu giấy ở CHƯƠNG 1 (Hình 1, Hình 2) với vai trò chứng từ nguồn.
> **Phân biệt:** Nội dung là **thiết kế ghi trong báo cáo**. Mã nguồn hiện tại **không** được dùng để bổ sung màn hình còn thiếu.
> **Số trang:** Có nêu ở một số mục vì đã có trong danh mục hình vẽ của DOCX; đây là số trang do trường mục lục của DOCX tạo ra, **cached/unverified** và **chưa được xác minh lại thủ công**.
> **Hình vẽ:** bản sao **byte-identical** của các part đã được lưu tại [`../assets/diagrams/`](../assets/diagrams/) và [`../assets/screens/`](../assets/screens/) (xem [`../assets/manifest.md`](../assets/manifest.md)). Ngày 2026-10-19, 37 tệp đã được **mở/kiểm tra hiển thị**; tài liệu **không vẽ lại** và **chưa chuyển ngữ nghĩa chi tiết**. Diễn giải ngữ nghĩa còn **chờ rà soát**.

---

## 1. Phân luồng màn hình (CHƯƠNG 4 › III)

Mục **"III. Phân luồng màn hình của ứng dụng (Screen Flow)"** trong DOCX **chỉ chứa một hình duy nhất, không có văn bản mô tả các bước luồng**.

- Hình phân luồng màn hình: `word/media/image11.png` (hiển thị ≈ 4.47 in × 8.77 in trong DOCX) — bản sao: ![Screen Flow](../assets/diagrams/image11.png).
- Bản trích xuất văn bản **không chứa** nội dung đồ họa của sơ đồ phân luồng, do đó **không thể xác định các nút màn hình và các cạnh điều hướng** từ văn bản.
- Bản sao byte-identical đã được lưu tại `../assets/diagrams/image11.png`; hình Screen Flow đã được **mở/kiểm tra hiển thị** ngày 2026-10-19, nhưng **chưa lập đặc tả luồng văn bản đầy đủ** (chưa chuyển ngữ nghĩa từng nút/cạnh).
- Không tái tạo hoặc vẽ lại sơ đồ phân luồng trong tài liệu này.

> **Lưu ý kiểm chứng:** Không suy diễn thứ tự màn hình từ danh sách màn hình ở mục 3 dưới đây. Danh sách màn hình chỉ là tập hợp các hình chụp giao diện được chèn trong mục IV, không phải biểu diễn luồng.

---

## 2. Thiết kế màn hình của ứng dụng (CHƯƠNG 4 › IV)

Mục này trình bày các hình chụp giao diện kèm chú thích. Trong bản DOCX, mỗi hình nằm trong một bảng/border riêng và có một caption in nghiêng bên dưới.

| Hình | Chú thích (nguyên văn DOCX) | Tệp hình trong DOCX | Số trang theo danh mục hình vẽ (cached/unverified) | Liên kết ảnh (đã sao chép) |
|---|---|---|---|---|
| Hình 3 | Đăng nhập | `word/media/image4.png` | 142 | ![Hình 3 – Đăng nhập](../assets/screens/image4.png) |
| Hình 4 | Quên mật khẩu | `word/media/image13.png` | 143 | ![Hình 4 – Quên mật khẩu](../assets/screens/image13.png) |
| Hình 5 | Màn trang chủ | `word/media/image39.png` | 143 | ![Hình 5 – Màn trang chủ](../assets/screens/image39.png) |
| Hình 6 | Nhập mã sản phẩm khi lập hoá đơn bán lẻ | `word/media/image3.png` | 144 | ![Hình 6 – Nhập mã sản phẩm khi lập hoá đơn bán lẻ](../assets/screens/image3.png) |
| Hình 7 | Chỉnh số lượng sản phẩm trong lập hoá đơn bán lẻ | `word/media/image19.png` | 145 | ![Hình 7 – Chỉnh số lượng sản phẩm trong lập hoá đơn bán lẻ](../assets/screens/image19.png) |
| Hình 8 | Trang chủ nhân viên | `word/media/image2.png` | 145 | ![Hình 8 – Trang chủ nhân viên](../assets/screens/image2.png) |
| Hình 9 | In hoá đơn | `word/media/image7.png` | 146 | ![Hình 9 – In hoá đơn](../assets/screens/image7.png) |
| Hình 10 | Quản lý khách hàng | `word/media/image9.png` | 146 | ![Hình 10 – Quản lý khách hàng](../assets/screens/image9.png) |
| Hình 11 | Thêm khách hàng mới | `word/media/image5.png` | 147 | ![Hình 11 – Thêm khách hàng mới](../assets/screens/image5.png) |
| Hình 12 | Thống kê ở quản lý | `word/media/image14.png` | 148 | ![Hình 12 – Thống kê ở quản lý](../assets/screens/image14.png) |
| Hình 13 | Quản lý hàng trong kho | `word/media/image15.png` | 149 | ![Hình 13 – Quản lý hàng trong kho](../assets/screens/image15.png) |
| Hình 14 | Trang chủ quản lý | `word/media/image1.png` | 150 | ![Hình 14 – Trang chủ quản lý](../assets/screens/image1.png) |

**Ghi chú về thứ tự trình bày:** Trong DOCX, các hình xuất hiện theo thứ tự: Hình 3, Hình 4, Hình 5, Hình 6, Hình 7, Hình 8, Hình 9, Hình 10, Hình 11, Hình 12, Hình 13, Hình 14. Không có Hình 15 trở lên ở mục này.

**Ghi chú về số trang:** Các số trang trên lấy từ "DANH MỤC CÁC HÌNH VẼ" của DOCX; đây là giá trị **cached/unverified** do trường mục lục tạo, cần rà soát lại nếu dùng để trích dẫn chính xác.

**Tài sản hình ảnh:** Bản sao byte-identical của Hình 3–Hình 14 nằm tại `../assets/screens/` (xem [`../assets/manifest.md`](../assets/manifest.md)). 12 hình đã được **mở/kiểm tra hiển thị** ngày 2026-10-19; tài liệu **không mô tả chi tiết bố cục, màu sắc hay thành phần màn hình** và **chưa chuyển ngữ nghĩa chi tiết**; diễn giải ngữ nghĩa còn **chờ rà soát**.

---

## 3. Tham chiếu biểu mẫu chứng từ giấy (CHƯƠNG 1)

Hai hình đầu tiên của báo cáo là biểu mẫu giấy đang dùng trong quy trình thủ công (không phải màn hình ứng dụng), được nhóm dùng làm căn cứ phân tích:

| Hình | Chú thích (nguyên văn DOCX) | Vị trí | Tệp hình trong DOCX | Số trang theo danh mục (cached/unverified) | Liên kết ảnh (đã sao chép) |
|---|---|---|---|---|---|
| Hình 1 | Hóa đơn bán lẻ | CHƯƠNG 1 › I › 1 | `word/media/image20.png` | 10 | ![Hình 1 – Hóa đơn bán lẻ](../assets/screens/image20.png) |
| Hình 2 | Phiếu Đổi/Trả hàng | CHƯƠNG 1 › I › 1 | `word/media/image32.png` | 12 | ![Hình 2 – Phiếu Đổi/Trả hàng](../assets/screens/image32.png) |

Mô tả văn bản đi kèm trong DOCX:

- **Hình 1 – Hóa đơn bán lẻ:** là một trong những chứng từ chính đang được sử dụng trong quy trình bán hàng thủ công; biểu mẫu thể hiện thông tin khách hàng, nhân viên bán hàng, ngày lập hóa đơn, danh sách sản phẩm, số lượng, đơn giá và tổng số tiền thanh toán; chưa tách riêng thuế suất, tiền thuế và giá trị hàng hóa trước thuế.
- **Hình 2 – Phiếu Đổi/Trả hàng:** ghi nhận các trường hợp khách hàng đổi hoặc trả sản phẩm sau khi mua; giúp nhận diện các thông tin nghiệp vụ cần lưu trữ như hóa đơn gốc, sản phẩm đổi/trả, số lượng, đơn giá, lý do đổi/trả, ngày xử lý và giá trị hoàn tiền.

Bản sao byte-identical của Hình 1 và Hình 2 nằm tại [`../assets/screens/image20.png`](../assets/screens/image20.png) và [`../assets/screens/image32.png`](../assets/screens/image32.png) (xem [`../assets/manifest.md`](../assets/manifest.md)); ảnh gốc vẫn trong DOCX. Hai hình đã được **mở/kiểm tra hiển thị** ngày 2026-10-19; tài liệu **chưa chuyển ngữ nghĩa chi tiết**, diễn giải ngữ nghĩa còn **chờ rà soát**.

---

## 4. Tham chiếu diagram use case (CHƯƠNG 3)

Các diagram liên quan đến luồng màn hình/use case nằm ở CHƯƠNG 3; bản trích xuất văn bản **không chứa nội dung đồ họa**. Bản sao byte-identical nằm tại `../assets/diagrams/` (xem [`../assets/manifest.md`](../assets/manifest.md)); các hình đã được **mở/kiểm tra hiển thị** ngày 2026-10-19. Danh sách dưới đây chỉ định vị hình trong nguồn và **chưa chuyển ngữ nghĩa chi tiết**; diễn giải ngữ nghĩa còn **chờ rà soát**.

| Vị trí | Tệp hình trong DOCX | Liên kết ảnh (đã sao chép) |
|---|---|---|
| CHƯƠNG 3 › I. Mô hình use case tổng quát | `word/media/image18.png` | ![CHƯƠNG 3 › I. Mô hình use case tổng quát](../assets/diagrams/image18.png) |
| UC003 › a. Sequence diagram | `word/media/image22.png` | ![UC003 › a. Sequence diagram](../assets/diagrams/image22.png) |
| UC003 › b. Activity diagram | `word/media/image21.png` | ![UC003 › b. Activity diagram](../assets/diagrams/image21.png) |
| UC004 › a. Sequence diagram | `word/media/image24.png` | ![UC004 › a. Sequence diagram](../assets/diagrams/image24.png) |
| UC004 › b. Activity diagram | `word/media/image28.png` | ![UC004 › b. Activity diagram](../assets/diagrams/image28.png) |
| UC005 › a. Sequence diagram | `word/media/image25.png` | ![UC005 › a. Sequence diagram](../assets/diagrams/image25.png) |
| UC005 › b. Activity diagram | `word/media/image36.png` | ![UC005 › b. Activity diagram](../assets/diagrams/image36.png) |
| UC006 › a. Sequence diagram | `word/media/image26.png` | ![UC006 › a. Sequence diagram](../assets/diagrams/image26.png) |
| UC006 › b. Activity diagram | `word/media/image27.png` | ![UC006 › b. Activity diagram](../assets/diagrams/image27.png) |
| UC008 › a. Sequence diagram | `word/media/image34.png` | ![UC008 › a. Sequence diagram](../assets/diagrams/image34.png) |
| UC008 › b. Activity diagram | `word/media/image29.png` | ![UC008 › b. Activity diagram](../assets/diagrams/image29.png) |
| UC014 › a. Sequence diagram | `word/media/image33.png` | ![UC014 › a. Sequence diagram](../assets/diagrams/image33.png) |
| UC014 › b. Activity diagram | `word/media/image31.png` | ![UC014 › b. Activity diagram](../assets/diagrams/image31.png) |
| UC015 › a. Sequence diagram | `word/media/image30.png` | ![UC015 › a. Sequence diagram](../assets/diagrams/image30.png) |
| UC015 › b. Activity diagram | `word/media/image23.png` | ![UC015 › b. Activity diagram](../assets/diagrams/image23.png) |
| UC016 › a. Sequence diagram | `word/media/image35.png` | ![UC016 › a. Sequence diagram](../assets/diagrams/image35.png) |
| UC016 › b. Activity diagram | `word/media/image37.png` | ![UC016 › b. Activity diagram](../assets/diagrams/image37.png) |
| UC017 › a. Sequence diagram | `word/media/image38.png` | ![UC017 › a. Sequence diagram](../assets/diagrams/image38.png) |
| UC017 › b. Activity diagram | `word/media/image10.png` | ![UC017 › b. Activity diagram](../assets/diagrams/image10.png) |

Các use case **không** kèm diagram trong DOCX: UC001, UC002, UC007, UC009, UC010, UC011, UC012, UC013, UC018, UC019.

---

## 5. Tham chiếu màn hình và luồng liên quan trong đặc tả use case

Phần đặc tả use case (CHƯƠNG 3 › IV) có nêu tên màn hình ở cấp văn bản. Dưới đây là các màn hình được nhắc trong luồng chính, giữ nguyên cách gọi của nguồn (không gán thêm ID màn hình):

| Use case | Màn hình/Trạng thái được nhắc trong luồng |
|---|---|
| UC001 – Đăng nhập | Màn hình đăng nhập; màn hình chính theo quyền. |
| UC002 – Đăng xuất | Yêu cầu xác nhận đăng xuất; màn hình đăng nhập. |
| UC003 – Lập hóa đơn bán lẻ | Màn hình lập hóa đơn; hóa đơn nháp; hóa đơn đã thanh toán. |
| UC004 – Tra cứu sản phẩm và tồn kho | Màn hình tra cứu; danh sách kết quả; danh sách máy sẵn sàng bán. |
| UC005 – Tra cứu khách hàng và giao dịch | Màn hình tra cứu; thông tin khách hàng và lịch sử mua hàng; chi tiết hóa đơn gốc; danh sách phiếu đổi/phiếu trả. |
| UC006 – Xử lý đổi trả sản phẩm | Màn hình tiếp nhận đổi/trả; phiếu trả; phiếu đổi và số tiền chênh lệch; chứng từ đã hoàn tất. |
| UC007 – Thêm khách hàng | Biểu mẫu thêm khách hàng; thông báo thêm thành công. |
| UC008 – Thêm sản phẩm | Biểu mẫu thêm máy; thông báo thêm sản phẩm thành công. |
| UC009 – Thêm dòng sản phẩm | Biểu mẫu thêm dòng sản phẩm. |
| UC010 – Thêm danh mục sản phẩm | Biểu mẫu thêm danh mục. |
| UC011 – Thêm sản phẩm lỗi | Biểu mẫu ghi nhận lỗi. |
| UC012 – Thêm khuyến mãi | Biểu mẫu thêm chương trình. |
| UC013 – Thêm nhân viên bán hàng | Biểu mẫu thêm nhân viên. |
| UC014 – Báo cáo sản phẩm tồn kho lâu ngày | Bộ lọc báo cáo; kết quả báo cáo; danh sách máy thuộc dòng được chọn. |
| UC015 – Thống kê doanh số theo sản phẩm | Bộ lọc thống kê; kết quả thống kê theo sản phẩm; giao dịch dùng để tổng hợp. |
| UC016 – Thống kê doanh số theo nhân viên | Bộ lọc thống kê; kết quả theo nhân viên; hóa đơn và điều chỉnh đổi/trả. |
| UC017 – Thống kê doanh thu | Bộ lọc kỳ báo cáo; báo cáo doanh thu; hộp thoại lưu file Excel. |
| UC018 – Phê duyệt yêu cầu đổi trả | Danh sách phiếu chờ duyệt; phiếu, hóa đơn gốc và máy liên quan; kết quả phê duyệt. |
| UC019 – Cập nhật giá bán và thuế suất | Danh sách dòng sản phẩm; giá và thuế suất hiện tại; thông tin sau cập nhật. |

> Đây là các màn hình **được nhắc trong văn bản đặc tả**, không phải danh sách màn hình đã được thiết kế trực quan. Các màn hình được thiết kế trực quan nằm ở mục 2 (Hình 3–Hình 14).

---

## 6. Ảnh chụp mã nguồn ngày 2026-10-20 (không dùng để bổ sung dữ kiện DOCX)

> Ảnh chụp tại ngày **2026-10-20**. Chỉ ghi nhận những gì đọc được từ đúng tệp/dòng được dẫn; không suy diễn màn hình từ mã nguồn.

| Quan sát trong ảnh chụp | Bằng chứng |
|---|---|
| `Main` lấy shared `DataSource` từ `ConnectDB.getDataSource()`, đăng ký shutdown hook, chạy `DbMigration.migrate(dataSource)` trên main thread trước EDT. Nếu migration lỗi, ứng dụng ghi log loại exception/cause, đóng pool và `return` mà không mở UI. Thành công thì `SwingUtilities.invokeLater` tạo `MainFrame` và gọi `setVisible(true)`. Không còn `MainController` hay `ProductDaoImpl`. | `src/main/java/com/dtaq/retail/Main.java:21-43` |
| `DbMigration` cấu hình Flyway với chính `DataSource` được truyền vào và location `classpath:db/migration`, rồi gọi `load().migrate()`. | `src/main/java/com/dtaq/retail/config/DbMigration.java:30-49` |
| `MainFrame` khởi tạo cửa sổ với tiêu đề lấy từ `AppInfo.APP_NAME`, dùng `MigLayout`, thêm nhãn header (tên + phiên bản), nhãn giữa “Chào mừng đến DTAQ Retail”, `pack()`, kích thước tối thiểu 800×520 và căn giữa màn hình. Không còn nhãn trạng thái hay `setStatus`. | `src/main/java/com/dtaq/retail/ui/MainFrame.java:12-30` |
| V1 initial schema nằm trong resource `classpath:db/migration` và tạo 14 bảng nghiệp vụ; không có bảng demo `products`. | `src/main/resources/db/migration/V1__initial_schema.sql` |

Hệ quả trong phạm vi ảnh chụp:

- `src/main/java/com/dtaq/retail/ui/` chỉ có `MainFrame` dạng cửa sổ chào mừng chung; **không có** màn hình nghiệp vụ tương ứng Hình 3–Hình 14 (Đăng nhập, Quên mật khẩu, Trang chủ, Lập hóa đơn bán lẻ, In hóa đơn, Quản lý khách hàng, Thống kê, Quản lý hàng trong kho,…).
- Không còn DAO/entity/controller cho Product; chưa có DAO/entity hay màn hình nghiệp vụ nào được hiện thực cho 14 bảng của V1.
- V1 đã được kiểm chứng runtime trên PostgreSQL **16.13 disposable** theo bằng chứng tại [schema-and-integrity.md §6.6](../database/schema-and-integrity.md#66-kiểm-chứng-v1): Flyway lần đầu áp dụng 1 migration, lần hai không còn migration chờ; không dùng CSDL thật. `DbMigrationTest` vẫn chỉ kiểm tra cấu hình, không mở kết nối.
- Vì vậy chưa thể đối chiếu trực quan giữa hình chụp màn hình trong DOCX với mã nguồn.

---

## 7. Nội dung cần làm rõ

1. **Luồng màn hình (Screen Flow):** bản sao đã lưu tại `../assets/diagrams/image11.png` và đã được **mở/kiểm tra hiển thị** ngày 2026-10-19; tuy nhiên **chưa chuyển ngữ nghĩa từng nút/cạnh**, nên chưa có đặc tả luồng văn bản đầy đủ.
2. **Màn hình "Quên mật khẩu" (Hình 4):** xuất hiện trong thiết kế màn hình, nhưng chương 3 không có use case "Quên mật khẩu". Cần làm rõ phạm vi.
3. **Màn hình "Quản lý hàng trong kho" (Hình 13):** chưa rõ tương ứng use case nào trong danh sách (UC004, UC008 hoặc UC014). Cần làm rõ.
4. **Quan hệ giữa Hình 5 "Màn trang chủ", Hình 8 "Trang chủ nhân viên" và Hình 14 "Trang chủ quản lý":** có ba chú thích liên quan "trang chủ"; cần làm rõ màn hình nào dùng cho vai trò nào.
5. **Số trang** trong danh mục hình vẽ có thể lệch so với bố cục thực tế; cần rà soát lại.

---

## 8. Checklist hoàn thành (file này)

- [x] Tham chiếu mục CHƯƠNG 4 › III (Screen Flow) với bản sao byte-identical, không vẽ lại.
- [x] Trích 12 caption màn hình Hình 3–Hình 14 nguyên văn.
- [x] Trích 2 caption chứng từ giấy Hình 1–Hình 2 nguyên văn kèm mô tả văn bản.
- [x] Liệt kê tệp hình tương ứng trong DOCX để định vị.
- [x] Ghi rõ bản sao byte-identical tại `../assets/`; ảnh đã mở/kiểm tra hiển thị (2026-10-19); diễn giải ngữ nghĩa còn chờ rà soát.
- [x] Tham chiếu diagram use case mục CHƯƠNG 3 mà không diễn giải nội dung.
- [x] Liệt kê màn hình được nhắc trong đặc tả use case.
- [x] Phân biệt thiết kế báo cáo với mã nguồn hiện tại.
- [x] Ghi nhận số trang là giá trị mục lục DOCX, cached/unverified.
