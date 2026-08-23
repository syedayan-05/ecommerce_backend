package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.AddressRequestDTO;
import com.ayan.ecommerce.dto.AddressResponseDTO;
import com.ayan.ecommerce.entity.Address;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.exception.AddressNotFoundException;
import com.ayan.ecommerce.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    public AddressResponseDTO addAddress(AddressRequestDTO dto){

        User user = currentUserService.getCurrentUser();

        Address address = Address.builder()
                .fullName(dto.getFullName())
                .phoneNumber(dto.getPhoneNumber())
                .street(dto.getStreet())
                .apartment(dto.getApartment())
                .landmark(dto.getLandmark())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .pinCode(dto.getPinCode())
                .addressType(dto.getAddressType())
                .defaultAddress(
                        addressRepository.findByUser(user).isEmpty()
                )
                .user(user)
                .build();

        Address savedAddress = addressRepository.save(address);

        return mapToResponse(savedAddress);
    }

    private AddressResponseDTO mapToResponse(Address address){

        return AddressResponseDTO.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .street(address.getStreet())
                .apartment(address.getApartment())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .pinCode(address.getPinCode())
                .addressType(address.getAddressType())
                .defaultAddress(address.getDefaultAddress())
                .build();
    }

    public List<AddressResponseDTO> getAllAddresses(){

        User user = currentUserService.getCurrentUser();

        return addressRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();

    }
    public AddressResponseDTO getAddressById(Long id){

        User user = currentUserService.getCurrentUser();

        Address address = addressRepository
                .findByIdAndUser(id,user)
                .orElseThrow(() ->
                        new AddressNotFoundException(
                                "Address Not Found"));

        return mapToResponse(address);
    }

    public AddressResponseDTO updateAddress(
            Long id,
            AddressRequestDTO dto){

        User user = currentUserService.getCurrentUser();

        Address address = addressRepository
                .findByIdAndUser(id,user)
                .orElseThrow(() ->
                        new AddressNotFoundException(
                                "Address Not Found"));

        address.setFullName(dto.getFullName());
        address.setPhoneNumber(dto.getPhoneNumber());
        address.setStreet(dto.getStreet());
        address.setApartment(dto.getApartment());
        address.setLandmark(dto.getLandmark());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setPinCode(dto.getPinCode());
        address.setAddressType(dto.getAddressType());

        Address updated =
                addressRepository.save(address);

        return mapToResponse(updated);
    }

    public String deleteAddress(Long id){

        User user = currentUserService.getCurrentUser();

        Address address = addressRepository
                .findByIdAndUser(id,user)
                .orElseThrow(() ->
                        new AddressNotFoundException(
                                "Address Not Found"));

        addressRepository.delete(address);

        return "Address Deleted Successfully";
    }

    public String makeDefault(Long id){

        User user = currentUserService.getCurrentUser();

        List<Address> addresses =
                addressRepository.findByUser(user);

        Address selectedAddress = null;

        for(Address address : addresses){

            if(address.getId().equals(id)){
                selectedAddress = address;
            }

            address.setDefaultAddress(false);
        }

        if(selectedAddress == null){
            throw new AddressNotFoundException(
                    "Address Not Found");
        }

        selectedAddress.setDefaultAddress(true);

        addressRepository.saveAll(addresses);

        return "Default Address Updated Successfully";
    }

}