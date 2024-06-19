package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.Stock;
import java.util.List;
import java.util.Optional;

public interface StockDao {

    List<Stock> getAll();

    Optional<Stock> get(String productId);

    void create(Stock newStock);
}
