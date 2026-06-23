package com.ayan.ecommerce.security;

import com.ayan.ecommerce.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    public static final String SECRET =
            "mysecretkeymysecretkeymysecretkeymysecretkey";
    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role" ,user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis()
                            +1000 * 60*60*24
                ))
                .signWith(SignatureAlgorithm.HS256,
                        SECRET)
                .compact();
    }

    public String extractUsername(String token){

        return Jwts.parser()
                .setSigningKey(SECRET)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validToken(
            String token,
            User user
    ){
        String username =
                extractUsername(token);

        return username.equals(
                user.getEmail()
        );
    }
}
