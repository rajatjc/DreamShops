package com.dailycodework.dreamshops.dto;

import com.dailycodework.dreamshops.model.Cart;
import com.dailycodework.dreamshops.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(List<Item> items, int itemCount, BigDecimal total) {

    public record Item(Long productId, String name, BigDecimal unitPrice, int quantity, int inStock, BigDecimal subtotal) {
        static Item from(CartItem item) {
            var product = item.getProduct();
            return new Item(product.getId(), product.getName(), product.getPrice(), item.getQuantity(),
                    product.getInventory(), item.subtotal());
        }
    }

    public static CartDto from(Cart cart) {
        List<Item> items = cart.getItems().stream().map(Item::from).toList();
        return new CartDto(items, items.stream().mapToInt(Item::quantity).sum(), cart.total());
    }
}
