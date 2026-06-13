package com.ayan.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantity;
    private Double price;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderRequest orderRequest;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

}
