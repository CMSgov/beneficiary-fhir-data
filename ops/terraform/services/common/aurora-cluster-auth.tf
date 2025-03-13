# NOTE: IAM database auth with Aurora doesn't support global conditions in policies. This limits our ability to cleanly
# restrict access based on things like Resource or Principal tags. For now, we are foregoing an ABAC approach and simply
# creating an IAM "fhirdb auth" role for each established environment (ie. prod, prod-sbx, test), and using IAM groups
# to manage access to the role.
# Ref: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/UsingWithRDS.IAMDBAuth.html

## POLICY DOCS
# allow assume role (require MFA and a matching role session name)
data "aws_iam_policy_document" "db_auth_role_trust_policy" {
  count = local.is_ephemeral_env ? 0 : 1
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.account_id}:root"]
    }
    condition {
      test     = "StringEquals"
      variable = "sts:RoleSessionName"
      values   = ["$${aws:username}"]
    }
    condition {
      test     = "Bool"
      variable = "aws:MultiFactorAuthPresent"
      values   = ["true"]
    }
  }
}

# allow rds-db:connect (no global conditions are available for this action)
data "aws_iam_policy_document" "db_rds_connect" {
  statement {
    sid     = "AllowRdsDbConnect"
    effect  = "Allow"
    actions = ["rds-db:connect"]
    resources = [
      "arn:aws:rds-db:${local.region}:${local.account_id}:dbuser:${aws_rds_cluster.aurora_cluster.cluster_resource_id}/*"
    ]
  }
}

# allow describe clusters and endpoints for the current Environment
data "aws_iam_policy_document" "db_describe_clusters" {
  count = local.is_ephemeral_env ? 0 : 1
  statement {
    sid    = "DescribeDbClustersAndEndpoints"
    effect = "Allow"
    actions = [
      "rds:DescribeDBClusters",
      "rds:DescribeDBClusterEndpoints"
    ]
    resources = [
      "arn:aws:rds:${local.region}:${local.account_id}:cluster:*",
      "arn:aws:rds:${local.region}:${local.account_id}:cluster-endpoint:*"
    ]
    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"
      values   = [local.env]
    }
  }
}

# allow db auth role assumption
data "aws_iam_policy_document" "db_allow_assume" {
  count = local.is_ephemeral_env ? 0 : 1
  statement {
    sid       = "AllowAssumeRole"
    effect    = "Allow"
    actions   = ["sts:AssumeRole"]
    resources = ["arn:aws:iam::${local.account_id}:role/bfd-fhirdb-${local.env}-auth"]
  }
}

# db auth role policy
data "aws_iam_policy_document" "db_auth_role_policy_combined" {
  count = local.is_ephemeral_env ? 0 : 1
  source_policy_documents = [
    data.aws_iam_policy_document.db_rds_connect.json,
    data.aws_iam_policy_document.db_describe_clusters[0].json
  ]
}

# fhirdb users group policy
data "aws_iam_policy_document" "db_users_group_policy_combined" {
  count = local.is_ephemeral_env ? 0 : 1
  source_policy_documents = [
    data.aws_iam_policy_document.db_allow_assume[0].json,
    data.aws_iam_policy_document.db_describe_clusters[0].json
  ]
}

## RESOURCES

# IAM group to manage database users
resource "aws_iam_group" "db_users" {
  count = local.is_ephemeral_env ? 0 : 1
  name  = "bfd-fhirdb-${local.env}-users"
}

# group policy
resource "aws_iam_policy" "db_users" {
  count       = local.is_ephemeral_env ? 0 : 1
  path = local.cloudtamer_iam_path
  name        = "bfd-fhirdb-${local.env}-users-gp"
  description = "Group policy for bfd-fhirdb-${local.env}-users"
  policy      = data.aws_iam_policy_document.db_users_group_policy_combined[0].json
}

# group policy attachment
resource "aws_iam_group_policy_attachment" "db_users" {
  count      = local.is_ephemeral_env ? 0 : 1
  group      = aws_iam_group.db_users[0].id
  policy_arn = aws_iam_policy.db_users[0].arn
}

# The fhirdb auth role users will assume when connecting to this environments clusters (includes ephemeral seeds).
# Note: the role session name must be set to the callers IAM username (case sensitive) when assuming this role.
resource "aws_iam_role" "db_auth" {
  count              = local.is_ephemeral_env ? 0 : 1
  name               = "bfd-fhirdb-${local.env}-auth"
  path = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  assume_role_policy = data.aws_iam_policy_document.db_auth_role_trust_policy[0].json
}

# Policy allowing the rds-db:connect action, as well as allowing assumed role users to describe clusters and endpoints.
resource "aws_iam_role_policy" "db_auth" {
  count  = local.is_ephemeral_env ? 0 : 1
  name   = "bfd-fhirdb-${local.env}-auth"
  role   = aws_iam_role.db_auth[0].id
  policy = data.aws_iam_policy_document.db_auth_role_policy_combined[0].json
}

# Policy allowing the rds-db:connect action for an ephemeral cluster
resource "aws_iam_policy" "db_auth_ephemeral" {
  count       = local.is_ephemeral_env ? 1 : 0
  name        = "bfd-fhirdb-${local.env}-auth"
  path = local.cloudtamer_iam_path
  description = "Role policy allowing db connect action for ${local.env} ephemeral cluster"
  policy      = data.aws_iam_policy_document.db_rds_connect.json
}

# attach the ephemeral policy to the auth role
data "aws_iam_role" "db_auth" {
  count = local.is_ephemeral_env ? 1 : 0
  name  = "bfd-fhirdb-${local.seed_env}-auth"
}
resource "aws_iam_role_policy_attachment" "db_auth" {
  count      = local.is_ephemeral_env ? 1 : 0
  role       = data.aws_iam_role.db_auth[0].name
  policy_arn = aws_iam_policy.db_auth_ephemeral[0].arn
}


# Role allowing monitoring for RDS Clusters and instances
resource "aws_iam_role" "db_monitoring" {
  name               = "bfd-fhirdb-${local.env}-rds-monitoring"
  path = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  assume_role_policy = data.aws_iam_policy_document.rds_monitoring_assume_role_policy.json
  managed_policy_arns = ["arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"]
}
