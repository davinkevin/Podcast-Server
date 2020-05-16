#!/busybox/sh

java $JAVA_OPTS -cp ".:/database/liquibase.jar:/database/lib/*" \
  liquibase.integration.commandline.Main \
  --url="$DATASOURCE_URL" \
  --username="$DATASOURCE_USERNAME" \
  --password="$DATASOURCE_PASSWORD" \
  --changeLogFile=/changelog/changelog.xml \
  --driver=org.postgresql.Driver \
  update
