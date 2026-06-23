package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken,Long> {
    Optional<VerificationToken> findByOtp(String otp);
    Optional<VerificationToken> findByUserId(Long userId);
}
