package com.dailycodework.dreamshops.service.category;

import com.dailycodework.dreamshops.model.Category;

import java.util.List;

public interface ICategoryService {
    List<Category> getAllCategories();

    Category getCategoryById(Long id);

    Category addCategory(String name);

    Category renameCategory(Long id, String name);

    /** Refuses to delete a category that still has products. */
    void deleteCategory(Long id);
}
