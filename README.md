# 🧩 Spring Boot Microservice Boilerplate

A minimal **Spring Boot + Spring Cloud + PostgreSQL** boilerplate for quickly starting new **microservices** that integrate with a **Config Server** and **Eureka Service Discovery**.

---

## 🚀 Features

- ✅ Spring Boot **3.5.7**  
- ☁️ Spring Cloud (2025.0.0) — **Eureka Client** & **Config Server** ready  
- 🗄️ PostgreSQL integration (via Spring Data JPA)  
- 🧠 Lombok support for concise and readable code  
- 🧪 Profile-based configuration (**dev** / **prod**)  
- 🐳 Two Dockerfiles:
  - `Dockerfile_dev` — optimized for local development
  - `Dockerfile_prod` — optimized for production releases

---

## 📁 Project Structure

````
spring-boot-microservice-boilerplate/
├── src/
│   ├── main/
│   │   ├── java/org/igdevx/...       # Source code
│   │   └── resources/
│   │       ├── application.yml        # Base configuration
│   │       ├── application-dev.yml    # Dev environment config (create this)
│   │       └── application-prod.yml   # Prod environment config (create this)
│   └── test/                          # Unit tests
├── Dockerfile_dev
├── Dockerfile_prod
├── pom.xml
└── README.md

````

---

## ⚙️ Configuration

### `application.yml`

```yaml
server:
  port: 5000

spring:
  application:
    name: spring-boot-microservice-boilerplate
  profiles:
    active: dev
  config:
    import: "optional:configserver:"
  datasource:
    url: "jdbc:postgresql://localhost:5432/mydb"
    username: myuser
    password: mypassword
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
````

> 🔧 **Important:**
> Replace all occurrences of
>
> ```
> spring-boot-microservice-boilerplate
> ```
>
> with the **name of your microservice**, for example:
>
> ```
> spring-boot-user-service
> ```

---

## 🌱 Environment Files

You need to create environment-specific configurations in:

* `application-dev.yml` → for local development
* `application-prod.yml` → for production deployment

Example:

```yaml
# application-dev.yml
spring:
  datasource:
    url: "jdbc:postgresql://localhost:5432/devdb"
    username: dev_user
    password: dev_password
```

```yaml
# application-prod.yml
spring:
  datasource:
    url: "jdbc:postgresql://postgres:5432/proddb"
    username: prod_user
    password: ${DB_PASSWORD}
```

> 💡 Tip: Use environment variables (`${...}`) in production and connect your service to a Config Server for centralized configuration management.

---

## 🧰 Requirements

* **Java 17+**
* **Maven 3.9+**
* **PostgreSQL** instance
* (Optional) **Spring Cloud Config Server** and **Eureka Server** for discovery and config management
* **Docker** if containerizing

---

## 🏗️ Build & Run

### Run locally

```bash
mvn spring-boot:run
```

or

```bash
./mvnw spring-boot:run
```

### Build jar

```bash
mvn clean package
```

### Run jar

```bash
java -jar target/spring-boot-microservice-boilerplate-0.0.1-SNAPSHOT.jar
```

---

## 🐳 Docker Usage

### Development build

Uses `Dockerfile_dev` for hot-reload and faster iteration.

```bash
docker build -f Dockerfile_dev -t my-microservice:dev .
docker run -p 5000:5000 my-microservice:dev
```

### Production build

Uses `Dockerfile_prod` for optimized images (no dev dependencies, layered for efficiency).

```bash
docker build -f Dockerfile_prod -t my-microservice:prod .
docker run -p 5000:5000 my-microservice:prod
```

---

## 🧭 Next Steps

* ✅ Rename all instances of `spring-boot-microservice-boilerplate` to your service name
* ✅ Update `application-dev.yml` and `application-prod.yml`
* ✅ Connect to your Config Server and Eureka
* ✅ Add your own domain logic, entities, and controllers

---

## 📄 License

This project is open-source and free to use under the [GNU GENERAL PUBLIC LICENSE](LICENSE).
