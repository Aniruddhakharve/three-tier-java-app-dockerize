# Project Continuation Log --- Three-Tier Java + DevSecOps CI/CD

**Updated:** 23 November 2026\
**GitHub:**
https://github.com/Aniruddhakharve/three-tier-java-app-dockerize\
**Repository:** `Aniruddhakharve/three-tier-java-app-dockerize`\
**Purpose:** Durable handoff for continuing this exact project in a new
ChatGPT chat.

## 1. Current status

The Java/Tomcat/PostgreSQL three-tier application has been Dockerized,
optimized, integrated into an eight-stage reusable GitHub Actions
DevSecOps pipeline, and deployed to AWS EC2 using Docker Compose.

**End-to-end proof:** The full eight-stage pipeline passed. A change was
made to `index.html`, pushed to GitHub, the pipeline built and pushed a
new SHA-tagged image, ran scans, deployed it to EC2, and the updated
page showing `CI/CD v2` appeared in the browser on port `8081`. This is
actual CI/CD verification, not merely a successful scan workflow.

The README and GitHub repository description were completed. Preferred
repo description:

> Containerized & optimized Java three-tier app with Docker/JLink, plus
> a Shift-Left DevSecOps CI/CD pipeline with GitHub Actions, security
> scanning, Docker Hub & AWS EC2.

## 2. Repository and image names

-   GitHub repo:
    `https://github.com/Aniruddhakharve/three-tier-java-app-dockerize`
-   Default branch: `main`
-   Current Docker Hub image: `aniruddhakharve/three-tier-app`
-   Current image tags: `<github-sha>` and `latest`
-   Historical Docker Hub repo: `aniruddhakharve/three-tier-java-app`
    (older tags included `latest` and `v3-jlink`).

**Important:** Current pipeline/deployment image is `three-tier-app`,
not the older `three-tier-java-app`.

## 3. Dockerization and application

The app is a Java web application on Tomcat with PostgreSQL.

Three Dockerfiles exist: - `Dockerfile` --- simple build. -
`Dockerfile.multistage` --- multi-stage build. - `Dockerfile.jlink` ---
production-oriented build with a custom `jlink` Java runtime; this is
the image used by CI/CD.

`Dockerfile.jlink` details: - Builder: `maven:3.9-eclipse-temurin-11` -
Final image: `debian:bookworm-slim` - Tomcat `9.0.121` - Builds the WAR
with Maven. - Custom runtime includes `java.base`, `java.logging`,
`java.management`, `java.naming`, `java.sql`, `java.xml`,
`java.desktop`, `java.security.jgss`, `java.instrument`,
`jdk.crypto.ec`, `jdk.unsupported`. - Removes default Tomcat webapps. -
Runs as non-root `tomcat` user. - Exposes `8080`; command runs
`catalina.sh run`. - Hadolint package pins:
`ca-certificates=20250419~deb12u1`, `curl=7.88.1-10+deb12u15`. - Image
reduced from about 209 MB to about 81 MB (\~61% reduction).

### Docker Compose facts

The project deploys the full app + database stack with Compose: - `db`:
`postgres:15`, container name `day36-postgres`. - `app`: Java/Tomcat,
container name `day36-java-app`. - PostgreSQL network alias:
`db-primary-service`. - App port mapping: **`8081:8080`**. Preserve
this; local host port 8080 may be occupied. - PostgreSQL healthcheck
uses `pg_isready`. - App waits for DB using `depends_on` with
`condition: service_healthy`. - PostgreSQL data is persisted in a named
volume mounted at `/var/lib/postgresql/data`. - Bridge network:
`three-tier-network`. - Image reference:
`${DOCKERHUB_USER}/three-tier-app:${DOCKER_TAG}`. - DB/application
credentials and connection details come from environment variables. -
Historical notes show a volume key/name discrepancy (`postgress_data` /
`day36-postgres-data`). Inspect the current live `docker-compose.yml`
before editing or asserting the exact volume key. Do not rename it
casually.

## 4. GitHub Actions workflow structure

Main orchestrator: - `.github/workflows/devsecops-pipeline.yml`

Exact reusable workflow filenames: 1. `code-quality.yml` 2.
`secrets-scan.yml` 3. `dependency-scan.yml` 4. `dockerfile-scan.yml` 5.
`docker-build-push.yml` 6. `container-scan.yml` 7. `dast-scan.yml` 8.
`deploy.yml`

**Filename accuracy matters:** use `secrets-scan.yml` (not
`secret-scan.yml`) and `docker-build-push.yml` (not
`build-and-push.yml`). Orchestrator calls each workflow with
`uses: ./.github/workflows/<filename>` and `secrets: inherit`; `needs`
establishes the order.

### Current orchestrator trigger

The main pipeline was temporarily changed to manual-only while the
README was being edited, to avoid triggering the full \~13-minute
pipeline on documentation pushes:

``` yaml
name: DevSecOps Pipeline
on:
  # push:
  #   branches:
  #     - main
  workflow_dispatch:
```

After documentation is fully finalized, restore push-to-main and keep
manual dispatch:

``` yaml
name: DevSecOps Pipeline
on:
  push:
    branches:
      - main
  workflow_dispatch:
```

The eight stages, in order: 1. Code Quality 2. Secret Scan 3. Dependency
Scan 4. Dockerfile Scan 5. Build and Push Docker Image 6. Container
Image Scan 7. DAST Scan 8. Deploy Application

Job IDs in the orchestrator may be `secret-scan` and `build-and-push`;
those are job IDs, not workflow filenames.

## 5. Stage implementation and resolved issues

### Code Quality --- `code-quality.yml`

-   Reusable via `workflow_call`.
-   Checkout, Java 11 Temurin, Maven compile, Checkstyle, SpotBugs +
    FindSecBugs.
-   Fixed ignored `Statement.executeQuery` result handling, restricted
    CORS through `ALLOWED_ORIGIN`, made Gson static final, added XSS
    servlet suppression with `@SuppressFBWarnings`, and added
    `spotbugs-annotations` `4.9.8` (provided).
-   Passed with BugInstance size 0.

### Secret Scan --- `secrets-scan.yml`

-   Reusable via `workflow_call`.
-   Checkout with `fetch-depth: 0`; Gitleaks action; `GITHUB_TOKEN`.
-   Earlier `workflow_run` trigger design was replaced with reusable
    workflow design.

### Dependency Scan --- `dependency-scan.yml`

-   OWASP Dependency-Check Maven plugin `12.1.0`, Java 17, `NVD_API_KEY`
    secret.
-   Cache: `~/.m2/repository/org/owasp/dependency-check-data`.
-   Uses `-Dformat=ALL`, `-DfailBuildOnCVSS=7`,
    `-DnvdApiKeyEnvironmentVariable=NVD_API_KEY`.
-   OSS Index 401 fixed using exact plugin setting
    `ossindexAnalyzerEnabled=false` (case-sensitive spelling was
    verified using Maven help/effective POM).
-   Passed.

### Dockerfile Scan --- `dockerfile-scan.yml`

-   Hadolint scans all three Dockerfiles.
-   Fixed `DL3008` in `Dockerfile.jlink` by pinning Debian package
    versions listed above.
-   Informational `DL3066` about `USER tomcat` remained below failure
    threshold.
-   Passed.

### Build and Push --- `docker-build-push.yml`

-   Reusable via `workflow_call`.
-   `actions/checkout@v7`, Docker Buildx, Docker Hub login.
-   Builds `Dockerfile.jlink`.
-   Pushes:
    -   `${{ secrets.DOCKERHUB_USERNAME }}/three-tier-app:${{ github.sha }}`
    -   `${{ secrets.DOCKERHUB_USERNAME }}/three-tier-app:latest`
-   Old Trivy block in this workflow was commented out, not active;
    separate `container-scan.yml` handles Trivy.

### Container Scan --- `container-scan.yml`

-   Runs after Build and Push, scanning the pushed SHA image.
-   Trivy action `v0.36.0`; HIGH/CRITICAL; `ignore-unfixed: true`; OS
    and library scanning; SARIF artifact.
-   Earlier `manifest unknown` happened when scanning before the
    SHA-tagged image was pushed. Fixed by ordering Build and Push before
    Container Scan.
-   Integrated pipeline passed.

### DAST --- `dast-scan.yml`

-   Runs ZAP stable container: `ghcr.io/zaproxy/zaproxy:stable`.
-   Starts test app on `8080:8080`, waits with `curl`, creates
    `zap-reports` with `chmod 777`.
-   ZAP uses `--network host`, target `http://127.0.0.1:8080`.
-   Produces JSON, Markdown, HTML reports; `-I` ignores warnings;
    uploads reports and cleans up app container.
-   `host.docker.internal` connectivity failed in runner; host
    networking + `127.0.0.1` fixed it.
-   In isolated DAST setup, app logs showed `db-primary-service` could
    not resolve, but Tomcat started and HTTP was available. Be precise:
    this DAST run tested reachable HTTP behavior, not full DB-backed
    integration.
-   Recorded results: High 0, Medium 2, Low 5, Info 3; `FAIL-NEW: 0`,
    `WARN-NEW: 8`, `PASS: 59`.
-   Warnings included missing CSP, anti-clickjacking, COEP/COOP/CORP,
    Permissions Policy, X-Content-Type-Options, suspicious comments,
    modern web app and cacheable content.

### Deploy --- `deploy.yml`

Secrets referenced: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`,
`EC2_USERNAME`, `EC2_SSH_KEY`, `NVD_API_KEY`, `POSTGRES_DB`,
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_HOST`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD`.

Deployment sequence: 1. Checkout. 2. SSH to EC2. 3. Check/install Docker
(`docker.io`) and Compose (`docker-compose-v2`), enable/start Docker. 4.
Create `~/devops`. 5. SCP `docker-compose.yml` to `~/devops/`. 6. Create
`~/devops/.env` using GitHub secrets plus `DOCKERHUB_USER` and
`DOCKER_TAG=${GITHUB_SHA}`; chmod `600`. 7. Docker Hub login. 8. Run
`docker compose down`, `docker compose pull`,
`docker compose up -d --force-recreate`. 9. Verify with `docker ps` and
`docker compose ps`.

The full Compose app ran successfully on EC2 at port `8081`. Status page
showed App UP, DB UP, DB host `db-primary-service`, DB port `5432`,
database `appdb`, user `appuser`.

Never put actual secret values in README, screenshots, or logs.

## 6. README and screenshot set

README title:
`# 🚀 Three-Tier Java Application — Dockerized, Optimized & DevSecOps CI/CD`

The README was expanded while retaining the original
Dockerization/project history. It includes project overview, badges,
Project at a Glance, Documentation Roadmap, Phase 1 Dockerization,
original docs/setup, Phase 2 DevSecOps pipeline, reusable workflows,
Shift-Left, stage explanations, deployment, secrets, Compose,
architecture, CI/CD proof, AWS deployment, screenshots, challenges,
learning outcomes, portfolio/resume positioning and final summary.

A reviewed version was about 3,021 lines. Re-open the live README before
any further edits; do not assume the repository has not changed. Keep
the existing project story; do not replace it with a short generic
README.

Store screenshots in root `screenshots/` and embed them as Markdown
images, e.g.:
`![DevSecOps CI/CD Pipeline - Successful Run](./screenshots/01-devsecops-pipeline-success.png)`

Expected screenshot names: 1. `01-devsecops-pipeline-success.png` ---
successful full pipeline. 2. `02-github-actions-pipeline.png` ---
pipeline visualization/order. 3. `03-dockerhub-image-sha-tag.png` ---
Docker Hub latest and SHA tags. 4. `04-trivy-container-scan.png` ---
Trivy scan. 5. `05-zap-dast-report.png` --- ZAP result. 6.
`06-aws-application-running.png` --- AWS app on :8081, App UP / DB UP.
7. `07-index-change-cicd-test.png` --- updated `CI/CD v2` page. 8.
`08-docker-compose-ps.png` --- EC2 `docker compose ps`, app Up and
PostgreSQL healthy.

The eighth screenshot replaced an earlier generic `docker ps` screenshot
with the more useful `docker compose ps`.

## 7. Resolved troubleshooting history

-   `.dockerignore` had accidentally excluded `src`.
-   JLink runtime initially missed `org/ietf/jgss/GSSException`; runtime
    modules/dependencies were corrected.
-   App container and DB environment/configuration mismatches were
    fixed.
-   Docker Compose network-label conflict was resolved.
-   Accidental image deletion was recovered.
-   SpotBugs findings were fixed.
-   CORS was restricted through environment configuration.
-   Gitleaks trigger design was changed to `workflow_call`.
-   Dependency-Check OSS Index 401 fixed by disabling its analyzer.
-   Hadolint package pinning fixed.
-   Trivy `manifest unknown` fixed by ensuring image push precedes scan.
-   ZAP networking fixed using host network and `127.0.0.1`.
-   EC2 Docker/Compose setup and deployment automated.
-   Deployment uses immutable SHA image tag, not only mutable `latest`.

## 8. Exact stopping point --- 23 November 2026

The user confirmed: "kk done" after finishing the README and repo
description. Assistant acknowledged the project as a portfolio piece.
User then greeted and asked to create a durable project log so work can
continue in a new chat if the current chat reaches its limit.

No new implementation task has started after
README/repository-description completion. Likely next actions could be
final repo audit, restoring the `push` trigger, running a final pipeline
test, or moving to the next learning/project phase --- but **ask the
user what they want next; do not assume**.

## 9. User's working preferences

-   Be upbeat, direct, practical and hands-on.
-   Teach and troubleshoot incrementally.
-   Use exact live filenames, YAML, env vars, image names/tags and
    ports.
-   When the user shares logs/files, inspect those exact contents before
    suggesting edits.
-   Do not redo completed stages unless asked or a refresher is useful.
-   Do not claim tests passed unless supported by actual results.
-   Preserve existing README content and project history.
-   Provide complete usable commands/configuration and explain why a
    change is required.
-   Preserve `8081:8080`.
-   Current image repository is `aniruddhakharve/three-tier-app`.
-   Use exact workflow filenames listed above.
-   Distinguish isolated DAST HTTP testing from full DB-backed
    integration testing.

## 10. New-chat handoff prompt

Paste this into a new conversation and attach this log:

> Continue my project using the attached **Project Continuation Log ---
> Three-Tier Java + DevSecOps CI/CD** as the source of truth. Read it
> fully first. Repository:
> https://github.com/Aniruddhakharve/three-tier-java-app-dockerize. We
> have Dockerized and optimized the Java/Tomcat/PostgreSQL three-tier
> app, built an eight-stage reusable GitHub Actions DevSecOps pipeline,
> deployed the full Docker Compose stack to AWS EC2, and verified real
> end-to-end CI/CD by changing `index.html` and seeing `CI/CD v2`
> deployed. README and repository description are done. Preserve
> existing implementation/history; do not redo completed work. Use exact
> workflow filenames and keep `8081:8080`. First confirm your
> understanding of the current state, then ask what I want to work on
> next.

------------------------------------------------------------------------

## Memory and continuity

This file is the durable handoff and should be kept in the repository or
another safe location. ChatGPT memory can help carry forward context,
but it is not a guaranteed full transcript archive. Attach this log in a
new chat to restore precise project state.
