output "name" {
  value = aws_kinesis_firehose_delivery_stream.main.name
}

output "partitions" {
  # NOTE: temporarily re-quoted string for 0.13 compatability
  value = [{name="dt", type="string", comment="Approximate delivery time"}]
}
