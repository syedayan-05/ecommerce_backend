package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.CheckoutRequestDTO;
import com.ayan.ecommerce.dto.OrderDetailsResponseDTO;
import com.ayan.ecommerce.dto.OrderResponseDTO;
import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping("/checkout")
    public OrderResponseDTO checkout(
            @RequestBody CheckoutRequestDTO dto
    ){
        return service.checkout(dto);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {

        return ResponseEntity.ok(
                service.getMyOrders()
        );
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponseDTO> getOrderDetails(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                service.getOrderDetails(orderId)
        );
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                service.cancelOrder(orderId)
        );
    }
}