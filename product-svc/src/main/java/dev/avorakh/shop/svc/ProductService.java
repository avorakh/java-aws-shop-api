package dev.avorakh.shop.svc;

import dev.avorakh.shop.function.model.ProductInputResource;

public interface ProductService {

    String create(String id, ProductInputResource productInput);
}
