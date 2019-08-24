#!/usr/bin/env sh

FS_FOLDER=files-system

rm -rf ${FS_FOLDER}/target
mkdir -p ${FS_FOLDER}/target/docker

cp -r ${FS_FOLDER}/src/docker/Dockerfile \
    ${FS_FOLDER}/src/conf/default.conf \
    ${FS_FOLDER}/target/docker

cd ${FS_FOLDER}/target/docker/ || exit 1
docker build -t podcast-server/file-system:"${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-$(date +"%s")}}" .
docker push podcast-server/file-system:"${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-$(date +"%s")}}"
