FROM python:3-slim-bullseye as base
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk bc

FROM base as app
WORKDIR /usr/local/synthea
COPY . .
RUN chmod +x ./docker_entrypoint.sh ./run_synthea ./national_bfd.sh ./national_bfd_v2.sh
RUN mkdir output logs

FROM app as local
ENTRYPOINT [ "./docker_entrypoint.sh" ]
CMD ["-h"]

# Only install awscli for jenkins variant of image as it is unnecessary for local variant
# Additionally, jenkins uses the image as an agent, so no ENTRYPOINT or CMD definitions are needed
FROM app as jenkins
RUN apt-get update && apt-get install -y \
    awscli 
