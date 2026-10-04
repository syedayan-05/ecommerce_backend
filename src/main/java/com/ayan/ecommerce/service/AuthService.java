package com.ayan.ecommerce.service;

import com.ayan.ecommerce.dto.*;
import com.ayan.ecommerce.entity.*;
import com.ayan.ecommerce.exception.BadRequestException;
import com.ayan.ecommerce.exception.ResourceNotFoundException;
import com.ayan.ecommerce.exception.UnauthorizedException;
import com.ayan.ecommerce.repository.CartRepository;
import com.ayan.ecommerce.repository.UserRepository;
import com.ayan.ecommerce.repository.VerificationTokenRepository;
import com.ayan.ecommerce.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository repository;
    private final EmailService emailService;
    private final CartRepository cartRepository;
    private final RateLimitService rateLimitService;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();


// =========================================================
// REGISTER
// =========================================================

    @Transactional
    public String register(RegisterRequestDTO dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException(
                    "Email Already Exists"
            );
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(
                        passwordEncoder.encode(
                                dto.getPassword()
                        )
                )
                .phoneNumber(dto.getPhoneNumber())
                .role(Role.USER)
                .verified(false)
                .build();

        User savedUser =
                userRepository.save(user);


        // =====================================================
        // CREATE CART FOR USER
        // =====================================================

        Cart cart = Cart.builder()
                .user(savedUser)
                .build();

        cartRepository.save(cart);


        // =====================================================
        // GENERATE EMAIL VERIFICATION OTP
        // =====================================================

        String otp = generateOtp();


        VerificationToken token =
                VerificationToken.builder()
                        .otp(otp)
                        .verified(false)
                        .expiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(5)
                        )
                        .purpose(
                                OtpPurpose.EMAIL_VERIFICATION
                        )
                        .user(savedUser)
                        .build();

        repository.save(token);


        // =====================================================
        // SEND VERIFICATION OTP
        // =====================================================

        emailService.sendVerificationOtp(
                savedUser.getEmail(),
                otp
        );

        return "User registered successfully";
    }


// =========================================================
// LOGIN
// =========================================================

    public String login(LoginRequestDTO dto) {

        User user =
                userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Invalid Credentials"
                                )
                        );


        // =====================================================
        // EMAIL VERIFICATION CHECK
        // =====================================================

        if (!user.isVerified()) {
            throw new BadRequestException(
                    "Please verify your email first"
            );
        }


        // =====================================================
        // PASSWORD CHECK
        // =====================================================

        if (!passwordEncoder.matches(
                dto.getPassword(),
                user.getPassword()
        )) {

            throw new UnauthorizedException(
                    "Invalid Credentials"
            );
        }


        // =====================================================
        // GENERATE LOGIN OTP
        // =====================================================

        String otp = generateOtp();


        VerificationToken token =
                repository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Verification token not found"
                                )
                        );


        token.setOtp(otp);
        token.setPurpose(OtpPurpose.LOGIN_OTP);
        token.setVerified(false);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        repository.save(token);


        // =====================================================
        // RESET LOGIN OTP ATTEMPTS
        // =====================================================

        String attemptKey =
                "otp:attempts:LOGIN_OTP:"
                        + user.getId();

        rateLimitService.reset(attemptKey);


        // =====================================================
        // SEND LOGIN OTP
        // =====================================================

        emailService.sendLoginOtp(
                user.getEmail(),
                otp
        );

        return "Login OTP sent successfully";
    }


// =========================================================
// VERIFY LOGIN OTP
// =========================================================

    @Transactional
    public String verifyLoginOtp(
            VerifyLoginOtpDTO dto
    ) {

        User user =
                userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        "Invalid OTP"
                                )
                        );


        VerificationToken token =
                repository
                        .findByUserAndPurposeAndVerifiedFalse(
                                user,
                                OtpPurpose.LOGIN_OTP
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid or expired login OTP"
                                )
                        );


        // =====================================================
        // CHECK OTP EXPIRY
        // =====================================================

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    "OTP Expired"
            );
        }


        // =====================================================
        // OTP ATTEMPT KEY
        // =====================================================

        String attemptKey =
                "otp:attempts:LOGIN_OTP:"
                        + user.getId();


        // =====================================================
        // CHECK OTP
        // =====================================================

        if (!token.getOtp().equals(dto.getOtp())) {

            long attempts =
                    rateLimitService.incrementAndGet(
                            attemptKey,
                            Duration.ofMinutes(5)
                    );


            // =================================================
            // INVALIDATE OTP AFTER 5 FAILED ATTEMPTS
            // =================================================

            if (attempts >= 5) {

                token.setVerified(true);
                repository.save(token);

                rateLimitService.reset(attemptKey);

                throw new BadRequestException(
                        "Too many invalid OTP attempts. Please login again."
                );
            }


            throw new BadRequestException(
                    "Invalid OTP"
            );
        }


        // =====================================================
        // OTP CORRECT
        // =====================================================

        token.setVerified(true);
        repository.save(token);

        rateLimitService.reset(attemptKey);


        // =====================================================
        // GENERATE JWT ONLY AFTER OTP VERIFICATION
        // =====================================================

        return jwtService.generateToken(user);
    }


// =========================================================
// VERIFY EMAIL
// =========================================================

    @Transactional
    public String verifyEmail(VerifyOtpDTO dto) {

        User user =
                userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );


        VerificationToken verificationToken =
                repository.findByOtpAndUserAndPurpose(
                                dto.getOtp(),
                                user,
                                OtpPurpose.EMAIL_VERIFICATION
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid OTP"
                                )
                        );


        // =====================================================
        // CHECK OTP ALREADY USED
        // =====================================================

        if (verificationToken.isVerified()) {
            throw new BadRequestException(
                    "OTP Already used"
            );
        }


        // =====================================================
        // CHECK OTP EXPIRY
        // =====================================================

        if (verificationToken.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    "OTP Expired"
            );
        }


        // =====================================================
        // VERIFY USER
        // =====================================================

        verificationToken.setVerified(true);
        user.setVerified(true);

        repository.save(verificationToken);
        userRepository.save(user);


        // =====================================================
        // SEND WELCOME EMAIL
        // =====================================================

        emailService.sendWelcomeEmail(
                user.getEmail(),
                user.getName()
        );

        return "Email Verified Successfully";
    }


// =========================================================
// FORGOT PASSWORD
// =========================================================

    @Transactional
    public void forgetPassword(
            ForgetPasswordDTO forgetPasswordDTO
    ) {

        User user =
                userRepository.findByEmail(
                                forgetPasswordDTO.getEmail()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User Not Found"
                                )
                        );


        if (!user.isVerified()) {
            throw new BadRequestException(
                    "Email is Not Registered"
            );
        }


        // =====================================================
        // GENERATE OTP
        // =====================================================

        String otp = generateOtp();


        // =====================================================
        // REUSE VERIFICATION TOKEN
        // =====================================================

        VerificationToken token =
                repository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Verification token not found"
                                )
                        );


        token.setOtp(otp);
        token.setPurpose(
                OtpPurpose.PASSWORD_RESET
        );
        token.setVerified(false);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        repository.save(token);


        // =====================================================
        // SEND PASSWORD RESET OTP
        // =====================================================

        emailService.sendPasswordResetOtp(
                user.getEmail(),
                otp
        );
    }


// =========================================================
// RESET PASSWORD
// =========================================================

    @Transactional
    public void resetPassword(
            ResetPasswordDTO resetPasswordDTO
    ) {

        User user =
                userRepository.findByEmail(
                                resetPasswordDTO.getEmail()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not Found"
                                )
                        );


        VerificationToken token =
                repository
                        .findByUserAndOtpAndPurposeAndVerifiedFalse(
                                user,
                                resetPasswordDTO.getOtp(),
                                OtpPurpose.PASSWORD_RESET
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid OTP"
                                )
                        );


        // =====================================================
        // CHECK OTP EXPIRY
        // =====================================================

        if (token.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    "OTP Expired"
            );
        }


        // =====================================================
        // UPDATE PASSWORD
        // =====================================================

        user.setPassword(
                passwordEncoder.encode(
                        resetPasswordDTO.getNewPassword()
                )
        );

        userRepository.save(user);


        // =====================================================
        // INVALIDATE OTP
        // =====================================================

        token.setVerified(true);

        repository.save(token);
    }


// =========================================================
// RESEND EMAIL VERIFICATION OTP
// =========================================================

    @Transactional
    public void resendVerificationsOtp(
            ResendOtpDTO dto
    ) {

        User user =
                userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );


        if (user.isVerified()) {
            throw new BadRequestException(
                    "Email is already verified"
            );
        }


        String otp = generateOtp();


        VerificationToken token =
                repository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Verification token not found"
                                )
                        );


        token.setOtp(otp);
        token.setPurpose(
                OtpPurpose.EMAIL_VERIFICATION
        );
        token.setVerified(false);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        repository.save(token);


        // =====================================================
        // SEND NEW OTP
        // =====================================================

        emailService.sendVerificationOtp(
                user.getEmail(),
                otp
        );
    }


// =========================================================
// RESEND PASSWORD RESET OTP
// =========================================================

    @Transactional
    public void resendPasswordResetOtp(
            ResendOtpDTO dto
    ) {

        User user =
                userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );


        if (!user.isVerified()) {
            throw new BadRequestException(
                    "Please verify your email first"
            );
        }


        String otp = generateOtp();


        VerificationToken token =
                repository.findByUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Verification token not found"
                                )
                        );


        token.setOtp(otp);
        token.setPurpose(
                OtpPurpose.PASSWORD_RESET
        );
        token.setVerified(false);
        token.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        repository.save(token);


        // =====================================================
        // SEND PASSWORD RESET OTP
        // =====================================================

        emailService.sendPasswordResetOtp(
                user.getEmail(),
                otp
        );
    }


// =========================================================
// SECURE OTP GENERATOR
// =========================================================

    private String generateOtp() {

        return String.valueOf(
                100000 + SECURE_RANDOM.nextInt(900000)
        );
    }
}