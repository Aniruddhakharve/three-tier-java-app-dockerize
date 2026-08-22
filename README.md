# Three-Tier Java Web Application

> ## 🚀 Dockerization & DevOps Implementation
>
> This repository contains my Docker and DevOps implementation built around the original three-tier Java web application.
>
> I took the existing Java/Tomcat/PostgreSQL application and extended it with a complete containerized deployment workflow, including:
>
> - 🐳 Docker containerization
> - 🏗️ Multi-stage Docker builds
> - ☕ Custom Java runtime using `jlink`
> - 🔐 Non-root container execution
> - 🐘 PostgreSQL containerization
> - 🔗 Custom Docker bridge networking
> - 💾 Persistent database storage using Docker volumes
> - 🧩 Docker Compose orchestration
> - ❤️ Database healthchecks
> - 🌱 Environment-based configuration
> - 📦 Docker image optimization
> - 🚀 Docker Hub publishing
> - 🔄 Fresh deployment from Docker Hub
> - 🧪 Application and database health verification
>
> ### 📊 Docker Image Optimization
>
> One of the main goals was to reduce the size of the Java application image.
>
> | Version | Approach | Image Content Size |
> |---|---|---:|
> | `v1` | Tomcat + JDK 11 | ~209 MB |
> | `v2-multistage` | Multi-stage Maven build + Tomcat/JDK 11 | ~209 MB |
> | `v3-jlink` | Multi-stage build + custom `jlink` Java runtime | ~81 MB |
>
> **Result: approximately 61% reduction in final image content size.**
>
> The `v3-jlink` image is published on Docker Hub:
>
> **Docker Hub:** https://hub.docker.com/r/aniruddhakharve/three-tier-java-app
>
> ### 🔗 My Project Repository
>
> https://github.com/Aniruddhakharve/three-tier-java-app-dockerize
>
> ### 🔗 90 Days of DevOps – Day 36
>
> https://github.com/Aniruddhakharve/90DaysOfDevOps-shubham-londe/tree/master/2026/day-36
>
> ---
>
> ## 🧑‍💻 My Contribution
>
> The original application provides the Java three-tier application foundation. My work in this repository focuses on containerization, image optimization, Docker networking, persistent storage, Compose orchestration, troubleshooting, and Docker Hub distribution.
>
> ### Docker/DevOps work completed
>
> 1. Created a Docker image for the Java/Tomcat application.
> 2. Built and tested a PostgreSQL container.
> 3. Created a dedicated Docker bridge network named `three-tier-network`.
> 4. Connected the Java application and PostgreSQL containers through the Docker network.
> 5. Configured database connectivity using environment variables.
> 6. Created a multi-stage Dockerfile using Maven as the builder stage.
> 7. Investigated why the first multi-stage image did not reduce the final image size.
> 8. Created a `jlink`-based custom Java runtime.
> 9. Reduced the application image from approximately 209 MB to approximately 81 MB.
> 10. Configured the final container to run as the `tomcat` user instead of root.
> 11. Created Docker Compose configuration for the application and database.
> 12. Added build context support so Compose can build the application image locally.
> 13. Verified the application through the browser and `/health` endpoint.
> 14. Verified PostgreSQL connectivity from the application.
> 15. Published `latest` and `v3-jlink` images to Docker Hub.
> 16. Documented the implementation and troubleshooting process as part of the 90 Days of DevOps challenge.
>
> ---
>
> ## 🏗️ Containerized Architecture
>
> ```text
>                         Client Browser
>                              │
>                              │ HTTP
>                              ▼
>                  ┌─────────────────────────┐
>                  │      Java Web App       │
>                  │                         │
>                  │      Tomcat 9           │
>                  │      Java 11             │
>                  │      app.war             │
>                  │                         │
>                  │      USER tomcat         │
>                  └────────────┬────────────┘
>                               │
>                               │ JDBC
>                               ▼
>                  ┌─────────────────────────┐
>                  │      PostgreSQL 15      │
>                  │                         │
>                  │      Database: appdb    │
>                  │      User: appuser       │
>                  └────────────┬────────────┘
>                               │
>                               ▼
>                     Docker Named Volume
>
>                 Both containers communicate
>                 through:
>
>                    three-tier-network
> ```
>
> ---
>
> ## 🐳 Docker Build Architecture
>
> ```text
>                       Source Code
>                           │
>                           ▼
>                 ┌─────────────────────┐
>                 │    Builder Stage    │
>                 │                     │
>                 │ Maven 3.9           │
>                 │ JDK 11              │
>                 │                     │
>                 │ mvn clean package   │
>                 └──────────┬──────────┘
>                            │
>                            │ app.war
>                            ▼
>                 ┌─────────────────────┐
>                 │     jlink Stage     │
>                 │                     │
>                 │ Java 11 modules     │
>                 │ selected with       │
>                 │ jdeps + jlink       │
>                 └──────────┬──────────┘
>                            │
>                            │ Minimal JVM
>                            ▼
>                 ┌─────────────────────┐
>                 │   Runtime Stage     │
>                 │                     │
>                 │ Tomcat 9            │
>                 │ Custom Java Runtime │
>                 │ app.war             │
>                 │ USER tomcat         │
>                 └──────────┬──────────┘
>                            │
>                            ▼
>                      Docker Image
>                            │
>                            ▼
>                       Docker Hub
> ```
>
> ---
>
> ## 📦 Docker Images
>
> ### Original Application Image – `v1`
>
> The first Docker image used the Tomcat image containing the complete JDK.
>
> ```text
> three-tier-java-app:v1
> ```
>
> Observed size:
>
> ```text
> ~209 MB content size
> ```
>
> Docker history showed that the majority of the image size came from the Java runtime and underlying Debian/Tomcat layers rather than the application itself.
>
> ---
>
> ### Multi-Stage Image – `v2-multistage`
>
> I then introduced a multi-stage Docker build.
>
> ```text
> Builder:
> Maven + JDK 11
>        │
>        ▼
>     app.war
>        │
>        ▼
> Runtime:
> Tomcat + JDK 11
> ```
>
> Image:
>
> ```text
> three-tier-java-app:v2-multistage
> ```
>
> The final image was still approximately:
>
> ```text
> ~209 MB
> ```
>
> ### Why didn't multi-stage reduce the image size?
>
> Multi-stage builds remove the builder environment from the final image, but the runtime stage was still based on a full Tomcat/JDK image.
>
> Therefore:
>
> ```text
> Builder dependencies removed
>              ↓
> Full JDK still present
>              ↓
> Runtime remains large
> ```
>
> This was an important Docker optimization lesson:
>
> **Multi-stage builds reduce build dependencies, but the final runtime base image still determines most of the final image size.**
>
> ---
>
> ## ⚡ `jlink` Optimization – `v3-jlink`
>
> To reduce the runtime further, I created a custom Java runtime using `jlink`.
>
> The build process identifies the Java modules required by the application and creates a smaller Java runtime containing only those modules.
>
> ```text
> Full JDK
>    │
>    │ jdeps
>    ▼
> Required Java Modules
>    │
>    │ jlink
>    ▼
> Custom Java Runtime
> ```
>
> The resulting image:
>
> ```text
> three-tier-java-app:v3-jlink
> ```
>
> Final observed image content size:
>
> ```text
> ~81 MB
> ```
>
> This represents an approximate reduction of:
>
> ```text
> 209 MB → 81 MB
> ```
>
> or approximately:
>
> ```text
> 61% reduction
> ```
>
> ### Verification
>
> ```bash
> docker image inspect three-tier-java-app:v3-jlink \
>   --format 'Size: {{.Size}} bytes'
> ```
>
> Example observed result:
>
> ```text
> Size: 81307174 bytes
> ```
>
> The final container was also verified to run as:
>
> ```bash
> docker inspect three-tier-java-app:v3-jlink \
>   --format 'User: {{.Config.User}}'
> ```
>
> Result:
>
> ```text
> User: tomcat
> ```
>
> ---
>
> ## 🔐 Non-Root Container
>
> The final runtime image does not run the application as root.
>
> ```text
> User: tomcat
> ```
>
> This follows the container security principle of using the least privilege necessary for the application.
>
> ---
>
> ## 🐘 PostgreSQL Container
>
> PostgreSQL 15 was containerized separately from the Java application.
>
> Example configuration used during testing:
>
> ```text
> POSTGRES_DB=appdb
> POSTGRES_USER=appuser
> POSTGRES_PASSWORD=appsecret
> ```
>
> The database was verified directly from the container:
>
> ```bash
> docker exec -it day36-postgres-test psql -U appuser -d appdb
> ```
>
> PostgreSQL confirmed:
>
> ```text
> You are connected to database "appdb" as user "appuser"
> ```
>
> The database list also confirmed the expected `appdb` database.
>
> ---
>
> ## 🔗 Custom Docker Network
>
> A dedicated bridge network was created:
>
> ```text
> three-tier-network
> ```
>
> The network used:
>
> ```text
> Subnet: 172.18.0.0/16
> Gateway: 172.18.0.1
> ```
>
> The containers were attached to the same network:
>
> ```text
> PostgreSQL
>     172.18.0.2
>
> Java Application
>     172.18.0.3
> ```
>
> This allowed the application container to communicate with PostgreSQL through Docker's internal networking instead of relying on the host machine.
>
> Example:
>
> ```bash
> docker network inspect three-tier-network
> ```
>
> ---
>
> ## 💾 Database Persistence
>
> PostgreSQL was configured to use persistent Docker storage so that database data does not depend on the lifecycle of a single PostgreSQL container.
>
> ```text
> PostgreSQL Container
>         │
>         ▼
>    Named Volume
>         │
>         ▼
> Persistent Database Data
> ```
>
> This means removing and recreating the database container does not inherently remove the database data when the named volume is retained.
>
> ---
>
> ## 🧩 Docker Compose
>
> Docker Compose was used to orchestrate the application and PostgreSQL services.
>
> The application service was configured to build from the custom Dockerfile:
>
> ```yaml
> app:
>   build:
>     context: .
>     dockerfile: Dockerfile.jlink
> ```
>
> This allowed the Compose workflow to build the optimized application image directly from the project source.
>
> Start the stack:
>
> ```bash
> docker compose up -d
> ```
>
> Check services:
>
> ```bash
> docker compose ps
> ```
>
> View logs:
>
> ```bash
> docker compose logs
> ```
>
> View application logs:
>
> ```bash
> docker compose logs app
> ```
>
> View database logs:
>
> ```bash
> docker compose logs db
> ```
>
> Stop the stack:
>
> ```bash
> docker compose down
> ```
>
> Rebuild the application:
>
> ```bash
> docker compose up -d --build
> ```
>
> ---
>
> ## ❤️ Health Verification
>
> The application includes a health endpoint:
>
> ```text
> /health
> ```
>
> The application was successfully verified through the browser using the health endpoint.
>
> The health verification confirmed that:
>
> - The Java/Tomcat application was running.
> - The application deployment succeeded.
> - PostgreSQL was running.
> - The application could communicate with the database.
> - Database connectivity was successful.
>
> ---
>
> ## 🧪 End-to-End Validation
>
> The final stack was validated through multiple layers.
>
> ```text
> Docker Compose
>       │
>       ├── Java/Tomcat Container
>       │       │
>       │       └── /health
>       │
>       └── PostgreSQL Container
>               │
>               └── appdb
> ```
>
> Validation included:
>
> ```text
> ✓ Docker image builds successfully
> ✓ Java application starts successfully
> ✓ Tomcat deploys app.war
> ✓ PostgreSQL starts successfully
> ✓ Database appdb exists
> ✓ appuser can connect to appdb
> ✓ Containers share three-tier-network
> ✓ Java application communicates with PostgreSQL
> ✓ /health endpoint responds successfully
> ✓ Browser access verified
> ✓ Docker Compose builds and starts the stack
> ✓ Optimized image runs successfully
> ✓ Docker Hub image published successfully
> ```
>
> ---
>
> ## 🐞 Problems Encountered & Solutions
>
> This project was not just about building the image; several real Docker and containerization issues were encountered and resolved during implementation.
>
> ### 1. Multi-stage build did not initially reduce the image size
>
> The first multi-stage image was approximately the same size as the original image:
>
> ```text
> v1              ~209 MB
> v2-multistage   ~209 MB
> ```
>
> Investigation of `docker history` showed that the runtime image still contained the full JDK and large Tomcat base layers.
>
> **Solution:**
>
> Use `jlink` to create a minimal Java runtime containing only the required Java modules.
>
> Result:
>
> ```text
> ~209 MB → ~81 MB
> ```
>
> ---
>
> ### 2. `.dockerignore` accidentally excluded `src`
>
> During the first multi-stage build, Docker reported:
>
> ```text
> CopyIgnoredFile: Attempting to Copy file "src" that is excluded by .dockerignore
> ```
>
> The build failed at:
>
> ```dockerfile
> COPY src ./src
> ```
>
> **Cause:**
>
> The `.dockerignore` file was excluding the source directory.
>
> **Solution:**
>
> Correct the `.dockerignore` configuration so that the application source code remains part of the Docker build context while unnecessary files such as build artifacts remain excluded.
>
> ---
>
> ### 3. `jlink` runtime initially failed to start Tomcat
>
> The first optimized image exited with:
>
> ```text
> NoClassDefFoundError: org/ietf/jgss/GSSException
> ```
>
> Tomcat could not start because the custom runtime did not contain a Java module required by Tomcat.
>
> **Solution:**
>
> Revisit the Java module requirements and include the missing runtime module when generating the `jlink` image.
>
> After correcting the runtime, the application started successfully and Tomcat deployed the WAR.
>
> This demonstrated an important lesson:
>
> **A smaller Java runtime is useful only when it contains every module required by the actual application server and application.**
>
> ---
>
> ### 4. Application container was initially stopped
>
> During Docker networking verification:
>
> ```bash
> docker exec day36-java-app getent hosts db-primary-service
> ```
>
> Docker returned:
>
> ```text
> container ... is not running
> ```
>
> **Solution:**
>
> Inspect the container state and logs:
>
> ```bash
> docker ps -a
> docker logs day36-java-app
> ```
>
> After fixing the runtime configuration, the Java application container remained running and networking could be tested successfully.
>
> ---
>
> ### 5. PostgreSQL connection configuration mismatch
>
> PostgreSQL was configured with:
>
> ```text
> POSTGRES_DB=appdb
> POSTGRES_USER=appuser
> POSTGRES_PASSWORD=appsecret
> ```
>
> The application configuration was also reviewed to ensure that the correct database hostname, database name, username, and password were being supplied through environment variables.
>
> The application source uses environment variables such as:
>
> ```text
> DB_HOST
> DB_NAME
> DB_USER
> DB_PASSWORD
> ```
>
> **Solution:**
>
> Verify the environment variables inside the PostgreSQL container and align the application configuration with the Compose service/network configuration.
>
> ---
>
> ### 6. Existing Docker network caused a Compose label conflict
>
> Docker Compose reported:
>
> ```text
> network three-tier-network was found but has incorrect label
> com.docker.compose.network set to ""
> expected: "three-tier-network"
> ```
>
> **Cause:**
>
> A Docker network with the same name already existed but had not been created with the labels expected by the current Compose project.
>
> **Solution:**
>
> Manage the network through Docker Compose or remove/recreate the conflicting manually-created network when appropriate.
>
> The important lesson is that a Docker network's name alone does not necessarily mean it is managed by the current Compose project.
>
> ---
>
> ### 7. Docker Compose build configuration
>
> The application service was initially referenced through an existing image.
>
> The Compose configuration was then changed to build the optimized image directly:
>
> ```yaml
> app:
>   build:
>     context: .
>     dockerfile: Dockerfile.jlink
> ```
>
> This successfully built the image and the application was verified in the browser.
>
> This makes the Compose project more reproducible because the application image can be rebuilt from the repository source.
>
> ---
>
> ### 8. Accidentally deleted the optimized image
>
> During image cleanup, the optimized image was accidentally removed:
>
> ```text
> three-tier-java-app:v3-jlink
> ```
>
> The image had not yet been pushed to Docker Hub at that point.
>
> **Solution:**
>
> Rebuild the optimized image from the `Dockerfile.jlink`, verify the resulting image, and publish it to Docker Hub.
>
> This was also a useful demonstration of why publishing important build artifacts and maintaining reproducible Dockerfiles matters.
>
> ---
>
> ## 📊 Final Optimization Result
>
> ```text
> ┌───────────────────────────────┐
> │ Original v1                   │
> │ ~209 MB                       │
> └───────────────┬───────────────┘
>                 │
>                 │ Multi-stage build
>                 ▼
> ┌───────────────────────────────┐
> │ v2-multistage                 │
> │ ~209 MB                       │
> │                               │
> │ Builder removed, but full     │
> │ JDK remains in runtime        │
> └───────────────┬───────────────┘
>                 │
>                 │ jdeps + jlink
>                 ▼
> ┌───────────────────────────────┐
> │ v3-jlink                      │
> │ ~81 MB                        │
> │                               │
> │ Custom minimal Java runtime   │
> └───────────────────────────────┘
> ```
>
> ### Optimization summary
>
> ```text
> Before:
> ~209 MB
>
> After:
> ~81 MB
>
> Reduction:
> ~128 MB
>
> Percentage:
> ~61%
> ```
>
> ---
>
> ## 🚀 Docker Hub
>
> The optimized image was published to Docker Hub with the following tags:
>
> ```text
> latest
> v3-jlink
> ```
>
> Docker Hub repository:
>
> https://hub.docker.com/r/aniruddhakharve/three-tier-java-app
>
> Pull the optimized version:
>
> ```bash
> docker pull aniruddhakharve/three-tier-java-app:v3-jlink
> ```
>
> Pull the latest version:
>
> ```bash
> docker pull aniruddhakharve/three-tier-java-app:latest
> ```
>
> ---
>
> ## 🔄 Reproducible Deployment
>
> One of the final goals was to ensure that the application could be deployed without depending on the locally-built image.
>
> The intended workflow is:
>
> ```text
> GitHub Repository
>        │
>        ▼
> Dockerfile.jlink
>        │
>        ▼
> Docker Image
>        │
>        ▼
> Docker Hub
>        │
>        ▼
> Fresh Machine
>        │
>        ▼
> docker pull
>        │
>        ▼
> docker compose up
>        │
>        ▼
> Running Application
> ```
>
> This demonstrates the basic DevOps principle of producing a reproducible deployment artifact rather than relying on a manually configured local environment.
>
> ---
>
> ## 📸 Project Screenshots
>
> Implementation screenshots from the Dockerization process are maintained in the 90 Days of DevOps Day 36 documentation:
>
> https://github.com/Aniruddhakharve/90DaysOfDevOps-shubham-londe/tree/master/2026/day-36
>
> The screenshots document the major stages of:
>
> - Docker image creation
> - PostgreSQL container setup
> - Database verification
> - Docker network configuration
> - Java application deployment
> - `jlink` optimization
> - Image size comparison
> - Docker Compose deployment
> - Browser verification
> - Health endpoint verification
> - Docker Hub publishing
>
> ---
>
> ## 📁 Docker/DevOps Files
>
> In addition to the original Java application files, the Docker implementation includes files such as:
>
> ```text
> three-tier-java-app-dockerize/
> │
> ├── Dockerfile
> ├── Dockerfile.multistage
> ├── Dockerfile.jlink
> ├── docker-compose.yml
> ├── .dockerignore
> ├── pom.xml
> ├── README.md
> │
> ├── scripts/
> │   ├── build.sh
> │   ├── deploy.sh
> │   └── verify-setup.sh
> │
> └── src/
>     └── main/
>         ├── java/
>         └── webapp/
> ```
>
> ---
>
> ## 🛠️ DevOps Skills Demonstrated
>
> | Area | Technologies / Concepts |
> |---|---|
> | Containerization | Docker |
> | Image Building | Dockerfile, BuildKit |
> | Image Optimization | Multi-stage builds, `jlink`, `jdeps` |
> | Java | Java 11 |
> | Application Server | Apache Tomcat 9 |
> | Build Tool | Apache Maven |
> | Database | PostgreSQL 15 |
> | Orchestration | Docker Compose |
> | Networking | Docker Bridge Network, Container DNS |
> | Storage | Docker Named Volumes |
> | Configuration | Environment Variables |
> | Security | Non-root container |
> | Distribution | Docker Hub |
> | Troubleshooting | Docker logs, inspect, history, network inspection |
> | Verification | Health endpoint, browser testing, PostgreSQL CLI |
>
> ---
>
> ## 💼 Portfolio / Resume Description
>
> **Three-Tier Java Application – Dockerized & Optimized Deployment**
>
> Dockerized a Java Servlet/Tomcat three-tier web application with PostgreSQL using Docker and Docker Compose. Implemented multi-stage builds, a custom `jlink` Java 11 runtime, non-root execution, persistent database volumes, custom Docker networking, environment-based configuration, and container health verification. Reduced the application image from approximately 209 MB to ~81 MB (~61% reduction) and published versioned images to Docker Hub.
>
> ---
>
> ## 🎯 Key Learning Outcomes
>
> This project provided hands-on experience with several concepts that are directly applicable to real-world DevOps work:
>
> - Understanding the difference between build-time and runtime dependencies.
> - Understanding why multi-stage builds do not automatically produce small images.
> - Optimizing Java container images using `jdeps` and `jlink`.
> - Running application containers as non-root users.
> - Connecting application and database containers through Docker networking.
> - Persisting database data independently of container lifecycle.
> - Using Docker Compose to define repeatable multi-container environments.
> - Diagnosing container startup failures through logs.
> - Debugging missing Java modules in custom runtimes.
> - Understanding Docker build contexts and `.dockerignore`.
> - Understanding Docker Compose network ownership and labels.
> - Publishing images to Docker Hub for distribution.
> - Testing an application from the perspective of a fresh deployment.
>
> ---
>
> <!-- The original project documentation below is retained from the original project README. Docker/DevOps implementation sections above document my work on top of the original application. Source: :contentReference[oaicite:0]{index=0} -->
>
> ## Project Overview
>
> This repository contains a Java web application that demonstrates a complete three-tier architecture pattern. The application is specifically designed for deployment on IBM Cloud Red Hat OpenShift Kubernetes Service (ROKS) with OpenShift Virtualization. See [ocp-v-3-tier-app](https://github.com/neil1taylor/ocp-v-3-tier-app)
>
> The application implements a user management system with a responsive web interface, RESTful API endpoints, and PostgreSQL database integration. It showcases the separation of concerns across the three tiers while providing a practical example of enterprise application architecture.
>
> ![alt text](3-tier-app.jpg)
>
> ### Purpose
>
> This project serves several purposes:
>
> 1. **Educational Resource**: Demonstrates implementing a three-tier architecture in Java
> 2. **Reference Implementation**: Provides a template for building scalable web applications
> 3. **Deployment Example**: Can be used as a multi-tier application on OpenShift Virtualization
>
> ## Three-Tier Architecture
>
> This application implements the classic three-tier architecture pattern, which separates the application into three logical and physical computing tiers:
>
> ### Architecture Overview
>
> ```
> Client Browser → Web Tier (NGINX) → Application Tier (Tomcat/Java) → Database Tier (PostgreSQL)
> ```
>
> The application is structured into three distinct tiers:
>
> 1. **Web Tier (Presentation Layer)**
>    - **Technology**: NGINX web server
>    - **Purpose**: Serves static content, handles HTTP requests, and forwards dynamic requests to the application tier
>    - **Components**: HTML, CSS, JavaScript files
>    - **Responsibilities**: User interface rendering, client-side validation, AJAX requests to the API
>
> 2. **Application Tier (Business Logic Layer)**
>    - **Technology**: Apache Tomcat with Java Servlets
>    - **Purpose**: Processes business logic, handles API requests, and manages communication with the database tier
>    - **Components**: Java servlets, business logic classes, data models
>    - **Responsibilities**: Request processing, data validation, business rule enforcement, transaction management
>
> 3. **Database Tier (Data Access Layer)**
>    - **Technology**: PostgreSQL database
>    - **Purpose**: Stores and manages application data
>    - **Components**: Database tables, indexes, constraints
>    - **Responsibilities**: Data storage, data integrity, query processing
>
> This GitHub repository focus is on the Application Tier. The web and database tiers are installed and configured via cloud-init in [ocp-v-3-tier-app](https://github.com/neil1taylor/ocp-v-3-tier-app).
>
> ### Benefits of Three-Tier Architecture
>
> - **Separation of Concerns**: Each tier has a specific responsibility, making the codebase more maintainable
> - **Scalability**: Each tier can be scaled independently based on specific requirements
> - **Security**: Sensitive operations and data can be isolated in the appropriate tier
> - **Flexibility**: Components within each tier can be modified or replaced without affecting other tiers
> - **Performance**: Optimizations can be applied to specific tiers as needed
>
> ## Core Technologies
>
> ### Java
> Java is a general-purpose, class-based, object-oriented programming language designed to have as few implementation dependencies as possible. It is a computing platform for application development that was first released by Sun Microsystems in 1995 and later acquired by Oracle Corporation.
>
> Key characteristics of Java include:
> - **Platform Independence**: Java follows the "write once, run anywhere" (WORA) principle, allowing code to run on any device with a Java Virtual Machine (JVM), regardless of the underlying hardware and operating system.
> - **Object-Oriented**: Java's object-oriented nature encourages modular and reusable code through concepts like encapsulation, inheritance, and polymorphism.
> - **Robust and Secure**: Java provides automatic memory management, strong type checking, and exception handling, making applications more robust and secure.
>
> In this three-tier application, Java serves as the foundation for the application tier, powering the business logic through servlets that process requests, implement business rules, and coordinate communication between the presentation and data tiers.
>
> ### Maven
> Maven is a powerful build automation and dependency management tool primarily used for Java projects. Developed by the Apache Software Foundation, Maven addresses two critical aspects of software development: how software is built and how dependencies are managed.
>
> Key features of Maven include:
> - **Dependency Management**: Maven automatically downloads and manages Java libraries and plugins required by the project, ensuring version compatibility.
> - **Standardized Build Lifecycle**: Maven defines a standard build lifecycle that includes phases like compile, test, package, install, and deploy.
> - **Project Object Model (POM)**: Projects are configured using a pom.xml file that defines project dependencies, build plugins, goals, and other settings.
>
> In this project, Maven manages all dependencies (such as servlet APIs, PostgreSQL drivers, and JSON libraries), standardizes the build process, and packages the application as a WAR file ready for deployment to Tomcat.
>
> ### Apache Tomcat
> Apache Tomcat is an open-source web server and servlet container developed by the Apache Software Foundation. It implements the Java Servlet, JavaServer Pages (JSP), WebSocket, and Java Expression Language specifications, providing a "pure Java" HTTP web server environment for Java code to run.
>
> Key aspects of Tomcat include:
> - **Servlet Container**: Tomcat provides a runtime environment for Java servlets, managing their lifecycle and providing access to the HTTP request/response objects.
> - **Web Server Capabilities**: While primarily a servlet container, Tomcat also functions as a web server capable of serving static content.
> - **Lightweight and Configurable**: Compared to full Java EE application servers, Tomcat is lightweight and easily configurable, making it ideal for a wide range of applications.
>
> In this three-tier application, Tomcat serves as the application server in the middle tier, hosting the Java servlets that process business logic and API requests. It manages HTTP connections, routes requests to the appropriate servlets, and handles the servlet lifecycle, allowing the application to focus on implementing business functionality rather than low-level HTTP processing.
>
> ## File Analysis
>
> This section provides a comprehensive description of each file in the project and its purpose within the three-tier architecture.
>
> ### Project Configuration Files
>
> #### pom.xml
> - **Purpose**: Maven Project Object Model file that defines project configuration, dependencies, and build settings
> - **Role**: Cross-tier configuration that affects all layers of the application
> - **Key Features**:
>   - Specifies dependencies for all tiers (servlet API, PostgreSQL, Gson, logging)
>   - Configures the build process and packaging format (WAR)
>   - Defines project metadata and version information
>
> #### .gitignore
> - **Purpose**: Specifies files and directories to be excluded from version control
> - **Role**: Development utility that spans all tiers
> - **Key Features**:
>   - Prevents build artifacts, IDE files, logs, and environment-specific configurations from being committed
>   - Ensures clean repository structure and prevents sensitive information from being shared
>
> ### Build and Deployment Scripts
>
> #### scripts/build.sh
> - **Purpose**: Automates the build process using Maven
> - **Role**: Development/deployment utility that spans all tiers
> - **Key Features**:
>   - Sets up Java environment (Java 17)
>   - Configures Maven options
>   - Builds the application using Maven
>   - Verifies the build output (WAR file)
>
> #### scripts/deploy.sh
> - **Purpose**: Automates the deployment process to Apache Tomcat
> - **Role**: Deployment utility primarily for the application tier
> - **Key Features**:
>   - Sets up Java environment
>   - Installs and configures Apache Tomcat
>   - Creates a systemd service for Tomcat
>   - Configures SELinux and firewall settings
>   - Deploys the WAR file to Tomcat
>   - Starts the Tomcat service and verifies the deployment
>
> #### scripts/verify-setup.sh
> - **Purpose**: Verifies the repository structure and required files
> - **Role**: Development utility that spans all tiers
> - **Key Features**:
>   - Checks directory structure
>   - Verifies presence of required files
>   - Validates script permissions
>   - Ensures proper setup before building or deploying
>
> ### Presentation Tier Components
>
> #### src/main/webapp/index.html
> - **Purpose**: Main entry point for the web application
> - **Role**: Presentation tier component that provides the user interface
> - **Key Features**:
>   - Responsive user interface with CSS styling
>   - User management form for adding new users
>   - Users directory for displaying existing users
>   - Health status display
>   - JavaScript functions for API interaction and UI updates
>   - Makes AJAX requests to UserServlet and HealthServlet
>
> #### src/main/webapp/WEB-INF/web.xml
> - **Purpose**: Java web application deployment descriptor
> - **Role**: Configuration file for the presentation and application tiers
> - **Key Features**:
>   - Servlet definitions and mappings
>   - Welcome file configuration
>   - Error page definitions
>   - Security constraints for API endpoints
>   - Maps servlet classes to URL patterns
>   - Configures servlet initialization parameters
>
> ### Application Tier Components
>
> #### src/main/java/com/threetier/webapp/UserServlet.java
> - **Purpose**: REST API servlet for user management
> - **Role**: Application tier component that handles user-related API requests
> - **Key Features**:
>   - Handles GET requests to retrieve all users
>   - Processes POST requests to create new users
>   - Initializes database schema on startup
>   - Error handling and JSON response formatting
>   - Uses DatabaseConnection for database operations
>   - Uses User class for data representation and validation
>
> #### src/main/java/com/threetier/webapp/HealthServlet.java
> - **Purpose**: Health check servlet for monitoring application and database status
> - **Role**: Application tier component that provides system health information
> - **Key Features**:
>   - Checks application status
>   - Tests database connectivity using DatabaseConnection
>   - Returns detailed health status in JSON format
>   - Sets appropriate HTTP status codes
>   - Provides real-time health information about the application
>
> #### src/main/java/com/threetier/webapp/User.java
> - **Purpose**: Model class representing a user entity
> - **Role**: Application tier component that defines the data model
> - **Key Features**:
>   - User attributes (id, name, email, timestamps)
>   - Data validation methods
>   - Object comparison and string representation utilities
>   - Used by UserServlet to validate and represent user data
>
> ### Data Tier Components
>
> #### src/main/java/com/threetier/webapp/DatabaseConnection.java
> - **Purpose**: Utility class for managing PostgreSQL database connections
> - **Role**: Data tier component responsible for database connectivity
> - **Key Features**:
>   - Configurable database connection parameters via environment variables
>   - Database schema initialization with users table
>   - Connection testing functionality
>   - Debugging information retrieval
>   - Used by UserServlet and HealthServlet to establish database connections
>
> ### Database Structure
>
> The database tier uses PostgreSQL and consists of:
>
> - **Users Table**:
>   - `id`: Primary key, auto-incrementing
>   - `name`: User's full name
>   - `email`: User's email address (unique)
>   - `created_at`: Timestamp of user creation
>   - `updated_at`: Timestamp of last update
>
> - **Indexes**:
>   - Email index for faster lookups
>
> ## Component Interactions
>
> ### Data Flow
>
> 1. **Client to Web Tier**:
>    - User interacts with the web interface
>    - Browser sends HTTP requests to the server
>    - JavaScript handles form submissions and UI updates
>
> 2. **Web Tier to Application Tier**:
>    - AJAX requests are sent to the application's REST API endpoints
>    - API requests are processed by the appropriate servlet
>
> 3. **Application Tier to Database Tier**:
>    - Servlets use the DatabaseConnection utility to interact with the database
>    - SQL queries are executed to retrieve or modify users
>    - Results are processed and transformed into Java objects
>
> 4. **Response Path**:
>    - Database returns query results to the application tier
>    - Application tier formats the data (typically as JSON)
>    - Web tier receives the response and updates the UI accordingly
>
> ### API Endpoints
>
> - **`GET /api/users/`**
>   - **Purpose**: Retrieve a list of all users
>   - **Response**: JSON array of user objects
>   - **Implementation**: `UserServlet.doGet()`
>
> - **`POST /api/users/`**
>   - **Purpose**: Create a new user
>   - **Parameters**: `name` (string), `email` (string)
>   - **Response**: JSON object of the created user
>   - **Implementation**: `UserServlet.doPost()`
>
> - **`GET /health` or `GET /api/system-health`**
>   - **Purpose**: Check system health status
>   - **Response**: JSON object with application and database status
>   - **Implementation**: `HealthServlet.doGet()`
>
> ## Setup and Installation
>
> ### Prerequisites
>
> - Java 11 or higher (Java 17 recommended)
> - Maven 3.6+
> - PostgreSQL database
> - Apache Tomcat 9.x (for local deployment)
> - NGINX (for production deployment)
>
> ### Environment Setup
>
> 1. **Clone the repository**:
>    ```bash
>    git clone https://github.com/yourusername/three-tier-java-app.git
>    cd three-tier-java-app
>    ```
>
> 2. **Verify the setup**:
>    ```bash
>    chmod +x scripts/verify-setup.sh
>    ./scripts/verify-setup.sh
>    ```
>
> 3. **Configure database connection**:
>    
>    Set the following environment variables to configure the database connection:
>    ```bash
>    export DB_HOST=localhost
>    export DB_PORT=5432
>    export DB_NAME=appdb
>    export DB_USER=appuser
>    export DB_PASSWORD=apppassword
>    ```
>
>    Alternatively, the application will use default values if these are not set.
>
> 4. **Create PostgreSQL database**:
>    ```bash
>    sudo -u postgres psql
>    CREATE DATABASE appdb;
>    CREATE USER appuser WITH ENCRYPTED PASSWORD 'apppassword';
>    GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;
>    \q
>    ```
>
> ## Build and Deployment
>
> ### Build Process
>
> The application uses Maven for building and packaging. The build process is automated through the `build.sh` script:
>
> 1. **Make the build script executable**:
>    ```bash
>    chmod +x scripts/build.sh
>    ```
>
> 2. **Run the build script**:
>    ```bash
>    ./scripts/build.sh
>    ```
>
> The build script performs the following actions:
> - Sets up the Java environment (Java 17)
> - Configures Maven options
> - Builds the application using Maven
> - Verifies the build output (WAR file)
>
> ### Deployment Process
>
> The deployment process is automated through the `deploy.sh` script:
>
> 1. **Make the deployment script executable**:
>    ```bash
>    chmod +x scripts/deploy.sh
>    ```
>
> 2. **Run the deployment script**:
>    ```bash
>    ./scripts/deploy.sh
>    ```
>
> The deployment script performs the following actions:
> - Sets up the Java environment
> - Installs and configures Apache Tomcat
> - Creates a systemd service for Tomcat
> - Configures SELinux and firewall settings
> - Deploys the WAR file to Tomcat
> - Starts the Tomcat service
> - Verifies the deployment
>
> ### Deployment to OpenShift
>
> The app has been designed to be built and deployed via a deployment to IBM Cloud ROKS with OpenShift Virtualization. See the [ocp-v-3-tier-app](https://github.com/neil1taylor/ocp-v-3-tier-app) GutHub repository.
>
> ## Environment Variables
>
> The application uses the following environment variables for configuration:
>
> | Variable | Description | Default Value |
> |----------|-------------|---------------|
> | `DB_HOST` | Database host | `db-primary-service` |
> | `DB_PORT` | Database port | `5432` |
> | `DB_NAME` | Database name | `appdb` |
> | `DB_USER` | Database user | `appuser` |
> | `DB_PASSWORD` | Database password | `apppassword` |
>
> ## Troubleshooting
>
> ### Common Issues
>
> 1. **Database Connection Failures**:
>    - Verify that the PostgreSQL service is running
>    - Check the database credentials in environment variables
>    - Ensure network connectivity to the database host
>
> 2. **Application Deployment Issues**:
>    - Check Tomcat logs: `tail -f /opt/tomcat/logs/catalina.out`
>    - Verify the WAR file was built correctly
>    - Ensure proper permissions on Tomcat directories
>
> 3. **Web Interface Not Loading**:
>    - Check browser console for JavaScript errors
>    - Verify that the application tier is responding to API requests
>    - Check NGINX configuration and logs
>
> ### Health Check
>
> The application provides a health check endpoint at `/health` or `/api/system-health` that can be used to diagnose issues:
>
> ```bash
> curl http://localhost:8080/health
> ```
>
> The response includes:
> - Overall system status
> - Application status
> - Database connection status
> - Database connection details
>
> ## Features
>
> - RESTful API for user management
> - Real-time health monitoring
> - Database connectivity with PostgreSQL
> - Responsive web interface
> - Automatic error handling
> - Cross-tier communication demonstration
>
> ## Repository Structure
>
> ```
> three-tier-java-app/
> ├── README.md                                     # Project documentation
> ├── pom.xml                                       # Maven project configuration
> ├── .gitignore                                    # Git ignore file
> ├── 3-tier-app.jpg                                # Architecture diagram
> ├── LICENSE                                       # MIT License file
> ├── scripts/                                      # Build and deployment scripts
> │   ├── build.sh                                  # Automated build script
> │   ├── deploy.sh                                 # Automated deployment script
> │   └── verify-setup.sh                           # Repository verification script
> └── src/                                          # Source code
>     └── main/
>         ├── java/com/threetier/webapp/            # Java application code
>         │   ├── DatabaseConnection.java           # Database connectivity (Data Tier)
>         │   ├── User.java                         # User model (Application Tier)
>         │   ├── UserServlet.java                  # User API endpoint (Application Tier)
>         │   └── HealthServlet.java                # Health check endpoint (Application Tier)
>         └── webapp/                               # Web application resources
>             ├── index.html                        # Main web interface (Presentation Tier)
>             └── WEB-INF/web.xml                   # Web application configuration
> ```
>
> ## License
>
> This project is licensed under the MIT License - see the LICENSE file for details.
>
> ---
>
> ## 📌 Project Attribution
>
> The underlying Java three-tier application was originally created by the original project author. This repository preserves the original application documentation and license while adding my Dockerization and DevOps implementation.
>
> **Original project:** https://github.com/neil1taylor/three-tier-java-app
>
> **My Dockerized repository:** https://github.com/Aniruddhakharve/three-tier-java-app-dockerize
>
> **90 Days of DevOps documentation:** https://github.com/Aniruddhakharve/90DaysOfDevOps-shubham-londe/tree/master/2026/day-36
>
> **Docker Hub:** https://hub.docker.com/r/aniruddhakharve/three-tier-java-app
>
> ---
>
> ## ⭐ Final Project Summary
>
> This project demonstrates the progression from a traditional Java web application to a containerized and optimized deployment:
>
> ```text
> Original Java Application
>          │
>          ▼
> Maven WAR Build
>          │
>          ▼
> Dockerized Tomcat Application
>          │
>          ▼
> PostgreSQL Container
>          │
>          ▼
> Docker Network + Persistent Volume
>          │
>          ▼
> Docker Compose
>          │
>          ▼
> Multi-Stage Docker Build
>          │
>          ▼
> jlink Custom Java Runtime
>          │
>          ▼
> ~81 MB Optimized Image
>          │
>          ▼
> Non-Root Runtime
>          │
>          ▼
> Docker Hub
>          │
>          ▼
> Reproducible Container Deployment
> ```
>
> **This repository demonstrates practical Docker and DevOps skills through the containerization, optimization, networking, persistence, troubleshooting, and distribution of a real Java three-tier application.**
