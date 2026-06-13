package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.RegisterRequestDTO;
import com.ayan.ecommerce.entity.Role;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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
                .build();

        userRepository.save(user);

        return "user saved....";
    }

}
