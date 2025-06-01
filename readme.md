# LinkedIn Job Search API

This project is a Spring Boot WebFlux application that allows users to search for jobs on LinkedIn, specifically filtering out "Easy Apply" jobs to provide direct application links. It uses OAuth 2.0 and OpenID Connect (OIDC) for authentication with LinkedIn.

## Features

*   Search for jobs on LinkedIn using a query and country.
*   Filters out "Easy Apply" jobs. (Filter out jobs created by Indian scammers and recruitment spams :-) )
*   Provides direct application URLs for the filtered jobs.
*   Secured with OAuth 2.0 (LinkedIn).
*   API documentation via Swagger UI.
*   Session management with Redis.

## Prerequisites

*   **Java 21 JDK** (or newer)
*   **LinkedIn Developer Application**: You need to create an application on the [LinkedIn Developer Portal](https://developer.linkedin.com/) to get a `Client ID` and `Client Secret`.
    *   Ensure your LinkedIn application has the necessary permissions/products enabled (e.g., "Sign In with LinkedIn using OpenID Connect").
    *   Configure the **Authorized redirect URLs** in your LinkedIn app settings. For local development, this will typically be `http://localhost:8080/login/oauth2/code/linkedin`.
*   **Redis Server**: Required for session management as configured.
*   **Docker** (Optional, for running with Cloud Native Buildpacks or custom Docker setup)
*   **GraalVM** (Optional, for building native executables, version 22.3+ required)

## Configuration

1.  **Clone the repository** (if applicable).
2.  **Set up LinkedIn Credentials & Application Configuration**:
    You need to configure your LinkedIn application's `Client ID` and `Client Secret`, and potentially other application settings, in the `src/main/resources/application.yaml` file. You can also use environment variables for these settings.

## Running the Application

You can run the application using the Spring Boot Gradle plugin:

``` bash ./gradlew bootRun ```

The application will start by default on `http://localhost:8080`. Ensure your Redis server is running and accessible.

## Using the API

1.  **Authentication**:
    *   Navigate to a protected endpoint, for example, `http://localhost:8080/jobs/search?query=java&country=us`.
    *   You will be redirected to LinkedIn to authenticate.
    *   After successful authentication, you will be redirected back to the application. Your session will be managed via Redis.

2.  **API Endpoints**:
    *   **Search Jobs**:
        *   `GET /jobs/search`
        *   Query Parameters:
            *   `query` (String, required): The job search query (e.g., "Software Engineer").
            *   `country` (String, required): The country code for the job search (e.g., "us", "gb").
        *   Example: `http://localhost:8080/jobs/search?query=Java%20Developer&country=de`
        *   Returns: A list of direct application URLs for jobs that are not "Easy Apply".

3.  **API Documentation (Swagger UI)**:
    Once the application is running, you can access the Swagger UI for interactive API documentation at:
    `http://localhost:8080/swagger-ui.html`

    The OpenAPI specification is available at:
    `http://localhost:8080/v3/api-docs`

## Security Notes

*   This application uses OAuth 2.0 and OpenID Connect for authentication with LinkedIn.
*   It includes a custom `ReactiveAuthenticationManager` (`NoNonceValidationOidcManager`). **This manager is primarily used to bypass OIDC nonce validation issues that can occur if your application is running behind certain types of proxies that might interfere with the nonce.** If you are not behind such a proxy, standard OIDC nonce validation (which this custom manager skips) is generally recommended for enhanced security against replay attacks.
*   Session management is configured to use Redis, which is suitable for distributed environments and helps maintain user sessions.

## GraalVM Native Support

This project has been configured to let you generate either a lightweight container or a native executable.

### Lightweight Container with Cloud Native Buildpacks
Docker should be installed and configured.

``` bash ./gradlew bootBuildImage docker run --rm linkedin:0.0.1-SNAPSHOT ```


### Executable with Native Build Tools
The GraalVM `native-image` compiler (version 22.3+) should be installed.

``` bash ./gradlew nativeCompile build/native/nativeCompile/linkedin ```

To run tests in a native image:

``` bash ./gradlew nativeTest ```

Refer to the Native Build Tools documentation for more on toolchain support.

## Technologies Used

*   Java 21
*   Spring Boot (version inferred from context, e.g., 3.x)
    *   Spring WebFlux (Reactive Programming)
    *   Spring Security (OAuth2 Client, OIDC)
    *   Spring Data Redis Reactive (for Session Management)
    *   Spring Session Data Redis
*   Project Reactor
*   Springdoc OpenAPI (for Swagger UI)
*   Gradle

## Project Structure

*   `com.example.linkedin.controller`: Contains the REST API controllers.
*   `com.example.linkedin.service`: Contains the business logic, including interaction with the LinkedIn API (`LinkedInService`).
*   `com.example.linkedin.model`: Defines data models (e.g., `Job`).
*   `com.example.linkedin.security`: Contains security configurations (`SecurityConfig`, `OAuth2Config`) and custom authentication components (`NoNonceValidationOidcManager`).

## Future Enhancements & To-Do

- [ ]  Adjust the API docs (e.g., more detailed descriptions, examples for request/response bodies)
- [ ]  Adjust the native build (e.g., optimize for size/speed, explore further GraalVM configurations, ensure all features work in native mode)
- [ ]  Adjust Docker infrastructure (e.g., multi-stage builds for smaller production images, Docker Compose for local development with Redis)
- [ ]  Adjust BDD tests (e.g., expand coverage for different job search scenarios, error handling, authentication flows)