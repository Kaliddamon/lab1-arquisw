FROM maven:3.9.16-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Cache Maven dependencies separately from application source changes.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/gitlab.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
