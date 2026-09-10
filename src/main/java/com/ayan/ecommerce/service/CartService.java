package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.AddToCartDTO;
import com.ayan.ecommerce.dto.CartItemResponseDTO;
import com.ayan.ecommerce.dto.CartResponseDTO;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.CartItem;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.exception.ProductNotFoundException;
import com.ayan.ecommerce.repository.CartItemRepository;
import com.ayan.ecommerce.repository.CartRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;


    // =============================================
    // ADD TO CART
    // =============================================

    @Transactional
    public CartItemResponseDTO addToCart(AddToCartDTO dto) {

        User user = userService.getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id "
                                        + dto.getProductId()
                        )
                );


        // =============================================
        // STOCK CHECK
        // =============================================

        if (product.getStock() < dto.getQuantity()) {

            throw new RuntimeException(
                    "Insufficient stock. Available stock: "
                            + product.getStock()
            );
        }


        // =============================================
        // CHECK EXISTING ITEM
        // =============================================

        CartItem existingItem = cart.getCartItems()
                .stream()
                .filter(item ->
                        item.getProduct()
                                .getId()
                                .equals(product.getId())
                )
                .findFirst()
                .orElse(null);


        if (existingItem != null) {

            int newQuantity =
                    existingItem.getQuantity()
                            + dto.getQuantity();


            if (newQuantity > product.getStock()) {

                throw new RuntimeException(
                        "Requested quantity exceeds available stock"
                );
            }

            existingItem.setQuantity(newQuantity);

            return mapToResponse(
                    cartItemRepository.save(existingItem)
            );
        }


        // =============================================
        // CREATE NEW CART ITEM
        // =============================================

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(dto.getQuantity())
                .build();

        return mapToResponse(
                cartItemRepository.save(cartItem)
        );
    }


    // =============================================
    // GET MY CART
    // =============================================

    @Transactional
    public CartResponseDTO getMyCart() {

        User user = userService.getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );

        return mapToCartResponse(cart);
    }
    // =============================================
    // REMOVE ITEM
    // =============================================

    @Transactional
    public String removeItem(Long cartItemId) {

        User user = userService.getLoggedInUser();

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart item not found"
                        )
                );


        // =============================================
        // OWNERSHIP CHECK
        // =============================================

        if (!item.getCart()
                .getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to modify this cart item"
            );
        }


        cartItemRepository.delete(item);

        return "Item removed successfully";
    }


    // =============================================
    // UPDATE QUANTITY
    // =============================================

    @Transactional
    public CartItemResponseDTO updateQuantity(
            Long cartItemId,
            Integer quantity
    ) {

        if (quantity == null || quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than 0"
            );
        }


        User user = userService.getLoggedInUser();

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart item not found"
                        )
                );


        // =============================================
        // OWNERSHIP CHECK
        // =============================================

        if (!item.getCart()
                .getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to modify this cart item"
            );
        }


        // =============================================
        // STOCK CHECK
        // =============================================

        Product product = item.getProduct();

        if (quantity > product.getStock()) {

            throw new RuntimeException(
                    "Requested quantity exceeds available stock"
            );
        }


        item.setQuantity(quantity);

        return mapToResponse(
                cartItemRepository.save(item)
        );
    }


    // =============================================
    // CLEAR MY CART
    // =============================================

    @Transactional
    public String clearCart() {

        User user = userService.getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );

        cart.getCartItems().clear();

        cartRepository.save(cart);

        return "Cart cleared successfully";
    }


    // =============================================
    // CALCULATE TOTAL
    // =============================================

    @Transactional
    public BigDecimal calculateTotal() {

        User user = userService.getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );

        return cart.getCartItems()
                .stream()
                .map(item ->
                        item.getProduct()
                                .getPrice()
                                .multiply(
                                        BigDecimal.valueOf(
                                                item.getQuantity()
                                        )
                                )
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // =============================================
    // MAP CART ITEM
    // =============================================

    private CartItemResponseDTO mapToResponse(
            CartItem item
    ) {

        BigDecimal subtotal =
                item.getProduct()
                        .getPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        item.getQuantity()
                                )
                        );

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .price(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }


    // =============================================
    // MAP CART
    // =============================================

    private CartResponseDTO mapToCartResponse(
            Cart cart
    ) {

        List<CartItemResponseDTO> items =
                cart.getCartItems() == null
                        ? new ArrayList<>()
                        : cart.getCartItems()
                          .stream()
                          .map(this::mapToResponse)
                          .toList();

        BigDecimal total =
                items.stream()
                        .map(CartItemResponseDTO::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .items(items)
                .total(total)
                .build();
    }
}

