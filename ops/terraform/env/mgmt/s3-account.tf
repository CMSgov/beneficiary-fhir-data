# This manages public access settings for s3 buckets at the account level.
# Please note, that as long as we host public test data from the `bfd-public-test-data` s3 bucket,
# we can only block public acls/policies on *new* s3 buckets.

resource "aws_s3_account_public_access_block" "this" {
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = false # leave false due to bfd-public-test-data bucket
  restrict_public_buckets = false # leave false due to bfd-public-test-data bucket
}

