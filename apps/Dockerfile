FROM openjdk:8-slim AS development

WORKDIR /app

ARG mvn_args

# installing maven on Debian requires a man directory
RUN mkdir -p /usr/share/man/man1
RUN set -eux; \
  apt-get update; \
  apt-get install -y --no-install-recommends \
          maven \
          curl

#COPY .git .git
COPY containers/toolchains.xml /root/.m2/toolchains.xml
COPY bfd-server-test-functions bfd-server-test-functions
COPY bfd-server-test-perf bfd-server-test-perf
COPY bfd-shared-utils bfd-shared-utils
COPY bfd-model bfd-model
COPY bfd-pipeline bfd-pipeline
COPY bfd-server bfd-server
COPY pom.xml pom.xml

RUN mvn install $mvn_args

ENV BFD_PORT=6500 \
    BFD_V2_ENABLED=true \
    BFD_KEYSTORE=/app/bfd-server/dev/ssl-stores/server-keystore.jks \
    BFD_TRUSTSTORE=/app/bfd-server/dev/ssl-stores/server-truststore.jks \
    BFD_WAR=/app/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war \
    LOGS_DIR=/app \
    DB_URL=jdbc:postgresql://bfd-db:5432/bfd \
    DB_USERNAME=bfd \
    DB_PASSWORD=InsecureLocalDev \
    DB_CONNECTIONS_MAX=40 \
    SCHEMA_APPLY=true \
    TMP_DIR=/tmp

CMD java \
  "-Xmx4g" \
  "-Dbfd-server-9876" \
  "-DbfdServer.logs.dir=$LOGS_DIR" \
  "-DbfdServer.db.url=$DB_URL" \
  "-DbfdServer.db.username=$DB_USERNAME" \
  "-DbfdServer.db.password=$DB_PASSWORD" \
  "-DbfdServer.db.connections.max=$DB_CONNECTIONS_MAX" \
  "-DbfdServer.v2.enabled=$BFD_V2_ENABLED" \
  "-DbfdServer.db.schema.apply=true" \
  "-Djava.io.tmpdir=$TMP_DIR" \
  -jar "bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT-capsule-fat.jar" 

FROM openjdk:8-slim AS production

WORKDIR /app

RUN apt-get update && apt-get install -y \
      curl \
      unzip
RUN mkdir -p newrelic/extensions
RUN cd /tmp \
  && curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
  && unzip newrelic-java.zip \
  && mv /tmp/newrelic/newrelic.jar /app/newrelic/newrelic.jar

COPY containers/newrelic.yml newrelic/newrelic.yml
COPY containers/newrelic_tracing.xml newrelic/extensions/newrelic_tracing.xml

COPY --from=development /app/bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT-capsule-fat.jar bfd-server-launcher.jar
COPY --from=development /app/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war bfd-server-war.war

#Required ENV vars for production
#-------------------------------
#BFD_PORT
#BFD_V2_ENABLED
#BFD_KEYSTORE
#BFD_TRUSTSTORE
#BFD_WAR
#LOGS_DIR
#DB_URL
#DB_USERNAME
#DB_PASSWORD
#DB_CONNECTIONS_MAX
#TMP_DIR
#NEW_RELIC_HOST
#NEW_RELIC_LICENSE_KEY
#NEW_RELIC_LOG_FILE_PATH
#NEW_RELIC_HIGH_SECURITY
#NEW_RELIC_EXTENSIONS_DIR
#NEW_RELIC_PROXY_HOST
#NEW_RELIC_PROXY_PORT
#NEW_RELIC_APP_NAME
#NEW_RELIC_ENVIRONMENT
#NEW_RELIC_METRIC_KEY
#NEW_RELIC_METRIC_HOST
#NEW_RELIC_METRIC_PATH
#NEW_RELIC_METRIC_PERIOD

CMD java \
  -javaagent:/app/newrelic/newrelic.jar \
  "-Xmx4g" \
  "-DbfdServer.logs.dir=$LOGS_DIR" \
  "-DbfdServer.db.url=$DB_URL" \
  "-DbfdServer.db.username=$DB_USERNAME" \
  "-DbfdServer.db.password=$DB_PASSWORD" \
  "-DbfdServer.db.connections.max=$DB_CONNECTIONS_MAX" \
  "-DbfdServer.v2.enabled=$BFD_V2_ENABLED" \
  "-Djava.io.tmpdir=$TMP_DIR \
  -jar "bfd-server-launcher.jar" \
  >>"/app/bluebutton-server-app-log.json" 2>&1

