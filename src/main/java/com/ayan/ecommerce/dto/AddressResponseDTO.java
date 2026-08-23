package com.ayan.ecommerce.dto;

import com.ayan.ecommerce.entity.AddressType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDTO {

    private Long id;

    private String fullName;

    private String phoneNumber;

    private String street;

    private String apartment;

    private String landmark;

    private String city;

    private String state;

    private String country;

    private String pinCode;

    private AddressType addressType;

    private Boolean defaultAddress;

}