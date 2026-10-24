package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.request.AddProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductService {
    Product addProduct(AddProductRequest request);

    Product getProductById(Long id);

    Product updateProduct(Long id, AddProductRequest request);

    void deleteProductById(Long id);

    /** Products matching every filter that isn't null. Category and brand match exactly, name by substring; all ignore case. */
    Page<Product> searchProducts(String category, String brand, String name, Pageable pageable);
}
