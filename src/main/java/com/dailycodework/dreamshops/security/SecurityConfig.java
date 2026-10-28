package com.dailycodework.dreamshops.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Stateless JWT security. Tokens are HS256-signed with app.jwt.secret and carry the user's roles.
 * Anyone can browse the catalog; changing it needs ADMIN; everything else needs a valid token.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecretKey jwtSigningKey(@Value("${app.jwt.secret:}") String secret) {
        byte[] key;
        if (secret.isBlank()) {
            log.warn("app.jwt.secret (JWT_SECRET) is not set; using a random key, so tokens stop working on restart");
            key = new byte[32];
            new SecureRandom().nextBytes(key);
        } else {
            key = secret.getBytes(StandardCharsets.UTF_8);
            if (key.length < 32) {
                throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
            }
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${api.prefix}") String api) throws Exception {
        String[] catalog = {api + "/products/**", api + "/categories/**", api + "/images/**"};
        http
                .csrf(AbstractHttpConfigurer::disable) // no cookies or sessions, so nothing for CSRF to ride on
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, api + "/auth/register", api + "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, catalog).permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(catalog).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken())));
        return http.build();
    }

    /** Map the token's "roles" claim (["USER", "ADMIN"]) to ROLE_USER / ROLE_ADMIN authorities. */
    private static JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
