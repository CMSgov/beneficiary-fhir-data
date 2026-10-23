FROM public.ecr.aws/amazoncorretto/amazoncorretto:25-al2023-headless-arm64@sha256:ba6700b84ce706503e1025c78e19c4fbd459e77d7d6b9e22e0cf055200b8409b AS base

RUN yum -y --security update && \
    yum -y clean all && \
    rm -rf /var/cache

FROM base AS java

ARG javaagent_dir=/opt/jmx_exporter
ARG javaagent_version=1.7.0
ARG javaagent_checksum=0ad843261c567d1924a017d542e14c40f2afff523d940c25ee000742fde4b3a9

RUN mkdir -p ${javaagent_dir} \
    && curl -fsSL -o ${javaagent_dir}/jmx_prometheus_javaagent.jar "https://github.com/prometheus/jmx_exporter/releases/download/${javaagent_version}/jmx_prometheus_javaagent-${javaagent_version}.jar" \
    && echo "${javaagent_checksum} ${javaagent_dir}/jmx_prometheus_javaagent.jar" | sha256sum --check --status

ADD opt/jmx_exporter/config.yaml ${javaagent_dir}/config.yaml
