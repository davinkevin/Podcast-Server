Podcast-Server
==============

**Docker images** : [![Backend](https://badgen.net/badge/docker/backend/blue?icon=docker)](https://hub.docker.com/r/podcastserver/backend) [![UI](https://badgen.net/badge/docker/ui/blue?icon=docker)](https://hub.docker.com/r/podcastserver/ui) [![Storage](https://badgen.net/badge/docker/storage/blue?icon=docker)](https://hub.docker.com/r/podcastserver/storage) [![Init-db](https://badgen.net/badge/docker/init-db/blue?icon=docker)](https://hub.docker.com/r/podcastserver/init-db)

Application designed to be your Podcast local proxy in your LAN network. This projects is able to transform many source 
into an RSS feed which can be consumed from the web UI or from your favorite podcast app.

It also works on many sources like YouTube, Twitch, RSS, Apple Podcasts, France•tv, MyTF1, Gulli, Dailymotion… See **[Supported sources](https://podcast-server.davinkevin.fr/sources)** for what each one accepts, or the **[project website](https://podcast-server.davinkevin.fr/)** for screenshots, install instructions and more.

The application is available in [docker images](https://hub.docker.com/r/podcastserver/), see docker links above.

## Local Development 

**Requirements**: [mise](https://mise.jdx.dev/) and Docker.

Every other tool — k3d, Skaffold, kubectl, kustomize, mkcert… — is pinned in `mise.toml`:

* `mise install` — get them all at the expected versions
* `mkcert -install` — once, to trust the local CA

**Start**

* `mise run skaffold:dev`

`mise tasks` lists everything else (certificates, local database, storage, release…).

## Install

The full install guide lives on the project website:

* **[Deploy to production](https://podcast-server.davinkevin.fr/install/production)** — wrap the `standalone` kustomize overlay from your own kustomization, with BYO PostgreSQL/S3 supported out of the box.
* **[Local dev setup](https://podcast-server.davinkevin.fr/install/local)** — try the app on k3d with `mise run skaffold:dev`.

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

