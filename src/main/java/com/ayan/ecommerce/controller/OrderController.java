package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.OrderRequestDTO;
import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/all")
    public List<OrderRequest> getAllOrder(){
        return service.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderRequest getOrderById(@PathVariable Long id){
        return service.getOrderById(id);
    }
}
