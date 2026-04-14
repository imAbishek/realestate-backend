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
│   ├── AppProperties.java              # Binds all app.* properties (incl. app.aws.endpoint)
│   ├── CorsConfig.java                 # CORS from app.cors.allowed-origins
│   ├── S3Config.java                   # S3Client bean — prod profile only; supports MinIO via endpoint override
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
    ├── AuthService.java                # register (defaults role=BUYER), login (email or phone identifier)
    ├── PropertyService.java            # Full CRUD, search, image mgmt — injects StorageService
    ├── StorageService.java             # Interface: uploadPropertyImage / deleteImage / deleteAllPropertyImages
    ├── ImageUploadService.java         # StorageService impl — dev profile (!prod), local filesystem
    ├── S3StorageService.java           # StorageService impl — prod profile, S3-compatible (AWS or R2/MinIO)
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

## Image storage
Two `StorageService` implementations selected by Spring profile:
- **Dev** (`@Profile("!prod")`): `ImageUploadService` — saves to disk, `WebMvcConfig` serves at `/uploads/**`. Upload dir: `app.storage.upload-dir=uploads`.
- **Prod** (`@Profile("prod")`): `S3StorageService` — uploads to S3-compatible store (Cloudflare R2 in prod). Configured via env vars:
  - `MINIO_ENDPOINT` — set to R2/MinIO endpoint URL; blank = use real AWS S3
  - `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET`
  - When `MINIO_ENDPOINT` is set, `S3Config` adds `.endpointOverride()` + `pathStyleAccessEnabled(true)` to the `S3Client`
  - Image URL format: `{endpoint}/{bucket}/properties/{id}/{uuid}.{ext}`

## Auth identifier resolution
`LoginRequest.identifier` accepts email or 10-digit Indian mobile (`^[6-9]\\d{9}$`).
`AuthService.login()` and `CustomUserDetailsService.loadUserByUsername()` both detect the format and route to `findByEmail()` or `findByPhone()`.
Registration always sets `role = BUYER` — no role field in `RegisterRequest`.

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
