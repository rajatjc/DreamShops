package com.dailycodework.dreamshops.repository;

import com.dailycodework.dreamshops.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Take a product out of every cart, before the product itself is deleted. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem i where i.product.id = :productId")
    void deleteByProductId(Long productId);
}
