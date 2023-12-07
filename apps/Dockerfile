# BASE_ANSIBLE tag, a known tag or latest available from https://github.com/CMSgov/beneficiary-fhir-data/pkgs/container/bfd-ansible
ARG BASE_ANSIBLE_TAG=master
FROM ghcr.io/cmsgov/bfd-ansible:$BASE_ANSIBLE_TAG
ARG BFD_VERSION
LABEL org.opencontainers.image.source=https://github.com/CMSgov/beneficiary-fhir-data

# flyway migrations for test harness setup
COPY bfd-model/bfd-model-rif/src/main/resources/db/migration /flyway/sql

# bfd-server assets
COPY bfd-server/bfd-server-launcher/target/bfd-server-launcher-${BFD_VERSION}.zip /.m2/repository/gov/cms/bfd/bfd-server-launcher/${BFD_VERSION}/bfd-server-launcher-${BFD_VERSION}.zip
COPY bfd-server/bfd-server-war/target/bfd-server-war-${BFD_VERSION}.war /.m2/repository/gov/cms/bfd/bfd-server-war/${BFD_VERSION}/bfd-server-war-${BFD_VERSION}.war

# bfd-pipeline assets
COPY bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-${BFD_VERSION}.zip /.m2/repository/gov/cms/bfd/bfd-pipeline-app/${BFD_VERSION}/bfd-pipeline-app-${BFD_VERSION}.zip

# bfd-db-migrator assets
COPY bfd-db-migrator/target/bfd-db-migrator-${BFD_VERSION}.zip /.m2/repository/gov/cms/bfd/bfd-db-migrator/${BFD_VERSION}/bfd-db-migrator-${BFD_VERSION}.zip
