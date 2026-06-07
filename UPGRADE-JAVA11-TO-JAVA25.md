# Java 11 → Java 25 Upgrade: Pre vs Post Comparison

This document captures every change made during the upgrade of this Kafka example project
from Java 11 / Gradle 7 / Kafka 2.x to Java 25 / Gradle 9.5.0 / Kafka 4.x.

---

## 1. Platform & Toolchain

| Dimension            | Before (Java 11)                        | After (Java 25)                              |
|----------------------|-----------------------------------------|----------------------------------------------|
| **Java version**     | 11                                      | 25                                           |
| **Gradle version**   | 7.x (system-installed `gradle`)         | 9.5.0 (project wrapper `./gradlew`)          |
| **Kotlin DSL**       | No — Groovy DSL                         | No — Groovy DSL (unchanged)                  |
| **Toolchain config** | `sourceCompatibility` / `targetCompatibility` flat properties | `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` |
| **Build invocation** | `gradle clean build installDist distZip` | `./gradlew clean build installDist`          |

### Root `build.gradle` — before
```groovy
subprojects {
    apply plugin: "java"

    targetCompatibility = '11'
    sourceCompatibility = '11'

    dependencies {
        implementation 'org.slf4j:slf4j-api:1.7.30'
        implementation 'org.slf4j:slf4j-simple:1.7.30'
    }
}
```

### Root `build.gradle` — after
```groovy
subprojects {
    apply plugin: "java"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        implementation 'org.slf4j:slf4j-api:2.0.17'
        implementation 'org.slf4j:slf4j-simple:2.0.17'
    }
}
```

**Why:** `sourceCompatibility`/`targetCompatibility` were removed from the Gradle project API in
Gradle 9.x. The Java toolchain DSL is the modern replacement and also ensures the correct JDK
is auto-provisioned at build time.

---

## 2. Dependency Versions

### kafka-common
| Dependency              | Before     | After      |
|-------------------------|------------|------------|
| `jackson-databind`      | `2.11.2`   | `2.19.0`   |
| `kafka-clients`         | `2.5.1`    | `4.2.0`    |
| `testng`                | `7.3.0`    | `7.10.2`   |

### kafka-client
| Dependency              | Before     | After      |
|-------------------------|------------|------------|
| `kafka-clients`         | `2.5.1`    | `4.2.0`    |
| `kafka-streams`         | `2.5.1`    | `4.2.0`    |
| `jcommander`            | `1.78`     | `1.82`     |
| `testng`                | `7.3.0`    | `7.10.2`   |

### kafka-service
| Dependency              | Before     | After      |
|-------------------------|------------|------------|
| `kafka-clients`         | `2.5.1`    | `4.2.0`    |
| `kafka-streams`         | `2.5.1`    | `4.2.0`    |
| `jcommander`            | `1.78`     | `1.82`     |
| `testng`                | `7.3.0`    | `7.10.2`   |

### Shared (root subprojects block)
| Dependency              | Before     | After      |
|-------------------------|------------|------------|
| `slf4j-api`             | `1.7.30`   | `2.0.17`   |
| `slf4j-simple`          | `1.7.30`   | `2.0.17`   |

**Why:** Kafka 4.x moved to SLF4J 2.x internally; keeping SLF4J 1.x would cause classpath
conflicts. All other libraries were aligned to current stable releases.

---

## 3. Gradle Application Plugin — `mainClassName` → `mainClass`

### Before (deprecated, removed in Gradle 9)
```groovy
mainClassName = 'itx.examples.kafka.client.ClientApp'
```

### After
```groovy
application {
    mainClass = 'itx.examples.kafka.client.ClientApp'
}
```

Applied to both `kafka-client/build.gradle` and `kafka-service/build.gradle`.

---

## 4. DTO Classes → Java Records

The biggest Java-language change. Both DTOs were rewritten as records, eliminating ~60 lines
of boilerplate per class.

### `ServiceRequest.java` — before (33 lines)
```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceRequest {
    private final String taskId;
    private final String clientId;
    private final String data;

    @JsonCreator
    public ServiceRequest(@JsonProperty("taskId") String taskId,
                          @JsonProperty("clientId") String clientId,
                          @JsonProperty("data") String data) {
        this.taskId = taskId;
        this.clientId = clientId;
        this.data = data;
    }

    public String getTaskId()   { return taskId; }
    public String getClientId() { return clientId; }
    public String getData()     { return data; }
}
```

### `ServiceRequest.java` — after (3 lines)
```java
public record ServiceRequest(String taskId, String clientId, String data) {
}
```

### `ServiceResponse.java` — before (40 lines)
```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceResponse {
    private final String taskId;
    private final String clientId;
    private final String data;
    private final String response;

    @JsonCreator
    public ServiceResponse(@JsonProperty("taskId") String taskId,
                           @JsonProperty("clientId") String clientId,
                           @JsonProperty("data") String data,
                           @JsonProperty("response") String response) {
        this.taskId   = taskId;
        this.clientId = clientId;
        this.data     = data;
        this.response = response;
    }

    public String getTaskId()   { return taskId; }
    public String getClientId() { return clientId; }
    public String getData()     { return data; }
    public String getResponse() { return response; }
}
```

### `ServiceResponse.java` — after (3 lines)
```java
public record ServiceResponse(String taskId, String clientId, String data, String response) {
}
```

**What records give you for free:**
- Canonical constructor
- Immutable private final fields
- `toString()`, `equals()`, `hashCode()`
- Accessor methods (`taskId()`, `clientId()`, etc.)
- No Jackson `@JsonCreator` / `@JsonProperty` needed — Jackson 2.12+ maps records natively

---

## 5. Accessor Style — Getters → Record Accessors

Every call-site that used JavaBean-style getters was updated to use the record accessor (no `get` prefix).

| Before                         | After                    |
|--------------------------------|--------------------------|
| `request.getTaskId()`          | `request.taskId()`       |
| `request.getClientId()`        | `request.clientId()`     |
| `request.getData()`            | `request.data()`         |
| `response.getTaskId()`         | `response.taskId()`      |
| `response.getClientId()`       | `response.clientId()`    |
| `response.getData()`           | `response.data()`        |
| `response.getResponse()`       | `response.response()`    |

Affected files: `ClientApp.java`, `ProcessingServiceClient.java`,
`ProcessingServiceBackend.java`, `BasicTests.java`.

---

## 6. Virtual Threads (`ProcessingServiceClient`)

### Before — platform thread pool
```java
this.executor = Executors.newSingleThreadExecutor();
```

### After — virtual thread per task (Project Loom, GA in Java 21)
```java
this.executor = Executors.newVirtualThreadPerTaskExecutor();
```

**Why:** Virtual threads are lightweight (JVM-managed, not OS-managed). Each Kafka
consumer-job future now runs on its own virtual thread with near-zero scheduling overhead,
enabling high message-count parallelism without a dedicated thread pool.

---

## 7. Collection Factory — `Collections.singletonList()` → `List.of()`

### Before
```java
import java.util.Collection;
import java.util.Collections;

Collection<String> topics = Collections.singletonList(TOPIC_SERVICE_RESPONSES);
this.consumer.subscribe(topics);
```

### After
```java
import java.util.List;

this.consumer.subscribe(List.of(TOPIC_SERVICE_RESPONSES));
```

Applied in both `ProcessingServiceClient.java` and `ProcessingServiceBackend.java`.

**Why:** `List.of()` (Java 9+) is the idiomatic immutable-list factory. It is more concise,
clearly communicates immutability, and avoids the legacy `Collections` utility class.

---

## 8. Local Variable Type Inference — Explicit Types → `var`

`var` (Java 10+) was applied to all local variables where the type is obvious from the
right-hand side, reducing verbosity without sacrificing readability.

### Example — `ClientApp.java`
```java
// Before
Arguments arguments = new Arguments();
ProcessingServiceClient processingService = new ProcessingServiceClient(...);
String taskId = UUID.randomUUID().toString();
ServiceRequest serviceRequest = new ServiceRequest(...);
ServiceResponse serviceResponse = process.get();
String eval = checkResponse(serviceRequest, serviceResponse);

// After
var arguments = new Arguments();
var processingService = new ProcessingServiceClient(...);
var taskId = UUID.randomUUID().toString();
var serviceRequest = new ServiceRequest(...);
var serviceResponse = process.get();
var eval = checkResponse(serviceRequest, serviceResponse);
```

Also applied in: `ProcessingServiceClient.java`, `ProcessingServiceBackend.java`,
`ServiceApp.java`, `BasicTests.java`.

---

## 9. Shutdown Hook — Anonymous Class → Lambda

### Before
```java
Runtime.getRuntime().addShutdownHook(new Thread() {
    @Override
    public void run() {
        processingServiceBackend.shutdown();
    }
});
```

### After
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> processingServiceBackend.shutdown()));
```

**Why:** `Runnable` is a functional interface. Lambda syntax is more concise and avoids
the anonymous inner-class boilerplate.

---

## 10. Exception Handling — `JsonProcessingException` → `IOException`

### Before (`DataMapper.java`)
```java
import com.fasterxml.jackson.core.JsonProcessingException;

public Bytes serialize(Object data) throws JsonProcessingException { ... }
```

### After (`DataMapper.java`)
```java
public Bytes serialize(Object data) throws IOException { ... }
```

### Before (`ProcessingServiceClient.java`)
```java
import com.fasterxml.jackson.core.JsonProcessingException;
...
} catch (JsonProcessingException e) {
```

### After (`ProcessingServiceClient.java`)
```java
import java.io.IOException;
...
} catch (IOException e) {
```

**Why:** `JsonProcessingException` extends `IOException`. Widening the throws clause to
`IOException` removes the Jackson type from `DataMapper`'s public API, so `kafka-client`
no longer needs `jackson-core` on its compile classpath. This enforces a cleaner module
boundary — only `kafka-common` has a direct Jackson dependency.

---

## 11. Conditional Expression — `if/return` → Ternary

### Before (`ClientApp.java`)
```java
public static String checkResponse(ServiceRequest request, ServiceResponse response) {
    if (request.getTaskId().equals(response.getTaskId())
            && request.getClientId().equals(response.getClientId())
            && request.getData().equals(response.getData())) {
        return "OK";
    }
    return "ERROR";
}
```

### After
```java
public static String checkResponse(ServiceRequest request, ServiceResponse response) {
    return (request.taskId().equals(response.taskId())
            && request.clientId().equals(response.clientId())
            && request.data().equals(response.data())) ? "OK" : "ERROR";
}
```

---

## 12. Kafka Broker Setup — ZooKeeper → KRaft

| Aspect                  | Before (Kafka 2.x)                              | After (Kafka 4.x)                                         |
|-------------------------|-------------------------------------------------|-----------------------------------------------------------|
| **Coordination layer**  | Apache ZooKeeper (separate process required)    | KRaft (built into the broker — no extra process)          |
| **Startup commands**    | `zookeeper-server-start.sh` + `kafka-server-start.sh` | `kafka-storage.sh format` + `kafka-server-start.sh` |
| **Config file**         | `config/server.properties`                      | `config/kraft/reconfig-server.properties`                 |
| **Storage format**      | ZooKeeper-managed                               | `kafka-storage.sh format --standalone -t <UUID> -c ...`   |
| **Operational overhead**| Two processes to manage, monitor, and secure    | Single process                                            |

### Before — start commands
```bash
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

### After — start commands
```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/kraft/reconfig-server.properties
bin/kafka-server-start.sh config/kraft/reconfig-server.properties
```

---

## 13. Lines-of-Code Impact

| File                        | Before (lines) | After (lines) | Delta     |
|-----------------------------|---------------|---------------|-----------|
| `ServiceRequest.java`       | 33            | 3             | **−30**   |
| `ServiceResponse.java`      | 40            | 3             | **−37**   |
| `DataMapper.java`           | 22            | 21            | −1        |
| `ClientApp.java`            | 55            | 52            | −3        |
| `ProcessingServiceClient.java` | 85         | 80            | −5        |
| `ProcessingServiceBackend.java` | 90        | 84            | −6        |
| `ServiceApp.java`           | 30            | 24            | −6        |
| `BasicTests.java`           | 20            | 18            | −2        |
| **Total source reduction**  |               |               | **−90 lines** |

---

## 14. Summary of Java Language Features Used

| Feature                         | Java Version Introduced | Used In                                      |
|---------------------------------|------------------------|----------------------------------------------|
| `var` (local type inference)    | Java 10                | All 5 modified source files                  |
| Records                         | Java 16 (GA)           | `ServiceRequest`, `ServiceResponse`          |
| `List.of()` factory             | Java 9                 | `ProcessingServiceClient`, `ProcessingServiceBackend` |
| Virtual threads                 | Java 21 (GA)           | `ProcessingServiceClient`                    |
| Lambda expressions              | Java 8                 | `ServiceApp` shutdown hook                   |
| Ternary expression (style)      | Java 1.0               | `ClientApp.checkResponse()`                  |

