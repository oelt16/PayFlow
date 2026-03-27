# PayFlow

Payment processing platform (portfolio). See [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) for the full technical specification.

## Prerequisites

- **Java 21** — this repo includes [`.java-version`](.java-version) for [jenv](https://github.com/jenv/jenv): run `jenv local 21` in this directory if needed.
- **Node.js 20** — use [`.nvmrc`](.nvmrc): `nvm install` then `nvm use`.
- **Docker** — for Testcontainers and local Compose (start Docker Desktop before running integration tests).

## Backend

```bash
cd backend
./mvnw verify
```

## Frontend

```bash
cd frontend
npm ci
npm run lint
npm run test
npm run build
```

## Layout

- `backend/` — Maven multi-module: payment, merchant, webhook, notification services
- `frontend/` — React + Vite + TypeScript (Phase 6)
- `infra/` — Docker Compose and Kubernetes manifests
