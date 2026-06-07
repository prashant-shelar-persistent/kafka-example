# Executive Summary

## Project Description

**kafka-example** is a minimal, educational Go application demonstrating the fundamentals of Apache Kafka messaging using the Sarama client library. It showcases a complete producer–consumer loop: a producer sends five numbered messages to a Kafka topic (`test-topic`) while a concurrent goroutine-based consumer reads and prints those messages. A standalone producer binary is also provided as an alternative entry point.

The project is intentionally small and focused — it serves as a reference implementation for anyone learning how to integrate Go applications with Kafka.

---

## Key Metrics

| Metric | Value |
|---|---|
| Total Source Files | 4 Go files |
| Lines of Code (est.) | ~175 LOC |
| Go Packages | 3 (`main`, `kafka`, standalone `producer/main`) |
| Entry Points | 2 (`main.go`, `producer/producer.go`) |
| Kafka Topics Used | 1 (`test-topic`) |
| Partitions Consumed | 1 (partition 0, offset newest) |
| Infrastructure Files | 1 (`docker-compose.yml`) |
| Test Files | None |

---

## Technology Stack

| Technology | Version | Role |
|---|---|---|
| Go | 1.21 | Primary programming language |
| Apache Kafka | (via Docker: `confluentinc/cp-kafka:latest`) | Message broker |
| Apache ZooKeeper | (via Docker: `confluentinc/cp-zookeeper:latest`) | Kafka coordination service |
| Docker Compose | v3 | Local infrastructure orchestration |

---

## Open Source Dependencies

| Library | Version | Purpose | License | Category |
|---|---|---|---|---|
| `github.com/IBM/sarama` | v1.43.3 | Kafka client — producer and consumer APIs | MIT | Messaging / Framework |
| `github.com/eapache/go-resiliency` | v1.7.0 | Retry and circuit-breaker primitives (Sarama internal) | MIT | Reliability |
| `github.com/eapache/go-xerial-snappy` | v0.0.0-20230731 | Snappy compression codec (Sarama internal) | MIT | Compression |
| `github.com/eapache/queue` | v1.1.0 | Efficient queue data structure (Sarama internal) | MIT | Data Structures |
| `github.com/golang/snappy` | v0.0.4 | Snappy compression (Sarama internal) | BSD-3-Clause | Compression |
| `github.com/hashicorp/errwrap` | v1.0.0 | Error wrapping utility (Sarama transitive dep) | MPL-2.0 | Error Handling |
| `github.com/hashicorp/go-multierror` | v1.1.1 | Multiple-error aggregation (Sarama transitive dep) | MPL-2.0 | Error Handling |
| `github.com/jcmturner/aescts/v2` | v2.0.0 | AES cipher suites for Kerberos (Sarama GSSAPI) | Apache-2.0 | Security |
| `github.com/jcmturner/dnsutils/v2` | v2.0.0 | DNS utilities for Kerberos (Sarama GSSAPI) | Apache-2.0 | Security |
| `github.com/jcmturner/gofork` | v1.7.6 | Patched Go stdlib forks (Sarama GSSAPI) | Apache-2.0 | Security |
| `github.com/jcmturner/gokrb5/v8` | v8.4.4 | Kerberos 5 authentication (Sarama GSSAPI) | Apache-2.0 | Security / Auth |
| `github.com/jcmturner/rpc/v2` | v2.0.3 | RPC primitives for Kerberos (Sarama GSSAPI) | Apache-2.0 | Security |
| `github.com/klauspost/compress` | v1.17.9 | Multi-algorithm compression (Sarama internal) | MIT/Apache | Compression |
| `github.com/pierrec/lz4/v4` | v4.1.21 | LZ4 compression codec (Sarama internal) | BSD-2-Clause | Compression |
| `github.com/rcrowley/go-metrics` | v0.0.0-20201227 | Metrics collection (Sarama internal) | BSD-2-Clause | Observability |
| `github.com/davecgh/go-spew` | v1.1.1 | Deep-print for debugging (Sarama test dep) | ISC | Debug / Testing |
| `golang.org/x/crypto` | v0.26.0 | Cryptographic primitives (TLS/SASL for Sarama) | BSD-3-Clause | Security |
| `golang.org/x/net` | v0.28.0 | Extended networking (Sarama internal) | BSD-3-Clause | Networking |

> All application-level dependencies are transitively pulled in by `github.com/IBM/sarama`. The application itself has no direct runtime dependencies beyond Sarama.
