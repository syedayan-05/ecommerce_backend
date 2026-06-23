package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.ProductRequestDTO;
import com.ayan.ecommerce.dto.ProductResponseDTO;
import com.ayan.ecommerce.entity.Category;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.CategoryRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldReturnProductById() {

        Product product = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .build();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        Product result =
                service.getProductById(1L);

        assertEquals(
                "iPhone 15",
                result.getName()
        );
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound(){
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                ()-> service.getProductById(1L)
        );
    }

    @Test
    void shouldSaveProduct(){

        Category category =
                Category.builder()
                        .id(1L)
                        .name("Mobile")
                        .build();

        ProductRequestDTO dto =
                ProductRequestDTO.builder()
                        .name("iPhone")
                        .description("Apple")
                        .price(BigDecimal.valueOf(100000))
                        .stock(10)
                        .categoryId(1L)
                        .build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(category));

        Product savedProduct =
                Product.builder()
                        .id(1L)
                        .name("iPhone")
                        .description("Apple")
                        .price(BigDecimal.valueOf(100000))
                        .stock(10)
                        .category(category)
                        .build();

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponseDTO result =
                service.saveProduct(dto);

        assertEquals("iPhone",result.getName());
    }

}
