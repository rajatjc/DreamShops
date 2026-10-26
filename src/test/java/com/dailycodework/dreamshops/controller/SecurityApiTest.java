package com.dailycodework.dreamshops.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registration, login and who may call what, using real tokens. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityApiTest {

    private static final String PRODUCT = """
            {"name": "Mouse", "brand": "Logitech", "price": 25, "inventory": 9, "category": "Accessories"}
            """;

    @Autowired
    MockMvc mvc;

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName": "Grace", "lastName": "Hopper", "email": "%s", "password": "%s"}
                """.formatted(email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private String token(String email, String password) throws Exception {
        String json = login(email, password).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void registerLoginAndSeeYourself() throws Exception {
        register("Grace@Example.com", "correct-horse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("grace@example.com"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder("USER")))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        login("grace@example.com", "correct-horse")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").exists());

        mvc.perform(get("/api/v1/users/me").header("Authorization", token("grace@example.com", "correct-horse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Grace"));
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        register("ada@example.com", "password-1").andExpect(status().isCreated());
        register("ADA@example.com", "password-2").andExpect(status().isConflict());
    }

    @Test
    void weakOrMalformedRegistrationIsRejected() throws Exception {
        register("not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        register("alan@example.com", "enigma-machine");
        login("alan@example.com", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        login("nobody@example.com", "enigma-machine")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void anyoneCanBrowseTheCatalog() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    }

    @Test
    void changingTheCatalogNeedsAnAdmin() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isUnauthorized());

        register("shopper@example.com", "password-123");
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT)
                        .header("Authorization", token("shopper@example.com", "password-123")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT)
                        .header("Authorization", token("admin@test.example", "admin-password")))
                .andExpect(status().isCreated());
    }

    @Test
    void privateEndpointsNeedAValidToken() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());

        String admin = token("admin@test.example", "admin-password");
        String tampered = admin.substring(0, admin.length() - 4) + "AAAA";
        mvc.perform(get("/api/v1/users/me").header("Authorization", tampered)).andExpect(status().isUnauthorized());
    }
}
