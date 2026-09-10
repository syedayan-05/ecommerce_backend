package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.CheckoutRequestDTO;
import com.ayan.ecommerce.dto.OrderDetailsResponseDTO;
import com.ayan.ecommerce.dto.OrderResponseDTO;
import com.ayan.ecommerce.dto.OrderStatusUpdateDTO;
import com.ayan.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;


    // =========================================================
    // CUSTOMER - CHECKOUT
    // =========================================================

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(
            @RequestBody CheckoutRequestDTO dto
    ) {

        return ResponseEntity.ok(
                service.checkout(dto)
        );
    }


    // =========================================================
    // CUSTOMER - MY ORDERS
    // =========================================================

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {

        return ResponseEntity.ok(
                service.getMyOrders()
        );
    }


    // =========================================================
    // CUSTOMER - ORDER DETAILS
    // =========================================================

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponseDTO> getOrderDetails(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                service.getOrderDetails(orderId)
        );
    }


    // =========================================================
    // CUSTOMER - CANCEL ORDER
    // =========================================================

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                service.cancelOrder(orderId)
        );
    }


    // =========================================================
    // ADMIN - GET ALL ORDERS
    // =========================================================

    @GetMapping("/admin/all")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {

        return ResponseEntity.ok(
                service.getAllOrders()
        );
    }


    // =========================================================
    // ADMIN - GET ORDER DETAILS
    // =========================================================

    @GetMapping("/admin/{orderId}")
    public ResponseEntity<OrderDetailsResponseDTO> getAdminOrderDetails(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                service.getAdminOrderDetails(orderId)
        );
    }


    // =========================================================
    // ADMIN - UPDATE ORDER STATUS
    // =========================================================

    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateDTO dto
    ) {

        return ResponseEntity.ok(
                service.updateOrderStatus(
                        orderId,
                        dto.getStatus()
                )
        );
    }
}