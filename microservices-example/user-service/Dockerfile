# Multi-stage build for user-service
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom first
COPY pom.xml ./pom.xml

# Copy common module
COPY common ./common

# Copy user-service pom and source
COPY user-service/pom.xml ./user-service/pom.xml
COPY user-service/src ./user-service/src

# Install parent POM to local Maven repository
RUN mvn -N install

# Build and install the common module to local Maven repository
WORKDIR /app/common
RUN mvn clean install -DskipTests

# Now build the user-service
WORKDIR /app/user-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/user-service/target/user-service-*.jar app.jar

# Expose the port from application.properties
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
