package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.AddToCartDTO;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.CartItem;
import com.ayan.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService service;

    @PostMapping("/add")
    public CartItem addCart(@RequestBody AddToCartDTO dto){
        return service.addToCart(dto);
    }

    @GetMapping("/{cartId}")
    public Cart getCart(@PathVariable Long cartId){
        return service.getCard(cartId);
    }

    @DeleteMapping("/remove/{cartItemId}")
    public String removeItem(@PathVariable Long cartItemId){

        return service.removeItem(cartItemId);
    }

    @PutMapping("/update/{cartItemId}")
    public CartItem updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity){

        return service.updateQuantity(
                cartItemId,
                quantity
        );
    }

    @DeleteMapping("/clear/{cartId}")
    public String clearCart(@PathVariable
                            Long cartId){
        return service.clearCart(cartId);
    }

    @GetMapping("/total/{cartId}")
    public Double calculateTotal(
            @PathVariable Long cartId
    ){
        return service.calculateTotal(cartId);
    }
}
