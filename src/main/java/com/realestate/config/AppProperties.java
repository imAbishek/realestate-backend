package com.realestate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds all custom `app.*` properties from application.properties
 * into typed Java objects. Inject this class wherever you need
 * JWT secret, S3 config, mail config, etc.
 *
 * Usage:
 *   @Autowired
 *   private AppProperties appProperties;
 *   String secret = appProperties.getJwt().getSecret();
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Aws aws = new Aws();
    private final Mail mail = new Mail();
    private final Cors cors = new Cors();
    private final Pagination pagination = new Pagination();

    // ─────────────────────────────────────────────
    // JWT
    // ─────────────────────────────────────────────
    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpiryMs;
        private long refreshTokenExpiryMs;
        private String issuer;
    }

    // ─────────────────────────────────────────────
    // AWS S3
    // ─────────────────────────────────────────────
    @Data
    public static class Aws {
        private String accessKey;
        private String secretKey;
        private String region;
        private final S3 s3 = new S3();

        @Data
        public static class S3 {
            private String bucketName;
        }
    }

    // ─────────────────────────────────────────────
    // Mail
    // ─────────────────────────────────────────────
    @Data
    public static class Mail {
        private String from;
        private String fromName;
    }

    // ─────────────────────────────────────────────
    // CORS
    // ─────────────────────────────────────────────
    @Data
    public static class Cors {
        /** Comma-separated origins e.g. https://yoursite.com */
        private String allowedOrigins;

        public String[] getAllowedOriginsArray() {
            return allowedOrigins != null
                ? allowedOrigins.split(",")
                : new String[]{};
        }
    }

    // ─────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────
    @Data
    public static class Pagination {
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }
}
