#FROM eclipse-temurin:21
#
#WORKDIR /app
#
#COPY target/*.jar app.jar
#
#ENTRYPOINT ["java","-jar","app.jar"]

# Stage 1

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


# Stage 2

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]