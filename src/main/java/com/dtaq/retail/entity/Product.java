package com.dtaq.retail.entity;

import java.math.BigDecimal;

/** Immutable product record representing a single retail item. */
public record Product(long id, String name, BigDecimal price, int quantity) {
}
