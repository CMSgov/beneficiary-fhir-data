#!/usr/bin/env bash
set -x
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdPipelineAppJar="$(echo ${scriptDirectory}/bfd-pipeline-app-*.jar)"
classpath="${bfdPipelineAppJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.pipeline.app.PipelineApplication"
env
if [[ -z "${JAVA_HOME}" ]]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
    javaExecutable=${JAVA_HOME}/bin/java
else
    javaExecutable=java
fi
echo "JAVA HOME: "

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
# [ -n "${JAVA_HOME}" ] && javaExecutable=${JAVA_HOME}/bin/java || javaExecutable=java

# exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
