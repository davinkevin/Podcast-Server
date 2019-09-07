#!/usr/bin/env sh

FS_FOLDER=files-system

rm -rf ${FS_FOLDER}/target
mkdir -p ${FS_FOLDER}/target/docker

cp -r ${FS_FOLDER}/src/docker/Dockerfile \
    ${FS_FOLDER}/src/conf/default.conf \
    ${FS_FOLDER}/target/docker
