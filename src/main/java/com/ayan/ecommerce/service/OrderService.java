package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.CheckoutRequestDTO;
import com.ayan.ecommerce.dto.OrderDetailsResponseDTO;
import com.ayan.ecommerce.dto.OrderItemResponseDTO;
import com.ayan.ecommerce.dto.OrderResponseDTO;
import com.ayan.ecommerce.entity.Address;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.CartItem;
import com.ayan.ecommerce.entity.OrderItem;
import com.ayan.ecommerce.entity.OrderRequest;
import com.ayan.ecommerce.entity.OrderStatus;
import com.ayan.ecommerce.entity.PaymentMethod;
import com.ayan.ecommerce.entity.PaymentStatus;
import com.ayan.ecommerce.entity.Product;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.AddressRepository;
import com.ayan.ecommerce.repository.CartRepository;
import com.ayan.ecommerce.repository.OrderItemRepository;
import com.ayan.ecommerce.repository.OrderRequestRepository;
import com.ayan.ecommerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final RefundService refundService;

    // CHECKOUT

    @Transactional
    public OrderResponseDTO checkout(CheckoutRequestDTO dto) {

        if (dto == null) {
            throw new RuntimeException("Checkout request is required");
        }

        if (dto.getAddressId() == null) {
            throw new RuntimeException("Address ID is required");
        }

        User user = userService.getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found")
                );

        if (cart.getCartItems() == null ||
                cart.getCartItems().isEmpty()) {

            throw new RuntimeException("Your cart is empty");
        }

        Address address = addressRepository.findByIdAndUser(
                        dto.getAddressId(),
                        user
                )
                .orElseThrow(() ->
                        new RuntimeException("Address not found")
                );

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getCartItems()) {

            if (item == null) {
                throw new RuntimeException("Invalid cart item");
            }

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new RuntimeException("Invalid cart quantity");
            }

            Product product = item.getProduct();

            if (product == null) {
                throw new RuntimeException(
                        "Product not found in cart"
                );
            }

            if (product.getPrice() == null ||
                    product.getPrice()
                            .compareTo(BigDecimal.ZERO) <= 0) {

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

            BigDecimal itemTotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantity()
                                    )
                            );

            totalAmount = totalAmount.add(itemTotal);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Invalid order amount"
            );
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
                        "ORD-" + System.currentTimeMillis()
                )
                .build();

        OrderRequest savedOrder =
                orderRepository.save(order);

        for (CartItem item : cart.getCartItems()) {

            Product product = item.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .orderRequest(savedOrder)
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .productName(product.getName())
                    .productImage(product.getImageUrl())
                    .build();

            orderItemRepository.save(orderItem);
        }

        return mapToOrderResponse(savedOrder);
    }

    // PAYMENT SUCCESS

    @Transactional
    public void confirmPaidOrder(
            OrderRequest order,
            PaymentMethod paymentMethod
    ) {

        if (order == null) {
            throw new RuntimeException("Order is required");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not pending");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
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

            throw new RuntimeException("Order has no items");
        }

        for (OrderItem item : order.getItems()) {

            if (item == null) {
                throw new RuntimeException(
                        "Invalid order item"
                );
            }

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
                                " is no longer available " +
                                "in requested quantity"
                );
            }
        }

        order.setPaymentStatus(
                PaymentStatus.COMPLETED
        );

        order.setPaymentMethod(paymentMethod);

        order.setStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);

        Cart cart = cartRepository.findByUser(
                order.getUser()
        ).orElse(null);

        if (cart != null &&
                cart.getCartItems() != null) {

            cart.getCartItems().clear();
            cartRepository.save(cart);
        }
    }

    // GET MY ORDERS

    public List<OrderResponseDTO> getMyOrders() {

        User user = userService.getLoggedInUser();

        List<OrderRequest> orders =
                orderRepository.findByUser(user);

        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    // GET ORDER DETAILS

    public OrderDetailsResponseDTO getOrderDetails(
            Long orderId
    ) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required"
            );
        }

        User user = userService.getLoggedInUser();

        OrderRequest orderRequest =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        if (orderRequest.getUser() == null ||
                orderRequest.getUser().getId() == null ||
                !orderRequest.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to view this order"
            );
        }

        return mapToOrderDetails(orderRequest);
    }

    // CANCEL ORDER

    public String cancelOrder(Long orderId) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required"
            );
        }

        User user = userService.getLoggedInUser();

        OrderRequest order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        if (order.getUser() == null ||
                order.getUser().getId() == null ||
                !order.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to cancel this order"
            );
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }

        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() ==
                        OrderStatus.OUT_FOR_DELIVERY ||
                order.getStatus() ==
                        OrderStatus.DELIVERED) {

            throw new RuntimeException(
                    "Order cannot be cancelled at this stage"
            );
        }

        if (order.getPaymentStatus() ==
                PaymentStatus.PENDING) {

            order.setStatus(OrderStatus.CANCELLED);

            orderRepository.save(order);

            return "Order cancelled successfully";
        }

        if (order.getPaymentStatus() ==
                PaymentStatus.COMPLETED) {

            return refundService.requestFullRefund(order);
        }

        if (order.getPaymentStatus() ==
                PaymentStatus.REFUND_PENDING) {

            return "Refund is already being processed";
        }

        throw new RuntimeException(
                "Order cannot be cancelled in current payment state"
        );
    }

    // ADMIN - GET ALL ORDERS

    public List<OrderResponseDTO> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    // ADMIN - GET ORDER DETAILS

    public OrderDetailsResponseDTO getAdminOrderDetails(
            Long orderId
    ) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required"
            );
        }

        OrderRequest order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        return mapToOrderDetails(order);
    }

    // ADMIN - UPDATE ORDER STATUS

    @Transactional
    public OrderResponseDTO updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required"
            );
        }

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

        if (currentStatus == newStatus) {
            throw new RuntimeException(
                    "Order is already " + newStatus
            );
        }

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException(
                    "Cancelled order cannot be updated"
            );
        }

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new RuntimeException(
                    "Delivered order cannot be updated"
            );
        }

        if (currentStatus == OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Pending order can only be confirmed " +
                            "after successful payment"
            );
        }

        boolean validTransition =
                (currentStatus == OrderStatus.CONFIRMED &&
                        newStatus == OrderStatus.SHIPPED)

                        ||

                        (currentStatus == OrderStatus.SHIPPED &&
                                newStatus ==
                                        OrderStatus.OUT_FOR_DELIVERY)

                        ||

                        (currentStatus ==
                                OrderStatus.OUT_FOR_DELIVERY &&
                                newStatus ==
                                        OrderStatus.DELIVERED);

        if (!validTransition) {
            throw new RuntimeException(
                    "Invalid order status transition: " +
                            currentStatus +
                            " → " +
                            newStatus
            );
        }

        order.setStatus(newStatus);

        OrderRequest updatedOrder =
                orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    // MAP ORDER RESPONSE

    private OrderResponseDTO mapToOrderResponse(
            OrderRequest order
    ) {

        if (order == null) {
            throw new RuntimeException(
                    "Order is required"
            );
        }

        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .amount(order.getAmount())
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
                .orderDate(order.getOrderDate())
                .build();
    }

    // MAP ORDER DETAILS

    private OrderDetailsResponseDTO mapToOrderDetails(
            OrderRequest orderRequest
    ) {

        if (orderRequest == null) {
            throw new RuntimeException(
                    "Order is required"
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
                        .map(item -> {

                            if (item == null) {
                                throw new RuntimeException(
                                        "Invalid order item"
                                );
                            }

                            if (item.getProduct() == null) {
                                throw new RuntimeException(
                                        "Product not found for order item"
                                );
                            }

                            return OrderItemResponseDTO.builder()
                                    .productId(
                                            item.getProduct().getId()
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
                                    .build();
                        })
                        .toList();

        return OrderDetailsResponseDTO.builder()
                .orderId(orderRequest.getId())
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
                                ? orderRequest
                                  .getStatus()
                                  .name()
                                : null
                )
                .paymentStatus(
                        orderRequest.getPaymentStatus() != null
                                ? orderRequest
                                  .getPaymentStatus()
                                  .name()
                                : null
                )
                .paymentMethod(
                        orderRequest.getPaymentMethod() != null
                                ? orderRequest
                                  .getPaymentMethod()
                                  .name()
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