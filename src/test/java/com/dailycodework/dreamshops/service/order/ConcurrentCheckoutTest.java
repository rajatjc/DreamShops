package com.dailycodework.dreamshops.service.order;

import com.dailycodework.dreamshops.model.Role;
import com.dailycodework.dreamshops.repository.ProductRepository;
import com.dailycodework.dreamshops.request.AddProductRequest;
import com.dailycodework.dreamshops.service.cart.CartService;
import com.dailycodework.dreamshops.service.product.IProductService;
import com.dailycodework.dreamshops.service.user.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two customers check out the last unit at the same moment. Whichever way the race goes (one sees the
 * other's commit, or both read the old stock and the @Version check rejects the second), exactly one
 * order succeeds and stock never goes negative.
 *
 * Not @Transactional: each checkout must commit on its own, so this test gets a database of its own.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:concurrent-checkout;DB_CLOSE_DELAY=-1")
class ConcurrentCheckoutTest {

    @Autowired
    AuthService authService;
    @Autowired
    IProductService productService;
    @Autowired
    CartService cartService;
    @Autowired
    OrderService orderService;
    @Autowired
    ProductRepository productRepository;

    @Test
    void lastUnitIsSoldOnce() throws Exception {
        long product = productService.addProduct(
                new AddProductRequest("Last Lamp", "Acme", new BigDecimal("10"), 1, null, "Lighting")).id();
        List<Long> buyers = List.of(
                authService.createUser("A", "A", "a@race.example", "password-123", Set.of(Role.USER)).getId(),
                authService.createUser("B", "B", "b@race.example", "password-123", Set.of(Role.USER)).getId());
        buyers.forEach(buyer -> cartService.addItem(buyer, product, 1));

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> results = buyers.stream().map(buyer -> pool.submit(() -> {
            start.await();
            try {
                orderService.placeOrder(buyer);
                return true;
            } catch (RuntimeException e) { // out of stock, or lost the optimistic-lock race
                return false;
            }
        })).toList();
        pool.shutdown();

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            succeeded += result.get(30, TimeUnit.SECONDS) ? 1 : 0;
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(productRepository.findById(product).orElseThrow().getInventory()).isZero();
    }
}
