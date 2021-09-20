#!/bin/bash

if [ -z "$1" ]
  then
    echo "usage: run_bridge <Rif Root Directory>"
else
  mvn clean install exec:java -DfilePath=$1
fi