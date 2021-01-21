#! /bin/sh
set -e

cd $BFD_MOUNT_POINT/apps;

mvn clean install -DskipITs

cd $BFD_MOUNT_POINT/apps/bfd-pipeline/bfd-pipeline-rif-load;

mvn -Dits.db.url="jdbc:postgresql://local_bfddb:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadLocalSyntheticData clean verify
