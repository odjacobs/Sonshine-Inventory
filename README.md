# Sonshine Inventory

A web application for managing a church food pantry — tracking stock levels, donor pledges, and admin inventory
management.

## What it does

**Public side:** Community members visit the home page to see what items the pantry currently needs. They can pledge to
donate a specific quantity of an item, leaving their name and contact info. A confirmation screen is shown immediately;
no account is required.

**Admin side:** Staff log in to manage the item catalog, set stock quotas, record incoming donations, and fulfill or
cancel pledges. A dashboard gives an at-a-glance view of inventory health.

## Tech stack

| Layer       | Technology                                |
|-------------|-------------------------------------------|
| Language    | Java 25                                   |
| Framework   | Spring Boot 4                             |
| UI          | Thymeleaf + Bootstrap 5 (server-rendered) |
| Persistence | Spring Data JPA / Hibernate               |
| Database    | MySQL 8                                   |
| Auth        | Spring Security — form login, BCrypt      |
| Migrations  | Flyway                                    |
| Hosting     | AWS Elastic Beanstalk + RDS               |

## Getting started

**Prerequisites:** Java 25, Maven, a running MySQL 8 instance.

1. Create the database:
```sql
CREATE DATABASE sonshine_inventory;
```

2. Set environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sonshine_inventory
export SPRING_DATASOURCE_USERNAME=<user>
export SPRING_DATASOURCE_PASSWORD=<password>
```

3. Start the dev server:
```bash
./mvnw spring-boot:run
```
The app will be available at `http://localhost:8080`. Flyway applies migrations automatically on startup.

4. On first run, visit `http://localhost:8080/setup` to create the initial admin account. This page is only accessible when no users exist in the database.

## Build

```bash
./mvnw clean package   # produces target/sonshine_inventory-*.war
./mvnw test            # run tests
```

## Deployment

Hosted on AWS Elastic Beanstalk (Java SE platform) backed by an RDS MySQL instance. Additional environment variables
required in production:

| Variable                 | Value  |
|--------------------------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT`            | `5000` |

Deploy by uploading the WAR artifact to Elastic Beanstalk via the AWS console or CLI.

## Design

See [DESIGN.md](DESIGN.md) for the full system design: data model, business logic, screen flows, and route reference.
