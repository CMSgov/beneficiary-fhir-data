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
+if [[ -z "${JAVA_HOME}" ]]; then
+    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
+    javaExecutable=${JAVA_HOME}/bin/java
+else
+    javaExecutable=java
+fi
echo "JAVA HOME: "
echo $JAVA_HOME

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
