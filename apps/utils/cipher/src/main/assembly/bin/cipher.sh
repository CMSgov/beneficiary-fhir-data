#!/bin/bash

bindir=`dirname $0`
if [[ ! -d $bindir/lib ]] ; then
  echo "No lib directory found in $bindir" 2>&1
  exit 1
fi

classpath=`echo $bindir/*.jar $bindir/lib/*.jar|tr ' ' :`
exec kotlin -cp $classpath gov.cms.cipher.MainKt "$@"
