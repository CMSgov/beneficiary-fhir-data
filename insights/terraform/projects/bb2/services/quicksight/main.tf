locals {
  tags = {
    business    = "OEDA"
    application = local.application
    project     = local.project
    Environment = local.env
    stack       = "${local.application}-${local.project}-${local.env}"
  }
  application = "bfd-insights"
  project     = "bb2"
  env         = terraform.workspace
  region      = "us-east-1"

  # Get vars mapped for target workspace
  dataset_vars = lookup(var.datasets_global_state_map, terraform.workspace,
    {
      id                    = ""
      name                  = ""
      data_source_id        = ""
      data_source_name      = ""
      physical_table_map_id = ""
  })
  dataset_per_app_vars = lookup(var.datasets_global_state_per_app_map, terraform.workspace,
    {
      id                    = ""
      name                  = ""
      data_source_id        = ""
      data_source_name      = ""
      physical_table_map_id = ""
  })
  analysis_prod_applications_vars = lookup(var.analysis_prod_applications_map, terraform.workspace,
    {
      id                    = ""
      name                  = ""
      first_app_name_select = ""
  })

}

module "quicksight-dataset-global-state" {
  source = "./modules/quicksight-dataset-global-state"

  id                           = local.dataset_vars.id
  name                         = local.dataset_vars.name
  data_source_id               = local.dataset_vars.data_source_id
  data_source_name             = local.dataset_vars.data_source_name
  physical_table_map_id        = local.dataset_vars.physical_table_map_id
  quicksight_groupname_readers = var.quicksight_groupname_readers
  quicksight_groupname_owners  = var.quicksight_groupname_owners
  quicksight_groupname_admins  = var.quicksight_groupname_admins

}

module "quicksight-dataset-global-state-per-app" {
  source = "./modules/quicksight-dataset-global-state-per-app"

  id                           = local.dataset_per_app_vars.id
  name                         = local.dataset_per_app_vars.name
  data_source_id               = local.dataset_per_app_vars.data_source_id
  data_source_name             = local.dataset_per_app_vars.data_source_name
  physical_table_map_id        = local.dataset_per_app_vars.physical_table_map_id
  quicksight_groupname_readers = var.quicksight_groupname_readers
  quicksight_groupname_owners  = var.quicksight_groupname_owners
  quicksight_groupname_admins  = var.quicksight_groupname_admins

}

module "quicksight-analysis-prod-applications" {
  source = "./modules/quicksight-analysis-prod-applications"

  id                          = local.analysis_prod_applications_vars.id
  name                        = local.analysis_prod_applications_vars.name
  first_app_name_select       = local.analysis_prod_applications_vars.first_app_name_select
  data_set_id                 = local.dataset_per_app_vars.id
  quicksight_groupname_owners = var.quicksight_groupname_owners
  quicksight_groupname_admins = var.quicksight_groupname_admins

}
