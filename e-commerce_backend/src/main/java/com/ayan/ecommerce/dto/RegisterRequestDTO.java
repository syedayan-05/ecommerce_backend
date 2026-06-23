package com.ayan.ecommerce.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String password;
}
