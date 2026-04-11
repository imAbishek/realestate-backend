package com.realestate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS config — tells Spring which origins, methods, and headers
 * the browser is allowed to send to this API.
 *
 * The allowed origins come from application.properties:
 *   app.cors.allowed-origins=http://localhost:3000,https://yoursite.com
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Which frontends can call this API
        config.setAllowedOrigins(
            Arrays.asList(appProperties.getCors().getAllowedOriginsArray())
        );

        // HTTP methods the frontend can use
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers the frontend can send (Authorization is needed for JWT)
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With"
        ));

        // Allow the browser to read the Authorization header in responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies / Authorization header (needed for JWT bearer tokens)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS requests)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
