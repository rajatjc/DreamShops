package com.dailycodework.dreamshops.service.product;

import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Category;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.repository.CategoryRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import com.dailycodework.dreamshops.request.AddProductRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryRepository categoryRepository;
    @InjectMocks
    ProductService productService;

    private static AddProductRequest request(String category) {
        return new AddProductRequest(" Galaxy S24 ", "Samsung", new BigDecimal("799.99"), 5, "Phone", category);
    }

    @Test
    void addProductReusesAnExistingCategory() {
        Category phones = new Category("Phones");
        when(categoryRepository.findByNameIgnoreCase("phones")).thenReturn(Optional.of(phones));
        when(productRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Product product = productService.addProduct(request("phones"));

        assertThat(product.getCategory()).isSameAs(phones);
        assertThat(product.getName()).isEqualTo("Galaxy S24");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void addProductCreatesAMissingCategory() {
        when(categoryRepository.findByNameIgnoreCase("Tablets")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(productRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Product product = productService.addProduct(request("Tablets"));

        assertThat(product.getCategory().getName()).isEqualTo("Tablets");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void missingProductThrowsNotFound() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
        assertThatThrownBy(() -> productService.deleteProductById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void updateChangesEveryField() {
        Product existing = new Product("Old", "Old brand", BigDecimal.ONE, 1, "old", new Category("Old"));
        Category phones = new Category("Phones");
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Phones")).thenReturn(Optional.of(phones));

        Product updated = productService.updateProduct(7L, request("Phones"));

        assertThat(updated.getName()).isEqualTo("Galaxy S24");
        assertThat(updated.getBrand()).isEqualTo("Samsung");
        assertThat(updated.getPrice()).isEqualByComparingTo("799.99");
        assertThat(updated.getInventory()).isEqualTo(5);
        assertThat(updated.getCategory()).isSameAs(phones);
    }
}
