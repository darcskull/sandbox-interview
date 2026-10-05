# Interview Lab

A hands-on interview-preparation repository for Java, C#, and DevOps. The demo is a Java 27 Spring Boot order API with Liquibase-managed relational data, MongoDB audit storage, Kafka and RabbitMQ publishers/consumers, Docker, GitLab CI, and Helm.

## Contents

- `docs/java-cheatsheet.md` — Java types, objects, collections, control flow, streams, exceptions, and concurrency.
- `docs/csharp-cheatsheet.md` — C# types, OOP, collections/LINQ, async, disposal, and memory.
- `docs/devops-cheatsheet.md` — CI/CD, Docker, Kubernetes, Helm, reliability, and troubleshooting.
- `docs/sql-cheatsheet.md` — relational modeling, SQL queries, transactions, indexes, migrations, and senior Q&A.
- `docs/mongodb-cheatsheet.md` — document modeling, CRUD, aggregation, indexes, replication/sharding, and senior Q&A.
- `docs/kafka-cheatsheet.md` and `docs/rabbitmq-cheatsheet.md` — broker concepts, delivery guarantees, operations, AI workload patterns, and senior Q&A.
- `docs/redis-cache-cheatsheet.md` — cache strategies, Redis data structures, consistency, operations, and senior Q&A.
- `docs/data-and-messaging-comparisons.md` — SQL vs NoSQL and Kafka vs RabbitMQ tradeoffs.
- `docs/ai-agentic-development-cheatsheet.md` — RAG, tool calling, agent loops, safety, evaluation, and delivery patterns.
- `src/` — grouped controllers, services, order model/repository, shared messaging, Kafka and RabbitMQ adapters, Mongo persistence, and the Thymeleaf dashboard.
- `src/main/resources/db/changelog/` — XML master changelog and separate XML change files.
- `scripts/start-project.sh` — builds and starts the API, then opens the home page. It defaults to standalone H2 mode.
- `scripts/stop-project.sh` — stops the app and its local Docker Compose dependencies.
- `.github/workflows/ci.yml` — GitHub Actions build, test, Docker build, and health-check example.
- `.gitlab-ci.yml` — commented GitLab CI example for tests, image build, verification, registry push, and Helm deploy.
- `deploy/helm/interview-lab/` — annotated Helm chart templates and configuration.

Java sources follow Google Java Format. `mvn verify` checks formatting; apply it with:

```bash
mvn com.spotify.fmt:fmt-maven-plugin:2.28:format
```

## Prerequisites

- JDK 27 and Maven 3.6.3+ are enough to run the default standalone application.
- Git Bash, WSL, or another Bash shell to run the start script from Windows.
- Docker Compose or Podman with its Compose provider only for the full MongoDB/Kafka/RabbitMQ/PostgreSQL demo and the container-based integration tests.
- Optional: Helm 3 and a Kubernetes context for deployment.

## Start the project

In PowerShell at the repository root, start the standalone H2 application (no containers required):

```powershell
& 'C:\Program Files\Git\bin\bash.exe' .\scripts\start-project.sh
```

This mode runs the dashboard and H2 pages while disabling external brokers. To run the full H2-backed demo with Kafka, RabbitMQ, and MongoDB, pass `h2`:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' .\scripts\start-project.sh h2
```

To use PostgreSQL with the same local services:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' .\scripts\start-project.sh postgres
```

These are Bash scripts, so do not run `./scripts/start-project.sh` directly at a PowerShell prompt and do not type `/usr/bin/env bash` there. If you use Git Bash, change to the repository root and run:

```bash
./scripts/start-project.sh
```

The default standalone mode needs no Docker or Podman. To start the full H2 demo with Compose:

```bash
./scripts/start-project.sh h2
```

Use this for the PostgreSQL Compose profile:

```bash
./scripts/start-project.sh postgres
```

Stop the app and local containers from PowerShell with:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' .\scripts\stop-project.sh
```

Or from Git Bash:

```bash
./scripts/stop-project.sh
```

Standalone is the default Spring profile and start-script mode. It uses embedded H2 and excludes MongoDB, Kafka, and RabbitMQ client auto-configuration, so no connections or broker admin retries occur. Framework logs are reduced to warnings while Interview Lab logs remain at info level. The MongoDB, Kafka, and RabbitMQ cards are marked unavailable. The `h2` and `postgres` script arguments start PostgreSQL, MongoDB, Kafka, and RabbitMQ using Docker Compose or Podman Compose; `h2` selects the H2 profile while still running the local services. Stop the app with `./scripts/stop-project.sh`; the app log and PID are stored under `.run/`.

You can also run without Bash from IntelliJ: open `InterviewLabApplication.java` and click the Run icon next to `main`. No active profile is needed; standalone is the default. IntelliJ uses the Java version configured by the imported Maven project (Java 27).

For Podman on Windows, install Podman Desktop, start its Podman machine, and set up the Compose provider. Then pass `h2` or `postgres` to run the full demo. The default start script runs standalone mode.

The default standalone profile uses in-memory H2 in PostgreSQL compatibility mode. The `postgres` profile uses the PostgreSQL Compose service. Liquibase creates `orders` and inserts two sample rows; Hibernate validates rather than generates the schema. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:interviewdb`, username `sa`, blank password).

## Integration tests

Run all integration tests and the build with:

```bash
mvn --batch-mode clean verify
```

The H2 integration test and embedded Kafka broker test run without containers. MongoDB and RabbitMQ use disposable Testcontainers and are skipped automatically when Docker is unavailable. The GitHub and GitLab pipelines intentionally run only the isolated unit tests and launch the Docker smoke-check in standalone mode, so CI does not contact Kafka, RabbitMQ, or MongoDB.

Fast unit tests cover order-service mapping/event publication and routing demo messages to the Kafka or RabbitMQ adapter; they do not need a running database or broker. To run the full local test suite, use `mvn --batch-mode clean verify`; container-based MongoDB and RabbitMQ integration tests run when Docker is available.

## Dashboard and event flow

The home page links to four small pages: H2 orders, MongoDB audit documents, Kafka messages, and RabbitMQ messages. Each page has a form, a random-data button, and a live list. Kafka and RabbitMQ pages show messages after their consumers receive them; the demo consumers mirror deliveries into MongoDB so the pages can query and display them. This mirror is for learning and observation, not a broker replacement.

The JSON API is also available for interview practice:

```bash
curl http://localhost:8080/api/orders
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerName":"Linus Torvalds","total":19.99}'
```

Creating an order persists it and publishes an event to Kafka topic `orders.created` and RabbitMQ queue `orders.created`. Kafka consumers log the event and save an audit document to MongoDB; the RabbitMQ consumer logs its copy.

Messaging code is grouped by transport under `src/main/java/dev/interview/lab/messaging/kafka/` and `rabbitmq/`. Broker-specific configuration, publishers, and listeners live with each adapter. Shared event records, destination names, the cross-broker publisher facade, and the standalone publisher remain in the parent `messaging/` package. Integration tests follow the same Kafka/RabbitMQ subpackage layout under `src/test/java/`.

## GitLab pipeline example

The repository contains both [`.gitlab-ci.yml`](.gitlab-ci.yml) and `.github/workflows/ci.yml`. GitLab demonstrates Maven verification, Docker build/health verification, registry push, and a manual Helm deployment. GitHub Actions demonstrates Maven verification, Docker build, and a container health check. These are learning examples; deployment requires configured runners, registries, Kubernetes credentials, a database Secret, and reachable cluster dependencies.

There is no local pipeline runner script. Use either CI definition as the pipeline example. To build locally:

```bash
mvn --batch-mode clean verify
docker build -t interview-lab:local .
```

## Helm chart

The chart source is under `deploy/helm/interview-lab/`. Comments in each chart file explain its metadata, values, helper templates, Deployment, Service, and release notes. Its default values demonstrate PostgreSQL mode and expect a Secret named `interview-lab-db` plus reachable dependent services. The GitLab deploy example explicitly overrides the profile to standalone H2 mode and omits those database environment variables, so it does not connect to MongoDB, Kafka, or RabbitMQ.

```bash
helm lint deploy/helm/interview-lab
helm template interview-lab deploy/helm/interview-lab \
  --set image.repository=example/interview-lab --set image.tag=local
```
