resource "aws_dynamodb_table" "state_table" {
  name           = "bfd-tf-table"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "LockID"
  billing_mode   = "PROVISIONED"

  attribute {
    name = "LockID"
    type = "S"
  }
}
