package com.example.linkedin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ReactiveAuthenticationManager authenticationManager;

    public SecurityConfig(ReactiveAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http,
        ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(
                        "/login",
                        "/login**",
                        "/error",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "*linkedin/**"
                )
                .permitAll()
                .anyExchange()
                .authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizedClientService(authorizedClientService)
                .authenticationManager(authenticationManager)
            );

        return http.build();
    }
}
