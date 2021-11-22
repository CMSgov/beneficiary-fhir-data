#!/usr/bin/env bash

scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdServerLauncherJar="$(echo ${scriptDirectory}/bfd-server-launcher-*.jar)"
classpath="${bfdServerLauncherJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.server.launcher.DataServerLauncherApp"
export BFD_JAVA_HOME=/usr/local/openjdk-11/
export PATH="${BFD_JAVA_HOME}/bin:$PATH"
[ -n "${BFD_JAVA_HOME}" ] && javaExecutable=${BFD_JAVA_HOME}/bin/java || javaExecutable=java

exec "${javaExecutable}" -cp "${classpath}" "$@" "${mainClass}"
