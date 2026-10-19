package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.config.ConnectDB;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProductDaoImpl} against the configured database. */
class ProductDaoImplTest {

  @Test
  void findAll_skipsWhenDatabaseUnavailable() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "PostgreSQL not available");
    assertNotNull(new ProductDaoImpl(ConnectDB.getDataSource()).findAll());
  }
}
