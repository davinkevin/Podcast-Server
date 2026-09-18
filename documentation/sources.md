# Supported sources

::: warning Sources are best-effort
Every source but RSS reads a platform we don't control, so any of them can break when that platform changes. A broken source just stops returning episodes; already-downloaded ones stay put. They are fixed as they break — [open an issue](https://gitlab.com/davinkevin.fr/podcast-server/-/issues) if one goes quiet.
:::

A podcast in Podcast-Server is a URL it knows how to follow. Paste one in *Add a podcast* and the server works out which source it is, fills in the title, description and cover, then keeps the feed up to date on its own.

| Source | Paste a URL like | Credentials |
| --- | --- | --- |
| [YouTube](#youtube) | `youtube.com/@handle` | Optional |
| [Twitch](#twitch) | `twitch.tv/<login>` | **Required** |
| [RSS](#rss) | any feed URL | — |
| [Apple Podcasts](#apple-podcasts) | `podcasts.apple.com/…/id123456` | — |
| [France•tv](#france•tv) | `www.france.tv/<channel>/<show>/` | — |
| [MyTF1](#mytf1) | `www.tf1.fr/<channel>/<show>` | — |
| [Dailymotion](#dailymotion) | `www.dailymotion.com/<user>` | — |
| [Gulli](#gulli) | `replay.gulli.fr/<show>` | — |
| [Upload](#upload) | — (manual) | — |

Whatever the source, the result is the same: episodes downloaded to your storage, and a clean RSS feed your podcast app can subscribe to.

## YouTube

Follows a channel or a playlist. Four URL shapes are recognised:

```
https://www.youtube.com/@handle
https://www.youtube.com/c/name
https://www.youtube.com/channel/UCxxxxxxxxxxxxxxxxxxxxxx
https://www.youtube.com/playlist?list=PLxxxxxxxx
```

Legacy `/user/<name>` URLs work too. Short `youtu.be` links do **not** — use the full `youtube.com` URL.

**Credentials are optional, and change how much history you get.** Without a [YouTube Data API key](https://developers.google.com/youtube/v3/getting-started), the server reads the channel's public Atom feed, which YouTube caps at its most recent entries. With a key set as `api.youtube` in the `podcast-server` secret, it uses the Data API instead and walks up to 500 videos back. Everything else is identical.

## Twitch

Follows a channel's VODs — the videos that survive a stream, not the stream itself.

```
https://www.twitch.tv/<login>
https://www.twitch.tv/<login>/videos
```

Channel URLs only. A single VOD, a clip or a directory page is not a channel and is not recognised.

**Credentials are required.** Twitch's API refuses anonymous calls, so without a client id and secret the Twitch integration is not registered at all and a Twitch URL is simply not recognised. Registering an application takes a couple of minutes and involves no user account: [Follow a Twitch channel](./guides/twitch) walks through it.

Two things worth knowing before you subscribe:

- **A stream in progress never becomes an episode.** It appears on the first update after the broadcast ends — otherwise the download would chase a stream that never finishes.
- **Twitch deletes stream archives** after 7 days (14 for Affiliates, 60 for Partners and Prime). Highlights and uploads never expire. You can restrict indexing to those with `PODCASTSERVER_API_TWITCH_VIDEO_TYPES=highlight,upload`.

## RSS

Any podcast feed, which is also the fallback for anything not recognised above.

```
https://example.com/feed.xml
```

Episodes come from the feed's `<enclosure>` elements — an entry without one is skipped, since there is nothing to download. Covers are taken per episode from `<media:thumbnail>`, falling back to the feed's own image.

## Apple Podcasts

A shortcut rather than a source of its own: Podcast-Server resolves the Apple listing to the underlying RSS feed and follows that.

```
https://podcasts.apple.com/fr/podcast/<name>/id123456
https://itunes.apple.com/…/id123456
```

## France•tv

Follows a show and picks up its replays.

```
https://www.france.tv/france-2/<show>/
```

Point at the show page — the server appends `/toutes-les-videos/` itself. Replays marked *indisponible* are left out. Only `www.france.tv` URLs are recognised.

## MyTF1

Follows a show on TF1.

```
https://www.tf1.fr/tf1/<show>
https://www.tf1.fr/tf1/<show>/videos
```

Ending the URL with `/replay`, `/extract` or `/bonus` restricts the podcast to that kind of video; without a suffix you get all of them. Up to 50 episodes per update.

## Dailymotion

Follows a user's videos.

```
https://www.dailymotion.com/<username>
```

Use the user page, not a video page.

## Gulli

Follows a show on Gulli's replay platform.

```
https://replay.gulli.fr/<show>
```

## Upload

Not a URL source: `Upload` podcasts hold files you add yourself, for media that lives nowhere the server can fetch. They are never refreshed automatically, and like every other podcast they are republished as an RSS feed.

## Adding credentials

Only YouTube and Twitch read anything from the `podcast-server` secret. Both go in your own kustomize overlay:

```yaml
# my-podcast-server/kustomization.yaml
secretGenerator:
  - name: podcast-server
    behavior: merge
    literals:
      - api.youtube=<your YouTube Data API key>
      - api.twitch.client-id=<your Twitch application client id>
      - api.twitch.client-secret=<your Twitch application client secret>
    type: Opaque
```

See [Deploy to production](./install/production) for the surrounding overlay, and [Follow a Twitch channel](./guides/twitch) for the Twitch credentials end to end.
