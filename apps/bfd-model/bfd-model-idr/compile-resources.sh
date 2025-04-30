#!/bin/sh
cd sushi && sushi build
cd ../
java -jar validator_cli.jar outputs/Coverage-FFS.json -ig hl7.fhir.us.carin-bb#2.1.0 -ig sushi/fsh-generated/resources/*
