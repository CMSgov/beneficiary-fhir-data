#!/usr/bin/env bash

DOCKER_TAG=${1:-mvn3-jdk8-an29-tf12}

docker build --file Dockerfile.cbc-build \
  --build-arg JAVA_VERSION=${2:-8} \
  --build-arg MAVEN_VERSION=${3:-3} \
  --build-arg ANSIBLE_VERSION=${4:-2.9.25} \
  --build-arg PACKER_VERSION=${5:-1.6.6} \
  --build-arg TERRAFORM_VERSION=${6:-0.12.31} \
  --tag public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG} \
  .

docker push public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG}
