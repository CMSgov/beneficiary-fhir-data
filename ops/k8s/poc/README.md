# EKS POC

This directory contains the script and helm values file used in the EKS Proof of Concept.
The script supports all three applications in bfd: server, pipeline (rda and ccw), and migrator.
The script copies configuration values from their current locations in SSM into a flat hierarchy
within SSM for use by the apps and then publishes the app to the EKS cluster using helm.

All three applications have been successfully executed in a POC environment.

In order to run the POC you need to have an available EKS cluster.
In order to avoid adding sensitive information to either the script or values
file, the script requires that several environment variables have been defined
before it can run.

These are:

- `EKS_SSM_KEY_ID`: The KMS Key ARN for the key used to encrypt/decrypt secure values in SSM.
- `EKS_SSM_PREFIX`: Path prefix of application settings in SSM.  This will be specific to the cluster's ephemeral environment.
- `EKS_SSM_CONFIG_ROOT`: Name of a new hierarchy node within SSM to add runtime configuration settings to.
- `EKS_RDS_WRITER_ENDPOINT`: Endpoint name for the RDS database's writer node.
- `EKS_ECR_REGISTRY`: ECR name that can be added to image names to allow K8S to download images from the registry.

Additional environment variables are required only for the pipeline script:

- `EKS_RDA_GRPC_HOST`: Host name of the RDA API server to be called by the RDA pipeline.
- `EKS_RDA_GRPC_PORT`: Port of the RDA API server to be called by the RDA pipeline.
- `EKS_RDA_GRPC_AUTH_TOKEN`: Token used by the RDA pipeline to authenticate to the RDA API server.
- `EKS_S3_BUCKET_NAME`: Name of the S3 bucket used by the CCW and RDA pipelines.

EKS has some special needs that a desktop cluster does not.
These have been accomodated by adding new values to the helm values file and expanding the template to use those values if they are present.  The special values are:

## Tolerations

EKS/Fargate [assigns taints](https://docs.aws.amazon.com/eks/latest/userguide/node-taints-managed-node-groups.html) to nodes.
In order for a pod to be scheduled on a tainted node we have to define `tolerations` in the pod spec.
A new `podTolerations` value was defined as a list of objects with three properties: `key`, `operator`, and `effect`.
These correspond to the taints that we are willing to accept on the nodes our pods run on.
When this list is non-empty the template renders a `tolerations` property in the job's pod spec.

## Labels

The EKS infrastructure defines Fargate profiles to assign specific permissions to pods running in the cluster.
For the POC we associated a label named `job-name` to the profile.
A new `labels` value was defined as a list of objects with two properties: `label` and `value`.
When this list is non-empty the template renders a `labels` property in the job's metadata.

## Service Account

A new `serviceAccountName` string value was defined to specify the appropriate service account for the job.
When this has a non-empty value a `serviceAccountName` property is added to the job's pod spec.

# Run scripts

The `run-migrator.sh` script does not require any command line arguments.

The `run-pipeline.sh` script requires a single command line argument to specify its run mode.  Possible values are `rda` (call the RDA API server and retrieve values from it), `random` (use an internal random rda server), or `ccw` (load CCW/RIF data from the S3 bucket).

The `run-server.sh` script requires a single command line argument to specify a working directory.  The script will use this directory to hold temporary files containing certs and a keystore file downloaded from SSM until they can be uploaded to a secret in EKS.  Once they have been uploaded to EKS the working files are deleted.
