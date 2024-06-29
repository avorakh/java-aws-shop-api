package dev.avorakh.shop.svc;

import dev.avorakh.shop.dao.ProductDao;
import dev.avorakh.shop.dao.StockDao;
import dev.avorakh.shop.function.model.Product;
import dev.avorakh.shop.function.model.ProductInputResource;
import dev.avorakh.shop.function.model.ProductOutputResource;
import dev.avorakh.shop.function.model.Stock;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultProductService implements ProductService {

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

    @Override
    public List<ProductOutputResource> getAll() {

        var allFoundProducts = productDao.getAll();
        var foundStockMap = stockDao.getAll().stream().collect(Collectors.toMap(Stock::getProductId, Stock::getCount));

        Function<String, Integer> getProductCount = (id) -> foundStockMap.getOrDefault(id, 0);

        return allFoundProducts.stream()
                .map(product -> toOutputResource(product, getProductCount))
                .toList();
    }

    @Override
    public Optional<ProductOutputResource> get(String id) {

        Function<String, Integer> getProductCountById =
                (productId) -> stockDao.get(productId).map(Stock::getCount).orElse(0);

        return productDao.get(id).map(product -> toOutputResource(product, getProductCountById));
    }

    ProductOutputResource toOutputResource(Product product, Function<String, Integer> getProductCount) {
        var productId = product.getId();

        return ProductOutputResource.builder()
                .id(productId)
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .count(getProductCount.apply(productId))
                .build();
    }
}
