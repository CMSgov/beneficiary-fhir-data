locals {
  tags = { business = "OEDA", application = "bfd-insights", project = "bb2" }
}

module "firehose" {
  source          = "./modules/firehose"
  buffer_interval = var.buffer_interval #default set to 300
  buffer_size     = var.buffer_size     #default set to 5
  tags            = local.tags

  for_each      = var.firehose
  firehose_name = each.key
  table_name    = each.value.table_name
  project       = each.value.project
  database      = each.value.database
}

module "cwl_destination" {
  source = "./modules/cwl_destination"
  bb2_acct      = var.bb2_acct

  for_each      = var.firehose
  firehose_name = each.key
  project       = each.value.project

}

module "glue_crawler" {
  source = "./modules/glue_crawler"

  for_each      = var.firehose
  firehose_name = each.key
  project       = each.value.project
  database      = each.value.database
}