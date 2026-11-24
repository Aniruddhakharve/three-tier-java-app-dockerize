# Horizontal Pod Autoscaler (HPA) – Implementation and Testing

## 1. Objective

Implement Horizontal Pod Autoscaling (HPA) for the Java application running in the Kubernetes cluster.

The objective is to automatically increase or decrease the number of Java application Pods based on CPU utilization.

### Learning outcomes

- Understand Horizontal Pod Autoscaling.
- Understand how HPA communicates with the Deployment.
- Install and configure Metrics Server.
- Understand CPU requests and limits in HPA calculations.
- Configure autoscaling using `autoscaling/v2`.
- Test automatic scale-up and scale-down.
- Understand stabilization windows and scaling policies.

---

## 2. Environment

| Component | Configuration |
|---|---|
| Kubernetes cluster | KIND |
| Cluster name | java-cluster |
| Kubernetes context | kind-java-cluster |
| Namespace | three-tier-app |
| Application | java-app |
| Workload type | Deployment |
| Initial replicas | 2 |
| CPU request per Pod | 250m |
| CPU limit per Pod | 500m |
| HPA API version | autoscaling/v2 |
| Minimum replicas | 2 |
| Maximum replicas | 5 |
| CPU target | 60% |

---

## 3. What is Horizontal Pod Autoscaling?

Horizontal Pod Autoscaling automatically adjusts the number of Pod replicas based on observed metrics.

For example:

- When CPU utilization increases, HPA can increase the number of application Pods.
- When CPU utilization decreases, HPA can reduce the number of Pods.
- HPA maintains the configured minimum and maximum replica counts.

HPA is useful for applications whose workload changes over time.

### Horizontal vs Vertical Scaling

**Horizontal scaling:**

- Increases or decreases the number of Pods.
- Example: 2 Pods → 4 Pods.

**Vertical scaling:**

- Changes the CPU or memory resources allocated to a container.
- Example: Increasing a container's CPU limit from 500m to 1 CPU.

HPA performs horizontal scaling.

---

## 4. How HPA Works

HPA does not directly create or delete Pods.

The scaling process involves multiple Kubernetes components:

1. Metrics Server collects resource usage metrics from Kubelets.
2. The HPA controller reads metrics through the Kubernetes Metrics API.
3. HPA calculates the desired replica count.
4. HPA updates the target Deployment's desired replica count.
5. The Deployment manages its ReplicaSet.
6. The ReplicaSet creates or removes Pods.

### Architecture flow

```text
Metrics Server
      |
      v
Kubernetes Metrics API
      |
      v
HPA Controller
      |
      v
Deployment
      |
      v
ReplicaSet
      |
      v
Java Application Pods
```

### Important distinction

- HPA adjusts the desired replica count.
- Deployment manages the ReplicaSet.
- ReplicaSet creates and removes Pods.

---

## 5. Metrics Server Installation

Initially, Metrics Server was not installed in the cluster.

Verified using:

```bash
kubectl get deployment metrics-server -n kube-system
```

The Deployment was initially not found.

### Metrics Server manifest

The official Metrics Server manifest was saved at:

```text
k8s/manifest/metrics-server/components.yaml
```

Applied using:

```bash
kubectl apply -f metrics-server/components.yaml
```

### KIND-specific configuration

The Metrics Server Deployment included:

```yaml
- --kubelet-insecure-tls
- --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname
- --kubelet-use-node-status-port
- --metric-resolution=15s
```

**Important security note:**

`--kubelet-insecure-tls` disables Kubelet TLS certificate verification.

It was used for this local KIND learning environment. It should not be treated as a production security configuration.

### Verification

```bash
kubectl get deployment metrics-server -n kube-system

kubectl get pods -n kube-system

kubectl get apiservice v1beta1.metrics.k8s.io

kubectl top pods -n three-tier-app

kubectl top nodes
```

Verified results:

- Metrics Server Deployment: 1/1 available.
- Metrics Server Pod: Running.
- Metrics APIService: Available=True.
- `kubectl top pods`: Working.
- `kubectl top nodes`: Working.

Metrics Server was successfully installed and verified.

---

## 6. Understanding CPU Requests and Limits

The Java application Deployment has:

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### CPU request

`250m` means 250 millicores, equivalent to 0.25 CPU core.

The CPU request is used by Kubernetes for scheduling and as the baseline for HPA CPU utilization calculations.

### CPU limit

`500m` means 0.5 CPU core.

The CPU limit defines the maximum CPU time the container can consume.

### HPA CPU utilization calculation

HPA calculates CPU utilization as a percentage of the CPU request, not the CPU limit.

Formula:

```text
CPU Utilization (%) =
(Current CPU Usage / CPU Request) × 100
```

Example:

```text
CPU Usage  = 150m
CPU Request = 250m

Utilization = (150 / 250) × 100

Utilization = 60%
```

Therefore, the HPA target of 60% corresponds to approximately 150m average CPU usage per Pod.

---

## 7. HPA Manifest

File:

```text
k8s/manifest/application/hpa.yaml
```

Configuration:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler

metadata:
  name: java-app-hpa
  namespace: three-tier-app

spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: java-app

  minReplicas: 2
  maxReplicas: 5

  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60

  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      selectPolicy: Max
      policies:
        - type: Pods
          value: 1
          periodSeconds: 15

    scaleDown:
      stabilizationWindowSeconds: 300
      selectPolicy: Min
      policies:
        - type: Pods
          value: 1
          periodSeconds: 30
```

### Important fields

| Field | Purpose |
|---|---|
| `apiVersion` | Uses autoscaling/v2 API |
| `kind` | Defines a HorizontalPodAutoscaler |
| `scaleTargetRef` | Identifies the Deployment to scale |
| `minReplicas` | Minimum number of Pods |
| `maxReplicas` | Maximum number of Pods |
| `metrics` | Defines the metric used for scaling |
| `averageUtilization` | Target average CPU utilization |
| `behavior` | Controls scaling behavior |
| `stabilizationWindowSeconds` | Controls stabilization period |
| `policies` | Defines scaling limits |

### Scale-up behavior

```yaml
scaleUp:
  stabilizationWindowSeconds: 0
  selectPolicy: Max
  policies:
    - type: Pods
      value: 1
      periodSeconds: 15
```

Meaning:

- No scale-up stabilization delay.
- Allows adding at most one Pod every 15 seconds under this policy.

### Scale-down behavior

```yaml
scaleDown:
  stabilizationWindowSeconds: 300
  selectPolicy: Min
  policies:
    - type: Pods
      value: 1
      periodSeconds: 30
```

Meaning:

- Uses a 300-second (5-minute) stabilization window.
- Allows removing at most one Pod every 30 seconds under this policy.
- Helps avoid unnecessary fluctuations in replica count.

The scale-down stabilization window considers recent scaling recommendations; it is not simply a fixed five-minute sleep.

---

## 8. Validate and Apply HPA

Server-side dry-run:

```bash
kubectl apply --dry-run=server -f hpa.yaml
```

Result:

```text
horizontalpodautoscaler.autoscaling/java-app-hpa created (server dry run)
```

Apply:

```bash
kubectl apply -f hpa.yaml
```

Result:

```text
horizontalpodautoscaler.autoscaling/java-app-hpa created
```

Verify:

```bash
kubectl get hpa -n three-tier-app
```

Detailed information:

```bash
kubectl describe hpa java-app-hpa -n three-tier-app
```

HPA was successfully created.

---

## 9. Understanding the HPA Scaling Formula

The simplified HPA replica calculation is:

```text
Desired Replicas =
ceil(
  Current Replicas ×
  (Current Metric / Target Metric)
)
```

Example:

```text
Current replicas = 2
Current CPU       = 120%
Target CPU        = 60%

Desired replicas = ceil(2 × 120/60)

Desired replicas = 4
```

The final replica count is also constrained by the configured minimum, maximum, and scaling behavior.

---

## 10. Practical HPA Scale-Up Test

### Objective

Generate CPU load inside the Java application container and observe whether HPA increases the replica count.

**Important:** CPU load must affect the Java Deployment's Pods. CPU consumed by an unrelated load-generator Pod would not increase the Java Deployment's CPU utilization.

### Step 1: Identify Java Pods

```bash
kubectl get pods -n three-tier-app -l app=java-app
```

Initial state:

```text
java-app-76fd678c7f-gqxng   1/1   Running
java-app-76fd678c7f-zcrl6   1/1   Running
```

### Step 2: Check available tools inside the container

```bash
kubectl exec java-app-76fd678c7f-gqxng \
  -n three-tier-app \
  -c java-app -- \
  sh -c 'echo "Shell is available"; command -v yes'
```

Verified:

```text
Shell is available
/usr/bin/yes
```

### Step 3: Generate CPU load

```bash
kubectl exec java-app-76fd678c7f-gqxng \
  -n three-tier-app \
  -c java-app -- \
  sh -c 'yes > /dev/null & echo $!'
```

Output:

```text
54
```

The CPU-intensive process started in the background with PID 54.

### Step 4: Observe CPU usage

```bash
kubectl top pods -n three-tier-app -l app=java-app
```

Observed CPU usage:

```text
java-app-76fd678c7f-gn7lp   500m
java-app-76fd678c7f-gqxng   500m
java-app-76fd678c7f-zcrl6   4m
```

The HPA subsequently increased the number of Java Pods.

### Step 5: Verify scale-up

```bash
kubectl get hpa java-app-hpa -n three-tier-app

kubectl get pods -n three-tier-app -l app=java-app
```

Observed HPA:

```text
NAME           REFERENCE             TARGETS     MINPODS   MAXPODS   REPLICAS
java-app-hpa   Deployment/java-app   cpu: 52%/60%   2         5         4
```

Four Java application Pods were running.

### Step 6: Inspect HPA events

```bash
kubectl describe hpa java-app-hpa -n three-tier-app
```

Observed events:

```text
SuccessfulRescale
New size: 3; reason: cpu resource utilization above target

SuccessfulRescale
New size: 4; reason: cpu resource utilization above target
```

**Result:** HPA successfully scaled the Java Deployment from 2 to 4 replicas.

---

## 11. Practical HPA Scale-Down Test

### Step 1: Stop the CPU load

The process PID was 54.

```bash
kubectl exec java-app-76fd678c7f-gqxng \
  -n three-tier-app \
  -c java-app -- \
  sh -c 'kill 54'
```

The background CPU-intensive process was terminated.

### Step 2: Verify CPU usage

```bash
kubectl top pods -n three-tier-app -l app=java-app
```

CPU usage returned to normal levels.

Example observed values:

```text
java-app-76fd678c7f-g5czl   9m
java-app-76fd678c7f-gn7lp   14m
java-app-76fd678c7f-gqxng  5m
java-app-76fd678c7f-zcrl6   5m
```

### Step 3: Verify HPA scaling

```bash
kubectl get hpa java-app-hpa -n three-tier-app

kubectl describe hpa java-app-hpa -n three-tier-app
```

Observed events:

```text
SuccessfulRescale
New size: 3; reason: All metrics below target

SuccessfulRescale
New size: 2; reason: All metrics below target
```

Final HPA metrics:

```text
CPU: 3% (8m) / 60%

Min replicas: 2
Max replicas: 5

Deployment pods: 2 current / 2 desired
```

**Result:** HPA successfully scaled the Java Deployment from 4 back to 2 replicas.

---

## 12. Final Verification

```bash
kubectl get hpa -n three-tier-app

kubectl get pods -n three-tier-app -l app=java-app

kubectl top pods -n three-tier-app -l app=java-app
```

Final Java Pods:

```text
java-app-76fd678c7f-gqxng   1/1   Running   0
java-app-76fd678c7f-zcrl6   1/1   Running   0
```

Final state:

- Two Java Pods running.
- Both Pods Ready.
- No container restarts.
- HPA active.
- CPU utilization below the 60% target.
- Replica count returned to the configured minimum.

---

## 13. HPA vs Cluster Autoscaler

These two components solve different problems.

| Feature | HPA | Cluster Autoscaler |
|---|---|---|
| Scaling unit | Pods | Nodes |
| Main purpose | Adjust application replicas | Adjust cluster node count |
| Trigger | Metrics such as CPU/memory | Unschedulable Pods and node utilization |
| Example | 2 Pods → 4 Pods | 2 Nodes → 3 Nodes |

HPA does not automatically add KIND nodes.

---

## 14. Important Concepts and Common Confusions

### Does HPA create Pods directly?

No. HPA updates the desired replica count of its target workload. The Deployment and ReplicaSet handle Pod creation and removal.

### Does HPA use CPU limits for utilization calculations?

No. CPU utilization is calculated against CPU requests.

### Why do we need Metrics Server?

Metrics Server provides resource usage metrics through the Kubernetes Metrics API. HPA uses those metrics for resource-based autoscaling.

### Why set minimum replicas to 2?

To ensure at least two Java application replicas remain available.

### Why set maximum replicas to 5?

To prevent uncontrolled horizontal scaling beyond the chosen limit.

### Why use a stabilization window for scale-down?

To avoid rapidly removing Pods when workload metrics fluctuate.

### Does HPA scale PostgreSQL?

No. This HPA targets only the Java Deployment.

PostgreSQL is a StatefulSet and requires a different scaling strategy because database replicas need appropriate replication and data-consistency management.

### Does HPA increase node count?

No. HPA adjusts Pod replicas. Cluster Autoscaler is responsible for node scaling.

---

## 15. Interview Questions and Answers

**Q1. What is Kubernetes HPA?**

HPA is a Kubernetes controller that automatically adjusts the number of Pod replicas based on observed metrics such as CPU and memory utilization.

**Q2. Why is Metrics Server required for CPU-based HPA?**

Metrics Server collects resource usage metrics from Kubelets and exposes them through the Kubernetes Metrics API, which HPA uses to make scaling decisions.

**Q3. What is the difference between CPU request and CPU limit?**

CPU request represents the CPU resource baseline used for scheduling and HPA utilization calculations. CPU limit defines the maximum CPU usage permitted for the container.

**Q4. Does HPA create Pods directly?**

No. HPA updates the desired replica count of the target workload. The Deployment and ReplicaSet manage the actual Pods.

**Q5. What is the difference between HPA and Cluster Autoscaler?**

HPA scales the number of Pods, whereas Cluster Autoscaler adjusts the number of cluster nodes.

**Q6. Why configure a scale-down stabilization window?**

It prevents premature scale-down by considering recent scaling recommendations and helps avoid unnecessary fluctuations in replica count.

**Q7. Can we use HPA with a StatefulSet?**

Yes, HPA can target supported scalable workloads, including StatefulSets. However, database scaling requires application-aware replication and data-consistency planning.

**Q8. Why must CPU requests be configured for CPU utilization-based HPA?**

CPU utilization is calculated as a percentage of the CPU request. Without the required CPU requests, HPA cannot calculate utilization for the affected Pods.

---

## 16. Quick Revision Cheat Sheet

| Concept | Remember |
|---|---|
| HPA | Automatically adjusts Pod replicas |
| Metrics Server | Provides resource usage metrics |
| Metrics API | Exposes metrics to Kubernetes components |
| CPU request | Baseline for HPA CPU utilization |
| CPU limit | Maximum CPU allowed |
| `averageUtilization` | CPU utilization target percentage |
| `minReplicas` | Minimum number of Pods |
| `maxReplicas` | Maximum number of Pods |
| `scaleUp` | Controls scale-up behavior |
| `scaleDown` | Controls scale-down behavior |
| Stabilization window | Helps avoid scaling fluctuations |
| Cluster Autoscaler | Adjusts node count |

### Useful Commands

```bash
# Check HPA
kubectl get hpa -n three-tier-app

# Detailed HPA information
kubectl describe hpa java-app-hpa -n three-tier-app

# Watch HPA
kubectl get hpa java-app-hpa -n three-tier-app -w

# Check Pod CPU and memory
kubectl top pods -n three-tier-app

# Check node CPU and memory
kubectl top nodes

# Check Java application replicas
kubectl get pods -n three-tier-app -l app=java-app

# Check Deployment
kubectl get deployment java-app -n three-tier-app

# Check Metrics Server
kubectl get deployment metrics-server -n kube-system
```

---

## 17. Completion Summary

The Java application now has a working Horizontal Pod Autoscaler.

Practical verification:

| Test | Expected result | Actual result |
|---|---|---|
| Initial replicas | 2 | 2 |
| CPU load generated | CPU increases | Verified |
| Scale-up | More Pods created | 2 → 3 → 4 |
| CPU load stopped | CPU decreases | Verified |
| Scale-down | Extra Pods removed | 4 → 3 → 2 |
| Final replicas | Minimum 2 | 2 |
| Pod health | All Ready | Verified |

**HPA implementation and practical testing completed successfully.**