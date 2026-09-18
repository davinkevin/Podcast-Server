# Follow a Twitch channel

Point Podcast-Server at `https://www.twitch.tv/<login>` and the channel's VODs become downloadable items, the same way a YouTube channel does.

This needs credentials: Twitch's API refuses anonymous calls. Without them nothing breaks — the Twitch integration is simply not registered, and a Twitch URL is not recognised.

This guide assumes a running install, wrapped in your own overlay as described in [Deploy to production](../install/production). For what the other sources accept, see [Supported sources](../sources).

## 1. Create a Twitch application

Credentials identify *your server*, not a Twitch account: there is no login flow, no consent screen, and the server never acts on behalf of a user.

1. Sign in on [dev.twitch.tv/console/apps](https://dev.twitch.tv/console/apps) with any Twitch account (two-factor authentication must be enabled on it — Twitch refuses to create an application otherwise).
2. **Register Your Application**, and fill in:
   - **Name** — anything unique across Twitch, e.g. `podcast-server-<yourname>`.
   - **OAuth Redirect URLs** — `http://localhost`. The field is mandatory but unused: the client credentials grant never redirects anywhere.
   - **Category** — *Application Integration*.
   - **Client Type** — *Confidential*. This is what allows the client credentials grant.
3. Open the application with **Manage**, copy the **Client ID**, then **New Secret** and copy the secret — it is shown once.

Generating a new secret immediately invalidates the previous one, so do it when you are ready to paste it somewhere.

## 2. Check the credentials before wiring them in

Thirty seconds here saves debugging the server later:

```bash
export TWITCH_CLIENT_ID=<your client id>
export TWITCH_CLIENT_SECRET=<your client secret>

# negotiate an app access token — this is exactly what the server does
export TWITCH_TOKEN=$(curl -s -X POST 'https://id.twitch.tv/oauth2/token' \
  -d "client_id=$TWITCH_CLIENT_ID" \
  -d "client_secret=$TWITCH_CLIENT_SECRET" \
  -d 'grant_type=client_credentials' | jq -r .access_token)

# resolve a channel, the call the finder makes
curl -s 'https://api.twitch.tv/helix/users?login=twitchdev' \
  -H "Client-Id: $TWITCH_CLIENT_ID" \
  -H "Authorization: Bearer $TWITCH_TOKEN" | jq '.data[0] | {id, display_name, description}'
```

A working pair prints the channel. A bad one answers `{"status":400,"message":"invalid client"}` on the first call, and `$TWITCH_TOKEN` ends up as `null`.

The token is valid for about 58 days. The server negotiates one lazily and reuses it, so it does not spend a round trip per call.

## 3. Add them to your overlay

Both values live in the `podcast-server` secret, next to the YouTube key:

```yaml
# my-podcast-server/kustomization.yaml
secretGenerator:
  - name: podcast-server
    behavior: merge
    literals:
      - api.twitch.client-id=<your client id>
      - api.twitch.client-secret=<your client secret>
    type: Opaque
```

```bash
kubectl apply -k my-podcast-server/
kubectl -n podcast-server rollout status deployment/backend
```

No manual restart is needed: kustomize suffixes generated secrets with a hash of their content, so changing a literal produces a new secret name, which updates the Deployment and rolls the pod on its own.

::: warning Keep the secret out of git
Your overlay is a git repository like any other. Committing the client secret there publishes it. Use [sops](https://github.com/getsops/sops) or [external-secrets](https://external-secrets.io/) rather than a plain literal, as suggested in the *Going further* section of the [production guide](../install/production).
:::

## 4. Choose which videos are indexed

Optional — by default every video of the channel is indexed. The setting is an environment variable on the `backend` Deployment, so it goes through a patch rather than the secret:

```yaml
# my-podcast-server/kustomization.yaml
patches:
  - target:
      kind: Deployment
      name: backend
    patch: |
      - op: add
        path: /spec/template/spec/containers/0/env/-
        value:
          name: PODCASTSERVER_API_TWITCH_VIDEO_TYPES
          value: highlight,upload
```

`PODCASTSERVER_API_TWITCH_VIDEO_TYPES` takes a comma-separated list:

| value | what it covers |
| --- | --- |
| `all` | everything (the default) |
| `archive` | automatic recordings of past streams — the ones Twitch deletes after 7 to 60 days |
| `highlight` | clips the streamer saved from a broadcast |
| `upload` | videos uploaded directly to the channel |

`highlight,upload` indexes only what never expires. `all` combined with anything else collapses to `all`, and any other value stops the startup with an explicit message. It applies to every Twitch podcast — it is not a per-podcast setting.

## 5. Walk through it

Below, `$PODCAST_SERVER` is the `domain` you configured — the same host the web app answers on. Both `/api` and `/actuator` are routed to the backend by the bundled Ingress.

```bash
export PODCAST_SERVER=https://podcast.mydomain.local
```

**Twitch is offered as a type.** It appears as soon as the integration is registered, and is absent otherwise:

```bash
curl -s "$PODCAST_SERVER/api/v1/podcasts/types" | jq '.content[].key'
```

**The channel is detected.** In the web app, *Add a podcast* → paste `https://www.twitch.tv/twitchdev` → title, description and cover fill in, typed `Twitch`. The same thing from the shell:

```bash
curl -s -X POST "$PODCAST_SERVER/api/v1/podcasts/find" \
  -H 'Content-Type: text/plain' -d 'https://www.twitch.tv/twitchdev' | jq
```

**The VODs are listed.** Create the podcast, then update it — the ⋮ menu in the web app, or:

```bash
curl -s "$PODCAST_SERVER/api/v1/podcasts/$PODCAST_ID/update"
```

Items appear with their title, publication date and cover. A channel that only streams and never highlights anything may legitimately have none: Twitch deletes stream archives after 7 days (14 for Affiliates, 60 for Partners and Prime). Pick a channel with highlights or uploads — `twitchdev` is a reliable one.

**A VOD downloads.** Hit download on an item and watch it complete as an `.mp4`.

**A live broadcast produces nothing.** Point at a channel that is streaming right now: the in-progress broadcast must *not* become an item, and re-running the update must not change the podcast signature. The item shows up on the first update after the stream ends.

**Health reports the token.** `/actuator/health` gains a `twitch` entry:

```bash
curl -s "$PODCAST_SERVER/actuator/health" | jq '.status, .components.twitch'
```

Before anything hits Twitch it reads `{"token": "absent", "reason": "no token negotiated yet"}` and is `UP`. After a successful call it turns to `{"token": "present", …}` with the obtention and expiry dates. With wrong credentials it goes `DOWN` and the aggregate endpoint answers `503` — while `/actuator/health/liveness` and `/actuator/health/readiness` stay `UP`, so a Twitch outage never restarts the pod.

## Troubleshooting

| What you see | What it means |
| --- | --- |
| `Twitch` missing from `/api/v1/podcasts/types` | No credentials reached the server — one of them is empty or blank. Both must be set for anything Twitch to be registered. |
| No `twitch` entry in `/actuator/health` | Same cause. The indicator only exists when the integration does. |
| `twitch` is `DOWN` with `invalid client` | The client id and secret don't match. Regenerating the secret invalidates the old one — check you pasted the new one. |
| Pasting a Twitch URL fails while Twitch is unconfigured | Expected. Unrecognised URLs fall through to the RSS finder, which fails on an HTML page — the same as pasting any other non-feed URL. |
| The channel is detected but has no item | Either its archives have expired, or it is streaming right now and the in-progress broadcast is deliberately skipped. |

## Not covered

Clips (`/<login>/clip/<slug>`) and collections are out of scope, as is recording live streams as they happen.
