# BB2 extended setup for BFD insights
Configuration for BB2 custom Firehose data streams.

## Setup
The custom (extended) setup requires Terraform >= v0.13 to allow for_each modular development. Terraform v0.13.7 was used here.

To setup a new firehose data stream, add a variable map to variables.tf with custom settings.

Example:
```
variable "firehose" {
  description = "Environments you wish to create a firehose"
  type        = map
  default     = {
    # add a map for each firehose to be created
    # the firehose name should be used as the key for each firehose
    firehose-name = {
      table_name = "events_firehose_name"
      project = "bb2"
      database = "bb2"
    }
  }
}
```

Terraform Apply will create/update firehose, glue crawler and cloudwatch log destination for each map in the firehose variable settings.

