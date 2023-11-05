# syntax=docker/dockerfile:1.5.2

FROM flyway/flyway:9.22.3 AS flyway-base

RUN echo "Remove useless drivers for additional databases" && \
  find drivers -iname *.jar | grep -v postgresql | grep -v jackson | xargs rm  && \
  rm -rf lib/enterprise lib/pro licenses/ README.txt flyway* sql/put-your-sql-migrations-here.txt drivers/put-your-jdbc-drivers-here.txt

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java17-debian12:latest as base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/community/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
