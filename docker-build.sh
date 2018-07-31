#!/usr/bin/env bash

set -euo pipefail

docker_build_with_apm () {
   echo "build docker image davinkevin/podcast-server:$1-with-apm"
   mkdir -p backend/target/docker
   cp backend/src/main/docker/apm/Dockerfile backend/target/docker/

   curl -L -o /tmp/glowroot.zip https://github.com/glowroot/glowroot/releases/download/v0.10.8/glowroot-0.10.8-dist.zip
   unzip -o /tmp/glowroot.zip -d /tmp/
   cp -r /tmp/glowroot backend/target/docker/
   cp backend/src/main/docker/apm/admin.json backend/target/docker/glowroot/

   cd backend/target/docker
   docker build . -t davinkevin/podcast-server:$1-with-apm

   rm -rf /tmp/glowroot.zip /tmp/glowroot backend/target/docker
   return
}

docker_build () {
   echo "build docker image davinkevin/podcast-server:$1"
   mkdir -p backend/target/docker
   cp backend/target/Podcast-Server.jar backend/src/main/docker/main/Dockerfile backend/target/docker/
   cd backend/target/docker
   docker build . -t davinkevin/podcast-server:$1
   cd ../../..
   rm -rf backend/target/docker/*
   return
}

if [ $TRAVIS_PULL_REQUEST != false ]; then
    exit 0;
fi

if [ "$TRAVIS_BRANCH" == "dev" ]; then
  docker_build dev
  docker_build_with_apm dev
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