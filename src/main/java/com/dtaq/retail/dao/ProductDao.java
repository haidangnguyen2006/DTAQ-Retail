package com.dtaq.retail.dao;

import com.dtaq.retail.entity.Product;
import java.util.List;
import java.util.Optional;

/** Data access contract for {@link Product} entities; failures surface as {@code DataAccessException}. */
public interface ProductDao {

  /**
   * Finds a product by its identifier.
   *
   * @param id the product identifier
   * @return the product, or empty when no row matches
   * @throws com.dtaq.retail.exception.DataAccessException when the query fails
   */
  Optional<Product> findById(long id);

  /**
   * Returns all products ordered by identifier.
   *
   * @return the matching products, or an empty list when none exist
   * @throws com.dtaq.retail.exception.DataAccessException when the query fails
   */
  List<Product> findAll();

  /**
   * Inserts a new product and returns its generated identifier.
   *
   * @param product the product to insert
   * @return the generated identifier
   * @throws com.dtaq.retail.exception.DataAccessException when the insert fails
   */
  long insert(Product product);
}
