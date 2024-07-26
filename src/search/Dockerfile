# Use a base image with JDK installed
#FROM openjdk:17-jdk-alpine as builder
FROM docker-images.yelpcorp.com/jammy_yelp
ENV JAVA_PACKAGE=java-17-amazon-corretto-jdk
ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto


RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
        $JAVA_PACKAGE \
	iputils-ping \
        && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

#USER nobody

# Set the working directory inside the container
WORKDIR /usr/src/app/

# Copy the Gradle wrapper and configuration files
COPY ./src/search/gradlew /usr/src/app/gradlew
COPY ./src/search/gradle /usr/src/app/gradle
COPY ./src/search/settings.gradle.kts /usr/src/app/settings.gradle.kts
COPY ./src/search/search/build.gradle.kts /usr/src/app/search/build.gradle.kts

COPY ./src/search /usr/src/app/ 

#COPY ./gradlew /usr/src/app/gradlew
#COPY ./gradle /usr/src/app/gradle
#COPY ./settings.gradle.kts /usr/src/app/settings.gradle.kts
#COPY ./search/build.gradle.kts /usr/src/app/search/build.gradle.kts
#COPY ./search /usr/src/app/ 


# Ensure the Gradle wrapper script has executable permissions
#RUN chmod +x ./gradlew

# Build the shadowJar
RUN ./gradlew shadowJar

#EXPOSE ${SEARCH_PORT}
EXPOSE 9081 
# Set the entry point to run the application
ENTRYPOINT ["./gradlew", "runShadowJar"]
