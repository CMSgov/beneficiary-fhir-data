#!/usr/bin/env bash

scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bfdDBMigratorAppJar="$(echo ${scriptDirectory}/bfd-db-migrator*.jar)"
classpath="${bfdDBMigratorAppJar}:${scriptDirectory}/lib/*"
mainClass="gov.cms.bfd.migrator.app.MigratorApp"
[ -n "$JAVA_HOME" ] && "javaExecutable=${JAVA_HOME}/bin/java" || javaExecutable=java

exec "$javaExecutable" -cp "$classpath" "$@" "$mainClass"
