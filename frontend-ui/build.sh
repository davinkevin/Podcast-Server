#!/usr/bin/env bash

UI_FOLDER=frontend-ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

cp -r ${UI_FOLDER}/src/docker/Dockerfile \
    ${UI_FOLDER}/src/conf/default.conf \
    ${UI_FOLDER}/target/docker

cp -r frontend-angularjs/target/dist ${UI_FOLDER}/target/docker/podcast-server

cd ${UI_FOLDER}/target/docker/
docker build -t davinkevin/podcast-server/ui .