 package com.ayan.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            nullable = false,
            length = 100
    )
    private String name;


    @Column(
            length = 500
    )
    private String description;


    @OneToMany(
            mappedBy = "category",
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("category")
    private List<Product> products;
}

