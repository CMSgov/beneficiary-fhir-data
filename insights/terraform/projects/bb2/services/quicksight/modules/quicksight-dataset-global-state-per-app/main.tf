data "aws_caller_identity" "current" {}

locals {
  account_id       = data.aws_caller_identity.current.account_id
  current_user_arn = data.aws_caller_identity.current.arn
}

resource "aws_quicksight_data_set" "quicksight_data_set_global_state_per_app" {

  data_set_id = var.id
  name        = var.name
  import_mode = "SPICE"

  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_readers}"
    actions = [
      "quicksight:DescribeDataSet",
      "quicksight:DescribeDataSetPermissions",
      "quicksight:DescribeIngestion",
      "quicksight:DescribeRefreshSchedule",
      "quicksight:ListIngestions",
      "quicksight:ListRefreshSchedules",
      "quicksight:PassDataSet",
    ]
  }

  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_owners}"
    actions = [
      "quicksight:DeleteDataSet",
      "quicksight:UpdateDataSetPermissions",
      "quicksight:PutDataSetRefreshProperties",
      "quicksight:CreateRefreshSchedule",
      "quicksight:CancelIngestion",
      "quicksight:DeleteRefreshSchedule",
      "quicksight:ListRefreshSchedules",
      "quicksight:UpdateRefreshSchedule",
      "quicksight:PassDataSet",
      "quicksight:DescribeDataSetRefreshProperties",
      "quicksight:DescribeDataSet",
      "quicksight:CreateIngestion",
      "quicksight:DescribeRefreshSchedule",
      "quicksight:ListIngestions",
      "quicksight:UpdateDataSet",
      "quicksight:DescribeDataSetPermissions",
      "quicksight:DeleteDataSetRefreshProperties",
      "quicksight:DescribeIngestion"
    ]
  }



  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_admins}"
    actions = [
      "quicksight:DeleteDataSet",
      "quicksight:UpdateDataSetPermissions",
      "quicksight:PutDataSetRefreshProperties",
      "quicksight:CreateRefreshSchedule",
      "quicksight:CancelIngestion",
      "quicksight:DeleteRefreshSchedule",
      "quicksight:ListRefreshSchedules",
      "quicksight:UpdateRefreshSchedule",
      "quicksight:PassDataSet",
      "quicksight:DescribeDataSetRefreshProperties",
      "quicksight:DescribeDataSet",
      "quicksight:CreateIngestion",
      "quicksight:DescribeRefreshSchedule",
      "quicksight:ListIngestions",
      "quicksight:UpdateDataSet",
      "quicksight:DescribeDataSetPermissions",
      "quicksight:DeleteDataSetRefreshProperties",
      "quicksight:DescribeIngestion"
    ]
  }


  physical_table_map {
    physical_table_map_id = var.physical_table_map_id

    relational_table {
      catalog         = "AwsDataCatalog"
      data_source_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:datasource/${var.data_source_id}"
      name            = var.data_source_name
      schema          = "bb2"



      input_columns {
          name = "vpc"
          type = "STRING"
      }
      input_columns {
          name = "start_date"
          type = "DATETIME"
      }
      input_columns {
          name = "end_date"
          type = "DATETIME"
      }
      input_columns {
          name = "report_date"
          type = "DATETIME"
      }
      input_columns {
          name = "max_group_timestamp"
          type = "STRING"
      }
      input_columns {
          name = "max_real_bene_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "max_synth_bene_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "max_crosswalk_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_crosswalk_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_crosswalk_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_crosswalk_archived_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_archived_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grantarchived_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grantarchived_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_and_archived_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_grant_and_archived_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_token_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_token_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_token_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_token_archived_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_apps_active_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_apps_inactive_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_apps_require_demographic_scopes_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_developer_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_developer_distinct_organization_name_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_developer_with_first_api_call_count"
          type = "INTEGER"
      }
      input_columns {
          name = "max_global_developer_with_registered_app_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_name"
          type = "STRING"
      }
      input_columns {
          name = "app_id"
          type = "STRING"
      }
      input_columns {
          name = "app_created"
          type = "STRING"
      }
      input_columns {
          name = "app_updated"
          type = "STRING"
      }
      input_columns {
          name = "app_active"
          type = "BOOLEAN"
      }
      input_columns {
          name = "app_first_active"
          type = "STRING"
      }
      input_columns {
          name = "app_last_active"
          type = "STRING"
      }
      input_columns {
          name = "app_require_demographic_scopes"
          type = "BOOLEAN"
      }
      input_columns {
          name = "app_user_organization"
          type = "STRING"
      }
      input_columns {
          name = "app_user_id"
          type = "INTEGER"
      }
      input_columns {
          name = "app_user_username"
          type = "STRING"
      }
      input_columns {
          name = "app_user_date_joined"
          type = "STRING"
      }
      input_columns {
          name = "app_user_last_login"
          type = "STRING"
      }
      input_columns {
          name = "app_real_bene_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "app_synth_bene_cnt"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_archived_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grantarchived_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grantarchived_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_and_archived_real_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_grant_and_archived_synthetic_bene_deduped_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_archived_table_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_eob_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_eob_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_coverage_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_coverage_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_patient_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_patient_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_metadata_call_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_eob_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_eob_since_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_coverage_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v1_coverage_since_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_eob_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_eob_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_coverage_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_coverage_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_patient_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_patient_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_metadata_call_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_eob_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_eob_since_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_coverage_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v2_coverage_since_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_eob_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_eob_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_coverage_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_coverage_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_patient_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_patient_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_metadata_call_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_eob_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_eob_since_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_coverage_since_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_coverage_since_call_synthetic_count"
          type = "INTEGER"
      }
    input_columns {
          name = "app_fhir_v3_generate_insurance_card_call_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_fhir_v3_generate_insurance_card_call_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_ok_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_ok_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_fail_or_deny_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_fail_or_deny_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_sharing_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_sharing_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_not_sharing_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_deny_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_required_choice_deny_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_not_required_not_sharing_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_not_required_not_sharing_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_not_required_deny_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_demoscope_not_required_deny_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_refresh_for_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_refresh_for_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_authorization_code_for_real_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_authorization_code_for_synthetic_bene_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_refresh_response_2xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_refresh_response_4xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_refresh_response_5xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_authorization_code_2xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_authorization_code_4xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_token_authorization_code_5xx_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authorize_initial_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_medicare_login_redirect_ok_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_medicare_login_redirect_fail_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_start_ok_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_start_fail_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_matched_new_bene_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_matched_new_bene_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_matched_returning_bene_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_authentication_matched_returning_bene_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_sls_callback_ok_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_sls_callback_ok_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_sls_callback_fail_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_get_ok_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_get_ok_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_get_fail_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_post_ok_real_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_post_ok_synthetic_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_approval_view_post_fail_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_ok_real_bene_distinct_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_ok_synthetic_bene_distinct_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_fail_or_deny_real_bene_distinct_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_auth_fail_or_deny_synthetic_bene_distinct_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_sdk_requests_python_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_sdk_requests_node_count"
          type = "INTEGER"
      }
      input_columns {
          name = "app_access_grant_category"
          type = "STRING"
      }
      input_columns {
          name = "app_access_grant_enabled"
          type = "BOOLEAN"
      }
    }
  }
}
