FROM python:3-slim-bullseye as base
ARG docker_entrypoint_sh

RUN apt-get update && apt-get install -y \
    openjdk-11-jdk bc

FROM base as dist
WORKDIR /usr/local/synthea
COPY . .
RUN echo $docker_entrypoint_sh > ./docker_entrypoint.sh
RUN chmod +x ./docker_entrypoint.sh ./run_synthea ./national_bfd.sh
RUN mkdir out output logs
ENTRYPOINT [ "./docker_entrypoint.sh" ]
CMD ["-h"]
