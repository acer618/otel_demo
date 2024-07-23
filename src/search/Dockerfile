# Use a base image with JDK installed
FROM openjdk:17-jdk-alpine as builder

# Set the working directory inside the container
WORKDIR /usr/src/app/

# Copy the Gradle wrapper and configuration files
COPY ./src/search/gradlew /usr/src/app/gradlew
COPY ./src/search/gradle /usr/src/app/gradle
COPY ./src/search/search/build.gradle.kts /usr/src/app/search/build.gradle.kts
COPY ./src/search/settings.gradle.kts /usr/src/app/settings.gradle.kts

# Copy the source code
COPY ./src/search /usr/src/app/ 

# Ensure the Gradle wrapper script has executable permissions
RUN chmod +x ./gradlew

# Build the shadowJar
RUN ./gradlew shadowJar

EXPOSE ${BUSINESS_SEARCH_PORT}
# Set the entry point to run the application
ENTRYPOINT ["./gradlew", "runShadowJar"]

