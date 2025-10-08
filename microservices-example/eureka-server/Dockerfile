# Multi-stage build for eureka-server
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom first
COPY pom.xml ./pom.xml

# Copy eureka-server pom and source
COPY eureka-server/pom.xml ./eureka-server/pom.xml
COPY eureka-server/src ./eureka-server/src

# Install parent POM to local Maven repository
RUN mvn -N install

# Now build the eureka-server
WORKDIR /app/eureka-server
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/eureka-server/target/eureka-server-*.jar app.jar

# Expose the port from application.yml
EXPOSE 8761

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
