package com.dtaq.retail.ui;

import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/** Panel thương hiệu tối màu dùng chung cho màn hình Đăng nhập và Quên mật khẩu. */
final class AuthBrandPanel extends JPanel {

  AuthBrandPanel() {
    super(new MigLayout("fill, insets 0", "[center]", "[center]"));
    setBackground(DtaqColors.BRAND_DARK);

    JPanel brandBlock = new JPanel(new MigLayout("insets 0", "[center]", "[]14[]2[]10[]"));
    brandBlock.setOpaque(false);

    brandBlock.add(
        Tile.icon("zap", 26, DtaqColors.PRIMARY, DtaqColors.SURFACE, 48, DtaqSpacing.RADIUS_L),
        "center, wrap");

    JLabel brand = new JLabel("DTAQ");
    brand.setFont(DtaqFonts.BRAND);
    brand.setForeground(DtaqColors.SURFACE);
    brandBlock.add(brand, "center, wrap");

    JLabel tagline = new JLabel("ĐIỆN MÁY");
    tagline.setFont(DtaqFonts.OVERLINE);
    tagline.setForeground(DtaqColors.BRAND_ON_DARK_MUTED);
    brandBlock.add(tagline, "center, wrap");

    JLabel description = new JLabel("Quản lý siêu thị điện máy");
    description.setFont(DtaqFonts.BODY_13);
    description.setForeground(DtaqColors.BRAND_ON_DARK_MUTED);
    brandBlock.add(description, "center");

    add(brandBlock, "center");
  }
}
