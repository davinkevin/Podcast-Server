#!/usr/bin/env bash

DOCKER_FOLDER=fake-external-podcast

rm -rf ${DOCKER_FOLDER}/target
mkdir -p ${DOCKER_FOLDER}/target/docker

cp -r ${DOCKER_FOLDER}/src/docker/Dockerfile \
    ${DOCKER_FOLDER}/src/conf/default.conf \
    ${DOCKER_FOLDER}/src/podcast \
    ${DOCKER_FOLDER}/target/docker

cd ${DOCKER_FOLDER}/target/docker/
docker build -t davinkevin/podcast-server/fake-external-podcast:latest .
