# Phase 7: Infrastructure and polish

This phase matches [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) section 11 (Phase 7) and sections 10.1–10.3: **Docker Compose**, **Kubernetes manifests**, **GitHub Actions CI/CD with container images**, and **README with an architecture diagram** (see [README.md](README.md)).

---

## 1. Deliverables

### 1.1 Dockerfiles

| Location | Purpose |
|----------|---------|
| [backend/payment-service/Dockerfile](backend/payment-service/Dockerfile) | Multi-stage Temurin 21 build + JRE runtime, port 8081 |
| [backend/merchant-service/Dockerfile](backend/merchant-service/Dockerfile) | Same pattern, port 8082 |
| [backend/webhook-service/Dockerfile](backend/webhook-service/Dockerfile) | Same pattern, port 8083 |
| [backend/notification-service/Dockerfile](backend/notification-service/Dockerfile) | Same pattern, port 8084 |
| [frontend/Dockerfile](frontend/Dockerfile) | Node 20 build + `nginx:alpine`, port 3000 |
| [frontend/nginx.conf](frontend/nginx.conf) | SPA `try_files` + `/api` proxy to payment, merchant, webhook services (same rules as [frontend/vite.config.ts](frontend/vite.config.ts)) |

Build context for Java images is **`backend/`** (parent POM required). Maven runs with **`-Dmaven.test.skip=true`** inside the image build for a faster layer.

### 1.2 `.dockerignore`

- [backend/.dockerignore](backend/.dockerignore) — excludes `target/`, `.git`, IDE files
- [frontend/.dockerignore](frontend/.dockerignore) — excludes `node_modules`, `dist`, env files

### 1.3 Docker Compose

[infra/docker-compose.yml](infra/docker-compose.yml) starts:

- **postgres:16-alpine** — database `payflow`, user from env (default `payflow` / `payflow`)
- **apache/kafka:3.9.2** — official ASF image, KRaft single broker (same family as Testcontainers `apache/kafka`), `kafka:9092` inside the network
- **provectuslabs/kafka-ui** — UI on host port **8080** (spec)
- **payment-service**, **merchant-service**, **webhook-service**, **notification-service** — built from Dockerfiles
- **frontend** — nginx on host port **3000**

Spring overrides use Docker DNS names (`postgres`, `kafka`, `webhook-service`) and **`PAYFLOW_WEBHOOK_DISPATCH_BASE_URL`** for the notification consumer.

Optional environment variables are documented in [.env.example](.env.example).

**Run from repo root:**

```bash
docker compose -f infra/docker-compose.yml up --build
```

If you previously ran Compose with another Kafka image, drop the broker volume once so KRaft storage matches the new layout, for example `docker volume rm payflow_kafka_data` (only that volume), or `docker compose -f infra/docker-compose.yml down -v` if you accept losing Postgres data too.

### 1.4 Kubernetes

Manifests live under [infra/k8s/](infra/k8s/):

| Area | Files |
|------|--------|
| Namespace | [namespace.yml](infra/k8s/namespace.yml) |
| Shared secret | [secrets.yml](infra/k8s/secrets.yml) — Postgres user, password, database name |
| PostgreSQL | [postgres/](infra/k8s/postgres/) — PVC, Deployment, Service, ConfigMap placeholder |
| Kafka | [kafka/](infra/k8s/kafka/) — Deployment + Service (emptyDir volume; use a PVC for durable clusters) |
| Apps | [payment-service/](infra/k8s/payment-service/), [merchant-service/](infra/k8s/merchant-service/), [webhook-service/](infra/k8s/webhook-service/), [notification-service/](infra/k8s/notification-service/), [frontend/](infra/k8s/frontend/) |
| Scaling | [payment-service/hpa.yml](infra/k8s/payment-service/hpa.yml) — CPU 60%, replicas 2–10 |
| Ingress | [ingress.yml](infra/k8s/ingress.yml) — `payflow.local` → frontend Service only (nginx in the pod still proxies `/api`) |

**Image names:** Deployments use placeholders `ghcr.io/myorg/payflow/<service>:latest`. Replace **`myorg/payflow`** with your **`github.repository`** in lowercase (same string CI uses when pushing to GHCR).

**Apply (example):**

```bash
kubectl apply -f infra/k8s/namespace.yml
kubectl apply -f infra/k8s/secrets.yml
kubectl apply -f infra/k8s/postgres/
kubectl apply -f infra/k8s/kafka/
kubectl apply -f infra/k8s/payment-service/
kubectl apply -f infra/k8s/merchant-service/
kubectl apply -f infra/k8s/webhook-service/
kubectl apply -f infra/k8s/notification-service/
kubectl apply -f infra/k8s/frontend/
kubectl apply -f infra/k8s/ingress.yml
```

You need an **NGINX Ingress Controller** (for example ingress-nginx) and a DNS or `/etc/hosts` entry for **`payflow.local`** (or edit the Ingress host).

For **private GHCR** images, create a pull secret and set `imagePullSecrets` on the pod templates.

### 1.5 GitHub Actions

- [.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml) — `mvnw verify` on every trigger; on **push to `main`/`master`**, after verify, builds and pushes **four** images to GHCR:  
  `ghcr.io/<lowercase github.repository>/<payment-service|merchant-service|webhook-service|notification-service>:latest` and `:sha-<short>`
- [.github/workflows/frontend-ci.yml](.github/workflows/frontend-ci.yml) — lint, test, build; on push to **main**/**master**, pushes **`ghcr.io/<lowercase github.repository>/frontend:latest`** (and `sha-` tag)

Workflows grant **`packages: write`** for `GITHUB_TOKEN`. The first push may require accepting the container package visibility in the GitHub UI.

### 1.6 README

[README.md](README.md) includes the Mermaid diagram, Compose quick start, service port table, layout tree, and links to all phase documents.

---

## 2. API keys in containers

The same cross-service key story as [PHASE6_REACT_FRONTEND.md](PHASE6_REACT_FRONTEND.md) applies: **merchant-service** hashes keys in Postgres; **payment-service** and **webhook-service** still read **`payflow.security.api-keys`** from YAML unless you add env-based config later. For a smooth Compose demo, use the dev key already in those `application.yml` files or align static entries after registering a merchant.

---

## 3. Related links

- [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) — sections 8, 10, 11
- [PHASE6_REACT_FRONTEND.md](PHASE6_REACT_FRONTEND.md) — dashboard and `/api` contract
