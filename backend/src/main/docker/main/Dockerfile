FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Kevin DAVIN  <https://github.com/davinkevin/Podcast-Server/issues>

#Installation of native dependencies :
RUN apk add --update ffmpeg
RUN apk add --update rtmpdump

# Configuration specific for docker container
ENV podcastserver.backup.location "/opt/podcast-server/backup/"
ENV podcastserver.externaltools.rtmpdump "/usr/bin/rtmpdump"
ENV podcastserver.externaltools.ffmpeg "/usr/bin/ffmpeg"
ENV podcastserver.rootfolder "/opt/podcast-server/podcasts/"
ENV logging.path "/opt/podcast-server/podcast-server.log"
ENV spring.datasource.url "jdbc:h2:/opt/podcast-server/db;TRACE_LEVEL_FILE=0;MVCC=TRUE"

# Exposed port for Web Part and Crash Shell
EXPOSE 8080

# Shared volume, containing podcast and logs
VOLUME /tmp
VOLUME /opt/podcast-server

WORKDIR /opt/podcast-server
ADD Podcast-Server.jar /opt/podcast-server/app.jar
RUN sh -c 'touch /opt/podcast-server/app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/podcast-server/app.jar"]

# RUN with docker like that :
# $ docker run -it -p 8080:8080 davinkevin/podcast-server:v1.0.0

# To configure it with external application.(properties|yaml), you can mount the file like this :
#
# $ docker run -it -v /path/to/application.yml:/opt/podcast-server/application.yml -p 8080:8080 davinkevin/podcast-server:1.0.0
#
# /!\ You can't override values defined as ENV in this Dockerfile through the application.yml. If you still want to, you have
# to use the `-e` flag of docker : -e podcastserver.backup.location='/opt/podcast-server/backup/'
