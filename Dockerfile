# Use Java 17 runtime image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the JAR built by GitHub Actions
COPY target/*.jar app.jar

# Expose the port your Spring Boot app uses
EXPOSE 5000

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
