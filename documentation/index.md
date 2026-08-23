---
layout: home

hero:
  name: Podcast Server
  text: Self-host every podcast in your home network.
  tagline: Aggregate YouTube, RSS, France•tv, MyTF1 and more into a single feed your favourite podcast app can consume.
  image:
    light: /logo-light.svg
    dark: /logo-dark.svg
    alt: Podcast Server
  actions:
    - theme: brand
      text: Install on your cluster
      link: /install/production
    - theme: alt
      text: View on GitLab
      link: https://gitlab.com/davinkevin.fr/podcast-server

features:
  - icon: 📡
    title: Many sources, one feed
    details: YouTube, RSS, France•tv, Dailymotion, MyTF1, Gulli — plus direct uploads. Turn anything into a podcast.
  - icon: ⚙️
    title: Kubernetes native
    details: Ships as a kustomize overlay. Deploy to k3s, MicroK8s, minikube or Docker Desktop in minutes.
  - icon: 🎨
    title: Modern web UI
    details: Command palette (⌘K), dark mode, responsive on phone, tablet and desktop.
  - icon: 💾
    title: Object storage ready
    details: Stores podcast media in any S3-compatible object storage (MinIO bundled, swap for anything that speaks S3).
  - icon: 🔁
    title: Auto refresh & retention
    details: Configurable concurrent downloads, refresh intervals and retention policies for items and covers.
  - icon: 🔌
    title: RSS out for any app
    details: Every podcast is republished as a clean RSS feed, ready for Pocket Casts, AntennaPod, Overcast…
---

## See it in action

### Spotlight

The home you land on — a curated Spotlight of fresh episodes (day / week / month) and a personalised "New for you" shelf.

![Spotlight landing page with a featured carousel and a "New for you" grid](/screenshots/spotlight.webp)

### Library

Browse every downloaded episode in one place — covers, source, instant filter.

![Library view showing the full episode grid with covers](/screenshots/library.webp)

### Podcasts

All your podcasts side-by-side, regardless of source.

![Podcasts page with all subscribed podcasts](/screenshots/podcasts.webp)

### A podcast, all its episodes

Drill into a podcast to get its full feed, descriptions, and one-click playback or download.

![Detail page of a podcast showing every episode](/screenshots/podcast-list.webp)

### A single episode

Cover, description, metadata, and the actions you want — play, add to a playlist, share.

![Detail page of a single episode](/screenshots/item.webp)

### Playlists

Group episodes across podcasts into custom listens.

![Playlists page](/screenshots/playlist.webp)

### Command palette

Press <kbd>⌘K</kbd> (or <kbd>Ctrl</kbd>+<kbd>K</kbd>) anywhere to jump to a page, open a podcast, trigger an update.

![Command palette open over an episode view](/screenshots/palette.webp)

## Supported by

Podcast-Server is built with open-source licenses generously provided by:

<div class="sponsors">
  <a href="https://www.jetbrains.com/" target="_blank" rel="noopener" aria-label="JetBrains">
    <img src="/sponsors/jetbrains.svg" alt="JetBrains">
  </a>
  <a href="https://www.yourkit.com/" target="_blank" rel="noopener" aria-label="YourKit">
    <img class="sponsor-yourkit" src="/sponsors/yourkit.png" alt="YourKit">
  </a>
  <a href="https://gradle.com/develocity/" target="_blank" rel="noopener" aria-label="Develocity">
    <img src="/sponsors/develocity.svg" alt="Develocity">
  </a>
  <a href="https://about.gitlab.com/" target="_blank" rel="noopener" aria-label="GitLab">
    <img src="/sponsors/gitlab.svg" alt="GitLab">
  </a>
</div>
