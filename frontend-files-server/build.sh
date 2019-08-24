#!/usr/bin/env sh

FS_FOLDER=frontend-files-server

rm -rf ${FS_FOLDER}/target
mkdir -p ${FS_FOLDER}/target/docker

cp -r ${FS_FOLDER}/src/docker/Dockerfile \
    ${FS_FOLDER}/src/conf/default.conf \
    ${FS_FOLDER}/target/docker

cd ${FS_FOLDER}/target/docker/ || exit 1
docker build -t davinkevin/podcast-server:fs-"${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-$(date +"%s")}}" .
