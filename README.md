# Movento

Movento is a local-first Netflix-inspired streaming portfolio app. It runs with Docker Compose and includes a Next.js frontend, a Spring Cloud API gateway, Eureka service discovery, Spring Boot domain services, PostgreSQL, Redis, and RabbitMQ.

This repo is intentionally configured for local development and GitHub Actions validation. It does not deploy to Vercel, Render, or any live host.

## System Architecture

Browser traffic goes to the Next.js app at `http://localhost:3000`. The Next.js app acts as a small backend-for-frontend for cookies and browser calls, then forwards API traffic to the public API gateway at `http://localhost:8080`.

The API gateway validates/forwards requests to private Spring services through Eureka. Domain services use PostgreSQL for durable state, Redis for cache/session-style support, and RabbitMQ for asynchronous events.

```text
Browser
  -> web / Next.js 16 :3000
    -> api-gateway :8080
      -> service-registry / Eureka :8761
      -> user-service :8081
      -> payment-service :8082
      -> streaming-service :8083
      -> recommendation-service :8084
      -> content-service :8085
            -> PostgreSQL :5432
            -> Redis :6379
            -> RabbitMQ :5672 / management :15672
```

## Containers

`docker-compose.yml` starts eleven containers:

| Service | Purpose | Port |
|---|---|---:|
| `web` | Next.js frontend and BFF route handlers | `3000` |
| `api-gateway` | Public API entry point and routing | `8080` |
| `service-registry` | Eureka discovery server | `8761` |
| `user-service` | auth, accounts, viewer profiles, refresh tokens | internal `8081` |
| `content-service` | catalog, search, watchlist, progress, admin catalog | internal `8085` |
| `streaming-service` | playback sessions and demo HLS fallback | internal `8083` |
| `recommendation-service` | recommendation API and event foundation | internal `8084` |
| `payment-service` | Stripe test subscription foundation | internal `8082` |
| `postgres` | local PostgreSQL databases | `5432` |
| `redis` | cache/local state support | `6379` |
| `rabbitmq` | events and management UI | `5672`, `15672` |

## Local Commands

Start everything:

```powershell
docker compose up --build
```

Start in the background:

```powershell
docker compose up -d --build
```

Check status:

```powershell
docker compose ps
```

Read logs:

```powershell
docker compose logs -f web
docker compose logs -f api-gateway
docker compose logs -f content-service
```

Stop containers but keep PostgreSQL, Redis, RabbitMQ, and web dependency volumes:

```powershell
docker compose down
```

Remove local volumes only when you want a fresh database/cache/message state:

```powershell
docker compose down -v
```

Health checks:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:3000
```

Open:

- App: `http://localhost:3000`
- Gateway health: `http://localhost:8080/actuator/health`
- Eureka: `http://localhost:8761`
- RabbitMQ UI: `http://localhost:15672` (`guest` / `guest` by default)

## Build Commands

Full local CI:

```powershell
.\scripts\local-ci.ps1
```

The local CI script creates an ignored Java truststore when a certificate is present in `.local-certs/`, then runs the backend reactor, frontend lint/type/build checks in Node 22, Compose validation, and all Docker image builds. This is the closest local equivalent of GitHub Actions.

Backend single module:

```powershell
.\mvnw.cmd clean package -pl content-service -am -DskipTests
```

Frontend:

```powershell
cd web
npm ci --no-audit --no-fund
npm run lint
npm run typecheck
npm run build
```

Docker image build matrix is handled by Compose locally:

```powershell
docker compose build
```

Validate Compose syntax:

```powershell
docker compose config --quiet
```

## Local Certificate Note

If Avast Web/Mail Shield or another antivirus intercepts HTTPS, Node inside Docker may fail with `UNABLE_TO_VERIFY_LEAF_SIGNATURE`. The web image imports optional PEM certificates from `.local-certs/` while it builds.

The real certificate file and generated truststore are ignored by git. If your machine does not need them, leave only `.local-certs/.gitkeep` in place.

## Catalog and Search

`content-service` owns catalog data. Viewer endpoints live under:

- `GET /api/v1/catalog/home`
- `GET /api/v1/catalog/titles`
- `GET /api/v1/catalog/titles?type=MOVIE`
- `GET /api/v1/catalog/titles?type=SERIES`
- `GET /api/v1/catalog/search?q=sci-fi`
- `GET /api/v1/catalog/titles/{slug}`

Search is PostgreSQL-backed for the local MVP. It searches title, synopsis, maturity rating, genre, and release year, then ranks exact/prefix title matches first, trending titles next, and newer titles after that.

## Auth and Local Billing

Auth is handled by `user-service` and proxied through Next.js route handlers:

- Browser forms call `POST /api/auth/register` or `POST /api/auth/login`.
- The Next.js route handler forwards to `api-gateway` and stores the real JWT pair in HTTP-only cookies.
- Protected browser calls go through `/api/backend/**`, which attaches the access token to the gateway.
- The gateway validates the JWT and injects trusted `X-Account-Id`, `X-User-Email`, `X-Profile-Id`, and role headers for internal services.

Quick auth smoke test:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$email = "local$(Get-Date -Format yyyyMMddHHmmss)@example.com"
$body = @{firstName="Local";lastName="User";email=$email;password="Password123!"} | ConvertTo-Json
Invoke-WebRequest -WebSession $session -Uri http://localhost:3000/api/auth/register -Method Post -ContentType application/json -Body $body
Invoke-WebRequest -WebSession $session -Uri http://localhost:3000/api/backend/users/me
```

Stripe is local/test-mode only:

- To open real Stripe Checkout, set `STRIPE_SECRET_KEY` and `STRIPE_PRICE_ID` in `.env`.
- `STRIPE_PRICE_ID` must be a recurring subscription Price ID from Stripe test mode, usually starting with `price_`.
- Recreate payment-service after changing Stripe env vars:

```powershell
docker compose up -d --build payment-service
```

- Test card: `4242 4242 4242 4242`, any future expiry, any CVC.
- If `STRIPE_PRICE_ID` is blank, checkout intentionally returns `http://localhost:3000/account?billing=demo` and the Account page shows setup guidance instead of silently failing.
- Subscription status changes only after Stripe webhooks are delivered to `POST /api/v1/webhooks/stripe` with a valid Stripe signature.

## Frontend Request Performance

The web app runs as a standalone production build on `node:22-alpine`. To keep local requests responsive:

- Public catalog reads use unauthenticated gateway fetches with short Next.js revalidation.
- Protected calls still use `cache: no-store` because they depend on the signed-in user.
- Dependencies, lint, typechecking, and `next build` run while the image is built; the container starts the precompiled application with `node server.js`.
- After changing frontend source, rebuild it with `docker compose up -d --build web`.
- Next BFF route handlers log a warning for backend calls slower than one second.

If the first request after `docker compose up` feels slow, wait for Eureka registration and Spring warmup:

```powershell
docker compose ps
docker compose logs -f api-gateway content-service user-service
```

## Flyway Migrations

Content service migrations:

| Migration | Purpose |
|---|---|
| `V1__Initial_schema.sql` | Base genres, content, movies, TV shows, seasons, episodes, ratings, view history |
| `V2__catalog_mvp.sql` | Slugs, featured/trending flags, language, content discriminator, media assets, trigram title index |
| `V3__profile_library.sql` | Profile-scoped watchlist and playback progress |
| `V4__demo_catalog.sql` | First demo movies and genre links |
| `V5__base_entity_columns.sql` | Adds optimistic-lock `version` columns expected by Java entities |
| `V6__view_history_entity_alignment.sql` | Aligns view history fields with the Java entity |
| `V7__view_history_user_id_bigint.sql` | Converts view history user IDs back to numeric IDs |
| `V8__expanded_demo_catalog.sql` | Adds more movies, series, genres, and genre links |

User service migrations include the base user schema, viewer profiles/normalized email, and refresh tokens. Payment service migrations include payment/subscription tables and webhook event tracking.

## Java Service Notes

The root Maven build is the canonical backend build. Each Spring Boot service is a Maven module using Java 17 and Spring Boot 3/Jakarta APIs.

Important packages:

- `controller`: public HTTP endpoints.
- `service`: application logic and transaction boundaries.
- `repository`: Spring Data JPA access.
- `model`: JPA entities mapped to Flyway-managed SQL tables.
- `dto`: request/response objects and mapper types.
- `config`: local infrastructure settings such as RabbitMQ, Redis, Elasticsearch toggles, metrics, and security.

The backend uses `spring.jpa.hibernate.ddl-auto=validate`, so Java entities must match Flyway SQL. If an entity and table drift apart, the service fails fast during startup instead of silently changing the database.

## Frontend Notes

The `web` app uses Next.js App Router, React 19, TypeScript, Tailwind CSS, and small UI primitives.

Key paths:

- `web/src/app/(app)/browse/page.tsx`: home/browse experience.
- `web/src/app/(app)/search/page.tsx`: server-rendered search using the gateway.
- `web/src/app/(app)/title/[slug]/page.tsx`: title detail page.
- `web/src/components/title-card.tsx`: reusable poster card.
- `web/src/components/content-rail.tsx`: horizontal content rails.
- `web/src/lib/api.ts`: server-side gateway calls with demo fallback data.

## GitHub Actions

`.github/workflows/ci-cd.yml` validates the supported local stack:

1. `backend`: runs `./mvnw --batch-mode verify`.
2. `frontend`: runs `npm ci`, lint, typecheck, and build.
3. `docker`: builds every backend service Docker image without pushing.
4. `compose`: starts the stack, waits for readiness, checks routing and Eureka registration, and always cleans up.

No deploy jobs are included.

`docker-compose-full.yml`, `nginx/`, and the old Prometheus/Grafana/Elasticsearch configuration are retained only as legacy reference material. They are not part of the supported local stack or CI and should not be used as a production deployment definition.

## Troubleshooting

RabbitMQ cookie permission errors:

```powershell
docker volume rm movento_rabbitmq_data
docker compose up -d rabbitmq
```

Do not remove PostgreSQL or Redis volumes unless you want to reset app data.

Re-run only one service after code changes:

```powershell
docker compose up -d --build content-service
```

Check why a service exited:

```powershell
docker compose ps --all
docker compose logs --tail=200 content-service
```

The production web container should start quickly. A frontend rebuild is slower because it performs a clean install, lint, typecheck, and production compilation before replacing the running container.
