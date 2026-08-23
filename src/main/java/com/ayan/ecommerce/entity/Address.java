package com.ayan.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;


    @Column(nullable = false)
    private String street;

    private String apartment;

    private String landmark;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    @Pattern(regexp="^[0-9]{6}$")
    private String pinCode;

    @Column(nullable = false)
    @Pattern(regexp="^[0-9]{10}$")
    private String phoneNumber;


    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Builder.Default
    private Boolean defaultAddress = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "shippingAddress")
    @JsonIgnore
    private List<OrderRequest> orders;
}