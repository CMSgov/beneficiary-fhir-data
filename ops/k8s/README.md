# Kubernetes Resource Directory

## Overview

This directory contains resources for deploying the three primary applications in a local kubernetes environment using Rancher Desktop or Docker Desktop.  In a future release this will be expanded to support deployment to an EKS environment.  For local development all resources are deployed into a namespace named `dev`.

The `ops/k8s` directory is intended for use with Helm.  There are five applications that can be deployed to kubernetes.  Each has a corresponding helm chart in its own subdirectory.

These three BFD applications can be installed for testing:

* migrator: The bfd-db-migrator program running inside kubernetes as a batch job.
* pipeline: The bfd-pipeline-app program running inside kubernetes as a batch job.
* server: The bfd-server-launcher/bfd-server-war program running inside kubernetes as a service.

All three of these programs are intended to be deployed using their corresponding run script in `/apps/utils/scripts`.  These scripts have been modified to include a `-k` command line option that directs the script to configure the program using the simulated SSM in the cluster and then deploy the app using resources in this directory.

The other two are required only for local deployment:

* postgres: A simple postgresql 16 service using a small persistent volume.
* localstack: A simulator for S3, SSM, and SQS.

These are installed using normal `helm install` commands as described later in this document.

## Getting Started

For local development you need to have either Rancher Desktop or Docker Desktop installed and kubernetes enabled.  You also must have `helm`, `kubectl` and `docker` commands installed and in your PATH.  It is possible that other kubernetes distributions will work as well but only Rancher Desktop and Docker Desktop have been tested so far.

Docker Desktop has Kubernetes support built in but you need to enable it.
To do so go to: `Options > Kubernetes` then check the `Enable Kubernetes` box and restart.

### Special Notes for Rancher Desktop

Be sure to use the versions of `helm`, `kubectl` and `docker` installed by Rancher Desktop.  These are installed in `$HOME/.rd/bin` on Mac when using `brew install rancher` to install RD.  Also be sure to set your docker context to point to Rancher Desktop rather than the default.  Unset the `DOCKER_HOST` environment variable if you have it set since it overrides the docker context setting.

There are known issues with running testcontainers inside of RD.  You may need to run normal builds in a shell using colima directly and limit your use of the shell using Rancher Desktop to running the applications and interacting with kubernetes.

### Installing the external service apps

In a real cluster the database and AWS services would be external to the cluster.  For local development we use local versions so that we can test without being connected to the internet or using external resources.

All commands in this document assume that your current working directory is `ops/k8s`.

First you need to create the namespace used to run all of the applications.

```sh
kubectl create namespace dev
```

Next you need to start the database and AWS services.

```sh
helm -n dev install postgres helm/postgres
helm -n dev install localstack helm/localstack
```

At this point you should see both charts have been installed.

```sh
$ helm -n dev list
NAME      	NAMESPACE	REVISION	UPDATED                             	STATUS  	CHART           	APP VERSION
localstack	default  	1       	2023-10-23 09:12:25.892621 -0400 EDT	deployed	localstack-0.1.0	2.2.0      
postgres  	default  	1       	2023-10-23 09:12:06.429432 -0400 EDT	deployed	postgres-0.1.0  	14         
```

## Building Docker Images

The local docker daemon needs to have the BFD docker images installed before any apps can be loaded.  The easiest way to build these is to perform a clean build using `build-bfd` with the `-J` option:

```sh
$ build-bfd -x -J
```

The equivalent maven command line is:

```sh
$ mvn -e clean install -DskipTests=true -DskipITs -Dmaven.javadoc.skip=true -Dcheckstyle.skip -Djib.skip=false -Dmaven.build.cache.skipCache=true
```

## Running the Migrator

The postgres chart always starts with an empty database.  None of the schemas or tables will be present when the service starts.  To populate the database you need to run the migrator at least once.

```sh
bash ../../apps/utils/scripts/run-db-migrator -k
```

This sends all of the migrator's configuration settings to the SSM inside the cluster.  They are added to a folder named `/bfd-db-migrator` in SSM.

The migrator should run once and exit.  You can view its logs using the command `kubectl -n dev logs --follow jobs/dev-migrator` or, if the job has already exited, by finding its pods and requesting the logs for them directly.

```sh
$ kubectl -n dev get pods
NAME                          READY   STATUS      RESTARTS       AGE
dev-migrator-ldmjr            0/1     Completed   0              3h2m
localstack-5646cbfd57-d7pqq   1/1     Running     1 (169m ago)   3h3m
postgres-5c8d8457dc-sl6nh     1/1     Running     1 (169m ago)   3h4m
$ kubectl -n dev logs dev-migrator-ldmjr
```

Or you can use a kubernetes GUI of your choice that makes this process easy!  Both Rancher Desktop and Docker Desktop have GUI dashboards built in.  Alternatively, Visual Studio Code has a kubernetes extension that supports browsing resources and searching logs.

## Running the RDA Pipeline

Now you can run the pipeline app to copy data into the database.  The easiest way to get some random data into the database is to use the RDA pipeline's random data mode.  This will install a number of partially adjudicated "claims" consisting of random values.

```sh
bash ../../apps/utils/scripts/run-bfd-pipeline -k random
```

The pipeline does not exit.  You can check from the logs to see if it succeeded and then kill it or remove it if you no longer want it to be running.  The easiest way to remove it from kubernetes is using `helm -n dev uninstall`.  Bear in mind this deletes the pipeline app entirely so its logs will be removed.  Of course this won't affect the data written to the database.  

```sh
helm -n dev uninstall pipeline-rda
```

This same deletion trick can be used for the migrator and server as well.

NOTE: The `run-bfd-pipeline` script adds `-rif` or `-rda` to the pipeline chart name depending on which pipeline you tell it to run.  You can always see which charts are currently running using `helm -n dev list`.

```sh
$ helm -n dev list
NAME      	NAMESPACE	REVISION	UPDATED                             	STATUS  	CHART                	APP VERSION   
localstack	dev      	1       	2023-10-23 10:26:56.282636 -0400 EDT	deployed	localstack-0.1.0     	2.2.0         
migrator  	dev      	1       	2023-10-23 10:30:35.171116 -0400 EDT	deployed	bfd-db-migrator-0.1.0	1.0.0-SNAPSHOT
postgres  	dev      	1       	2023-10-23 10:27:21.341919 -0400 EDT	deployed	postgres-0.1.0       	14            
```

## Running the FHIR Server

Now you can run the server to query some of the random data.

```sh
bash ../../apps/utils/scripts/run-bfd-server -k
```

The server takes a few seconds to start.  Once it has started (you can confirm this using its logs) you can query for one of the random claims you added when the pipeline ran.  This curl should work since the RDA pipeline script always uses the same PRNG seed when starting the pipeline.

```sh
curl --insecure --cert ../../apps/bfd-server/dev/ssl-stores/client-unsecured.pem 'https://localhost:6500/v2/fhir/Claim/?isHashed=false&mbi=t7zw7bw0kzb&_format=json'
```