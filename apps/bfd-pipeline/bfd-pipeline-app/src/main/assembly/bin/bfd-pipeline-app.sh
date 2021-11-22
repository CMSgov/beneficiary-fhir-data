#!/usr/bin/env bash
set -x
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdPipelineAppJar="$(echo ${scriptDirectory}/bfd-pipeline-app-*.jar)"
classpath="${bfdPipelineAppJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.pipeline.app.PipelineApplication"
echo "JENKINS HOME: "
echo $JENKINS_HOME
echo "JENKINS URL: "
echo JENKINS_URL
if [[ ! -z "${JENKINS_HOME}" ]] && [[ ! -z "${JENKINS_URL}" ]]; then
	export JAVA_HOME=/usr/local/openjdk-11/
    export PATH="${JAVA_HOME}/bin:$PATH"
fi
echo "JAVA HOME: "
echo $JAVA_HOME
echo dirname $(dirname $(readlink -f $(which javac)))
[ -n "${JAVA_HOME}" ] && javaExecutable=${JAVA_HOME}/bin/java || javaExecutable=java

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
