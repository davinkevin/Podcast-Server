#!/usr/bin/env sh

UI_FOLDER=ui

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

echo "Injection of SWS configuration"
cp -r ${UI_FOLDER}/src/docker/* ${UI_FOLDER}/target/docker/

# V1 and V3 are bundled side by side so a single SWS instance can serve
# either one by switching SERVER_ROOT / SERVER_FALLBACK_PAGE at runtime.
# V3 is the default (see Dockerfile ENV); V1 ships as an opt-in fallback.

echo "Injection of frontend-angular (V3) files at /podcast-server/v3/"
mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v3/
cp -r frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v3/

echo "Injection of frontend-angularjs (V1) files at /podcast-server/v1/"
mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v1/
cp -r frontend-angularjs/target/dist/* ${UI_FOLDER}/target/docker/podcast-server/v1/
