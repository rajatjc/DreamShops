package com.dailycodework.dreamshops.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** An image's metadata, without its bytes. Built straight from a query so listing products never loads image data. */
public record ImageDto(Long id, String fileName, String fileType, @JsonIgnore Long productId) {

    @JsonProperty("downloadUrl")
    public String downloadUrl() {
        return "/api/v1/images/" + id;
    }
}
