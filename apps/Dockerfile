# BASE_ANSIBLE tag, a known tag or latest available from https://github.com/CMSgov/beneficiary-fhir-data/pkgs/container/bfd-ansible
ARG BASE_ANSIBLE_TAG=master
FROM ghcr.io/cmsgov/bfd-ansible:$BASE_ANSIBLE_TAG
LABEL org.opencontainers.image.source=https://github.com/CMSgov/beneficiary-fhir-data

# bfd-server assets
COPY bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT.zip /.m2/repository/gov/cms/bfd/bfd-server-launcher/1.0.0-SNAPSHOT/bfd-server-launcher-1.0.0-SNAPSHOT.zip
COPY bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war /.m2/repository/gov/cms/bfd/bfd-server-war/1.0.0-SNAPSHOT/bfd-server-war-1.0.0-SNAPSHOT.war

# bfd-pipeline assets
COPY bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT.zip /.m2/repository/gov/cms/bfd/bfd-pipeline-app/1.0.0-SNAPSHOT/bfd-pipeline-app-1.0.0-SNAPSHOT.zip

# bfd-db-migrator assets
COPY bfd-db-migrator/target/bfd-db-migrator-1.0.0-SNAPSHOT.zip /.m2/repository/gov/cms/bfd/bfd-db-migrator/1.0.0-SNAPSHOT/bfd-db-migrator-1.0.0-SNAPSHOT.zip
