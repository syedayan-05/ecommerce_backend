package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateDTO {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}