# syntax=docker/dockerfile:1.20.0

FROM flyway/flyway:11.20.0 AS flyway-base

RUN echo "Remove useless drivers for additional databases" && \
    find /flyway/drivers -iname *.jar | grep -v postgresql | grep -v jackson | xargs rm && \
    rm -rf /flyway/licenses/ /flyway/README.txt /flyway/flyway* /flyway/drivers/*.txt /flyway/conf/ /flyway/assets

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java25-debian13:latest@sha256:7b62d2eafa7e484add5b8be46c0b437b1540a3ce6e0f7ac91b3ffa64ba210246 AS base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
