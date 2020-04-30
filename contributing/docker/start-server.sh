#! /bin/bash

cd $BFD_MOUNT_POINT/apps

mvn clean install -DskipITs

cd $BFD_MOUNT_POINT/apps/bfd-server

#./bfd-server-war/src/main/config/server-start.sh -j $JAVA_HOME/jre -m -Xmx4g -t /app/apps/bfd-server/bfd-server-war/target -u jdbc:postgresql://bfddb:InsecureLocalDev@bfddb:5432/bfd;

mvn -X -Dits.db.url="jdbc:postgresql://bfddb:5432/bfd?user=bfd&password=InsecureLocalDev" --projects bfd-server-war package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start

tail -f $BFD_MOUNT_POINT/apps/bfd-server/bfd-server-war/target/server-work/server-console.log
