package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.*;
import com.ayan.ecommerce.service.AuthService;
import com.ayan.ecommerce.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    private final EmailService emailService;


    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequestDTO dto
    ) {

        String message = service.register(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                message,
                null,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {

        String token = service.Login(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Login successful",
                token,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(
            @Valid @RequestBody VerifyOtpDTO dto
    ) {

        String message = service.verifyEmail(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                message,
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // FORGET PASSWORD
    // =====================================================

    @PostMapping("/forget-password")
    public ResponseEntity<ApiResponse<String>> forgetPassword(
            @Valid @RequestBody ForgetPasswordDTO dto
    ) {

        service.forgetPassword(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Password reset OTP sent successfully",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // RESET PASSWORD
    // =====================================================

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordDTO dto
    ) {

        service.resetPassword(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Password reset successfully",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // RESEND EMAIL VERIFICATION OTP
    // =====================================================

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<ApiResponse<String>> resendVerificationOtp(
            @Valid @RequestBody ResendOtpDTO dto
    ) {

        service.resendVerificationsOtp(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Verification OTP sent successfully",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // RESEND PASSWORD RESET OTP
    // =====================================================

    @PostMapping("/resend-password-reset-otp")
    public ResponseEntity<ApiResponse<String>> resendPasswordResetOtp(
            @Valid @RequestBody ResendOtpDTO dto
    ) {

        service.resendPasswordResetOtp(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Password reset OTP sent successfully",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-email")
    public String testEmail(
            @RequestParam String email
    ) {

        emailService.sendTestEmail(email);

        return "Email sent successfully";
    }

}