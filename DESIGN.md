Food Pantry Inventory Management System	**Design Document**

**Food Pantry**

**Inventory Management System**

System Design Document - v1.0

Spring Boot + AWS Elastic Beanstalk + MySQL RDS

*Prepared for Internal Use*

# **1. Purpose ****&**** Scope**

This document describes the design of a web-based inventory management system for a church food pantry. The system has two distinct sides: a public-facing donor portal where community members can see current needs and make pledges, and a password-protected admin portal where staff manage inventory, items, quotas, and fulfillment.

This document covers the v1.0 scope and is intended to guide development decisions before any code is written.

# **2. Goals ****&**** Non-Goals**

## **2.1 Goals**

- Allow the public to see what items the pantry currently needs and pledge to donate specific items

- Allow admins to manage item catalog, set stock quotas, and edit inventory counts

- Allow admins to view open pledges and mark them fulfilled when donations arrive

- Provide a basic dashboard showing inventory health at a glance

- Mobile-friendly UI for both donor and admin sides

- Low-cost hosting on AWS free tier

## **2.2 Non-Goals (v1)**

- Client/recipient tracking — the system does not track who receives food

- Expiration date or lot/batch tracking

- Donor notifications (email/SMS confirmations) — confirmation is on-screen only

- Barcode or UPC scanning

- Public-facing REST API — all API endpoints are internal only

- Financial donation tracking

# **3. Users ****&**** Roles**

| **Role** | **Authentication** | **Capabilities** |
| --- | --- | --- |
| Public Donor | None — anonymous | View needs list, submit a pledge with name + phone or email |
| Admin / Staff | Username + password (Spring Security) | Full access to inventory, items, categories, quotas, pledges, and dashboard |

Admin accounts are individual per staff member with no permission levels — all admins share equal access. Account creation is handled by an existing admin (no self-registration).

# **4. Data Model**

## **4.1 Entity Overview**

The following entities form the core domain:

| **Entity** | **Description** |
| --- | --- |
| Category | A top-level grouping (e.g., Canned Goods, Bread, Frozen Items, Cereal, Pasta, Toiletries) |
| Item | A specific product within a category (e.g., Canned Green Beans, Cheerios). Has a quota and a current inventory count. |
| Pledge | A donor's commitment to bring in a quantity of a specific item, with contact info and an expiration date. |
| User | A staff member who can log in and manage the system. |
| Authorities | Roles granted to a user (e.g., `ROLE_ADMIN`); used by Spring Security. |

## **4.2 Entity Details**

### **Category**

| **Field** | **Type** | **Notes** |
| --- | --- | --- |
| id | INT PK | Auto-generated |
| name | VARCHAR(100) | Unique, not null (e.g., "Canned Goods") |
| displayOrder | INT | Controls sort order on public page |
| active | BOOLEAN | Soft-delete flag; inactive categories are hidden |

### **Item**

| **Field** | **Type** | **Notes** |
| --- | --- | --- |
| id | INT PK | Auto-generated |
| category | FK → Category | Many-to-one |
| name | VARCHAR(150) | e.g., "Canned Green Beans" |
| unitLabel | VARCHAR(50) | e.g., "cans", "loaves", "boxes" — used in UI display |
| currentQuantity | INT | Current stock on hand; editable directly by admin |
| quota | INT | Target stock level; drives the needs calculation |
| active | BOOLEAN | Soft-delete flag |

*Needs calculation: needQuantity = MAX(0, quota − currentQuantity). An item appears on the public needs page if needQuantity **>** 0.*

### **Pledge**

| **Field** | **Type** | **Notes** |
| --- | --- | --- |
| id | INT PK | Auto-generated |
| item | FK → Item | Many-to-one |
| donorName | VARCHAR(150) | Donor's name as entered — not tied to an account |
| donorContact | VARCHAR(200) | Phone or email — stored as free text |
| quantity | INT | How many units pledged; must be > 0 |
| status | ENUM | OPEN, FULFILLED, EXPIRED, CANCELLED |
| createdAt | DATETIME | Timestamp when pledge was submitted |
| expiresAt | DATETIME | Set automatically to createdAt + 7 days |
| fulfilledAt | DATETIME | Set when admin marks pledge fulfilled; null otherwise |

### **User**

Matches the Spring Security `JdbcDaoImpl` schema. `displayName` is an app-specific extension column.

| **Field** | **Type** | **Notes** |
| --- | --- | --- |
| username | VARCHAR(50) PK | Natural primary key; unique login name |
| password | VARCHAR(500) | BCrypt hashed — never stored in plain text |
| enabled | BOOLEAN | `false` prevents login |
| displayName | VARCHAR(150) | App-specific extension; shown in admin UI (e.g., "Jane Smith") |

### **Authorities**

| **Field** | **Type** | **Notes** |
| --- | --- | --- |
| username | VARCHAR(50) | FK → users(username) |
| authority | VARCHAR(50) | e.g., `ROLE_ADMIN` |

Unique constraint on (username, authority). All admin accounts are granted `ROLE_ADMIN`. Spring Security's `JdbcDaoImpl` reads this table directly with no custom configuration.

## **4.3 Pledge Lifecycle**

Pledges follow this state machine:

- OPEN — Pledge submitted by donor. Counts toward pledgedQuantity shown on the public page.

- FULFILLED — Admin has marked the items as received and inventory has been updated.

- EXPIRED — A scheduled job runs nightly and transitions any OPEN pledge past its expiresAt date to EXPIRED. Expired pledges no longer count toward pledgedQuantity.

- CANCELLED — Admin cancels a pledge (e.g., donor called to say they cannot bring the item).

*The public needs page shows three numbers per item: need (quota minus current stock), pledged (sum of OPEN pledge quantities), and remaining need (need minus pledged). This gives donors visibility without falsely reducing the stated need before items actually arrive.*

# **5. Architecture**

## **5.1 Technology Stack**

| **Layer** | **Technology Choice** |
| --- | --- |
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.0.6 |
| Web / UI | Thymeleaf with Bootstrap 5 (server-rendered, mobile-responsive) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8 on AWS RDS (db.t3.micro, free tier eligible) |
| Auth | Spring Security — form login with BCrypt password hashing |
| DB Migrations | Flyway — schema versioned in source control |
| Build | Maven |
| Hosting | AWS Elastic Beanstalk (Java SE platform, single instance) |
| Scheduling | Spring @Scheduled — nightly pledge expiration job |

## **5.2 Application Layers**

- Controller layer — Thymeleaf-backed @Controller classes for page routing and form handling

- Service layer — Business logic (needs calculation, pledge expiration, inventory updates)

- Repository layer — Spring Data JPA repositories for each entity

- Domain layer — JPA entity classes (Category, Item, Pledge, User, Authorities)

## **5.3 AWS Infrastructure**

- Elastic Beanstalk environment using the Java SE platform — accepts the packaged JAR directly

- RDS MySQL instance in the same VPC; Elastic Beanstalk environment variables supply the JDBC URL, username, and password at runtime

- A single t3.micro EC2 instance and db.t3.micro RDS instance keep the deployment within free tier limits for the first 12 months

- HTTPS via Elastic Beanstalk's built-in load balancer with an ACM certificate (optional but recommended)

## **5.4 Security**

- All /admin/** routes require an authenticated session (Spring Security)

- Public routes (/  and /pledge/**) are fully unauthenticated

- Passwords stored as BCrypt hashes — never in plain text

- CSRF protection enabled for all form submissions (Spring Security default)

- Database credentials are injected via Elastic Beanstalk environment variables — never hardcoded in source

# **6. Screens ****&**** User Flows**

## **6.1 Public Side**

### **Needs Page  (/)**

The home page, accessible without login. Shows all active items grouped by category where need > 0.

For each item the page displays:

- Item name and unit label

- Need quantity (quota minus current stock)

- Already pledged quantity (sum of OPEN pledges)

- Remaining need (need minus pledged) — the call-to-action number

- A "Pledge to Donate" button that opens the pledge form

Items where the remaining need is zero are shown with a "Fully pledged — thank you!" indicator but remain visible. Items where currentQuantity >= quota do not appear at all.

### **Pledge Form  (/pledge/{itemId})**

A simple mobile-friendly form. Fields:

- Your name (required)

- Phone number or email (required — at least one)

- Quantity you can bring (required, numeric, defaults to remaining need)

On submit: pledge is saved with status OPEN and expiresAt = now + 7 days. A confirmation summary is shown on the next page (no email/SMS sent). Donor is returned to the needs page after viewing the confirmation.

## **6.2 Admin Side**

All admin pages are under /admin/ and require a logged-in session.

### **Dashboard  (/admin/dashboard)**

Landing page after login. At-a-glance health metrics:

- Total items tracked

- Items below quota (count and percentage)

- Open pledges count and total pledged units

- Expired pledges awaiting review

- A category-level summary table: quota total vs. current stock per category

### **Inventory  (/admin/inventory)**

Table of all items showing category, name, unit, current quantity, quota, need, and open pledge total. Admins can:

- Edit an item's current quantity directly (inline or modal)

- Edit an item's quota

- Add a new item to any category

- Deactivate (soft-delete) an item

### **Categories  (/admin/categories)**

Manage the top-level categories:

- Add a new category with a name and display order

- Rename an existing category

- Reorder categories (controls the sequence on the public page)

- Deactivate a category (hides it and all its items from the public page)

### **Pledges  (/admin/pledges)**

A filterable list of all pledges. Columns: donor name, contact, item, quantity, status, created, expires. Filters: status, category, item. Admins can:

- Mark a pledge as FULFILLED — this triggers an inventory update: currentQuantity += pledge.quantity

- Mark a pledge as CANCELLED

- View pledge history per item

### **Admin Users  (/admin/users)**

Manage staff accounts:

- Create a new admin account (username + display name + temporary password)

- Deactivate an account

- Reset a password

# **7. Key Business Logic**

## **7.1 Needs Calculation**

Computed at query time — not stored as a column. For a given item:

needQty       = MAX(0, item.quota − item.currentQuantity)

pledgedQty    = SUM(quantity) WHERE item = ? AND status = OPEN

remainingNeed = MAX(0, needQty − pledgedQty)

The public page only shows items where needQty > 0. remainingNeed can reach zero (fully pledged) but pledging is still allowed in case a donor does not follow through.

## **7.2 Pledge Expiration Job**

A Spring @Scheduled task runs nightly at 2:00 AM. It finds all pledges where status = OPEN and expiresAt < NOW() and transitions them to EXPIRED. Expired pledges are excluded from pledgedQty calculations. Admins can see expired pledges in the pledge list filtered by status.

## **7.3 Pledge Fulfillment**

When an admin marks a pledge FULFILLED:

- pledge.status is set to FULFILLED

- pledge.fulfilledAt is set to the current timestamp

- item.currentQuantity is incremented by pledge.quantity

- The needs page recalculates automatically on next load

This is the only automated way inventory increases. Admins may also edit currentQuantity directly (e.g., to correct an error or record a donation that arrived without an associated pledge).

# **8. Internal Route Reference**

All routes serve Thymeleaf-rendered HTML. There are no public REST endpoints. The following is a reference for planning controller classes.

| **Method** | **Route** | **Description** |
| --- | --- | --- |
| GET | / | Public needs page |
| GET | /pledge/{itemId} | Pledge form for a specific item |
| POST | /pledge/{itemId} | Submit a pledge |
| GET | /pledge/confirmation/{pledgeId} | On-screen confirmation after pledging |
| GET | /admin/login | Admin login page |
| GET | /admin/dashboard | Admin dashboard |
| GET | /admin/inventory | Inventory list |
| POST | /admin/inventory/{itemId}/quantity | Update current quantity |
| POST | /admin/inventory/{itemId}/quota | Update quota |
| GET | /admin/items/new | New item form |
| POST | /admin/items | Create item |
| POST | /admin/items/{itemId}/deactivate | Soft-delete item |
| GET | /admin/categories | Category management page |
| POST | /admin/categories | Create category |
| POST | /admin/categories/{id} | Update category (name / order) |
| GET | /admin/pledges | Pledge list (filterable) |
| POST | /admin/pledges/{id}/fulfill | Mark pledge fulfilled + update inventory |
| POST | /admin/pledges/{id}/cancel | Cancel a pledge |
| GET | /admin/users | Admin user list |
| POST | /admin/users | Create admin user |
| POST | /admin/users/{id}/deactivate | Deactivate admin user |

# **9. Deployment ****&**** Infrastructure**

## **9.1 AWS Services Used**

- Elastic Beanstalk — Java SE platform, single t3.micro instance, handles deployment and health monitoring

- RDS — MySQL 8, db.t3.micro, single-AZ (sufficient for this workload; free tier for 12 months)

- S3 — Elastic Beanstalk uses S3 internally for application versions

- IAM — service roles for Elastic Beanstalk to access RDS and S3

## **9.2 Environment Variables**

The following environment variables are set in the Elastic Beanstalk environment — never committed to source control:

| **Variable** | **Purpose** |
| --- | --- |
| SPRING_DATASOURCE_URL | JDBC connection string for RDS MySQL |
| SPRING_DATASOURCE_USERNAME | Database username |
| SPRING_DATASOURCE_PASSWORD | Database password |
| SPRING_PROFILES_ACTIVE | Set to "prod" to activate production config |
| SERVER_PORT | Set to 5000 (Elastic Beanstalk default proxy port) |

## **9.3 CI/CD Recommendation**

For v1, a simple manual deploy is acceptable: run mvn clean package and upload the JAR to Elastic Beanstalk via the AWS console or CLI. For future iterations, a GitHub Actions workflow can automate the build and deploy on push to main.

## **9.4 Database Migrations**

Flyway is included as a Spring Boot dependency. Migration scripts live in src/main/resources/db/migration and are applied automatically on startup. This ensures the schema is always in sync with the application version and provides an auditable history of every schema change.

# **10. Open Questions ****&**** Future Considerations**

## **10.1 Deferred to v2**

- Donor notifications — email or SMS confirmation when a pledge is submitted or about to expire

- Reporting exports — CSV or PDF reports for monthly totals

- Barcode scanning — allow volunteers to scan items during intake on a mobile device

- Distribution tracking — if the pantry later wants to record what goes out and to whom

- CI/CD pipeline — automated deployment via GitHub Actions

## **10.2 Questions to Revisit Before Development**

- Should donors be able to look up their own pledge by phone/email to cancel it, or is cancellation admin-only?

- Is 7 days the right expiration window, or should it vary (e.g., shorter before a weekly pantry distribution day)?

- Should the dashboard send any kind of alert (email to admins) when stock falls critically low? Even a simple weekly digest could be valuable.

- Is there a need for a pantry "schedule" or hours on the public page, or is it purely a needs/pledge page?

# **11. Glossary**

| **Term** | **Definition** |
| --- | --- |
| Quota | The target quantity the pantry wants to keep in stock for a given item |
| Need | The gap between quota and current stock (quota − currentQuantity) |
| Pledge | A donor's stated intention to bring in a quantity of a specific item |
| Remaining Need | Need minus total open pledges — the actionable ask shown to donors |
| Fulfilled | A pledge that has been marked as received and applied to inventory |
| Soft-delete | Marking a record inactive rather than removing it from the database |
| Elastic Beanstalk | AWS managed deployment platform used to host the Spring Boot application |
| Flyway | Database migration tool that applies versioned SQL scripts on startup |

	Confidential — Internal Use Only