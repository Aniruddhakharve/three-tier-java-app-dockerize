# Project Continuation Log — Three-Tier Java + DevSecOps CI/CD + Kubernetes + Helm

**Updated:** 27 November 2026  
**GitHub:** https://github.com/Aniruddhakharve/three-tier-java-app-dockerize  
**Repository:** `Aniruddhakharve/three-tier-java-app-dockerize`  
**Default branch:** `main`  
**Purpose:** Durable handoff for continuing this exact project in a new ChatGPT chat.

---

## 1. Current status

The Java/Tomcat/PostgreSQL three-tier application has been:

- Dockerized and optimized using multi-stage builds and a custom JLink runtime.
- Integrated into an eight-stage reusable GitHub Actions DevSecOps pipeline.
- Deployed to AWS EC2 using Docker Compose.
- Verified through an actual end-to-end CI/CD deployment.
- Deployed to a local KIND Kubernetes cluster.
- Extended with Helm learning and a Headlamp Kubernetes dashboard deployment.

### DevSecOps pipeline

The full eight-stage pipeline passed. A change was made to `index.html`, pushed to GitHub, the pipeline built and pushed a new SHA-tagged image, ran scans, deployed it to EC2, and the updated page showing `CI/CD v2` appeared in the browser on port `8081`.

This is actual CI/CD verification, not merely a successful scan workflow.

### Kubernetes

The Java + PostgreSQL application is deployed to a local KIND cluster. The Kubernetes setup includes:

- Java application Deployment with two replicas.
- NodePort Service.
- PostgreSQL StatefulSet.
- PostgreSQL headless Service and ClusterIP Service.
- Persistent storage through a PVC.
- ConfigMap and Secret configuration.
- Metrics Server.
- Horizontal Pod Autoscaler.
- Headlamp dashboard installed using Helm.

### Latest repository update

The latest commit was pushed successfully:

- Commit: `9ee29da`
- Message: `refactor(k8s): organize manifests and document Headlamp deployment`
- Branch: `main`
- Git working tree: clean.
- Local branch synchronized with `origin/main`.

The README and GitHub repository description were completed earlier.

Preferred repository description:

> Containerized & optimized Java three-tier app with Docker/JLink, plus a Shift-Left DevSecOps CI/CD pipeline with GitHub Actions, security scanning, Docker Hub & AWS EC2.

---

## 2. Repository and image names

- GitHub repository: `https://github.com/Aniruddhakharve/three-tier-java-app-dockerize`
- Default branch: `main`
- Current Docker Hub image: `aniruddhakharve/three-tier-app`
- Current image tags: `<github-sha>` and `latest`
- Historical Docker Hub repository: `aniruddhakharve/three-tier-java-app` (older tags included `latest` and `v3-jlink`).

**Important:** Current pipeline/deployment image is `three-tier-app`, not the older `three-tier-java-app`.

### Latest Git state

```text
Branch: main
Latest commit: 9ee29da
Commit message: refactor(k8s): organize manifests and document Headlamp deployment
Remote: origin/main
Working tree: clean
```

Latest push:

```text
5aa29da..9ee29da  main -> main
```

---

## 3. Dockerization and application

The app is a Java web application on Tomcat with PostgreSQL.

Three Dockerfiles exist:

- `Dockerfile` — simple build.
- `Dockerfile.multistage` — multi-stage build.
- `Dockerfile.jlink` — production-oriented build with a custom `jlink` Java runtime; this is the image used by CI/CD.

### Dockerfile.jlink details

- Builder: `maven:3.9-eclipse-temurin-11`
- Final image: `debian:bookworm-slim`
- Tomcat: `9.0.121`
- Builds the WAR with Maven.
- Custom runtime includes:
  - `java.base`
  - `java.logging`
  - `java.management`
  - `java.naming`
  - `java.sql`
  - `java.xml`
  - `java.desktop`
  - `java.security.jgss`
  - `java.instrument`
  - `jdk.crypto.ec`
  - `jdk.unsupported`
- Removes default Tomcat webapps.
- Runs as non-root `tomcat` user.
- Exposes `8080`; command runs `catalina.sh run`.
- Hadolint package pins:
  - `ca-certificates=20250419~deb12u1`
  - `curl=7.88.1-10+deb12u15`
- Image reduced from about 209 MB to about 81 MB (~61% reduction).

### Docker Compose facts

The project deploys the full app + database stack with Compose:

- `db`: `postgres:15`, container name `day36-postgres`.
- `app`: Java/Tomcat, container name `day36-java-app`.
- PostgreSQL network alias: `db-primary-service`.
- App port mapping: **`8081:8080`**. Preserve this; local host port 8080 may be occupied.
- PostgreSQL healthcheck uses `pg_isready`.
- App waits for DB using `depends_on` with `condition: service_healthy`.
- PostgreSQL data is persisted in a named volume mounted at `/var/lib/postgresql/data`.
- Bridge network: `three-tier-network`.
- Image reference: `${DOCKERHUB_USER}/three-tier-app:${DOCKER_TAG}`.
- DB/application credentials and connection details come from environment variables.
- Historical notes show a volume key/name discrepancy (`postgress_data` / `day36-postgres-data`). Inspect the current live `docker-compose.yml` before editing or asserting the exact volume key. Do not rename it casually.

---

## 4. GitHub Actions workflow structure

Main orchestrator:

`.github/workflows/devsecops-pipeline.yml`

Exact reusable workflow filenames:

1. `code-quality.yml`
2. `secrets-scan.yml`
3. `dependency-scan.yml`
4. `dockerfile-scan.yml`
5. `docker-build-push.yml`
6. `container-scan.yml`
7. `dast-scan.yml`
8. `deploy.yml`

**Filename accuracy matters:** use `secrets-scan.yml` (not `secret-scan.yml`) and `docker-build-push.yml` (not `build-and-push.yml`). Orchestrator calls each workflow with `uses: ./.github/workflows/<filename>` and `secrets: inherit`; `needs` establishes the order.

### Current orchestrator trigger

The main pipeline was temporarily changed to manual-only while the README was being edited, to avoid triggering the full ~13-minute pipeline on documentation pushes.

```yaml
name: DevSecOps Pipeline
on:
  # push:
  #   branches:
  #     - main
  workflow_dispatch:
```

After documentation is fully finalized, restore push-to-main and keep manual dispatch:

```yaml
name: DevSecOps Pipeline
on:
  push:
    branches:
      - main
  workflow_dispatch:
```

**Important:** This trigger restoration was still listed as a future action in the 23 November log. Reopen the live workflow before assuming it has or has not been restored.

The eight stages, in order:

1. Code Quality
2. Secret Scan
3. Dependency Scan
4. Dockerfile Scan
5. Build and Push Docker Image
6. Container Image Scan
7. DAST Scan
8. Deploy Application

Job IDs in the orchestrator may be `secret-scan` and `build-and-push`; those are job IDs, not workflow filenames.

---

## 5. Stage implementation and resolved issues

### Code Quality — `code-quality.yml`

- Reusable via `workflow_call`.
- Checkout, Java 11 Temurin, Maven compile, Checkstyle, SpotBugs + FindSecBugs.
- Fixed ignored `Statement.executeQuery` result handling, restricted CORS through `ALLOWED_ORIGIN`, made Gson static final, added XSS servlet suppression with `@SuppressFBWarnings`, and added `spotbugs-annotations` `4.9.8` (provided).
- Passed with BugInstance size 0.

### Secret Scan — `secrets-scan.yml`

- Reusable via `workflow_call`.
- Checkout with `fetch-depth: 0`; Gitleaks action; `GITHUB_TOKEN`.
- Earlier `workflow_run` trigger design was replaced with reusable workflow design.

### Dependency Scan — `dependency-scan.yml`

- OWASP Dependency-Check Maven plugin `12.1.0`, Java 17, `NVD_API_KEY` secret.
- Cache: `~/.m2/repository/org/owasp/dependency-check-data`.
- Uses `-Dformat=ALL`, `-DfailBuildOnCVSS=7`, `-DnvdApiKeyEnvironmentVariable=NVD_API_KEY`.
- OSS Index 401 fixed using exact plugin setting `ossindexAnalyzerEnabled=false` (case-sensitive spelling was verified using Maven help/effective POM).
- Passed.

### Dockerfile Scan — `dockerfile-scan.yml`

- Hadolint scans all three Dockerfiles.
- Fixed `DL3008` in `Dockerfile.jlink` by pinning Debian package versions listed above.
- Informational `DL3066` about `USER tomcat` remained below failure threshold.
- Passed.

### Build and Push — `docker-build-push.yml`

- Reusable via `workflow_call`.
- `actions/checkout@v7`, Docker Buildx, Docker Hub login.
- Builds `Dockerfile.jlink`.
- Pushes:
  - `${{ secrets.DOCKERHUB_USERNAME }}/three-tier-app:${{ github.sha }}`
  - `${{ secrets.DOCKERHUB_USERNAME }}/three-tier-app:latest`
- Old Trivy block in this workflow was commented out, not active; separate `container-scan.yml` handles Trivy.

### Container Scan — `container-scan.yml`

- Runs after Build and Push, scanning the pushed SHA image.
- Trivy action `v0.36.0`; HIGH/CRITICAL; `ignore-unfixed: true`; OS and library scanning; SARIF artifact.
- Earlier `manifest unknown` happened when scanning before the SHA-tagged image was pushed. Fixed by ordering Build and Push before Container Scan.
- Integrated pipeline passed.

### DAST — `dast-scan.yml`

- Runs ZAP stable container: `ghcr.io/zaproxy/zaproxy:stable`.
- Starts test app on `8080:8080`, waits with `curl`, creates `zap-reports` with `chmod 777`.
- ZAP uses `--network host`, target `http://127.0.0.1:8080`.
- Produces JSON, Markdown, HTML reports; `-I` ignores warnings; uploads reports and cleans up app container.
- `host.docker.internal` connectivity failed in runner; host networking + `127.0.0.1` fixed it.
- In isolated DAST setup, app logs showed `db-primary-service` could not resolve, but Tomcat started and HTTP was available. Be precise: this DAST run tested reachable HTTP behavior, not full DB-backed integration.
- Recorded results: High 0, Medium 2, Low 5, Info 3; `FAIL-NEW: 0`, `WARN-NEW: 8`, `PASS: 59`.
- Warnings included missing CSP, anti-clickjacking, COEP/COOP/CORP, Permissions Policy, X-Content-Type-Options, suspicious comments, modern web app and cacheable content.

### Deploy — `deploy.yml`

Secrets referenced: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`, `NVD_API_KEY`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

Deployment sequence:

1. Checkout.
2. SSH to EC2.
3. Check/install Docker (`docker.io`) and Compose (`docker-compose-v2`), enable/start Docker.
4. Create `~/devops`.
5. SCP `docker-compose.yml` to `~/devops/`.
6. Create `~/devops/.env` using GitHub secrets plus `DOCKERHUB_USER` and `DOCKER_TAG=${GITHUB_SHA}`; chmod `600`.
7. Docker Hub login.
8. Run `docker compose down`, `docker compose pull`, `docker compose up -d --force-recreate`.
9. Verify with `docker ps` and `docker compose ps`.

The full Compose app ran successfully on EC2 at port `8081`. Status page showed App UP, DB UP, DB host `db-primary-service`, DB port `5432`, database `appdb`, user `appuser`.

Never put actual secret values in README, screenshots, or logs.

---

## 6. Kubernetes deployment

### Local cluster

- Distribution: KIND.
- Cluster name: `java-cluster`.
- Current Kubernetes context: `kind-java-cluster`.
- Kubernetes version: `v1.37.0`.
- Nodes:
  - `java-cluster-control-plane`
  - `java-cluster-worker`
- Both nodes were verified `Ready`.

### Application namespace

Namespace: `three-tier-app`

The Java + PostgreSQL application was deployed and verified in this namespace.

Resources include:

- Java application Deployment: 2 replicas.
- Java application NodePort Service.
- PostgreSQL StatefulSet.
- PostgreSQL headless Service.
- PostgreSQL ClusterIP Service.
- PersistentVolumeClaim.
- ConfigMap.
- Secret.
- Metrics Server.
- Horizontal Pod Autoscaler.

### Application details

- Application container port: `8080`.
- Local host access: `http://localhost:8081`.
- Health endpoint: `/health`.
- The application Deployment uses an init container that waits for PostgreSQL readiness using `pg_isready`.
- Application configuration is injected through ConfigMap and Secret references.
- Java Deployment uses startup, readiness and liveness probes.
- Application image was SHA-tagged.

**Important:** The Deployment currently has this intentional rolling-update configuration:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 1
    maxSurge: 0
```

Do not casually revert `maxSurge: 0` to `1`. This was an intentional user change and is included in commit `9ee29da`.

### HPA

- Metrics Server installed in the KIND cluster.
- HPA configured for the Java application.
- Target CPU utilization: 60%.
- Minimum replicas: 2.
- Maximum replicas: 5.

The HPA implementation and testing notes are located at:

`k8s/learning-logs/hpa-implementation-and-testing.md`

### Kubernetes manifest directory

The old `k8s/manifest/` directory was renamed to `k8s/manifests/`.

Current structure:

```text
k8s/
├── README.md
├── kind-config.yml
├── learning-logs/
│   └── hpa-implementation-and-testing.md
└── manifests/
    ├── application/
    │   ├── deployment.yaml
    │   ├── hpa.yaml
    │   └── service.yaml
    ├── database/
    │   ├── headless-service.yaml
    │   ├── service.yaml
    │   └── statefulset.yaml
    ├── headlamp/
    │   ├── headlamp-admin-sa.yaml
    │   ├── headlamp-readonly-binding.yaml
    │   ├── headlamp-readonly-clusterrole.yaml
    │   ├── headlamp-readonly-sa.yaml
    │   ├── values.yaml
    │   └── README.md
    ├── metrics-server/
    │   └── components.yaml
    ├── configmap.yaml
    ├── namespace.yaml
    └── secret.yml
```

**Secret handling:**

- `k8s/manifests/secret.yml` exists locally.
- It is ignored by `.gitignore`.
- It is not tracked by Git.
- Do not delete it casually.
- Never commit actual database credentials or tokens.

Relevant `.gitignore` entries:

```gitignore
# Kubernetes sensitive manifests
k8s/manifests/secret.yml
k8s/manifests/secrets/
```

### Kubernetes documentation

The following documentation files were created:

- `k8s/README.md` — overall Kubernetes project overview, architecture, prerequisites, deployment instructions and verification commands.
- `k8s/manifests/headlamp/README.md` — Headlamp installation, Helm configuration, RBAC, authentication, verification and useful commands.

---

## 7. Helm learning and hands-on practice

### Helm installation

Helm was installed and verified:

```text
Helm v4.3.1
```

Helm is used as the Kubernetes package manager. It manages charts and releases but does not replace Kubernetes.

Core terminology:

- **Chart:** Package containing Kubernetes templates and default configuration.
- **Release:** An installed instance of a chart.
- **Repository:** A location from which charts can be discovered and downloaded.

### Chart structure

A starter chart was generated using:

```bash
helm create helm-practice
```

Typical chart structure:

```text
helm-practice/
├── Chart.yaml
├── values.yaml
├── charts/
└── templates/
    ├── _helpers.tpl
    ├── deployment.yaml
    ├── hpa.yaml
    ├── service.yaml
    ├── serviceaccount.yaml
    └── tests/
```

Concepts studied:

- `Chart.yaml`
- `values.yaml`
- `templates/`
- `_helpers.tpl`
- `charts/`
- `.Values`
- `.Chart`
- `.Release`
- `include`
- `define`
- `default`
- `toYaml`
- `nindent`
- `if`
- `with`

### Values and template rendering

Practiced:

```bash
helm template demo ./helm-practice
helm template demo ./helm-practice --set replicaCount=3
helm template demo ./helm-practice --set replicaCount=3 --set image.tag=latest
helm template demo ./helm-practice -f ./helm-practice/values-dev.yaml
helm lint helm-practice
```

Learned values precedence:

1. Chart's default `values.yaml`.
2. Custom values file supplied with `-f`.
3. Command-line overrides supplied with `--set`.

`helm template` renders Kubernetes manifests locally; it does not install resources into the cluster.

`helm lint` checks chart structure and common template/configuration problems. It does not prove the application will run successfully in a cluster.

### Helpers and naming

Studied `_helpers.tpl`:

- `name`
- `fullname`
- `chart`
- `labels`
- `selectorLabels`
- `serviceAccountName`

Compared:

- `nameOverride`
- `fullnameOverride`

Verified how names and labels affect the rendered Kubernetes resources.

### Helm release lifecycle

A practice chart was installed in the separate namespace `helm-practice`.

Install:

```bash
helm install nginx-demo ./helm-practice \
  --namespace helm-practice \
  --create-namespace
```

Upgrade:

```bash
helm upgrade nginx-demo helm-practice \
  --namespace helm-practice \
  -f helm-practice/values-dev.yaml
```

Inspect history:

```bash
helm history nginx-demo -n helm-practice
```

Rollback:

```bash
helm rollback nginx-demo 1 -n helm-practice
```

Uninstall:

```bash
helm uninstall nginx-demo -n helm-practice
```

Verified that uninstall removes resources managed by the release but leaves the namespace.

The `helm-practice` namespace was retained for future practice.

### Bitnami NGINX chart review

Added/used the Bitnami repository:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

Inspected:

```bash
helm search repo bitnami/nginx
helm show chart bitnami/nginx
helm show values bitnami/nginx
```

Reviewed chart configuration for:

- Image configuration.
- Deployment settings.
- Service configuration.
- Ingress.
- NetworkPolicy.
- Resource requests and limits.
- Startup, liveness and readiness probes.
- Autoscaling.
- Volumes and volume mounts.
- ServiceAccount.
- Sidecars and init containers.
- Pod disruption budget.
- Node scheduling settings.

Important learning: inspect the actual chart's values before assuming a setting exists. Do not invent configuration options that are not exposed by the chart.

---

## 8. Headlamp dashboard using Helm

### Chart repository

Official Headlamp Helm repository:

```bash
helm repo add headlamp https://kubernetes-sigs.github.io/headlamp/
helm repo update
```

Chart search:

```bash
helm search repo headlamp
```

Inspected:

```bash
helm show chart headlamp/headlamp
helm show values headlamp/headlamp
```

At the time of installation:

- Chart version: `0.46.0`
- App version: `0.46.0`
- Chart: `headlamp/headlamp`
- Image: `ghcr.io/headlamp-k8s/headlamp:0.46.0`

Recheck the repository for newer versions before future upgrades.

### Headlamp namespace

Namespace:

```text
headlamp
```

Created with:

```bash
kubectl create namespace headlamp
```

### Custom RBAC architecture

The goal was to avoid using the chart's default `cluster-admin` binding and instead configure custom read-only and admin identities.

Created the following resources:

#### Read-only ServiceAccount

File:

`k8s/manifests/headlamp/headlamp-readonly-sa.yaml`

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: headlamp-sa
  namespace: headlamp
```

#### Read-only ClusterRole

File:

`k8s/manifests/headlamp/headlamp-readonly-clusterrole.yaml`

Permissions include `get`, `list`, and `watch` for:

- Pods
- Services
- Namespaces
- Nodes
- ConfigMaps
- PersistentVolumes
- PersistentVolumeClaims
- Deployments
- DaemonSets
- ReplicaSets
- StatefulSets
- Jobs
- CronJobs

The role does not grant permission to create, update, patch or delete these resources. It also does not grant access to Secrets.

#### Read-only ClusterRoleBinding

File:

`k8s/manifests/headlamp/headlamp-readonly-binding.yaml`

Binds:

```text
ServiceAccount: headlamp/headlamp-sa
ClusterRole: headlamp-readonly
```

#### Admin ServiceAccount

File:

`k8s/manifests/headlamp/headlamp-admin-sa.yaml`

Creates:

```text
ServiceAccount: headlamp/headlamp-admin-sa
```

It also creates a ClusterRoleBinding to the built-in `cluster-admin` ClusterRole.

**Security warning:** `cluster-admin` provides broad cluster-wide permissions, including access to Secrets. Use this identity only for administrative tasks.

### RBAC verification

Read-only identity:

```bash
kubectl auth can-i get pods \
  --as=system:serviceaccount:headlamp:headlamp-sa

kubectl auth can-i list deployments \
  --as=system:serviceaccount:headlamp:headlamp-sa

kubectl auth can-i delete pods \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

Observed results:

```text
yes
yes
no
```

Admin identity was tested for reading, creating, deleting resources and accessing Secrets. Those checks returned `yes`.

### Helm custom values

File:

`k8s/manifests/headlamp/values.yaml`

Configuration:

```yaml
replicaCount: 1

config:
  inCluster: true
  unsafeUseServiceAccountToken: false

automountServiceAccountToken: true

serviceAccount:
  create: false
  name: headlamp-sa

clusterRoleBinding:
  create: false

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: false

httpRoute:
  enabled: false

persistentVolumeClaim:
  enabled: false
```

Important configuration decisions:

- `serviceAccount.create: false` tells Helm to use the existing read-only ServiceAccount.
- `serviceAccount.name: headlamp-sa` selects that ServiceAccount.
- `clusterRoleBinding.create: false` prevents the chart from creating its default ClusterRoleBinding to `cluster-admin`.
- `automountServiceAccountToken: true` allows the Headlamp Pod to communicate with the Kubernetes API using its ServiceAccount credentials.
- `config.inCluster: true` enables in-cluster Kubernetes API configuration.
- `config.unsafeUseServiceAccountToken: false` prevents all logged-in Headlamp users from automatically being treated as the Pod's ServiceAccount identity.

**Important architecture distinction:** Headlamp's Pod identity and the authenticated dashboard user's identity are separate concepts. Keep `unsafeUseServiceAccountToken: false` for this setup.

### Installation

Installed the chart using:

```bash
helm install headlamp headlamp/headlamp \
  --namespace headlamp \
  -f k8s/manifests/headlamp/values.yaml
```

Installation was successful.

Observed:

```text
NAME: headlamp
NAMESPACE: headlamp
STATUS: deployed
REVISION: 1
CHART: headlamp-0.46.0
APP VERSION: 0.46.0
```

### Verification

```bash
helm list -n headlamp
kubectl get all -n headlamp
kubectl get sa -n headlamp
kubectl get clusterrolebinding | grep headlamp
```

Observed:

- Headlamp Pod: `1/1 Running`.
- Headlamp Service: ClusterIP on port `80`.
- Deployment: `1/1` available.
- Pod uses `headlamp-sa`.
- No chart-created `cluster-admin` ClusterRoleBinding.
- Custom read-only and admin RBAC bindings exist.

### Dashboard access

Port-forward:

```bash
kubectl port-forward -n headlamp svc/headlamp 8080:80
```

Dashboard:

```text
http://localhost:8080
```

Read-only token:

```bash
kubectl create token headlamp-sa -n headlamp
```

Admin token:

```bash
kubectl create token headlamp-admin-sa -n headlamp
```

Tokens were used to log into separate Headlamp browser sessions.

**Security:** Never paste tokens into ChatGPT, commit them to Git, or store them in README files. These tokens are short-lived by default; request a new token when needed.

### Permission testing through Headlamp

Verified:

- Read-only identity can inspect permitted Kubernetes resources.
- Read-only identity cannot access Secrets.
- Admin identity can inspect resources that are forbidden to read-only users.
- The dashboard showed the expected ServiceAccount identity.

The read-only dashboard returned a forbidden error when attempting to inspect the PostgreSQL Secret. This was expected and confirmed least-privilege access.

### Hands-on ConfigMap RBAC test

Read-only identity could not create a ConfigMap:

```bash
kubectl create configmap rbac-demo \
  --from-literal=purpose=testing-rbac \
  -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

The API returned `Forbidden`, as expected.

Admin identity successfully created it:

```bash
kubectl create configmap rbac-demo \
  --from-literal=purpose=testing-rbac \
  -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa
```

Read-only identity could read it but could not delete it. Admin identity deleted it successfully.

Final verification returned `NotFound`, confirming cleanup.

This exercise demonstrated the difference between the RBAC verbs:

- `get`
- `list`
- `watch`
- `create`
- `update`
- `patch`
- `delete`

---

## 9. README and screenshot set

README title:

`# 🚀 Three-Tier Java Application — Dockerized, Optimized & DevSecOps CI/CD`

The README was expanded while retaining the original Dockerization/project history. It includes project overview, badges, Project at a Glance, Documentation Roadmap, Phase 1 Dockerization, original docs/setup, Phase 2 DevSecOps pipeline, reusable workflows, Shift-Left, stage explanations, deployment, secrets, Compose, architecture, CI/CD proof, AWS deployment, screenshots, challenges, learning outcomes, portfolio/resume positioning and final summary.

A reviewed version was about 3,021 lines. Re-open the live README before any further edits; do not assume the repository has not changed. Keep the existing project story; do not replace it with a short generic README.

Store screenshots in root `screenshots/` and embed them as Markdown images, e.g.:

`![DevSecOps CI/CD Pipeline - Successful Run](./screenshots/01-devsecops-pipeline-success.png)`

Expected screenshot names:

1. `01-devsecops-pipeline-success.png` — successful full pipeline.
2. `02-github-actions-pipeline.png` — pipeline visualization/order.
3. `03-dockerhub-image-sha-tag.png` — Docker Hub latest and SHA tags.
4. `04-trivy-container-scan.png` — Trivy scan.
5. `05-zap-dast-report.png` — ZAP result.
6. `06-aws-application-running.png` — AWS app on :8081, App UP / DB UP.
7. `07-index-change-cicd-test.png` — updated `CI/CD v2` page.
8. `08-docker-compose-ps.png` — EC2 `docker compose ps`, app Up and PostgreSQL healthy.

The eighth screenshot replaced an earlier generic `docker ps` screenshot with the more useful `docker compose ps`.

Kubernetes documentation:

- `k8s/README.md`
- `k8s/manifests/headlamp/README.md`

These were committed and pushed in commit `9ee29da`.

---

## 10. Resolved troubleshooting history

- `.dockerignore` had accidentally excluded `src`.
- JLink runtime initially missed `org/ietf/jgss/GSSException`; runtime modules/dependencies were corrected.
- App container and DB environment/configuration mismatches were fixed.
- Docker Compose network-label conflict was resolved.
- Accidental image deletion was recovered.
- SpotBugs findings were fixed.
- CORS was restricted through environment configuration.
- Gitleaks trigger design was changed to `workflow_call`.
- Dependency-Check OSS Index 401 fixed by disabling its analyzer.
- Hadolint package pinning fixed.
- Trivy `manifest unknown` fixed by ensuring image push precedes scan.
- ZAP networking fixed using host network and `127.0.0.1`.
- EC2 Docker/Compose setup and deployment automated.
- Deployment uses immutable SHA image tag, not only mutable `latest`.
- Kubernetes application deployed to KIND.
- Metrics Server and HPA configured.
- Headlamp deployed through Helm.
- Custom read-only and admin RBAC identities created and tested.
- Kubernetes manifest directory reorganized.
- Sensitive Kubernetes Secret manifest confirmed ignored by Git.
- Git staging whitespace issues corrected before commit.

---

## 11. Exact stopping point — 27 November 2026

### Latest completed work

The user completed the Headlamp Helm deployment and custom RBAC implementation.

Headlamp is installed in the `headlamp` namespace using chart version `0.46.0`. The custom read-only ServiceAccount is used by the Headlamp Pod, and the chart's default `cluster-admin` binding is disabled.

Both read-only and admin identities were tested through Headlamp and `kubectl`.

The user then reorganized the Kubernetes project files:

- Renamed `k8s/manifest/` to `k8s/manifests/`.
- Moved Headlamp RBAC YAML files into `k8s/manifests/headlamp/`.
- Moved the custom Headlamp Helm values file into that directory.
- Removed temporary chart-inspection files and empty directories.
- Created `k8s/README.md`.
- Created `k8s/manifests/headlamp/README.md`.
- Updated `.gitignore` for the new Secret manifest path.
- Verified the Secret manifest is ignored and not tracked.
- Preserved the intentional `maxSurge: 0` Deployment change.
- Fixed staged whitespace errors.

### Latest commit and push

Commit:

```text
9ee29da
```

Message:

```text
refactor(k8s): organize manifests and document Headlamp deployment
```

Push:

```text
5aa29da..9ee29da  main -> main
```

Final Git verification:

```text
On branch main
Your branch is up to date with 'origin/main'.

nothing to commit, working tree clean
```

### Current learning position

The user has studied Helm fundamentals, chart structure, values, templates, helpers, rendering, linting, release installation, upgrade, rollback, uninstall and chart inspection.

The user has also installed Headlamp with Helm and configured custom RBAC.

**Do not assume the entire Helm learning journey is finished.** Continue with the next appropriate Helm topic, teaching incrementally and using hands-on examples.

### Possible next actions

Potential next steps include:

- Continue Helm learning with the next topic.
- Practice Helm upgrade and rollback using Headlamp or another chart.
- Learn Helm dependencies and subcharts.
- Learn how to create a Helm chart for the existing Java + PostgreSQL application.
- Learn how to manage environment-specific values.
- Learn Helm hooks, tests and chart packaging.
- Review the repository and improve the Kubernetes documentation.
- Revisit the DevSecOps pipeline trigger and run a final verification if needed.

**Ask the user what they want to work on next. Do not assume or start a new implementation task without confirmation.**

---

## 12. User's working preferences

- Be upbeat, direct, practical and hands-on.
- Teach and troubleshoot incrementally.
- Use exact live filenames, YAML, env vars, image names/tags and ports.
- When the user shares logs/files, inspect those exact contents before suggesting edits.
- Do not redo completed stages unless asked or a refresher is useful.
- Do not claim tests passed unless supported by actual results.
- Preserve existing README content and project history.
- Provide complete usable commands/configuration and explain why a change is required.
- Preserve `8081:8080`.
- Current image repository is `aniruddhakharve/three-tier-app`.
- Use exact workflow filenames listed above.
- Distinguish isolated DAST HTTP testing from full DB-backed integration testing.
- Teach Helm and Kubernetes from beginner level when needed.
- Explain why a Helm value or Kubernetes resource is used, what problem it solves, and what alternatives exist.
- Do not dump large YAML files or multiple commands without explaining them.
- Proceed one step at a time: explain, give the next task/command, inspect the user's output, then continue.
- Preserve intentional user changes such as `maxSurge: 0`.
- Never ask the user to share tokens, passwords, or other secret values.

---

## 13. New-chat handoff prompt

Paste this into a new conversation and attach this continuation log:

> Continue my Three-Tier Java + DevSecOps + Kubernetes project using the attached **Project Continuation Log** as the source of truth. Read it fully first.
>
> Repository: https://github.com/Aniruddhakharve/three-tier-java-app-dockerize
>
> We have Dockerized and optimized the Java/Tomcat/PostgreSQL three-tier application, built an eight-stage reusable GitHub Actions DevSecOps pipeline, deployed the Docker Compose stack to AWS EC2, and verified real end-to-end CI/CD by changing `index.html` and seeing `CI/CD v2` deployed.
>
> We have also deployed the Java + PostgreSQL application to KIND, configured Metrics Server and HPA, learned Helm fundamentals, installed Headlamp using the official Helm chart, and implemented and tested custom read-only and admin RBAC identities.
>
> The latest commit is `9ee29da`, pushed to `main`. The working tree is clean. The Kubernetes manifests are now under `k8s/manifests/`.
>
> Preserve the existing implementation and project history. Do not redo completed work. Use exact workflow filenames and keep the Docker Compose mapping `8081:8080`. Do not expose or request secrets.
>
> I prefer beginner-friendly, incremental, hands-on teaching. Explain one concept at a time, provide the next command or task, and wait for my output before continuing.
>
> First confirm your understanding of the current project state, then ask what I want to work on next.

---

## Memory and continuity

This file is the durable handoff and should be kept in the repository or another safe location. ChatGPT memory can help carry forward context, but it is not a guaranteed full transcript archive. Attach this log in a new chat to restore precise project state.