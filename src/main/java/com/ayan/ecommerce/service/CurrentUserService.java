package com.ayan.ecommerce.service;

import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    @Autowired
    private final UserRepository repository;

    public User getCurrentUser(){

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();
        String email = authentication.getName();

        return repository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException(
                        "User not Found"
                ));

    }
}
