# Kubernetes Deployment – Three-Tier Java Application

This directory contains the Kubernetes manifests and configuration used to deploy and manage the three-tier Java application on a local KIND Kubernetes cluster.

The deployment includes a Java application, PostgreSQL database, persistent storage, autoscaling, Metrics Server, and Headlamp for cluster visualization and management.

## Architecture

```text
                    Developer / User
                           |
                           |
                    KIND Cluster
                           |
                 +-------------------+
                 |   Java Service    |
                 |     NodePort      |
                 |     30080         |
                 +-------------------+
                           |
                           |
                 +-------------------+
                 |   Java Deployment |
                 |    2 Replicas     |
                 +-------------------+
                           |
                           |
                 +-------------------+
                 | PostgreSQL Service|
                 |     ClusterIP     |
                 +-------------------+
                           |
                           |
                 +-------------------+
                 | PostgreSQL        |
                 | StatefulSet       |
                 | Persistent Volume |
                 +-------------------+

       Metrics Server → HPA → Java Deployment

       Headlamp → Kubernetes API
```

## Environment

| Component | Configuration |
|---|---|
| Kubernetes distribution | KIND |
| Cluster name | `java-cluster` |
| Kubernetes version | v1.37.0 |
| Cluster nodes | 1 control-plane, 1 worker |
| Application namespace | `three-tier-app` |
| Headlamp namespace | `headlamp` |
| Helm practice namespace | `helm-practice` |

## Kubernetes Resources

### Java Application

- Deployment manages 2 application replicas.
- NodePort Service exposes the application on port `30080`.
- RollingUpdate deployment strategy.
- Horizontal Pod Autoscaler manages application replicas.

### PostgreSQL Database

- StatefulSet manages the PostgreSQL Pod.
- Headless Service provides stable network identity.
- ClusterIP Service provides internal application connectivity.
- PersistentVolumeClaim provides 5 GiB of persistent storage.
- Access mode: `ReadWriteOnce` (RWO).

### Autoscaling

The Java application uses a Horizontal Pod Autoscaler.

| Configuration | Value |
|---|---:|
| Minimum replicas | 2 |
| Maximum replicas | 5 |
| Target CPU utilization | 60% |
| Current CPU utilization (observed) | 1% |
| Current replicas (observed) | 2 |

Metrics Server provides resource metrics used by the HPA.

### Headlamp

Headlamp is deployed using its official Helm chart in a separate namespace.

Custom Kubernetes RBAC is configured with two identities:

- Read-only ServiceAccount
- Admin ServiceAccount

The read-only identity can inspect permitted cluster resources but cannot create, modify, or delete them. The admin identity is bound to the built-in `cluster-admin` ClusterRole.

See [Headlamp documentation](manifests/headlamp/README.md) for installation and RBAC details.

## Repository Structure

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

**Security note:** `secret.yml` contains sensitive Kubernetes configuration and is excluded from Git using `.gitignore`. Do not commit database passwords, tokens, or other credentials.

## Prerequisites

Install the following tools:

- Docker
- KIND
- kubectl
- Helm

Verify installations:

```bash
docker --version
kind version
kubectl version --client
helm version
```

## Deployment

### 1. Create the KIND cluster

```bash
kind create cluster --config k8s/kind-config.yml
```

Verify:

```bash
kubectl get nodes
kubectl config current-context
```

### 2. Create the application namespace

```bash
kubectl apply -f k8s/manifests/namespace.yaml
```

### 3. Apply application configuration and database resources

Ensure your local `secret.yml` is configured before applying the database and application manifests.

```bash
kubectl apply -f k8s/manifests/configmap.yaml
kubectl apply -f k8s/manifests/secret.yml
kubectl apply -f k8s/manifests/database/
```

### 4. Deploy the Java application

```bash
kubectl apply -f k8s/manifests/application/
```

### 5. Deploy Metrics Server

```bash
kubectl apply -f k8s/manifests/metrics-server/components.yaml
```

### 6. Verify deployment

```bash
kubectl get all -n three-tier-app
kubectl get pvc -n three-tier-app
kubectl get hpa -n three-tier-app
kubectl top pods -n three-tier-app
```

## Useful Commands

### Workloads

```bash
kubectl get pods -n three-tier-app
kubectl get deployments -n three-tier-app
kubectl get statefulsets -n three-tier-app
kubectl get replicasets -n three-tier-app
```

### Services and storage

```bash
kubectl get svc -n three-tier-app
kubectl get pvc -n three-tier-app
kubectl get pv
```

### Autoscaling

```bash
kubectl get hpa -n three-tier-app
kubectl describe hpa java-app-hpa -n three-tier-app
kubectl top pods -n three-tier-app
```

### Troubleshooting

```bash
kubectl describe pod <pod-name> -n three-tier-app
kubectl logs <pod-name> -n three-tier-app
kubectl get events -n three-tier-app --sort-by=.metadata.creationTimestamp
```

## Application Access

The Java application Service is configured as a NodePort Service:

- Service port: `8080`
- NodePort: `30080`

Alternatively, use port forwarding:

```bash
kubectl port-forward svc/java-app 8081:8080 -n three-tier-app
```

Then access:

```text
http://localhost:8081
```

Health endpoint:

```text
http://localhost:8081/health
```

## Learning Log

Hands-on implementation and testing notes are maintained in:

- [HPA Implementation and Testing](learning-logs/hpa-implementation-and-testing.md)

## Important Notes

- PostgreSQL data is stored using persistent storage rather than relying only on the Pod filesystem.
- StatefulSet provides stable identity for the PostgreSQL Pod.
- The HPA scales the Java application based on observed resource utilization.
- Metrics Server is required for resource metrics used by the HPA.
- Headlamp uses custom RBAC identities to demonstrate least-privilege access.
- Sensitive credentials must never be committed to Git.