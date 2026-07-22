# Primer — backend

Spring Boot backend for Primer, a guided research-paper reading assistant.

This is an empty skeleton. No Java code, no database, no external services — just the folder
structure and a minimal Maven build.

---

## Structure

```
primer-backend/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/dev/primer/
    │   │   ├── config/        @Configuration classes and settings objects
    │   │   ├── controller/    REST endpoints
    │   │   ├── service/       business logic
    │   │   ├── repository/    data access
    │   │   ├── entity/        persisted models
    │   │   ├── dto/
    │   │   │   ├── request/   what comes IN over HTTP
    │   │   │   └── response/  what goes OUT over HTTP
    │   │   ├── client/        calls to outside systems (arXiv, LLM providers, search)
    │   │   ├── exception/     custom exceptions + the global error handler
    │   │   └── util/          small shared helpers
    │   └── resources/
    │       └── application.yml
    └── test/java/dev/primer/
        ├── controller/
        └── service/
```

The empty folders are tracked with `.gitkeep` files. Delete those as you add real classes.

---

## Before it will run

You need a main class. Create `src/main/java/dev/primer/PrimerApplication.java`:

```java
package dev.primer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrimerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrimerApplication.class, args);
    }
}
```

Then:

```bash
mvn spring-boot:run     # http://localhost:8080
```

---

## What's deliberately not here

`pom.xml` has exactly two dependencies: `spring-boot-starter-web` and the test starter. Everything
else is a decision you haven't made yet. When you're ready:

| You want | Add |
|---|---|
| Database + JPA | `spring-boot-starter-data-jpa` + a driver (`postgresql`, `h2`, …) |
| Schema versioning | `flyway-core`, and a `src/main/resources/db/migration/` folder |
| `@Valid` on request DTOs | `spring-boot-starter-validation` |
| Less boilerplate | `lombok` |
| API docs at `/swagger-ui.html` | `springdoc-openapi-starter-webmvc-ui` |
| Login / auth | `spring-boot-starter-security` |
| Health endpoint for your host | `spring-boot-starter-actuator` |

If you skip JPA, `entity/` and `repository/` still make sense — rename `entity/` to `model/` if
your storage isn't relational.

---

## Conventions to hold to

- **Constructor injection**, not `@Autowired` on fields. Dependencies are `private final`.
- **Controllers stay thin** — read the request, call a service, return a DTO. No business rules.
- **Never return an entity from a controller.** Map it to a response DTO so you can change a
  column without breaking clients.
- **Config goes in `application.yml`**, read via `@ConfigurationProperties`, not hardcoded.
- **Secrets come from environment variables** with sensible local defaults, e.g.
  `${DB_URL:jdbc:postgresql://localhost:5432/primer}`. This is what makes deploying to Render,
  Railway or Fly a config change rather than a code change.
- When a folder passes ~15 classes, add subfolders by feature (`service/paper/`,
  `service/questionnaire/`) rather than letting it sprawl.

---

## Planned build order

1. Ingest an arXiv link → store the PDF → serve it back
2. Parse the PDF into text and image sections
3. Generate a questionnaire from the parsed paper
4. Turn the answers into a knowledge report and a reading path
5. Pull in external sources for the gaps
6. Reading session: annotations, notes, summaries, ask-AI
7. Next-paper recommendations
