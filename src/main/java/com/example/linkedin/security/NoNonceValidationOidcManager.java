package com.example.linkedin.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * This is mainly because I sit behind a proxy, and it plays around with the nonce automatically while requests are being proxied
 * But if you don't sit behind a proxy, then this implementation makes no sense.
 */
public class NoNonceValidationOidcManager implements ReactiveAuthenticationManager {

    private final ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
    private final ReactiveOAuth2UserService<OidcUserRequest, OidcUser> userService;
    private GrantedAuthoritiesMapper authoritiesMapper = (authorities) -> authorities;
    private ReactiveJwtDecoderFactory<ClientRegistration> jwtDecoderFactory;

    public NoNonceValidationOidcManager(
            ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
            ReactiveOAuth2UserService<OidcUserRequest, OidcUser> userService) {
        Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
        Assert.notNull(userService, "userService cannot be null");
        this.accessTokenResponseClient = accessTokenResponseClient;
        this.userService = userService;
    }

    public void setJwtDecoderFactory(ReactiveJwtDecoderFactory<ClientRegistration> jwtDecoderFactory) {
        Assert.notNull(jwtDecoderFactory, "jwtDecoderFactory cannot be null");
        this.jwtDecoderFactory = jwtDecoderFactory;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        Assert.notNull(authoritiesMapper, "authoritiesMapper cannot be null");
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.defer(() -> {
            OAuth2AuthorizationCodeAuthenticationToken authorizationCodeAuthentication =
                (OAuth2AuthorizationCodeAuthenticationToken) authentication;

            if (!authorizationCodeAuthentication.getAuthorizationExchange()
                .getAuthorizationRequest().getScopes().contains("openid")) {
                return Mono.empty();
            }

            OAuth2AuthorizationRequest authorizationRequest = authorizationCodeAuthentication
                .getAuthorizationExchange().getAuthorizationRequest();
            OAuth2AuthorizationResponse authorizationResponse = authorizationCodeAuthentication
                .getAuthorizationExchange().getAuthorizationResponse();

            if (authorizationResponse.statusError()) {
                return Mono.error(new OAuth2AuthenticationException(
                    authorizationResponse.getError(), authorizationResponse.getError().toString())
                );
            }

            if (!authorizationResponse.getState().equals(authorizationRequest.getState())) {
                OAuth2Error oauth2Error = new OAuth2Error("invalid_state_parameter");
                return Mono.error(new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString()));
            }

            OAuth2AuthorizationCodeGrantRequest authzRequest = new OAuth2AuthorizationCodeGrantRequest(
                authorizationCodeAuthentication.getClientRegistration(),
                authorizationCodeAuthentication.getAuthorizationExchange()
            );

            return this.accessTokenResponseClient.getTokenResponse(authzRequest)
                .flatMap(accessTokenResponse ->
                    this.customAuthenticationResult(authorizationCodeAuthentication, accessTokenResponse)
                )
                .onErrorMap(OAuth2AuthorizationException.class, e ->
                    new OAuth2AuthenticationException(e.getError(), e.getError().toString(), e))
                .onErrorMap(JwtException.class, e -> {
                    OAuth2Error invalidIdTokenError = new OAuth2Error("invalid_id_token", e.getMessage(), null);
                    return new OAuth2AuthenticationException(invalidIdTokenError, invalidIdTokenError.toString(), e);
                });
        });
    }

    private Mono<OAuth2LoginAuthenticationToken> customAuthenticationResult(
        OAuth2AuthorizationCodeAuthenticationToken authorizationCodeAuthentication,
        OAuth2AccessTokenResponse accessTokenResponse
    ) {
        OAuth2AccessToken accessToken = accessTokenResponse.getAccessToken();
        ClientRegistration clientRegistration = authorizationCodeAuthentication.getClientRegistration();
        Map<String, Object> additionalParameters = accessTokenResponse.getAdditionalParameters();

        if (!additionalParameters.containsKey("id_token")) {
            OAuth2Error invalidIdTokenError = new OAuth2Error(
                "invalid_id_token",
                "Missing (required) ID Token in Token Response for Client Registration: " + clientRegistration.getRegistrationId(), null
            );
            return Mono.error(new OAuth2AuthenticationException(invalidIdTokenError, invalidIdTokenError.toString()));
        }

        Mono<OidcIdToken> oidcIdTokenMono = createOidcToken(clientRegistration, accessTokenResponse);

        return oidcIdTokenMono
            .map(idToken -> new OidcUserRequest(clientRegistration, accessToken, idToken, additionalParameters))
            .flatMap(this.userService::loadUser)
            .map(oauth2User -> {
                Collection<? extends GrantedAuthority> mappedAuthorities =
                    this.authoritiesMapper.mapAuthorities(oauth2User.getAuthorities());

                return new OAuth2LoginAuthenticationToken(
                    authorizationCodeAuthentication.getClientRegistration(),
                    authorizationCodeAuthentication.getAuthorizationExchange(),
                    oauth2User,
                    mappedAuthorities,
                    accessToken,
                    accessTokenResponse.getRefreshToken()
                );
            });
    }

    private Mono<OidcIdToken> createOidcToken(
        ClientRegistration clientRegistration,
        OAuth2AccessTokenResponse accessTokenResponse
    ) {
        Assert.notNull(this.jwtDecoderFactory, "jwtDecoderFactory cannot be null. Ensure it's set.");
        ReactiveJwtDecoder jwtDecoder = this.jwtDecoderFactory.createDecoder(clientRegistration);
        String rawIdToken = (String) accessTokenResponse.getAdditionalParameters().get("id_token");
        return jwtDecoder.decode(rawIdToken)
            .map(jwt -> new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(),
                jwt.getExpiresAt(), jwt.getClaims()));
    }
}