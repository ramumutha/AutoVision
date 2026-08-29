package com.autovision.platform.security;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Primary
    @Profile("!test")
    JwtDecoder jwtDecoder(
            @Value("${AUTOVISION_OIDC_JWK_SET_URI:}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer
    ) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            return JwtDecoders.fromIssuerLocation(issuer);
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                        .permitAll()
                    .requestMatchers("/api/v1/public/commercial-enquiries")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/public/commercial-enquiries/verify")
                    .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/public/commercial-enquiries"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }));

        return http.build();
    }
}
