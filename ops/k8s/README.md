# Kubernetes Resource Directory

## Overview

This directory contains resources for deploying the three primary applications in a local kubernetes environment using Rancher Desktop.  In a future release this will be expanded to support deployment to an EKS environment.  For local development all resources are deployed into a namespace named `dev`.

The directory is intended for use with Kustomize.  There are five applications that can be deployed to kubernetes.  Two of these are required only for local deployment:

* postgres: A simple postgresql 14 service using a small persistent volume.
* localstack: A simulator for S3, SSM, and SQS.

The other three applications are BFD applications that can be deployed for testing:

* migrator: The bfd-db-migrator program running inside kubernetes as a batch job.
* pipeline: The bfd-pipeline-app program running inside kubernetes as a batch job.
* server: The bfd-server-launcher/bfd-server-war program running inside kubernetes as a service.

All three of these programs are intended to be deployed using their corresponding run script in `/apps/utils/scripts`.  These scripts have been modified to include a `-k` command line option that directs the script to configure the program using the simulated SSM in the cluster and then deploy the app using resources in this directory.

## Getting Started

For local development you need to have either Rancher Desktop or Docker Desktop installed and kubernetes enabled.  You also must have `kubectl` and `docker` commands installed and in your PATH.  It is possible that other kubernetes distributions will work as well but only Rancher Desktop has been tested so far.

### Special Notes for Rancher Desktop

Be sure to use the versions of `kubectl` and `docker` installed by Rancher Desktop.  These are installed in `$HOME/.rd/bin` on Mac when using `brew install rancher` to install RD.  Also be sure to set your docker context to point to Rancher Desktop rather than the default.  Unset the `DOCKER_HOST`` environment variable if you have it set since it overrides the docker context setting.

There are known issues with running testcontainers inside of RD.  You may need to run normal builds in an shell using colima directly and limit your use of the RD shell to running the applications and interacting with kubernetes.

### Installing the external service apps

In a real cluster the database and AWS services would be external to the cluster.  For local development we use local versions so that we can test without being connected to the internet or using external resources.

All commands in this document assume that `ops/k8s` is your current working directory.

First you need to create the namespace used to run all of the applications.

```sh
kubectl create namespace dev
```

Next you need to start the database and AWS services.

```sh
kubectl apply -k postgres
kubectl apply -k localstack
```

## Running the Migrator

When starting with an empty database you need to run the migrator at least once.  Doing so creates the database schemas used by BFD in the cluster's postres database.

```sh
bash ../../apps/utils/scripts/run-db-migrator -k
```

This sends all of the migrator's configuration settings to the SSM inside the cluster.  They are added to a folder names `/bfd-db-migrator` in SSM.

The migrator should run once and exit.  You can view its logs using the command `kubectl -n dev logs --follow jobs/dev-migrator` or, if the job has already exited, by finding its pods and requesting the logs for them directly.

```sh
$ kubectl -n dev get pods
NAME                          READY   STATUS      RESTARTS       AGE
dev-migrator-ldmjr            0/1     Completed   0              3h2m
localstack-5646cbfd57-d7pqq   1/1     Running     1 (169m ago)   3h3m
postgres-5c8d8457dc-sl6nh     1/1     Running     1 (169m ago)   3h4m
dev-server-797db8dcd4-6f95f   1/1     Running     0              165m
dev-pipeline-6b98t            1/1     Running     0              30m
$ kubectl -n dev logs dev-migrator-ldmjr
```

Or you can install a kubernetes gui of your choice that makes this process easy!

## Running the RDA Pipeline

Now you can run the pipeline app to copy data into the database.  The easiest way to get some random data into the database is to use the RDA pipeline's random data mode.  This will install a number of partially adjudicated "claims" consisting of random values.

```sh
bash ../../apps/utils/scripts/run-bfd-pipeline -k random
```

The pipeline does not exit.  You can check from the logs to see if it succeeded and then kill it or remove it if you no longer want it to be running.  The easiest way to remove it from kubernetes is using kubectl.  Bear in mind this deletes the pipeline app entirely so the logs will be lost.  Of course the data will remain in the database.  

```sh
kubectl delete -k pipeline/overlays/local
```

This same deletion trick can be used for the migrator and server as well.

## Running the FHIR Server

Now you can run the server to query some of the random data.

```sh
bash ../../apps/utils/scripts/run-bfd-server -k
```

The server takes a few seconds to start.  Once it has started (you can confirm this using the logs) you can query for one of the random claims you added when the pipeline ran.  This curl should work since the RDA pipeline script always uses the same PRNG seed when starting the pipeline.

```sh
curl --insecure --cert ../../apps/bfd-server/dev/ssl-stores/client-unsecured.pem 'https://localhost:6500/v2/fhir/Claim/?isHashed=false&mbi=t7zw7bw0kzb&_format=json'
```