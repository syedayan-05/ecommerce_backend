package com.ayan.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 3,max = 40)
    private String name;

    @Email(message = "Invalid Email")
    private String email;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter valid  mobile number"
    )
    private String phoneNumber;
}