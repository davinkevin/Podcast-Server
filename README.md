Podcast-Server
==============

**Back-end** : [![Codacy Badge](https://app.codacy.com/project/badge/Grade/1cf045bbebc94d8fb99c19a53f794ad6)](https://www.codacy.com/manual/davin-kevin/Podcast-Server?utm_source=gitlab.com&amp;utm_medium=referral&amp;utm_content=davinkevin/Podcast-Server&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gl/davinkevin/Podcast-Server/branch/master/graph/badge.svg)](https://codecov.io/gl/davinkevin/Podcast-Server)

**Front-end** : [![Code Climate](https://codeclimate.com/github/davinkevin/Podcast-Server/badges/gpa.svg)](https://codeclimate.com/github/davinkevin/Podcast-Server)

**Docker images** : [![Backend](https://badgen.net/badge/docker/backend/blue?icon=docker)](https://hub.docker.com/r/podcastserver/backend) [![UI](https://badgen.net/badge/docker/ui/blue?icon=docker)](https://hub.docker.com/r/podcastserver/ui) [![File-System](https://badgen.net/badge/docker/file-system/blue?icon=docker)](https://hub.docker.com/r/podcastserver/file-system) [![Init-db](https://badgen.net/badge/docker/init-db/blue?icon=docker)](https://hub.docker.com/r/podcastserver/init-db)

Application designed to be your Podcast local proxy in your LAN network. This projects is able to transform many source 
into an RSS feed which can be consumed from the web UI or from your favorite podcast app.

It also works on many sources like Youtube, Dailymotion, CanalPlus… Check this 
http://davinkevin.github.io/Podcast-Server/ and enjoy !

The application is available in [docker images](https://hub.docker.com/r/podcastserver/), see docker links above.

## Local Development 

**Requirements**: 
* [k3d](https://k3d.io/v5.6.0/)
* [Taskfile](https://taskfile.dev/)
* [Skaffold](https://skaffold.dev/)
* [mkcert](https://mkcert.dev)

**Start**

* `task skaffold:dev` 

## Install 

To see how to install the application, follow the [install documentation](https://gitlab.com/davinkevin/Podcast-Server/-/blob/master/documentation/modules/ROOT/pages/installation/)

## Support

Big thanks to all OpenSource program offering to the project licenses for wonderful tools!

<img src="https://download.davinkevin.fr/project/podcast-server/jetbrains-logo.png" alt="Jetbrains" width="150"/>
<img src="https://download.davinkevin.fr/project/podcast-server/yourkit-logo.png" alt="Jetbrains" width="200"/>

## License

Copyright 2023 DAVIN KEVIN

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

