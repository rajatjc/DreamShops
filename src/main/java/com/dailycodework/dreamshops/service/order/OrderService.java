package com.dailycodework.dreamshops.service.order;

import com.dailycodework.dreamshops.dto.OrderDto;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.*;
import com.dailycodework.dreamshops.repository.CartRepository;
import com.dailycodework.dreamshops.repository.OrderRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    /**
     * Turn the user's cart into an order: check every line is still in stock, take the stock, copy the
     * current prices into the order and empty the cart. All or nothing.
     */
    @Transactional
    public OrderDto placeOrder(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("Your cart is empty"));

        // Check every line before touching any stock.
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (item.getQuantity() > product.getInventory()) {
                throw new IllegalStateException("Only " + product.getInventory() + " of " + product.getName()
                        + " left; update your cart and try again");
            }
        }

        Order order = new Order(cart.getUser(), Instant.now(clock));
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            product.setInventory(product.getInventory() - item.getQuantity());
            order.addItem(product, item.getQuantity());
        }
        cart.getItems().clear();
        return OrderDto.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByPlacedAtDescIdDesc(userId).stream().map(OrderDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long userId, Long orderId) {
        return OrderDto.from(findOrder(userId, orderId));
    }

    /** Cancel a placed order and put its stock back (for products that still exist). */
    @Transactional
    public OrderDto cancelOrder(Long userId, Long orderId) {
        Order order = findOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order " + orderId + " is already " + order.getStatus().name().toLowerCase());
        }
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId())
                    .ifPresent(product -> product.setInventory(product.getInventory() + item.getQuantity()));
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderDto.from(order);
    }

    /** Another user's order is reported as missing rather than forbidden, so ids can't be probed. */
    private Order findOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
    }
}
