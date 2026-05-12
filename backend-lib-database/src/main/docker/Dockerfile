# syntax=docker/dockerfile:1.23.0

FROM flyway/flyway:12.6.1 AS flyway-base

RUN echo "Remove useless drivers and plugins, keep only postgresql and jackson" && \
    find /flyway/drivers -iname "*.jar" \
      -not -iname "*postgresql*" \
      -not -iname "*jackson*" \
      -delete && \
    find /flyway/lib/flyway -iname "*.jar" \( \
        -iname "flyway-database-*" \
        -o -iname "flyway-firebird-*" \
        -o -iname "flyway-gcp-*" \
        -o -iname "flyway-mysql-*" \
        -o -iname "flyway-singlestore-*" \
        -o -iname "flyway-sqlserver-*" \
        -o -iname "flyway-nc-*" \
        -o -iname "flyway-locations-s3-*" \
      \) -not -iname "*postgresql*" -delete && \
    rm -rf /flyway/licenses/ /flyway/README.txt /flyway/flyway* /flyway/drivers/*.txt /flyway/conf/ /flyway/assets

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java25-debian13:latest@sha256:3e0a1496b365a18d2c01ccfe27c8bc93b1a6b8ca7460c02b8badb791bf296fce AS base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
