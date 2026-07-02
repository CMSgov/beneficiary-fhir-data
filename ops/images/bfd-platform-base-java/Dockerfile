FROM public.ecr.aws/amazoncorretto/amazoncorretto:25-al2023-headless-arm64 AS base

RUN yum -y --security update && \
    yum -y clean all && \
    rm -rf /var/cache

FROM base AS java

ARG javaagent_dir=/opt/jmx_exporter
ARG javaagent_version=1.5.0
ARG javaagent_checksum=0315f3f657876302c6205a98d4036ec775dca529c5d0419ca60ee669c688239f

RUN mkdir -p ${javaagent_dir} \
    && curl -fsSL -o ${javaagent_dir}/jmx_prometheus_javaagent.jar "https://github.com/prometheus/jmx_exporter/releases/download/${javaagent_version}/jmx_prometheus_javaagent-${javaagent_version}.jar" \
    && echo "${javaagent_checksum} ${javaagent_dir}/jmx_prometheus_javaagent.jar" | sha256sum --check --status

ADD opt/jmx_exporter/config.yaml ${javaagent_dir}/config.yaml

ARG datadogagent_dir=/opt/datadog
ARG datadogagent_version=1.63.0
ARG datadogagent_checksum=0019dfc4b32d63c1392aa264aed2253c1e0c2fb09216f8e2cc269bbfb8bb49b5

RUN mkdir -p ${datadogagent_dir} \
    && curl -fsSL -o ${datadogagent_dir}/dd-java-agent.jar "https://github.com/DataDog/dd-trace-java/releases/download/${datadogagent_version}/dd-java-agent-${datadogagent_version}.jar" \
    && echo "${datadogagent_checksum} ${datadogagent_dir}/dd-java-agent.jar" | sha256sum --check --status
