#!/usr/bin/env sh

UI_FOLDER=ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

cp -r ${UI_FOLDER}/src/docker/Dockerfile \
    ${UI_FOLDER}/src/conf/default.conf \
    ${UI_FOLDER}/target/docker

cp -r frontend-angularjs/target/dist ${UI_FOLDER}/target/docker/podcast-server

mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v2/
cp frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v2/
