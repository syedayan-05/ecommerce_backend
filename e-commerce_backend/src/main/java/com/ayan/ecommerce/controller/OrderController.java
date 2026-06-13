package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.OrderRequestDTO;
import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping
    public String placeOrder(
            @RequestBody OrderRequestDTO dto
            ){
        service.placeOrder(dto);

        return "Order placed successfully";
    }
}
