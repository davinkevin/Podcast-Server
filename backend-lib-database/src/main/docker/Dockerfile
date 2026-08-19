# syntax=docker/dockerfile:1.26.0

FROM flyway/flyway:13.3.0 AS flyway-base

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

FROM gcr.io/distroless/java25-debian13:latest@sha256:34f47c4e018bf205c771ad12a1c8c06d0801a69c0a5566677f0e31f166c89793 AS base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate", "-locations=filesystem:/database/sql"]
