#!/usr/bin/env bash
set -x
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdServerLauncherJar="$(echo ${scriptDirectory}/bfd-server-launcher-*.jar)"
classpath="${bfdServerLauncherJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.server.launcher.DataServerLauncherApp"

env

if [[ -z "${JAVA_HOME}" ]]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
    javaExecutable=${JAVA_HOME}/bin/java
else
    javaExecutable=java
fi

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
