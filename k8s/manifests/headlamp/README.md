# Headlamp – Kubernetes Dashboard Using Helm

## Overview

[Headlamp](https://headlamp.dev/) is a web-based Kubernetes dashboard that provides a graphical interface for inspecting and managing Kubernetes resources.

In this project, Headlamp is deployed using its official Helm chart. Custom Kubernetes RBAC configuration is used to demonstrate read-only and administrative access.

## Environment

| Component | Configuration |
|---|---|
| Helm repository | `https://kubernetes-sigs.github.io/headlamp/` |
| Helm chart | `headlamp/headlamp` |
| Chart version | `0.46.0` |
| Application version | `0.46.0` |
| Namespace | `headlamp` |
| Service type | ClusterIP |
| Service port | `80` |
| Replicas | `1` |
| Kubernetes cluster | `kind-java-cluster` |

## Architecture

```text
                  User / Browser
                         |
                         |
                  Headlamp UI
                         |
                         |
                  Headlamp Service
                    ClusterIP:80
                         |
                         |
                  Headlamp Pod
                         |
                         |
                  Kubernetes API
                         |
             +-----------+-----------+
             |                       |
       Read-only Identity       Admin Identity
             |                       |
       headlamp-sa          headlamp-admin-sa
             |                       |
       headlamp-readonly       cluster-admin
         ClusterRole              ClusterRole
             |                       |
       Read-only access          Full access
```

**Important:** The Headlamp application Pod's ServiceAccount identity and the identity used by a logged-in dashboard user are separate concepts in this configuration. We kept `config.unsafeUseServiceAccountToken` set to `false` so dashboard users do not automatically inherit the Pod's ServiceAccount permissions.

## 1. Add the Helm Repository

```bash
helm repo add headlamp https://kubernetes-sigs.github.io/headlamp/
helm repo update
```

Verify the chart:

```bash
helm search repo headlamp
```

## 2. Inspect the Chart

Inspect chart metadata:

```bash
helm show chart headlamp/headlamp
```

Export default values:

```bash
helm show values headlamp/headlamp > headlamp-values.yaml
```

Inspect the default configuration before creating custom values.

## 3. Create the Namespace

```bash
kubectl create namespace headlamp
```

Verify:

```bash
kubectl get namespace headlamp
```

## 4. Configure Custom RBAC

Instead of using the chart's default `cluster-admin` ClusterRoleBinding, we disabled its automatic creation and supplied our own ServiceAccount.

### Read-only ServiceAccount

File: `headlamp-readonly-sa.yaml`

Creates the `headlamp-sa` ServiceAccount in the `headlamp` namespace.

### Read-only ClusterRole

File: `headlamp-readonly-clusterrole.yaml`

The `headlamp-readonly` ClusterRole grants `get`, `list`, and `watch` permissions for selected resources:

- Pods, Services, Namespaces, Nodes
- ConfigMaps, PersistentVolumes, PersistentVolumeClaims
- Deployments, DaemonSets, ReplicaSets, StatefulSets
- Jobs and CronJobs

It does not grant permissions to create, update, patch, or delete these resources.

### Read-only ClusterRoleBinding

File: `headlamp-readonly-binding.yaml`

Binds the `headlamp-sa` ServiceAccount to the `headlamp-readonly` ClusterRole.

Apply the read-only RBAC manifests:

```bash
kubectl apply -f headlamp-readonly-sa.yaml
kubectl apply -f headlamp-readonly-clusterrole.yaml
kubectl apply -f headlamp-readonly-binding.yaml
kubectl apply -f k8s/manifests/headlamp/headlamp-readonly-sa.yaml
kubectl apply -f k8s/manifests/headlamp/headlamp-readonly-clusterrole.yaml
kubectl apply -f k8s/manifests/headlamp/headlamp-readonly-binding.yaml
kubectl apply -f k8s/manifests/headlamp/headlamp-admin-sa.yaml
```

### Admin ServiceAccount

File: `headlamp-admin-sa.yaml`

Creates the `headlamp-admin-sa` ServiceAccount and a ClusterRoleBinding to the built-in `cluster-admin` ClusterRole.

This identity has broad administrative permissions across the cluster, including access to Secrets.

**Security warning:** Use the admin identity only when administrative permissions are necessary.

## 5. Configure Helm Values

File: `values.yaml`

Important custom settings:

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

### Why these settings?

| Setting | Purpose |
|---|---|
| `config.inCluster: true` | Connects Headlamp to the Kubernetes API using in-cluster configuration. |
| `automountServiceAccountToken: true` | Mounts the Pod ServiceAccount token for in-cluster API access. |
| `serviceAccount.create: false` | Uses our previously created ServiceAccount. |
| `serviceAccount.name: headlamp-sa` | Specifies the ServiceAccount used by the Headlamp Pod. |
| `clusterRoleBinding.create: false` | Prevents the chart from creating its default ClusterRoleBinding. |
| `unsafeUseServiceAccountToken: false` | Avoids making all dashboard users act as the Pod's ServiceAccount. |
| `service.type: ClusterIP` | Keeps the dashboard Service internal to the cluster. |
| `ingress.enabled: false` | Does not create an Ingress resource. |
| `httpRoute.enabled: false` | Does not create an HTTPRoute resource. |

## 6. Install Headlamp Using Helm

```bash
helm install headlamp headlamp/headlamp \
  --namespace headlamp \
  -f k8s/manifests/headlamp/values.yaml
```

Check the release:

```bash
helm list -n headlamp
helm status headlamp -n headlamp
```

Verify resources:

```bash
kubectl get all -n headlamp
kubectl get sa -n headlamp
kubectl get clusterrolebinding | grep headlamp
```

## 7. Access the Dashboard

Use port forwarding:

```bash
kubectl port-forward -n headlamp svc/headlamp 8080:80
```

Open:

```text
http://localhost:8080
```

## 8. Generate Authentication Tokens

Generate a token for the read-only identity:

```bash
kubectl create token headlamp-sa -n headlamp
```

Generate a token for the admin identity:

```bash
kubectl create token headlamp-admin-sa -n headlamp
```

Use the appropriate token to authenticate to Headlamp.

**Security:** These tokens are credentials. Never commit them to Git or share them in screenshots, logs, or chat. Kubernetes TokenRequest tokens are time-limited by default.

## 9. Verify RBAC Permissions

Use Kubernetes authorization checks to verify permissions independently of the dashboard.

### Read-only identity

```bash
kubectl auth can-i get pods \
  --as=system:serviceaccount:headlamp:headlamp-sa

kubectl auth can-i list deployments \
  --as=system:serviceaccount:headlamp:headlamp-sa

kubectl auth can-i delete pods \
  --as=system:serviceaccount:headlamp:headlamp-sa

kubectl auth can-i get secrets \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

Expected results:

| Action | Expected |
|---|---|
| Get Pods | yes |
| List Deployments | yes |
| Delete Pods | no |
| Get Secrets | no |

### Admin identity

```bash
kubectl auth can-i get pods \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa

kubectl auth can-i create pods \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa

kubectl auth can-i delete pods \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa

kubectl auth can-i get secrets \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa
```

Expected results: `yes` for all four checks.

## 10. Practical RBAC Test

A ConfigMap was used to demonstrate the difference between the two identities.

### Attempt creation using read-only identity

```bash
kubectl create configmap rbac-demo \
  --from-literal=purpose=testing-rbac \
  -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

Expected: Forbidden.

### Create using admin identity

```bash
kubectl create configmap rbac-demo \
  --from-literal=purpose=testing-rbac \
  -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa
```

Expected: ConfigMap created.

### Read using read-only identity

```bash
kubectl get configmap rbac-demo -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

Expected: Successful.

### Attempt deletion using read-only identity

```bash
kubectl delete configmap rbac-demo -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-sa
```

Expected: Forbidden.

### Delete using admin identity

```bash
kubectl delete configmap rbac-demo -n headlamp \
  --as=system:serviceaccount:headlamp:headlamp-admin-sa
```

Expected: ConfigMap deleted.

This test demonstrated that the read-only identity can inspect permitted resources but cannot create or delete them.

## 11. Useful Helm Commands

```bash
# List releases
helm list -n headlamp

# Inspect release status
helm status headlamp -n headlamp

# View release history
helm history headlamp -n headlamp

# View configured values
helm get values headlamp -n headlamp --all

# Render manifests without installing
helm template headlamp headlamp/headlamp \
  -n headlamp \
  -f k8s/manifests/headlamp/values.yaml

# Upgrade the release
helm upgrade headlamp headlamp/headlamp \
  -n headlamp \
  -f k8s/manifests/headlamp/values.yaml

# Uninstall the release
helm uninstall headlamp -n headlamp
```

## Key Learnings

- Helm charts package Kubernetes resources and values into reusable deployments.
- Helm releases provide installation, upgrade, history, and rollback management.
- ServiceAccounts provide identities for workloads and API access.
- ClusterRoles define permissions; ClusterRoleBindings associate those permissions with identities.
- RBAC follows the principle of least privilege.
- A read-only identity should not receive unnecessary write permissions.
- `cluster-admin` grants broad administrative access and should be protected.
- `kubectl auth can-i` is useful for testing Kubernetes authorization.
- Headlamp provides a visual interface, but Kubernetes RBAC still controls API permissions.