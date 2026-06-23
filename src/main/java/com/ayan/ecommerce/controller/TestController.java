package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final EmailService service;

    @GetMapping("/test-email")
    public String TestEmail(){
        service.sendOtp(
                "syedayan4128@gmail.com",
                "050505"
        );

        return "Email Sent";
    }
}
