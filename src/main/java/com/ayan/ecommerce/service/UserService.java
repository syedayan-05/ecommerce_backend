package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.UpdateUserDTO;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getLoggedInUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    public User getProfileUpdate(UpdateUserDTO dto){
        User user = getLoggedInUser();

        if (!user.getEmail().equals(dto.getEmail())
                &&
                userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email Already Exist");
        }

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());

        return userRepository.save(user);
    }

    public void deleteAccount(){

        User user = getLoggedInUser();

        userRepository.delete(user);
    }
}
