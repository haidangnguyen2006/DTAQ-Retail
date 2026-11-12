from openpyxl import Workbook
from openpyxl.styles import Font
from pathlib import Path

out = Path(r'D:\DTAQ-Retail\docs\reference\templates\mau_schema_import_dtaq.xlsx')
out.parent.mkdir(parents=True, exist_ok=True)

wb = Workbook()

ws1 = wb.active
ws1.title = 'SanPham'
headers1 = [
    'Số serial',
    'Mã nhà cung cấp',
    'Mã dòng sản phẩm',
    'Giá nhập',
    'Ngày nhập kho ban đầu',
    'Trạng thái',
]
ws1.append(headers1)
ws1.append(['SP-IMPORT-001', 'NCC-001', 'DSP-2026-0001', 12000000, '2026-11-12', 'SAN_SANG_BAN'])
ws1.append(['SP-IMPORT-002', 'NCC-001', 'DSP-2026-0001', 13500000, '2026-11-12', 'SAN_SANG_BAN'])
for cell in ws1[1]:
    cell.font = Font(bold=True)

ws2 = wb.create_sheet('DongSanPham')
headers2 = [
    'Tên dòng sản phẩm',
    'Mã danh mục',
    'Đơn vị tính',
    'Giá bán trước thuế',
    'Thuế suất (%)',
    'Đang kinh doanh',
]
ws2.append(headers2)
ws2.append(['Laptop ABC 14', 'DM-001', 'Chiếc', 24000000, 10, True])
ws2.append(['Máy in laser', 'DM-002', 'Cái', 4500000, 8, True])
for cell in ws2[1]:
    cell.font = Font(bold=True)

ws3 = wb.create_sheet('HuongDan')
ws3.append(['Sheet', 'Cột', 'Ghi chú'])
ws3.append(['SanPham', 'Số serial', 'Duy nhất trong hệ thống'])
ws3.append(['SanPham', 'Mã nhà cung cấp', 'Phải tồn tại trong NCC'])
ws3.append(['SanPham', 'Mã dòng sản phẩm', 'Phải tồn tại trong dòng SP'])
ws3.append(['SanPham', 'Giá nhập', 'Số, đơn vị VND'])
ws3.append(['SanPham', 'Ngày nhập kho ban đầu', 'Định dạng yyyy-mm-dd'])
ws3.append(['SanPham', 'Trạng thái', 'SAN_SANG_BAN'])
ws3.append(['DongSanPham', 'Tên dòng sản phẩm', 'Tên dòng mới'])
ws3.append(['DongSanPham', 'Mã danh mục', 'Phải tồn tại'])
ws3.append(['DongSanPham', 'Đơn vị tính', 'Chiếc hoặc Cái'])
ws3.append(['DongSanPham', 'Giá bán trước thuế', 'Số, đơn vị VND'])
ws3.append(['DongSanPham', 'Thuế suất (%)', 'Ví dụ 10'])
ws3.append(['DongSanPham', 'Đang kinh doanh', 'True/False'])
for cell in ws3[1]:
    cell.font = Font(bold=True)

wb.save(out)
print(f'Generated: {out}')
