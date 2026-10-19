package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ProductDao;
import com.dtaq.retail.entity.Product;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Plain-JDBC implementation of {@link ProductDao}. */
public final class ProductDaoImpl implements ProductDao {

  private static final String SELECT_BY_ID = "SELECT id, name, price, quantity FROM products WHERE id = ?";
  private static final String SELECT_ALL = "SELECT id, name, price, quantity FROM products ORDER BY id";
  private static final String INSERT = "INSERT INTO products(name, price, quantity) VALUES (?,?,?)";

  private final DataSource dataSource;

  public ProductDaoImpl(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<Product> findById(long id) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setLong(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new DataAccessException("Failed to find product by id " + id, e);
    }
  }

  @Override
  public List<Product> findAll() {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
         ResultSet rs = statement.executeQuery()) {
      List<Product> products = new ArrayList<>();
      while (rs.next()) {
        products.add(map(rs));
      }
      return products;
    } catch (SQLException e) {
      throw new DataAccessException("Failed to load all products", e);
    }
  }

  @Override
  public long insert(Product product) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, product.name());
      statement.setBigDecimal(2, product.price());
      statement.setInt(3, product.quantity());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getLong(1);
        }
        throw new DataAccessException("Insert did not return a generated key for product " + product.name());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Failed to insert product " + product.name(), e);
    }
  }

  private Product map(ResultSet rs) throws SQLException {
    return new Product(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getBigDecimal("price"),
        rs.getInt("quantity"));
  }
}
