package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.dto.ImageDto;
import com.dailycodework.dreamshops.dto.PageDto;
import com.dailycodework.dreamshops.dto.ProductDto;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Category;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.repository.CartItemRepository;
import com.dailycodework.dreamshops.repository.CategoryRepository;
import com.dailycodework.dreamshops.repository.ImageRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public ProductDto addProduct(AddProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toDto(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProduct(Long id) {
        return toDto(findProduct(id));
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long id, AddProductRequest request) {
        Product product = findProduct(id);
        apply(product, request);
        return toDto(product);
    }

    @Override
    @Transactional
    public void deleteProductById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product " + id + " not found");
        }
        cartItemRepository.deleteByProductId(id); // past orders keep their own copy of the product's details
        productRepository.delete(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<ProductDto> searchProducts(String category, String brand, String name, Pageable pageable) {
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
        Page<Product> page = productRepository.findAll(matches, pageable);
        Map<Long, List<ImageDto>> images = imagesFor(page.getContent());
        return PageDto.from(page, product -> ProductDto.from(product, images.getOrDefault(product.getId(), List.of())));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    private ProductDto toDto(Product product) {
        return ProductDto.from(product, imagesFor(List.of(product)).getOrDefault(product.getId(), List.of()));
    }

    /** One query for the image metadata of all the given products, grouped by product id. */
    private Map<Long, List<ImageDto>> imagesFor(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findSummariesByProductIds(ids).stream()
                .collect(Collectors.groupingBy(ImageDto::productId));
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
