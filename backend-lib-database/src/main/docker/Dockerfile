# syntax=docker/dockerfile:1.21.0

FROM flyway/flyway:12.0.3 AS flyway-base

RUN echo "Remove useless drivers for additional databases" && \
    find /flyway/drivers -iname *.jar | grep -v postgresql | grep -v jackson | xargs rm && \
    rm -rf /flyway/licenses/ /flyway/README.txt /flyway/flyway* /flyway/drivers/*.txt /flyway/conf/ /flyway/assets

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java25-debian13:latest@sha256:202ae7b10d0929fec7590927e5d14fad9b9a9b886ead6df4c555b8ae9ebeec17 AS base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
