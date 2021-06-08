#!/bin/bash

# Spin Docker Image with Postgres and test beneficiary Data
docker run -d --name 'local_bfddb' -e 'POSTGRES_DB=bfd' -e 'POSTGRES_USER=bfd' -e 'POSTGRES_PASSWORD=InsecureLocalDev' -p '5432:5432' postgres:12
cd ../apps/bfd-pipeline/bfd-pipeline-ccw-rif && mvn -Dits.db.url="jdbc:postgresql://localhost:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadSampleA clean verify

# Fetch Synthetic Data
cd ../../../contributing && make synthetic-data/*.rif 
cd ../ && git apply contributing/patches/load_local_synthetic_data.patch
cd apps && mvn clean install -DskipITs

# Run Maven test and load synthetic data
cd bfd-pipeline/bfd-pipeline-ccw-rif && mvn -Dits.db.url="jdbc:postgresql://localhost:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadLocalSyntheticData clean verify