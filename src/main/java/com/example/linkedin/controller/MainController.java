package com.example.linkedin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class MainController {

    @GetMapping
    public Mono<OidcUser> getAuthenticatedUser(@AuthenticationPrincipal OidcUser oidcUser) {
        return Mono.justOrEmpty(oidcUser);
    }
}

