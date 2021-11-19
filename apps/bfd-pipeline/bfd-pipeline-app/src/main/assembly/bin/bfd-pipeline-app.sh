#!/usr/bin/env bash
set -x
echo "Starting bfd-pipeline"
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdPipelineAppJar="$(echo ${scriptDirectory}/bfd-pipeline-app-*.jar)"
classpath="${bfdPipelineAppJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.pipeline.app.PipelineApplication"
export JAVA_HOME=/usr/local/openjdk-11/
export PATH="${JAVA_HOME}/bin:$PATH"

echo "JAVA HOMEEEE: "
echo "${JAVA_HOME}"
echo $JAVA_HOME
[ -n "${JAVA_HOME}" ] && javaExecutable=${JAVA_HOME}/bin/java || javaExecutable=java

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
