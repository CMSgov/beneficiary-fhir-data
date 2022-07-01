# Creates AWS Glue Database named "bfd-insights-bfd-<environment>"
module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk = data.aws_kms_key.kms_key.arn
  tags       = local.tags
}


# API Requests
#
# Target location for ingested logs, no matter the method of ingestion.

# Target Glue Table where ingested logs are eventually stored
module "api-requests-table" {
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
    },
    {
      name    = "day"
      type    = "string"
      comment = "Day of request"
    }
  ]

  columns = [
    { name="context", type = "string", comment = "" },
    { name="level", type = "string", comment = "" },
    { name="logger", type = "string", comment = "" },
    { name="message", type = "string", comment = "" },
    { name="thread", type = "string", comment = "" },
    { name="timestamp", type = "string", comment = "" },
    { name="mdc.bene_id", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.batch", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.success", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.bene_by_hicn_or_id.include_hicns_and_mbis.type", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.batch", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.success", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_hicn.hicns_from_beneficiarieshistory.type", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.batch", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.success", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_id.include_hicns_and_mbis.type", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.batch", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.size", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.success", type = "string", comment = "" },
    { name="mdc.database_query.bene_by_mbi.mbis_from_beneficiarieshistory.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.carrier.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.dme.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hha.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.hospice.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.inpatient.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.outpatient.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.pde.type", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.batch", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.size", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.success", type = "string", comment = "" },
    { name="mdc.database_query.eobs_by_bene_id.snf.type", type = "string", comment = "" },
    { name="mdc.database_query.unknown.batch", type = "string", comment = "" },
    { name="mdc.database_query.unknown.batch_size", type = "string", comment = "" },
    { name="mdc.database_query.unknown.datasource_name", type = "string", comment = "" },
    { name="mdc.database_query.unknown.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.database_query.unknown.query", type = "string", comment = "" },
    { name="mdc.database_query.unknown.size", type = "string", comment = "" },
    { name="mdc.database_query.unknown.success", type = "string", comment = "" },
    { name="mdc.database_query.unknown.type", type = "string", comment = "" },
    { name="mdc.http_access.request.clientssl.dn", type = "string", comment = "" },
    { name="mdc.http_access.request.header.accept", type = "string", comment = "" },
    { name="mdc.http_access.request.header.accept-charset", type = "string", comment = "" },
    { name="mdc.http_access.request.header.accept-encoding", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bfd-originalqueryid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-application", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-applicationid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-authappid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-authappname", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-authclientid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-authuuid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-backendcall", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-beneficiaryid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-developerid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-originalquery", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-originalquerycounter", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-originalqueryid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-originalquerytimestamp", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-originalurl", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bluebutton-userid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bulk-clientid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.bulk-jobid", type = "string", comment = "" },
    { name="mdc.http_access.request.header.connection", type = "string", comment = "" },
    { name="mdc.http_access.request.header.content-length", type = "string", comment = "" },
    { name="mdc.http_access.request.header.content-type", type = "string", comment = "" },
    { name="mdc.http_access.request.header.host", type = "string", comment = "" },
    { name="mdc.http_access.request.header.includeaddressfields", type = "string", comment = "" },
    { name="mdc.http_access.request.header.includeidentifiers", type = "string", comment = "" },
    { name="mdc.http_access.request.header.includetaxnumbers", type = "string", comment = "" },
    { name="mdc.http_access.request.header.keep-alive", type = "string", comment = "" },
    { name="mdc.http_access.request.header.user-agent", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-forwarded-for", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-forwarded-host", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-forwarded-proto", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-newrelic-id", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-newrelic-synthetics", type = "string", comment = "" },
    { name="mdc.http_access.request.header.x-newrelic-transaction", type = "string", comment = "" },
    { name="mdc.http_access.request.http_method", type = "string", comment = "" },
    { name="mdc.http_access.request.operation", type = "string", comment = "" },
    { name="mdc.http_access.request.query_string", type = "string", comment = "" },
    { name="mdc.http_access.request.uri", type = "string", comment = "" },
    { name="mdc.http_access.request.url", type = "string", comment = "" },
    { name="mdc.http_access.request_type", type = "string", comment = "" },
    { name="mdc.http_access.response.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.http_access.response.header.connection", type = "string", comment = "" },
    { name="mdc.http_access.response.header.content-encoding", type = "string", comment = "" },
    { name="mdc.http_access.response.header.content-location", type = "string", comment = "" },
    { name="mdc.http_access.response.header.content-type", type = "string", comment = "" },
    { name="mdc.http_access.response.header.date", type = "string", comment = "" },
    { name="mdc.http_access.response.header.last-modified", type = "string", comment = "" },
    { name="mdc.http_access.response.header.x-powered-by", type = "string", comment = "" },
    { name="mdc.http_access.response.header.x-request-id", type = "string", comment = "" },
    { name="mdc.http_access.response.output_size_in_bytes", type = "string", comment = "" },
    { name="mdc.http_access.response.status", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.bene_by_hicn_or_id.include_.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.bene_by_hicn_or_id.include_.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.bene_by_hicn_or_id.include_.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.hicns_from_beneficiarieshistory.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.hicns_from_beneficiarieshistory.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_hicn.hicns_from_beneficiarieshistory.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_mbi.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_mbi.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_id.include_mbi.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_mbi.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_mbi.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_mbi.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.benes_by_year_month_part_d_contract_id.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eob_by_id.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eob_by_id.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eob_by_id.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.carrier.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.carrier.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.dme.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.dme.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.dme.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hha.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hha.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hha.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hospice.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.hospice.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.inpatient.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.outpatient.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.pde.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.pde.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.pde.record_count", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.snf.duration_milliseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.snf.duration_nanoseconds", type = "string", comment = "" },
    { name="mdc.jpa_query.eobs_by_bene_id.snf.record_count", type = "string", comment = "" }
  ]
}

# Crawler for the API Requests table
resource "aws_glue_crawler" "api-requests-crawler" {
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
  name     = "${local.full_name}-api-requests-crawler"
  role     = data.aws_iam_role.glue-role.arn

  catalog_target {
    database_name = module.database.name
    tables = [
      module.api-requests-table.name,
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
    update_behavior = "UPDATE_IN_DATABASE"
  }
}


# BFD History
#
# Storage and Jobs for manually ingesting historical logs.

# Glue Table to store API History
module "api-history-table" {
  source         = "../../../modules/table"
  table          = "${local.full_name_underscore}_api_history"
  description    = "Store log files from BFD for analysis in BFD Insights"
  database       = module.database.name
  bucket         = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk     = data.aws_kms_key.kms_key.arn
  storage_format = "json"
  serde_format   = "grok"
  serde_parameters = {
    "input.format" = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
  tags           = local.tags
  partitions = [
    {
      name    = "partition_0"
      type    = "string"
      comment = ""
    },
    {
      name    = "partition_1"
      type    = "string"
      comment = ""
    }
  ]
  columns = [] # Don't specify here, because the schema is complex and it's sufficient to allow the crawler to define the columns
}

# Glue Crawler for the API History table
resource "aws_glue_crawler" "bfd-history-crawler" {
  database_name = module.database.name
  name          = "${local.full_name}-history-crawler"
  description   = "Glue Crawler to ingest logs into the API History Glue Table"
  role          = data.aws_iam_role.glue-role.arn

  classifiers = [
    aws_glue_classifier.bfd-historicals-local.name,
  ]

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  catalog_target {
    database_name = module.database.name
    tables        = [ module.api-history-table.name ]
  }

  schema_change_policy {
    delete_behavior = "LOG" # "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }

  configuration = jsonencode(
    {
      "Version": 1.0,
      "Grouping": {
        "TableGroupingPolicy": "CombineCompatibleSchemas"
      }
    }
  )
}

# Classifier for the History Crawler
resource "aws_glue_classifier" "bfd-historicals-local" {
  name = "${local.full_name}-historicals-local"

  grok_classifier {
    classification = "cw-history"
    grok_pattern   = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
}

# S3 object containing the Glue Script for history ingestion
resource "aws_s3_object" "bfd-history-ingest" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_history_ingest.py"
  metadata           = {}
  storage_class      = "STANDARD"
  source             = "glue_src/bfd_history_ingest.py"
}

# Glue Job for history ingestion
resource "aws_glue_job" "bfd-history-ingest-job" {
  name                      = "${local.full_name}-history-ingest"
  description               = "Ingest historical log data"
  connections               = []
  glue_version              = "3.0"
  max_retries               = 0
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  timeout                   = 2880
  worker_type               = "G.1X"

  default_arguments = {
    "--TempDir"                          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-enable"
    "--job-language"                     = "python"
    "--sourceDatabase"                   = module.database.name
    "--sourceTable"                      = module.api-history-table.name
    "--spark-event-logs-path"            = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = module.database.name
    "--targetTable"                      = module.api-requests-table.name
  }

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-history-ingest.bucket}/${aws_s3_object.bfd-history-ingest.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}


# Glue Workflow
#
# Organizes the Glue jobs / crawlers and runs them in sequence

# Glue Workflow Object
resource "aws_glue_workflow" "glue-workflow" {
  name = "${local.full_name}-api-requests-workflow"
  max_concurrent_runs = "1"
}

# Trigger for History Ingest Crawler. This will run every night at 4am UTC, but it can also be run
# manually through the Console
resource "aws_glue_trigger" "history-ingest-crawler-trigger" {
  name          = "${local.full_name}-history-ingest-crawler-trigger"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "SCHEDULED"
  schedule      = "cron(0 4 * * ? *)" # Every day at 4am UTC

  actions {
    crawler_name = aws_glue_crawler.bfd-history-crawler.name
  }
}

# Trigger for History Ingest Job
resource "aws_glue_trigger" "bfd-history-ingest-job-trigger" {
  name          = "${local.full_name}-history-ingest-trigger"
  description   = "Trigger to start the History Ingest Glue Job whenever the Crawler completes successfully"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "CONDITIONAL"

  actions {
    job_name = aws_glue_job.bfd-history-ingest-job.name
  }

  predicate {
    conditions {
      crawler_name = aws_glue_crawler.bfd-history-crawler.name
      crawl_state  = "SUCCEEDED"
    }
  }
}

# Trigger for API Requests Crawler
resource "aws_glue_trigger" "bfd-api-requests-crawler-trigger" {
  name          = "${local.full_name}-api-requests-crawler-trigger"
  description   = "Trigger to start the API Requests Crawler whenever the History Ingest Job completes successfully"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "CONDITIONAL"

  actions {
    crawler_name = aws_glue_crawler.api-requests-crawler.name
  }

  predicate {
    conditions {
      job_name = aws_glue_job.bfd-history-ingest-job.name
      state  = "SUCCEEDED"
    }
  }
}
