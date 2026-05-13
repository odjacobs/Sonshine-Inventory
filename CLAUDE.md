# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Sonshine Inventory** is a Spring Boot web application for managing a church food pantry — tracking item quantities, donor pledges, and admin inventory management. Public users can view needed items and submit pledges; admins manage inventory and fulfill pledges.

**License:** GNU General Public License v3.0

## Build & Run Commands

```bash
./mvnw spring-boot:run      # Start local dev server (port 8080 by default)
./mvnw clean package        # Build JAR artifact
./mvnw test                 # Run tests
./mvnw test -Dtest=FooTest  # Run a single test class
```

## Environment Variables & Local Dev

SSL and server config are externalized. For local development, activate the `local` Spring profile:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

`src/main/resources/application-local.properties` (gitignored) holds local overrides — copy from `deploy/env.example` and fill in values. This file is never committed.

Production (DigitalOcean droplet via systemd) sets env vars via `/etc/sonshine/env` (see `deploy/sonshine-inventory.service` and `deploy/env.example`).

**Key env vars:**

| Variable | Purpose |
|---|---|
| `SERVER_PORT` | HTTP port (default `8080`) |
| `SSL_ENABLED` | Enable HTTPS (default `false`) |
| `SSL_KEY_STORE` | Path to keystore (e.g. `/opt/sonshine-inventory/keystore.p12`) |
| `SSL_KEY_STORE_PASSWORD` | Keystore password |
| `SSL_KEY_ALIAS` | Key alias in keystore |
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |

## Architecture

**Stack:** Java 25, Spring Boot 4, Thymeleaf (server-rendered HTML), Spring Security, Spring Data JPA/Hibernate, MySQL 8, Flyway migrations.

**Package:** `net.disc0.sonshine_inventory`

**Layers:**
- **Entities** (`/entities`) — JPA-mapped domain objects: `Category`, `Item`, `Pledge`, `User`. Soft-deletes via `active` boolean flag on Category and Item.
- **Repositories** (`/dao`) — Spring Data JPA interfaces: `CategoryRepository`, `ItemRepository`, `PledgeRepository`, `UserRepository`.
- **Controllers** (`/controller`) — Thymeleaf `@Controller` classes: `HomeController`, `PledgeController`, `AdminController`, `SetupController`. No separate service layer; business logic lives in controllers. `GlobalModelAdvice` injects `appVersion` into every model.
- **Security** (`/security`) — `SecurityConfig` configures `JdbcUserDetailsManager`, `BCryptPasswordEncoder`, and the security filter chain.
- **Templates** — Thymeleaf HTML in `src/main/resources/templates/`.
- **Migrations** — Flyway SQL scripts in `src/main/resources/db/migration/` (naming: `V{n}__{description}.sql`).

**Core domain logic (from [DESIGN.md](DESIGN.md)):**
- `Item.need = MAX(0, quota - currentQuantity)`
- `Item.remainingNeed = MAX(0, need - SUM(open_pledge_quantities))` — implemented in `HomeController` and `PledgeController`
- Pledge status lifecycle: `OPEN → FULFILLED` (admin action, increments `item.currentQuantity`) or `OPEN → EXPIRED` (nightly scheduler at 2 AM) or `OPEN → CANCELLED`
- `Pledge.expiresAt = createdAt + 7 days`

**Routes:**
- `GET /` — public needs list, grouped by category
- `GET /pledge?itemId={id}` — pledge form for a specific item
- `POST /pledge/save` — submit a pledge, redirects to confirmation
- `GET /pledge/{publicId}` — pledge confirmation page (UUID-based public id)
- `GET /setup` — first-time admin setup (redirects to `/login` if any user exists)
- `POST /setup` — create the first admin account
- `GET /admin` — admin dashboard (categories, items, pledges, users)
- `GET /admin/pledges/fragment?pledgeStatus={status}` — AJAX endpoint returning pledge card body fragment only
- `POST /admin/save` — bulk save categories, items, and user edits
- `POST /admin/pledges/{id}/fulfill` — mark pledge fulfilled, increment item quantity
- `POST /admin/pledges/{id}/cancel` — cancel a pledge
- `POST /admin/categories` — add a new category
- `POST /admin/items` — add a new item
- `GET /admin/users/register` — register a new admin user
- `POST /admin/users/register` — create a new admin user
- `GET /admin/export/pledges.csv` — CSV export of pledges
- `GET /admin/export/inventory.csv` — CSV export of inventory
- `GET /actuator/health` — public health endpoint for load-balancer checks

**Security:** Spring Security form-based login (default `/login` page), BCrypt password hashing. Users stored in `users` table, authorities in `authorities` table, managed via `JdbcUserDetailsManager`. All admin routes require authentication; `/`, `/pledge/**`, `/setup`, and `/actuator/health` are public. CSRF protection enabled (Spring Security default).

**Templates:**
- `fragments.html` — shared `head`, `header` (responsive navbar), `footer` (shows version + GPL v3 link), and `scripts` fragments
- `index.html` — public needs list, mobile-first card layout grouped by category
- `pledge.html` — pledge form with item details
- `setup.html` — first-time admin account creation (standalone, no navbar)
- `admin.html` — full admin dashboard; inline-editable categories/items/users, AJAX pledge filter, inactive item/category toggle
- `admin-register.html` — form to register additional admin users
- `pledge-confirmation.html` — post-submission confirmation page
- `error/403.html`, `error/404.html`, `error/500.html`, `error/error.html` — custom error pages

**Flyway migrations:**
- `V1` — `users` and `authorities` tables
- `V2` — `categories`, `items`, `pledges` tables
- `V3` — add `AUTO_INCREMENT` to id columns
- `V4` — add `display_name` column to `users`
- `V5` — add `public_id` (UUID) to `pledges`

## Deployment

See `deploy/` for production artifacts:
- `sonshine-inventory.service` — systemd unit file for DigitalOcean droplet
- `env.example` — template for `/etc/sonshine/env` secrets file
- `demo-data.sql` — seed data for demos

Deploy flow: `./mvnw clean package` → `scp target/Sonshine_Inventory-*.jar droplet:/opt/sonshine-inventory/sonshine-inventory.jar` → `systemctl restart sonshine-inventory`.

## Current State

v1.1.1 — functionally complete. Implemented features:
- Public inventory view and pledge submission
- Admin dashboard: inline editing of categories/items/users; AJAX pledge status filter; inactive item/category toggle (hidden by default, toggle to reveal)
- Pledge management: fulfill, cancel, nightly expiration scheduler (2 AM)
- CSV export for pledges and inventory
- Admin user registration and first-time setup
- Mobile-responsive UI (Bootstrap 5.3.8)
- Custom error pages (403, 404, 500)
- Version displayed in footer, read from build-info at compile time

**Queued / not yet started:**
- Admin dashboard at `/admin/dashboard` (summary stats, charts)

[DESIGN.md](DESIGN.md) is the authoritative spec for intended behavior.
