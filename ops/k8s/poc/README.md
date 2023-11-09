# EKS POC

This directory contains the script and helm values file used in the EKS Proof of Concept.
The script copies values into SSM for use by the migrator job and then publishes the
job to the EKS cluster using helm.

The migrator job was sucessfully executed in a POC environment.

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
