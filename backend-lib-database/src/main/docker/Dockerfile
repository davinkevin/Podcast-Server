# syntax=docker/dockerfile:1.6.0

FROM flyway/flyway:10.1.0 AS flyway-base

RUN echo "Remove useless drivers for additional databases" && \
    find /flyway/drivers -iname *.jar | grep -v postgresql | grep -v jackson | xargs rm && \
    rm -rf /flyway/licenses/ /flyway/README.txt /flyway/flyway* /flyway/drivers/*.txt /flyway/conf/ /flyway/assets

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java21-debian12:latest as base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/flyway/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
