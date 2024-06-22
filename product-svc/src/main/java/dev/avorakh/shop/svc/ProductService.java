package dev.avorakh.shop.svc;

import dev.avorakh.shop.function.model.ProductInputResource;
import dev.avorakh.shop.function.model.ProductOutputResource;
import java.util.List;

public interface ProductService {

    String create(String id, ProductInputResource productInput);

    List<ProductOutputResource> getAll();
}
