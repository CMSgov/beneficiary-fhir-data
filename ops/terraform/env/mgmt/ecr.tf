locals {
  ecr_container_repositories = toset([
    # utility container image repositories
    "bfd-mgmt-eft-sftp-outbound-transfer-lambda",
    "bfd-mgmt-pipeline-ccw-manifests-verifier-lambda",
    "bfd-mgmt-server-load-broker",
    "bfd-mgmt-server-load-controller",
    "bfd-mgmt-server-load-node",
    "bfd-mgmt-server-regression",
    "bfd-mgmt-synthea-generation",
    # base container image repositories
    "bfd-mgmt-base-python",
    "bfd-mgmt-base-jdk",
    "bfd-mgmt-pipeline-idr",
    # sidecar container image repositories
    "bfd-mgmt-mount-certstores",
    # application container image repositories
    "bfd-db-migrator",
    "bfd-pipeline-app",
    "bfd-server",
  ])
}

resource "aws_ecr_repository" "bfd" {
  for_each = local.ecr_container_repositories

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = local.kms_key_id
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}
