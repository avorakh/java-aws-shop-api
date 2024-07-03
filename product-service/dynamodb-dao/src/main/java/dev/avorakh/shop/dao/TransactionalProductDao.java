package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.ProductInputResource;

public interface TransactionalProductDao {

    String create(String id, ProductInputResource productInput);
}
