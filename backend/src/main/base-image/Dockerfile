# syntax=docker/dockerfile:1.25.0

# renovate: datasource=github-releases depName=yt-dlp/yt-dlp versioning=regex:^(?<major>\d{4})\.(?<minor>\d{2})\.(?<patch>\d{2})$
ARG YT_DLP_VERSION=2026.07.04

# renovate: datasource=custom.btbn-ffmpeg depName=BtbN/FFmpeg-Builds versioning=loose
ARG FFMPEG_VERSION=8.1

FROM denoland/deno:bin-2.6.3@sha256:9ac6b95faa289ad20ba55e51c9a09fcd96fa8b65365f460ec2880b6c9e2a4e86 AS deno
FROM debian:trixie-20260623 AS downloader

ARG YT_DLP_VERSION
ARG FFMPEG_VERSION

WORKDIR /opt

RUN echo "install build dependencies" && \
  apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl xz-utils tree && \
  rm -rf /var/lib/apt/lists/* && \
  ARCH="$(dpkg --print-architecture)" && \
  echo "resolve arch-specific asset names (dpkg arch=${ARCH})" && \
  case "${ARCH}" in \
    amd64) FFMPEG_ARCH=linux64; YT_DLP_ASSET=yt-dlp_linux ;; \
    arm64) FFMPEG_ARCH=linuxarm64; YT_DLP_ASSET=yt-dlp_linux_aarch64 ;; \
    *) echo "Unsupported architecture: '${ARCH}'" >&2; exit 1 ;; \
  esac && \
  echo "install ffmpeg ${FFMPEG_VERSION} static from BtbN (${FFMPEG_ARCH})" && \
  mkdir /opt/ffmpeg && \
  curl -qsSL "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-n${FFMPEG_VERSION}-latest-${FFMPEG_ARCH}-gpl-${FFMPEG_VERSION}.tar.xz" \
    | tar xJv -C /opt/ffmpeg --wildcards --strip-components 2 '*/bin/ffmpeg' '*/bin/ffprobe' && \
  echo "install yt-dlp standalone (${YT_DLP_ASSET}; PyInstaller, embeds Python and EJS scripts)" && \
  mkdir /opt/yt-dlp && \
  curl -qsSL -o /opt/yt-dlp/yt-dlp \
    "https://github.com/yt-dlp/yt-dlp/releases/download/${YT_DLP_VERSION}/${YT_DLP_ASSET}" && \
  chmod +x /opt/yt-dlp/yt-dlp && \
  echo "Print content of downloader folder" && \
  tree -L 2 /opt/

COPY --from=deno /deno /opt/deno/deno

RUN echo "Print versions of bundled tools" && \
  /opt/ffmpeg/ffmpeg -version | head -n 1 && \
  /opt/ffmpeg/ffprobe -version | head -n 1 && \
  /opt/yt-dlp/yt-dlp --version && \
  /opt/deno/deno --version | head -n 1

FROM gcr.io/distroless/java25-debian13:latest@sha256:0aa10bfac55df3fed8ce238f4d35c5f14e9b705be763943e80b92d815e703201 AS base-image

COPY --from=downloader /opt/ffmpeg/ /opt/ffmpeg/
COPY --from=downloader /opt/yt-dlp/ /opt/yt-dlp/
COPY --from=deno /deno /opt/deno/deno

ENV PATH="/opt/deno:/opt/ffmpeg:/opt/yt-dlp:${PATH}"
ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG="/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE="/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_YTDLP_PATH="/opt/yt-dlp/yt-dlp"
