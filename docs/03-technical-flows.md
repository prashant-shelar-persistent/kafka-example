# Technical Flows

## 1. Application Startup Flow (main.go)

```mermaid
sequenceDiagram
    participant Main as main()
    participant KafkaPkg as kafka package
    participant Sarama as Sarama Library
    participant Broker as Kafka Broker:9092
    participant Stdout as stdout

    Main->>KafkaPkg: NewConsumer()
    KafkaPkg->>Sarama: sarama.NewConfig()
    KafkaPkg->>Sarama: sarama.NewConsumer(brokers, config)
    Sarama->>Broker: TCP Connect + Metadata Request
    Broker-->>Sarama: Broker metadata
    Sarama-->>KafkaPkg: Consumer instance
    KafkaPkg-->>Main: *Consumer

    Main->>Main: go consumer.Consume("test-topic", handler)
    Note over Main: Consumer goroutine started

    Main->>KafkaPkg: NewProducer()
    KafkaPkg->>Sarama: sarama.NewConfig()
    KafkaPkg->>Sarama: sarama.NewSyncProducer(brokers, config)
    Sarama->>Broker: TCP Connect + Metadata Request
    Broker-->>Sarama: Broker metadata
    Sarama-->>KafkaPkg: SyncProducer instance
    KafkaPkg-->>Main: *Producer

    loop 5 iterations (i = 0..4)
        Main->>KafkaPkg: producer.Send("test-topic", message)
        KafkaPkg->>Sarama: producer.SendMessage(ProducerMessage)
        Sarama->>Broker: PRODUCE request
        Broker-->>Sarama: Partition + Offset ack
        Sarama-->>KafkaPkg: partition, offset
        KafkaPkg-->>Main: nil error
        Main->>Stdout: fmt.Printf("Sent message: ..")
        Main->>Main: time.Sleep(1s)
    end

    Main->>Main: time.Sleep(2s)
    Main->>KafkaPkg: producer.Close()
    Main->>KafkaPkg: consumer.Close()
```

---

## 2. Consumer Goroutine Message Processing Flow

```mermaid
sequenceDiagram
    participant ConsGoroutine as Consumer Goroutine
    participant Sarama as Sarama Library
    participant Broker as Kafka Broker:9092
    participant Handler as handler func(msg)
    participant Stdout as stdout

    ConsGoroutine->>Sarama: c.consumer.ConsumePartition("test-topic", 0, OffsetNewest)
    Sarama->>Broker: FETCH request (partition 0, offset newest)
    Broker-->>Sarama: Partition consumer stream
    Sarama-->>ConsGoroutine: partitionConsumer

    loop Infinite select loop
        alt Message received
            Broker->>Sarama: Message bytes
            Sarama->>ConsGoroutine: partitionConsumer.Messages() channel
            ConsGoroutine->>Handler: handler(msg)
            Handler->>Stdout: fmt.Printf("Received message: %s", msg.Value)
        else Error received
            Broker->>Sarama: Error signal
            Sarama->>ConsGoroutine: partitionConsumer.Errors() channel
            ConsGoroutine->>Stdout: log.Printf("Consumer error: %v")
        end
    end
```

---

## 3. Standalone Producer Flow (producer/producer.go)

```mermaid
sequenceDiagram
    participant SPMain as producer/main()
    participant Sarama as Sarama Library
    participant Broker as Kafka Broker:9092
    participant Stdout as stdout

    SPMain->>Sarama: sarama.NewConfig()
    Note over SPMain,Sarama: Return.Successes=true, Return.Errors=true
    SPMain->>Sarama: sarama.NewSyncProducer(["localhost:9092"], config)
    Sarama->>Broker: TCP Connect + Metadata Request
    Broker-->>Sarama: Broker metadata
    Sarama-->>SPMain: SyncProducer

    loop 5 iterations (i = 0..4)
        SPMain->>SPMain: Build ProducerMessage{Topic, Value}
        SPMain->>Sarama: producer.SendMessage(msg)
        Sarama->>Broker: PRODUCE request
        Broker-->>Sarama: Ack (partition, offset)
        Sarama-->>SPMain: partition, offset, nil
        SPMain->>Stdout: fmt.Printf("Message sent to partition %d at offset %d")
    end

    SPMain->>Sarama: producer.Close()
    Sarama->>Broker: Disconnect
```

---

## 4. Error Handling Flow

```mermaid
flowchart TD
    A["NewConsumer() / NewProducer()"] -->|"error != nil"| B["log.Fatalf — process exits"]
    A -->|"error == nil"| C["Proceed"]

    D["producer.Send()"] -->|"error != nil"| E["log.Printf — continue loop"]
    D -->|"error == nil"| F["fmt.Printf sent confirmation"]

    G["consumer goroutine error channel"] --> H["log.Printf — continue consuming"]

    I["consumer.Consume() returns error"] --> J["log.Printf in goroutine — goroutine exits"]
```

---

## 5. Configuration Flow

```mermaid
flowchart LR
    subgraph ProducerConfig["Producer Config"]
        PC1["sarama.NewConfig()"]
        PC2["Return.Successes = true"]
        PC3["Return.Errors = true"]
        PC1 --> PC2 --> PC3
    end

    subgraph ConsumerConfig["Consumer Config"]
        CC1["sarama.NewConfig()"]
        CC2["Consumer.Return.Errors = true"]
        CC1 --> CC2
    end

    subgraph BrokerAddr["Broker Address (hardcoded)"]
        BA["localhost:9092"]
    end

    ProducerConfig --> NewSyncProducer["sarama.NewSyncProducer"]
    ConsumerConfig --> NewConsumer["sarama.NewConsumer"]
    BrokerAddr --> NewSyncProducer
    BrokerAddr --> NewConsumer
```
