package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.AddToCartDTO;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.CartItem;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.CartItemRepository;
import com.ayan.ecommerce.repository.CartRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.lang.Long.sum;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartItem addToCart(AddToCartDTO dto){
        Cart cart = cartRepository.findById(dto.getCartId())
                .orElseThrow(()->
                        new RuntimeException("Cart Not  Found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(()->
                        new ProductNotFoundException("Product Not Found"));

        CartItem cartItem =CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(dto.getQuantity())
                .build();

        return cartItemRepository.save(cartItem);
    }

    public  Cart getCard(Long id){
        return cartRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cart NOt Found"));
    }

    public String removeItem(Long cartItemId){
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(()->
                        new RuntimeException("Cart Not  Found"));
        cartItemRepository.delete(item);

        return "Item removed Successfully";

    }

    public CartItem updateQuantity(Long cartItemId,
                                   Integer quantity){

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart Item Not Found"
                        ));

        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    public  String clearCart(Long cartId){
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()->
                new RuntimeException("Cart Not  Found"));

        cartItemRepository.deleteAll(
                cart.getCartItems()
        );

        return  "cart cleared Successfully";
    }

    public Double calculateTotal(Long cartId){

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart Not Found"
                        ));

        return cart.getCartItems()
                .stream()
                .mapToDouble(item ->
                        item.getProduct().getPrice()
                                .doubleValue()
                                * item.getQuantity())
                .sum();
    }
}
