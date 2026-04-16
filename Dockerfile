FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/store.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]