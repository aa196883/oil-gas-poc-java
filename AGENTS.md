# Repository Guidelines

## Project Structure & Module Organization

The Java 21 Spring Boot backend lives in `src/main/java/com/semsoft/poc`; import code is under `importdata/`. Configuration and Flyway migrations are in `src/main/resources`. Backend tests mirror this structure under `src/test/java`.

The Vue 3/Vite client is in `frontend/`, with code in `src/` and static files in `public/`. Sample imports are in `data/`; `compose.yaml` provides PostgreSQL.

## Build, Test, and Development Commands

- `docker compose up -d postgres`: start PostgreSQL 17 on port 5432.
- `mvn spring-boot:run`: run the API.
- `mvn test`: run JUnit 5 tests; Testcontainers tests require Docker.
- `mvn package`: test and build the backend JAR in `target/`.
- `cd frontend && npm ci`: install locked dependencies.
- `cd frontend && npm run dev`: start the Vite development server.
- `cd frontend && npm run build`: build the production bundle.
- `cd frontend && npm run lint`: run Oxlint and ESLint; use `npm run format` for Prettier.
- `cd frontend && npm run screenshot`: launch a temporary preview with mocked API data and save a UI screenshot to `artifacts/frontend.png`.
- Use the screenshot command to inspect rendering and exercise representative values and UI controls after frontend changes.

## Coding Style & Naming Conventions

Use four-space Java indentation, PascalCase classes, camelCase members, and constructor injection. This is a POC: keep the backend compact and follow its existing lean architecture. Use records only when valuable. Avoid unnecessary abstractions, interfaces, factories, boilerplate, and scattered files. Keep controllers thin, logic in services, and SQL in repositories. Name migrations `V<number>__<description>.sql`.

Frontend code follows EditorConfig, ESLint, Oxlint, and Prettier. Use Vue Composition API, camelCase identifiers, and PascalCase component filenames.

## Change Scope & Workflow

Make the smallest targeted change that satisfies the requirement. Avoid unrelated cleanup. Divide large requests into small, independently verifiable tasks before implementation.

## Testing Guidelines

Keep tests minimal, readable, and targeted at specific non-trivial behavior; broad coverage is not a goal. Name tests `*Test.java` with methods such as `shouldRejectInvalidDates`. Use Mockito for isolated logic and Testcontainers only for relevant PostgreSQL behavior. API endpoints are important contracts: keep them RESTful and add focused tests for changed behavior, validation, status codes, and errors.

## Commit & Pull Request Guidelines

Recent commits use short, topic-focused messages, often in French. Keep each commit scoped to one change. Pull requests should explain the change, list verification commands, link relevant issues, and include screenshots for UI work. Highlight schema, configuration, or API contract changes.

## Security & Configuration

Credentials in `compose.yaml` and `application.yaml` are development-only. Do not commit production secrets; supply sensitive settings through environment-specific configuration.
