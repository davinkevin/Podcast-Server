# syntax=docker/dockerfile:1.12.1

FROM debian:12 AS downloader

WORKDIR /opt

RUN echo "install dependencies" && \
  apt-get update && apt-get install tree curl xz-utils -y && \
  rm -rf /var/lib/apt/lists/* && \
  echo "install ffmpeg" && \
  mkdir /opt/ffmpeg && \
  curl -qsSL https://www.johnvansickle.com/ffmpeg/old-releases/ffmpeg-5.1.1-amd64-static.tar.xz | tar xJv -C /opt/ffmpeg --strip-components 1 && \
  echo "Print content of downloader folder" && \
  tree /opt/

FROM bellsoft/liberica-openjdk-debian:23.0.1-13 as base-image

COPY --from=downloader /opt/ /opt/

ENV PATH="/opt/ffmpeg:/opt/youtube-dl/:/opt/yt-dl/:${PATH}"
ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG    "/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE   "/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_RTMPDUMP  "/usr/bin/rtmpdump"
ENV PODCASTSERVER_EXTERNALTOOLS_YOUTUBEDL "/usr/local/bin/yt-dlp"

RUN echo "install runtime dependencies" && \
  apt-get update && apt-get install python3 python3-pip rtmpdump procps -y --no-install-recommends && \
  python3 -m pip install --break-system-packages -U yt-dlp[default] && \
  rm -rf /var/lib/apt/lists/* && apt-get autoremove && \
  echo "print version of built in tools" && \
  ffmpeg -version | head -n 1 && \
  yt-dlp --version && \
  rtmpdump --help 2>&1 | head -n 1