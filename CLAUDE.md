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
- **Repositories** (`/dao`) — Spring Data JPA interfaces: `CategoryRepository`, `ItemRepository`, `PledgeRepository`, `UserRepository`.
- **Controllers** (`/controller`) — Thymeleaf `@Controller` classes: `HomeController`, `PledgeController`, `AdminController`, `SetupController`. No separate service layer; business logic lives in controllers.
- **Security** (`/security`) — `SecurityConfig` configures `JdbcUserDetailsManager`, `BCryptPasswordEncoder`, and the security filter chain.
- **Templates** — Thymeleaf HTML in `src/main/resources/templates/`.
- **Migrations** — Flyway SQL scripts in `src/main/resources/db/migration/` (naming: `V{n}__{description}.sql`).

**Core domain logic (from [DESIGN.md](DESIGN.md)):**
- `Item.need = MAX(0, quota - currentQuantity)`
- `Item.remainingNeed = MAX(0, need - SUM(open_pledge_quantities))`
- Pledge status lifecycle: `OPEN → FULFILLED` (admin action, increments `item.currentQuantity`) or `OPEN → EXPIRED` (nightly scheduler at 2 AM, not yet implemented) or `OPEN → CANCELLED`
- `Pledge.expiresAt = createdAt + 7 days`

**Routes:**
- `GET /` — public needs list, grouped by category
- `GET /pledge?itemId={id}` — pledge form for a specific item
- `POST /pledge/save` — submit a pledge, redirects to home
- `GET /setup` — first-time admin setup (redirects to `/login` if any user exists)
- `POST /setup` — create the first admin account
- `GET /admin` — admin dashboard (categories, items, pledges, users)
- `POST /admin/save` — bulk save categories, items, and user edits
- `POST /admin/pledges/{id}/fulfill` — mark pledge fulfilled, increment item quantity
- `POST /admin/pledges/{id}/cancel` — cancel a pledge
- `POST /admin/categories` — add a new category
- `POST /admin/items` — add a new item
- `GET /admin/users/register` — register a new admin user
- `POST /admin/users/register` — create a new admin user

**Security:** Spring Security form-based login (default `/login` page), BCrypt password hashing. Users are stored in the `users` table and authorities in the `authorities` table, managed via `JdbcUserDetailsManager`. All admin routes require authentication; `/`, `/pledge`, and `/setup` are public.

**Templates:**
- `fragments.html` — shared `head`, `header` (responsive navbar with hamburger), `footer`, and `scripts` (Bootstrap JS) fragments
- `index.html` — public needs list, mobile-first list-group layout grouped by category
- `pledge.html` — pledge form with item details as key-value pairs
- `setup.html` — first-time admin account creation (standalone, no navbar)
- `admin.html` — full admin dashboard with inline-editable categories, items, users tables and pledge management
- `admin-register.html` — form to register additional admin users

**Flyway migrations:**
- `V1` — `users` and `authorities` tables
- `V2` — `categories`, `items`, `pledges` tables
- `V3` — add `AUTO_INCREMENT` to id columns
- `V4` — add `display_name` column to `users`

## Current State

Functionally complete for v1 core features. What exists:
- Public inventory view and pledge submission
- Admin dashboard: inline editing of categories, items, and users; pledge management (fulfill/cancel)
- Admin user registration and first-time setup
- Mobile-responsive UI (Bootstrap 5.3.8)

**Not yet implemented (from DESIGN.md):**
- Nightly pledge expiration scheduler (`@Scheduled` job at 2 AM)
- Pledge confirmation page (`GET /pledge/{id}`)
- `remainingNeed` calculation (subtracting open pledges from displayed need)

[DESIGN.md](DESIGN.md) is the authoritative spec for intended behavior.