package com.dailycodework.dreamshops.service.cart;

import com.dailycodework.dreamshops.dto.CartDto;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Cart;
import com.dailycodework.dreamshops.model.CartItem;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.repository.CartRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import com.dailycodework.dreamshops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartDto getCart(Long userId) {
        return CartDto.from(cartFor(userId));
    }

    /** Add `quantity` of a product, on top of any already in the cart. */
    @Transactional
    public CartDto addItem(Long userId, Long productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Add at least one");
        }
        Cart cart = cartFor(userId);
        Product product = findProduct(productId);
        CartItem item = cart.findItem(productId).orElseGet(() -> cart.addItem(product));
        item.setQuantity(inStock(product, item.getQuantity() + quantity));
        return CartDto.from(cart);
    }

    /** Set the quantity of a product in the cart; 0 removes it. */
    @Transactional
    public CartDto setQuantity(Long userId, Long productId, int quantity) {
        Cart cart = cartFor(userId);
        CartItem item = cart.findItem(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in your cart"));
        if (quantity == 0) {
            cart.removeItem(productId);
        } else {
            item.setQuantity(inStock(item.getProduct(), quantity));
        }
        return CartDto.from(cart);
    }

    @Transactional
    public CartDto removeItem(Long userId, Long productId) {
        return setQuantity(userId, productId, 0);
    }

    @Transactional
    public CartDto clear(Long userId) {
        Cart cart = cartFor(userId);
        cart.getItems().clear();
        return CartDto.from(cart);
    }

    /** The user's cart, created on first use. */
    Cart cartFor(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(new Cart(
                userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found")))));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
    }

    private static int inStock(Product product, int wanted) {
        if (wanted > product.getInventory()) {
            throw new IllegalStateException("Only " + product.getInventory() + " of " + product.getName() + " in stock");
        }
        return wanted;
    }
}
