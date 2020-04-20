locals {
  tags = {business = "OEDA", application = "bfd-insights", project="foodtruck"}
}

## Firehose for gathering data

module "firehose" {
  source          = "../../modules/firehose"
  stream          = "main"
  project         = "foodtruck"
  sensitivity     = "moderate"
  buffer_interval = 60
  tags            = local.tags
}
