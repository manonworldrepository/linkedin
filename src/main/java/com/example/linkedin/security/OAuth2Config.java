package com.example.linkedin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;

import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

@Configuration
public class OAuth2Config {

    @Bean
    public ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new WebClientReactiveAuthorizationCodeTokenResponseClient();
    }

    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return new OidcReactiveOAuth2UserService();
    }

    @Bean
    public ReactiveJwtDecoderFactory<ClientRegistration> jwtDecoderFactory() {
        return clientRegistration -> {
            String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
            return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        };
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
        ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
        ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
        ReactiveJwtDecoderFactory<ClientRegistration> jwtDecoderFactory
    ) {
        NoNonceValidationOidcManager manager = new NoNonceValidationOidcManager(accessTokenResponseClient, oidcUserService);
        manager.setJwtDecoderFactory(jwtDecoderFactory);
        return manager;
    }

}

