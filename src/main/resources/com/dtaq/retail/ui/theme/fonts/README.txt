DTAQ Numerals
=============

DtaqNumerals-Regular.otf / DtaqNumerals-SemiBold.otf là bản Inter 4.1 (lấy từ jar
flatlaf-fonts-inter-4.1, đường dẫn com/formdev/flatlaf/fonts/inter/) đã "đóng băng"
tính năng OpenType 'tnum' (chữ số đều cột).

Lý do: Java2D không bật được feature OpenType 'tnum', nên chữ số Inter mặc định có bề rộng
khác nhau (số 1 hẹp hơn số 0) làm cột tiền trong bảng không thẳng hàng.

Cách tạo (fonttools): đọc các lookup thay thế đơn của feature GSUB 'tnum' -> áp ánh xạ vào
mọi bảng cmap -> đổi name ID 1/4/6 thành "DTAQ Numerals[ SemiBold]" -> lưu.

Giấy phép: SIL Open Font License 1.1 (xem OFL-Inter.txt). Bản phái sinh đã đổi tên họ font.
