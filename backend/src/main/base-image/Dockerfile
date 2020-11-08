FROM debian:10 AS downloader

WORKDIR /opt

RUN apt-get update && \
    apt-get install tree curl xz-utils wget unzip -y && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir /opt/ffmpeg && \
    curl -qsSL https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz | tar xJv -C /opt/ffmpeg --strip-components 1 && \
    ## Youtube DL Installation
    mkdir /opt/youtube-dl && \
    curl -qsSL https://yt-dl.org/downloads/latest/youtube-dl -o /opt/youtube-dl/youtube-dl && chmod +x /opt/youtube-dl/youtube-dl && \
    ## Yourkit Installation
    wget -q https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2020.9-docker.zip -P /tmp/ && \
    unzip /tmp/YourKit-JavaProfiler-2020.9-docker.zip -d /opt/ && \
    rm /tmp/YourKit-JavaProfiler-2020.9-docker.zip && \
    mkdir /opt/yourkit && \
    cd /opt/YourKit-*/ && \
    cp -r . /opt/yourkit && \
    rm -rf /opt/YourKit-*/ && \
    ## Final check
    tree /opt/

FROM openjdk:11-slim as base-image
RUN apt-get update && apt-get install python3 rtmpdump procps -y --no-install-recommends && \
      rm -rf /var/lib/apt/lists/* && \
      apt-get autoremove && \
      ln -s /usr/bin/python3 /usr/bin/python

ENV PATH="/opt/ffmpeg:/opt/youtube-dl/:${PATH}"
COPY --from=downloader /opt/ /opt/

RUN ffmpeg -version | head -n 1 && \
    youtube-dl --version && \
    rtmpdump --help 2>&1 | head -n 1

ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG    "/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE   "/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_RTMPDUMP  "/usr/bin/rtmpdump"
ENV PODCASTSERVER_EXTERNALTOOLS_YOUTUBEDL "/opt/youtube-dl/youtube-dl"
