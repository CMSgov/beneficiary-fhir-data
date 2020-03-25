#! /bin/bash

cd $BFD_MOUNT_POINT/apps/$1

mvn clean verify ${@:2}
