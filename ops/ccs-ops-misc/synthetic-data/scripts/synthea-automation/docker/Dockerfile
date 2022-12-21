FROM python:3-slim-bullseye as base
RUN apt-get update && apt-get install -y \
    libpq-dev openjdk-11-jdk bc

ARG SYNTHEA_PROPS_DIR
ARG SYNTHEA_JAR_DIR
ARG SYNTHEA_MAPPINGS_DIR
ARG SYNTHEA_SCRIPTS_DIR
ARG RUN_SCRIPT_PATH
FROM base as dist
WORKDIR /usr/local/synthea
COPY $SYNTHEA_PROPS_DIR/synthea.properties ./src/main/resources/
COPY $SYNTHEA_JAR_DIR/* .
COPY $SYNTHEA_MAPPINGS_DIR/* ./src/main/resources/export/
COPY $SYNTHEA_SCRIPTS_DIR/* .
COPY $RUN_SCRIPT_PATH .
COPY ./entrypoint.sh .
COPY ./run_synthea .
RUN chmod +x ./entrypoint.sh
RUN chmod +x ./run_synthea
ENTRYPOINT [ "./entrypoint.sh" ]
CMD ["-h"]