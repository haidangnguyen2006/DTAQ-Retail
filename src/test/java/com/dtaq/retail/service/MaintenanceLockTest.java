package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử khóa dùng chung {@link MaintenanceLock}. */
class MaintenanceLockTest {

  @Test
  @DisplayName("Giữ khóa thì bận, nhả khóa thì rảnh")
  void giuVaNhaKhoa() throws InterruptedException {
    assertTrue(MaintenanceLock.tryAcquire(Duration.ofMillis(200)));
    try {
      assertTrue(MaintenanceLock.isBusy());
    } finally {
      MaintenanceLock.release();
    }
    assertFalse(MaintenanceLock.isBusy());
  }

  @Test
  @DisplayName("Luồng khác không lấy được khóa khi khóa đang bị giữ")
  void luongKhacChoHetThoiGian() throws Exception {
    assertTrue(MaintenanceLock.tryAcquire(Duration.ofMillis(200)));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> ketQua =
          executor.submit(() -> MaintenanceLock.tryAcquire(Duration.ofMillis(150)));
      assertFalse(ketQua.get(5, TimeUnit.SECONDS), "không được lấy khóa khi đang bị giữ");
      assertTrue(MaintenanceLock.isBusy());
    } finally {
      MaintenanceLock.release();
      executor.shutdownNow();
    }
    assertFalse(MaintenanceLock.isBusy());
  }

  @Test
  @DisplayName("Nhả khóa khi chưa giữ khóa không gây lỗi")
  void nhaKhoaKhiChuaGiu() {
    MaintenanceLock.release();
    assertFalse(MaintenanceLock.isBusy());
  }
}
