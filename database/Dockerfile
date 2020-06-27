FROM flyway/flyway:6.5.0 AS flyway-base

RUN find drivers -iname *.jar | grep -v postgresql | xargs rm && \
    rm -rf lib/enterprise lib/pro licenses/ README.txt flyway* \
    sql/put-your-sql-migrations-here.txt drivers/put-your-jdbc-drivers-here.txt

COPY database/migrations/*.sql /flyway/sql/

FROM gcr.io/distroless/java-debian10:11 as base-image

COPY --from=flyway-base /flyway/ /database/
WORKDIR /database/

ENTRYPOINT [ "java", "-cp", "lib/*:lib/community/*:drivers/*", "org.flywaydb.commandline.Main", "migrate"]
