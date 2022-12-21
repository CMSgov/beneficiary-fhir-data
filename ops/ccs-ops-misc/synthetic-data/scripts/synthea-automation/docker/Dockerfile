FROM python:3-slim-bullseye as base
RUN apt-get update && apt-get install -y \
    libpq-dev openjdk-11-jdk bc

FROM base as dist
WORKDIR /usr/local/synthea
COPY . .
RUN chmod +x ./entrypoint.sh ./run_synthea ./national_bfd.sh ./national_bfd_v2.sh
RUN mkdir output logs
ENTRYPOINT [ "./entrypoint.sh" ]
CMD ["-h"]
