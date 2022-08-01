resource "aws_iam_policy" "ecr" {
  name        = "bfd-${local.env}-${local.service}-ecr"
  description = "Permissions to describe ${local.service} ECR images"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeImages"
            ],
            "Resource": "*"
        }
    ]
}   
EOF  
}

resource "aws_iam_policy" "ssm" {
  name        = "bfd-${local.env}-${local.service}-ssm-parameters"
  description = "Permissions to /bfd/${local.env}/common and /bfd/${local.env}/server SSM hierarchies"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:GetParametersByPath",
                "ssm:GetParameters",
                "ssm:GetParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/server/*"
            ]
        }
    ]
} 
EOF
}

resource "aws_iam_policy" "kms" {
  name        = "bfd-${local.env}-${local.service}-kms"
  description = "Permissions to decrypt master KMS key for ${local.env}"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kms:Decrypt"
            ],
            "Resource": [
                "${local.kms_key_arn}"
            ]
        }
    ]
}    
EOF  
}

resource "aws_iam_policy" "rds" {
  name        = "bfd-${local.env}-${local.service}-rds"
  description = "Permissions to describe ${local.env} RDS clusters"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "rds:DescribeDBClusters"
            ],
            "Resource": [
                "arn:aws:rds:us-east-1:${local.account_id}:cluster:bfd-${local.env}-aurora-cluster"
            ]
        }
    ]
}    
EOF  
}

resource "aws_iam_policy" "logs" {
  name        = "bfd-${local.env}-${local.service}-logs"
  description = "Permissions to create and write to bfd-${local.env}-${local.service} logs"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "logs:CreateLogGroup",
            "Resource": "arn:aws:logs:us-east-1:${local.account_id}:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": [
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${local.env}-${local.service}:*"
            ]
        }
    ]
}
EOF  
}

resource "aws_iam_policy" "s3" {
  name        = "bfd-${local.env}-${local.service}-s3"
  description = "Permissions to write to ${data.aws_s3_bucket.insights.arn} S3 bucket and associated ${local.service} paths"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl",
                "s3:AbortMultipartUpload"
            ],
            "Resource": [
                "${data.aws_s3_bucket.insights.arn}",
                "${data.aws_s3_bucket.insights.arn}/databases/bfd-insights-bfd-${local.env}/bfd_insights_bfd_bfd_${local.env}_server_regression/*"
            ]
        }
    ]
}
EOF  
}

resource "aws_iam_policy" "athena" {
  name        = "bfd-${local.env}-${local.service}-athena"
  description = "Permissions to query Athena tables and put query results at a particular S3 path in ${data.aws_s3_bucket.insights.arn}"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetBucketLocation",
                "s3:GetObject",
                "s3:ListBucket",
                "s3:ListBucketMultipartUploads",
                "s3:ListMultipartUploadParts",
                "s3:AbortMultipartUpload",
                "s3:CreateBucket",
                "s3:PutObject"
            ],
            "Resource": [
                "arn:aws:s3:::aws-athena-query-results-*",
                "${data.aws_s3_bucket.insights.arn}",
                "${data.aws_s3_bucket.insights.arn}/adhoc/query_results/bfd_insights_bfd_${local.env}_server_regression/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "athena:StartQueryExecution",
                "athena:GetQueryResults",
                "athena:GetWorkGroup",
                "athena:StopQueryExecution",
                "athena:GetQueryExecution"
            ],
            "Resource": [
                "arn:aws:athena:*:${local.account_id}:workgroup/bfd"
            ]
        }
    ]
}
EOF  
}

resource "aws_iam_role" "this" {
  name        = "bfd-${local.env}-${local.service}"
  path        = "/"
  description = "Role for lambda profile use for ${local.service} in ${local.env}"

  assume_role_policy = <<-EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Action": "sts:AssumeRole",
              "Effect": "Allow",
              "Principal": {
                  "Service": "lambda.amazonaws.com"
              }
          }
      ]
  }
  EOF

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole",
    aws_iam_policy.ecr.arn,
    aws_iam_policy.ssm.arn,
    aws_iam_policy.kms.arn,
    aws_iam_policy.rds.arn,
    aws_iam_policy.logs.arn,
    aws_iam_policy.s3.arn,
    aws_iam_policy.athena.arn
  ]
}

resource "aws_iam_policy" "glue" {
  name        = "bfd-${local.env}-${local.service}-glue"
  description = "Permissions start the bfd-${local.env}-${local.service} Glue crawler"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "glue:StartCrawler",
            "Resource": "${aws_glue_crawler.this.arn}"
        }
    ]
}
EOF  
}

resource "aws_iam_role" "glue_trigger" {
  name        = "bfd-${local.env}-${local.service}-glue-trigger"
  path        = "/"
  description = "Role for bfd-${local.env}-${local.service}-glue-trigger Lambda"

  assume_role_policy = <<-EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Action": "sts:AssumeRole",
              "Effect": "Allow",
              "Principal": {
                  "Service": "lambda.amazonaws.com"
              }
          }
      ]
  }
  EOF

  managed_policy_arns = [
    aws_iam_policy.glue.arn
  ]
}