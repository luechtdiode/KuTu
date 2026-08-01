# KuTu — Agent Guide

Wettkampf-App für Kunst- und Geräteturnen. Desktop (ScalaFX) + Web/Mobile (Angular/Ionic/Capacitor).

## Build Commands

### Backend (sbt, Scala 3.8.4)
```bash
sbt compile          # compile all
sbt test             # run all tests (forked JVM, sequential)
sbt "testOnly ch.seidel.kutu.domain.WettkampfSpec"  # single test class
```

### Frontend (Angular 22, Ionic 8)
```bash
cd newclient/kutu-app
npx ng build         # production build, always run after frontend changes to verify
```
No lint or typecheck commands are configured beyond what `ng build` catches.

## Architecture

### Backend (`src/main/scala/ch/seidel/kutu/`)
- **`KuTuApp.scala`** — Desktop entry point (ScalaFX)
- **`KuTuServer.scala`** — Pekko HTTP server
- **`domain/`** — Core domain: `DurchgangService`, `RiegenService`, `DurchgangManager`, data model (`package.scala`)
- **`http/`** — REST routes: `WettkampfRoutes`, `RegistrationRoutes`, `JsonSupport` (spray-json formats)
- **`squad/`** — Riegen/Durchgang generation: `DurchgangBuilder`, `DurchgangGrouper`, `RiegenBuilder`
- **`view/`** — JavaFX UI components (`RiegenTab`, `DurchgangEditor`)
- **`data/`** — Import/export, data exchange
- **`calc/`** — Score calculation

Key pattern: Backend operations (e.g. `DurchgangManager`) follow load-all → transform-in-memory → write-all-back. This is shared between the JavaFX client and REST endpoints.

### Frontend (`newclient/kutu-app/src/app/`)
- **`services/admin-backend.service.ts`** — All API calls (uses `x-access-token` header)
- **`backend-types.ts`** — TypeScript interfaces matching backend case classes
- **`riege-einteilung/`** — Main page for Riegen/Durchgang assignment (drag-and-drop, multi-select)
- **`competitions/`** — Competition management
- **`editors/`** — Modal editor components

### Database
- SQLite (primary), PostgreSQL supported
- Schema managed via Slick migrations in `RiegenService`

## Gotchas

- **TypeScript `flatMap`**: The TS target (`ES2022` with `lib: es2018`) does not support `Array.flatMap`. Use `.reduce()` instead.
- **`ion-button` in `<td>`**: Ionic buttons don't render inside native HTML `<td>`. Use `<span>` with click handlers for inline table actions.
- **Scala 3 tuple destructuring in lambdas**: Requires `case` keyword: `.foldLeft(...) { case (acc, (a, b)) => ... }` not `{ (acc, (a, b)) => ... }`.
- **`private[kutu]` visibility**: Use this to share utilities between `domain/` and `squad/` packages (both under `ch.seidel.kutu`).
- **JSON field naming**: Backend uses `Option[Set[String]]` for collection fields. Frontend sends `string[]`. The JSON key name stays singular (e.g. `filterDurchgang`) even though it's now an array.
- **Tests fork the JVM**: `Test / fork := true` with `parallelExecution := false`. Tests require `--add-opens` flags (already configured in `build.sbt`).

## Commit Conventions (from CONTRIBUTING.md)
- Prefix with action: `add`, `fix`, `clean code`, `refactoring`
- One feature per commit, atomic commits
- Do not commit secrets
- Rebase on origin/master before PR
