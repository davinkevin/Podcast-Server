#!/usr/bin/env sh

UI_FOLDER=ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

echo "Injection of SWS configuration"
cp -r ${UI_FOLDER}/src/docker/* ${UI_FOLDER}/target/docker/

echo "Injection of ui-v1 files"
cp -r frontend-angularjs/target/dist ${UI_FOLDER}/target/docker/podcast-server

echo "Injection of ui-v2 files"
mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v2/
cp frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v2/
