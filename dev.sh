#!/usr/bin/env bash

set -euo pipefail

function cleanup {
  docker stop db-for-code-generation > /dev/null
  k3d cluster delete podcast-server
}

echo "Creation of folders mounted inside the container, for file storage"
mkdir -p    /tmp/podcast-server/files/ \
            /tmp/podcast-server/database/backup/ \
            /tmp/podcast-server/database/init/ \
            /tmp/podcast-server/database/data/

export SKAFFOLD=true
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
           postgres:15.3-alpine > /dev/null

k3d cluster create podcast-server --port 80:80@loadbalancer --port 443:443@loadbalancer

trap cleanup EXIT

echo "Creation of the frontend without automatic reloading."
echo "To reload on code change, run in another term \"./gradlew frontend-angularjs:skaffold_build -t\""
./gradlew frontend-angularjs:skaffold_build

echo "skaffold dev"
skaffold dev --status-check=false
