# Tài liệu tham chiếu DTAQ

Đây là bộ tài liệu dẫn xuất từ báo cáo phân tích và thiết kế của dự án **Hệ thống quản lý siêu thị Điện máy DTAQ**. Tài liệu dùng để tra cứu phạm vi, yêu cầu và use case khi triển khai; **không thay thế** tệp DOCX gốc.

## 1. Nguồn và phương pháp

| Hạng mục | Nội dung |
|---|---|
| Tệp nguồn | [`docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Tên báo cáo | Báo cáo môn học Phát triển ứng dụng – Hệ thống quản lý siêu thị Điện máy DTAQ |
| Nhóm thực hiện | Nhóm 04 – Lớp DHKTPM20A_KSTN |
| Phiên bản | v12 (theo tên tệp nguồn) |
| Ngày trên bìa | 10/1/2026; Học kỳ 01, năm học 2025–2026 |
| Metadata DOCX | `docProps/core.xml`: `dcterms:created = 2026-10-18T16:41:00Z` |
| Cách ghi nhận nội dung | Đọc trực tiếp DOCX; ghi lại nội dung bảng và heading nguồn, đối chiếu chéo giữa các mục; không hiệu đính câu chữ nguồn |
| Ngày cập nhật tài liệu | 2026-10-19 |
| Phạm vi tài liệu | Chương 1 mục II–V: phạm vi, tác nhân, FR001–FR016, NFR001–NFR017, 08 Q&A. Chương 3 mục II–IV: 02 tác nhân, UC001–UC019. Chương 4 mục I–IV: 14 đặc tả thực thể, **06 kiểu liệt kê**, 66 ràng buộc `RB01`–`RB66` trong 09 nhóm, quy tắc liên tính năng, screen flow và 37 asset byte-identical (23 diagrams, 14 screens; xem `assets/manifest.md`). 37 tệp đã được **xem/kiểm tra hiển thị** ngày 2026-10-19; **chuyển ngữ nghĩa chi tiết chờ rà soát**. Chương 4 mục V (kiểm thử): heading tồn tại nhưng **không chuyển nội dung kiểm thử** và không tạo tài liệu kiểm thử. |
| Định vị | Theo tên chương/mục và tiêu đề use case |
| Số trang | Số trang cho **chương/mục/UC** là **unverified**: DOCX có `TOC` field instruction nhưng không có nội dung mục lục đã lưu trong body. Ngoại lệ: các giá trị trang trong **danh mục hình vẽ** (List of Figures) đã có trong DOCX, được ghi là **cached/unverified** (xem `ui/screen-flow-and-screens.md`) và vẫn cần rà soát. |

## 2. Cấu trúc tài liệu

| Tệp | Nội dung | Dùng khi |
|---|---|---|
| [requirements/scope-and-requirements.md](requirements/scope-and-requirements.md) | Phạm vi, tác nhân, FR001–FR016, NFR001–NFR017, quy tắc nghiệp vụ, xung đột nguồn | Cần xác định một chức năng có thuộc phạm vi hay không; cần nguyên văn yêu cầu |
| [use-cases/catalog.md](use-cases/catalog.md) | Danh mục 19 use case, tên/tác nhân/mô tả ngắn theo nguồn, đặc tả bổ sung, khác biệt tên gọi | Cần tra cứu nhanh một UC hoặc đối chiếu danh mục với đặc tả |
| [traceability.md](traceability.md) | Ánh xạ tường minh **UC → tác nhân và UC → thực thể**; quan hệ **FR ↔ UC chỉ là ứng viên/chưa xác nhận**; khoảng trống và câu hỏi được ghi lại | Cần kiểm tra độ phủ hoặc lập kế hoạch truy vết |
| [business-rules/rules-and-exceptions.md](business-rules/rules-and-exceptions.md) | Quy tắc nghiệp vụ, ngoại lệ và điều kiện phê duyệt | Cần đối chiếu hành vi với quy tắc nghiệp vụ |
| [ui/screen-flow-and-screens.md](ui/screen-flow-and-screens.md) | Luồng màn hình và thiết kế màn hình; liên kết ảnh đã sao chép; trang List of Figures ghi cached/unverified | Cần tra cứu luồng điều hướng, bố cục màn hình |
| [assets/manifest.md](assets/manifest.md) | Manifest 37 tệp hình byte-identical: part nguồn, đích, chú thích/mục, SHA-256 | Cần kiểm tra nguồn gốc, toàn vẹn hoặc liên kết ảnh |
| [domain/class-model-and-entities.md](domain/class-model-and-entities.md) | Mô hình lớp, thực thể và kiểu liệt kê (Chương 4, mục I) | Cần tra cứu thuộc tính, kiểu dữ liệu, quan hệ |
| [database/schema-and-integrity.md](database/schema-and-integrity.md) | Thiết kế CSDL, ràng buộc toàn vẹn, khóa, miền giá trị (Chương 4, mục II) | Cần tra cứu bảng, ràng buộc, quy tắc lưu trữ |
| [use-cases/UC0xx-*.md](use-cases/) | Đặc tả chi tiết từng use case (một tệp cho mỗi UC) | Cần luồng chính/phụ, tiền/hậu điều kiện ở mức chi tiết |
| [plans/work-assignment-4-members.md](plans/work-assignment-4-members.md) | Phân công 4 thành viên: UC, module mở rộng, hợp đồng DAO, sở hữu file, lộ trình, DoD | Cần biết ai làm gì, thứ tự tích hợp, điểm chặn còn lại |
| [plans/guide-quy.md](plans/guide-quy.md) · [guide-tuan-anh.md](plans/guide-tuan-anh.md) · [guide-tai.md](plans/guide-tai.md) | Hướng dẫn code theo từng UC: kỹ thuật nền, các bước code, test, lỗi thường gặp | Thành viên cần biết **cách làm** một UC cụ thể |

**Tài liệu kiểm thử:** các heading kiểm thử ở **Chương 4 mục V** đã được xem xét; **không chuyển nội dung kiểm thử** và không tạo tệp kiểm thử vì nội dung kiểm thử khả dụng chưa được xác nhận. Đây là **bỏ qua có điều kiện** (conditional/omitted), không phải khẳng định nguồn không có nội dung kiểm thử.

**Sơ đồ và hình màn hình:** 37 part (`word/media/<name>`) đã được sao chép **byte-identical** vào `assets/diagrams/` (23) và `assets/screens/` (14); xem [assets/manifest.md](assets/manifest.md). 37 tệp đã được **mở/kiểm tra hiển thị** ngày 2026-10-19; tài liệu **không vẽ lại** và **chưa chuyển ngữ nghĩa chi tiết** — diễn giải ngữ nghĩa còn **chờ rà soát**.

Thứ tự đọc đề nghị: **phạm vi và yêu cầu → danh mục use case → truy vết**; tra cứu thêm quy tắc nghiệp vụ, luồng màn hình, tệp use case, mô hình lớp hoặc CSDL khi cần chi tiết.

## 3. Quy ước trạng thái

- **Xác nhận**: nội dung có câu chữ trực tiếp trong nguồn; kèm định vị chương/mục.
- **Chưa xác nhận**: nguồn không nêu ánh xạ hoặc không đủ dữ kiện; không suy diễn thành yêu cầu.
- **Mâu thuẫn**: hai vị trí trong nguồn nói khác nhau; cả hai được ghi lại, không tự chọn một bên.
- **Trích nguyên văn**: nội dung trong dấu ngoặc kép hoặc khối trích dẫn giữ nguyên cách viết của nguồn, kể cả lỗi chính tả.
- **Định vị trang**: dùng chương/mục/tiêu đề UC; số trang cho chương/mục/UC là **unverified** vì body không có nội dung mục lục đã lưu. Giá trị trang trong **danh mục hình vẽ** (List of Figures) là **cached/unverified**, đã ghi trong `ui/screen-flow-and-screens.md`; vẫn cần rà soát lại.

## 4. Tóm tắt độ phủ nguồn

- **02 tác nhân**: Nhân viên bán hàng; Nhân viên quản lý (Chương 3, mục II).
- **16 yêu cầu chức năng**: FR001–FR016 (Chương 1, mục IV).
- **17 yêu cầu phi chức năng**: NFR001–NFR017 (Chương 1, mục V).
- **19 use case**: UC001–UC019 (Chương 3, mục III–IV).
- **14 đặc tả thực thể** (Chương 4, mục I.3) và **06 kiểu liệt kê** (Chương 4, mục I.4).
- **66 ràng buộc toàn vẹn** `RB01`–`RB66` trong **09 nhóm** (Chương 4, mục II.3).
- **12 hình màn hình** Hình 3–14 (Chương 4, mục IV): ảnh gốc vẫn trong DOCX và bản sao byte-identical nằm tại `assets/screens/`; đã **xem hình (visual viewing) hoàn tất** ngày 2026-10-19, **chuyển ngữ nghĩa (semantic transcription) còn chờ rà soát**.
- **08 câu hỏi làm rõ yêu cầu** (Chương 1, mục III) là nguồn cho các quy tắc thanh toán tiền mặt, tra cứu khách hàng, phê duyệt đổi/trả, giá/thuế, hủy giao dịch, hàng tồn lâu ngày, khóa tài khoản và ngừng kinh doanh sản phẩm.

## 5. Cảnh báo chính

Các điểm dưới đây đã được kiểm chứng trong nguồn và được ghi chi tiết tại [mục 6 của tài liệu yêu cầu](requirements/scope-and-requirements.md#6-xung-đột-và-điểm-chưa-xác-nhận):

1. UC014 có tên danh mục “Báo cáo sản phẩm tồn kho **hằng ngày**”, nhưng tiêu đề đặc tả chi tiết và FR014 dùng “**lâu ngày**”. Không có mục lục đã lưu để đối chiếu thêm.
2. UC007–UC013 mang tên “Quản lý …” trong danh mục nhưng đặc tả chỉ là “Thêm …”.
3. UC008–UC011 có tác nhân danh mục là “Quản lý” nhưng đặc tả ghi “Nhân viên bán hàng”.
4. Phạm vi Chương 1 loại trừ phân hệ khuyến mãi, trong khi UC012 và luồng UC003 vẫn mô tả khuyến mãi. **Đã giải quyết ngày 2026-10-24:** khuyến mãi cơ bản thuộc phạm vi — xem §5.1.
5. FR015/FR016/NFR011 yêu cầu sao lưu – phục hồi nhưng danh mục 19 UC không có use case tương ứng.
6. Không có bảng ánh xạ tường minh FR ↔ UC trong nguồn; 16 FR và 19 UC không tương ứng một-một.

### 5.1. Quyết định implementation đã duyệt (2026-10-20 và 2026-10-24)

- **Mã định danh:** do **ứng dụng** sinh khi lưu mới thành công; giữ đúng định dạng đã tài liệu hóa (`TIỀN_TỐ-YYYY-NNNNNN`, ví dụ `NVQUANLY-YYYY-NNNNNN`, `NVBANHANG-YYYY-NNNNNN`); V1/PostgreSQL không có sequence, identity hay default. Các bước nhập mã trong DOCX vẫn giữ nguyên; xung đột nguồn C-12/G-10/BR-C11 được lưu làm bằng chứng lịch sử, không bị sửa.
- **Mô hình hóa đơn/đổi trả:** V1 amendment đã duyệt (`ChiTietHoaDon` theo serial + snapshot tại thời điểm bán; `SanPhamLoi` theo serial và nguồn phiếu trả/đổi) là implementation hiện tại; xung đột thiết kế DOCX G-11/BR-C12 vẫn được giữ làm bằng chứng lịch sử. Xem [domain §8](domain/class-model-and-entities.md#8-bản-đồ-v1-đã-phê-duyệt) và [database §6](database/schema-and-integrity.md#6-bản-đồ-ddl-v1-đã-phê-duyệt).
- **Phạm vi khuyến mãi (C-04, chốt 2026-10-24):** khuyến mãi **cơ bản thuộc phạm vi** — UC012 quản lý chương trình, UC003 áp dụng khuyến mãi trên từng dòng sản phẩm ở bước 7–8; câu “không xây dựng phân hệ khuyến mãi” tại Ch.1 §II.6/§II.8 là nội dung cũ chưa cập nhật. Voucher, tích điểm và khuyến mãi chuyên sâu vẫn ngoài phạm vi. DOCX không bị sửa. Chi tiết: [requirements §6 → C-04](requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case).
- **Quy tắc làm tròn tiền (C-11, chốt 2026-10-24):** đơn vị **đồng**, **0 chữ số thập phân**, chế độ **`HALF_UP`**; một điểm làm tròn duy nhất trong `TienUtil` (`BigDecimal`); làm tròn ở **cấp chi tiết** (giảm giá, thuế) rồi cộng thành tổng; DB lưu giá trị đã tròn; giao diện/Excel chỉ định dạng dạng `1.250.000 VND`. Chi tiết: [requirements §6 → C-11](requirements/scope-and-requirements.md#c-11--quy-tắc-làm-tròn-tiền-đã-chốt-2026-10-24).
- **Hóa đơn "in" (chốt 2026-10-24):** chỉ **giả lập hiển thị phiếu trên màn hình** (`HoaDonPreviewDialog`); **không** in ra máy in, không xuất PDF.
- **Báo cáo – thống kê (chốt 2026-10-25):** UC014 → Tài; UC015 → Quý; UC016 → Tuấn Anh; UC017 → Tài; **cả 4 báo cáo đều có xuất Excel** từ đúng kết quả đang hiển thị (`ExcelExporter` do Đăng cung cấp). Đây là **mở rộng đã duyệt** so với nguồn (nguồn chỉ quy định xuất Excel cho UC017 — FR011); lỗi ghi file phải thông báo và giữ báo cáo trên màn hình.
- **Sao lưu – phục hồi (C-05, chốt 2026-10-24):** triển khai dù không có UC — **cron 00:00 hằng ngày** cho dữ liệu quan trọng, kèm **chạy bù khi khởi động** nếu bản gần nhất đã quá 24 giờ (ứng dụng desktop chỉ chạy khi đang mở); sao lưu bằng JDBC `COPY … TO STDOUT` (không phụ thuộc `pg_dump`), có `manifest` + SHA-256; phục hồi phải xác nhận 2 bước, tự sao lưu hiện trạng trước và chạy trong một transaction.
- **Cấu hình sao lưu (chốt 2026-10-25):** thư mục `backup.dir` mặc định `./backups` (thêm vào `.gitignore`), giữ **7 bản** (`backup.keep.days=7`), **nén zip** kèm `manifest.json`; phạm vi **cả 14 bảng, gồm `NhanVien`** (nếu thiếu thì restore xong không đăng nhập được) — tệp backup chứa hash BCrypt nên **không commit, không chia sẻ**; múi giờ chạy `Asia/Ho_Chi_Minh`.
- **Excel (chốt 2026-10-25):** duyệt thêm dependency `org.apache.poi:poi-ooxml` (5.4.1) + `log4j-to-slf4j` để đi qua Logback; chỉ dùng trong `util/ExcelExporter`; chỉ Đăng sửa `pom.xml`. Nếu giảng viên không cho thêm thư viện → fallback CSV UTF-8 BOM và **không** đặt tên `.xlsx`.
- **UC014 — định nghĩa "còn tồn" (chốt 2026-10-25):** tính các trạng thái `SAN_SANG_BAN`, `HANG_LOI`, `DANG_BAO_HANH`; **loại** `DA_BAN`, `DA_TRA_NHA_CUNG_CAP`, `DA_THANH_LY`. Ô ngưỡng ngày được **điền sẵn 60** (chỉ là gợi ý hiển thị, logic do người dùng nhập); có checkbox "Chỉ tính máy sẵn sàng bán" (mặc định tắt).
- **Quyền cập nhật giá/thuế (C-08, chốt 2026-10-25):** **chỉ Nhân viên quản lý** được sửa `donGiaBanTruocThue`/`thue` (UC019); nhân viên bán hàng chỉ đọc, kể cả trong màn hình lập hóa đơn; không xây luồng phê duyệt giá riêng.
- **Thời hạn đổi/trả (chốt 2026-10-25):** cấu hình `return.window.days`; **để trống = không chặn** (nguồn không quy định số ngày — `RB34`).
- **Chồng lấn khuyến mãi (chốt 2026-10-25):** chỉ chặn khi **cả hai** chương trình `dangApDung = true` và khoảng ngày giao nhau; chương trình đã dừng vẫn được lưu làm lịch sử.
- **Phân công 4 thành viên (2026-10-25):** Đăng — UC003, UC006, UC018, FR015/FR016 + hạ tầng dùng chung; Quý — UC004, UC008, UC009, UC019, UC015; Tuấn Anh — UC005, UC007, UC013, UC016; Tài — UC010, UC011, UC012, UC014, UC017. Xem [phân công](plans/work-assignment-4-members.md) và [hướng dẫn code](plans/guide-quy.md).

Các xung đột **chưa được quyết định** như C-07 (mức hạt tồn kho serial hay số lượng) vẫn giữ nguyên trạng thái và không bị coi là đã giải quyết.

## 6. Giới hạn

- Không chứa mã nguồn, kế hoạch kiểm thử hoặc nội dung kiểm thử. Hình ảnh (sơ đồ, màn hình) đã được sao chép byte-identical vào `assets/` và đã mở/kiểm tra hiển thị ngày 2026-10-19, nhưng **chưa chuyển ngữ nghĩa chi tiết**; tài liệu kiểm thử được bỏ qua có điều kiện vì chưa xác nhận nội dung.
- DOCX gốc không bị chỉnh sửa; mã nguồn ứng dụng không bị thay đổi. `AGENTS.md` có chứa con trỏ điều hướng/điểm rủi ro tới bộ tài liệu tham chiếu này (đã được cập nhật theo nhiệm vụ được duyệt), nên không được hiểu là `AGENTS.md` nguyên trạng.
- Không có page locator đáng tin cho chương/mục/UC; số trang là **unverified** (trang trong danh mục hình vẽ là **cached/unverified**), không thay thế việc đọc lại DOCX khi có tranh chấp.
- Khi nguồn mâu thuẫn, tài liệu giữ nguyên mâu thuẫn và yêu cầu xác nhận, không tự ra quyết định nghiệp vụ. Các quyết định implementation đã được người dùng phê duyệt được ghi riêng tại mục 5.1 và không sửa câu chữ DOCX.

## 7. Cập nhật tài liệu

1. Chỉ cập nhật khi DOCX nguồn thay đổi hoặc có quyết định nghiệp vụ được duyệt.
2. Ghi lại phiên bản/ngày nguồn và ngày cập nhật tài liệu mới.
3. Giữ nguyên ID FR/NFR/UC; không đánh số lại.
4. Nếu xung đột được giải quyết, ghi rõ quyết định và người/nhóm phê duyệt; không âm thầm sửa theo một bên.
5. Cập nhật đồng thời các liên kết và bảng truy vết liên quan.
