# Local development setup

Try Podcast-Server — or hack on it — on a throwaway k3d cluster in ~5 minutes. Skaffold builds the images from source and live-reloads on every change.

::: warning This is the dev workflow, not a way to self-host
This page describes the **developer setup**: the cluster is k3d, images are built from source, data lives under `/tmp` and disappears when the cluster is torn down. **Don't run your real podcast library on this.** For a real install, go to [Deploy to production](./production).
:::

## Prerequisites

- [k3d](https://k3d.io/)
- [Skaffold](https://skaffold.dev/)
- [Task](https://taskfile.dev/)
- [mkcert](https://github.com/FiloSottile/mkcert) — run `mkcert -install` once to trust the local CA

## Steps

```bash
git clone https://gitlab.com/davinkevin/Podcast-Server.git
cd Podcast-Server

# 1. Generate the TLS certs for podcast.k8s.local (one-off)
task certificates:generate

# 2. Create the k3d cluster + deploy via skaffold
task skaffold:dev
```

The first run pulls base images and builds backend / ui / storage — give it a few minutes. Once skaffold reports `Deployments stabilized`, open **https://podcast.k8s.local**.

::: tip Live reload
`task skaffold:dev` keeps watching the source tree. Edit code and skaffold rebuilds and redeploys the impacted images automatically. Hit `Ctrl+C` to tear it down.
:::

::: details What does `task skaffold:dev` actually do?
1. Creates a k3d cluster named `podcast-server` with ports `80` and `443` mapped to the load-balancer (Traefik disabled).
2. Installs [Contour](https://projectcontour.io/) + the Gateway API CRDs into the cluster.
3. Runs `skaffold dev` against the `distribution/kubernetes/overlays/podcast.k8s.local` overlay — which uses `hostPath` volumes under `/tmp/podcast-server/` (no manual `mkdir` needed).
:::

::: info Architecture
The application is built for `linux/amd64` only. `arm` nodes (Raspberry Pi…) are not supported.
:::
