# frontend-v3

Greenfield Angular 19 UI for Podcast-Server, served under `/v3`. Material 3 Expressive theme with cover-card-first layout. See `documentation/` and `/Users/kdavin/.claude/plans/eager-chasing-abelson.md` for the full plan.

## Requirements

- Node 20.x (provisioned by the Gradle `node` plugin — `./gradlew :frontend-v3:downloadDependencies`).

## Local development

```bash
# Install deps via Gradle (uses the pinned Node)
./gradlew :frontend-v3:downloadDependencies

# Start the dev server (proxies /api to http://localhost:8080)
cd frontend-v3 && npm start
# → http://localhost:4200/v3/

# Production build (output: frontend-v3/dist/)
./gradlew :frontend-v3:build

# Unit tests (Vitest)
./gradlew :frontend-v3:npm_run_test

# E2E (Playwright) — installs browsers on first run
cd frontend-v3 && npx playwright install chromium && npm run e2e
```

## Stack

- Angular 19 (standalone components, signals, new control flow `@if/@for/@switch`)
- `@angular/build` (esbuild)
- Angular Material 19 + CDK, Material 3 Expressive theme
- Vitest (unit) + Playwright (e2e)
- ESLint flat config + Prettier
