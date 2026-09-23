
# Support Ticket Management System — Backend

This is the backend foundation for the Support Ticket Management System, built using Java 21, Spring Boot, Maven, Spring Data JPA, PostgreSQL, H2 (for local/testing), and Flyway for database migrations.

---

## Project Structure

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── supportticket/
    │   │               ├── SupportTicketApplication.java
    │   │               ├── controller/
    │   │               ├── service/
    │   │               ├── repository/
    │   │               ├── entity/
    │   │               ├── dto/
    │   │               └── exception/
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │           └── migration/
    └── test/
        └── java/
            └── com/
                └── example/
                    └── supportticket/
```

### Key Packages

- `controller` — for REST controllers (empty for now)
- `service` — for business logic and services (empty for now)
- `repository` — for Spring Data JPA repositories (empty for now)
- `entity` — for JPA entities (empty for now)
- `dto` — for API and service DTOs (empty for now)
- `exception` — for custom exception classes and handlers (empty for now)

---

## Files

### pom.xml

Maven build file with dependencies (Spring Boot, JPA, PostgreSQL, H2, Flyway):

```xml
<!-- backend/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>support-ticket-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>Support Ticket Backend</name>
    <description>Backend service for the Support Ticket Management System</description>
    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
    </properties>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>${spring-boot.version}</version>
    </parent>
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <!-- PostgreSQL driver (production) -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- H2 Database (test & local dev) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- Flyway for DB Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Main Application

```java
// src/main/java/com/example/supportticket/SupportTicketApplication.java
package com.example.supportticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SupportTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportTicketApplication.class, args);
    }
}
```

---

### Application Configuration

Basic config using env variables for credentials and supporting both PostgreSQL (production) and H2 (dev/test):

```yaml
# src/main/resources/application.yml

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/support_ticket}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

---
# For local development and tests: use H2 unless DB_USE_POSTGRES=true is set
spring:
  config:
    activate:
      on-profile: h2
  datasource:
    url: jdbc:h2:mem:support_ticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
```

- Use the `h2` profile for local runs with `-Dh2` or set `SPRING_PROFILES_ACTIVE=h2`.

---

### Directory Guidance

- `controller`, `service`, `repository`, `entity`, `dto`, `exception` — **empty for now**; implement per the implementation plan.
- `db/migration` — Flyway migration files will go here (e.g., `V1__init.sql`), to be added when implementing the data model.

---

## Getting Started

1. **Java 21 required** — please install JDK 21.
2. **Local run (H2):**

    ```
    mvn spring-boot:run -Dspring-boot.run.profiles=h2
    ```

3. **With PostgreSQL:**
   - Ensure PostgreSQL is running and the database is created.
   - Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables as needed.
   - Run:

    ```
    mvn spring-boot:run
    ```

---

## Next Steps

- Implement database schema and entities (see `spec/implementation-plan.md`, step 1).
- Implement core domain classes, repositories, controllers, and services in the respective packages.

**Stay within the provided spec and plan. Do not add features or endpoints not listed in the requirements.**

---

