package com.ayan.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgetPasswordDTO {
    @NotBlank
    @Email
    private String email;

    public ForgetPasswordDTO(String email) {
        this.email = email;
    }

    public ForgetPasswordDTO(){}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
