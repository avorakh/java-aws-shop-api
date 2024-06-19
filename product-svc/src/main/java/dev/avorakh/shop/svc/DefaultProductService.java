package dev.avorakh.shop.svc;

import dev.avorakh.shop.dao.ProductDao;
import dev.avorakh.shop.dao.StockDao;
import dev.avorakh.shop.function.model.Product;
import dev.avorakh.shop.function.model.ProductInputResource;
import dev.avorakh.shop.function.model.Stock;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultProductService implements ProductService{

    ProductDao productDao;
    StockDao stockDao;

    @Override
    public String create(String id, ProductInputResource productInput) {

        var newProduct = Product.builder()
                .id(id)
                .title(productInput.getTitle())
                .description(productInput.getDescription())
                .price(productInput.getPrice())
                .build();

        var newStock = new Stock(id, productInput.getCount());
        productDao.create(newProduct);
        stockDao.create(newStock);
        return id;
    }
}
