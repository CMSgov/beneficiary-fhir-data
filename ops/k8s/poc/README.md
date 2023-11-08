# EKS POC

## Overview

This directory contains the script and helm values file used in the EKS Proof of Concept.
The script copies values into SSM for use by the migrator job and then publishes the
job to the EKS cluster using helm.

The migrator job was sucessfully executed in a POC environment.

In order to run the POC you need to have an available EKS cluster.
In order to avoid adding sensitive information to either the script or values
file, the script requires that several environment variables have been defined
before it can run.

These are:

EKS_SSM_KEY_ID: The KMS Key ARN for the key used to encrypt/decrypt secure values in SSM.
EKS_SSM_PREFIX: Path prefix of application settings in SSM.  This will be specific to the cluster's ephemeral environment.
EKS_SSM_CONFIG_ROOT: Name of a new hierarchy node within SSM to add runtime configuration settings to.
EKS_RDS_WRITER_ENDPOINT: Endpoint name for the RDS database's writer node.
EKS_ECR_REGISTRY: ECR name that can be added to image names to allow K8S to download images from the registry.
