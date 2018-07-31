FROM davinkevin/podcast-server:dev
MAINTAINER Kevin DAVIN  <https://github.com/davinkevin/Podcast-Server/issues>

# Exposed port for 4000 for the APM part
EXPOSE 4000

WORKDIR /opt
RUN mkdir -p /opt/glowroot && ls -al
COPY glowroot /opt/glowroot

WORKDIR /opt/podcast-server
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-javaagent:/opt/glowroot/glowroot.jar","-jar","/opt/podcast-server/app.jar"]

# RUN with docker like that :
# $ docker run -it -p 8080:8080 davinkevin/podcast-server:v1.0.0

# To configure it with external application.(properties|yaml), you can mount the file like this :
#
# $ docker run -it -v /path/to/application.yml:/opt/podcast-server/application.yml -p 8080:8080 davinkevin/podcast-server:dev-with-apm
#
# /!\ You can't override values defined as ENV in this Dockerfile through the application.yml. If you still want to, you have
# to use the `-e` flag of docker : -e podcastserver.backup.location='/opt/podcast-server/backup/'