package com.example.linkedin.service;

import com.example.linkedin.model.Job;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LinkedInService {
    private final WebClient webClient;
    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    public LinkedInService(WebClient.Builder webClientBuilder, ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = webClientBuilder.baseUrl("https://api.linkedin.com/v2").build();
        this.authorizedClientService = authorizedClientService;
    }

    public Mono<List<String>> getDirectApplyLinks(String query, String country) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .cast(OAuth2AuthenticationToken.class)
            .flatMap(authToken -> authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName()
            ))
            .cast(OAuth2AuthorizedClient.class)
            .map(OAuth2AuthorizedClient::getAccessToken)
            .map(AbstractOAuth2Token::getTokenValue)
            .flatMap(accessToken -> {
                System.out.println("Security Token Debug: " + accessToken);

                return webClient.get()
                    .uri("/jobs?q=" + query + "&country=" + country)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToFlux(Job.class)
                    .filter(job -> !job.easyApply())
                    .map(Job::applyUrl)
                    .collectList();
            });
    }
}
