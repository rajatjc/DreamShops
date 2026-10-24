package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    CategoryRepository categoryRepository;

    private ResultActions addProduct(String name, String brand, String category) throws Exception {
        String body = """
                {"name": "%s", "brand": "%s", "price": 499.99, "inventory": 10, "description": "demo", "category": "%s"}
                """.formatted(name, brand, category);
        return mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long idOf(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void createReadUpdateDeleteProduct() throws Exception {
        long id = idOf(addProduct("Pixel 9", "Google", "Phones")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Pixel 9"))
                .andExpect(jsonPath("$.data.category.name").value("Phones")));

        mvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(499.99));

        mvc.perform(put("/api/v1/products/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Pixel 9 Pro", "brand": "Google", "price": 999, "inventory": 3, "category": "Phones"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Pixel 9 Pro"))
                .andExpect(jsonPath("$.data.inventory").value(3));

        mvc.perform(delete("/api/v1/products/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product " + id + " not found"));
    }

    @Test
    void deletingAProductKeepsItsCategory() throws Exception {
        long id = idOf(addProduct("Kindle", "Amazon", "Readers"));
        mvc.perform(delete("/api/v1/products/" + id)).andExpect(status().isOk());
        assertThat(categoryRepository.findByNameIgnoreCase("Readers")).isPresent();
    }

    @Test
    void productsShareCategoriesCaseInsensitively() throws Exception {
        addProduct("iPhone 16", "Apple", "Phones");
        addProduct("Galaxy S24", "Samsung", "phones");
        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidProductIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"brand\": \"X\", \"price\": -1, \"inventory\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.price").exists())
                .andExpect(jsonPath("$.data.inventory").exists())
                .andExpect(jsonPath("$.data.category").exists());
    }

    @Test
    void searchFiltersByCategoryBrandAndName() throws Exception {
        addProduct("iPhone 16", "Apple", "Phones");
        addProduct("Galaxy S24", "Samsung", "Phones");
        addProduct("MacBook Air", "Apple", "Laptops");

        mvc.perform(get("/api/v1/products").param("brand", "apple"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("iPhone 16", "MacBook Air")));
        mvc.perform(get("/api/v1/products").param("category", "Phones").param("brand", "Apple"))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("iPhone 16")));
        mvc.perform(get("/api/v1/products").param("name", "galaxy"))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Galaxy S24")));
        mvc.perform(get("/api/v1/products").param("size", "2"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(get("/api/v1/products").param("size", "1000")).andExpect(status().isBadRequest());
    }

    @Test
    void categoryLifecycle() throws Exception {
        long id = idOf(mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Audio\"}"))
                .andExpect(status().isCreated()));

        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"audio\"}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/v1/categories/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Headphones\"}"))
                .andExpect(jsonPath("$.data.name").value("Headphones"));

        addProduct("WH-1000XM5", "Sony", "Headphones");
        mvc.perform(delete("/api/v1/categories/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category Headphones still has products"));

        mvc.perform(get("/api/v1/categories")).andExpect(jsonPath("$.data[0].name").value("Headphones"));
        mvc.perform(get("/api/v1/categories/999999")).andExpect(status().isNotFound());
    }
}
