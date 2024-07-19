#!/bin/bash

# Path to the properties file
PROPERTIES_FILE=src/main/resources/config.properties

# Read the properties file and construct the -D options
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
while IFS='=' read -r key value; do
    # Skip lines that are comments or empty
    [[ "$key" =~ ^#.*$ ]] && continue
    [[ -z "$key" ]] && continue
    JAVA_OPTS="$JAVA_OPTS -D$key=$value"
done < "$PROPERTIES_FILE"

java $JAVA_OPTS -cp build/libs/business_search-all.jar org.example.HttpServer

