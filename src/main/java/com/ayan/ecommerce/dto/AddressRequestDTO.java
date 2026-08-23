package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDTO {
    @NotBlank
    private String fullName;
    @NotBlank
    private String street;
    private String apartment;
    private String landmark;
    @NotBlank
    public String city;
    @NotBlank
    private String state;
    @NotBlank
    private String country;
    @Pattern(regexp="^[0-9]{6}$")
    private String pinCode;
    @Pattern(regexp="^[0-9]{10}$")
    private String phoneNumber;
    //enum
    private AddressType addressType;
}
