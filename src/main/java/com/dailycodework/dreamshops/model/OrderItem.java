package com.dailycodework.dreamshops.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One line of an order. The product's name and price are copied in when the order is placed, and the
 * product id is kept without a foreign key, so the order reads the same after the product changes or
 * is deleted.
 */
@Getter
@NoArgsConstructor
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;

    OrderItem(Order order, Product product, int quantity) {
        this.order = order;
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
