package com.ayan.ecommerce.service;


import com.ayan.ecommerce.dto.LoginRequestDTO;
import com.ayan.ecommerce.dto.RegisterRequestDTO;
import com.ayan.ecommerce.entity.Role;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.entity.VerificationToken;
import com.ayan.ecommerce.repository.UserRepository;
import com.ayan.ecommerce.repository.VerificationTokenRepository;
import com.ayan.ecommerce.security.JwtService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.ayan.ecommerce.security.JwtService.SECRET;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository repository;
    private final EmailService emailService;

    public String register(RegisterRequestDTO dto){
        if (userRepository.findByEmail(dto.getEmail())
                .isPresent()){
            throw new RuntimeException("Email Already Exists");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(
                        passwordEncoder.encode(
                                dto.getPassword()
                        )
                )
                .role(Role.USER)
                .verified(false)
                .build();

        User savedUser = userRepository.save(user);

        String otp = String.valueOf(
                (int)(100000 + Math.random() * 900000)
        );

        VerificationToken token =
                VerificationToken.builder()
                        .otp(otp)
                        .verified(false)
                        .expiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(5)
                        )
                        .user(savedUser)
                        .build();
        repository.save(token);

        emailService.sendOtp(
                savedUser.getEmail(),
                otp
        );

        return "user saved....";
    }

    public String Login(LoginRequestDTO dto){
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(()->
                        new RuntimeException(
                                "Invalid Credentials"
                        ));
        if(!user.isVerified()){
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }
        if (!passwordEncoder.matches(dto.getPassword(),
                user.getPassword())){
            throw new RuntimeException("Invalid Credentials");
        }

        return jwtService.generateToken(user);
    }


    public String  verifyEmail(String otp){
        VerificationToken verificationToken =
                repository.findByOtp(otp)
                        .orElseThrow(()->
                                new RuntimeException("Invalid OTP"));

        if(verificationToken.getExpiryTime().isBefore(LocalDateTime.now())){
            throw new RuntimeException("OTP Expired");
        }

        verificationToken.setVerified(true);
        User user = verificationToken.getUser();
        user.setVerified(true);
        repository.save(verificationToken);
        userRepository.save(user);

        return "Email Verified Successfully";
    }

}
