# --- Build stage: compiles the app using Maven + JDK 17 ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Run stage: lightweight JRE-only image to actually run the app ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/idcard-generator-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
