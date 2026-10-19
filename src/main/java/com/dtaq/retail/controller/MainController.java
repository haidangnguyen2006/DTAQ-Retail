package com.dtaq.retail.controller;

import com.dtaq.retail.dao.ProductDao;
import com.dtaq.retail.entity.Product;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.ui.MainFrame;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Coordinates loading product data and reflecting it in the main window. */
public class MainController {

  private final MainFrame view;
  private final ProductDao dao;

  public MainController(MainFrame view, ProductDao dao) {
    this.view = view;
    this.dao = dao;
  }

  public void refresh() {
    SwingWorker<List<Product>, Void> worker = new SwingWorker<>() {
      @Override
      protected List<Product> doInBackground() {
        return dao.findAll();
      }

      @Override
      protected void done() {
        String status;
        try {
          List<Product> products = get();
          int count = products == null ? 0 : products.size();
          status = "Sẵn sàng - " + count + " sản phẩm";
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          status = "Tải dữ liệu bị gián đoạn";
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          status = cause instanceof DataAccessException && cause.getMessage() != null
              ? cause.getMessage()
              : "Không thể tải dữ liệu";
        } catch (DataAccessException e) {
          status = e.getMessage() != null ? e.getMessage() : "Không thể tải dữ liệu";
        }
        String message = status;
        SwingUtilities.invokeLater(() -> view.setStatus(message));
      }
    };
    worker.execute();
  }
}
