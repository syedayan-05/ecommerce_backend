package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.CheckoutRequestDTO;
import com.ayan.ecommerce.dto.OrderDetailsResponseDTO;
import com.ayan.ecommerce.dto.OrderItemResponseDTO;
import com.ayan.ecommerce.dto.OrderResponseDTO;
import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRequestRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private final UserService userService;

//    User user = userService.getLoggedInUser();

//    public User getLoggedInUser(){
//        return (User) SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getPrincipal();
//    }


    @Transactional
    public OrderResponseDTO checkout(CheckoutRequestDTO dto) {

        // 1. Logged-in User
        User user = userService.getLoggedInUser();

        // 2. User Cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Your cart is empty");
        }

        // 3. Address Validation
        Address address = addressRepository.findByIdAndUser(
                dto.getAddressId(),
                user
        ).orElseThrow(() ->
                new RuntimeException("Address not found"));

        // 4. Payment Method
        PaymentMethod paymentMethod =
                PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase());

        // 5. Calculate Total + Stock Check
        double totalAmount = 0.0;

        for (CartItem item : cart.getCartItems()) {

            Product product = item.getProduct();

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException(
                        product.getName() + " is out of stock"
                );
            }

            totalAmount +=
                    product.getPrice().doubleValue()
                            * item.getQuantity();
        }

        // 6. Create Order
        OrderRequest order = OrderRequest.builder()
                .user(user)
                .shippingAddress(address)
                .orderDate(LocalDateTime.now())
                .amount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .orderNumber("ORD-" + System.currentTimeMillis())

                .build();

        OrderRequest savedOrder = orderRepository.save(order);

        // 7. Convert CartItems → OrderItems
        for (CartItem item : cart.getCartItems()) {

            Product product = item.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .orderRequest(savedOrder)
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getPrice().doubleValue())
                    .productName(product.getName())
                    .productImage(product.getImageUrl())
                    .build();

            orderItemRepository.save(orderItem);

            // Reduce Stock
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // 8. Empty Cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        // 9. Response
        return OrderResponseDTO.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .amount(savedOrder.getAmount())
                .status(savedOrder.getStatus().name())
                .paymentStatus(savedOrder.getPaymentStatus().name())
                .paymentMethod(savedOrder.getPaymentMethod().name())
                .build();
    }

    public List<OrderResponseDTO> getMyOrders() {


        User user = userService.getLoggedInUser();

        List<OrderRequest> orders =
                orderRepository.findByUser(user);

        return orders.stream()
                .map(order -> OrderResponseDTO.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .amount(order.getAmount())
                        .status(order.getStatus().name())
                        .paymentStatus(
                                order.getPaymentStatus() != null
                                        ? order.getPaymentStatus().name()
                                        : null
                        )
                        .paymentMethod(
                                order.getPaymentMethod() != null
                                        ? order.getPaymentMethod().name()
                                        : null
                        )
                        .orderDate(order.getOrderDate())
                        .build())
                .toList();


    }

    public OrderDetailsResponseDTO getOrderDetails(Long orderId){
        User user = userService.getLoggedInUser();
        OrderRequest orderRequest = orderRepository.findById(orderId)
                .orElseThrow(()->
                        new RuntimeException("Order not found"));
        if (!orderRequest.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to view this order"
            );

        }
        List<OrderItemResponseDTO> items =
                orderRequest.getItems()
                        .stream()
                        .map(item -> OrderItemResponseDTO.builder()
                                .productId(item.getProduct().getId())
                                .productName(item.getProductName())
                                .productImage(item.getProductImage())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .toList();

        return OrderDetailsResponseDTO.builder()
                .orderId(orderRequest.getId())
                .orderNumber(orderRequest.getOrderNumber())
                .orderDate(orderRequest.getOrderDate())
                .amount(orderRequest.getAmount())
                .status(
                        orderRequest.getStatus() != null
                                ? orderRequest.getStatus().name()
                                : null
                )
                .paymentStatus(
                        orderRequest.getPaymentStatus() != null
                                ? orderRequest.getPaymentStatus().name()
                                : null
                )
                .paymentMethod(
                        orderRequest.getPaymentMethod() != null
                                ? orderRequest.getPaymentMethod().name()
                                : null
                )
                .addressId(
                        orderRequest.getShippingAddress() != null
                                ? orderRequest.getShippingAddress().getId()
                                : null
                )
                .items(items)
                .build();    
    }

    @Transactional
    public String cancelOrder(Long orderId) {

        // 1. Logged-in user
        User user = userService.getLoggedInUser();

        // 2. Order find karo
        OrderRequest order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // 3. Security check
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to cancel this order"
            );
        }

        // 4. Order status check
        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Order cannot be cancelled"
            );
        }

        // 5. Stock restore
        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            product.setStock(
                    product.getStock() + item.getQuantity()
            );

            productRepository.save(product);
        }

        // 6. Cancel order
        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return "Order cancelled successfully";
    }

}
