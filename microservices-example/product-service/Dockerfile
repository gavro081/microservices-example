# Multi-stage build for product-service
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom first
COPY pom.xml ./pom.xml

# Copy common module
COPY common ./common

# Copy product-service pom and source
COPY product-service/pom.xml ./product-service/pom.xml
COPY product-service/src ./product-service/src

# Install parent POM to local Maven repository
RUN mvn -N install

# Build and install the common module to local Maven repository
WORKDIR /app/common
RUN mvn clean install -DskipTests

# Now build the product-service
WORKDIR /app/product-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/product-service/target/product-service-*.jar app.jar

# Expose the port from application.properties
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
