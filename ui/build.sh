#!/usr/bin/env sh

UI_FOLDER=ui
VERSION="${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-$(date +"%s")}}"

rm -rf ${UI_FOLDER}/target
mkdir -p ${UI_FOLDER}/target/docker

cp -r ${UI_FOLDER}/src/docker/Dockerfile \
    ${UI_FOLDER}/src/conf/default.conf \
    ${UI_FOLDER}/target/docker

cp -r frontend-angularjs/target/dist ${UI_FOLDER}/target/docker/podcast-server

mkdir -p ${UI_FOLDER}/target/docker/podcast-server/v2/
cp frontend-angular/dist/* ${UI_FOLDER}/target/docker/podcast-server/v2/

cd ${UI_FOLDER}/target/docker/ || exit 1
docker build -t podcastserver/ui:"${VERSION}" .
docker push podcastserver/ui:"${VERSION}" .

if [ "$VERSION" == "master" ]; then
  docker tag podcastserver/ui:"${VERSION}" podcastserver/ui:latest
  docker push podcastserver/ui:latest
fi
