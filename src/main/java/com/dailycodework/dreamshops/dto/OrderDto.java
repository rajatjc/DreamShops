package com.dailycodework.dreamshops.dto;

import com.dailycodework.dreamshops.model.Order;
import com.dailycodework.dreamshops.model.OrderItem;
import com.dailycodework.dreamshops.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(Long id, Instant placedAt, OrderStatus status, BigDecimal total, List<Item> items) {

    public record Item(Long productId, String name, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.subtotal());
        }
    }

    public static OrderDto from(Order order) {
        return new OrderDto(order.getId(), order.getPlacedAt(), order.getStatus(), order.getTotal(),
                order.getItems().stream().map(Item::from).toList());
    }
}
