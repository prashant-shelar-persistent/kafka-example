# Deployment

## Overview

This is a **local development / educational project** with no production deployment pipeline. Infrastructure is entirely managed via Docker Compose for local execution. There is no CI/CD configuration, container registry, Kubernetes manifests, or cloud provider configuration in the repository.

---

## Local Deployment Architecture

```mermaid
flowchart TB
    subgraph Host["Developer Machine"]
        subgraph DockerNetwork["Docker Compose Network (bridge)"]
            ZK["ZooKeeper Container\nimage: confluentinc/cp-zookeeper:latest\nport: 2181\nenv: ZOOKEEPER_CLIENT_PORT=2181\nenv: ZOOKEEPER_TICK_TIME=2000"]
            Kafka["Kafka Broker Container\nimage: confluentinc/cp-kafka:latest\nport: 9092\nenv: KAFKA_BROKER_ID=1\nenv: KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181\nenv: KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092\nenv: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"]
            ZK -->|"ZooKeeper protocol :2181"| Kafka
        end

        subgraph GoRuntime["Go Runtime (host network)"]
            MainBin["go run main.go\nor\ngo run producer/producer.go"]
        end

        MainBin -->|"PLAINTEXT TCP :9092\nlocalhost mapped port"| Kafka
    end
```

---

## Docker Compose Service Map

```mermaid
flowchart LR
    subgraph Compose["docker-compose.yml (version: 3)"]
        ZKService["zookeeper\nimage: confluentinc/cp-zookeeper:latest\nexposed: 2181"]
        KafkaService["kafka\nimage: confluentinc/cp-kafka:latest\nexposed: 9092\ndepends_on: zookeeper"]
        ZKService -->|"depends_on"| KafkaService
    end
```

---

## Startup Sequence

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Compose as Docker Compose
    participant ZK as ZooKeeper Container
    participant KB as Kafka Broker Container
    participant App as Go Application

    Dev->>Compose: docker-compose up -d
    Compose->>ZK: Start ZooKeeper (port 2181)
    ZK-->>Compose: Healthy
    Compose->>KB: Start Kafka (depends on ZooKeeper)
    KB->>ZK: Connect on zookeeper:2181
    ZK-->>KB: Session established
    KB-->>Compose: Healthy (port 9092 bound)
    Compose-->>Dev: Services up

    Dev->>App: go run main.go
    App->>KB: TCP connect to localhost:9092
    KB-->>App: Connected
    App->>App: Run producer-consumer demo
    App-->>Dev: Output to stdout, exits after ~7s
```

---

## Teardown

```mermaid
flowchart LR
    A["Developer: docker-compose down"] --> B["Stop Kafka container"]
    B --> C["Stop ZooKeeper container"]
    C --> D["Remove containers and networks"]
    D --> E["All data lost (no volumes defined)"]
```

> **Note:** The Docker Compose configuration defines **no named volumes**, so all Kafka topic data and offsets are lost when containers are stopped.

---

## Environment Configuration

| Parameter | Value | Location |
|---|---|---|
| Kafka Broker Address | `localhost:9092` | Hardcoded in Go source files |
| ZooKeeper Port | `2181` | `docker-compose.yml` |
| Kafka Port | `9092` | `docker-compose.yml` |
| Kafka Broker ID | `1` | `docker-compose.yml` |
| Replication Factor | `1` | `docker-compose.yml` env |
| Topic Name | `test-topic` | Hardcoded in `main.go` |
| Consumer Partition | `0` | Hardcoded in `kafka/consumer.go` |
| Consumer Start Offset | `OffsetNewest` | Hardcoded in `kafka/consumer.go` |

> All configuration is hardcoded. There is no `.env` file, environment variable injection, or configuration management layer.

---

## CI/CD Pipeline

**No CI/CD pipeline is configured in this repository.** There are no:
- GitHub Actions workflows
- GitLab CI files
- Jenkinsfiles
- Makefile build targets
- Dockerfiles for the Go application itself
- Container registry pushes

The project is intended to be run manually by developers for learning purposes.

---

## Production Readiness Notes

This project is explicitly a **development example**, not production-ready code. For production deployment, the following would need to be addressed:

| Gap | Production Recommendation |
|---|---|
| Hardcoded broker address | Inject via environment variable or config file |
| No consumer group | Use `sarama.ConsumerGroup` for scalable consumption |
| OffsetNewest only | Persist consumer group offsets to Kafka |
| Single partition | Design for multi-partition topics |
| No TLS/SASL | Enable TLS and SASL authentication for Kafka connections |
| No health checks | Add readiness/liveness probes if containerized |
| No metrics export | Expose `rcrowley/go-metrics` data to Prometheus |
| Single broker | Use a multi-broker Kafka cluster with replication |
