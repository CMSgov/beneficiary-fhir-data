output "name" {
  value = aws_kinesis_firehose_delivery_stream.main.name
}

output "partitions" {
  value = [{name="dt", type=string, comment="Approximate delivery time"}]
}
