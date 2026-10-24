package com.dailycodework.dreamshops.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Body of POST /products and PUT /products/{id}. The category is created if it doesn't exist yet. */
public record AddProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String brand,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
        @NotNull @PositiveOrZero Integer inventory,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 60) String category) {
}
