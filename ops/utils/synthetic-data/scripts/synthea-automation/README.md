# Synthea Automation Scripts and Docker Image

This directory contains several Synthea-related scripts pertaining to generating synthetic data using Synthea.

## Generating Synthea Data Using the `bfd-run-synthea-generation` Jenkins Pipeline

This method is _preferred_ if you are looking to generate data that will be loaded into any of the
BFD Server's environments. Using this will automatically upload the Synthea data into S3 for easy
loading into the Pipeline.

1. Login to Jenkins
2. Navigate to `bfd`
3. Open the `bfd-run-synthea-generation` Pipeline
4. Choose "Build with Parameters"
5. Enter the desired number of beneficiaries to generate and number of months into the future to
   generate claims. Additionally, check the checkbox if the generated dataset **will be loaded into
   any environment**, otherwise the `end_state.properties` used for subsequent runs will _not be
   updated_
6. Click "Build" and wait until the pipeline finishes
7. The generated data will be available under the `generated` directory in the `bfd-mgmt-synthea` S3 bucket

## Generating Synthea Data Locally Using the `bfd-mgmt-synthea-generation` Docker Image

Generating Synthea data locally using the `bfd-mgmt-synthea-generation` Docker image has the
following advantages over using the `run_synthea_locally.sh` script:

1. The Docker image comes pre-built with the necessary mapping files, the Synthea executable, and
   all other dependencies
2. Using the Docker image does not affect your local filesystem
3. Using the Docker image does not require connecting to AWS or any external source; all data
   generation is done locally to your machine
   1. This applies only to the container itself -- since our Synthea generation process requires the
      previous `end_state.properties` and this file is stored in S3 _you_ (the operator) will need
      to download this file from S3 and provide it to the container in order to run generation. If
      this file is ever moved, the container will not need to be updated -- hence why this is a
      benefit

Usage is simple:

1. Ensure you are authenticated with AWS and are able to pull images from our ECR as well as from S3
2. Pull the latest `end_state.properties` file from S3:

   ```bash
   aws s3 cp "s3://bfd-mgmt-synthea/end_state/end_state.properties" .
   ```

3. Pull the `bfd-mgmt-synthea-generation` image from ECR:

   ```bash
   AWS_REGION="us-east-1"
   PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
   IMAGE_NAME="${PRIVATE_REGISTRY_URI}/bfd-mgmt-synthea-generation"

   docker pull "$IMAGE_NAME"
   ```

4. Run the image as a container, _bind-mounting_ the various directories (`out/`, `logs/`) as
   well as the `end_state.properties` file downloaded in step (2):

   ```bash
   NUMBER_OF_BENES=100
   NUMBER_OF_FUTURE_MONTHS=0
   docker run -v "$(pwd)"/end_state.properties:/usr/local/synthea/end_state.properties:ro \
    -v "$(pwd)"/logs:/usr/local/synthea/logs \
    -v "$(pwd)"/out:/usr/local/synthea/out "$IMAGE_NAME:latest" -n "$NUMBER_OF_BENES" -f "$NUMBER_OF_FUTURE_MONTHS"
   ```

5. When finished, the generated output should be available in your current working directory in the
   `out` sub-directory
