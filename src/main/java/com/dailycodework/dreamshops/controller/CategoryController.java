package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.dto.CategoryDto;
import com.dailycodework.dreamshops.request.CategoryRequest;
import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.service.category.ICategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories() {
        var categories = categoryService.getAllCategories().stream().map(CategoryDto::from).toList();
        return ResponseEntity.ok(new ApiResponse("Found " + categories.size() + " categories", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("Category found", CategoryDto.from(categoryService.getCategoryById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addCategory(@Valid @RequestBody CategoryRequest request) {
        var category = CategoryDto.from(categoryService.addCategory(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Category added", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> renameCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(new ApiResponse("Category updated", CategoryDto.from(categoryService.renameCategory(id, request.name()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse("Category deleted", null));
    }
}
