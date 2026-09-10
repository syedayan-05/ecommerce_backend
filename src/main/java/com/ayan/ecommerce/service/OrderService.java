package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.CheckoutRequestDTO;
import com.ayan.ecommerce.dto.OrderDetailsResponseDTO;
import com.ayan.ecommerce.dto.OrderItemResponseDTO;
import com.ayan.ecommerce.dto.OrderResponseDTO;
import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.repository.AddressRepository;
import com.ayan.ecommerce.repository.CartRepository;
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
    private final OrderRequestRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserService userService;

    // CHECKOUT
    @Transactional
    public OrderResponseDTO checkout(CheckoutRequestDTO dto) {
        User user = userService.getLoggedInUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );
        if (cart.getCartItems() == null ||
                cart.getCartItems().isEmpty()) {

            throw new RuntimeException(
                    "Your cart is empty"
            );
        }
        Address address = addressRepository.findByIdAndUser(
                dto.getAddressId(),
                user
        ).orElseThrow(() ->
                new RuntimeException("Address not found")
        );

//        Calculate total + validate stock
        double totalAmount = 0.0;

        for (CartItem item : cart.getCartItems()) {

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid cart quantity"
                );
            }

            Product product = item.getProduct();

            if (product == null) {
                throw new RuntimeException(
                        "Product not found in cart"
                );
            }

            if (product.getPrice() == null ||
                    product.getPrice().doubleValue() <= 0) {

                throw new RuntimeException(
                        "Invalid product price"
                );
            }

            if (product.getStock() == null ||
                    product.getStock() < item.getQuantity()) {

                throw new RuntimeException(
                        product.getName() +
                                " is out of stock"
                );
            }

            totalAmount +=
                    product.getPrice().doubleValue()
                            * item.getQuantity();
        }

        // 6. Create PENDING order
        //
        // Payment is NOT completed yet.
        // Therefore:
        // - Order = PENDING
        // - Payment = PENDING
        // - PaymentMethod = null
        // - Stock is NOT reduced
        // - Cart is NOT cleared

        OrderRequest order = OrderRequest.builder()
                .user(user)
                .shippingAddress(address)
                .orderDate(LocalDateTime.now())
                .amount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(null)
                .orderNumber(
                        "ORD-" +
                                System.currentTimeMillis()
                )
                .build();

        OrderRequest savedOrder =
                orderRepository.save(order);

        // 7. Create order items
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
        }

        // IMPORTANT:
        //
        // We DO NOT reduce stock here.
        // We DO NOT clear cart here.
        //
        // These operations happen only after
        // successful Razorpay payment verification.

        return OrderResponseDTO.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .amount(savedOrder.getAmount())
                .status(savedOrder.getStatus().name())
                .paymentStatus(
                        savedOrder.getPaymentStatus().name()
                )
                .paymentMethod(null)
                .orderDate(savedOrder.getOrderDate())
                .build();
    }


    // =========================================================
    // PAYMENT SUCCESS → CONFIRM ORDER
    // =========================================================

    @Transactional
    public void confirmPaidOrder(
            OrderRequest order,
            PaymentMethod paymentMethod
    ) {

        // 1. Order must be pending
        if (order.getStatus() != OrderStatus.PENDING) {

            throw new RuntimeException(
                    "Order is not pending"
            );
        }

        // 2. Payment must be pending
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not pending"
            );
        }

        // 3. Validate payment method
        if (paymentMethod == null) {

            throw new RuntimeException(
                    "Payment method is required"
            );
        }

        // 4. Validate order items
        if (order.getItems() == null ||
                order.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Order has no items"
            );
        }

        // 5. Check stock again
        //
        // Customer may have spent time on
        // Razorpay checkout.
        //
        // During that time stock could have changed.

        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            if (product == null) {
                throw new RuntimeException(
                        "Product not found for order item"
                );
            }

            if (product.getStock() == null ||
                    product.getStock() < item.getQuantity()) {

                throw new RuntimeException(
                        product.getName() +
                                " is no longer available"
                );
            }
        }

        // 6. Reduce stock
        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            product.setStock(
                    product.getStock()
                            - item.getQuantity()
            );

            productRepository.save(product);
        }

        // 7. Payment completed
        order.setPaymentStatus(
                PaymentStatus.COMPLETED
        );

        // 8. Save actual payment method
        order.setPaymentMethod(
                paymentMethod
        );

        // 9. Confirm order
        order.setStatus(
                OrderStatus.CONFIRMED
        );

        orderRepository.save(order);

        // 10. Clear user's cart
        Cart cart = cartRepository.findByUser(
                order.getUser()
        ).orElse(null);

        if (cart != null &&
                cart.getCartItems() != null) {

            cart.getCartItems().clear();

            cartRepository.save(cart);
        }
    }


    // =========================================================
    // GET MY ORDERS
    // =========================================================

    public List<OrderResponseDTO> getMyOrders() {

        User user = userService.getLoggedInUser();

        List<OrderRequest> orders =
                orderRepository.findByUser(user);

        return orders.stream()
                .map(order ->
                        OrderResponseDTO.builder()
                                .orderId(order.getId())
                                .orderNumber(
                                        order.getOrderNumber()
                                )
                                .amount(
                                        order.getAmount()
                                )
                                .status(
                                        order.getStatus() != null
                                                ? order.getStatus().name()
                                                : null
                                )
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
                                .orderDate(
                                        order.getOrderDate()
                                )
                                .build()
                )
                .toList();
    }


    // =========================================================
    // GET ORDER DETAILS
    // =========================================================

    public OrderDetailsResponseDTO getOrderDetails(
            Long orderId
    ) {

        User user = userService.getLoggedInUser();

        OrderRequest orderRequest =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        // Security check
        if (!orderRequest.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to view this order"
            );
        }

        if (orderRequest.getItems() == null) {

            throw new RuntimeException(
                    "Order items not found"
            );
        }

        List<OrderItemResponseDTO> items =
                orderRequest.getItems()
                        .stream()
                        .map(item ->
                                OrderItemResponseDTO.builder()
                                        .productId(
                                                item.getProduct()
                                                        .getId()
                                        )
                                        .productName(
                                                item.getProductName()
                                        )
                                        .productImage(
                                                item.getProductImage()
                                        )
                                        .quantity(
                                                item.getQuantity()
                                        )
                                        .price(
                                                item.getPrice()
                                        )
                                        .build()
                        )
                        .toList();

        return OrderDetailsResponseDTO.builder()
                .orderId(
                        orderRequest.getId()
                )
                .orderNumber(
                        orderRequest.getOrderNumber()
                )
                .orderDate(
                        orderRequest.getOrderDate()
                )
                .amount(
                        orderRequest.getAmount()
                )
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
                                ? orderRequest
                                  .getShippingAddress()
                                  .getId()
                                : null
                )
                .items(items)
                .build();
    }


    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @Transactional
    public String cancelOrder(Long orderId) {

        User user = userService.getLoggedInUser();

        OrderRequest order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        // Security check
        if (!order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to cancel this order"
            );
        }

        // Already cancelled
        if (order.getStatus() ==
                OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }

        // Shipped / out for delivery / delivered
        // cannot be cancelled
        if (order.getStatus() ==
                OrderStatus.SHIPPED ||
                order.getStatus() ==
                        OrderStatus.OUT_FOR_DELIVERY ||
                order.getStatus() ==
                        OrderStatus.DELIVERED) {

            throw new RuntimeException(
                    "Order cannot be cancelled at this stage"
            );
        }

        /*
         * If payment was completed:
         *      restore stock.
         *
         * If payment is still pending:
         *      stock was never reduced.
         */

        if (order.getPaymentStatus() ==
                PaymentStatus.COMPLETED) {

            if (order.getItems() != null) {

                for (OrderItem item :
                        order.getItems()) {

                    Product product =
                            item.getProduct();

                    product.setStock(
                            product.getStock()
                                    + item.getQuantity()
                    );

                    productRepository.save(product);
                }
            }
        }

        // Cancel order
        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);

        return "Order cancelled successfully";
    }
}