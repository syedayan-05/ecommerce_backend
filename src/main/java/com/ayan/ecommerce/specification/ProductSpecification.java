package com.ayan.ecommerce.specification;

import com.ayan.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String keyword,
            Long categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Integer minStock,
            Integer maxStock) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();
            // KEYWORD FILTER
            if (keyword != null && !keyword.trim().isEmpty()) {

                String searchKeyword =
                        "%" + keyword.trim().toLowerCase() + "%";

                Predicate namePredicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                searchKeyword
                        );

                Predicate descriptionPredicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                searchKeyword
                        );

                predicates.add(
                        criteriaBuilder.or(
                                namePredicate,
                                descriptionPredicate
                        )
                );
            }

            // CATEGORY FILTER
            if (categoryId != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("category").get("id"),
                                categoryId
                        )
                );
            }

            // MIN PRICE
            if (minPrice != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("price"),
                                minPrice
                        )
                );
            }

            // MAX PRICE
            if (maxPrice != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("price"),
                                maxPrice
                        )
                );
            }

            // MIN STOCK
            if (minStock != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("stock"),
                                minStock
                        )
                );
            }

            // MAX STOCK
            if (maxStock != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("stock"),
                                maxStock
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}