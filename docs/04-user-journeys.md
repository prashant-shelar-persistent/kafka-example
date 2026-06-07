# User Journeys

## Overview

This is a developer-facing CLI application. There are no end-users or UI components. The "users" are Go developers learning Kafka fundamentals or running local integration tests. The journeys below reflect the developer experience of running the application.

---

## Primary Journey: Run the Combined Demo (main.go)

```mermaid
journey
    title Developer runs the Kafka combined demo
    section Setup
      Install Go 1.21+: 5: Developer
      Install Docker and Docker Compose: 5: Developer
      Clone the repository: 5: Developer
    section Start Infrastructure
      Run docker-compose up -d: 5: Developer
      Wait for Kafka and ZooKeeper to be ready: 3: Developer
    section Run Application
      Execute go run main.go: 5: Developer
      Observe 5 produced messages in stdout: 5: Developer
      Observe 5 consumed messages in stdout: 5: Developer
    section Completion
      Application exits gracefully after ~7s total: 5: Developer
      Review output to understand producer-consumer loop: 5: Developer
```

---

## Secondary Journey: Run the Standalone Producer

```mermaid
journey
    title Developer runs only the standalone producer binary
    section Prerequisites
      Kafka broker is already running on localhost:9092: 4: Developer
    section Execution
      Execute go run producer/producer.go: 5: Developer
      Observe partition and offset confirmations per message: 5: Developer
    section Learning
      Compare standalone producer with kafka/ package wrapper: 5: Developer
      Understand direct sarama API vs abstracted wrapper: 5: Developer
```

---

## Error Journey: Kafka Broker Not Available

```mermaid
flowchart TD
    A["Developer runs: go run main.go"] --> B["NewConsumer() called"]
    B --> C{"Kafka broker reachable\nat localhost:9092?"}
    C -->|"No"| D["sarama.NewConsumer returns error"]
    D --> E["log.Fatalf: Failed to create consumer"]
    E --> F["Process exits with status 1"]
    C -->|"Yes"| G["Application proceeds normally"]

    A2["Developer runs: go run producer/producer.go"] --> B2["sarama.NewSyncProducer called"]
    B2 --> C2{"Kafka broker reachable?"}
    C2 -->|"No"| D2["log.Fatalf: Failed to create producer"]
    D2 --> F2["Process exits with status 1"]
    C2 -->|"Yes"| G2["Messages sent normally"]
```

---

## Error Journey: Message Send Failure

```mermaid
flowchart TD
    A["producer.Send() called"] --> B["sarama.SendMessage()"]
    B --> C{"Broker accepts message?"}
    C -->|"Yes"| D["Return nil — fmt.Printf sent confirmation"]
    C -->|"No"| E["Return wrapped error"]
    E --> F["main.go: log.Printf — non-fatal"]
    F --> G["Loop continues to next message"]
```

---

## Developer Learning Journey: Understanding the Codebase

```mermaid
flowchart LR
    Start["Read README.md"]
    --> DockerSetup["Review docker-compose.yml\nUnderstand Kafka + ZK topology"]
    --> GoMod["Read go.mod\nIdentify IBM/sarama dependency"]
    --> KafkaPackage["Read kafka/consumer.go\nkafka/producer.go\nUnderstand wrapper structs"]
    --> MainFile["Read main.go\nSee orchestration: goroutine + loop"]
    --> StandaloneProducer["Read producer/producer.go\nCompare direct vs wrapped API"]
    --> RunDemo["Run docker-compose + go run main.go"]
    --> Experiment["Modify topic name, message count, or offsets"]
```
