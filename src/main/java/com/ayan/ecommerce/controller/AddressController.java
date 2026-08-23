package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.AddressRequestDTO;
import com.ayan.ecommerce.dto.AddressResponseDTO;
import com.ayan.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public AddressResponseDTO addAddress(
            @Valid @RequestBody AddressRequestDTO dto){

        return addressService.addAddress(dto);
    }

    @GetMapping
    public List<AddressResponseDTO> getAllAddresses(){

        return addressService.getAllAddresses();
    }

    @GetMapping("/{id}")
    public AddressResponseDTO getAddressById(
            @PathVariable Long id){

        return addressService.getAddressById(id);
    }

    @PutMapping("/{id}")
    public AddressResponseDTO updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequestDTO dto){

        return addressService.updateAddress(id,dto);
    }

    @DeleteMapping("/{id}")
    public String deleteAddress(
            @PathVariable Long id){

        return addressService.deleteAddress(id);
    }

    @PutMapping("/{id}/default")
    public String makeDefault(
            @PathVariable Long id){

        return addressService.makeDefault(id);
    }

}