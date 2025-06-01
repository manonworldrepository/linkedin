package com.example.linkedin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class LinkedInMockServer {
    private static WireMockServer wireMockServer;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInMockServer.class);

    @Bean
    public WebClient.Builder webClientBuilder() {
        // This ensures that the WebClient used by LinkedInService in tests
        // points to our WireMock instance.
        logger.info("Configuring WebClient.Builder to use base URL: http://localhost:8089");
        return WebClient.builder().baseUrl("http://localhost:8089");
    }

    @BeforeAll
    public static void startServer() {
        // Check if the server is already running to prevent issues if hooks are called multiple times
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            logger.info("Attempting to start WireMock server on port 8089...");
            wireMockServer = new WireMockServer(8089); // Or WireMockConfiguration.options().port(8089)
            try {
                wireMockServer.start();
                logger.info("WireMock server started successfully on port: {}", wireMockServer.port());

                // Configure stubs
                wireMockServer.stubFor(get(urlPathMatching("/jobs.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                // IMPORTANT: Ensure this body matches your Job record, including 'easyApply'
                                .withBody("[{\"id\": \"123\", \"applyUrl\": \"https://company.com/apply\", \"easyApply\": false}]")));
                logger.info("WireMock stubs configured.");

            } catch (Exception e) {
                logger.error("Failed to start or configure WireMock server.", e);
                // If startup fails, ensure wireMockServer is null so stopServer doesn't try to stop a non-existent server
                if (wireMockServer != null && wireMockServer.isRunning()) {
                    wireMockServer.stop(); // Attempt to clean up if partially started
                }
                wireMockServer = null;
            }
        } else {
            logger.info("WireMock server is already running on port: {}", wireMockServer.port());
        }
    }

    @AfterAll
    public static void stopServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            logger.info("Attempting to stop WireMock server on port: {}", wireMockServer.port());
            try {
                wireMockServer.stop();
                logger.info("WireMock server stopped successfully.");
            } catch (Exception e) {
                logger.error("Error occurred while stopping WireMock server.", e);
            }
            // Optionally set to null after stopping, though for @AfterAll it's usually the end.
            // wireMockServer = null;
        } else {
            logger.warn("WireMock server was not running or was null at @AfterAll. No stop action taken.");
        }
    }
}