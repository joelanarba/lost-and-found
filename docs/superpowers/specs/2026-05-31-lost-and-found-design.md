# Lost & Found Management System — Design Document

**Date:** 2026-05-31
**Course:** Introduction to Computing (INNF101) — end-of-semester submission
**Goal:** Highest possible grade. Priority order: stability → clean UI/UX → complete functionality → proper MVC → professional documentation.

## 1. Overview

A JavaFX desktop application that lets university students/staff report lost and found
items, browse/search the catalogue, claim found items, and lets administrators review
claims, manage users and reports, export data, and audit all activity. An automated
matching engine suggests likely lost↔found pairs.

## 2. Technology

| Concern        | Choice                                            |
|----------------|---------------------------------------------------|
| Language       | Java (compiled to `release 17` bytecode)          |
| Runtime JDK    | Oracle JDK 26.0.1 (installed on build machine)    |
| UI             | JavaFX 21.0.2 (LTS) — FXML + CSS                  |
| Database       | SQLite via `org.xerial:sqlite-jdbc:3.45.1.0`      |
| Password hash  | BCrypt (`org.mindrot:jbcrypt:0.4`)                |
| Build          | Maven 3.9.16 + `javafx-maven-plugin:0.0.8`        |

## 3. Architecture — strict MVC

```
FXML + CSS  →  Controllers  →  Services  →  DAOs  →  DatabaseManager  →  SQLite
                                  ▲
                       Utilities (Session, Navigation, Hash, Image, Validation, CSV)
```

- **Controllers** never touch JDBC or `Connection`; they call **Services**.
- **Services** hold business logic and call **DAOs**.
- **DAOs** are the only layer that uses `Connection`/`PreparedStatement`.
- **Models** are plain POJOs passed between layers.
- **Utilities** support all layers horizontally.

## 4. Data model (SQLite)

Five tables, all created with `CREATE TABLE IF NOT EXISTS` in `DatabaseManager.initialize()`
on every startup, using `PreparedStatement` only:

- `users(user_id, name, index_no UNIQUE, email UNIQUE, password_hash, phone, role, is_active, created_at)`
- `items(item_id, user_id FK, type, name, category, description, location, image_path, date_reported, status, created_at)`
- `claims(claim_id, item_id FK, claimant_id FK, features_desc, proof_desc, status, admin_note, created_at)`
- `matches(match_id, lost_item_id FK, found_item_id FK, score, created_at)`
- `audit_log(log_id, actor_id, action, target_type, target_id, note, timestamp)`

A default admin (`admin@lfms.edu` / `Admin@1234`, BCrypt-hashed) is seeded if no ADMIN exists.

## 5. Matching engine scoring

For a new item, scan all OPEN items of the opposite type and score each candidate:

- `+3` exact category match
- `+2` per shared keyword in item NAME (stopwords removed)
- `+1` per shared keyword in DESCRIPTION
- `+2` location strings share a non-stopword token
- `+1` `dateReported` values within 7 days

Keep score `>= 4`. Confidence: `>=8` HIGH, `5–7` MEDIUM, `4` LOW. Persist new pairs only.

## 6. Screens (14)

Student: Login, Register, Dashboard, ReportLost, ReportFound, Browse, ItemDetail,
ClaimForm, MyReports. Admin: AdminDashboard (PieChart + BarChart on real data),
AdminClaims, AdminReports, AdminUsers, AdminAuditLog. Consistent left sidebar on all
authenticated screens; colour scheme primary `#1F4E79`, accent `#2E75B6`.

## 7. Environment-driven decisions (not in original spec)

1. **JDK 26 vs "Java 17+":** Build targets `release 17` bytecode and JavaFX 21.0.2 as
   specified; verified to launch on JDK 26. Fallback if a runtime incompatibility appears:
   bump JavaFX to a newer build and re-verify.
2. **`pom.xml`:** use `<maven.compiler.release>17</maven.compiler.release>` and pin
   `maven-compiler-plugin:3.13.0` for clean behaviour on JDK 26.
3. **Launcher pattern:** `com.lfms.Main` (entry point, does not extend `Application`)
   delegates to `Application.launch(App.class)` — avoids the "JavaFX runtime components
   are missing" error and works via both `mvn javafx:run` and a packaged jar.
4. **Maven not on PATH:** Chocolatey installed Maven but did not wire PATH; build commands
   inject `JAVA_HOME` + Maven/JDK `bin` for the session.
5. **Git:** repo initialised (`main`) so the design doc and submission are versioned.

## 8. Verification

`mvn clean compile` must pass with zero errors. `mvn javafx:run` must launch to the Login
screen; evidence captured by screenshot where the environment allows, otherwise by
confirming DB init, admin seed, and FXML/scene load without exceptions.

## 9. Documentation deliverables

`docs/diagrams/*.puml` (use-case, class, ER, architecture), `src/test/test-cases.md`
(25 functional cases), and a professional `README.md`.
