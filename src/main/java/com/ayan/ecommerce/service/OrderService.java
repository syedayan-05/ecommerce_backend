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


    // =========================================================
    // CHECKOUT
    // =========================================================

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

        for (CartItem item : cart.getCartItems()) {

            Product product = item.getProduct();

            OrderItem orderItem =
                    OrderItem.builder()
                            .orderRequest(savedOrder)
                            .product(product)
                            .quantity(item.getQuantity())
                            .price(
                                    product.getPrice()
                                            .doubleValue()
                            )
                            .productName(product.getName())
                            .productImage(product.getImageUrl())
                            .build();

            orderItemRepository.save(orderItem);
        }

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

        if (order.getStatus() != OrderStatus.PENDING) {

            throw new RuntimeException(
                    "Order is not pending"
            );
        }

        if (order.getPaymentStatus() !=
                PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Payment is not pending"
            );
        }

        if (paymentMethod == null) {

            throw new RuntimeException(
                    "Payment method is required"
            );
        }

        if (order.getItems() == null ||
                order.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Order has no items"
            );
        }

        // -----------------------------------------------------
        // ATOMIC STOCK DEDUCTION
        // -----------------------------------------------------

        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found for order item"
                );
            }

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid order item quantity"
                );
            }

            int updatedRows =
                    productRepository.decrementStockIfAvailable(
                            product.getId(),
                            item.getQuantity()
                    );

            if (updatedRows == 0) {

                throw new RuntimeException(
                        product.getName() +
                                " is no longer available in requested quantity"
                );
            }
        }

        // -----------------------------------------------------
        // PAYMENT COMPLETED
        // -----------------------------------------------------

        order.setPaymentStatus(
                PaymentStatus.COMPLETED
        );

        order.setPaymentMethod(
                paymentMethod
        );

        // -----------------------------------------------------
        // ORDER CONFIRMED
        // -----------------------------------------------------

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        orderRepository.save(order);

        // -----------------------------------------------------
        // CLEAR CART
        // -----------------------------------------------------

        Cart cart =
                cartRepository.findByUser(
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
                .map(this::mapToOrderResponse)
                .toList();
    }


    // =========================================================
    // GET ORDER DETAILS - CUSTOMER
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

        if (!orderRequest.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to view this order"
            );
        }

        return mapToOrderDetails(orderRequest);
    }


    // =========================================================
    // CANCEL ORDER - CUSTOMER
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

        if (!order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to cancel this order"
            );
        }

        if (order.getStatus() ==
                OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }

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

        // -----------------------------------------------------
        // PAID ORDER
        // -----------------------------------------------------
        //
        // Refund module is not implemented yet.
        //
        // Therefore we must NOT cancel a paid order,
        // otherwise customer's payment would not be refunded.
        // -----------------------------------------------------

        if (order.getPaymentStatus() ==
                PaymentStatus.COMPLETED) {

            throw new RuntimeException(
                    "Paid order cannot be cancelled until refund is processed"
            );
        }

        // -----------------------------------------------------
        // PENDING PAYMENT
        // -----------------------------------------------------
        //
        // Stock was never deducted.
        // Therefore no stock restoration is required.
        // -----------------------------------------------------

        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);

        return "Order cancelled successfully";
    }


    // =========================================================
    // ADMIN - GET ALL ORDERS
    // =========================================================

    public List<OrderResponseDTO> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }


    // =========================================================
    // ADMIN - GET ORDER DETAILS
    // =========================================================

    public OrderDetailsResponseDTO getAdminOrderDetails(
            Long orderId
    ) {

        OrderRequest order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        return mapToOrderDetails(order);
    }


    // =========================================================
    // ADMIN - UPDATE ORDER STATUS
    // =========================================================

    @Transactional
    public OrderResponseDTO updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        if (newStatus == null) {

            throw new RuntimeException(
                    "Order status is required"
            );
        }

        OrderRequest order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        OrderStatus currentStatus =
                order.getStatus();

        if (currentStatus == null) {

            throw new RuntimeException(
                    "Current order status is missing"
            );
        }

        // -----------------------------------------------------
        // SAME STATUS
        // -----------------------------------------------------

        if (currentStatus == newStatus) {

            throw new RuntimeException(
                    "Order is already " +
                            newStatus
            );
        }

        // -----------------------------------------------------
        // CANCELLED ORDERS ARE FINAL
        // -----------------------------------------------------

        if (currentStatus ==
                OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be updated"
            );
        }

        // -----------------------------------------------------
        // DELIVERED ORDERS ARE FINAL
        // -----------------------------------------------------

        if (currentStatus ==
                OrderStatus.DELIVERED) {

            throw new RuntimeException(
                    "Delivered order cannot be updated"
            );
        }

        // -----------------------------------------------------
        // ADMIN CANNOT CHANGE PAYMENT-DRIVEN STATES
        // -----------------------------------------------------
        //
        // PENDING → CONFIRMED happens only after
        // successful Razorpay payment.
        //
        // CANCELLED is handled separately.
        // -----------------------------------------------------

        if (currentStatus ==
                OrderStatus.PENDING) {

            throw new RuntimeException(
                    "Pending order can only be confirmed after successful payment"
            );
        }

        // -----------------------------------------------------
        // VALID LIFECYCLE TRANSITIONS
        // -----------------------------------------------------

        boolean validTransition =
                (currentStatus == OrderStatus.CONFIRMED &&
                        newStatus == OrderStatus.SHIPPED)

                        ||

                        (currentStatus == OrderStatus.SHIPPED &&
                                newStatus == OrderStatus.OUT_FOR_DELIVERY)

                        ||

                        (currentStatus == OrderStatus.OUT_FOR_DELIVERY &&
                                newStatus == OrderStatus.DELIVERED);

        if (!validTransition) {

            throw new RuntimeException(
                    "Invalid order status transition: "
                            + currentStatus
                            + " → "
                            + newStatus
            );
        }

        // -----------------------------------------------------
        // UPDATE STATUS
        // -----------------------------------------------------

        order.setStatus(newStatus);

        OrderRequest updatedOrder =
                orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }


    // =========================================================
    // MAP ORDER RESPONSE
    // =========================================================

    private OrderResponseDTO mapToOrderResponse(
            OrderRequest order
    ) {

        return OrderResponseDTO.builder()
                .orderId(
                        order.getId()
                )
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
                .build();
    }


    // =========================================================
    // MAP ORDER DETAILS
    // =========================================================

    private OrderDetailsResponseDTO mapToOrderDetails(
            OrderRequest orderRequest
    ) {

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
}