#!/bin/bash

# Path to the properties file
PROPERTIES_FILE=src/main/resources/config.properties

# Read the properties file and construct the -D options
JAVA_OPTS=""
while IFS='=' read -r key value; do
    # Skip lines that are comments or empty
    [[ "$key" =~ ^#.*$ ]] && continue
    [[ -z "$key" ]] && continue
    JAVA_OPTS="$JAVA_OPTS -D$key=$value"
done < "$PROPERTIES_FILE"

java $JAVA_OPTS -cp build/libs/search-all.jar org.example.SearchClient

