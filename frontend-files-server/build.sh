#!/usr/bin/env bash

FS_FOLDER=frontend-files-server

rm -rf ${FS_FOLDER}/target
mkdir -p ${FS_FOLDER}/target/docker

cp -r ${FS_FOLDER}/src/docker/Dockerfile \
    ${FS_FOLDER}/src/conf/default.conf \
    ${FS_FOLDER}/target/docker

cd ${FS_FOLDER}/target/docker/
docker build -t davinkevin/podcast-server/files-server:latest .