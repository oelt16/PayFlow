# PayFlow — Payment Processing Platform
## v3.0 Cloud-Native & Reliability Specification

> **Build on top of:** v2.0 (Phases 1–15 complete — full DDD/TDD/Hexagonal backend, React frontend, Docker Compose, K8s manifests, GitHub Actions CI/CD)
>
> **Focus of v3.0:** Production-grade AWS infrastructure simulation, IaC, EKS-grade Kubernetes, OpenAPI documentation, microservices resilience, and security hardening — all running 100% locally via Floci.

---

## Table of Contents

1. [v3.0 Overview & Principles](#1-v30-overview--principles)
2. [Target Architecture](#2-target-architecture)
3. [New Technology Stack](#3-new-technology-stack)
4. [Phase 16 — OpenAPI 3.1 Documentation & Swagger UI](#4-phase-16--openapi-31-documentation--swagger-ui)
5. [Phase 17 — Infrastructure as Code with Terraform + Floci](#5-phase-17--infrastructure-as-code-with-terraform--floci)
6. [Phase 18 — Production-Grade Kubernetes (kind + Floci EKS)](#6-phase-18--production-grade-kubernetes-kind--floci-eks)
7. [Phase 19 — Microservices Resilience with Resilience4j](#7-phase-19--microservices-resilience-with-resilience4j)
8. [Phase 20 — Chaos Engineering](#8-phase-20--chaos-engineering)
9. [Phase 21 — Security Hardening](#9-phase-21--security-hardening)
10. [Updated Project Structure](#10-updated-project-structure)
11. [Suggested Implementation Order](#11-suggested-implementation-order)
12. [New Interview Talking Points](#12-new-interview-talking-points)

---

## 1. v3.0 Overview & Principles

v2.0 proved the application works. v3.0 proves the application is **operable**. The distinction matters in fintech interviews: a senior/staff engineer is expected to own the infrastructure, not just the code.

The guiding principles for v3.0:

**Simulate production locally, deploy identically to AWS.** Every infrastructure decision uses real AWS SDK calls against Floci (a free, open-source AWS emulator running on `localhost:4566`). The Terraform code that provisions your local environment is the same code, with a single `endpoint_url` override, that would provision real AWS. No stub, no mock, no shortcut.

**Secrets never touch source code or environment variables.** All credentials flow through AWS Secrets Manager (Floci-emulated), fetched at runtime by Spring Cloud AWS. `application.yml` contains no passwords.

**Every inter-service call is resilient.** Circuit breakers, retries, and bulkheads are applied to all synchronous service-to-service communication. The system degrades gracefully when a dependency is slow or unavailable.

**Infrastructure is code.** Drift between environments is impossible because there is no environment that wasn't created by Terraform. The K8s manifests from v2.0 are superseded by Helm charts that accept values per environment.

### What Floci Is

Floci is a fast, free, open-source AWS emulator. It runs as a single Docker container on port `4566` and emulates 51 AWS services including RDS (manages real PostgreSQL containers), MSK (manages real Redpanda/Kafka containers), Secrets Manager, KMS, IAM, ECR, EKS, API Gateway, CloudWatch, and ElastiCache. All services accept dummy credentials — no AWS account required.

```bash
# Floci global setup (add to .bashrc / .zshrc)
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
```

---

## 2. Target Architecture

### 2.1 v3.0 Architecture Diagram

```
+------------------------------------------------------------------+
|  Developer Machine                                               |
|                                                                  |
|  +---------------+    +--------------------------------------+   |
|  |  React UI     |    |  kind cluster (local K8s)            |   |
|  |  :3000        +---->                                      |   |
|  +---------------+    |  +----------+  +----------------+   |   |
|                        |  |  Ingress |  |  API Gateway   |   |   |
|                        |  |  nginx   |  |  (Floci :4566) |   |   |
|                        |  +----+-----+  +-------+--------+   |   |
|                        |       |                |             |   |
|                        |  +----v----------------v----------+  |   |
|                        |  | payment  merchant  webhook     |  |   |
|                        |  | :8081    :8082      :8083      |  |   |
|                        |  | notification :8084             |  |   |
|                        |  +---------------+----------------+  |   |
|                        +------------------+------------------+    |
|                                           |                       |
|  +----------------------------------------v-------------------+   |
|  |  Floci  (localhost:4566) - AWS emulation layer             |   |
|  |                                                             |   |
|  |  RDS (PostgreSQL :7001-7003)    MSK / Redpanda (:9092)     |   |
|  |  Secrets Manager                KMS (envelope encryption)  |   |
|  |  CloudWatch Metrics/Logs        ECR (image registry)       |   |
|  |  IAM / STS                      API Gateway v2             |   |
|  +-------------------------------------------------------------+   |
|                                                                  |
|  Terraform ---provisions---> Floci resources                    |
|  Helm      ---deploys------> kind cluster                       |
+------------------------------------------------------------------+
```

### 2.2 Key Architectural Shifts from v2.0

| Concern | v2.0 | v3.0 |
|---|---|---|
| Database | `postgres:16-alpine` in Compose | Floci RDS (PostgreSQL 16, wire-compatible, IAM auth capable) |
| Kafka | `apache/kafka` in Compose | Floci MSK (Redpanda, Kafka-compatible, managed lifecycle) |
| Secrets | Hardcoded in `application.yml` | Floci Secrets Manager, fetched at runtime by Spring Cloud AWS |
| Infra provisioning | Manual `docker compose up` | `terraform apply` against Floci |
| K8s | Raw manifests (`kubectl apply`) | Helm charts, `kind` cluster, metrics-server, HPA |
| Inter-service calls | No resilience | Resilience4j: circuit breaker, retry, bulkhead, time limiter |
| API docs | None | springdoc-openapi 3.1, Swagger UI per service, aggregated |
| Security | BCrypt API keys, nginx TLS | KMS envelope encryption, Secrets Manager rotation, IAM policies |

---

## 3. New Technology Stack

| Component | Tool | Version |
|---|---|---|
| AWS emulation | Floci | latest |
| Infrastructure as Code | Terraform + AWS provider | 1.9+ / 5.x |
| Local Kubernetes | kind (Kubernetes in Docker) | 0.23+ |
| K8s package manager | Helm | 3.15+ |
| K8s metrics | metrics-server | 0.7+ |
| API documentation | springdoc-openapi | 2.6+ |
| Swagger UI | springdoc-openapi-starter-webmvc-ui | 2.6+ |
| Resilience | Resilience4j | 2.2+ |
| Spring Cloud AWS | spring-cloud-aws-starter-secrets-manager | 3.2+ |
| Chaos Engineering | Chaos Monkey for Spring Boot | 3.1+ |
| Secret injection in K8s | init-container pattern (AWS CLI + Floci) | — |

---

## 4. Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

**Why first?** OpenAPI is the lowest-risk, highest-visibility change. It requires no infrastructure modifications, ships in one `pom.xml` dependency per service, and immediately makes your API demonstrable to any recruiter or engineer who opens a browser. It is also a prerequisite for the API Gateway integration in Phase 17.

**Pattern:** Auto-generated documentation via springdoc-openapi. Each service exposes its own OpenAPI spec at `/v3/api-docs` and an interactive Swagger UI at `/swagger-ui.html`. An aggregated spec is accessible from the frontend nginx.

### 4.1 Dependency (all three REST services)

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

### 4.2 application.yml configuration (each service)

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
  info:
    title: PayFlow Payment Service
    version: "2.0"
    description: >
      Simplified Stripe-like payment processing API.
      Create, capture, cancel, and refund payment intents.
    contact:
      name: PayFlow Engineering
      url: https://github.com/oelt16/PayFlow
  servers:
    - url: http://localhost:8081
      description: Local development
    - url: https://api.payflow.io/v1
      description: Production (AWS API Gateway)
```

### 4.3 Domain annotations

Annotate every controller and DTO to generate rich, accurate documentation:

```java
// PaymentController.java
@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payments", description = "Payment intent lifecycle: create, capture, cancel, refund")
public class PaymentController {

    @PostMapping
    @Operation(
        summary = "Create a payment intent",
        description = "Creates a PENDING payment. The client_secret is used to confirm from the frontend.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Payment created",
                content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key")
        }
    )
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestBody @Valid CreatePaymentRequest request) { ... }
}
```

Annotate every DTO with `@Schema`:

```java
public record CreatePaymentRequest(
    @Schema(description = "Amount in minor units (cents)", example = "10000", minimum = "1")
    @NotNull @Positive
    Integer amount,

    @Schema(description = "ISO 4217 currency code", example = "USD", allowableValues = {"USD","EUR","GBP"})
    @NotBlank @Size(min = 3, max = 3)
    String currency,

    @Schema(description = "Simulated card details — no real PANs processed")
    @Valid @NotNull
    CardDetailsRequest card
) {}
```

### 4.4 Security scheme declaration

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI payflowOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .name("BearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .description("API key issued by merchant-service. Format: Bearer sk_test_...")));
    }
}
```

### 4.5 Nginx aggregation

Expose all three service specs through the frontend nginx:

```nginx
# frontend/nginx.conf additions
location /api-docs/payments  { proxy_pass http://payment-service:8081/v3/api-docs; }
location /api-docs/merchants { proxy_pass http://merchant-service:8082/v3/api-docs; }
location /api-docs/webhooks  { proxy_pass http://webhook-service:8083/v3/api-docs; }
location /swagger-ui/        { proxy_pass http://payment-service:8081/swagger-ui/; }
```

### 4.6 TDD

- `PaymentControllerOpenApiTest`: assert `GET /v3/api-docs` returns 200 and JSON contains `paths./v1/payments.post` and `components.securitySchemes.BearerAuth`.
- `SchemaValidationTest`: assert all request/response DTOs have `@Schema` on every field (reflection-based scan).

---

## 5. Phase 17 — Infrastructure as Code with Terraform + Floci

**Why?** "How do you provision infrastructure?" is a senior-level screening question. Answering "I write Terraform" and opening a `.tf` file is incomparably stronger than "I edit YAML files manually." This phase also replaces all hardcoded passwords with Secrets Manager.

**Pattern:** Terraform AWS provider pointed at Floci's endpoint. The same `.tf` files provision your local environment (pointing at `localhost:4566`) and real AWS (removing the `endpoint_url` override). Infrastructure is split into modules: `security`, `data`, `messaging`, `registry`.

### 5.1 Install prerequisites

```bash
# Terraform
brew install terraform   # macOS
# or: https://developer.hashicorp.com/terraform/install

# AWS CLI
brew install awscli

# Verify Floci is running
docker compose -f infra/docker-compose.yml up -d floci
aws s3 ls --endpoint-url http://localhost:4566   # should return empty, not error
```

### 5.2 New infra layout

```
infra/
├── docker-compose.yml          # now includes floci service
├── terraform/
│   ├── main.tf                 # root module: providers, backend
│   ├── variables.tf
│   ├── outputs.tf
│   ├── terraform.tfvars.local  # local overrides (gitignored)
│   └── modules/
│       ├── security/           # KMS, Secrets Manager, IAM
│       ├── data/               # RDS instances (one per service)
│       ├── messaging/          # MSK cluster
│       └── registry/           # ECR repositories
```

### 5.3 Provider configuration

```hcl
# infra/terraform/main.tf
terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

provider "aws" {
  region                      = var.aws_region
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    rds            = "http://localhost:4566"
    kafka          = "http://localhost:4566"
    secretsmanager = "http://localhost:4566"
    kms            = "http://localhost:4566"
    ecr            = "http://localhost:4566"
    iam            = "http://localhost:4566"
    sts            = "http://localhost:4566"
  }
}

variable "aws_region"  { default = "us-east-1" }
variable "environment" { default = "local" }
variable "db_password" { sensitive = true }
```

### 5.4 docker-compose.yml — add Floci

```yaml
# infra/docker-compose.yml additions
services:
  floci:
    image: floci/floci:latest
    ports:
      - "4566:4566"
      - "7001-7003:7001-7003"     # RDS proxy ports (one per service)
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./floci-data:/app/data
    environment:
      FLOCI_STORAGE_MODE: hybrid
      FLOCI_SERVICES_RDS_PROXY_BASE_PORT: "7001"
      FLOCI_SERVICES_DOCKER_NETWORK: infra_default
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4566/_floci/health"]
      interval: 5s
      timeout: 3s
      retries: 10
```

### 5.5 Security module — KMS + Secrets Manager

```hcl
# infra/terraform/modules/security/main.tf

resource "aws_kms_key" "payflow_master" {
  description             = "PayFlow master encryption key"
  deletion_window_in_days = 7
  enable_key_rotation     = true
  tags = { Environment = var.environment }
}

resource "aws_kms_alias" "payflow_master" {
  name          = "alias/payflow/${var.environment}/master"
  target_key_id = aws_kms_key.payflow_master.key_id
}

resource "aws_secretsmanager_secret" "payment_db" {
  name       = "/payflow/${var.environment}/payment-service/db"
  kms_key_id = aws_kms_key.payflow_master.key_id
}

resource "aws_secretsmanager_secret_version" "payment_db" {
  secret_id     = aws_secretsmanager_secret.payment_db.id
  secret_string = jsonencode({
    url      = "jdbc:postgresql://localhost:${var.payment_db_port}/payments"
    username = "payment_svc"
    password = var.db_password
  })
}

# Repeat for merchant_db and webhook_db secrets
output "kms_key_arn"           { value = aws_kms_key.payflow_master.arn }
output "payment_db_secret_arn" { value = aws_secretsmanager_secret.payment_db.arn }
```

### 5.6 Data module — RDS instances

```hcl
# infra/terraform/modules/data/main.tf
# Floci RDS manages real PostgreSQL 16 containers, proxied on ports 7001-7003

resource "aws_db_instance" "payment" {
  identifier        = "payflow-${var.environment}-payment"
  engine            = "postgres"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "payments"
  username          = "payment_svc"
  password          = var.db_password
}

resource "aws_db_instance" "merchant" {
  identifier        = "payflow-${var.environment}-merchant"
  engine            = "postgres"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "merchants"
  username          = "merchant_svc"
  password          = var.db_password
}

resource "aws_db_instance" "webhook" {
  identifier        = "payflow-${var.environment}-webhook"
  engine            = "postgres"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "webhooks"
  username          = "webhook_svc"
  password          = var.db_password
}
```

### 5.7 Messaging module — MSK cluster

```hcl
# infra/terraform/modules/messaging/main.tf
# Floci MSK spawns a real Redpanda container (Kafka-compatible API)

resource "aws_msk_cluster" "payflow" {
  cluster_name           = "payflow-${var.environment}"
  kafka_version          = "3.6.1"
  number_of_broker_nodes = 1

  broker_node_group_info {
    instance_type  = "kafka.m5.large"
    client_subnets = ["subnet-00000001"]
    storage_info {
      ebs_storage_info { volume_size = 20 }
    }
  }
  tags = { Environment = var.environment }
}

output "bootstrap_brokers" { value = aws_msk_cluster.payflow.bootstrap_brokers }
```

### 5.8 ECR module

```hcl
# infra/terraform/modules/registry/main.tf
locals {
  services = ["payment-service","merchant-service","webhook-service","notification-service","frontend"]
}

resource "aws_ecr_repository" "payflow" {
  for_each             = toset(local.services)
  name                 = "payflow/${each.key}"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

output "registry_url" { value = "000000000000.dkr.ecr.us-east-1.localhost:5000" }
```

### 5.9 Spring Cloud AWS — Secrets Manager integration

```xml
<!-- backend/payment-service/pom.xml -->
<dependency>
  <groupId>io.awspring.cloud</groupId>
  <artifactId>spring-cloud-aws-starter-secrets-manager</artifactId>
  <version>3.2.0</version>
</dependency>
```

```yaml
# BEFORE (v2.0): passwords hardcoded
# spring.datasource.password: payflow

# AFTER (v3.0): passwords come from Secrets Manager at startup
spring:
  datasource:
    url: ${db.url}
    username: ${db.username}
    password: ${db.password}
  config:
    import: "aws-secretsmanager:/payflow/${ENVIRONMENT:local}/payment-service/db"
  cloud:
    aws:
      endpoint: http://localhost:4566
      region.static: us-east-1
      credentials:
        access-key: test
        secret-key: test
```

The `spring.config.import` line fetches the JSON secret at startup and binds `db.url`, `db.username`, and `db.password` from the JSON fields. Zero code changes in the rest of the application.

### 5.10 Full provisioning workflow

```bash
# 1. Start Floci
docker compose -f infra/docker-compose.yml up -d floci

# 2. Provision all AWS resources
cd infra/terraform
terraform init
terraform apply -var="db_password=localdevpassword" -auto-approve

# 3. Verify secrets
aws secretsmanager get-secret-value \
  --secret-id /payflow/local/payment-service/db \
  --endpoint-url http://localhost:4566

# 4. Verify RDS instances
aws rds describe-db-instances --endpoint-url http://localhost:4566 \
  --query 'DBInstances[*].[DBInstanceIdentifier,Endpoint.Port,DBInstanceStatus]' \
  --output table

# 5. Start services (they fetch credentials from Floci Secrets Manager on startup)
docker compose -f infra/docker-compose.yml up --build
```

### 5.11 CI validation (Terraform)

```yaml
# .github/workflows/infra-ci.yml
name: Infra CI
on: [pull_request]
jobs:
  terraform:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.9.0
      - run: terraform fmt -check -recursive
        working-directory: infra/terraform
      - run: terraform init -backend=false
        working-directory: infra/terraform
      - run: terraform validate
        working-directory: infra/terraform
```

---

## 6. Phase 18 — Production-Grade Kubernetes (kind + Floci EKS)

**Why?** The K8s manifests from v2.0 are functional but not production-grade. They lack liveness/readiness probes, security contexts, rolling update strategies, and are not parameterised across environments. This phase migrates from raw `kubectl apply` to Helm — how every real team manages K8s.

**Pattern:** `kind` for the local cluster (zero dependencies, pure Docker). Helm charts replace raw manifests. metrics-server enables HPA. All environment-specific values are in `values.local.yaml` — promoting to production is a values swap.

### 6.1 Install prerequisites

```bash
# kind
brew install kind   # macOS
# or: https://kind.sigs.k8s.io/docs/user/quick-start/#installation

# Helm
brew install helm
# or: https://helm.sh/docs/intro/install/

# Verify
kind version && helm version
```

### 6.2 kind cluster configuration

```yaml
# infra/kind-config.yml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: payflow
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
  - role: worker
```

```bash
# Create cluster and install dependencies
kind create cluster --config infra/kind-config.yml

# Ingress controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# metrics-server (required for HPA)
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
helm install metrics-server metrics-server/metrics-server \
  --namespace kube-system \
  --set args="{--kubelet-insecure-tls}"

# Load built images into kind (no registry required locally)
kind load docker-image payflow/payment-service:latest --name payflow
kind load docker-image payflow/merchant-service:latest --name payflow
kind load docker-image payflow/webhook-service:latest --name payflow
kind load docker-image payflow/notification-service:latest --name payflow
kind load docker-image payflow/frontend:latest --name payflow
```

### 6.3 Helm chart structure

```
infra/helm/
├── payflow/                        # umbrella chart
│   ├── Chart.yaml
│   ├── values.yaml                 # production defaults
│   ├── values.local.yaml           # kind overrides (gitignored for db_password)
│   └── charts/                     # sub-charts (one per service)
└── charts/
    ├── payment-service/
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── hpa.yaml
    │       ├── configmap.yaml
    │       └── serviceaccount.yaml
    ├── merchant-service/           # same structure
    ├── webhook-service/            # same structure
    ├── notification-service/       # same structure
    └── frontend/                   # same structure
```

### 6.4 Production-grade Deployment template

```yaml
# infra/helm/charts/payment-service/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}-payment-service
spec:
  replicas: {{ .Values.replicaCount }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0           # Zero-downtime deploys

  selector:
    matchLabels: { app: payment-service }

  template:
    metadata:
      labels:
        app: payment-service
        version: {{ .Values.image.tag }}
    spec:
      serviceAccountName: payment-service

      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000

      containers:
        - name: payment-service
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - containerPort: 8081

          # Allow up to 60s for JVM startup before liveness kicks in
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: 8081 }
            failureThreshold: 30
            periodSeconds: 2

          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8081 }
            periodSeconds: 10
            failureThreshold: 3
            timeoutSeconds: 5

          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8081 }
            periodSeconds: 5
            failureThreshold: 3
            timeoutSeconds: 3

          resources:
            requests: { cpu: 250m, memory: 512Mi }
            limits:   { cpu: 1000m, memory: 1Gi }

          env:
            - name: ENVIRONMENT
              value: {{ .Values.environment }}
            - name: SPRING_CLOUD_AWS_ENDPOINT
              value: {{ .Values.aws.endpoint }}

          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities: { drop: ["ALL"] }

          volumeMounts:
            - { name: tmp, mountPath: /tmp }

      volumes:
        - name: tmp
          emptyDir: {}
```

### 6.5 HPA configuration

```yaml
# infra/helm/charts/payment-service/templates/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ .Release.Name }}-payment-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ .Release.Name }}-payment-service
  minReplicas: {{ .Values.hpa.minReplicas }}
  maxReplicas: {{ .Values.hpa.maxReplicas }}
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 60 }
    - type: Resource
      resource:
        name: memory
        target: { type: Utilization, averageUtilization: 75 }
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 30
      policies:
        - { type: Pods, value: 2, periodSeconds: 60 }
    scaleDown:
      stabilizationWindowSeconds: 300   # 5-min cooldown
```

### 6.6 Local values override

```yaml
# infra/helm/payflow/values.local.yaml
image:
  repository: payflow/payment-service
  tag: latest
  pullPolicy: Never        # use kind-loaded images, skip registry

replicaCount: 1            # save memory locally
environment: local
springProfile: local

aws:
  endpoint: http://host.docker.internal:4566   # reach Floci from inside kind

hpa:
  minReplicas: 1
  maxReplicas: 3
```

### 6.7 Deploy commands

```bash
# First deploy
helm install payflow infra/helm/payflow \
  --namespace payflow --create-namespace \
  -f infra/helm/payflow/values.local.yaml

# After a code change
kind load docker-image payflow/payment-service:latest --name payflow
helm upgrade payflow infra/helm/payflow \
  --namespace payflow \
  -f infra/helm/payflow/values.local.yaml

# Inspect
kubectl get pods -n payflow -w
kubectl get hpa  -n payflow
kubectl logs -n payflow -l app=payment-service -f
```

### 6.8 Spring Boot Actuator for K8s probes

```yaml
# All services — application.yml
management:
  endpoint.health.probes.enabled: true
  health:
    livenessState.enabled: true
    readinessState.enabled: true
  endpoints.web.exposure.include: health,info,prometheus,metrics
```

---

## 7. Phase 19 — Microservices Resilience with Resilience4j

**Why?** The merchant-service API key cache (Phase 12) makes synchronous HTTP calls to merchant-service. Without a circuit breaker, a slow or unavailable merchant-service will exhaust payment-service's thread pool and cause cascading failure — the most common cause of production outages in microservice architectures.

**Pattern:** Circuit breaker + retry + bulkhead + time limiter applied to every synchronous inter-service call via Resilience4j. Fail-secure fallback strategy: if the circuit is open and the key is not cached, the request is rejected (denying a valid payment is safer than accepting an invalid one in a fintech context).

### 7.1 Dependencies

```xml
<!-- pom.xml (payment-service and webhook-service) -->
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version>2.2.0</version>
</dependency>
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-micrometer</artifactId>
  <version>2.2.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 7.2 Resilience4j configuration

```yaml
# payment-service/src/main/resources/application.yml
resilience4j:

  circuitbreaker:
    instances:
      merchant-key-validation:
        slidingWindowSize: 10
        failureRateThreshold: 50          # OPEN after 50% of 10 calls fail
        waitDurationInOpenState: 10s      # Try HALF_OPEN after 10s
        permittedNumberOfCallsInHalfOpenState: 3
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.payflow.payment.domain.exception.InvalidApiKeyException

  retry:
    instances:
      merchant-key-validation:
        maxAttempts: 3
        waitDuration: 200ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2   # 200ms -> 400ms -> 800ms
        retryExceptions:
          - java.io.IOException

  bulkhead:
    instances:
      merchant-key-validation:
        maxConcurrentCalls: 20
        maxWaitDuration: 100ms

  timelimiter:
    instances:
      merchant-key-validation:
        timeoutDuration: 2s
        cancelRunningFuture: true
```

### 7.3 Circuit-breaker-aware API key cache

```java
// MerchantApiKeyCache.java
@Component
@RequiredArgsConstructor
public class MerchantApiKeyCache {

    private final MerchantServiceClient merchantServiceClient;
    private final Cache<String, Optional<String>> localCache;   // Caffeine, 5-min TTL

    @CircuitBreaker(name = "merchant-key-validation", fallbackMethod = "validateFromCacheOnly")
    @Retry(name = "merchant-key-validation")
    @Bulkhead(name = "merchant-key-validation")
    @TimeLimiter(name = "merchant-key-validation")
    public CompletableFuture<Optional<String>> validateKey(String rawApiKey) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> cached = localCache.getIfPresent(rawApiKey);
            if (cached != null) return cached;

            Optional<String> result = merchantServiceClient.validateKey(rawApiKey);
            localCache.put(rawApiKey, result);
            return result;
        });
    }

    // Fallback: circuit OPEN — serve cache only
    // Fail-secure: uncached key during open circuit = reject (empty Optional)
    private CompletableFuture<Optional<String>> validateFromCacheOnly(
            String rawApiKey, CallNotPermittedException ex) {
        log.warn("Circuit OPEN for merchant-key-validation. Serving from cache only.");
        return CompletableFuture.completedFuture(
            Optional.ofNullable(localCache.getIfPresent(rawApiKey)).flatMap(o -> o)
        );
    }
}
```

### 7.4 Circuit breaker state in /actuator/health

```yaml
management.health.circuitbreakers.enabled: true
```

```json
{
  "circuitBreakers": {
    "merchant-key-validation": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failureRate": "0.0%",
        "bufferedCalls": 4
      }
    }
  }
}
```

### 7.5 Resilience4j Prometheus metrics (auto-published)

```
resilience4j_circuitbreaker_calls_total{name="merchant-key-validation",kind="successful"}
resilience4j_circuitbreaker_calls_total{name="merchant-key-validation",kind="not_permitted"}
resilience4j_circuitbreaker_state{name="merchant-key-validation",state="open"}
resilience4j_retry_calls_total{name="merchant-key-validation",kind="successful_with_retry"}
resilience4j_bulkhead_available_concurrent_calls{name="merchant-key-validation"}
```

Add a Grafana panel: "Circuit breaker state over time" and "Retry rate vs direct success rate."

### 7.6 TDD for resilience

```java
@SpringBootTest
class MerchantApiKeyCacheResilienceTest {

    @MockBean MerchantServiceClient merchantServiceClient;
    @Autowired MerchantApiKeyCache cache;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void circuitBreaker_opensAfterConsecutiveFailures() {
        given(merchantServiceClient.validateKey(any()))
            .willThrow(new IOException("Connection refused"));

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> cache.validateKey("key_abc").get())
                .hasCauseInstanceOf(IOException.class);
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("merchant-key-validation");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void fallback_servesFromCacheWhenCircuitIsOpen() {
        cache.seedForTest("sk_test_valid", "mer_123");
        circuitBreakerRegistry.circuitBreaker("merchant-key-validation")
            .transitionToOpenState();

        Optional<String> result = cache.validateKey("sk_test_valid").join();
        assertThat(result).contains("mer_123");
    }

    @Test
    void retry_exponentialBackoff_retriesThreeTimes() {
        AtomicInteger attempts = new AtomicInteger(0);
        given(merchantServiceClient.validateKey(any())).willAnswer(inv -> {
            if (attempts.incrementAndGet() < 3) throw new IOException("timeout");
            return Optional.of("mer_123");
        });

        assertThat(cache.validateKey("sk_test_retry").join()).contains("mer_123");
        assertThat(attempts.get()).isEqualTo(3);
    }
}
```

---

## 8. Phase 20 — Chaos Engineering

**Why?** Resilience4j proves the code handles failures in unit tests. Chaos Engineering proves the running system handles failures under real network conditions. The difference between "I implemented circuit breakers" and "I ran documented experiments to verify they work" is the difference between a mid-level and senior engineer in fintech interviews.

**Pattern:** Chaos Monkey for Spring Boot for application-layer fault injection; `kubectl delete pod` for infrastructure-layer failure; `docker compose stop` for dependency failure.

### 8.1 Chaos Monkey for Spring Boot

```xml
<!-- pom.xml (payment-service and merchant-service) -->
<dependency>
  <groupId>de.codecentric</groupId>
  <artifactId>chaos-monkey-spring-boot</artifactId>
  <version>3.1.0</version>
</dependency>
```

```yaml
# application-chaos.yml (activated only during experiments)
chaos:
  monkey:
    enabled: true
    watcher:
      rest-controller: true
      service: true
    assaults:
      level: 5                   # 1 in 5 calls assaulted
      latency-active: true
      latency-range-start: 500
      latency-range-end: 3000
      exceptions-active: true
      exception:
        type: java.io.IOException
        arguments:
          - type: java.lang.String
            value: "Chaos: simulated network failure"
```

### 8.2 Chaos Experiment 1 — merchant-service latency

**Hypothesis:** When merchant-service response time exceeds 2s, the time limiter fires, the fallback serves from cache, and payment-service remains responsive. Circuit opens after 5 consecutive timeouts and recovers automatically.

```bash
# 1. Inject 2500ms latency on merchant-service via Actuator
curl -X POST http://localhost:8082/actuator/chaosmonkey/assaults \
  -H "Content-Type: application/json" \
  -d '{"level":1,"latencyActive":true,"latencyRangeStart":2500,"latencyRangeEnd":2500,"exceptionsActive":false}'

# 2. Send 15 payment requests
for i in $(seq 1 15); do
  time curl -s -X POST http://localhost:8081/v1/payments \
    -H "Authorization: Bearer sk_test_abc123" \
    -H "Content-Type: application/json" \
    -d '{"amount":1000,"currency":"USD","description":"Chaos","card":{...}}' &
done; wait

# 3. Verify circuit opened
curl http://localhost:8081/actuator/health | jq '.components.circuitBreakers'
# Expected: "state": "OPEN"

# 4. Reset
curl -X POST http://localhost:8082/actuator/chaosmonkey/assaults \
  -d '{"level":1,"latencyActive":false,"exceptionsActive":false}'
```

**Expected outcome:** First 4 requests: <2.1s (time limiter fires, fallback returns cached merchantId). Request 5: circuit opens. Requests 6–15: immediate fallback, no thread blocked. 10s later: circuit transitions to HALF_OPEN and recovers.

### 8.3 Chaos Experiment 2 — pod kill in kind

**Hypothesis:** Killing a payment-service pod causes no request failures because K8s readiness probes prevent traffic to the new pod until it is healthy, and 2 replicas ensure continuity.

```bash
# 1. Confirm 2 replicas
kubectl get pods -n payflow -l app=payment-service

# 2. Start background load (10 req/s for 60s)
hey -z 60s -c 10 -q 10 \
  -H "Authorization: Bearer sk_test_abc123" \
  http://localhost/api/v1/payments &

# 3. Kill one pod mid-test
kubectl delete pod -n payflow $(kubectl get pods -n payflow \
  -l app=payment-service -o name | head -1)

# 4. Watch replacement
kubectl get pods -n payflow -l app=payment-service -w

# Expected: 0% error rate, <500ms P99 latency spike during pod kill
```

### 8.4 Chaos Experiment 3 — Kafka broker down

**Hypothesis:** When Kafka is unavailable, payment creation still succeeds (outbox write is a DB transaction, independent of Kafka). When Kafka recovers, the relay drains the backlog in order.

```bash
# 1. Stop Kafka
docker compose -f infra/docker-compose.yml stop kafka

# 2. Create 5 payments (should succeed)
for i in $(seq 1 5); do curl -X POST http://localhost:8081/v1/payments ...; done

# 3. Verify 5 unpublished outbox events
psql -h localhost -p 7001 -U payment_svc -d payments \
  -c "SELECT COUNT(*) FROM outbox_events WHERE published = false;"
# Expected: 5

# 4. Restart Kafka and wait for relay drain
docker compose -f infra/docker-compose.yml start kafka
sleep 30
psql -h localhost -p 7001 -U payment_svc -d payments \
  -c "SELECT COUNT(*) FROM outbox_events WHERE published = false;"
# Expected: 0
```

### 8.5 Experiment documentation

Create `docs/chaos-experiments.md` with each experiment's: hypothesis, method, result, and what the result proves. This document is directly quotable in interviews and distinguishes you from engineers who only say "I tested it."

---

## 9. Phase 21 — Security Hardening

**Why?** Any fintech company will probe security awareness. This phase implements KMS envelope encryption for sensitive fields, IAM least-privilege policies per service, HTTPS via TLS, and security response headers.

### 9.1 KMS envelope encryption for card data

**Pattern:** Generate a per-record data key from KMS → encrypt the field with AES-256-GCM → store (encrypted_data, encrypted_key) in DB → discard plaintext key immediately. To decrypt: call KMS to unwrap the key, decrypt the data.

```java
// infrastructure/crypto/KmsEnvelopeEncryptor.java
@Component
@RequiredArgsConstructor
public class KmsEnvelopeEncryptor {

    private final KmsClient kmsClient;

    @Value("${payflow.kms.key-id}")   // alias/payflow/local/master
    private String keyId;

    public EncryptedValue encrypt(String plaintext) {
        GenerateDataKeyResponse dataKey = kmsClient.generateDataKey(r -> r
            .keyId(keyId).keySpec(DataKeySpec.AES_256));

        byte[] ciphertext = aesGcmEncrypt(
            dataKey.plaintext().asByteArray(),
            plaintext.getBytes(UTF_8));

        // Plaintext key goes out of scope here — never persisted
        return new EncryptedValue(
            Base64.encode(ciphertext),
            Base64.encode(dataKey.ciphertextBlob().asByteArray())
        );
    }

    public String decrypt(EncryptedValue value) {
        DecryptResponse decrypted = kmsClient.decrypt(r -> r
            .keyId(keyId)
            .ciphertextBlob(SdkBytes.fromByteArray(Base64.decode(value.encryptedKey()))));

        return new String(
            aesGcmDecrypt(decrypted.plaintext().asByteArray(), Base64.decode(value.encryptedData())),
            UTF_8);
    }
}
```

```yaml
# application.yml addition
payflow:
  kms:
    key-id: alias/payflow/local/master
```

### 9.2 IAM least-privilege policies per service

```hcl
# infra/terraform/modules/security/iam.tf

# payment-service: read its own secret + use KMS for data keys
resource "aws_iam_policy" "payment_service" {
  name = "payflow-${var.environment}-payment-service"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadOwnSecret"
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
        Resource = [var.payment_db_secret_arn]
      },
      {
        Sid    = "UseKmsForDataKeys"
        Effect = "Allow"
        Action = ["kms:GenerateDataKey", "kms:Decrypt"]
        Resource = [var.kms_key_arn]
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:${var.aws_region}:*:log-group:/payflow/${var.environment}/payment-service:*"
      }
    ]
  })
}

# merchant-service: read its own secret ONLY — no access to payment or webhook secrets
resource "aws_iam_policy" "merchant_service" {
  name = "payflow-${var.environment}-merchant-service"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [var.merchant_db_secret_arn]
    }]
  })
}
```

### 9.3 Security HTTP response headers

```java
@Component
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, res);
    }
}
```

### 9.4 Secret rotation

```bash
# Trigger Secrets Manager rotation via Floci
aws secretsmanager rotate-secret \
  --secret-id /payflow/local/payment-service/db \
  --rotation-rules AutomaticallyAfterDays=30 \
  --endpoint-url http://localhost:4566
```

Configure Spring Cloud AWS to re-fetch secrets every 30s in local profile:

```yaml
spring.cloud.aws.secretsmanager.reload.period: 30s
```

This allows observing live rotation without restarting the service.

### 9.5 TDD for security

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityHeadersTest {
    @Autowired TestRestTemplate restTemplate;

    @Test
    void allResponses_includeRequiredSecurityHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getHeaders().getFirst("Strict-Transport-Security")).startsWith("max-age=31536000");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
    }
}

class KmsEnvelopeEncryptorTest {
    @Autowired KmsEnvelopeEncryptor encryptor;

    @Test
    void encryptedValue_isDecryptableAndNotEqualToPlaintext() {
        EncryptedValue encrypted = encryptor.encrypt("4242");
        assertThat(encrypted.encryptedData()).isNotEqualTo("4242");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("4242");
    }

    @Test
    void encryptingSamePlaintext_producesDifferentCiphertexts() {
        // AES-GCM with random IV must never produce identical ciphertext
        assertThat(encryptor.encrypt("4242").encryptedData())
            .isNotEqualTo(encryptor.encrypt("4242").encryptedData());
    }
}
```

---

## 10. Updated Project Structure

```
payflow/
├── backend/
│   ├── payment-service/
│   │   └── src/main/java/com/payflow/payment/
│   │       ├── domain/
│   │       ├── application/
│   │       ├── infrastructure/
│   │       │   ├── cache/          # MerchantApiKeyCache (Resilience4j)
│   │       │   ├── crypto/         # KmsEnvelopeEncryptor           [NEW v3.0]
│   │       │   ├── messaging/
│   │       │   └── persistence/
│   │       └── api/
│   │           └── config/
│   │               └── OpenApiConfig.java                           [NEW v3.0]
│   ├── merchant-service/           (same additions)
│   ├── webhook-service/            (same additions)
│   └── notification-service/
├── frontend/
├── docs/
│   └── chaos-experiments.md                                         [NEW v3.0]
├── infra/
│   ├── docker-compose.yml          # now includes floci service
│   ├── kind-config.yml             # kind cluster definition        [NEW v3.0]
│   ├── floci-data/                 # Floci persistent storage       [NEW v3.0]
│   ├── terraform/                                                   [NEW v3.0]
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── modules/
│   │       ├── security/           # KMS, Secrets Manager, IAM
│   │       ├── data/               # RDS instances
│   │       ├── messaging/          # MSK cluster
│   │       └── registry/           # ECR repositories
│   └── helm/                       # Replaces raw k8s/ manifests   [NEW v3.0]
│       ├── payflow/                # umbrella chart
│       └── charts/
│           ├── payment-service/
│           ├── merchant-service/
│           ├── webhook-service/
│           ├── notification-service/
│           └── frontend/
├── .github/workflows/
│   ├── backend-ci.yml
│   ├── frontend-ci.yml
│   └── infra-ci.yml                # Terraform fmt + validate       [NEW v3.0]
└── README.md
```

---

## 11. Suggested Implementation Order

| Priority | Phase | Effort | Rationale |
|---|---|---|---|
| 1 | **Phase 16 — OpenAPI docs** | 2 days | Zero risk, immediate visual impact. No infra dependency. |
| 2 | **Phase 17 — Terraform + Floci** | 1 week | Everything else depends on IaC being in place; secrets out of YAML. |
| 3 | **Phase 19 — Resilience4j** | 1 week | Add before chaos testing — circuit breakers must exist before you verify them. |
| 4 | **Phase 18 — kind + Helm** | 1 week | Migrate K8s once infra is stable; Helm values reference Terraform outputs. |
| 5 | **Phase 20 — Chaos Engineering** | 3 days | Short phase; requires Phase 18 (kind) and Phase 19 (circuit breakers) complete. |
| 6 | **Phase 21 — Security** | 1 week | KMS, IAM, TLS. Add last — all services must be stable before encryption is layered in. |

**Total estimated effort: 5–6 weeks at a sustainable pace.**

---

## 12. New Interview Talking Points

**Terraform + Floci (Phase 17):**
"I provision infrastructure with the same Terraform code locally and in AWS — the only difference is removing the `endpoint_url` override. Floci manages real PostgreSQL and Kafka containers under the hood, so the wire protocol is identical to RDS and MSK. I'm not mocking anything."

**Secrets Manager (Phase 17):**
"Passwords never appear in source code, environment variables, or Kubernetes Secrets. Spring Cloud AWS fetches the JSON secret from Secrets Manager at startup using `spring.config.import`. Rotating a credential is a one-command operation. With the 30-second reload period in local dev, I can observe live rotation without restarting the service."

**kind + Helm (Phase 18):**
"I use `kind` because it runs in Docker with zero dependencies, making CI integration trivial. Helm parameterises all environment-specific values — image tags, replica counts, resource limits, AWS endpoints. Promoting from local to production is a single `helm upgrade` with a different values file. The Deployment template uses `maxUnavailable: 0` to guarantee zero-downtime rolling updates."

**Resilience4j circuit breaker (Phase 19):**
"The circuit breaker protects payment-service from cascading failure when merchant-service is slow. The fallback is fail-secure: an uncached key during an open circuit is rejected. We'd rather reject a valid payment than accept an invalid one — that asymmetry is a deliberate fintech-specific security decision, not a default."

**Chaos engineering (Phase 20):**
"I ran three documented experiments with explicit hypotheses. The Kafka experiment proved a subtle property: payment creation is independent of Kafka because the outbox write is a DB transaction. The pod-kill experiment validated that readiness probes prevent traffic to pods that aren't ready — the HPA holding two replicas is only meaningful if the probes are correctly configured."

**KMS envelope encryption (Phase 21):**
"I use envelope encryption: KMS generates a data key per record, I encrypt the sensitive field with AES-256-GCM, store the ciphertext and encrypted key in the DB, and discard the plaintext key immediately. The master KMS key never leaves AWS. The same plaintext produces different ciphertext every time because of the random GCM IV — this prevents chosen-plaintext attacks and deduplication fingerprinting."

---

*PayFlow — v3.0 Cloud-Native & Reliability Specification*
*Builds on v2.0 (Phases 1–15). Covers Phases 16–21.*
