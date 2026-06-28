# UCC Lost & Found Management System

> A JavaFX desktop application for managing lost and found items at the University of Cape Coast.

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![SQLite](https://img.shields.io/badge/Database-SQLite-003B57)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)

The Lost & Found Management System (LFMS) lets students and staff report items they have
lost or found, browse and search the catalogue, and claim found items. Administrators
review claims, manage users and reports, export data, and audit every action. An automated
**matching engine** suggests likely lost ↔ found pairings.

---

## Features

**Students / Staff**
- Register and log in (passwords hashed with BCrypt)
- Report **lost** and **found** items, with optional/required image upload
- Browse and search the full catalogue by keyword, type, category and location
- View detailed item pages and **claim** found items with proof of ownership
- Track personal lost items, found items and claims under **My Reports**
- See **suggested matches** generated automatically by the matching engine

**Administrators**
- Dashboard with key statistics and **live charts** (Lost vs Found pie chart, Claims-by-status bar chart)
- Review pending claims and **approve** or **reject** them (with a reason)
- Manage all reports: search, change status, delete (with reason), **export to CSV**
- Manage users: real-time search, activate/deactivate, delete, **export to CSV**
- Full **audit log** with keyword/date filtering and CSV export

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java (compiled to release 17) |
| UI | JavaFX 21 (FXML + CSS) |
| Database | SQLite via `org.xerial:sqlite-jdbc` |
| Password hashing | BCrypt (`org.mindrot:jbcrypt`) |
| Build tool | Apache Maven (Maven Wrapper bundled) |
| Architecture | Strict MVC — Controller → Service → DAO → Database |

---

## Prerequisites

- **JDK 17 or newer** (developed and verified on JDK 26)
- Apache Maven is **optional** — the project ships with the Maven Wrapper (`mvnw`)

Check your JDK:

```bash
java -version
```

---

## Getting Started (Installation & Running)

Using the bundled **Maven Wrapper** — no local Maven installation required:

```bash
git clone <repo-url>
cd lost-and-found

# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

Or, if you already have Maven installed:

```bash
mvn javafx:run
```

The wrapper downloads Maven, JavaFX and the other dependencies automatically on first run,
then launches the application at the Login screen.

> **Windows note:** ensure a JDK 17+ is installed and `JAVA_HOME` points to it
> (e.g. `C:\Program Files\Java\jdk-26.0.1`). The SQLite database and uploaded images are
> created automatically under `data/`.

> **Tip — instant demo:** a rich demo dataset is seeded automatically on first launch —
> 8 students, 33 lost/found items located at real University of Cape Coast landmarks
> (Sam Jonah Library, Casely-Hayford Hall, Science Lecture Theatre, etc.), each with a
> generated category image, several matching lost↔found pairs, and a pending claim. You can
> also re-trigger it with **“Load demo data”** on the Login screen.

---

## Default Admin Account

| Field | Value |
|-------|-------|
| Email | `admin@gmail.com` |
| Password | `12345678` |

This account is seeded automatically on first launch.
**Note:** change the password after first login in a production setting.

**Demo accounts:** The seeded students all use the password `Password1`. For example, `joel.amuni@stu.ucc.edu.gh` or `PS/CSC/23/0043` with `Password1`.

---

## Screenshots

| Login | Admin Dashboard |
|-------|-----------------|
| ![Login](docs/screenshots/login.png) | ![Admin Dashboard](docs/screenshots/admin-dashboard.png) |

| Browse Items |
|--------------|
| ![Browse](docs/screenshots/browse.png) |

---

## Project Structure (brief)

```
lost-and-found/
├── pom.xml                     Maven build configuration
├── mvnw, mvnw.cmd, .mvn/       Maven Wrapper (run without installing Maven)
├── README.md
├── data/                       SQLite database + uploaded images (created at runtime)
├── docs/
│   ├── diagrams/               UML: use-case, class, ER, architecture (PlantUML + PNG)
│   └── screenshots/            README screenshots
├── src/
│   ├── main/java/com/lfms/
│   │   ├── Main.java / App.java     Launcher + JavaFX application
│   │   ├── database/                DatabaseManager (schema + admin seed)
│   │   ├── model/                   User, Item, Claim, Match, AuditLog
│   │   ├── dao/                      Data-access objects (JDBC only here)
│   │   ├── service/                 Business logic (Auth, Item, Claim, Matching, …)
│   │   ├── controller/              JavaFX controllers (+ admin/)
│   │   └── util/                     Session, navigation, hashing, images, CSV, …
│   └── main/resources/com/lfms/
│       ├── fxml/                    14 screens (+ admin/) + shared sidebars
│       ├── css/main.css             Application stylesheet
│       └── images/placeholder.png
└── src/test/test-cases.md           25 functional test cases
```

---

## Architecture

The application follows a strict layered MVC design:

```
FXML + CSS  →  Controllers  →  Services  →  DAOs  →  DatabaseManager  →  SQLite
```

- **Controllers** never touch JDBC; they call **Services**.
- **Services** hold the business logic and call **DAOs**.
- **DAOs** are the only classes that use JDBC `Connection`/`PreparedStatement`.
- **Utilities** (session, navigation, hashing, images, validation, CSV, dialogs) support all layers.

![Architecture](docs/diagrams/architecture.png)

### Use Case Diagram

![Use case diagram](docs/diagrams/use-case.png)

### Class Diagram

![Class diagram](docs/diagrams/class-diagram.png)

PlantUML sources for all diagrams live in [`docs/diagrams/`](docs/diagrams).

---

## Database Schema

Five tables created automatically with `CREATE TABLE IF NOT EXISTS`:
`users`, `items`, `claims`, `matches`, `audit_log`.

![Entity relationship diagram](docs/diagrams/er-diagram.png)

---

## Testing

Twenty-five manual functional test cases covering every module are documented in
[`src/test/test-cases.md`](src/test/test-cases.md).

---

## Team

| Name | Index / ID |
|------|------------|
| Sherif Haruna | PS/CSC/23/0131 |
| Yakubu Abdulai | PS/CSC/23/0050 |
| Rita Tsotsoo Tetteh | PS/CSC/23/0073 |
| Egmine James | PS/CSC/23/0163 |
| Joel Anarba Amuni | PS/CSC/23/0043 |
| Akudugu Abdul-Latif | PS/CSC/23/0012 |
| Noamesi Enyonam Elena | PS/CSC/23/0124 |
| Yakubu Saeed | PS/CSC/23/0100 |
| Kukekpor Bernice | PS/CSC/23/0060 |
| Theophilus Botchway | PS/CSC/23/0064 |

> Developed for **INNF101 — Introduction to Computing**.

---

## License

Released for academic use under the MIT License. See course submission guidelines for details.
