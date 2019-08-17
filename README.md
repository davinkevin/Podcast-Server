Podcast-Server
==============

[![Join the chat at https://gitter.im/davinkevin/Podcast-Server](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/davinkevin/Podcast-Server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/davinkevin/Podcast-Server.svg?branch=master)](https://travis-ci.org/davinkevin/Podcast-Server) 

Back-end : [![Codacy Badge](https://api.codacy.com/project/badge/Grade/2030290b1c2145f6878e9ad7811c542e)](https://www.codacy.com/app/davin-kevin/Podcast-Server?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=davinkevin/Podcast-Server&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/davinkevin/Podcast-Server/badge.svg?branch=master)](https://coveralls.io/r/davinkevin/Podcast-Server?branch=master)

Migration : ![Java stage](https://badgen.net/badge/Java/7%25/orange) ![Kotlin stage](https://badgen.net/badge/Kotlin/93%25/purple)

Front-end : [![Code Climate](https://codeclimate.com/github/davinkevin/Podcast-Server/badges/gpa.svg)](https://codeclimate.com/github/davinkevin/Podcast-Server)

Docker : [![Image Layer IO](https://badge.imagelayers.io/davinkevin/podcast-server:latest.svg)](https://imagelayers.io/?images=davinkevin/podcast-server:latest 'Get your own badge on imagelayers.io')

Application design to be your Podcast local proxy in your lan network.

It also works on many sources like Youtube, Dailymotion, CanalPlus... Check this http://davinkevin.github.io/Podcast-Server/ and enjoy !

The application is available in [fat-jar](https://github.com/davinkevin/Podcast-Server/releases) format or in [docker images](https://hub.docker.com/r/davinkevin/podcast-server/) 

## Run in local env: 

### Building components: 

* building base-image: `docker build -t davinkevin/podcast-server-base-image:latest -f backend/src/main/docker/base-image/Dockerfile .`
* building backend: `mvn clean liquibase:dropAll liquibase:update jooq-codegen:generate compile jib:dockerBuild -Ddatabase.url=jdbc:h2:/tmp/podcast-server -Dtag=local-dev`

### Start components one by one

* backend: `docker run --rm -it --link podcast-server-database:podcast-server-database -p 8080:8080 -v /tmp/podcast-server:/tmp/podcast-server -e SPRING_DATASOURCE_URL="jdbc:h2:tcp://podcast-server-database:1521/podcast-server" davinkevin/podcast-server:local-dev`
* nginx file server: `docker run --rm -it -p 8181:80 -v /tmp/podcast-server/:/var/www/podcast-server-files/data/ davinkevin/podcast-server/files-server:latest`
* h2 database: `docker run --rm -it -p 8999:81 -p 1521:1521 -v /tmp/h2-podcast-server:/opt/h2-data --name podcast-server-database davinkevin/podcast-server/database:latest`
* Update model of the database: `mvn -f backend/pom.xml liquibase:dropAll liquibase:update -Ddatabase.url=jdbc:h2:tcp://localhost:1521/podcast-server` 
* frontend: `./target/node/npm run serve`

## License

Copyright 2018 DAVIN KEVIN

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

