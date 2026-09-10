package com.ayan.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is Required")
    @Size(min = 3,max = 50)
    private String name;

    @Email(message = "Invalid Email")
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    @Size(min = 8)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean verified;

    @Column(unique = true,length = 10)
    @Pattern(regexp="^[0-9]{10}$",message = "Please enter mobile number")
    private String phoneNumber;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(
            mappedBy = "user",
            fetch = FetchType.LAZY
    )
    private VerificationToken verificationToken;

    @PrePersist
    public void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<OrderRequest> orders;

    @OneToOne(mappedBy = "user",
              cascade = CascadeType.ALL,
              orphanRemoval = true
    )
    @JsonIgnore
    private Cart cart;

}