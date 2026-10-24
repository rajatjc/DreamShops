package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Category;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.repository.CategoryRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import com.dailycodework.dreamshops.request.AddProductRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Product addProduct(AddProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, AddProductRequest request) {
        Product product = getProductById(id);
        apply(product, request);
        return product;
    }

    @Override
    @Transactional
    public void deleteProductById(Long id) {
        productRepository.delete(getProductById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String category, String brand, String name, Pageable pageable) {
        Specification<Product> matches = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category").get("name")), category.trim().toLowerCase()));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return productRepository.findAll(matches, pageable);
    }

    private void apply(Product product, AddProductRequest request) {
        product.setName(request.name().trim());
        product.setBrand(request.brand().trim());
        product.setPrice(request.price());
        product.setInventory(request.inventory());
        product.setDescription(request.description());
        product.setCategory(findOrCreateCategory(request.category().trim()));
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(new Category(name)));
    }
}
