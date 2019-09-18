#!/usr/bin/env bash

set -euo pipefail

echo "Creation of folders mounted inside the container, for file storage"
mkdir -p /tmp/podcast-server/files/ /tmp/podcast-server/database/ /tmp/podcast-server/database/backup/ /tmp/podcast-server/files/

echo "Creation of the database"
./mvnw -f backend/pom.xml liquibase:dropAll liquibase:update -Ddatabase.url=jdbc:h2:/tmp/podcast-server/database/podcast-server

echo "Creation of the frontend without automatic reload, to reload, launch in another term \"./mvn -f frontend-angularjs/pom.xml frontend:gulp@skaffold-watch\""
./mvnw -f frontend-angularjs/pom.xml frontend:gulp@skaffold

echo "skaffold dev"
skaffold dev
