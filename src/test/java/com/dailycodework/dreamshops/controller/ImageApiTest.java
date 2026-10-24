package com.dailycodework.dreamshops.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Autowired
    MockMvc mvc;

    private long productId;

    @BeforeEach
    void addProduct() throws Exception {
        String json = mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Desk Lamp", "brand": "IKEA", "price": 29.99, "inventory": 4, "category": "Lighting"}
                        """))
                .andReturn().getResponse().getContentAsString();
        productId = Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private static MockMultipartFile file(String part, String name, String type, byte[] bytes) {
        return new MockMultipartFile(part, name, type, bytes);
    }

    @Test
    void uploadListDownloadReplaceAndDelete() throws Exception {
        String json = mvc.perform(multipart("/api/v1/products/" + productId + "/images")
                        .file(file("files", "front.png", "image/png", PNG))
                        .file(file("files", "side.png", "image/png", PNG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileName").value("front.png"))
                .andReturn().getResponse().getContentAsString();
        long imageId = Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/v1/products/" + productId))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andExpect(jsonPath("$.data.images[0].downloadUrl").value("/api/v1/images/" + imageId));

        mvc.perform(get("/api/v1/images/" + imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Content-Disposition", startsWith("inline; filename=\"front.png\"")));

        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 9};
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/images/" + imageId).file(file("file", "new.jpg", "image/jpeg", jpeg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("new.jpg"));
        mvc.perform(get("/api/v1/images/" + imageId)).andExpect(content().bytes(jpeg));

        mvc.perform(delete("/api/v1/images/" + imageId)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/images/" + imageId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/products/" + productId)).andExpect(jsonPath("$.data.images.length()").value(1));
    }

    @Test
    void onlyImagesAreAccepted() throws Exception {
        mvc.perform(multipart("/api/v1/products/" + productId + "/images")
                        .file(file("files", "notes.txt", "text/plain", "hello".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPEG, PNG, WebP and GIF images are accepted"));
    }

    @Test
    void emptyAndOversizedFilesAreRejected() throws Exception {
        mvc.perform(multipart("/api/v1/products/" + productId + "/images").file(file("files", "a.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/products/" + productId + "/images")
                        .file(file("files", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Images can be at most 5 MB"));
    }

    @Test
    void uploadingToAMissingProductIs404() throws Exception {
        mvc.perform(multipart("/api/v1/products/999999/images").file(file("files", "a.png", "image/png", PNG)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAProductDeletesItsImages() throws Exception {
        String json = mvc.perform(multipart("/api/v1/products/" + productId + "/images").file(file("files", "a.png", "image/png", PNG)))
                .andReturn().getResponse().getContentAsString();
        long imageId = Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mvc.perform(delete("/api/v1/products/" + productId)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/images/" + imageId)).andExpect(status().isNotFound());
    }
}
