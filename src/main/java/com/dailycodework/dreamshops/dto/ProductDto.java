package com.dailycodework.dreamshops.dto;

import com.dailycodework.dreamshops.model.Product;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String brand, BigDecimal price, int inventory,
                         String description, CategoryDto category) {

    public static ProductDto from(Product product) {
        return new ProductDto(product.getId(), product.getName(), product.getBrand(), product.getPrice(),
                product.getInventory(), product.getDescription(), CategoryDto.from(product.getCategory()));
    }
}
