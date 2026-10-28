package com.dailycodework.dreamshops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The whole shopping flow against a real PostgreSQL, to catch what H2 lets through: reserved words in
 * table names, bytea image columns, case-insensitive search. Skipped when Docker isn't available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    private String token(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private static long id(String json) {
        return Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private String call(String token, AbstractMockHttpServletRequestBuilder<?> request, int expectedStatus) throws Exception {
        return mvc.perform(request.header("Authorization", token))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void shopFromCatalogToCancelledOrder() throws Exception {
        String admin = token("admin@test.example", "admin-password");
        long product = id(call(admin, post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Espresso Machine", "brand": "Breville", "price": 699.95, "inventory": 3, "category": "Kitchen"}
                """), 201));

        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};
        long image = id(call(admin, multipart("/api/v1/products/" + product + "/images")
                .file(new MockMultipartFile("files", "machine.png", "image/png", png)), 201));
        mvc.perform(get("/api/v1/images/" + image)).andExpect(content().bytes(png));
        mvc.perform(get("/api/v1/products").param("category", "kitchen").param("name", "ESPRESSO"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName": "Pat", "lastName": "Doe", "email": "pat@pg.example", "password": "password-123"}
                """)).andExpect(status().isCreated());
        String pat = token("pat@pg.example", "password-123");

        call(pat, post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"quantity\": 2}".formatted(product)), 200);
        long order = id(call(pat, post("/api/v1/orders"), 201));
        mvc.perform(get("/api/v1/products/" + product)).andExpect(jsonPath("$.data.inventory").value(1));

        call(pat, post("/api/v1/orders/" + order + "/cancel"), 200);
        mvc.perform(get("/api/v1/products/" + product)).andExpect(jsonPath("$.data.inventory").value(3));
        mvc.perform(get("/api/v1/orders").header("Authorization", pat))
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].total").value(1399.90));
    }
}
