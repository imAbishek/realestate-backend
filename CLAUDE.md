# Backend — Spring Boot API

## Tech stack
- Java 17, Spring Boot 3.2.5, Maven
- Spring Security + JWT (stateless, no sessions)
- Spring Data JPA + Hibernate, PostgreSQL, Flyway migrations
- Redis caching, AWS S3 / MinIO for image storage
- SpringDoc OpenAPI (Swagger UI at /api/swagger-ui.html in dev)

## Exact package structure (as of current codebase)
```
com/realestate/
├── RealEstateApplication.java          # @EnableCaching @EnableScheduling @EnableJpaAuditing
├── config/
│   ├── AppProperties.java              # Binds all app.* properties
│   ├── CorsConfig.java                 # CORS from app.cors.allowed-origins
│   ├── SecurityConfig.java             # JWT filter chain, route access rules
│   └── WebMvcConfig.java               # Serves /uploads/** from local filesystem
├── controller/
│   ├── AuthController.java             # /auth/**
│   ├── PropertyController.java         # /properties/**
│   ├── SearchController.java           # /search/**
│   └── AdminController.java            # /admin/** (ADMIN role only)
├── dto/
│   ├── auth/AuthDtos.java              # RegisterRequest, LoginRequest, AuthResponse, etc.
│   └── property/
│       ├── PropertyDtos.java           # PropertyRequest, PropertyCardResponse, PropertyDetailResponse
│       └── PropertySearchRequest.java  # All search filter params
├── entity/
│   ├── User.java                       # Role enum: BUYER SELLER AGENT ADMIN
│   ├── Property.java                   # ListingType, PropertyType, ListingStatus, etc.
│   ├── PropertyImage.java
│   ├── City.java
│   ├── Locality.java
│   └── Amenity.java
├── exception/
│   ├── GlobalExceptionHandler.java     # @RestControllerAdvice — all error responses
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   └── UnauthorizedException.java
├── repository/
│   ├── UserRepository.java
│   ├── PropertyRepository.java
│   ├── PropertyImageRepository.java
│   ├── CityRepository.java
│   ├── LocalityRepository.java
│   ├── AmenityRepository.java
│   └── PropertySpecification.java      # Dynamic JPA Criteria search builder
├── security/
│   ├── JwtUtil.java                    # Token generation + validation
│   ├── JwtAuthFilter.java              # Intercepts every request
│   └── CustomUserDetailsService.java   # Loads user by email for Spring Security
└── service/
    ├── AuthService.java                # register, login, OTP, password reset
    ├── PropertyService.java            # Full CRUD, search, image mgmt
    ├── ImageUploadService.java         # S3/MinIO upload + delete
    └── EmailService.java               # OTP + inquiry notification emails (async)
```

## Database
- PostgreSQL in Docker (localhost:5432, db: realestate_db)
- Flyway migrations auto-run on startup — src/main/resources/db/migration/
  - V1__create_users_table.sql
  - V2__create_property_tables.sql
- New schema changes → new file V3__, V4__ etc. NEVER edit V1 or V2

## Key enums (must stay in sync with frontend src/types/index.ts)
- UserRole:         BUYER, SELLER, AGENT, ADMIN
- ListingType:      SALE, RENT, PG
- ListingStatus:    DRAFT, PENDING_REVIEW, ACTIVE, EXPIRED, REJECTED, SOLD_RENTED
- PropertyType:     APARTMENT, INDEPENDENT_HOUSE, VILLA, PLOT, COMMERCIAL_OFFICE,
                    COMMERCIAL_SHOP, BUILDER_FLOOR, PG_HOSTEL
- FurnishingStatus: UNFURNISHED, SEMI_FURNISHED, FULLY_FURNISHED
- PriceUnit:        TOTAL, PER_MONTH, PER_SQFT

## Security rules (from SecurityConfig.java)
Public (no token):
  /auth/**
  GET /properties/**
  POST /properties/*/inquiries   ← guest inquiry allowed
  GET /search/**
  GET /uploads/**                ← local image serving
  /swagger-ui/**, /v3/api-docs/**
  /actuator/health

Admin only:  /admin/**
Everything else: requires valid JWT Bearer token

## Local image storage
- WebMvcConfig serves files at GET /uploads/**
- Upload dir configured via: app.storage.upload-dir=uploads
- Files stored at: realestate-backend/uploads/properties/{id}/filename

## Coding rules
- Use Lombok: @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor on entities
- Never expose entity classes in API responses — always use DTOs
- Each repository interface in its own file (Spring Data requirement)
- Exception classes are separate files (not nested)
- Add @PreAuthorize("hasRole('ADMIN')") on admin controller methods

## Validation command
```bash
mvn compile
```
Must show BUILD SUCCESS. Fix all errors before saying done.
