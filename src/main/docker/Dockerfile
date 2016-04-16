FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Kevin DAVIN  <https://github.com/davinkevin/Podcast-Server/issues>

RUN apk add --update rtmpdump

WORKDIR /tmp
RUN wget -q http://johnvansickle.com/ffmpeg/releases/ffmpeg-release-64bit-static.tar.xz \
  && tar xJf /tmp/ffmpeg-release-64bit-static.tar.xz -C /tmp \
  && mv /tmp/ffmpeg-3.0-64bit-static/ffmpeg /usr/local/bin/ \
  && rm -rf /tmp/ffmpeg*

WORKDIR /opt/podcast-server

ENV podcastserver.backup.location "/opt/podcast-server/backup/"
ENV podcastserver.rootfolder "/opt/podcast-server/podcasts"
ENV spring.datasource.url "jdbc:h2:/opt/podcast-server/db;TRACE_LEVEL_FILE=0;MVCC=TRUE"


EXPOSE 8080
# Expose Web server port
EXPOSE 2000
# Exposer crash shell port

VOLUME /tmp
VOLUME /opt/podcast-server
ADD Podcast-Server.jar /opt/podcast-server/app.jar
RUN sh -c 'touch /opt/podcast-server/app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/podcast-server/app.jar"]

# RUN with docker like that :
# $ docker run -it -p 8080:8080 -p 2000:2000 -e podcastserver.server-url='http://dockerhost:8080/' davinkevin/podcast-server:v1.0.0