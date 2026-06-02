# Backend — Local Setup (Windows / Linux / macOS)

Spring Boot 3.2.5 REST API on **Java 17 + Maven**, backed by **PostgreSQL + Redis + MinIO**
(all three run in Docker). The Spring app itself runs on your host, not in Docker.

> First time on this machine? Install prerequisites via the master guide:
> [`../SETUP.md` §1](../SETUP.md). You need **JDK 17, Maven, Docker** for the backend.

---

## Step 1 — Verify your toolchain

```bash
java -version       # must be 17.x
mvn -version        # Apache Maven 3.x, running on Java 17
docker compose version
```

If `mvn -version` shows a Java other than 17, set `JAVA_HOME` to your JDK 17 and reopen the terminal.

There is **no Maven wrapper** (`mvnw`) in this repo — you must use a system-installed `mvn`.

---

## Step 2 — Start the Docker services (postgres + redis + minio)

```bash
docker compose up -d
```

Wait until all three containers are healthy:

```bash
docker compose ps
```

This brings up:

| Container | Port | Purpose |
|---|---|---|
| `realestate_postgres` | 5432 | Database `realestate_db` (postgres / postgres) |
| `realestate_redis` | 6379 | Cache (dev only) |
| `realestate_minio` | 9000 (API) / 9001 (console) | Local S3 for image uploads (minioadmin / minioadmin) |

> **Windows/macOS:** Docker Desktop must be running first (launch it from the Start menu /
> Applications). **Linux:** `sudo systemctl start docker` if the daemon isn't up.

---

## Step 3 — Run the API

```bash
mvn spring-boot:run
```

On first start, **Flyway** runs the migrations in `src/main/resources/db/migration/` and creates
all tables automatically. You should see `BUILD`/startup logs ending with Tomcat on port 8080.

Verify it's alive:

```bash
curl http://localhost:8080/api/actuator/health      # → {"status":"UP"}
```

Then open **Swagger** to explore/test endpoints: http://localhost:8080/api/swagger-ui.html

> The active Spring profile is `dev` by default. Image uploads in dev save to the local
> `uploads/` folder and are served at `GET /uploads/**` (MinIO is wired but the dev profile
> uses the filesystem storage implementation).

---

## Step 4 — Validate after any change

```bash
mvn compile        # must show BUILD SUCCESS
```

Fix all errors before considering a change done.

---

## Reset the database (wipe all data)

```bash
docker compose down -v && docker compose up -d
```

`-v` deletes the Postgres + MinIO volumes, so Flyway re-creates a clean schema on the next API start.

---

## Stop everything

```bash
# stop the API: Ctrl+C in its terminal
docker compose down        # stop containers, keep data
docker compose down -v     # stop containers AND wipe data
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Port 5432 is already allocated` | Another Postgres is running. Stop it, or change the port in `docker-compose.yml`. |
| API fails: `Connection refused` to DB | Containers not healthy yet — `docker compose ps`, wait, retry `mvn spring-boot:run`. |
| `mvn` not found | Install Maven (see master guide §1) and reopen the terminal. |
| Flyway validation error | You edited an existing migration — never do that. Add a new `V{n}__*.sql` instead, or reset with `docker compose down -v`. |
| Wrong Java version | Set `JAVA_HOME` to JDK 17. |
| Port 8080 in use | Stop the other process (Win: `netstat -ano \| findstr :8080`), or set `server.port` in `application-dev.properties`. |
