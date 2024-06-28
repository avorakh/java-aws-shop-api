package dev.avorakh.shop.svc;

import dev.avorakh.shop.function.model.ProductInputResource;
import dev.avorakh.shop.function.model.ProductOutputResource;
import java.util.List;
import java.util.Optional;

public interface ProductService {

    String create(String id, ProductInputResource productInput);

    List<ProductOutputResource> getAll();

    Optional<ProductOutputResource> get(String id);
}
