package com.dailycodework.dreamshops.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Each user has one cart, created the first time they use it. Prices are read live from the products. */
@Getter
@NoArgsConstructor
@Entity
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    public Cart(User user) {
        this.user = user;
    }

    public Optional<CartItem> findItem(Long productId) {
        return items.stream().filter(item -> item.getProduct().getId().equals(productId)).findFirst();
    }

    public CartItem addItem(Product product) {
        CartItem item = new CartItem(this, product, 0);
        items.add(item);
        return item;
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
