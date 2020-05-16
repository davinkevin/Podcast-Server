FROM openjdk:11-jdk AS downloader

RUN wget -q https://github.com/liquibase/liquibase/releases/download/v3.8.9/liquibase-3.8.9.tar.gz -O liquibase-3.8.9-bin.tar.gz \
    && mkdir database \
    && tar zxf liquibase-3.8.9-bin.tar.gz --directory /database \
    && wget -q -P /database/lib/ jdbc.postgresql.org/download/postgresql-42.2.12.jar

FROM gcr.io/distroless/java:11-debug as base-image

COPY --from=downloader /database/ /database/
COPY backend/src/main/database/update.sh /database/
COPY backend/src/main/database/changelog/ /changelog/

ENTRYPOINT [ "/busybox/sh", "-c", "/database/update.sh" ]
