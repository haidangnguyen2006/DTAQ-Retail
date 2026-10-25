package com.dtaq.retail.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.PageHeader;
import javax.swing.JPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Kiểm tra trang tạm PlaceholderPage")
class PlaceholderPageTest {

  @Test
  @DisplayName("Trả về đúng id, title, section, view và mặc định cuộn được")
  void testBasicAccessors() {
    PlaceholderPage page =
        new PlaceholderPage(PageId.PROMOTIONS, "Quản lý khuyến mãi", "Quản lý", "zap");

    assertEquals(PageId.PROMOTIONS, page.id());
    assertEquals("Quản lý khuyến mãi", page.title());
    assertEquals("Quản lý", page.section());
    assertNotNull(page.view(), "view() không được null");
    assertTrue(page.scrollable(), "PlaceholderPage phải dùng thanh cuộn mặc định của shell");
  }

  @ParameterizedTest
  @EnumSource(PageId.class)
  @DisplayName("Mọi PageId đều dựng được trang tạm có view hợp lệ")
  void testAllPageIdsBuildAView(PageId id) {
    PlaceholderPage page =
        new PlaceholderPage(id, "Trang " + id.name(), "Quản lý", "layout-dashboard");

    JPanel view = assertInstanceOf(JPanel.class, page.view(), "view() phải là JPanel với " + id);
    assertEquals(2, view.getComponentCount(), "view phải gồm PageHeader và Card với " + id);
    assertInstanceOf(PageHeader.class, view.getComponent(0), "phần tử đầu phải là PageHeader với " + id);
    Card card = assertInstanceOf(Card.class, view.getComponent(1), "phần tử thứ hai phải là Card với " + id);
    assertTrue(card.getComponentCount() >= 1, "Card phải có nội dung (EmptyState) với " + id);
  }

  @Test
  @DisplayName("Ném NullPointerException khi id null")
  void testNullId() {
    assertThrows(
        NullPointerException.class,
        () -> new PlaceholderPage(null, "Trang", "Quản lý", "zap"));
  }

  @Test
  @DisplayName("Ném NullPointerException khi title null")
  void testNullTitle() {
    assertThrows(
        NullPointerException.class,
        () -> new PlaceholderPage(PageId.PROMOTIONS, null, "Quản lý", "zap"));
  }

  @Test
  @DisplayName("Ném NullPointerException khi section null")
  void testNullSection() {
    assertThrows(
        NullPointerException.class,
        () -> new PlaceholderPage(PageId.PROMOTIONS, "Trang", null, "zap"));
  }

  @Test
  @DisplayName("Ném NullPointerException khi iconName null")
  void testNullIconName() {
    assertThrows(
        NullPointerException.class,
        () -> new PlaceholderPage(PageId.PROMOTIONS, "Trang", "Quản lý", null));
  }
}
