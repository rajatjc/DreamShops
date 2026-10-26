package com.dailycodework.dreamshops.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** POST /cart/items adds this many; PUT /cart/items/{productId} sets the quantity (0 removes the item). */
public record CartItemRequest(Long productId, @NotNull @Min(0) @Max(99) Integer quantity) {
}
