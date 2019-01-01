#!/usr/bin/env bash

DESTINATION=/shares/backup/BDD/podcastserver/

java -cp h2/bin/h2*.jar org.h2.tools.Script \
    -url jdbc:h2:tcp://localhost:1521/podcastserver \
    -user sa \
    -script podcast-server-$(date +"%Y-%m-%dT%H-%M-%S").zip \
    -options compression zip

mv data/*.zip ${DESTINATION}
chown -R podcast-server:podcast-server ${DESTINATION}