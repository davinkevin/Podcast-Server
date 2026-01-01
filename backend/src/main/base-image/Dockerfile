# syntax=docker/dockerfile:1.20.0
FROM denoland/deno:bin-2.6.3@sha256:9ac6b95faa289ad20ba55e51c9a09fcd96fa8b65365f460ec2880b6c9e2a4e86 AS deno
FROM debian:13 AS downloader

WORKDIR /opt

RUN echo "install dependencies" && \
  apt-get update && apt-get install tree curl xz-utils -y && \
  rm -rf /var/lib/apt/lists/* && \
  echo "install ffmpeg" && \
  mkdir /opt/ffmpeg && \
  curl -qsSL https://www.johnvansickle.com/ffmpeg/old-releases/ffmpeg-5.1.1-amd64-static.tar.xz | tar xJv -C /opt/ffmpeg --strip-components 1 && \
  echo "Print content of downloader folder" && \
  tree /opt/

FROM bellsoft/liberica-openjdk-debian:25@sha256:89d1251423e0f33452bd88239b74d23e869d19535281862d6e949b00e3d526f8 AS base-image

COPY --from=downloader /opt/ /opt/
COPY --from=deno /deno /opt/deno/deno

ENV PATH="/opt/deno:/opt/ffmpeg:/opt/youtube-dl/:/opt/yt-dl/:${PATH}"
ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG="/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE="/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_RTMPDUMP="/usr/bin/rtmpdump"
ENV PODCASTSERVER_EXTERNALTOOLS_YTDLP_PATH="/usr/local/bin/yt-dlp"

RUN echo "install runtime dependencies" && \
  apt-get update && apt-get install python3 python3-pip rtmpdump procps -y --no-install-recommends && \
  python3 -m pip install --break-system-packages -U yt-dlp[default] && \
  rm -rf /var/lib/apt/lists/* && apt-get autoremove && \
  echo "print version of built in tools" && \
  ffmpeg -version | head -n 1 && \
  yt-dlp --version && \
  rtmpdump --help 2>&1 | head -n 1 && \
  python3 -c "import yt_dlp_ejs; print('Plugin EJS présent')" && \
  deno --version