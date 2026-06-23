package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.OrderRequestDTO;
import com.ayan.ecommerce.entity.OrderItem;
import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.exception.InsufficientStockException;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.OrderItemRepository;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRequestRepository requestRepository;
    private final OrderItemRepository itemRepository;

    @Transactional
    public void placeOrder(OrderRequestDTO dto){
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id " + dto.getProductId()));
    if (product.getStock() < dto.getQuantity()){
        throw  new InsufficientStockException("Insufficient stock available ");
    }

    double totalAmount =
            product.getPrice().doubleValue() * dto.getQuantity();

    OrderRequest order = OrderRequest.builder()
            .orderDate(LocalDateTime.now())
            .amount(totalAmount)
            .build();

    OrderRequest savedOrder =
            requestRepository.save(order);

     OrderItem orderItem = OrderItem.builder()
             .orderRequest(savedOrder)
             .product(product)
             .quantity(dto.getQuantity())
             .price(product.getPrice().doubleValue())
             .build();
     itemRepository.save(orderItem);

     product.setStock(product.getStock() - dto.getQuantity());
     productRepository.save(product);
    }
    public List<OrderRequest> getAllOrders(){
        return requestRepository.findAll();
    }

    public OrderRequest getOrderById(Long id){
        return requestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Order Not Found"));
    }
}
