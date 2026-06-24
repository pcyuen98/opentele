FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/opentele-stacktrace-1.0.0.jar app.jar

EXPOSE 5080

ENTRYPOINT ["java", "-jar", "app.jar"]