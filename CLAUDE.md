# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Sonshine Inventory** is a Spring Boot web application for managing a church food pantry — tracking item quantities, donor pledges, and admin inventory management. Public users can view needed items and submit pledges; admins manage inventory and fulfill pledges.

## Build & Run Commands

```bash
./mvnw spring-boot:run      # Start local dev server (port 8080)
./mvnw clean package        # Build WAR artifact
./mvnw test                 # Run tests
./mvnw test -Dtest=FooTest  # Run a single test class
```

## Environment Variables

The app reads DB credentials from environment variables (not committed). For local development, set these before running:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sonshine_inventory
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

Production (AWS Elastic Beanstalk) additionally requires `SPRING_PROFILES_ACTIVE=prod` and `SERVER_PORT=5000`.

## Architecture

**Stack:** Java 25, Spring Boot 4, Thymeleaf (server-rendered HTML), Spring Security, Spring Data JPA/Hibernate, MySQL 8, Flyway migrations.

**Package:** `net.disc0.sonshine_inventory`

**Layers:**
- **Entities** (`/entities`) — JPA-mapped domain objects: `Category`, `Item`, `Pledge`, `User`. Soft-deletes via `active` boolean flag on Category and Item.
- **Repositories** (`/dao`) — Spring Data JPA interfaces: `CategoryRepository`, `ItemRepository`, `PledgeRepository`. User/Authorities repositories not yet created.
- **Services** — Business logic (to be created): needs calculation, pledge expiration via `@Scheduled` nightly job
- **Controllers** — Thymeleaf `@Controller` classes handling HTTP routes (to be created)
- **Templates** — Thymeleaf HTML in `src/main/resources/templates/` (to be created)
- **Migrations** — Flyway SQL scripts in `src/main/resources/db/migration/` (naming: `V1__description.sql`; none created yet)

**Core domain logic (from [DESIGN.md](DESIGN.md)):**
- `Item.need = MAX(0, quota - currentQuantity)`
- `Item.remainingNeed = MAX(0, need - SUM(open_pledge_quantities))`
- Pledge status lifecycle: `OPEN → FULFILLED` (admin action, increments `item.currentQuantity`) or `OPEN → EXPIRED` (nightly scheduler at 2 AM) or `OPEN → CANCELLED`
- `Pledge.expiresAt = createdAt + 7 days`

**Routes (from DESIGN.md):**
- `GET /` — public needs list
- `POST /pledge` — submit a pledge
- `GET /pledge/{id}` — pledge confirmation
- `GET /admin/*` — admin inventory and pledge management (Spring Security protected)
- `GET /admin/login` — login page

**Security:** Spring Security form-based login, BCrypt password hashing. `User` entity uses the standard Spring Security `JdbcDaoImpl` schema (`users` / `authorities` tables); an `Authorities` entity is planned but not yet created.

## Current State

Early-stage. What exists:
- **Entities:** `Category`, `Item`, `Pledge`, `User` (in `/entities`)
- **Repositories:** `CategoryRepository`, `ItemRepository`, `PledgeRepository` (in `/dao`) — bare `JpaRepository` extensions, no custom queries yet
- **Nothing yet:** SQL migrations, services, controllers, Thymeleaf templates, `Authorities` entity, security config class

[DESIGN.md](DESIGN.md) is the authoritative spec for intended behavior.
