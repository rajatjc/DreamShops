package com.dailycodework.dreamshops.security;

import com.dailycodework.dreamshops.dto.TokenDto;
import com.dailycodework.dreamshops.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration lifetime;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.lifetime:PT1H}") Duration lifetime) {
        this.encoder = encoder;
        this.lifetime = lifetime;
    }

    /** A signed token whose subject is the user's id. */
    public TokenDto issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dreamshops")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenDto(token, "Bearer", expiresAt);
    }

    /** The id of the user a validated token was issued to. */
    public static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
