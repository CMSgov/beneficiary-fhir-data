#!/usr/bin/env bash

scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdServerLauncherJar="$(echo ${scriptDirectory}/bfd-server-launcher-*.jar)"
classpath="${bfdServerLauncherJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.server.launcher.DataServerLauncherApp"

if [[ ! -z "${JENKINS_HOME}" ]] && [[ ! -z "${JENKINS_URL}" ]]; then
	export JAVA_HOME=/usr/local/openjdk-11/
    export PATH="${JAVA_HOME}/bin:$PATH"
fi

[ -n "${JAVA_HOME}" ] && javaExecutable=${JAVA_HOME}/bin/java || javaExecutable=java

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
