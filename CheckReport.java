import java.time.LocalDate;
import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.impl.ThongKeSanPhamDaoImpl;

public class CheckReport {
  public static void main(String[] args) {
    var dao = new ThongKeSanPhamDaoImpl(ConnectDB.getDataSource());
    var rows = dao.thongKe(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), null);
    for (var r : rows) {
      System.out.println(r.maDongSP() + " | " + r.tenDongSP() + " | " + r.tenDanhMuc() + " | " + r.soLuongBan() + " | " + r.tienTruocThue() + " | " + r.soLuongTra() + " | " + r.doanhSoSauDieuChinh());
    }
  }
}
