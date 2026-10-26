package com.dailycodework.dreamshops.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cart and checkout, as two different customers, with an admin managing the products. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShoppingApiTest {

    @Autowired
    MockMvc mvc;

    private String admin;
    private String alice;
    private String bob;
    private long keyboard;
    private long monitor;

    @BeforeEach
    void setUp() throws Exception {
        admin = token("admin@test.example", "admin-password");
        alice = customer("alice@example.com");
        bob = customer("bob@example.com");
        keyboard = product("Keyboard", "49.99", 5);
        monitor = product("Monitor", "199.00", 2);
    }

    // --- helpers ---

    private String token(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private String customer(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName": "Test", "lastName": "Customer", "email": "%s", "password": "password-123"}
                """.formatted(email)));
        return token(email, "password-123");
    }

    private long product(String name, String price, int stock) throws Exception {
        String json = as(admin, post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "%s", "brand": "Acme", "price": %s, "inventory": %d, "category": "Computers"}
                """.formatted(name, price, stock))).andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private ResultActions as(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", token));
    }

    private ResultActions add(String token, long productId, int quantity) throws Exception {
        return as(token, post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"quantity\": %d}".formatted(productId, quantity)));
    }

    private long orderId(ResultActions result) throws Exception {
        return Long.parseLong(result.andReturn().getResponse().getContentAsString().replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    // --- tests ---

    @Test
    void cartNeedsALogin() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void buildUpACart() throws Exception {
        as(alice, get("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(0));

        add(alice, keyboard, 2);
        add(alice, keyboard, 1); // adds to the existing line
        add(alice, monitor, 1)
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.itemCount").value(4))
                .andExpect(jsonPath("$.data.total").value(348.97));

        as(alice, put("/api/v1/cart/items/" + keyboard).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 1}"))
                .andExpect(jsonPath("$.data.total").value(248.99));
        as(alice, delete("/api/v1/cart/items/" + monitor))
                .andExpect(jsonPath("$.data.items.length()").value(1));
        as(alice, delete("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void cartsArePerUser() throws Exception {
        add(alice, keyboard, 2);
        as(bob, get("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void cannotAddMoreThanIsInStock() throws Exception {
        add(alice, monitor, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only 2 of Monitor in stock"));
        add(alice, monitor, 0).andExpect(status().isBadRequest());
        add(alice, 999_999, 1).andExpect(status().isNotFound());
        as(alice, put("/api/v1/cart/items/" + keyboard).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkoutTakesStockEmptiesTheCartAndKeepsThePricePaid() throws Exception {
        add(alice, keyboard, 2);
        add(alice, monitor, 1);

        long order = orderId(as(alice, post("/api/v1/orders"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PLACED"))
                .andExpect(jsonPath("$.data.total").value(298.98))
                .andExpect(jsonPath("$.data.items.length()").value(2)));

        mvc.perform(get("/api/v1/products/" + keyboard)).andExpect(jsonPath("$.data.inventory").value(3));
        mvc.perform(get("/api/v1/products/" + monitor)).andExpect(jsonPath("$.data.inventory").value(1));
        as(alice, get("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(0));

        // A later price change doesn't rewrite history.
        as(admin, put("/api/v1/products/" + keyboard).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Keyboard", "brand": "Acme", "price": 99.99, "inventory": 3, "category": "Computers"}
                """));
        as(alice, get("/api/v1/orders/" + order)).andExpect(jsonPath("$.data.items[0].unitPrice").value(49.99));
    }

    @Test
    void emptyCartCannotBeOrdered() throws Exception {
        as(alice, post("/api/v1/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your cart is empty"));
    }

    @Test
    void checkoutFailsWholeIfStockRanOut() throws Exception {
        add(alice, keyboard, 1);
        add(alice, monitor, 2);
        add(bob, monitor, 2);
        as(bob, post("/api/v1/orders")).andExpect(status().isCreated());

        as(alice, post("/api/v1/orders"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only 0 of Monitor left; update your cart and try again"));
        mvc.perform(get("/api/v1/products/" + keyboard)).andExpect(jsonPath("$.data.inventory").value(5));
        as(alice, get("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(3));
    }

    @Test
    void ordersArePrivate() throws Exception {
        add(alice, keyboard, 1);
        long order = orderId(as(alice, post("/api/v1/orders")));

        as(alice, get("/api/v1/orders")).andExpect(jsonPath("$.data.length()").value(1));
        as(bob, get("/api/v1/orders")).andExpect(jsonPath("$.data.length()").value(0));
        as(bob, get("/api/v1/orders/" + order)).andExpect(status().isNotFound());
        as(bob, post("/api/v1/orders/" + order + "/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void cancellingPutsStockBackOnce() throws Exception {
        add(alice, keyboard, 4);
        long order = orderId(as(alice, post("/api/v1/orders")));
        mvc.perform(get("/api/v1/products/" + keyboard)).andExpect(jsonPath("$.data.inventory").value(1));

        as(alice, post("/api/v1/orders/" + order + "/cancel")).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mvc.perform(get("/api/v1/products/" + keyboard)).andExpect(jsonPath("$.data.inventory").value(5));

        as(alice, post("/api/v1/orders/" + order + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order " + order + " is already cancelled"));
    }

    @Test
    void deletingAProductClearsItFromCartsButNotFromOrders() throws Exception {
        add(alice, keyboard, 1);
        long order = orderId(as(alice, post("/api/v1/orders")));
        add(bob, keyboard, 2);

        as(admin, delete("/api/v1/products/" + keyboard)).andExpect(status().isOk());

        as(bob, get("/api/v1/cart")).andExpect(jsonPath("$.data.itemCount").value(0));
        as(alice, get("/api/v1/orders/" + order)).andExpect(jsonPath("$.data.items[0].name").value("Keyboard"));
    }
}
