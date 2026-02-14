# syntax=docker/dockerfile:1.21.0

FROM flyway/flyway:12.0.1 AS flyway-base

RUN echo "Remove useless drivers for additional databases" && \
    find /flyway/drivers -iname *.jar | grep -v postgresql | grep -v jackson | xargs rm && \
    rm -rf /flyway/licenses/ /flyway/README.txt /flyway/flyway* /flyway/drivers/*.txt /flyway/conf/ /flyway/assets

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java25-debian13:latest@sha256:b10a14cd68320b9dd71e72736fb72d83799ab78a0d79f2b2598e5d0afe9f8c8c AS base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
