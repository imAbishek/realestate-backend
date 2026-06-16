<div align="center">

# 🏡 PropFind API

**The Spring Boot backend powering PropFind — a full-stack real estate listing platform.**

Browse, post, and inquire on properties; admins moderate listings. One API serves both a Next.js web app and a React Native mobile app.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Flyway-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT%20stateless-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/tests-54%20passing-brightgreen)](#-testing--ci)
[![CodeQL](https://img.shields.io/badge/SAST-CodeQL-blueviolet?logo=github)](.github/workflows/codeql.yml)
[![Sentry](https://img.shields.io/badge/observability-Sentry-362D59?logo=sentry&logoColor=white)](https://sentry.io/)

</div>

---

## 🔗 Live

| | |
|---|---|
| **API base** | https://realestate-backend-tgbv.onrender.com/api |
| **Health** | [`/actuator/health`](https://realestate-backend-tgbv.onrender.com/api/actuator/health) |
| **Web app** | https://realestate-frontend-ten-lyart.vercel.app |

> ⏱️ Hosted on Render's free tier — the API spins down after 15 min idle, so the first request may take ~30s to cold-start.

## ✨ What it does

- **Authentication** — register/login with **email _or_ 10-digit Indian mobile**, stateless JWT, password reset via email OTP, BCrypt-hashed passwords.
- **Property listings** — full CRUD with image uploads, plot/agricultural/promoter fields, and owner-uploaded verification documents.
- **Powerful search** — dynamic filtering (type, price, location, bedrooms, amenities…) built with JPA Criteria specifications.
- **Admin moderation** — approve/reject listings, feature toggles, ban/reinstate users, KPIs & analytics, verification-document review.
- **Site visits & favorites** — book/cancel property viewings, save listings.
- **Pluggable image storage** — local filesystem in dev, S3-compatible (Cloudflare R2) in prod, selected by Spring profile.

## 🏗️ Architecture

Clean layered architecture — `controller → service → repository → entity`:

```
com.realestate
├── controller/   # Thin HTTP layer (@PreAuthorize role checks)
├── service/      # All business logic (Auth, Property, Email, Storage)
├── repository/   # Spring Data JPA + PropertySpecification (dynamic queries)
├── entity/       # JPA entities → PostgreSQL
├── dto/          # Request/response DTOs (entities never exposed) + MapStruct
├── security/     # JwtUtil, JwtAuthFilter, RateLimitFilter
├── config/       # SecurityConfig, CorsConfig, S3Config, WebMvcConfig
└── exception/    # GlobalExceptionHandler → consistent JSON errors
```

## 🛠️ Tech stack

| Area | Choice |
|---|---|
| Language / framework | Java 17, Spring Boot 3.2.5 (Maven) |
| Security | Spring Security, stateless JWT (HS256), BCrypt (strength 12) |
| Data | Spring Data JPA + Hibernate 6, PostgreSQL, **Flyway** migrations (`ddl-auto=validate`) |
| Caching | Spring Cache (in-memory) |
| Rate limiting | Bucket4j (in-memory, per-IP) |
| Storage | AWS S3 SDK v2 → Cloudflare R2 (prod) / local FS (dev) |
| Mapping | MapStruct, Lombok |
| Docs | SpringDoc OpenAPI (Swagger UI, dev only) |
| Email | Spring Mail (async) |

## 🔒 Security highlights

Security is treated as a first-class concern (mapped against the OWASP Top 10):

- **Stateless JWT** auth with **role-based access control** (`@PreAuthorize` + URL rules).
- **Per-IP rate limiting** on auth, inquiries, and uploads (brute-force / abuse protection).
- **IDOR protection** with ownership checks (covered by dedicated tests).
- **Magic-byte file validation** on uploads (not just extension/MIME).
- **Generic error responses** to clients; full stack traces logged server-side only.
- **CodeQL** static analysis (SAST) + **Dependabot** dependency scanning (SCA) in CI.
- **Sentry** error monitoring in prod with email + Slack alerting.

See the workspace-level `SECURITY.md` for the full posture and roadmap.

## ✅ Testing & CI

Tests are **mandatory** — every change ships with tests and `mvn test` must pass. Enforced by GitHub Actions on every push/PR to `main`.

```bash
mvn test          # compiles + runs the full suite (54 tests)
```

Stack: JUnit 5 + Mockito + AssertJ + Spring Security Test. Coverage spans auth/JWT, IDOR/ownership guards, rate limiting, file validation, uploads, site visits, and favorites.

## 🚀 Getting started

> **Full per-OS setup (Windows / Linux / macOS): see [`SETUP.md`](SETUP.md).**

```bash
# 1. Start dependencies (Postgres + Redis + MinIO)
docker compose up -d

# 2. Run the API (dev profile by default)
mvn spring-boot:run
```

| Service | URL |
|---|---|
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| MinIO console | http://localhost:9001 |

## 🧩 Part of PropFind

| Repo | Description |
|---|---|
| **realestate-backend** (this) | Spring Boot REST API |
| [realestate-frontend](https://github.com/imAbishek/realestate-frontend) | Next.js 16 web app |
| [realestate-mobile](https://github.com/imAbishek/realestate-mobile) | React Native + Expo app |

---

<div align="center">
Built by <a href="https://github.com/imAbishek">Abishek</a> · Java 17 · Spring Boot
</div>
