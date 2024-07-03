package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductDao {

    List<Product> getAll();

    Optional<Product> get(String id);

    void create(Product newProduct);
}
