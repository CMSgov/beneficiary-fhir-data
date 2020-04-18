data "aws_kms_key" "moderate" {
  key_id = "alias/bfd-insights-cmk"
}

resource "aws_iam_policy" "use_moderate_cmk" {
  name        = "bfd-insights-moderate-cmk"
  path        = "/bfd-insights/"
  description = "Allow access and use of the customer managed key"
  policy      = <<-EOF
  {
  "Version": "2012-10-17",
  "Statement": {
  "Effect": "Allow",
      "Action": [
            "kms:DescribeKey",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey",
            "kms:Decrypt",  
            "kms:Encrypt"
        ],
      "Resource": "${data.aws_kms_key.moderate.arn}"
    }
  }
  EOF
}



