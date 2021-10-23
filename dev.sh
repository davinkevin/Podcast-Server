#!/usr/bin/env bash

set -euo pipefail

function cleanup {
  docker stop db-for-code-generation > /dev/null
}

echo "Creation of folders mounted inside the container, for file storage"
mkdir -p /tmp/podcast-server/files/ /tmp/podcast-server/database/ /tmp/podcast-server/database/backup/ /tmp/podcast-server/files/

export DATABASE_PASSWORD=nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa
export DATABASE_USERNAME=podcast-server-user
export DATABASE_NAME=podcast-server-code-generation
export DATABASE_PORT=$RANDOM

echo "Creation of the database $DATABASE_NAME"
docker run --rm -d \
           -e POSTGRES_USER=$DATABASE_USERNAME \
           -e POSTGRES_PASSWORD=$DATABASE_PASSWORD \
           -e POSTGRES_DB=$DATABASE_NAME \
           -p $DATABASE_PORT:5432 \
           --name db-for-code-generation \
           postgres:12.3-alpine > /dev/null

trap cleanup EXIT

echo "Creation of the frontend without automatic reloading."
echo "To reload on code change, run in another term \"./gradlew -p frontend-angularjs skaffold_watch\""
./gradlew -p frontend-angularjs skaffold_build

echo "skaffold dev"
skaffold dev --status-check=false
