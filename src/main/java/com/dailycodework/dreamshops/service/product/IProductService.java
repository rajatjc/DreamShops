package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.model.Product;

public interface IProductService {
    Product addProducts(Product product);
    List<Product> getAllProducts();
    Product getProductById(Long id);
}
