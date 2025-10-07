# Multi-stage build for order-service
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom first
COPY pom.xml ./pom.xml

# Copy common module
COPY common ./common

# Copy order-service pom and source
COPY order-service/pom.xml ./order-service/pom.xml
COPY order-service/src ./order-service/src

# Install parent POM to local Maven repository
RUN mvn -N install

# Build and install the common module to local Maven repository
WORKDIR /app/common
RUN mvn clean install -DskipTests

# Now build the order-service
WORKDIR /app/order-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/order-service/target/order-service-*.jar app.jar

# Expose the port from application.properties
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
