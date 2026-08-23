package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.OtpPurpose;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository
        extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByOtp(String otp);

    Optional<VerificationToken> findByOtpAndUserAndPurpose(
            String otp,
            User user,
            OtpPurpose purpose
    );

    Optional<VerificationToken> findByUserAndPurpose(
            User user,
            OtpPurpose purpose
    );

    Optional<VerificationToken> findByUserAndOtpAndPurposeAndVerifiedFalse(
            User user,
            String otp,
            OtpPurpose purpose
    );


    Optional<VerificationToken> findByUser(
            User user
    );


}