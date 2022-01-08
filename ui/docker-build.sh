#!/usr/bin/env sh

UI_FOLDER=ui
VERSION="${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-$(date +"%s")}}"

./${UI_FOLDER}/compile.sh

cd ${UI_FOLDER}/target/docker/ || exit 1
docker build -t podcastserver/ui:"${VERSION}" .
[ -n "$CI" ] && docker push "podcastserver/ui:${VERSION}"

if [ "$VERSION" == "$CI_DEFAULT_BRANCH" ]; then
   docker tag "podcastserver/ui:${VERSION}" podcastserver/ui:latest
   [ -n "$CI" ] && docker push podcastserver/ui:latest
fi
