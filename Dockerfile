# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY mvnw* pom.xml .mvn/ ./

# Copy the source code
COPY src ./src

# Package the application (skip tests for speed)
RUN ./mvnw clean package -DskipTests

# Copy the built JAR file
COPY target/*.jar app.jar

# Expose the application's port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 