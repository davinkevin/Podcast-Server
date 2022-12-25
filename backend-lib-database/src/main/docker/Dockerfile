FROM flyway/flyway:9.8.3 AS flyway-base

RUN find drivers -iname *.jar | grep -v postgresql | xargs rm && \
    rm -rf lib/enterprise lib/pro licenses/ README.txt flyway* \
    sql/put-your-sql-migrations-here.txt drivers/put-your-jdbc-drivers-here.txt

COPY backend-lib-database/src/main/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java17-debian11:latest as base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/community/*:lib/aad/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
