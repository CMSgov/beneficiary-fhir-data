FROM openjdk:8-slim AS development

WORKDIR /app

# installing maven on Debian requires a man directory
RUN mkdir -p /usr/share/man/man1
RUN set -eux; \
  apt-get update; \
  apt-get install -y --no-install-recommends \
          maven

COPY .git .git
COPY apps/containers/toolchains.xml /root/.m2/toolchains.xml
COPY apps/bfd-server-test-functions bfd-server-test-functions
COPY apps/bfd-server-test-perf bfd-server-test-perf
COPY apps/bfd-shared-utils bfd-shared-utils
COPY apps/bfd-model bfd-model
COPY apps/bfd-pipeline bfd-pipeline
COPY apps/bfd-server bfd-server
COPY apps/pom.xml pom.xml

RUN mvn install -DskipITs

ENV BFD_PORT=6500 \
    BFD_V2_ENABLED=true \
    BFD_KEYSTORE=bfd-server/dev/ssl-stores/server-keystore.jks \
    BFD_TRUSTSTORE=bfd-server/dev/ssl-stores/server-keystore.jks \
    BFD_WAR=bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war \
    LOGS_DIR=/app \
    DB_URL=jdbc:bfd-test:hsqldb:mem \
    DB_CONNECTIONS_MAX=40 \
    TMP_DIR=/tmp

#CMD ["mvn", "-X", "-DskipTests=true", "--projects", "bfd-server-war", "package", "dependency:copy", "antrun:run", "org.codehaus.mojo:exec-maven-plugin:exec@server-start"]
CMD java \
  "-Xmx4g" \
  "-DbfdServer.logs.dir=$LOGS_DIR" \
  "-DbfdServer.db.url=$DB_URL" \
  "-DbfdServer.db.username=$DB_USERNAME" \
  "-DbfdServer.db.password=$DB_PASSWORD" \
  "-DbfdServer.db.connections.max=$DB_CONNECTIONS_MAX" \
  "-DbfdServer.v2.enabled=$BFD_V2_ENABLED" \
  "-Djava.io.tmpdir=$TMP_DIR" \
  -jar "bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT-capsule-fat.jar" 
