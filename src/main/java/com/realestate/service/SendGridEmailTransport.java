package com.realestate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prod email delivery via the SendGrid Web API over HTTPS (port 443).
 *
 * Why not SMTP: Render (and most PaaS hosts) block outbound SMTP ports
 * (25/465/587), so JavaMailSender connections time out. The HTTPS API is
 * the supported path. Uses the same SendGrid API key as the SMTP password.
 */
@Component
@Profile("prod")
@Slf4j
public class SendGridEmailTransport implements EmailTransport {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    private final AppProperties appProperties;
    private final ObjectMapper  objectMapper;
    private final HttpClient    httpClient;

    public SendGridEmailTransport(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper  = objectMapper;
        this.httpClient    = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        String apiKey = appProperties.getMail().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("SendGrid API key not configured (app.mail.api-key); skipping email to {}", to);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SENDGRID_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(to, subject, htmlBody), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202) {
                log.debug("Email sent via SendGrid API to: {} | Subject: {}", to, subject);
            } else {
                // 4xx usually = unverified sender / bad key; body has the reason.
                log.error("SendGrid API rejected email to {} (HTTP {}): {}", to, response.statusCode(), response.body());
            }
        } catch (java.io.IOException e) {
            log.error("Failed to send email via SendGrid API to {}: {}", to, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending email via SendGrid API to {}: {}", to, e.getMessage());
        }
    }

    /** Builds the SendGrid v3 mail/send JSON. Package-private for testing. */
    String buildPayload(String to, String subject, String htmlBody) throws JsonProcessingException {
        Map<String, Object> from = new LinkedHashMap<>();
        from.put("email", appProperties.getMail().getFrom());
        from.put("name",  appProperties.getMail().getFromName());

        Map<String, Object> personalization = Map.of("to", List.of(Map.of("email", to)));
        Map<String, Object> content         = Map.of("type", "text/html", "value", htmlBody);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("personalizations", List.of(personalization));
        body.put("from", from);
        body.put("subject", subject);
        body.put("content", List.of(content));

        return objectMapper.writeValueAsString(body);
    }
}
