package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.AddToCartDTO;
import com.ayan.ecommerce.dto.CartItemResponseDTO;
import com.ayan.ecommerce.dto.CartResponseDTO;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.CartItem;
import com.ayan.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
   private final CartService service;

   @PostMapping("/add")
    public CartItemResponseDTO addCart(
            @Valid @RequestBody AddToCartDTO dto
   ){
        return service.addToCart(dto);
   }

   @GetMapping
    public CartResponseDTO getMyCart(){
        return service.getMyCart();
   }

   @DeleteMapping("/remove/{cartItemId}")
    public String removeItem(
            @PathVariable Long cartItemId
   ){
        return service.removeItem(cartItemId);
   }

   @PutMapping("update/{cartItemId}")
    public CartItemResponseDTO updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity
   ){
       return service.updateQuantity(cartItemId, quantity);
   }

    @DeleteMapping("/clear")
    public String clearCart()
    {
        return service.clearCart();
    }
    @GetMapping("/total")
    public java.math.BigDecimal calculateTotal()
    {
        return service.calculateTotal();
    }



}
