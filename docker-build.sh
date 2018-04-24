#!/usr/bin/env bash

set -euo pipefail

docker_build () {
   echo "build docker image davinkevin/podcast-server:$1"
   mkdir -p backend/target/docker
   cp backend/target/Podcast-Server.jar backend/src/main/docker/Dockerfile backend/target/docker
   cd backend/target/docker
   docker build . -t davinkevin/podcast-server:$1
   return
}

if [ $TRAVIS_PULL_REQUEST != false ]; then
    exit 0;
fi

if [ "$TRAVIS_BRANCH" == "dev" ]; then
  docker_build dev
  exit 0;
fi

if [ "$TRAVIS_BRANCH" == "master" ]; then
  docker_build latest
  exit 0;
fi

if [ "$TRAVIS_TAG" != "" ]; then
    docker_build $TRAVIS_TAG
    exit 0;
fi