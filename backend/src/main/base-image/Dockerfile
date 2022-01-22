FROM debian:11 AS downloader

WORKDIR /opt

RUN apt-get update && \
    apt-get install tree curl xz-utils -y && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir /opt/ffmpeg && \
    curl -qsSL https://www.johnvansickle.com/ffmpeg/old-releases/ffmpeg-4.4-amd64-static.tar.xz | tar xJv -C /opt/ffmpeg --strip-components 1

RUN mkdir /opt/youtube-dl && \
    ## Youtube DL Installation
    curl -qsSL https://yt-dl.org/downloads/latest/youtube-dl -o /opt/youtube-dl/youtube-dl && chmod +x /opt/youtube-dl/youtube-dl

RUN mkdir /opt/yt-dl && \
    curl -qsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /opt/yt-dl/yt-dlp && \
    chmod a+rx /opt/yt-dl/yt-dlp && \
    ## Final check
    tree /opt/

FROM openjdk:17-slim as base-image
RUN apt-get update && apt-get install python3 rtmpdump procps -y --no-install-recommends && \
      rm -rf /var/lib/apt/lists/* && \
      apt-get autoremove && \
      ln -s /usr/bin/python3 /usr/bin/python

ENV PATH="/opt/ffmpeg:/opt/youtube-dl/:/opt/yt-dl/:${PATH}"
COPY --from=downloader /opt/ /opt/

RUN ffmpeg -version | head -n 1 && \
    youtube-dl --version && \
    yt-dlp --version && \
    rtmpdump --help 2>&1 | head -n 1

ENV PODCASTSERVER_EXTERNALTOOLS_FFMPEG    "/opt/ffmpeg/ffmpeg"
ENV PODCASTSERVER_EXTERNALTOOLS_FFPROBE   "/opt/ffmpeg/ffprobe"
ENV PODCASTSERVER_EXTERNALTOOLS_RTMPDUMP  "/usr/bin/rtmpdump"
ENV PODCASTSERVER_EXTERNALTOOLS_YOUTUBEDL "/opt/yt-dl/yt-dlp"
