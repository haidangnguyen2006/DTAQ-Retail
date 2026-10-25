# Phạm vi và yêu cầu

Tài liệu ghi lại từ báo cáo v12 của Nhóm 04, dùng để tra cứu phạm vi, tác nhân và nguyên văn các yêu cầu. Nguồn chuẩn: [`docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx).

**Phương pháp và định vị:** nội dung được ghi lại từ DOCX và đối chiếu trực tiếp với bảng/tiêu đề nguồn (ngày cập nhật tài liệu: 2026-10-19); câu chữ dưới đây giữ nguyên nguồn (kể cả lỗi chính tả). DOCX có `TOC` field instruction nhưng không có nội dung mục lục đã lưu trong body; do đó **không dùng số trang cho chương/mục/UC** — trang là **unverified**. Định vị dùng chương/mục/tiêu đề. Xem [quy ước chung](../README.md#3-quy-ước-trạng-thái).

**Liên kết:** [Danh mục use case](../use-cases/catalog.md) · [Truy vết](../traceability.md) · [README](../README.md)

## 1. Phạm vi đề tài

Nguồn: Chương 1, mục II “Phạm vi đề tài”. Hệ thống là phần mềm quản lý nội bộ tại **một cửa hàng**, chỉ phục vụ nhân sự nội bộ; không có cổng/tài khoản cho khách hàng tự đăng nhập.

| Nhóm phạm vi | Nội dung được nêu trong nguồn | Định vị |
|---|---|---|
| Người dùng | Hai nhóm: **Nhân viên quản lý** và **Nhân viên bán hàng**; phân quyền theo trách nhiệm nghiệp vụ | Ch.1 §II.1; Ch.3 §II |
| Dữ liệu | Tài khoản, nhân viên, khách hàng, loại sản phẩm, sản phẩm, hóa đơn bán lẻ, chi tiết hóa đơn, phiếu đổi/trả, chi tiết đổi/trả, sản phẩm lỗi | Ch.1 §II.2 |
| Bán hàng | Chọn sản phẩm; kiểm tra tồn; đơn giá trước thuế; thuế tính riêng; không tự đổi giá/thuế/chiết khấu; tự động tính thành tiền và tổng; lưu giá/thuế tại thời điểm bán; **chỉ thanh toán tiền mặt**; hoàn tất khi thanh toán đủ; giảm tồn sau giao dịch | Ch.1 §II.3; Q&A 1 |
| Đổi/trả | Gắn hóa đơn gốc; không nhận hàng không xác định được giao dịch; đổi sản phẩm hoặc hoàn tiền; trả thêm phần chênh lệch; hoàn tiền từ **3.000.000 VNĐ** phải được Quản lý phê duyệt; cập nhật tồn; ghi nhận hàng lỗi; dùng giá/thuế đã lưu trên hóa đơn gốc | Ch.1 §II.4; Q&A 3 |
| Thống kê, báo cáo | Doanh thu theo ngày/tuần/tháng/năm/khoảng thời gian; doanh số theo nhân viên và sản phẩm; báo cáo tồn kho lâu ngày (tham chiếu **trên 60 ngày**); kết xuất dữ liệu; doanh thu nội bộ theo giá trị trước thuế đã điều chỉnh đổi/trả, thuế hiển thị riêng | Ch.1 §II.5; Q&A 6 |
| Khuyến mãi | Câu chữ nguồn: **không** xây dựng phân hệ quản lý khuyến mãi hoàn chỉnh; chỉ hỗ trợ gián tiếp bằng dữ liệu tồn kho, thời gian tồn, doanh số và báo cáo tồn lâu ngày. **Câu chữ này đã lỗi thời theo quyết định 2026-10-24:** khuyến mãi cơ bản (quản lý ở UC012, áp dụng trên từng dòng sản phẩm ở UC003 bước 7–8) **thuộc phạm vi** — xem [C-04](#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case) | Ch.1 §II.6; quyết định 2026-10-24 |
| Sao lưu, phục hồi | Quản lý được sao lưu và phục hồi dữ liệu hóa đơn, dữ liệu khách hàng | Ch.1 §II.7 |
| Ngoài phạm vi | Website/đặt hàng trực tuyến; giao hàng; thanh toán trực tuyến/QR/thẻ/ví; công nợ; trả góp; nhà cung cấp chuyên sâu; chuỗi cung ứng; nhân sự chuyên sâu/chấm công/tính lương/phân ca; khuyến mãi – voucher – điểm thành viên chuyên sâu; CRM; thương mại điện tử; đa chi nhánh; ERP/kế toán; kê khai/quyết toán thuế, hóa đơn điện tử | Ch.1 §II.8 |
| Kỹ thuật | Ứng dụng desktop nội bộ, kết nối CSDL tập trung; kiến trúc phân lớp `gui`, `entity`, `dao`, `connect` | Ch.1 §II.9; NFR012 |

> **Cập nhật phạm vi 2026-10-24 (C-04):** cụm “khuyến mãi – voucher – điểm thành viên **chuyên sâu**” trong mục loại trừ vẫn **ngoài phạm vi**; khuyến mãi **cơ bản** theo UC012 thì **thuộc phạm vi**. DOCX không bị sửa; quyết định ghi tại [C-04](#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case).

**Kết luận phạm vi (nguyên văn rút gọn):** “số hóa các nghiệp vụ nội bộ cốt lõi của một siêu thị điện máy tại một cửa hàng”, gồm quản lý nhân viên, khách hàng, sản phẩm và tồn kho; lập hóa đơn bán lẻ; đổi/trả; thống kê – báo cáo; sao lưu – phục hồi (Ch.1 §II, “Kết luận phạm vi”).

## 2. Tác nhân

Nguồn: Chương 3, mục II “Danh sách các tác nhân và mô tả”.

| Tác nhân | Mô tả trong nguồn | Ghi chú trong nguồn |
|---|---|---|
| Nhân viên bán hàng | Thực hiện nghiệp vụ bán hàng: tra cứu sản phẩm và tồn kho, kiểm tra máy còn đủ để bán; lập hóa đơn bán lẻ, áp dụng khuyến mãi, thanh toán và phát hành hóa đơn; xử lý đổi trả sản phẩm; tra cứu khách hàng, lịch sử mua hàng, hóa đơn và phiếu đổi trả; thêm và cập nhật thông tin khách hàng. | Phải đăng nhập. Yêu cầu đổi trả cần được nhân viên quản lý phê duyệt. |
| Nhân viên quản lý | Quản lý sản phẩm, dòng sản phẩm, danh mục và sản phẩm lỗi; tiếp nhận hàng và ghi nhận số serial; quản lý chương trình khuyến mãi, nhân viên bán hàng và tài khoản đăng nhập; xem báo cáo tồn kho, thống kê doanh số theo sản phẩm, nhân viên và doanh thu, xuất báo cáo Excel; phê duyệt yêu cầu đổi trả; cập nhật giá bán và thuế suất. | Phải đăng nhập bằng tài khoản có quyền quản lý. |

> **Mâu thuẫn tác nhân:** mô tả tác nhân ở Ch.3 §II giao quản lý sản phẩm/dòng sản phẩm/danh mục/sản phẩm lỗi cho **Nhân viên quản lý**, trong khi Ch.1 §II.1.b và đặc tả UC008–UC011 giao cho **Nhân viên bán hàng**. Xem [C-03](#c-03--tác-nhân-uc008uc011-không-thống-nhất).

## 3. Yêu cầu chức năng (FR001–FR016)

Nguồn: Chương 1, mục IV “Yêu cầu chức năng”. Giữ nguyên ID, tên và mô tả.

### FR001 — Đăng nhập

> Hệ thống cho phép Nhân viên quản lý và Nhân viên bán hàng đăng nhập bằng tài khoản được cấp. Hệ thống phải xác thực thông tin đăng nhập, kiểm tra trạng thái tài khoản và xác định quyền người dùng trước khi cho phép truy cập các chức năng tương ứng.

### FR002 — Đăng xuất

> Người dùng đang đăng nhập có thể kết thúc phiên làm việc và trở về màn hình đăng nhập. Sau khi đăng xuất, các chức năng yêu cầu xác thực không được tiếp tục truy cập bằng phiên làm việc cũ.

### FR003 — Quản lý nhân viên bán hàng

> Nhân viên quản lý có thể thêm mới, cập nhật và tra cứu thông tin Nhân viên bán hàng; đồng thời tạo và quản lý tài khoản đăng nhập tương ứng. Khi nhân viên nghỉ việc, tài khoản được chuyển sang trạng thái khóa thay vì xóa nhằm bảo toàn hóa đơn, doanh số và dữ liệu lịch sử liên quan.

### FR004 — Quản lý thông tin khách hàng

> Nhân viên bán hàng có thể thêm mới, cập nhật và tra cứu thông tin khách hàng. Hệ thống cho phép tìm khách hàng thông qua số điện thoại và tra cứu lịch sử mua hàng để hỗ trợ xác minh giao dịch, bảo hành hoặc xuất lại chứng từ khi khách hàng làm mất hóa đơn.

### FR005 — Quản lý danh mục loại sản phẩm

> Nhân viên bán hàng có thể tạo mới, cập nhật và tra cứu các loại sản phẩm dùng để phân nhóm các mặt hàng điện máy. Mỗi sản phẩm phải thuộc một loại sản phẩm cụ thể.

### FR006 — Quản lý thông tin sản phẩm và tồn kho

> Nhân viên bán hàng có thể thêm mới, cập nhật, tra cứu sản phẩm và theo dõi số lượng tồn kho. Hệ thống lưu các thông tin như mã sản phẩm, tên sản phẩm, loại sản phẩm, đơn giá bán trước thuế, thuế suất áp dụng, đơn vị tính, số lượng tồn, ngày nhập kho và trạng thái kinh doanh. Việc cập nhật giá bán hoặc thuế suất phải dựa trên thông tin đã được Quản lý phê duyệt. Sản phẩm ngừng kinh doanh được chuyển trạng thái thay vì xóa vĩnh viễn để không ảnh hưởng đến dữ liệu hóa đơn và đổi/trả trước đó.

### FR007 — Lập hóa đơn bán lẻ

> Nhân viên bán hàng có thể lập hóa đơn cho khách hàng bằng cách lựa chọn khách hàng và các sản phẩm cần mua. Hệ thống kiểm tra số lượng tồn kho và lấy đơn giá bán trước thuế cùng thuế suất đang áp dụng của từng sản phẩm.
>
> Hệ thống tự động tính thành tiền trước thuế, tiền thuế và thành tiền sau thuế của từng mặt hàng; đồng thời tính tổng tiền trước thuế, tổng tiền thuế và tổng số tiền khách phải thanh toán.
>
> Đơn giá và thuế suất thực tế được sử dụng phải được lưu cùng Chi tiết hóa đơn để bảo toàn lịch sử giao dịch. Nhân viên bán hàng không được tự ý thay đổi đơn giá hoặc thuế suất trực tiếp khi lập hóa đơn.
>
> Giao dịch chỉ sử dụng hình thức thanh toán bằng tiền mặt; số lượng bán không được vượt quá tồn kho. Hóa đơn chỉ được hoàn tất sau khi khách hàng thanh toán đủ số tiền và tồn kho được cập nhật tương ứng sau khi giao dịch hoàn tất.

### FR008 — Xử lý hoàn trả/đổi sản phẩm

> Nhân viên bán hàng có thể tiếp nhận yêu cầu đổi hoặc trả hàng, tra cứu hóa đơn gốc, ghi nhận sản phẩm trả, số lượng, tình trạng hàng, lý do và hình thức xử lý.
>
> Giá trị của sản phẩm trả phải được xác định dựa trên đơn giá và thuế suất đã lưu trong Chi tiết hóa đơn gốc. Hệ thống hỗ trợ hai hình thức chính là đổi sang sản phẩm khác và hoàn tiền.
>
> Khi đổi sản phẩm, hệ thống kiểm tra tồn kho của sản phẩm mới và tính phần chênh lệch dựa trên giá trị thanh toán sau thuế của sản phẩm trả và sản phẩm đổi. Khi hoàn tiền, số tiền hoàn được xác định từ giá trị thực tế của sản phẩm trên giao dịch ban đầu.
>
> Trường hợp số tiền hoàn từ 3.000.000 VNĐ trở lên phải được Nhân viên quản lý phê duyệt trước khi giao dịch được hoàn tất. Tồn kho được cập nhật tương ứng sau khi xử lý thành công.

### FR009 — Xử lý giao dịch đã thanh toán nhưng khách hủy mua

> Hóa đơn đã hoàn tất thanh toán không được xóa hoặc hủy trực tiếp. Nếu khách hàng không tiếp tục mua, Nhân viên bán hàng phải xử lý thông qua nghiệp vụ đổi/trả để bảo toàn hóa đơn gốc, lịch sử giao dịch và dữ liệu tồn kho.

### FR010 — Thống kê doanh thu

> Nhân viên quản lý có thể xem số liệu bán hàng theo ngày, tuần, tháng, năm hoặc một khoảng thời gian xác định. Hệ thống tổng hợp riêng giá trị hàng hóa trước thuế, tổng tiền thuế và tổng số tiền thanh toán từ các hóa đơn hợp lệ.
>
> Chỉ tiêu doanh thu phục vụ thống kê nội bộ được xác định từ giá trị hàng hóa trước thuế và được điều chỉnh theo các giao dịch đổi/trả phát sinh trong khoảng thời gian được lựa chọn. Tiền thuế được hiển thị thành chỉ tiêu riêng.

### FR011 — Xuất báo cáo doanh thu

> Từ kết quả thống kê doanh thu, Nhân viên quản lý có thể kết xuất dữ liệu ra file Excel để lưu trữ, đối soát hoặc sử dụng trong công tác quản lý.

### FR012 — Thống kê doanh số theo nhân viên

> Nhân viên quản lý có thể lựa chọn khoảng thời gian và xem số lượng hóa đơn, doanh số bán hàng tương ứng của từng Nhân viên bán hàng nhằm phục vụ theo dõi hiệu quả hoạt động. Doanh số theo nhân viên được xác định theo giá trị hàng hóa trước thuế của các hóa đơn do nhân viên đó lập, sau khi tính đến các giao dịch đổi/trả liên quan.

### FR013 — Thống kê doanh số theo sản phẩm

> Nhân viên quản lý có thể thống kê số lượng bán và doanh số của từng sản phẩm trong khoảng thời gian xác định để theo dõi tình hình kinh doanh của các mặt hàng. Doanh số theo sản phẩm được xác định theo giá trị hàng hóa trước thuế và số lượng thực bán của từng sản phẩm sau khi điều chỉnh các giao dịch đổi/trả liên quan.

### FR014 — Báo cáo hàng tồn kho lâu ngày

> Nhân viên quản lý có thể xem và lọc danh sách những sản phẩm tồn kho trong thời gian dài, đặc biệt theo ngưỡng như trên 60 ngày, nhằm cung cấp dữ liệu hỗ trợ việc xem xét giảm giá, khuyến mãi hoặc xả hàng.

> **Làm rõ nguồn (không phải mâu thuẫn):** FR014 nêu ngưỡng “như trên 60 ngày” với tính chất **tham chiếu**. Đặc tả UC014 để Quản lý **tự nhập ngưỡng số ngày tồn kho** (“Nhập ngưỡng số ngày tồn kho”), và nguồn **không quy định giá trị mặc định**. Vì vậy 60 ngày là mốc tham khảo, không phải giá trị mặc định bắt buộc.

### FR015 — Sao lưu dữ liệu

> Nhân viên quản lý có thể tạo bản sao lưu đối với các dữ liệu quan trọng nằm trong phạm vi đề tài, đặc biệt là dữ liệu hóa đơn và khách hàng, nhằm hạn chế nguy cơ mất dữ liệu.

### FR016 — Phục hồi dữ liệu

> Nhân viên quản lý có thể lựa chọn bản sao lưu hợp lệ để phục hồi dữ liệu hóa đơn và khách hàng khi xảy ra mất mát hoặc sự cố dữ liệu.

## 4. Yêu cầu phi chức năng (NFR001–NFR017)

Nguồn: Chương 1, mục V “Yêu cầu phi chức năng”. Giữ nguyên ID, nhóm và nội dung.

| ID | Nhóm | Yêu cầu |
|---|---|---|
| NFR001 | Bảo mật – Xác thực | Mọi người dùng phải đăng nhập bằng tài khoản hợp lệ trước khi truy cập các chức năng nghiệp vụ. Tài khoản bị khóa không được phép đăng nhập vào hệ thống. |
| NFR002 | Bảo mật – Phân quyền | Hệ thống phải phân biệt rõ quyền của Nhân viên quản lý và Nhân viên bán hàng. Người dùng không được truy cập chức năng nằm ngoài quyền hạn của mình, kể cả khi cố truy cập trực tiếp từ giao diện hoặc luồng xử lý khác. |
| NFR003 | Bảo mật dữ liệu | Dữ liệu hóa đơn, doanh thu, tài khoản và báo cáo quản lý phải được bảo vệ khỏi truy cập trái phép. Các báo cáo tổng hợp và chức năng sao lưu/phục hồi chỉ được phép sử dụng bởi Nhân viên quản lý. |
| NFR004 | Hiệu năng | Các thao tác thông thường như đăng nhập, tìm kiếm khách hàng, tra cứu sản phẩm, kiểm tra tồn kho và xử lý hóa đơn phải phản hồi trong thời gian không quá 3 giây trong điều kiện vận hành bình thường tại cửa hàng. |
| NFR005 | Hiệu năng thống kê | Các báo cáo thống kê phải được xử lý trong thời gian hợp lý với quy mô dữ liệu của cửa hàng và không được làm gián đoạn các thao tác bán hàng đang diễn ra. |
| NFR006 | Khả năng sử dụng | Giao diện phải trực quan, dễ hiểu và phù hợp với nhân viên không có nhiều kiến thức công nghệ. Tên chức năng, nút lệnh và thông báo phải sử dụng tiếng Việt rõ ràng, thống nhất với thuật ngữ nghiệp vụ của cửa hàng. |
| NFR007 | Khả năng sử dụng | Các chức năng thường xuyên sử dụng tại quầy như tìm khách hàng, tìm sản phẩm, nhập số lượng và lập hóa đơn phải được bố trí để hạn chế số bước thao tác không cần thiết; ưu tiên hỗ trợ thao tác nhanh bằng bàn phím ở những vị trí phù hợp. |
| NFR008 | Tính toàn vẹn dữ liệu | Hệ thống phải bảo đảm các quan hệ giữa hóa đơn, khách hàng, nhân viên, sản phẩm và Phiếu Đổi/Trả không bị mất hoặc sai lệch. Không được xóa dữ liệu đang được tham chiếu bởi các giao dịch lịch sử nếu thao tác đó làm mất tính toàn vẹn dữ liệu. Đơn giá và thuế suất đã lưu trong Chi tiết hóa đơn không được tự động thay đổi khi thông tin giá hoặc thuế suất hiện tại của sản phẩm được cập nhật. |
| NFR009 | Tính nhất quán giao dịch | Các nghiệp vụ có nhiều bước cập nhật dữ liệu như lập hóa đơn hoặc đổi/trả phải bảo đảm tính nhất quán. Nếu quá trình lưu dữ liệu thất bại, hệ thống không được để lại trạng thái chỉ cập nhật một phần, ví dụ đã lưu hóa đơn nhưng chưa trừ kho hoặc đã hoàn tiền nhưng chưa ghi nhận Phiếu Đổi/Trả. Việc lưu hóa đơn, lưu Chi tiết hóa đơn, ghi nhận đơn giá và thuế suất tại thời điểm bán, cập nhật tồn kho và lưu các tổng tiền liên quan phải được thực hiện nhất quán trong cùng giao dịch nghiệp vụ. |
| NFR010 | Độ tin cậy | Khi xảy ra lỗi nhập liệu, lỗi nghiệp vụ hoặc lỗi kết nối cơ sở dữ liệu, hệ thống phải hiển thị thông báo rõ ràng và không được làm mất dữ liệu người dùng đã nhập nếu vẫn có thể tiếp tục thao tác. |
| NFR011 | Sao lưu và phục hồi | Hệ thống phải cung cấp khả năng sao lưu và phục hồi các dữ liệu quan trọng nằm trong phạm vi đề tài. Bản sao lưu phải có khả năng được lựa chọn và phục hồi khi xảy ra mất mát hoặc lỗi dữ liệu. |
| NFR012 | Khả năng bảo trì | Mã nguồn phải được tổ chức theo kiến trúc phân lớp, tách biệt giao diện, thực thể, truy xuất dữ liệu và kết nối cơ sở dữ liệu thông qua các nhóm gui, entity, dao và connect, nhằm giảm phụ thuộc giữa các thành phần và thuận tiện cho sửa đổi, kiểm thử và bảo trì. |
| NFR013 | Tính nhất quán giao diện | Các màn hình trong cùng hệ thống phải sử dụng cách bố trí, thuật ngữ, kiểu nút và hành vi điều hướng thống nhất. Sau khi đăng nhập, người dùng có thể chuyển giữa các section được cấp quyền thông qua màn hình chính và sidebar mà không cần mở nhiều cửa sổ không cần thiết. |
| NFR014 | Khả năng kiểm tra dữ liệu đầu vào | Hệ thống phải kiểm tra các trường bắt buộc, kiểu dữ liệu, giá trị số và các điều kiện nghiệp vụ trước khi ghi dữ liệu xuống cơ sở dữ liệu; các trường hợp không hợp lệ phải được thông báo cụ thể cho người dùng. |
| NFR015 | Khả năng truy vết | Các giao dịch quan trọng phải giữ được thông tin liên quan đến người thực hiện và chứng từ gốc, chẳng hạn hóa đơn phải xác định được nhân viên lập và Phiếu Đổi/Trả phải xác định được hóa đơn liên quan và người phê duyệt khi thuộc trường hợp cần phê duyệt. |
| NFR016 | Khả năng mở rộng | Thiết kế hệ thống cần cho phép bổ sung các chức năng trong tương lai như quản lý khuyến mãi chuyên sâu, thêm phương thức thanh toán hoặc mở rộng báo cáo mà không cần thay đổi toàn bộ kiến trúc ứng dụng hiện tại. |
| NFR017 | Độ chính xác tính toán tài chính | Hệ thống phải bảo đảm các phép tính liên quan đến thành tiền, tiền thuế và tổng số tiền thanh toán được thực hiện chính xác và nhất quán. Quy tắc làm tròn giá trị tiền tệ phải được áp dụng thống nhất trên giao diện, hóa đơn, dữ liệu lưu trữ và các báo cáo thống kê, tránh phát sinh chênh lệch giữa số tiền hiển thị và số tiền được lưu trong cơ sở dữ liệu. Việc tính toán không được dựa trên giá trị hiển thị đã làm tròn ở bước trung gian nếu điều đó có thể tạo ra sai lệch giữa tổng chi tiết và tổng hóa đơn. |

## 5. Quy tắc nghiệp vụ chính

Các quy tắc dưới đây là tổng hợp nguyên văn/rút gọn từ nguồn, không phải quy tắc mới.

| # | Quy tắc | Định vị |
|---|---|---|
| BR-01 | Chỉ hỗ trợ thanh toán bằng **tiền mặt**; hóa đơn chỉ hoàn tất khi khách thanh toán đủ 100% giá trị | Ch.1 §II.3; Q&A 1 |
| BR-02 | Số lượng bán phải kiểm tra với tồn kho; không cho phép bán vượt tồn | Ch.1 §II.3; FR007 |
| BR-03 | Đơn giá lưu trong hệ thống là **giá trước thuế**; thuế tính riêng theo thuế suất từng sản phẩm; nhân viên không được tự đổi giá, thuế hoặc nhập chiết khấu tùy ý | Ch.1 §II.3; FR006; FR007 |
| BR-04 | Đơn giá và thuế suất thực tế tại thời điểm bán phải lưu cùng Chi tiết hóa đơn; thay đổi giá/thuế hiện tại không làm thay đổi giao dịch cũ | Ch.1 §II.3; §II.4; FR007; NFR008 |
| BR-05 | Chỉ sản phẩm/máy có trạng thái `SAN_SANG_BAN` được bán | Ch.3 §III, ghi chú UC003 |
| BR-06 | Giao dịch đổi/trả phải gắn với hóa đơn gốc; không xử lý hàng không xác định được giao dịch mua trước đó | Ch.1 §II.4 |
| BR-07 | Hoàn tiền từ **3.000.000 VNĐ trở lên** phải được Nhân viên quản lý phê duyệt trước khi hoàn tất; nhân viên không được tự phê duyệt | Ch.1 §II.1; §II.4; FR008; Q&A 3; UC018 |
| BR-08 | Hàng trả đủ điều kiện được tăng tồn; hàng mới giao trong đổi hàng bị giảm tồn; hàng lỗi được ghi nhận riêng | Ch.1 §II.4 |
| BR-09 | Giá trị hàng trả dựa trên đơn giá và thuế suất đã lưu trên hóa đơn gốc; hoàn tiền dùng giá trị thực tế khách đã thanh toán; chênh lệch đổi hàng tính trên giá trị sau thuế | Ch.1 §II.4; FR008 |
| BR-10 | Không xóa/hủy trực tiếp hóa đơn đã thanh toán; nếu khách hủy mua, xử lý qua nghiệp vụ đổi/trả | Ch.1 §II.4; FR009; Q&A 5 |
| BR-11 | Báo cáo tồn kho lâu ngày dùng ngưỡng tham chiếu **trên 60 ngày**, chỉ hỗ trợ ra quyết định, không tự tạo khuyến mãi | Ch.1 §II.5; FR014; Q&A 6 |

> **Ghi chú BR-11 (làm rõ nguồn):** ngưỡng “trên 60 ngày” là mốc **tham chiếu**; UC014 cho Quản lý tự nhập ngưỡng và nguồn không nêu giá trị mặc định. Xem chú thích tại [FR014](#fr014--báo-cáo-hàng-tồn-kho-lâu-ngày).
| BR-12 | Doanh thu/doanh số nội bộ tính theo **giá trị trước thuế** sau điều chỉnh đổi/trả; tiền thuế là chỉ tiêu riêng | Ch.1 §II.5; FR010; FR012; FR013 |
| BR-13 | Không xóa vĩnh viễn nhân viên/sản phẩm có dữ liệu lịch sử: khóa tài khoản khi nghỉ việc, chuyển sản phẩm sang trạng thái ngừng kinh doanh | Q&A 7; Q&A 8; FR003; FR006 |
| BR-14 | Sao lưu/phục hồi tập trung vào dữ liệu hóa đơn và khách hàng | Ch.1 §II.7; FR015; FR016; NFR011 |

## 6. Xung đột và điểm chưa xác nhận

Các mục dưới đây **không được tự động giải quyết** trong tài liệu này.

### C-01 — Tiêu đề UC014

- Danh mục Ch.3 §III ghi: “Báo cáo sản phẩm tồn kho **hằng ngày**” và mô tả ngắn “Theo dõi lượng máy tồn theo ngày”.
- Tiêu đề đặc tả chi tiết Ch.3 §IV và FR014 ghi: “Báo cáo sản phẩm tồn kho **lâu ngày**” / “Báo cáo hàng tồn kho lâu ngày”.
- Không có nội dung mục lục đã lưu trong DOCX để đối chiếu thêm; không dùng mục lục làm căn cứ.
- Nội dung đặc tả dùng “chọn ngưỡng lưu kho để xem máy tồn lâu”, phù hợp hướng “lâu ngày”.
- **Trạng thái:** mâu thuẫn tên gọi; cần xác nhận tên chuẩn. Không tự đổi ID UC014.

### C-02 — Tên “Quản lý …” so với “Thêm …”

Các UC sau có tên danh mục khác tên đặc tả:

| UC | Tên danh mục (Ch.3 §III) | Tên đặc tả (Ch.3 §IV) |
|---|---|---|
| UC007 | Quản lý khách hàng | Thêm khách hàng |
| UC008 | Quản lý sản phẩm | Thêm sản phẩm |
| UC009 | Quản lý dòng sản phẩm | Thêm dòng sản phẩm |
| UC010 | Quản lý danh mục sản phẩm | Thêm danh mục sản phẩm |
| UC011 | Quản lý sản phẩm lỗi | Thêm sản phẩm lỗi |
| UC012 | Quản lý khuyến mãi | Thêm khuyến mãi |
| UC013 | Quản lý nhân viên bán hàng | Thêm nhân viên bán hàng |

Ngoài tên gọi, “Chức năng” trong danh mục thường rộng hơn luồng đặc tả: ví dụ UC010 có “thêm, cập nhật, xóa”, UC013 có “tra cứu, cập nhật, khóa”, nhưng đặc tả chỉ mô tả luồng **thêm**. **Trạng thái:** mâu thuẫn phạm vi từng UC; chưa xác nhận có UC riêng cho cập nhật/tra cứu/xóa hay không.

### C-03 — Tác nhân UC008–UC011 không thống nhất

- Danh mục Ch.3 §III ghi tác nhân “Quản lý” cho UC008, UC009, UC010, UC011.
- Đặc tả Ch.3 §IV ghi “Nhân viên bán hàng” cho cả bốn UC.
- Mô tả tác nhân Ch.3 §II giao sản phẩm/dòng sản phẩm/danh mục/sản phẩm lỗi cho **Nhân viên quản lý**.
- Ch.1 §II.1.b lại giao “Quản lý thông tin sản phẩm”, “Quản lý danh mục loại sản phẩm” cho **Nhân viên bán hàng**.
- **Trạng thái:** mâu thuẫn ba chiều; cần quyết định trước khi phân quyền.

### C-04 — Khuyến mãi vừa ngoài phạm vi vừa có use case

- Ch.1 §II.6: “hệ thống không xây dựng một phân hệ quản lý chương trình khuyến mãi hoàn chỉnh”; các chức năng tạo chiến dịch, mã giảm giá, voucher, tích điểm “không thuộc phạm vi triển khai hiện tại”.
- Ch.1 §II.8 loại trừ “Quản lý chương trình khuyến mãi, voucher và điểm thành viên **chuyên sâu**”.
- Tuy nhiên Ch.3 §III có **UC012 – Quản lý khuyến mãi**, Ch.3 §IV có đặc tả “Thêm khuyến mãi”; luồng chính UC003 bước 7–8 kiểm tra và áp dụng khuyến mãi; danh mục nêu thực thể `KhuyenMai`.
- **Trạng thái nguồn (giữ nguyên, không sửa DOCX):** mâu thuẫn giữa Ch.1 §II.6/§II.8 với Ch.3 §III–IV và Ch.4 (`KhuyenMai`; các cột `maKM`, `tyLeGiamTaiLucBan`, `tienKhuyenMaiThucTe`, `tienTruocKhuyenMai`, `tienSauKhuyenMai` trên `ChiTietHoaDon`).

**Quyết định phạm vi (đã chốt 2026-10-24, chủ dự án xác nhận):**

- **Khuyến mãi cơ bản thuộc phạm vi triển khai.** Hệ thống **có** phân hệ quản lý khuyến mãi theo UC012 (tạo chương trình, thời gian hiệu lực, tỷ lệ giảm, gắn dòng sản phẩm, trạng thái áp dụng) và áp dụng khuyến mãi **trên từng dòng sản phẩm** ở UC003 bước 7–8.
- **Câu chữ Ch.1 §II.6/§II.8 là nội dung cũ chưa được cập nhật** (chủ dự án xác nhận). DOCX **không bị sửa**; quyết định chỉ được ghi tại tài liệu dẫn xuất này.
- **Vẫn ngoài phạm vi:** voucher, mã giảm giá, tích điểm/điểm thành viên và “khuyến mãi chuyên sâu” nhiều điều kiện (Ch.1 §II.8) — không triển khai.
- **Ràng buộc kèm theo:** mỗi chương trình gắn **một** dòng sản phẩm (`RB20`); `0 ≤ tyLeGiam ≤ 100` (`RB14`); chỉ áp dụng khi `dangApDung = true` và ngày giao dịch nằm trong `[ngayBatDau, ngayKetThuc]` (`RB28`); tiền giảm làm tròn theo `C-11`; **giá, thuế và khuyến mãi tại thời điểm bán phải được snapshot** trên `ChiTietHoaDon` (`RB46`) và **không tính lại giao dịch lịch sử**.
- **Công thức áp dụng:** theo Ch.4 — `tienTruocKhuyenMai = soLuong × donGiaBanTruocThue`; `tienKhuyenMaiThucTe = làmTròn(tienTruocKhuyenMai × tyLeGiam / 100)`; `tienSauKhuyenMai = tienTruocKhuyenMai − tienKhuyenMaiThucTe`; thuế tính trên `tienSauKhuyenMai` — khớp V1 `ChiTietHoaDon` (`RB42`–`RB46`). Khác biệt với công thức Ch.1 (không có khuyến mãi) ghi tại [BR-C01](../business-rules/rules-and-exceptions.md#6-xung-đột-nguồn-và-nội-dung-bị-cắt).
- **Trạng thái:** **đã giải quyết ở mức phạm vi**; quy tắc làm tròn tiền vẫn chờ `C-11`.
- Liên kết: [BR-C08](../business-rules/rules-and-exceptions.md#6-xung-đột-nguồn-và-nội-dung-bị-cắt) · [UC012](../use-cases/UC012-promotion-management.md) · [UC003](../use-cases/UC003-retail-invoice.md)

### C-05 — Thiếu use case cho sao lưu/phục hồi

- FR015, FR016, NFR011 và Ch.1 §II.7 yêu cầu sao lưu/phục hồi dữ liệu hóa đơn, khách hàng.
- Danh mục 19 UC (UC001–UC019) **không có** use case sao lưu hoặc phục hồi.
- **Trạng thái:** khoảng trống phủ; chưa xác nhận chức năng này được đặc tả ở tài liệu khác hay chưa được lập UC.

### C-06 — Yêu cầu rộng hơn đặc tả use case

| Yêu cầu | Nội dung yêu cầu | Độ phủ use case quan sát được |
|---|---|---|
| FR003 | Thêm, cập nhật, tra cứu nhân viên; tạo/quản lý tài khoản; khóa khi nghỉ việc | UC013 chỉ đặc tả luồng thêm nhân viên và tài khoản |
| FR004 | Thêm, cập nhật, tra cứu khách hàng; tìm theo số điện thoại; xem lịch sử mua hàng | UC007 chỉ thêm khách hàng; UC005 tra cứu khách hàng/giao dịch |
| FR005 | Tạo, cập nhật, tra cứu loại sản phẩm | UC010 chỉ đặc tả thêm danh mục |
| FR006 | Thêm, cập nhật, tra cứu sản phẩm; theo dõi tồn kho | UC008/UC009 chỉ thêm sản phẩm/dòng sản phẩm; UC004 tra cứu |
| FR009 | Hủy mua sau thanh toán phải xử lý qua đổi/trả | Không có UC riêng; Ch.1 §II.4 nêu xử lý qua nghiệp vụ đổi/trả (UC006) |
| FR011 | Kết xuất báo cáo doanh thu ra Excel | Nằm trong chức năng/hậu điều kiện của UC017; không có UC riêng |

**Trạng thái:** chưa xác nhận các luồng cập nhật/tra cứu/xóa có được triển khai trong cùng UC hay cần UC bổ sung.

### C-07 — Mô hình tồn kho: serial hay số lượng?

- Ch.1 §II.3 và FR007 mô tả kiểm tra **số lượng tồn kho**, bán theo số lượng.
- Danh mục UC003/UC004/UC008 và Ch.3 §IV mô tả **từng máy theo serial**, trạng thái `SAN_SANG_BAN`; Ch.4 dùng `SanPham` cho từng máy và `DongSanPham` cho dòng sản phẩm.
- **Trạng thái:** mâu thuẫn mức hạt dữ liệu (sản phẩm/số lượng so với máy/serial); cần xác nhận trước khi thiết kế tồn kho.

### C-08 — Ai cập nhật giá bán và thuế suất?

- Ch.1 §II.1.b: Nhân viên bán hàng “cập nhật thông tin giá bán và thuế suất của sản phẩm theo thông tin đã được Quản lý phê duyệt”.
- FR006: việc cập nhật giá/thuế “phải dựa trên thông tin đã được Quản lý phê duyệt”.
- UC019: tác nhân là **Nhân viên quản lý**, người này “cập nhật giá và thuế cho giao dịch mới”.
- **Trạng thái nguồn:** mâu thuẫn vai trò thực hiện (DOCX không bị sửa).

**Quyết định implementation (đã chốt 2026-10-25, chủ dự án xác nhận):**

- **Chỉ Nhân viên quản lý** được cập nhật `donGiaBanTruocThue` và `thue` (theo UC019 + `NavItem` hiện tại).
- **Nhân viên bán hàng không có quyền sửa giá/thuế** ở bất kỳ màn hình nào (kể cả UC003 — chỉ đọc giá tại thời điểm bán).
- Không xây luồng “phê duyệt giá” riêng (nguồn không có UC); mọi thay đổi giá/thuế ghi `ngayCapNhapCuoi` để truy vết.
- Giao dịch cũ giữ nguyên snapshot giá/thuế đã chốt (NFR008, `RB46`).
- **Trạng thái:** **đã giải quyết cho implementation**.
- Liên kết: [BR-C10](../business-rules/rules-and-exceptions.md#6-xung-đột-nguồn-và-nội-dung-bị-cắt) · [UC019](../use-cases/UC019-price-tax-update.md)

### C-09 — Ngưỡng hiệu năng “dưới 3 giây” và “không quá 3 giây”

- Phần mở đầu Ch.1 §V nêu “thời gian phản hồi **dưới 3 giây**”.
- NFR004 nêu “**không quá 3 giây**”.
- **Trạng thái:** khác biệt biên (strict so với inclusive); cần chốt tiêu chí kiểm thử.

### C-10 — Không có ánh xạ tường minh FR ↔ UC

- Ch.1 §IV nêu “các yêu cầu được đánh số tương ứng với bộ Use Case của hệ thống”, nhưng không cung cấp bảng cặp FR–UC.
- Có **16 FR** và **19 UC**, không tương ứng một-một.
- **Trạng thái:** chưa xác nhận; xem [traceability.md](../traceability.md).

### C-11 — Quy tắc làm tròn tiền (đã chốt 2026-10-24)

- NFR017 yêu cầu quy tắc làm tròn tiền tệ thống nhất và không dùng giá trị đã làm tròn trung gian, nhưng nguồn không nêu công thức, đơn vị làm tròn hay thứ tự làm tròn.
- **Trạng thái nguồn:** chưa xác định trong DOCX (DOCX không bị sửa).

**Quyết định implementation (đã chốt 2026-10-24, chủ dự án xác nhận):**

- **Đơn vị làm tròn:** đồng (VND), **0 chữ số thập phân**, chế độ **`HALF_UP`**.
- **Một điểm làm tròn duy nhất** trong lớp ứng dụng (`TienUtil`), dùng `BigDecimal`; không dùng `double`/`float` cho tiền.
- **Thứ tự:** làm tròn **ở cấp chi tiết** (tiền khuyến mãi, tiền thuế, thành tiền) → **cộng các giá trị đã tròn** để ra tổng chứng từ; không làm tròn lại tổng và không tính tiếp trên giá trị đã hiển thị (khớp NFR017, XBR-16).
- **Lưu trữ:** DB lưu giá trị **đã tròn** (`NUMERIC`, 0 chữ số thập phân); giao diện và Excel **chỉ định dạng**, không làm tròn lại.
- **Định dạng hiển thị:** nhóm nghìn bằng dấu chấm + hậu tố ` VND` (ví dụ `1.250.000 VND`, `0 VND`).
- **Phạm vi áp dụng:** hóa đơn (UC003), đổi/trả (UC006), 4 báo cáo (UC014–UC017) và file Excel — cùng một quy tắc.
- **Trạng thái:** **đã giải quyết cho implementation**; các công thức nguồn giữ nguyên câu chữ.
- Liên kết: [BR-C04](../business-rules/rules-and-exceptions.md#6-xung-đột-nguồn-và-nội-dung-bị-cắt) · [XBR-16](../business-rules/rules-and-exceptions.md#2-quy-tắc-liên-tính-năng) · NFR017

### C-12 — Nhập mã định danh so với mã tự sinh bất biến

- Ch.3 §IV có các bước yêu cầu người dùng **nhập mã định danh**: UC009 “Nhập mã và tên dòng sản phẩm”; UC010 “Nhập mã danh mục”; UC012 “Nhập mã, tên và mô tả chương trình”; UC013 “Nhập mã, họ tên và thông tin liên hệ”.
- Ch.4 §I.3 quy định mã định danh do hệ thống **tự sinh**: `DongSanPham` constructor “Không nhận mã tự sinh”; `DanhMucSanPham` constructor “Mã được hệ thống cấp khi lưu”; `KhuyenMai` constructor “Mã chương trình được hệ thống cấp khi lưu mới thành công”; `NhanVien` constructor “không yêu cầu nhập `maNV`. Mã được cấp khi lưu mới thành công”.
- Ch.4 §I.3 “Quy ước chung” và các ràng buộc `RB02`, `RB03`: mã tự sinh theo dạng `TIỀN_TỐ-YYYY-NNNNNN`, được cấp khi lưu mới thành công, không trùng, không tái sử dụng và không thay đổi sau khi đã cấp.
- **Trạng thái nguồn:** xung đột giữa các bước **nhập mã** trong DOCX và quy ước mã **tự sinh** tại Ch.4 §I.3 vẫn tồn tại trong nguồn; DOCX không bị sửa.
- **Quyết định implementation hiện tại (đã duyệt 2026-10-20):** mã định danh do **ứng dụng sinh** khi lưu mới thành công, giữ đúng các định dạng đã tài liệu hóa (`TIỀN_TỐ-YYYY-NNNNNN`, ví dụ `NVQUANLY-YYYY-NNNNNN`, `NVBANHANG-YYYY-NNNNNN`); V1/PostgreSQL **không** dùng sequence, identity hay default để cấp ID. Đây là quyết định implementation, không phải chỉnh sửa DOCX.
- Liên kết sổ đăng ký: [G-10](../traceability.md#6-khoảng-trống-và-câu-hỏi-cần-xác-nhận) · [BR-C11](../business-rules/rules-and-exceptions.md#6-xung-đột-nguồn-và-nội-dung-bị-cắt).

## 7. Điều hướng sổ đăng ký: `C-xx` ↔ `G-xx` ↔ `BR-Cxx`

Bảng dưới đây chỉ nối các mã đã được lập ở tài liệu này; ô “—” nghĩa là **chưa lập hoặc chưa xác nhận ánh xạ**, không suy diễn thêm.

| `C-xx` (tài liệu này §6) | `G-xx` ([truy vết](../traceability.md)) | `BR-Cxx` ([quy tắc cắt ngang](../business-rules/rules-and-exceptions.md)) |
|---|---|---|
| C-01 | — | — |
| C-02 | — | — |
| C-03 | G-09 | BR-C09 |
| C-04 (đã giải quyết phạm vi 2026-10-24) | — | BR-C08 |
| C-05 | G-02 | — |
| C-06 | G-05 | — |
| C-07 | G-07 | — |
| C-08 (đã chốt 2026-10-25) | G-08 | BR-C10 |
| C-09 | — | — |
| C-10 | G-01 | — |
| C-11 (đã chốt 2026-10-24) | G-06 | BR-C04 |
| C-12 | G-10 | BR-C11 |
| — | G-11 | BR-C12 |
| — | G-03 | — |
| — | G-04 | — |
| — | — | BR-C01 |
| — | — | BR-C02 |
| — | — | BR-C03 |
| — | — | BR-C05 |
| — | — | BR-C06 |
| — | — | BR-C07 |

Ghi chú điều hướng:

- `BR-C01`–`BR-C03` là khác biệt công thức/ngưỡng giữa Ch.1 và Ch.4, chưa lập `C-xx` tương ứng.
- `BR-C05`–`BR-C07` là lỗi trình bày/văn bản nguồn, không phải xung đột nghiệp vụ.
- `G-03`, `G-04` là khoảng trống phủ use case, chưa lập `C-xx` tương ứng.
- `C-12`/`G-10`/`BR-C11`: các bước nhập mã trong DOCX được giữ nguyên làm bằng chứng nguồn; xung đột nguồn vẫn tồn tại, còn implementation hiện tại do ứng dụng sinh mã (không có DB generator).
- `G-11`/`BR-C12`: xung đột mô hình `ChiTietHoaDon` trong DOCX được giữ làm bằng chứng lịch sử; implementation hiện tại theo V1 amendment đã duyệt — xem [domain §8](../domain/class-model-and-entities.md#8-bản-đồ-v1-đã-phê-duyệt) và [database §6](../database/schema-and-integrity.md#6-bản-đồ-ddl-v1-đã-phê-duyệt).

## 8. Liên kết

- [README bộ tài liệu](../README.md)
- [Danh mục use case](../use-cases/catalog.md)
- [Truy vết yêu cầu – use case – thực thể](../traceability.md)
- [Quy tắc nghiệp vụ liên tính năng và ngoại lệ](../business-rules/rules-and-exceptions.md)
- [Mô hình lớp và thực thể](../domain/class-model-and-entities.md)
- [Thiết kế CSDL và ràng buộc toàn vẹn](../database/schema-and-integrity.md)
