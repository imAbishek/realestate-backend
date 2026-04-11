package com.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Real Estate Portal backend.
 *
 *  @EnableCaching     — activates Redis caching via @Cacheable annotations
 *  @EnableScheduling  — activates @Scheduled cron jobs (search alert emails etc.)
 *  @EnableJpaAuditing — auto-populates @CreatedDate / @LastModifiedDate on entities
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaAuditing
public class RealEstateApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealEstateApplication.class, args);
    }
}
