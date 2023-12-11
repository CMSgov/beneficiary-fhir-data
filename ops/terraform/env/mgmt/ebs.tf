##
# Encrypt EBS volumes by default
##

# Get the default AWS managed key for the current region.
data "aws_kms_key" "ebs_amk" {
  key_id = "alias/aws/ebs"
}

# Set the default key for any EBS volume created without one.
resource "aws_ebs_default_kms_key" "ebs" {
  key_arn = data.aws_kms_key.ebs_amk.arn
}

# Ensure all EBS volumes are encrypted by default.
resource "aws_ebs_encryption_by_default" "default" {
  enabled = true
}
