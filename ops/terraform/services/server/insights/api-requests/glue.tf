# Creates AWS Glue Database named "bfd-insights-bfd-<environment>"
module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk = data.aws_kms_key.kms_key.arn
  tags       = local.tags
}

# Target Glue Table where ingested logs are eventually stored
module "glue-table-api-requests" {
  source         = "../../../modules/table"
  table          = "${local.full_name_underscore}_api_requests"
  description    = "Target Glue Table where ingested logs are eventually stored"
  database       = module.database.name
  bucket         = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk     = data.aws_kms_key.kms_key.arn
  storage_format = "parquet"
  serde_format   = "parquet"
  tags           = local.tags

  partitions = [
    {
      name    = "year"
      type    = "string"
      comment = "Year of request"
    },
    {
      name    = "month"
      type    = "string"
      comment = "Month of request"
    }
  ]

  # We have to explicitly specify the schema here, because Firehose cannot export to a parquet
  # file without a static table schema.
  columns = [
    {
      "name"    = "cw_timestamp",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "cw_id",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "timestamp",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "level",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "thread",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "logger",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "message",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "context",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_bene_id",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_coverage_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_hicn_hicns_from_beneficiarieshistory_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_include_hicns_and_mbis_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_id_omit_hicns_and_mbis_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_bene_by_mbi_mbis_from_beneficiarieshistory_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_by_hash_collision_distinct_bene_ids",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_carrier_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_dme_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hha_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_hospice_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_inpatient_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_outpatient_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_pde_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_eobs_by_bene_id_snf_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_batch_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_batch",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_datasource_name",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_query",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_size",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_success",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_database_query_unknown_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_clientssl_dn",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_accept-charset",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_accept-encoding",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_accept",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bcda-cmsid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bcda-jobid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bfd-originalqueryid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-application",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-applicationid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-authappid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-authappname",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-authclientid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-authuuid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-backendcall",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-beneficiaryid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-developer",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-developerid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originalquery",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originalquerycounter",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originalqueryid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originalquerytimestamp",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originalurl",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-originatingipaddress",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bluebutton-userid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bulk-clientid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_bulk-jobid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_cache-control",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_connection",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_content-length",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_content-type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_dpc_clientid",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_host",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_includeaddressfields",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_includeidentifiers",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_includetaxnumbers",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_keep-alive",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_newrelic",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_traceparent",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_tracestate",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_user-agent",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-forwarded-for",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-forwarded-host",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-forwarded-proto",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-newrelic-id",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-newrelic-synthetics",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_header_x-newrelic-transaction",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_http_method",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_operation",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_query_string",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_uri",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_request_url",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_duration_per_kb",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_cache-control",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_connection",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_content-encoding",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_content-length",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_content-location",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_content-type",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_date",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_last-modified",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_x-newrelic-app-data",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_x-powered-by",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_header_x-request-id",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_output_size_in_bytes",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_http_access_response_status",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_bene_by_hicn_or_id_include__duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_bene_by_hicn_or_id_include__duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_bene_by_hicn_or_id_include__record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_hicns_from_beneficiarieshistory_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_hicns_from_beneficiarieshistory_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_hicn_hicns_from_beneficiarieshistory_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include__duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include__duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include__record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include_mbi_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include_mbi_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_id_include_mbi_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include__duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include__duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include__record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include_mbi_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include_mbi_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_bene_by_mbi_or_id_include_mbi_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_mbis_from_beneficiarieshistory_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_mbis_from_beneficiarieshistory_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_by_mbi_mbis_from_beneficiarieshistory_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_count_by_year_month_part_d_contract_id_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_count_by_year_month_part_d_contract_id_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_count_by_year_month_part_d_contract_id_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_ids_by_year_month_part_d_contract_id_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_ids_by_year_month_part_d_contract_id_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_bene_ids_by_year_month_part_d_contract_id_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_benes_by_year_month_part_d_contract_id_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_benes_by_year_month_part_d_contract_id_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_benes_by_year_month_part_d_contract_id_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_carrier_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_carrier_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_carrier_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_dme_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_dme_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_dme_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hha_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hha_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hha_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hospice_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hospice_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_hospice_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_inpatient_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_inpatient_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_inpatient_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_outpatient_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_outpatient_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_outpatient_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_pde_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_pde_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_pde_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_snf_duration_milliseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_snf_duration_nanoseconds",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_jpa_query_eobs_by_bene_id_snf_record_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_resources_returned_count",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_incoming_request_pre_process_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_incoming_request_post_process_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_incoming_request_pre_handle_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_outgoing_response_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_processing_completed_normally_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
    {
      "name"    = "mdc_hapi_server_processing_completed_timestamp_in_millis",
      "type"    = "string",
      "comment" = ""
    },
  ]
}

# Crawler for the API Requests table
resource "aws_glue_crawler" "glue-crawler-api-requests" {
  classifiers   = []
  database_name = module.database.name
  configuration = jsonencode(
    {
      CrawlerOutput = {
        Partitions = {
          AddOrUpdateBehavior = "InheritFromTable"
        }
      }
      Grouping = {
        TableGroupingPolicy = "CombineCompatibleSchemas"
      }
      Version = 1
    }
  )
  name = "${local.full_name}-api-requests-crawler"
  role = data.aws_iam_role.iam-role-glue.arn
  # Run this crawler to create the new partition at 00:30 on the first of every month
  schedule = "cron(30 0 1 * ? *)"

  catalog_target {
    database_name = module.database.name
    tables = [
      module.glue-table-api-requests.name,
    ]
  }

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "LOG"
  }
}
