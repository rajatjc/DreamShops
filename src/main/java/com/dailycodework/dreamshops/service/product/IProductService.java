package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.request.AddProductRequest;

import java.util.List;

public interface IProductService {
    Product addProducts(AddProductRequest product);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    void updateProduct(Product product,Long productId);
    List<Product> getAllProducts();
    List<Product> getProductsByCategory(String category);
    List<Product> getProductsByBrand(String brand);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product> getProductByName(String name);

    List<Product> getProductByBrandAndName(String brand, String name);
    Long countProductsByBrandAndName(String brand, String name);

}
