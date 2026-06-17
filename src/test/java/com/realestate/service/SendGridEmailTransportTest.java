package com.realestate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SendGridEmailTransportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppProperties appProperties;
    private SendGridEmailTransport transport;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getMail().setFrom("noreply@propfind.in");
        appProperties.getMail().setFromName("PropFind");
        appProperties.getMail().setApiKey("SG.test-key");
        transport = new SendGridEmailTransport(appProperties, objectMapper);
    }

    @Test
    void buildPayload_producesValidSendGridV3Json() throws Exception {
        String json = transport.buildPayload("user@example.com", "Reset your password", "<h2>Code: 123456</h2>");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("from").get("email").asText()).isEqualTo("noreply@propfind.in");
        assertThat(root.get("from").get("name").asText()).isEqualTo("PropFind");
        assertThat(root.get("subject").asText()).isEqualTo("Reset your password");

        JsonNode to = root.get("personalizations").get(0).get("to").get(0);
        assertThat(to.get("email").asText()).isEqualTo("user@example.com");

        JsonNode content = root.get("content").get(0);
        assertThat(content.get("type").asText()).isEqualTo("text/html");
        assertThat(content.get("value").asText()).isEqualTo("<h2>Code: 123456</h2>");
    }

    @Test
    void buildPayload_escapesSpecialCharactersInHtmlBody() throws Exception {
        // Quotes and newlines must survive JSON encoding so SendGrid receives valid JSON.
        String html = "<a href=\"https://x.com\">link</a>\nLine2";
        String json = transport.buildPayload("u@x.com", "S\"ubject", html);

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.get("content").get(0).get("value").asText()).isEqualTo(html);
        assertThat(root.get("subject").asText()).isEqualTo("S\"ubject");
    }
}
