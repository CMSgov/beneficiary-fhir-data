FROM python:3-slim-bookworm AS base
RUN apt-get update && apt-get install -y \
    curl gnupg
RUN curl -s https://apt.corretto.aws/corretto.key |  gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg && \
echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" |  tee /etc/apt/sources.list.d/corretto.list
RUN apt-get update && apt-get install -y \
    java-21-amazon-corretto-jdk bc

FROM base AS dist
WORKDIR /usr/local/synthea
COPY . .

ARG docker_entrypoint_sh
RUN echo "${docker_entrypoint_sh}" > ./docker_entrypoint.sh
RUN chmod +x ./docker_entrypoint.sh ./run_synthea ./national_bfd.sh
RUN mkdir out output logs
ENTRYPOINT [ "./docker_entrypoint.sh" ]
CMD ["-h"]
 