# Deploy to production

For a real install, layer your own kustomize overlay on top of `distribution/kubernetes/overlays/standalone/`. The standalone overlay provisions everything you need to start:

| Component   | Role                                              |
| ----------- | ------------------------------------------------- |
| `backend`   | Spring Boot API                                   |
| `ui`        | Angular web app                                   |
| `init-db`   | One-shot schema bootstrap                         |
| `database` * | PostgreSQL + nightly backup `CronJob`            |
| `storage` * | Embedded MinIO (S3-compatible object storage)     |
| `ingress`   | HTTP entry point on your chosen domain            |

::: info * Bundled but optional — bring your own
The bundled `database` and `storage` are there so the standalone overlay works out of the box. Both are **fully optional**:

- **PostgreSQL** — point Podcast-Server at any existing PostgreSQL by overriding the `database` ConfigMap (`dns`, `name`, `username`) and Secret (`password`). Then drop the bundled `database` Deployment from your overlay. Managed Postgres (RDS, Cloud SQL, [CloudNativePG](https://cloudnative-pg.io/)…) all work.
- **Object storage** — any **S3-compatible** bucket works. Override the `storage` ConfigMap (`url`, `bucket`, `username`) and Secret (`password`), then drop the bundled MinIO. Tested against MinIO, but anything implementing the S3 API will do (AWS S3, Backblaze B2, Cloudflare R2, Garage, SeaweedFS…).
:::

::: tip Just evaluating or hacking on the code?
Skip the production setup and use the [local dev path](./local) — a fresh k3d cluster, ready in 5 minutes.
:::

## Prerequisites

- A Kubernetes cluster — [k3s](https://k3s.io/), [MicroK8s](https://microk8s.io/), [minikube](https://github.com/kubernetes/minikube), Docker Desktop with Kubernetes enabled… Managed distributions (GKE, AKS, EKS) work too but are overkill for a home server.
- `kubectl` v1.27+ (built-in `kustomize` v5+) — or a standalone `kustomize` v5+.
- `linux/amd64` node(s). `arm` is not supported.

## 1. Create your own overlay

Don't apply `standalone` directly — wrap it so you can keep your customizations in git:

```yaml
# my-podcast-server/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: podcast-server

resources:
  # Pin a specific release. See the GitLab tags page for available versions.
  - https://gitlab.com/davinkevin.fr/podcast-server.git/distribution/kubernetes/overlays/standalone?ref=v6.x.y

configMapGenerator:
  - name: installation-parameters
    behavior: merge
    literals:
      - location=/opt                     # base host dir for podcast files + DB
      - domain=podcast.mydomain.local     # the Ingress hostname

secretGenerator:
  - name: database
    behavior: merge
    literals:
      - password=<change-me>
  - name: storage
    behavior: merge
    literals:
      - password=<change-me>
  - name: podcast-server
    behavior: merge
    literals:
      - api.youtube=<your YouTube Data API key>
      - api.twitch.client-id=<your Twitch application client id>
      - api.twitch.client-secret=<your Twitch application client secret>
    type: Opaque
```

::: warning Override the default passwords
The standalone overlay ships with publicly-known default passwords for the database and the object storage. **You must override them in your overlay before applying.**
:::

::: info YouTube API key is optional
Skip the `api.youtube` literal if you don't plan to consume YouTube podcasts.
:::

::: info Twitch credentials are optional
Skip both `api.twitch.*` literals if you don't plan to consume Twitch channels — without them no Twitch integration is registered at all, and a Twitch URL is simply not recognised. Note the overlays ship a `TO_BE_DEFINED` placeholder: leaving it in place registers the integration, which then fails loudly against Twitch instead of staying silent — see the health entry below.

To get them, register an application on [dev.twitch.tv/console/apps](https://dev.twitch.tv/console/apps): it hands you a client id and a client secret, which the server exchanges for an app access token on its own. No user account is involved and no OAuth redirect is needed. [Follow a Twitch channel](../guides/twitch) walks through it step by step.

Which videos get indexed is controlled by the `PODCASTSERVER_API_TWITCH_VIDEO_TYPES` environment variable, to add on the `backend` Deployment in your own overlay. It takes a comma-separated list accepting `all` (the default), `archive`, `highlight` and `upload` — for example `highlight,upload` to skip the automatic stream archives. Any other value fails the startup. It is a global setting, not a per-podcast one.

Note that Twitch deletes stream archives after 7 days (14 for Affiliates, 60 for Partners and Prime); highlights and uploads never expire. An item indexed before its archive expires stays in the database and its download eventually fails, just like an entry dropped from an RSS feed.
:::

::: tip Watching the integration
Once configured, `/actuator/health` gains a `twitch` entry reporting whether the server holds a valid app access token, and when it expires. It turns `DOWN` with a readable reason when the credentials are wrong or Twitch is unreachable, which makes the aggregate endpoint answer `503`.

It is deliberately outside the `liveness` and `readiness` groups, so a Twitch outage never restarts or removes the pod from service.
:::

## 2. Create the host directories

The standalone overlay mounts host paths for the podcast files, the database and the database backups. Create them on the node(s) where workloads will be scheduled, using the same base path you set in `location`:

```bash
export PODCAST_SERVER_LOCATION=/opt/podcast-server
mkdir -p \
  $PODCAST_SERVER_LOCATION/files/ \
  $PODCAST_SERVER_LOCATION/database/data \
  $PODCAST_SERVER_LOCATION/database/init \
  $PODCAST_SERVER_LOCATION/database/backup/
```

## 3. Apply

```bash
kubectl apply -k my-podcast-server/
```

Image pulls and the `init-db` `initContainer` take a few minutes the first time. After that, the app answers on the `domain` you configured.

## Going further

Once `standalone` is running, real-world deployments usually layer in:

- **Resource requests/limits** + JVM tuning (e.g. Shenandoah GC for the backend)
- **Observability** — `ServiceMonitor` for Prometheus, Grafana dashboards for downloads/updates
- **Secrets out of git** — [sops](https://github.com/getsops/sops) or external-secrets for credentials
- **Pinned, rolled releases** — pin image tags and add a small CronJob that rolls Deployments when a new tag ships

Each is a small kustomize component, patch, or `configMapGenerator` merge in your overlay.
