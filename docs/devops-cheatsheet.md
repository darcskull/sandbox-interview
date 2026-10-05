# DevOps interview cheat sheet: CI/CD, containers, Kubernetes, Helm

## CI/CD pipeline shape

A pipeline automates repeatable delivery: checkout → compile → unit/integration tests → static/security checks → package → build/tag container → publish artifact → deploy → verify/observe. Keep stages small, fail fast, and promote the same immutable artifact through environments. Use commit SHA or release tags rather than `latest` for deployed images. Cache dependencies carefully; don't cache outputs that compromise reproducibility.

```yaml
- name: Test
  run: mvn --batch-mode verify
- name: Build image
  run: docker build -t registry.example/app:${GIT_SHA} .
- name: Deploy
  run: helm upgrade --install app ./chart --set image.tag=${GIT_SHA} --wait
```

Protect secrets with a secret manager/short-lived identity; never echo credentials. Pin third-party actions/images to trusted versions or digests. Add least-privilege permissions, branch protection, provenance/SBOM/scanning where required, and approval gates for production. Rollback means redeploying a known-good image/chart revision; database changes must remain backward compatible during rollout.

## Docker essentials

An image is an immutable template; a container is a running process from it. Layers are cached; order stable dependency manifests before frequently changing source. `.dockerignore` limits build context. Multi-stage builds keep compilers out of runtime images. Run as non-root, use a small maintained base image, avoid secrets in build args/layers, define health checks or rely on orchestrator probes, handle SIGTERM, and log to stdout/stderr.

```dockerfile
FROM eclipse-temurin:27-jdk AS build
WORKDIR /src
RUN apt-get update && apt-get install --yes --no-install-recommends maven
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:27-jre
COPY --from=build /src/target/app.jar /app/app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Container networking: publish ports explicitly for host access; containers on one Compose network reach peers by service DNS name. Persist database state in volumes. Compose is useful for local integration environments, not a production scheduler.

## Kubernetes objects and operations

A Pod is the scheduling unit; usually deploy through a Deployment (replicas and rolling updates). A Service gives stable virtual IP/DNS for matching Pods. Ingress/Gateway exposes HTTP; ConfigMap stores non-secret config; Secret stores credentials (base64 is not encryption—use encryption at rest/external secret management). StatefulSet provides stable identity/storage semantics; Job runs to completion; CronJob schedules Jobs. Namespace scopes names and policy.

```bash
kubectl get pods -n demo
kubectl describe pod POD -n demo
kubectl logs deploy/api -c api --tail=100
kubectl rollout status deploy/api -n demo
kubectl rollout undo deploy/api -n demo
kubectl port-forward svc/api 8080:80 -n demo
```

Requests are scheduling guarantees; limits are ceilings (CPU throttles, memory overage can be OOMKilled). Readiness controls traffic, liveness restarts stuck containers, startup probes protect slow startup. Set resource requests/limits and graceful termination. HPA scales on metrics; disruption budgets limit voluntary disruption. Debug in order: events, scheduling/resources, probe failures, app logs, service selectors/endpoints, DNS/network policy, configuration/secrets.

## Helm

A chart templates Kubernetes manifests. `Chart.yaml` names/version; `values.yaml` provides defaults; `templates/` renders resources. Values can be overridden with `-f values-prod.yaml` or `--set`. Chart `version` tracks the chart; `appVersion` describes app. Use named templates/helpers for consistent labels, quote strings, and keep secrets outside ordinary values files.

```bash
helm lint deploy/helm/app
helm template demo deploy/helm/app -f values-staging.yaml
helm upgrade --install demo deploy/helm/app -n demo --create-namespace --wait
helm history demo -n demo
helm rollback demo 2 -n demo
```

Render and inspect before applying; validate against the target Kubernetes API. Avoid embedding cluster-specific values in templates. Use environment overlays for replicas/resources/hosts. Helm rollback does not reverse external side effects or safely undo incompatible database migrations.

## Reliability and incident response

Define SLI (measured behavior), SLO (target), and error budget (acceptable failure). Monitor latency, traffic, errors, saturation plus business signals. Alerts should be actionable and symptom-based. Use traces/correlation IDs across service boundaries; keep cardinality controlled. For incidents: mitigate impact, communicate, preserve evidence, identify contributing conditions, then write blameless follow-ups with owners and deadlines. Prefer progressive delivery/canaries for risky releases; verify rollback paths.

## AI and agentic systems in delivery pipelines

Treat model prompts, retrieval configuration, tool schemas, and evaluation sets as versioned artifacts. Test them alongside application code; scan dependencies/images and protect model/provider keys as secrets. A pipeline can run deterministic evals and publish model/prompt version metadata, but should not automatically promote on a single aggregate score. Canary changes, monitor quality/cost/latency and unsafe tool-call rates, and keep a rollback/kill switch. Do not send production secrets or unrestricted datasets to a CI model agent.

## Senior interview questions, answers, and reasoning

1. **Readiness versus liveness?** Readiness controls whether a Pod receives traffic; liveness restarts a process judged stuck. A dependency outage should not automatically fail liveness and trigger restart storms. Startup probes protect slow initialization.
2. **Requests versus limits?** Requests affect scheduling and resource guarantees; limits cap consumption. CPU limit pressure throttles; memory limit breach can OOM-kill. Set them from measured usage and understand node capacity/overcommit.
3. **How do you debug a Pending Pod?** Inspect events and scheduler constraints first: requests versus allocatable capacity, affinity/taints, PVC binding, quotas, and topology. Logs are unavailable before a container starts; don't begin with application debugging.
4. **How do you make deployments and DB migrations safe together?** Use backward-compatible expand/contract changes, deploy code that tolerates both schemas, migrate/backfill, observe, and remove old schema later. A Kubernetes rollback does not reverse an incompatible database change.
5. **How do you secure CI/CD?** Least privilege, short-lived identities, protected environments, pinned trusted actions/images, isolated untrusted PR jobs, secret masking, artifact provenance, and audit trails. A masked variable can still be exfiltrated by malicious code.
6. **How should AI agents be used in operations?** Restrict them to read-only diagnostics or narrowly approved runbooks initially; require policy checks and human approval for destructive actions. Scope credentials, record tool calls, set budgets/timeouts, and retain an immediate disable path.
