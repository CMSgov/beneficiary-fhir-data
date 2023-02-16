# DynamoDB table for Terraform state locking (state is stored in S3, but DynamoDB is used for locking)
# (see https://www.terraform.io/docs/language/settings/backends/s3.html#dynamodb-state-locking)
resource "aws_dynamodb_table" "state_table" {
  name           = "bfd-tf-table"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "LockID"
  billing_mode   = "PAY_PER_REQUEST"

  point_in_time_recovery {
    enabled = true
  }

  attribute {
    name = "LockID"
    type = "S"
  }
}
