#!/bin/bash

scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdServerLauncherJar="${scriptDirectory}/$(ls ${scriptDirectory} | grep '^bfd-server-launcher-.*jar')"
classpath="${bfdServerLauncherJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.server.launcher.DataServerLauncherApp"
[ -n "${BFD_JAVA_HOME}" ] && javaExecutable=${BFD_JAVA_HOME}/bin/java || javaExecutable=java

exec ${javaExecutable} -cp ${classpath} $* ${mainClass}
