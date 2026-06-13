package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.RegisterRequestDTO;
import com.ayan.ecommerce.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequestDTO dto
            ){
        return service.register(dto);
    }
}
