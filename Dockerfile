# Stage 1: build inside Docker
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /project
COPY . .
RUN mvn -DskipTests package
# Stage 2: copy JAR into slim runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /project/target/stock-ticker-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]