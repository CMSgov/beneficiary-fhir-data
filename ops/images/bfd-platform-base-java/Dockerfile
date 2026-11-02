FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261026.1-minimal@sha256:c25add18b6c35620a6e096ef03ecbafbcaed993281ad227792f67c324980011b AS base

RUN dnf -y install java-25-amazon-corretto-headless && \
    dnf -y upgrade && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS java

ARG javaagent_dir=/opt/jmx_exporter
ARG javaagent_version=1.7.0
ARG javaagent_checksum=0ad843261c567d1924a017d542e14c40f2afff523d940c25ee000742fde4b3a9

RUN mkdir -p ${javaagent_dir} \
    && curl -fsSL -o ${javaagent_dir}/jmx_prometheus_javaagent.jar "https://github.com/prometheus/jmx_exporter/releases/download/${javaagent_version}/jmx_prometheus_javaagent-${javaagent_version}.jar" \
    && echo "${javaagent_checksum} ${javaagent_dir}/jmx_prometheus_javaagent.jar" | sha256sum --check --status

ADD opt/jmx_exporter/config.yaml ${javaagent_dir}/config.yaml
