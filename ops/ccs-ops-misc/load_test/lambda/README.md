# AWS Lambda Containers

Folders and files in this directory are related to building the Docker images used by AWS Lambda Container Functions. Each Lambda Container Image should have a corresponding subfolder under this (`lambda/`) directory.

## `bfd-locust-regression`

This sub-folder specifies the `Dockerfile` and Python handler necessary to build the Docker image for the regression suite Lambda Function. A `Jenkinsfile` also exists in this directory, specifying a Pipeline Job that builds the corresponding Docker image and pushes it to the `bfd-common-regression-suite-lambda` ECR.
