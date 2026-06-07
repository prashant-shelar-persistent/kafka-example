# Architecture

## System Architecture Overview

The kafka-example project is a **single-node, dual-binary Go application** that exercises the Apache Kafka messaging paradigm locally. There are two runnable binaries: the main combined demo (`main.go`) and a standalone producer (`producer/producer.go`). All Kafka broker and ZooKeeper infrastructure is provided via Docker Compose.

```mermaid
flowchart TB
    subgraph Host["Host Machine"]
        subgraph App["Go Application (main.go)"]
            MainEntry["main() entry point"]
            ProdWrapper["kafka.Producer wrapper"]
            ConsWrapper["kafka.Consumer wrapper"]
            Goroutine["Consumer Goroutine"]
        end

        subgraph StandaloneProducer["Standalone Binary (producer/producer.go)"]
            SPMain["main() - inline producer"]
        end

        subgraph KafkaPackage["kafka/ package"]
            NewProducer["NewProducer()"]
            NewConsumer["NewConsumer()"]
            SendFn["Producer.Send()"]
            ConsumeFn["Consumer.Consume()"]
        end
    end

    subgraph Docker["Docker Compose Infrastructure"]
        ZK["ZooKeeper:2181"]
        KB["Kafka Broker:9092"]
        ZK --> KB
    end

    MainEntry --> NewProducer
    MainEntry --> NewConsumer
    NewProducer --> ProdWrapper
    NewConsumer --> ConsWrapper
    ConsWrapper --> Goroutine
    Goroutine --> ConsumeFn
    MainEntry --> SendFn
    SendFn -->|"PLAINTEXT TCP"| KB
    ConsumeFn -->|"PLAINTEXT TCP"| KB
    SPMain -->|"PLAINTEXT TCP"| KB
```

---

## Package / Module Diagram

```mermaid
flowchart LR
    subgraph ModuleRoot["Module: github.com/nicholasgasior/nextgsr/kafka-example"]
        Main["package main\n(main.go)"]
        KafkaPkg["package kafka\n(kafka/)"]
        StandalonePkg["package main\n(producer/)"]
    end

    subgraph ExternalDeps["External Dependencies"]
        Sarama["github.com/IBM/sarama v1.43.3"]
    end

    Main -->|"imports"| KafkaPkg
    Main -->|"imports"| Sarama
    KafkaPkg -->|"imports"| Sarama
    StandalonePkg -->|"imports"| Sarama
```

---

## Component Breakdown

```mermaid
flowchart TB
    subgraph KafkaPkg["kafka/ package — reusable wrappers"]
        direction TB
        ConsumerStruct["Consumer struct\n- consumer sarama.Consumer\n- brokers string[]"]
        ProducerStruct["Producer struct\n- producer sarama.SyncProducer\n- brokers string[]"]

        NewConsumerFn["NewConsumer() *Consumer"]
        ConsumeFn["Consume(topic, handler)"]
        ConsCloseF["Consumer.Close()"]

        NewProducerFn["NewProducer() *Producer"]
        SendFn["Send(topic, message)"]
        ProdCloseF["Producer.Close()"]

        NewConsumerFn --> ConsumerStruct
        ConsumerStruct --> ConsumeFn
        ConsumerStruct --> ConsCloseF

        NewProducerFn --> ProducerStruct
        ProducerStruct --> SendFn
        ProducerStruct --> ProdCloseF
    end

    subgraph MainBinary["main.go — orchestration"]
        CreateConsumer["kafka.NewConsumer()"]
        CreateProducer["kafka.NewProducer()"]
        GoRoutine["go consumer.Consume() (goroutine)"]
        Loop["for i := 0 to 4\nproducer.Send()\ntime.Sleep(1s)"]
        FinalWait["time.Sleep(2s)"]
        CreateConsumer --> GoRoutine
        CreateProducer --> Loop
        Loop --> FinalWait
    end

    subgraph StandaloneBinary["producer/producer.go — inline producer"]
        InlineConfig["sarama.NewConfig()"]
        InlineProducer["sarama.NewSyncProducer()"]
        InlineLoop["for i := 0 to 4\nSendMessage()"]
        InlineConfig --> InlineProducer --> InlineLoop
    end
```

---

## Layer Dependency View

```mermaid
flowchart BT
    Sarama["Sarama Library (IBM/sarama v1.43.3)"]
    KafkaWrappers["kafka/ package\nconsumer.go + producer.go"]
    MainApp["main.go\nOrchestration Layer"]
    StandaloneApp["producer/producer.go\nStandalone Producer"]

    Sarama --> KafkaWrappers
    KafkaWrappers --> MainApp
    Sarama --> StandaloneApp
```
