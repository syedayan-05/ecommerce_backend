package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.LoginRequestDTO;
import com.ayan.ecommerce.dto.RegisterRequestDTO;
import com.ayan.ecommerce.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/login")
    public String login(
            @RequestBody LoginRequestDTO dto
    ){
        return service.Login(dto);
    }

    @PostMapping("/verify")
    public String verify(
            @RequestParam String otp
    ){
        return service.verifyEmail(otp);
    }
}
