#!/usr/bin/env bash
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdServerLauncherJar="$(echo ${scriptDirectory}/bfd-server-launcher-*.jar)"
classpath="${bfdServerLauncherJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.server.launcher.DataServerLauncherApp"
[ -n "${JAVA_HOME}" ] && javaExecutable=${JAVA_HOME}/bin/java || javaExecutable=java

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
