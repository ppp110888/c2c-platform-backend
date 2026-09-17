# C2C Platform Backend

Spring Boot 3 and Spring Cloud backend for the C2C mobile application.

## Setup

Requirements: Java 17, Maven 3.9+, and Docker Compose.

Copy `.env.example` to `.env`, replace every placeholder, and export the same variables to the shell or IDE that starts the Java services. Never commit `.env`.

The previously committed AI key, Alipay private key, database password, MinIO credentials, and JWT secret must be revoked or rotated before deployment.

```powershell
docker compose up -d
mvn test
```

Start the four application services and then `c2c-gateway`. Only gateway port `8080` should be exposed publicly. Service ports `8081` through `8084` are loopback-only and require the internal request secret.

Payment completion is accepted only from an Alipay asynchronous notification with a valid signature and matching application ID, order number, and amount. Configure a public HTTPS `notifyUrl` before testing payments.
