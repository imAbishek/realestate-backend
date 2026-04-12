package com.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Real Estate Portal backend.
 *
 *  @EnableCaching     — activates Redis caching via @Cacheable annotations
 *  @EnableAsync       — activates @Async methods (email sending runs off HTTP thread)
 *  @EnableScheduling  — activates @Scheduled cron jobs (search alert emails etc.)
 *  @EnableJpaAuditing — auto-populates @CreatedDate / @LastModifiedDate on entities
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
public class RealEstateApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealEstateApplication.class, args);
    }
}
