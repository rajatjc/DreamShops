package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.dto.PageDto;
import com.dailycodework.dreamshops.dto.ProductDto;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.request.AddProductRequest;
import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.service.product.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    /** GET /products?category=&brand=&name=&page=&size= — every filter is optional. */
    @GetMapping
    public ResponseEntity<ApiResponse> searchProducts(@RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String brand,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and 100");
        }
        var products = productService.searchProducts(category, brand, name, PageRequest.of(page, size, Sort.by("id")));
        return ResponseEntity.ok(new ApiResponse("Found " + products.getTotalElements() + " products",
                PageDto.from(products, ProductDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("Product found", ProductDto.from(productService.getProductById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addProduct(@Valid @RequestBody AddProductRequest request) {
        Product product = productService.addProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Product added", ProductDto.from(product)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody AddProductRequest request) {
        return ResponseEntity.ok(new ApiResponse("Product updated", ProductDto.from(productService.updateProduct(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
        return ResponseEntity.ok(new ApiResponse("Product deleted", null));
    }
}
