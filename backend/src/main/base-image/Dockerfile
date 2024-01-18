# syntax=docker/dockerfile:1.6.0

FROM debian:12 AS downloader

WORKDIR /opt

RUN echo "install dependencies" && \
  apt-get update && apt-get install tree curl xz-utils -y && \
  rm -rf /var/lib/apt/lists/* && \
  echo "install ffmpeg" && \
  mkdir /opt/ffmpeg && \
  curl -qsSL https://www.johnvansickle.com/ffmpeg/old-releases/ffmpeg-5.1.1-amd64-static.tar.xz | tar xJv -C /opt/ffmpeg --strip-components 1 && \
  echo "install yt-dlp" && \
  mkdir /opt/yt-dl && \
  curl -qsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /opt/yt-dl/yt-dlp && \
  chmod a+rx /opt/yt-dl/yt-dlp && \
  echo "Print content of downloader folder" && \
  tree /opt/

FROM bellsoft/liberica-openjdk-debian:21.0.2 as base-image

COPY --from=downloader /opt/ /opt/

ENV PATH="/opt/ffmpeg:/opt/youtube-dl/:/opt/yt-dl/:${PATH}"
ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG    "/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE   "/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_RTMPDUMP  "/usr/bin/rtmpdump"
ENV PODCASTSERVER_EXTERNALTOOLS_YOUTUBEDL "/opt/yt-dl/yt-dlp"

RUN echo "install runtime dependencies" && \
  apt-get update && apt-get install python3 rtmpdump procps -y --no-install-recommends && \
  rm -rf /var/lib/apt/lists/* && apt-get autoremove && \
  echo "print version of built in tools" && \
  ffmpeg -version | head -n 1 && \
  yt-dlp --version && \
  rtmpdump --help 2>&1 | head -n 1
