# syntax=docker/dockerfile:1.24.0

# renovate: datasource=github-releases depName=yt-dlp/yt-dlp versioning=regex:^(?<major>\d{4})\.(?<minor>\d{2})\.(?<patch>\d{2})$
ARG YT_DLP_VERSION=2026.03.17

FROM denoland/deno:bin-2.6.3@sha256:9ac6b95faa289ad20ba55e51c9a09fcd96fa8b65365f460ec2880b6c9e2a4e86 AS deno
FROM debian:trixie-20260518 AS downloader

ARG YT_DLP_VERSION

WORKDIR /opt

RUN echo "install build dependencies" && \
  apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl xz-utils tree && \
  rm -rf /var/lib/apt/lists/* && \
  ARCH="$(dpkg --print-architecture)" && \
  echo "resolve arch-specific asset names (dpkg arch=${ARCH})" && \
  case "${ARCH}" in \
    amd64) FFMPEG_ARCH=amd64; YT_DLP_ASSET=yt-dlp_linux ;; \
    arm64) FFMPEG_ARCH=arm64; YT_DLP_ASSET=yt-dlp_linux_aarch64 ;; \
    *) echo "Unsupported architecture: '${ARCH}'" >&2; exit 1 ;; \
  esac && \
  echo "install ffmpeg static (${FFMPEG_ARCH})" && \
  mkdir /opt/ffmpeg && \
  curl -qsSL "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-${FFMPEG_ARCH}-static.tar.xz" \
    | tar xJv -C /opt/ffmpeg --strip-components 1 && \
  rm -rf \
    /opt/ffmpeg/model \
    /opt/ffmpeg/manpages \
    /opt/ffmpeg/qt-faststart \
    /opt/ffmpeg/readme.txt && \
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

FROM gcr.io/distroless/java25-debian13:latest@sha256:583ba2e08558063002bd1b5874a81b33b7204a0ad46727d4b6cbeff5a25935ba AS base-image

COPY --from=downloader /opt/ffmpeg/ /opt/ffmpeg/
COPY --from=downloader /opt/yt-dlp/ /opt/yt-dlp/
COPY --from=deno /deno /opt/deno/deno

ENV PATH="/opt/deno:/opt/ffmpeg:/opt/yt-dlp:${PATH}"
ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG="/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE="/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_YTDLP_PATH="/opt/yt-dlp/yt-dlp"
