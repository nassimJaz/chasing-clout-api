# The API as a container, used by docker-compose.demo.yml. Tests are skipped here: they need the database (mvn test).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# dependencies in their own layer: only downloaded again when pom.xml changes
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN useradd --system app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
