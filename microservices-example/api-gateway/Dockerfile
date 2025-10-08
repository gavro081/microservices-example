# Multi-stage build for api-gateway
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom first
COPY pom.xml ./pom.xml

# Copy common module (if api-gateway needs it later)
COPY common ./common

# Copy api-gateway pom and source
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src

# Install parent POM to local Maven repository
RUN mvn -N install

# Now build the api-gateway
WORKDIR /app/api-gateway
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar

# Expose the port from application.yml
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
