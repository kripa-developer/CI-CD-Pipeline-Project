# CI/CD Pipeline Project

Spring Boot app → Docker image → Jenkins/GitHub Actions → Kubernetes (rolling update).

## Project layout

```
.
├── pom.xml                          # Maven build (Spring Boot 3.3, Java 17)
├── src/main/java/...                # App code (REST endpoint + actuator health)
├── src/test/java/...                # Basic test used by CI
├── Dockerfile                       # Multi-stage build → small non-root runtime image
├── .dockerignore
├── .github/workflows/ci.yml         # GitHub Actions pipeline
├── Jenkinsfile                      # Jenkins declarative pipeline (alternative)
├── k8s/                             # Plain Kubernetes manifests
│   ├── 00-namespace.yaml
│   ├── 01-configmap.yaml
│   ├── 02-deployment.yaml           # RollingUpdate strategy + probes
│   ├── 03-service.yaml
│   ├── 04-ingress.yaml
│   └── 05-hpa.yaml
└── helm/spring-boot-app/            # Equivalent Helm chart
    ├── Chart.yaml
    ├── values.yaml
    └── templates/
```

## 1. Run locally

```bash
mvn spring-boot:run
curl http://localhost:8080/
curl http://localhost:8080/actuator/health
```

## 2. Build & test

```bash
mvn clean test
mvn clean package        # produces target/demo-app.jar
```

## 3. Build the Docker image

```bash
docker build -t yourdockerhubuser/demo-app:latest .
docker run -p 8080:8080 yourdockerhubuser/demo-app:latest
```

## 4. CI pipeline

**GitHub Actions** (`.github/workflows/ci.yml`) runs on every push/PR to `main`:
1. `build-and-test` — compiles and runs unit tests on every push/PR.
2. `docker-build-push` — on `main` only: builds the image, tags it with the short commit SHA, pushes to Docker Hub.
3. `deploy` — patches the live Deployment with `kubectl set image` (this is what triggers the rolling update) and waits for `kubectl rollout status`; auto rolls back with `kubectl rollout undo` if the rollout fails.

Required repo secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `KUBE_CONFIG_DATA` (base64-encoded kubeconfig).

**Jenkins** (`Jenkinsfile`) does the same five stages using Jenkins credentials `dockerhub-credentials` and `kubeconfig-file`.

## 5. Deploy to Kubernetes

### Option A — plain manifests
```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/
```

### Option B — Helm
```bash
helm install demo-app helm/spring-boot-app \
  --set image.repository=yourdockerhubuser/demo-app \
  --set image.tag=latest
```

Upgrade later (this is what CI does under the hood conceptually):
```bash
helm upgrade demo-app helm/spring-boot-app --set image.tag=<new-sha>
```

## 6. Rolling update, explained

`k8s/02-deployment.yaml` (and the Helm equivalent) sets:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1          # 1 extra pod may be created above the desired replica count
    maxUnavailable: 0    # desired replica count must always stay fully available
```

With `replicas: 4`, a new version rolls out by spinning up 1 new pod at a time, waiting
for its `readinessProbe` to pass before killing an old pod — so the service never dips
below 4 ready pods and there's no downtime.

Watch a rollout live:
```bash
kubectl rollout status deployment/demo-app -n demo
kubectl get pods -n demo -w
```

Roll back if something goes wrong:
```bash
kubectl rollout undo deployment/demo-app -n demo
kubectl rollout history deployment/demo-app -n demo
```

## Notes / things to customize before using in a real cluster

- Replace `yourdockerhubuser/demo-app` everywhere with your actual registry/image path.
- Replace `demo-app.example.com` in the ingress with your real hostname, and make sure an ingress controller (e.g. nginx) is installed.
- The GitHub Actions workflow assumes a self-hosted or reachable cluster via `KUBE_CONFIG_DATA`; adjust auth (e.g. cloud-provider specific actions like `aws-actions/configure-aws-credentials` + `eks` for AWS) as needed for your cluster.
- This was built and YAML-validated in a sandboxed environment without Maven/Docker/Helm/Kubernetes network access, so run `mvn clean test`, `docker build`, and `helm lint` locally to fully verify before pushing to production.
