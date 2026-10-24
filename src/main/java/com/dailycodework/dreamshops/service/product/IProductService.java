package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.dto.PageDto;
import com.dailycodework.dreamshops.dto.ProductDto;
import com.dailycodework.dreamshops.request.AddProductRequest;
import org.springframework.data.domain.Pageable;

public interface IProductService {
    ProductDto addProduct(AddProductRequest request);

    ProductDto getProduct(Long id);

    ProductDto updateProduct(Long id, AddProductRequest request);

    void deleteProductById(Long id);

    /** Products matching every filter that isn't null. Category and brand match exactly, name by substring; all ignore case. */
    PageDto<ProductDto> searchProducts(String category, String brand, String name, Pageable pageable);
}
