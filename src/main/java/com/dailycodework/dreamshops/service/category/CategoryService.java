package com.dailycodework.dreamshops.service.category;

import com.dailycodework.dreamshops.exceptions.AlreadyExistsException;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Category;
import com.dailycodework.dreamshops.repository.CategoryRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }

    @Override
    @Transactional
    public Category addCategory(String name) {
        requireUnusedName(name.trim());
        return categoryRepository.save(new Category(name.trim()));
    }

    @Override
    @Transactional
    public Category renameCategory(Long id, String name) {
        Category category = getCategoryById(id);
        if (!category.getName().equalsIgnoreCase(name.trim())) {
            requireUnusedName(name.trim());
        }
        category.setName(name.trim());
        return category;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Category " + category.getName() + " still has products");
        }
        categoryRepository.delete(category);
    }

    private void requireUnusedName(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new AlreadyExistsException("Category " + name + " already exists");
        }
    }
}
