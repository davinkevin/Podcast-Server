#!/usr/bin/env sh

UI_FOLDER=ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

echo "Injection of SWS configuration"
cp -r ${UI_FOLDER}/src/docker/* ${UI_FOLDER}/target/docker/

echo "Injection of ui-v1 files (served at /)"
cp -r frontend-angularjs/target/dist ${UI_FOLDER}/target/docker/podcast-server

echo "Injection of frontend-angular files (served at /v3/)"
mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v3/
cp -r frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v3/
