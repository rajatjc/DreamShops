package com.dailycodework.dreamshops.dto;

import com.dailycodework.dreamshops.model.Category;

public record CategoryDto(Long id, String name) {

    public static CategoryDto from(Category category) {
        return category == null ? null : new CategoryDto(category.getId(), category.getName());
    }
}
