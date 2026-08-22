#!/usr/bin/env sh

UI_FOLDER=ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

echo "Injection of SWS configuration"
cp -r ${UI_FOLDER}/src/docker/* ${UI_FOLDER}/target/docker/

echo "Injection of frontend-angular (V3) files at /podcast-server/v3/"
mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v3/
cp -r frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v3/

