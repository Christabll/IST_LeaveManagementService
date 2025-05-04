#!/bin/bash
# Automatically export all variables from .env
set -a
source .env
set +a

# Start the app
mvn spring-boot:run
