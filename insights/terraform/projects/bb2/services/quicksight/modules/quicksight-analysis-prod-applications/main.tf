data "aws_caller_identity" "current" {}

locals {
  account_id       = data.aws_caller_identity.current.account_id
  current_user_arn = data.aws_caller_identity.current.arn
}

resource "aws_quicksight_analysis" "quicksight_analysis_prod_applications" {
  analysis_id = var.id
  name        = var.name
  theme_arn   = "arn:aws:quicksight::aws:theme/MIDNIGHT"

  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_owners}"
    actions = [
      "quicksight:DeleteAnalysis",
      "quicksight:DescribeAnalysis",
      "quicksight:DescribeAnalysisPermissions",
      "quicksight:QueryAnalysis",
      "quicksight:RestoreAnalysis",
      "quicksight:UpdateAnalysis",
      "quicksight:UpdateAnalysisPermissions",
    ]
  }

  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_admins}"
    actions = [
      "quicksight:DeleteAnalysis",
      "quicksight:DescribeAnalysis",
      "quicksight:DescribeAnalysisPermissions",
      "quicksight:QueryAnalysis",
      "quicksight:RestoreAnalysis",
      "quicksight:UpdateAnalysis",
      "quicksight:UpdateAnalysisPermissions",
    ]
  }

  definition {
    data_set_identifiers_declarations {

      data_set_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:dataset/${var.data_set_prod_per_app_id}"
      identifier   = "prod_global_state_per_app"
    }

    analysis_defaults {
      default_new_sheet_configuration {
        sheet_content_type = "INTERACTIVE"

        interactive_layout_configuration {
          grid {
            canvas_size_options {
              screen_canvas_size_options {
                resize_option = "RESPONSIVE"
              }
            }
          }
        }
      }
    }


    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"

      expression = "  sum({app_grant_and_archived_real_bene_deduped_count})\n  -\n  lag(\n    sum({app_grant_and_archived_real_bene_deduped_count}), \n    [{report_date} ASC],\n    1,\n    [{app_user_organization}, {app_name}]\n  )\n"
      name       = "calc_app_grant_and_archived_real_bene_deduped_wow_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "({calc_app_authentication_start_ok_count} / {calc_app_medicare_login_redirect_ok_count})"
      name                = "calc_app_diff_auth_start_vs_initial_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "ifelse($${DYNAMICFIELD} = 'APP Enrollees Served (all-time)', {calc_app_grant_and_archived_real_bene_deduped_count},\n$${DYNAMICFIELD} = 'APP Enrollees Served WoW (gains this week)', {calc_app_grant_and_archived_real_bene_deduped_wow_count},\n$${DYNAMICFIELD} = 'APP Requires Demographic Scopes', {calc_app_require_demographic_scopes},\n$${DYNAMICFIELD} = 'APP Is Enabled?', {calc_app_active},\n$${DYNAMICFIELD} = 'APP Is Grant Enabled?', {calc_app_access_grant_enabled},\n$${DYNAMICFIELD} = 'APP Synth Enrollee Served', {calc_app_grant_and_archived_synthetic_bene_deduped_count},\n$${DYNAMICFIELD} = 'Auth Status OK Real Enrollee', {calc_app_auth_ok_real_bene_count},\n$${DYNAMICFIELD} = 'Auth Status Deny Real Enrollee', {calc_app_auth_fail_or_deny_real_bene_count},\n$${DYNAMICFIELD} = 'Auth App Wants Demographic Enrollee Share Real', {calc_app_auth_demoscope_required_choice_sharing_real_bene_count},\n$${DYNAMICFIELD} = 'Auth App Wants Demographic Enrollee Not Share Real', {calc_app_auth_demoscope_required_choice_not_sharing_real_bene_count},\n$${DYNAMICFIELD} = 'Auth App Wants Demographic Enrollee Deny Real', {calc_app_auth_demoscope_required_choice_deny_real_bene_count},\n$${DYNAMICFIELD} = 'Auth App Not Want Demographic Enrollee Not Share Real', {calc_app_auth_demoscope_not_required_not_sharing_real_bene_count},\n$${DYNAMICFIELD} = 'Auth App Not want Demographic Enrollee Deny Real', {calc_app_auth_demoscope_not_required_deny_real_bene_count},\n$${DYNAMICFIELD} = 'Token Auth-Code Requests Real Enrollee', {calc_app_token_authorization_code_for_real_bene_count},\n$${DYNAMICFIELD} = 'Token Auth-Code Requests Synth Enrollee', {calc_app_token_authorization_code_for_synthetic_bene_count},\n$${DYNAMICFIELD} = 'Token Auth-Code Requests 2xx', {calc_app_token_authorization_code_2xx_count},\n$${DYNAMICFIELD} = 'Token Auth-Code Requests 4xx', {calc_app_token_authorization_code_4xx_count},\n$${DYNAMICFIELD} = 'Token Auth-Code Requests 5xx', {calc_app_token_authorization_code_5xx_count},\n$${DYNAMICFIELD} = 'Token Refresh Requests Real Enrollee', {calc_app_token_refresh_for_real_bene_count},\n$${DYNAMICFIELD} = 'Token Refresh Requests Synth Enrollee', {calc_app_token_refresh_for_synthetic_bene_count},\n$${DYNAMICFIELD} = 'Token Refresh Requests 2xx', {calc_app_token_refresh_response_2xx_count},\n$${DYNAMICFIELD} = 'Token Refresh Requests 4xx', {calc_app_token_refresh_response_4xx_count},\n$${DYNAMICFIELD} = 'Token Refresh Requests 5xx', {calc_app_token_refresh_response_5xx_count},\n$${DYNAMICFIELD} = 'FHIR Total Requests', {calc_app_fhir_call_real_count},\n$${DYNAMICFIELD} = 'FHIR EOB Requests', {calc_app_fhir_eob_real_count},\n$${DYNAMICFIELD} = 'FHIR Coverage Requests', {calc_app_fhir_coverage_real_count},\n$${DYNAMICFIELD} = 'FHIR Patient Requests', {calc_app_fhir_patient_real_count},\n$${DYNAMICFIELD} = 'FHIR EOB With Since Requests', {calc_app_fhir_eob_since_real_count},\n$${DYNAMICFIELD} = 'FHIR Coverage With Since Requests', {calc_app_fhir_coverage_since_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 Total Requests', {calc_app_fhir_v1_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 EOB Requests', {calc_app_fhir_v1_eob_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 Coverage Requests', {calc_app_fhir_v1_coverage_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 Patient Requests', {calc_app_fhir_v1_patient_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 EOB With Since Requests', {calc_app_fhir_v1_eob_since_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V1 Coverage With Since Requests', {calc_app_fhir_v1_coverage_since_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 Total Requests', {calc_app_fhir_v2_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 EOB Requests', {calc_app_fhir_v2_eob_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 Coverage Requests', {calc_app_fhir_v2_coverage_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 Patient Requests', {calc_app_fhir_v2_patient_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 EOB With Since Requests', {calc_app_fhir_v2_eob_since_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V2 Coverage With Since Requests', {calc_app_fhir_v2_coverage_since_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V3 Coverage Requests', {calc_app_fhir_v3_coverage_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V3 EOB Requests', {calc_app_fhir_v3_eob_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V3 Patient Requests', {calc_app_fhir_v3_patient_call_real_count},\n $${DYNAMICFIELD} = 'FHIR V3 Generate DIC Requests', {calc_app_fhir_v3_generate_insurance_card_call_real_count},\n$${DYNAMICFIELD} = 'FHIR V3 Total Requests', {calc_app_fhir_v3_call_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Authorize Initial' , {calc_app_authorize_initial_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Medicare Login Redirect OK', {calc_app_medicare_login_redirect_ok_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Medicare Login Redirect FAIL', {calc_app_medicare_login_redirect_fail_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Start OK', {calc_app_authentication_start_ok_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Start FAIL', {calc_app_authentication_start_fail_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Matched New Enrollee Real', {calc_app_authentication_matched_new_bene_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Matched New Enrollee Synthetic', {calc_app_authentication_matched_new_bene_synthetic_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Matched Returning Enrollee Real', {calc_app_authentication_matched_returning_bene_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Auth Matched Returning Enrollee Synthetic', {calc_app_authentication_matched_returning_bene_synthetic_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW SLS CallBack OK Real', {calc_app_sls_callback_ok_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW SLS CallBack OK Synthetic', {calc_app_sls_callback_ok_synthetic_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW SLS CallBack FAIL', {calc_app_sls_callback_fail_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View GET OK Real', {calc_app_approval_view_get_ok_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View GET OK Synthetic', {calc_app_approval_view_get_ok_synthetic_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View GET FAIL', {calc_app_approval_view_get_fail_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View POST OK Real', {calc_app_approval_view_post_ok_real_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View POST OK Synthetic', {calc_app_approval_view_post_ok_synthetic_count},\n$${DYNAMICFIELD} = 'AUTH-FLOW Approval View POST FAIL', {calc_app_approval_view_post_fail_count},\n {calc_app_grant_and_archived_real_bene_deduped_count})"
      name                = "Dynamic Field"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "round(({calc_app_sls_callback_ok_real_count}) / {calc_app_authentication_start_ok_count}, 3)"
      name                = "calc_app_diff_auth_sls_callback_ok_vs_start_real_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "round({calc_app_sls_callback_ok_synthetic_count} / {calc_app_authentication_start_ok_count}, 3)"
      name                = "calc_app_diff_auth_sls_callback_ok_vs_start_synthetic_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "substring({app_created},1,10)"
      name                = "calc_app_created"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "substring({app_first_active},1,10)"
      name                = "calc_app_first_active"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "substring({app_last_active}, 1, 10)"
      name                = "calc_app_last_active"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_active})"
      name                = "calc_app_active"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_get_fail_count})"
      name                = "calc_app_approval_view_get_fail_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_get_ok_real_count})"
      name                = "calc_app_approval_view_get_ok_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_get_ok_synthetic_count})"
      name                = "calc_app_approval_view_get_ok_synthetic_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_post_fail_count})"
      name                = "calc_app_approval_view_post_fail_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_post_ok_real_count})"
      name                = "calc_app_approval_view_post_ok_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_approval_view_post_ok_synthetic_count})"
      name                = "calc_app_approval_view_post_ok_synthetic_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_demoscope_not_required_deny_real_bene_count})"
      name                = "calc_app_auth_demoscope_not_required_deny_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_demoscope_not_required_not_sharing_real_bene_count})"
      name                = "calc_app_auth_demoscope_not_required_not_sharing_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_demoscope_required_choice_deny_real_bene_count})"
      name                = "calc_app_auth_demoscope_required_choice_deny_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_demoscope_required_choice_not_sharing_real_bene_count})"
      name                = "calc_app_auth_demoscope_required_choice_not_sharing_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_demoscope_required_choice_sharing_real_bene_count})"
      name                = "calc_app_auth_demoscope_required_choice_sharing_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_fail_or_deny_real_bene_count})"
      name                = "calc_app_auth_fail_or_deny_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_fail_or_deny_synthetic_bene_count})"
      name                = "calc_app_auth_fail_or_deny_synthetic_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_ok_real_bene_count})"
      name                = "calc_app_auth_ok_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_auth_ok_synthetic_bene_count})"
      name                = "calc_app_auth_ok_synthetic_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_matched_new_bene_real_count})"
      name                = "calc_app_authentication_matched_new_bene_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_matched_new_bene_synthetic_count})"
      name                = "calc_app_authentication_matched_new_bene_synthetic_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_matched_returning_bene_real_count})"
      name                = "calc_app_authentication_matched_returning_bene_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_matched_returning_bene_synthetic_count})"
      name                = "calc_app_authentication_matched_returning_bene_synthetic_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_start_fail_count})"
      name                = "calc_app_authentication_start_fail_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authentication_start_ok_count})"
      name                = "calc_app_authentication_start_ok_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_authorize_initial_count})\n"
      name                = "calc_app_authorize_initial_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_call_real_count} + {app_fhir_v2_call_real_count} + {app_fhir_v3_call_real_count})"
      name                = "calc_app_fhir_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_call_real_count})"
      name                = "calc_app_fhir_v1_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_coverage_call_real_count} + {app_fhir_v2_coverage_call_real_count} + {app_fhir_v3_coverage_call_real_count})"
      name                = "calc_app_fhir_coverage_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_coverage_call_real_count})"
      name                = "calc_app_fhir_v1_coverage_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_coverage_since_call_real_count})"
      name                = "calc_app_fhir_v1_coverage_since_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_coverage_since_call_real_count} + {app_fhir_v2_coverage_since_call_real_count})"
      name                = "calc_app_fhir_coverage_since_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_eob_call_real_count} + {app_fhir_v2_eob_call_real_count} + {app_fhir_v3_eob_call_real_count})"
      name                = "calc_app_fhir_eob_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_eob_call_real_count})"
      name                = "calc_app_fhir_v1_eob_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_eob_since_call_real_count})"
      name                = "calc_app_fhir_v1_eob_since_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_eob_since_call_real_count}+{app_fhir_v2_eob_since_call_real_count})"
      name                = "calc_app_fhir_eob_since_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_patient_call_real_count})"
      name                = "calc_app_fhir_v1_patient_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v1_patient_call_real_count} + {app_fhir_v2_patient_call_real_count} + {app_fhir_v3_patient_call_real_count})"
      name                = "calc_app_fhir_patient_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_call_real_count})"
      name                = "calc_app_fhir_v2_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_coverage_call_real_count})"
      name                = "calc_app_fhir_v2_coverage_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_coverage_since_call_real_count})"
      name                = "calc_app_fhir_v2_coverage_since_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_eob_call_real_count})"
      name                = "calc_app_fhir_v2_eob_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_eob_since_call_real_count})"
      name                = "calc_app_fhir_v2_eob_since_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v2_patient_call_real_count})"
      name                = "calc_app_fhir_v2_patient_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v3_call_real_count})"
      name                = "calc_app_fhir_v3_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v3_coverage_call_real_count})"
      name                = "calc_app_fhir_v3_coverage_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v3_eob_call_real_count})"
      name                = "calc_app_fhir_v3_eob_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v3_patient_call_real_count})"
      name                = "calc_app_fhir_v3_patient_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_fhir_v3_generate_insurance_card_call_real_count})"
      name                = "calc_app_fhir_v3_generate_insurance_card_call_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_grant_and_archived_real_bene_deduped_count})"
      name                = "calc_app_grant_and_archived_real_bene_deduped_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_grant_and_archived_synthetic_bene_deduped_count})"
      name                = "calc_app_grant_and_archived_synthetic_bene_deduped_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_medicare_login_redirect_fail_count})"
      name                = "calc_app_medicare_login_redirect_fail_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_medicare_login_redirect_ok_count})"
      name                = "calc_app_medicare_login_redirect_ok_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_require_demographic_scopes})"
      name                = "calc_app_require_demographic_scopes"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_sls_callback_fail_count})"
      name                = "calc_app_sls_callback_fail_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_sls_callback_ok_real_count})"
      name                = "calc_app_sls_callback_ok_real_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_sls_callback_ok_synthetic_count})"
      name                = "calc_app_sls_callback_ok_synthetic_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_authorization_code_2xx_count})"
      name                = "calc_app_token_authorization_code_2xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_authorization_code_4xx_count})"
      name                = "calc_app_token_authorization_code_4xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_authorization_code_5xx_count})"
      name                = "calc_app_token_authorization_code_5xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_authorization_code_for_real_bene_count})"
      name                = "calc_app_token_authorization_code_for_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_authorization_code_for_synthetic_bene_count})"
      name                = "calc_app_token_authorization_code_for_synthetic_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_refresh_for_real_bene_count})"
      name                = "calc_app_token_refresh_for_real_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_refresh_for_synthetic_bene_count})"
      name                = "calc_app_token_refresh_for_synthetic_bene_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_refresh_response_2xx_count})"
      name                = "calc_app_token_refresh_response_2xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_refresh_response_4xx_count})"
      name                = "calc_app_token_refresh_response_4xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_token_refresh_response_5xx_count})"
      name                = "calc_app_token_refresh_response_5xx_count"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "sum({app_access_grant_enabled})"
      name                = "calc_app_access_grant_enabled"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state_per_app"
      expression          = "ifelse( isNull(substring({app_access_grant_category}, 1, 10)) , 'N/A',  substring({app_access_grant_category}, 1, 10))"
      name                = "calc_app_access_grant_category"
    }


    # BUG NOTE: With AWS provider v5.29.0
    # Sometimes after tf apply, the ordering of the next 2 column_configurations
    #  show to be changed in the plan; however, the apply works.
    #  To remove the changes showing from the plan, you can reorder these.
    column_configurations {
      role = "MEASURE"

      column {
        column_name         = "Dynamic Field"
        data_set_identifier = "prod_global_state_per_app"
      }
    }
    column_configurations {
      role = "DIMENSION"

      column {
        column_name         = "calc_app_last_active"
        data_set_identifier = "prod_global_state_per_app"
      }
    }

    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "5f5c3a1c-eed6-4787-a57b-abdb380bb25d"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "3f24c4d9-c6d8-49fe-8df1-aa7250403d39"

          column {
            column_name         = "app_name"
            data_set_identifier = "prod_global_state_per_app"
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c262e161-9e85-427f-b144-d0a7d197a945"
            visual_ids = [
              "a5f7319f-be89-4099-b36e-bd5bd792f0c6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "ac6c1097-b008-4b1d-995b-fef2c2565d99"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "9d8894b7-755b-4c00-8e5d-034b6077db06"
          include_maximum  = false
          include_minimum  = false
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "MINUTE"

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c262e161-9e85-427f-b144-d0a7d197a945"
            visual_ids = [
              "a5f7319f-be89-4099-b36e-bd5bd792f0c6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "6dd27e39-d61b-47df-ab45-7e98f25a123f"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "45a910d1-9c43-4c03-93f9-46eb76ec0b83"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 4
          time_granularity    = "WEEK"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c262e161-9e85-427f-b144-d0a7d197a945"
            visual_ids = [
              "a5f7319f-be89-4099-b36e-bd5bd792f0c6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "aa4a19e9-0883-49d4-b87e-90891a7b6533"
      status          = "ENABLED"

      filters {
        numeric_equality_filter {
          filter_id      = "bafbe2f4-cb84-48e7-b620-46740c27f884"
          match_operator = "EQUALS"
          null_option    = "ALL_VALUES"
          value          = 1

          column {
            column_name         = "app_active"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c262e161-9e85-427f-b144-d0a7d197a945"
            visual_ids = [
              "a5f7319f-be89-4099-b36e-bd5bd792f0c6",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "d076ff6e-a0b3-48c9-a325-a0ba5f382236"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "9cee4c9d-b4e5-46bd-8bb6-f6e9e1c836cf"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 4
          time_granularity    = "WEEK"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "264ec360-194a-49f7-87e3-bc247db276f0"
            visual_ids = [
              "a3b98163-c952-434f-b005-02fa7752c291",
              "a7c939ae-2f6e-43b0-b853-8cac4374766f",
              "f4683986-1199-4076-b2d0-7693babb77ca",
              "5c8f9fef-8a98-4e11-9c18-20c3ff946b0a",
              "73c6bcda-1785-4a18-92c0-68ff3f5693c3",
              "3e698805-3207-40c6-93ff-9dd22210e5b8",
              "4135531e-7600-4810-8d1b-42f36befa634",
              "2dd07d8b-1cb9-48d0-b796-8f57ecbd5bcd",
              "7fc87a91-19c8-4122-aa03-7ba92d5742d5",
              "31f5ba4c-f73e-489e-b7c4-cdad33732a0b",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "21e02aa3-bf98-45f0-84ab-a357450481da"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "cb138c93-bfa2-4dcf-b04b-e4a4384d725e"

          column {
            column_name         = "app_name"
            data_set_identifier = "prod_global_state_per_app"
          }

          configuration {
            filter_list_configuration {
              match_operator  = "CONTAINS"
              category_values = ["${var.first_app_name_select}", ]
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "264ec360-194a-49f7-87e3-bc247db276f0"
            visual_ids = [
              "a3b98163-c952-434f-b005-02fa7752c291",
              "a7c939ae-2f6e-43b0-b853-8cac4374766f",
              "f4683986-1199-4076-b2d0-7693babb77ca",
              "5c8f9fef-8a98-4e11-9c18-20c3ff946b0a",
              "73c6bcda-1785-4a18-92c0-68ff3f5693c3",
              "3e698805-3207-40c6-93ff-9dd22210e5b8",
              "4135531e-7600-4810-8d1b-42f36befa634",
              "2dd07d8b-1cb9-48d0-b796-8f57ecbd5bcd",
              "7fc87a91-19c8-4122-aa03-7ba92d5742d5",
              "31f5ba4c-f73e-489e-b7c4-cdad33732a0b",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "20de0506-8d87-49d9-af21-48729cf5bfff"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "96309e8e-cee0-41b1-abba-89ee4beddbd0"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 1
          time_granularity    = "WEEK"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "264ec360-194a-49f7-87e3-bc247db276f0"
            visual_ids = [
              "2dd07d8b-1cb9-48d0-b796-8f57ecbd5bcd",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "e9f7c0be-15d6-477d-8908-e94b1980f0a1"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "0749175e-a584-414b-bc2c-0936863bdea3"

          column {
            column_name         = "app_name"
            data_set_identifier = "prod_global_state_per_app"
          }

          configuration {
            filter_list_configuration {
              match_operator  = "DOES_NOT_CONTAIN"
              category_values = ["BB2_TEST_APP", ]
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"
            visual_ids = [
              "96b056e7-1d03-4b7f-8bfe-be1746e29b67",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "b192ec08-9d4d-4ba9-b56b-f2b5eff3363e"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "c37665a4-0632-446f-81e4-f89c56d1cf62"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 1
          time_granularity    = "WEEK"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"
            visual_ids = [
              "96b056e7-1d03-4b7f-8bfe-be1746e29b67",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "934b62ec-7c49-4594-8bd4-65b818aaccb6"
      status          = "ENABLED"

      filters {
        numeric_equality_filter {
          filter_id      = "ddcb7121-71df-42a9-829a-c40dddd0c225"
          match_operator = "EQUALS"
          null_option    = "ALL_VALUES"
          value          = 1

          aggregation_function {
            numerical_aggregation_function {
              simple_numerical_aggregation = "SUM"
            }
          }

          column {
            column_name         = "app_active"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"
            visual_ids = [
              "96b056e7-1d03-4b7f-8bfe-be1746e29b67",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "311f74be-9510-40bf-a169-39a9b21e7e97"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "3139075a-a9f3-4f44-a373-46d27a7af734"

          column {
            column_name         = "app_name"
            data_set_identifier = "prod_global_state_per_app"
          }

          configuration {
            filter_list_configuration {
              match_operator     = "CONTAINS"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c75cb161-472f-4a1d-bb20-664110b0761f"
            visual_ids = [
              "df9821cf-58d4-4443-9a7c-9094797f0899",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "7756128f-f3c3-4817-978e-2cdf42d577e0"
      status          = "ENABLED"

      filters {
        time_range_filter {
          filter_id        = "f4ec105f-56fb-4d64-9f37-f570f10233c9"
          include_maximum  = false
          include_minimum  = false
          null_option      = "NON_NULLS_ONLY"
          time_granularity = "MINUTE"

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c75cb161-472f-4a1d-bb20-664110b0761f"
            visual_ids = [
              "df9821cf-58d4-4443-9a7c-9094797f0899",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "cf690244-a7eb-4eb7-97dd-d2dc8e5d9e71"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "03315562-d5fa-4152-8123-3be38df16aae"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 4
          time_granularity    = "WEEK"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c75cb161-472f-4a1d-bb20-664110b0761f"
            visual_ids = [
              "df9821cf-58d4-4443-9a7c-9094797f0899",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "f7bf614d-bb52-4009-89cc-a6af9c08a4d5"
      status          = "ENABLED"

      filters {
        numeric_equality_filter {
          filter_id      = "84333753-c1e2-4668-94bc-37ff1726ac71"
          match_operator = "EQUALS"
          null_option    = "ALL_VALUES"
          value          = 1

          column {
            column_name         = "app_active"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "c75cb161-472f-4a1d-bb20-664110b0761f"
            visual_ids = [
              "df9821cf-58d4-4443-9a7c-9094797f0899",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "3d58056e-245e-4a83-81c3-ca24bb097fe3"
      status          = "ENABLED"

      filters {
        numeric_equality_filter {
          filter_id          = "cbc81c13-26d5-4ce3-8dca-a42974efb0d3"
          match_operator     = "EQUALS"
          null_option        = "NON_NULLS_ONLY"
          select_all_options = "FILTER_ALL_VALUES"
          value              = 0

          column {
            column_name         = "app_access_grant_enabled"
            data_set_identifier = "prod_global_state_per_app"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"
            visual_ids = [
              "96b056e7-1d03-4b7f-8bfe-be1746e29b67",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "37fcbe3e-b7df-41f4-becb-49503ce8bf6f"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id      = "d3271572-ce23-4214-a871-1fe37a050258"

          column {
            column_name         = "calc_app_access_grant_category"
            data_set_identifier = "prod_global_state_per_app"
          }

          configuration {
            custom_filter_list_configuration {
              match_operator     = "CONTAINS"
              null_option        = "NON_NULLS_ONLY"
              select_all_options = "FILTER_ALL_VALUES"
            }
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope    = "SELECTED_VISUALS"
            sheet_id = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"
            visual_ids = [
              "96b056e7-1d03-4b7f-8bfe-be1746e29b67",
            ]
          }
        }
      }
    }
    parameter_declarations {
      string_parameter_declaration {
        name                 = "DYNAMICFIELD"
        parameter_value_type = "SINGLE_VALUED"

        default_values {
          static_values = [
            "APP Beneficiaries Served (all-time)",
          ]
        }

        values_when_unset {
          value_when_unset_option = "NULL"
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "PROD Applications"
      sheet_id     = "c262e161-9e85-427f-b144-d0a7d197a945"

      filter_controls {
        relative_date_time {
          filter_control_id = "cdd0a93f-b946-4f9e-b314-ce1c87b4ba7a"
          source_filter_id  = "45a910d1-9c43-4c03-93f9-46eb76ec0b83"
          title             = "Date Range"

          display_options {
            date_time_format = "YYYY/MM/DD HH:mm:ss"

            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "30efa582-15c3-41ad-a2cd-f7f14d6ed213"
          source_filter_id  = "3f24c4d9-c6d8-49fe-8df1-aa7250403d39"
          title             = "Select Applications"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "9d52ec6a-c62d-446b-80a0-d62412401638"
          source_filter_id  = "bafbe2f4-cb84-48e7-b620-46740c27f884"
          title             = "Select Enabled (active: 1=True/0=False)"
          type              = "SINGLE_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 35
              element_id   = "a5f7319f-be89-4099-b36e-bd5bd792f0c6"
              element_type = "VISUAL"
              row_index    = "3"
              row_span     = 18
            }
          }
        }
      }

      parameter_controls {
        dropdown {
          parameter_control_id  = "47ad7424-ad42-44f4-a346-6c2ee050ab89"
          source_parameter_name = "DYNAMICFIELD"
          title                 = "Select Metric"
          type                  = "SINGLE_SELECT"

          display_options {
            select_all_options {
              visibility = "HIDDEN"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }

          selectable_values {
            values = [
              "APP Enrollees Served (all-time)",
              "APP Enrollees Served WoW (gains this week)",
              "APP Is Enabled?",
              "APP Is Grant Enabled?",
              "APP Requires Demographic Scopes",
              "APP Synth Enrollees Served",
              "AUTH-FLOW Approval View GET FAIL",
              "AUTH-FLOW Approval View GET OK Real",
              "AUTH-FLOW Approval View GET OK Synthetic",
              "AUTH-FLOW Approval View POST FAIL",
              "AUTH-FLOW Approval View POST OK Real",
              "AUTH-FLOW Approval View POST OK Synthetic",
              "AUTH-FLOW Auth Matched New Enrollee Real",
              "AUTH-FLOW Auth Matched New Enrollee Synthetic",
              "AUTH-FLOW Auth Matched Returning Enrollee Real",
              "AUTH-FLOW Auth Matched Returning Enrollee Synthetic",
              "AUTH-FLOW Auth Start FAIL",
              "AUTH-FLOW Auth Start OK",
              "AUTH-FLOW Authorize Initial",
              "AUTH-FLOW Medicare Login Redirect FAIL",
              "AUTH-FLOW Medicare Login Redirect OK",
              "AUTH-FLOW SLS CallBack FAIL",
              "AUTH-FLOW SLS CallBack OK Real",
              "AUTH-FLOW SLS CallBack OK Synthetic",
              "Auth App Not Want Demographic Enrollee Not Share Real",
              "Auth App Not want Demographic Enrollee Deny Real",
              "Auth App Wants Demographic Enrollee Deny Real",
              "Auth App Wants Demographic Enrollee Not Share Real",
              "Auth App Wants Demographic Enrollee Share Real",
              "Auth Status Deny Real Enrollee",
              "Auth Status OK Real Enrollee",
              "FHIR Coverage Requests",
              "FHIR Coverage With Since Requests",
              "FHIR EOB Requests",
              "FHIR EOB With Since Requests",
              "FHIR Patient Requests",
              "FHIR Total Requests",
              "FHIR V1 Coverage Requests",
              "FHIR V1 Coverage With Since Requests",
              "FHIR V1 EOB Requests",
              "FHIR V1 EOB With Since Requests",
              "FHIR V1 Patient Requests",
              "FHIR V1 Total Requests",
              "FHIR V2 Coverage Requests",
              "FHIR V2 Coverage With Since Requests",
              "FHIR V2 EOB Requests",
              "FHIR V2 EOB With Since Requests",
              "FHIR V2 Patient Requests",
              "FHIR V2 Total Requests",
              "Token Auth-Code Requests 2xx",
              "Token Auth-Code Requests 4xx",
              "Token Auth-Code Requests 5xx",
              "Token Auth-Code Requests Real Enrollee",
              "Token Auth-Code Requests Synth Enrollee",
              "Token Refresh Requests 2xx",
              "Token Refresh Requests 4xx",
              "Token Refresh Requests 5xx",
              "Token Refresh Requests Real Enrollee",
              "Token Refresh Requests Synth Enrollee",
            ]
          }
        }
      }

      sheet_control_layouts {
        configuration {
          grid_layout {
            elements {
              column_span  = 3
              element_id   = "47ad7424-ad42-44f4-a346-6c2ee050ab89"
              element_type = "PARAMETER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "cdd0a93f-b946-4f9e-b314-ce1c87b4ba7a"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "30efa582-15c3-41ad-a2cd-f7f14d6ed213"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "9d52ec6a-c62d-446b-80a0-d62412401638"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "a5f7319f-be89-4099-b36e-bd5bd792f0c6"

          chart_configuration {
            field_options {
              data_path_options {
                width = "258px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                  field_value = "app_name"
                }
              }
              data_path_options {
                width = "151px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                  field_value = "app_user_organization"
                }
              }
              selected_field_options {
                custom_label = "Organization"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Name"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Created On"
                field_id     = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Grant Category"
                field_id      = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                field_id   = "042af5eb-617c-4742-bed1-5a3674d1f8b0.4.1697137648700"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"

                    column {
                      column_name         = "app_user_organization"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"

                    column {
                      column_name         = "app_name"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"

                    column {
                      column_name         = "calc_app_created"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"

                    column {
                      column_name         = "calc_app_access_grant_category"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "042af5eb-617c-4742-bed1-5a3674d1f8b0.4.1697137648700"

                    column {
                      column_name         = "Dynamic Field"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                  }
                }
              }
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                  }
                }
              }
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"
                  }
                }
              }
            }
            table_options {
              single_metric_visibility  = "HIDDEN"
              column_names_visibility   = "VISIBLE"
              toggle_buttons_visibility = "VISIBLE"

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                horizontal_text_alignment = "CENTER"
                vertical_text_alignment   = "TOP"
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       height = 0 -> 40
                height = 40

                border {
                  uniform_border {
                    style     = "SOLID"
                    thickness = 1
                  }
                }
              }
              cell_style {
                text_wrap = "WRAP"
                height    = 40
              }
            }
            total_options {
              column_subtotal_options {
                totals_visibility = "HIDDEN"
              }
              column_total_options {
                totals_visibility = "HIDDEN"
              }
              row_subtotal_options {
                totals_visibility = "HIDDEN"
                #total_cell_style {
                #   BUG NOTE: With AWS provider v5.29.0
                #     After TF apply, plan still shows: height = 0
                #   Also causes an apply error if including these lines.
                #}
              }
              row_total_options {
                custom_label      = "Total"
                scroll_status     = "PINNED"
                totals_visibility = "VISIBLE"

              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">PROD Apps Main</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">Use the controls to change metric or filter for date range.</inline>\n  </block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">Total can include duplicates across apps.</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "PROD Single Application"
      sheet_id     = "264ec360-194a-49f7-87e3-bc247db276f0"

      filter_controls {
        relative_date_time {
          filter_control_id = "79859066-eeb6-4f95-a3c7-6cbd19fd8414"
          source_filter_id  = "9cee4c9d-b4e5-46bd-8bb6-f6e9e1c836cf"
          title             = "Choose Date Range"

          display_options {
            date_time_format = "YYYY/MM/DD HH:mm:ss"

            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "0f55b5fc-4d48-40a0-a6aa-73e428a15b97"
          source_filter_id  = "cb138c93-bfa2-4dcf-b04b-e4a4384d725e"
          title             = "Choose Application Name"
          type              = "SINGLE_SELECT"

          display_options {
            select_all_options {
              visibility = "HIDDEN"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "2"
              column_span  = 9
              element_id   = "0f55b5fc-4d48-40a0-a6aa-73e428a15b97"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 3
            }
            elements {
              column_index = "11"
              column_span  = 8
              element_id   = "79859066-eeb6-4f95-a3c7-6cbd19fd8414"
              element_type = "FILTER_CONTROL"
              row_index    = "0"
              row_span     = 3
            }
            elements {
              column_index = "0"
              column_span  = 15
              element_id   = "2dd07d8b-1cb9-48d0-b796-8f57ecbd5bcd"
              element_type = "VISUAL"
              row_index    = "3"
              row_span     = 5
            }
            elements {
              column_index = "15"
              column_span  = 18
              element_id   = "73c6bcda-1785-4a18-92c0-68ff3f5693c3"
              element_type = "VISUAL"
              row_index    = "3"
              row_span     = 5
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "3e698805-3207-40c6-93ff-9dd22210e5b8"
              element_type = "VISUAL"
              row_index    = "8"
              row_span     = 6
            }
            elements {
              column_index = "18"
              column_span  = 18
              element_id   = "f4683986-1199-4076-b2d0-7693babb77ca"
              element_type = "VISUAL"
              row_index    = "8"
              row_span     = 6
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "31f5ba4c-f73e-489e-b7c4-cdad33732a0b"
              element_type = "VISUAL"
              row_index    = "14"
              row_span     = 6
            }
            elements {
              column_index = "18"
              column_span  = 18
              element_id   = "4135531e-7600-4810-8d1b-42f36befa634"
              element_type = "VISUAL"
              row_index    = "14"
              row_span     = 6
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "7fc87a91-19c8-4122-aa03-7ba92d5742d5"
              element_type = "VISUAL"
              row_index    = "20"
              row_span     = 6
            }
            elements {
              column_index = "18"
              column_span  = 18
              element_id   = "a3b98163-c952-434f-b005-02fa7752c291"
              element_type = "VISUAL"
              row_index    = "20"
              row_span     = 6
            }
            elements {
              column_index = "0"
              column_span  = 18
              element_id   = "5c8f9fef-8a98-4e11-9c18-20c3ff946b0a"
              element_type = "VISUAL"
              row_index    = "26"
              row_span     = 8
            }
            elements {
              column_span  = 18
              element_id   = "a7c939ae-2f6e-43b0-b853-8cac4374766f"
              element_type = "VISUAL"
              row_span     = 8
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "a7c939ae-2f6e-43b0-b853-8cac4374766f"

          chart_configuration {
            field_options {
              data_path_options {
                width = "355px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-00 Initial Authorize Link Count"
                field_id     = "4e193545-559f-4cc4-b5e1-ce3d50e8e220.6.1666640378599"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-01 Redirect to Medicare Login OK"
                field_id     = "2c7859a0-29a8-450c-bdb6-b807fcd23dac.7.1666640537715"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-02 Starts Auth After Medicare Login Count"
                field_id     = "730b19e2-bc04-4445-87fe-60de84f6bc41.8.1666640650789"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-02 Starts Auth After Medicare Login % "
                field_id     = "4b1adc08-d2f4-45f4-b2ff-f1029285e6a7.2.1666636887530"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-03 Bene Matches to BFD FHIR_ID REAL Count "
                field_id     = "a78240ce-2ddb-4645-b548-a50ac6c09a67.5.1666641068052"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-03 Bene Matches to BFD FHIR_ID REAL %"
                field_id     = "6109fa77-554b-42d3-8de2-4e0ae541b5d6.6.1666641082801"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-03 Bene Matches to BFD FHIR_ID SYNTH Count "
                field_id     = "263359ba-439c-4d3e-9505-8700e2ec868b.7.1666641230872"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-03 Bene Matches to BFD FHIR_ID SYNTH %"
                field_id     = "8d709830-e960-4567-8c37-894137305ff0.8.1666641373347"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-04 Bene Clicks ALLOW REAL Count"
                field_id     = "10f8cbbc-3e2e-4798-870b-5803f4b350c6.9.1666642446271"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "QAUTH-04 Bene Clicks ALLOW SYNTH Count"
                field_id     = "067db8e5-370a-4cf0-9be4-4a043c164b68.10.1666642506450"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                field_id   = "b547c687-20c4-4b8f-92f5-df0331fb978e.11.1666643591258"
                visibility = "VISIBLE"
              }
              selected_field_options {
                field_id   = "22db1188-61d5-447c-a868-778c5f0f9955.12.1666643696106"
                visibility = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "4e193545-559f-4cc4-b5e1-ce3d50e8e220.6.1666640378599"

                    column {
                      column_name         = "calc_app_authorize_initial_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "2c7859a0-29a8-450c-bdb6-b807fcd23dac.7.1666640537715"

                    column {
                      column_name         = "calc_app_medicare_login_redirect_ok_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "730b19e2-bc04-4445-87fe-60de84f6bc41.8.1666640650789"

                    column {
                      column_name         = "calc_app_authentication_start_ok_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "4b1adc08-d2f4-45f4-b2ff-f1029285e6a7.2.1666636887530"

                    column {
                      column_name         = "calc_app_diff_auth_start_vs_initial_percent"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "a78240ce-2ddb-4645-b548-a50ac6c09a67.5.1666641068052"

                    column {
                      column_name         = "calc_app_sls_callback_ok_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "6109fa77-554b-42d3-8de2-4e0ae541b5d6.6.1666641082801"

                    column {
                      column_name         = "calc_app_diff_auth_sls_callback_ok_vs_start_real_percent"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "263359ba-439c-4d3e-9505-8700e2ec868b.7.1666641230872"

                    column {
                      column_name         = "calc_app_sls_callback_ok_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8d709830-e960-4567-8c37-894137305ff0.8.1666641373347"

                    column {
                      column_name         = "calc_app_diff_auth_sls_callback_ok_vs_start_synthetic_percent"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "10f8cbbc-3e2e-4798-870b-5803f4b350c6.9.1666642446271"

                    column {
                      column_name         = "calc_app_auth_ok_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "067db8e5-370a-4cf0-9be4-4a043c164b68.10.1666642506450"

                    column {
                      column_name         = "calc_app_auth_ok_synthetic_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "b547c687-20c4-4b8f-92f5-df0331fb978e.11.1666643591258"

                    column {
                      column_name         = "calc_app_auth_fail_or_deny_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "22db1188-61d5-447c-a868-778c5f0f9955.12.1666643696106"

                    column {
                      column_name         = "calc_app_auth_fail_or_deny_synthetic_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "LEFT"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">AUTH-FLOW Questions</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For both real/synth.</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "5c8f9fef-8a98-4e11-9c18-20c3ff946b0a"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Authorize Initial"
                field_id     = "4e193545-559f-4cc4-b5e1-ce3d50e8e220.1.1666276945830"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Medicare Login Redirect OK"
                field_id     = "2c7859a0-29a8-450c-bdb6-b807fcd23dac.2.1666276978720"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Medicare Login Redirect FAIL"
                field_id     = "70db5a18-e4bc-46d1-bb1c-b8d31a29824d.3.1666276980893"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Start OK"
                field_id     = "730b19e2-bc04-4445-87fe-60de84f6bc41.4.1666277003052"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Start FAIL"
                field_id     = "747d3e80-a95f-40ef-898a-05acc0d2eba1.5.1666277004628"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Matched New Bene Real"
                field_id     = "ab1c111d-4705-47f5-8fae-aeb440d7fbb9.6.1666277035168"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Matched Returning Bene Real"
                field_id     = "945cd621-935f-4770-96ab-fada8075dfb6.7.1666277036797"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Matched New Bene Synthetic"
                field_id     = "b4bb4e31-5ff2-481b-92ad-00aabdbb173e.8.1666277038894"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Auth Matched Returning Bene Synthetic"
                field_id     = "0a6fb8d3-cf01-48c5-9caa-e88aefeb68cc.9.1666277040410"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW SLS CallBack OK Real"
                field_id     = "a78240ce-2ddb-4645-b548-a50ac6c09a67.10.1666277080489"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW SLS CallBack OK Synthetic"
                field_id     = "263359ba-439c-4d3e-9505-8700e2ec868b.11.1666277082155"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW SLS CallBack FAIL"
                field_id     = "a774a7e5-582b-48b2-af62-c2f40fb10304.12.1666277083979"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View GET OK Real"
                field_id     = "e2ea91ce-6f6b-4e1d-8bba-9fbc1312cb43.13.1666277109367"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View GET OK Synthetic"
                field_id     = "ac476b63-4288-44e0-b67b-15d3c429827a.14.1666277112028"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View GET FAIL"
                field_id     = "bab8a97c-7006-40e9-965f-bcbc043d19c0.15.1666277114212"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View POST OK Real"
                field_id     = "cbaa8b9c-8804-4177-96a0-8f18f87c436c.16.1666277120962"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View POST OK Synthetic"
                field_id     = "68f0ce04-a763-4938-9f8a-78592132ff6a.17.1666277124157"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AUTH-FLOW Approval View POST FAIL"
                field_id     = "de006f60-6028-4e2a-837f-9aeeb593d1f5.18.1666277126073"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests Real Bene"
                field_id     = "7cf376a4-063b-4418-8f65-e91d85b4111e.19.1666277608198"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests Synth Bene"
                field_id     = "61d0594b-45be-485a-ad06-19d1145f484f.20.1666277609552"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "4e193545-559f-4cc4-b5e1-ce3d50e8e220.1.1666276945830"

                    column {
                      column_name         = "calc_app_authorize_initial_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "2c7859a0-29a8-450c-bdb6-b807fcd23dac.2.1666276978720"

                    column {
                      column_name         = "calc_app_medicare_login_redirect_ok_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "70db5a18-e4bc-46d1-bb1c-b8d31a29824d.3.1666276980893"

                    column {
                      column_name         = "calc_app_medicare_login_redirect_fail_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "730b19e2-bc04-4445-87fe-60de84f6bc41.4.1666277003052"

                    column {
                      column_name         = "calc_app_authentication_start_ok_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "747d3e80-a95f-40ef-898a-05acc0d2eba1.5.1666277004628"

                    column {
                      column_name         = "calc_app_authentication_start_fail_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ab1c111d-4705-47f5-8fae-aeb440d7fbb9.6.1666277035168"

                    column {
                      column_name         = "calc_app_authentication_matched_new_bene_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "945cd621-935f-4770-96ab-fada8075dfb6.7.1666277036797"

                    column {
                      column_name         = "calc_app_authentication_matched_returning_bene_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "b4bb4e31-5ff2-481b-92ad-00aabdbb173e.8.1666277038894"

                    column {
                      column_name         = "calc_app_authentication_matched_new_bene_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "0a6fb8d3-cf01-48c5-9caa-e88aefeb68cc.9.1666277040410"

                    column {
                      column_name         = "calc_app_authentication_matched_returning_bene_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "a78240ce-2ddb-4645-b548-a50ac6c09a67.10.1666277080489"

                    column {
                      column_name         = "calc_app_sls_callback_ok_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "263359ba-439c-4d3e-9505-8700e2ec868b.11.1666277082155"

                    column {
                      column_name         = "calc_app_sls_callback_ok_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "a774a7e5-582b-48b2-af62-c2f40fb10304.12.1666277083979"

                    column {
                      column_name         = "calc_app_sls_callback_fail_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "e2ea91ce-6f6b-4e1d-8bba-9fbc1312cb43.13.1666277109367"

                    column {
                      column_name         = "calc_app_approval_view_get_ok_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ac476b63-4288-44e0-b67b-15d3c429827a.14.1666277112028"

                    column {
                      column_name         = "calc_app_approval_view_get_ok_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "bab8a97c-7006-40e9-965f-bcbc043d19c0.15.1666277114212"

                    column {
                      column_name         = "calc_app_approval_view_get_fail_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "cbaa8b9c-8804-4177-96a0-8f18f87c436c.16.1666277120962"

                    column {
                      column_name         = "calc_app_approval_view_post_ok_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "68f0ce04-a763-4938-9f8a-78592132ff6a.17.1666277124157"

                    column {
                      column_name         = "calc_app_approval_view_post_ok_synthetic_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "de006f60-6028-4e2a-837f-9aeeb593d1f5.18.1666277126073"

                    column {
                      column_name         = "calc_app_approval_view_post_fail_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "7cf376a4-063b-4418-8f65-e91d85b4111e.19.1666277608198"

                    column {
                      column_name         = "calc_app_token_authorization_code_for_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "61d0594b-45be-485a-ad06-19d1145f484f.20.1666277609552"

                    column {
                      column_name         = "calc_app_token_authorization_code_for_synthetic_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">AUTH-FLOW Tracking</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For both real/synth.</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "a3b98163-c952-434f-b005-02fa7752c291"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Refresh Requests Real Bene"
                field_id     = "ace23bcc-487f-48f7-b15d-83be50246f86.1.1665091437501"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Refresh Requests Synth Bene"
                field_id     = "ed543edd-7ff2-4536-9d0a-cfc5bcdb1b4b.2.1665091440377"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Refresh Requests 2xx"
                field_id     = "d68487e6-13c0-4f22-a860-0937c6954464.3.1665091447023"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Refresh Requests 4xx"
                field_id     = "6fd220a7-f6c4-4c01-a618-98591a54d190.4.1665091448965"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Refresh Requests 5xx"
                field_id     = "4c63e831-087d-42b5-832f-971ca14fbd11.5.1665091450112"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ace23bcc-487f-48f7-b15d-83be50246f86.1.1665091437501"

                    column {
                      column_name         = "calc_app_token_refresh_for_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ed543edd-7ff2-4536-9d0a-cfc5bcdb1b4b.2.1665091440377"

                    column {
                      column_name         = "calc_app_token_refresh_for_synthetic_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "d68487e6-13c0-4f22-a860-0937c6954464.3.1665091447023"

                    column {
                      column_name         = "calc_app_token_refresh_response_2xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "6fd220a7-f6c4-4c01-a618-98591a54d190.4.1665091448965"

                    column {
                      column_name         = "calc_app_token_refresh_response_4xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "4c63e831-087d-42b5-832f-971ca14fbd11.5.1665091450112"

                    column {
                      column_name         = "calc_app_token_refresh_response_5xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">Token Refresh API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For both real/synth.</inline>\n  </block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">Step in Auth where RefreshToken is used.</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "7fc87a91-19c8-4122-aa03-7ba92d5742d5"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests Real Bene"
                field_id     = "7cf376a4-063b-4418-8f65-e91d85b4111e.1.1665089819331"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests Synth Bene"
                field_id     = "61d0594b-45be-485a-ad06-19d1145f484f.2.1665089827505"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests 2xx"
                field_id     = "397385cb-ae3c-47e0-8d9c-01becc50fb5f.3.1665089830887"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests 4xx"
                field_id     = "95932cd4-7243-46bc-a4f6-aaf555208ae8.4.1665089832135"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Token Auth-Code Requests 5xx"
                field_id     = "ef38e870-48e6-4365-bc8c-23bdaea2bb5a.5.1665089833463"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "7cf376a4-063b-4418-8f65-e91d85b4111e.1.1665089819331"

                    column {
                      column_name         = "calc_app_token_authorization_code_for_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "61d0594b-45be-485a-ad06-19d1145f484f.2.1665089827505"

                    column {
                      column_name         = "calc_app_token_authorization_code_for_synthetic_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "397385cb-ae3c-47e0-8d9c-01becc50fb5f.3.1665089830887"

                    column {
                      column_name         = "calc_app_token_authorization_code_2xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "95932cd4-7243-46bc-a4f6-aaf555208ae8.4.1665089832135"

                    column {
                      column_name         = "calc_app_token_authorization_code_4xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ef38e870-48e6-4365-bc8c-23bdaea2bb5a.5.1665089833463"

                    column {
                      column_name         = "calc_app_token_authorization_code_5xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">Token Authorization Code API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For both real/synth.</inline>\n  </block>\n  <br/>\n  <block align=\"center\">Step in auth where 10-hour AccessToken is created on success.</block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "31f5ba4c-f73e-489e-b7c4-cdad33732a0b"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "V1 Combined"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_call_real_count.2.1664220151450"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_eob_call_real_count.3.1664220178642"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_coverage_call_real_count.4.1664220186621"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Patient"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_patient_call_real_count.5.1664220190474"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB w/ Since Parameter"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_eob_since_call_real_count.5.1664220733401"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage w/ Since Parameter"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_coverage_since_call_real_count.6.1664220741041"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_call_real_count.2.1664220151450"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_eob_call_real_count.3.1664220178642"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_eob_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_coverage_call_real_count.4.1664220186621"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_coverage_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_patient_call_real_count.5.1664220190474"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_patient_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_eob_since_call_real_count.5.1664220733401"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_eob_since_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v1_coverage_since_call_real_count.6.1664220741041"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v1_coverage_since_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">V1 FHIR Total API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For REAL patient_id &gt;=0</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "f4683986-1199-4076-b2d0-7693babb77ca"

          chart_configuration {
            field_options {
              data_path_options {
                width = "316px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "STATUS = OK"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_ok_real_bene_count.2.1664224349057"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "STATUS = FAIL/DENY"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_fail_or_deny_real_bene_count.2.1664224376289"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Wants & Bene Shares Demographicphic"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_sharing_real_bene_count.3.1664224561646"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Wants & Bene NOT Sharing Demographic"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_not_sharing_real_bene_count.4.1664224570059"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Wants Demographic & Bene DENY"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_deny_real_bene_count.5.1664224625209"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Not Needing Demographic & Auto Not shared"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_not_required_not_sharing_real_bene_count.7.1664224660931"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Not Needing Demographic & Bene DENY"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_not_required_deny_real_bene_count.6.1664224656127"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_ok_real_bene_count.2.1664224349057"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_ok_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_fail_or_deny_real_bene_count.2.1664224376289"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_fail_or_deny_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_sharing_real_bene_count.3.1664224561646"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_demoscope_required_choice_sharing_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_not_sharing_real_bene_count.4.1664224570059"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_demoscope_required_choice_not_sharing_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_required_choice_deny_real_bene_count.5.1664224625209"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_demoscope_required_choice_deny_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_not_required_not_sharing_real_bene_count.7.1664224660931"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_demoscope_not_required_not_sharing_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_auth_demoscope_not_required_deny_real_bene_count.6.1664224656127"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_auth_demoscope_not_required_deny_real_bene_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">Auth Flow Requests</block>\n  <br/>\n  <block align=\"center\"/>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "4135531e-7600-4810-8d1b-42f36befa634"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "V2 Combined"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_call_real_count.2.1664220539176"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_eob_call_real_count.2.1664220557917"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_coverage_call_real_count.3.1664220562817"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Patient"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_patient_call_real_count.4.1664220569350"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB w/  Since Parameter"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_eob_since_call_real_count.5.1664220911812"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage w/  Since Parameter"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_coverage_since_call_real_count.6.1664220919707"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_call_real_count.2.1664220539176"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_eob_call_real_count.2.1664220557917"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_eob_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_coverage_call_real_count.3.1664220562817"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_coverage_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_patient_call_real_count.4.1664220569350"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_patient_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_eob_since_call_real_count.5.1664220911812"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_eob_since_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v2_coverage_since_call_real_count.6.1664220919707"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v2_coverage_since_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">V2 FHIR Total API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For REAL patient_id &gt;=0</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "31f5ba4c-f73e-489e-b7c4-cdad33732a0b"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "V1 Combined"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_call_real_count.2.1770152434768"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_eob_call_real_count.3.1770152443463"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_coverage_call_real_count.4.1664220186621"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Patient"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_patient_call_real_count.5.1770152448316"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_call_real_count.2.1770152434768"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v3_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_eob_call_real_count.3.1770152443463"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v3_eob_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_coverage_call_real_count.4.1770152446959"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v3_coverage_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_patient_call_real_count.5.1770152448316"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v3_patient_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_fhir_v3_generate_insurance_card_call_real_count.5.1772212977956"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_fhir_v3_generate_insurance_card_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">V3 FHIR Total API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For REAL patient_id &gt;=0</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "3e698805-3207-40c6-93ff-9dd22210e5b8"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "V1 and V2 Combined"
                field_id     = "8bd16d1f-7d26-47f5-882e-4f103bf9a123.2.1664226716143"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB"
                field_id     = "dd08a813-f3e7-45d2-8101-20bcc856ab15.2.1664889633884"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage"
                field_id     = "e4ed9c1f-39dc-4031-8f49-d30befeff455.6.1664889697029"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Patient"
                field_id     = "a307dd28-1a3b-43b2-a952-e11b01c05d68.4.1664889688417"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "EOB w/ Since Parameter"
                field_id     = "61d21842-f29b-4ffb-8f76-75060cc21c3a.5.1664889695472"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Coverage w/ Since Parameter"
                field_id     = "23590bc5-04b6-498b-9f19-d9414a2b884d.3.1664889672327"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8bd16d1f-7d26-47f5-882e-4f103bf9a123.2.1664226716143"

                    column {
                      column_name         = "calc_app_fhir_call_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "dd08a813-f3e7-45d2-8101-20bcc856ab15.2.1664889633884"

                    column {
                      column_name         = "calc_app_fhir_eob_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "e4ed9c1f-39dc-4031-8f49-d30befeff455.6.1664889697029"

                    column {
                      column_name         = "calc_app_fhir_coverage_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "a307dd28-1a3b-43b2-a952-e11b01c05d68.4.1664889688417"

                    column {
                      column_name         = "calc_app_fhir_patient_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "61d21842-f29b-4ffb-8f76-75060cc21c3a.5.1664889695472"

                    column {
                      column_name         = "calc_app_fhir_eob_since_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "23590bc5-04b6-498b-9f19-d9414a2b884d.3.1664889672327"

                    column {
                      column_name         = "calc_app_fhir_coverage_since_real_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">V1 &amp; V2 FHIR Total API Requests</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">For REAL patient_id &gt;=0</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        table_visual {
          visual_id = "2dd07d8b-1cb9-48d0-b796-8f57ecbd5bcd"

          chart_configuration {
            field_options {
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                width        = "164px"
              }
              selected_field_options {
                custom_label = "Application Name"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.4.1663878023563"
              }
              selected_field_options {
                custom_label = "Organization"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.5.1663878830468"
              }
              selected_field_options {
                custom_label = "Created"
                field_id     = "584bdf55-45e0-48fa-a60b-99db83f3bc16.5.1663878464008"
              }
              selected_field_options {
                custom_label = "First Active"
                field_id     = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663878584654"
              }
              selected_field_options {
                custom_label = "Last Active"
                field_id     = "89ccbbc4-cdf4-45d9-88ef-a70341c05ba8.5.1663878616924"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.4.1663878023563"

                    column {
                      column_name         = "app_name"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.5.1663878830468"

                    column {
                      column_name         = "app_user_organization"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "584bdf55-45e0-48fa-a60b-99db83f3bc16.5.1663878464008"

                    column {
                      column_name         = "calc_app_created"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663878584654"

                    column {
                      column_name         = "calc_app_first_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "89ccbbc4-cdf4-45d9-88ef-a70341c05ba8.5.1663878616924"

                    column {
                      column_name         = "calc_app_last_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
            }
            table_options {
              orientation = "HORIZONTAL"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              header_style {
                height                    = 40
                horizontal_text_alignment = "CENTER"
                text_wrap                 = "WRAP"
                vertical_text_alignment   = "MIDDLE"
                visibility                = "VISIBLE"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">Application Record Information</block>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "73c6bcda-1785-4a18-92c0-68ff3f5693c3"

          chart_configuration {
            field_options {
              data_path_options {
                width = "286px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Is Active (1=Yes/0=No)?"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_active.4.1663877673684"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Beneficiaries Served"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_real_bene_deduped_count.1.1663876069401"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Synth Benes Served"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_synthetic_bene_deduped_count.2.1663876072194"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Requires Demographic Scopes  (1=Yes/0=No)?"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_require_demographic_scopes.0.1663876053734"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_active.4.1663877673684"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_real_bene_deduped_count.1.1663876069401"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_grant_and_archived_real_bene_deduped_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_synthetic_bene_deduped_count.2.1663876072194"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_grant_and_archived_synthetic_bene_deduped_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_require_demographic_scopes.0.1663876053734"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_require_demographic_scopes"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.3.1663876105758"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
              }
            }
          }

          subtitle {
            visibility = "HIDDEN"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">Application Stats</block>\n</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "PROD Applications Enabled/Disabled"
      sheet_id     = "8945dc75-5874-45e9-bc0f-3e5a9db0a62d"

      filter_controls {
        list {
          filter_control_id = "8a2feb46-9bfe-4725-a4ef-16fada3212d1"
          source_filter_id  = "ddcb7121-71df-42a9-829a-c40dddd0c225"
          title             = "Filter By Activated Application (1=Yes/0=No)"
          type              = "SINGLE_SELECT"

          display_options {
            search_options {
              visibility = "HIDDEN"
            }
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      filter_controls {
        dropdown {
          filter_control_id = "21027e4e-eef3-43c4-8c15-95d1d91fa51d"
          source_filter_id  = "cbc81c13-26d5-4ce3-8dca-a42974efb0d3"
          title             = "Filter by Access Grant Enabled (1=Yes/0=No)"
          type              = "SINGLE_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "005d7e89-e99d-4c59-b3d3-09980527345b"
          source_filter_id  = "d3271572-ce23-4214-a871-1fe37a050258"
          title             = "Filter by Access Grant Category"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }

          selectable_values {
            values = [
              "ONE_TIME",
              "RESEARCH_S",
              "THIRTEEN_M",
            ]
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_span  = 35
              element_id   = "96b056e7-1d03-4b7f-8bfe-be1746e29b67"
              element_type = "VISUAL"
              row_span     = 18
            }
          }
        }
      }

      sheet_control_layouts {
        configuration {
          grid_layout {
            elements {
              column_span  = 2
              element_id   = "8a2feb46-9bfe-4725-a4ef-16fada3212d1"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "21027e4e-eef3-43c4-8c15-95d1d91fa51d"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "005d7e89-e99d-4c59-b3d3-09980527345b"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
          }
        }
      }

      visuals {
        table_visual {
          visual_id = "96b056e7-1d03-4b7f-8bfe-be1746e29b67"

          chart_configuration {
            field_options {
              selected_field_options {
                custom_label = "Organization"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
              }
              selected_field_options {
                custom_label = "App Name"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
              }
              selected_field_options {
                custom_label = "Created On"
                field_id     = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"
              }
              selected_field_options {
                custom_label = "First Active"
                field_id     = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663854772668"
              }
              selected_field_options {
                custom_label = "Last Active"
                field_id     = "89ccbbc4-cdf4-45d9-88ef-a70341c05ba8.6.1664288640814"
              }
              selected_field_options {
                custom_label = "Grant Category"
                field_id = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"
              }
              selected_field_options {
                custom_label = "Access Grant Enabled?"
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_access_grant_enabled.7.1724857533205"
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"

                    column {
                      column_name         = "app_user_organization"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"

                    column {
                      column_name         = "app_name"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"

                    column {
                      column_name         = "calc_app_created"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663854772668"

                    column {
                      column_name         = "calc_app_first_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "89ccbbc4-cdf4-45d9-88ef-a70341c05ba8.6.1664288640814"

                    column {
                      column_name         = "calc_app_last_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  categorical_dimension_field {
                    field_id = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"

                    column {
                      column_name         = "calc_app_access_grant_category"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  numerical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_access_grant_enabled.7.1724857533205"

                    column {
                      column_name         = "app_access_grant_enabled"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            table_options {
              cell_style {
                text_wrap = "WRAP"
                height    = 40
              }
            }
          }

          conditional_formatting {
            conditional_formatting_options {
              cell {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_access_grant_enabled.7.1724857533205"
                text_format {
                  icon {
                    custom_condition {
                      color      = "#219FD7"
                      expression = "{app_access_grant_enabled} = 1"

                      display_configuration {
                        icon_display_option = "ICON_ONLY"
                      }

                      icon_options {
                        icon = "CHECKMARK"
                      }
                    }
                  }
                  text_color {}
                  background_color { }
                }
              }
            }
            conditional_formatting_options {
              cell {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_access_grant_enabled.7.1724857533205"
                text_format {
                  icon {
                    custom_condition {
                      color      = "#DE3B00"
                      expression = "{app_access_grant_enabled} = 0"

                      display_configuration {
                        icon_display_option = "ICON_ONLY"
                      }

                      icon_options {
                        icon = "X"
                      }
                    }
                  }
                  text_color {}
                  background_color {}
                }
              }
            }
          }
          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>PROD Apps Enabled/Disabled Current Report Week</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "PROD Applications Token Activity"
      sheet_id     = "c75cb161-472f-4a1d-bb20-664110b0761f"

      filter_controls {
        relative_date_time {
          filter_control_id = "947e69a1-c6ff-44cc-90ba-b0dbb09819a5"
          source_filter_id  = "03315562-d5fa-4152-8123-3be38df16aae"
          title             = "Date Range"

          display_options {
            date_time_format = "YYYY/MM/DD HH:mm:ss"

            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "0b745871-3814-43e8-9b00-82e1173fb617"
          source_filter_id  = "3139075a-a9f3-4f44-a373-46d27a7af734"
          title             = "Select Applications"
          type              = "MULTI_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }
      filter_controls {
        dropdown {
          filter_control_id = "040a63f6-99c8-409d-8aeb-17e6ee8cb3f2"
          source_filter_id  = "84333753-c1e2-4668-94bc-37ff1726ac71"
          title             = "Select Enabled (active: 1=True/0=False)"
          type              = "SINGLE_SELECT"

          display_options {
            select_all_options {
              visibility = "VISIBLE"
            }
            title_options {
              visibility = "VISIBLE"

              font_configuration {
                font_size {
                  relative = "MEDIUM"
                }
              }
            }
          }
        }
      }

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "1"
              column_span  = 35
              element_id   = "df9821cf-58d4-4443-9a7c-9094797f0899"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 18
            }
          }
        }
      }

      sheet_control_layouts {
        configuration {
          grid_layout {
            elements {
              column_span  = 2
              element_id   = "947e69a1-c6ff-44cc-90ba-b0dbb09819a5"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "0b745871-3814-43e8-9b00-82e1173fb617"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
            elements {
              column_span  = 2
              element_id   = "040a63f6-99c8-409d-8aeb-17e6ee8cb3f2"
              element_type = "FILTER_CONTROL"
              row_span     = 1
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "df9821cf-58d4-4443-9a7c-9094797f0899"

          chart_configuration {
            field_options {
              data_path_options {
                width = "258px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                  field_value = "app_name"
                }
              }
              data_path_options {
                width = "151px"

                data_path_list {
                  field_id    = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                  field_value = "app_user_organization"
                }
              }
              selected_field_options {
                custom_label = "Organization"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "App Name"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Created On"
                field_id     = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "First Active"
                field_id     = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663854772668"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Grant Category"
                field_id = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "AccessToken"
                field_id     = "397385cb-ae3c-47e0-8d9c-01becc50fb5f.5.1666894083754"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "RefreshToken"
                field_id     = "d68487e6-13c0-4f22-a860-0937c6954464.6.1666894131148"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"

                    column {
                      column_name         = "app_user_organization"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"

                    column {
                      column_name         = "app_name"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "584bdf55-45e0-48fa-a60b-99db83f3bc16.4.1663854685543"

                    column {
                      column_name         = "calc_app_created"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "214a59ab-d317-4659-9b52-fc62f0462f03.5.1663854772668"

                    column {
                      column_name         = "calc_app_first_active"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "8783cd59-0891-49ff-b328-3a38b1855bb6.0.1709327453902"

                    column {
                      column_name         = "calc_app_access_grant_category"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "397385cb-ae3c-47e0-8d9c-01becc50fb5f.5.1666894083754"

                    column {
                      column_name         = "calc_app_token_authorization_code_2xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "d68487e6-13c0-4f22-a860-0937c6954464.6.1666894131148"

                    column {
                      column_name         = "calc_app_token_refresh_response_2xx_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.1.1656511700288"
                  }
                }
              }
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_name.2.1656511719055"
                  }
                }
              }
              field_sort_options {
                field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.0.1656511666014"
                  }
                }
              }
            }
            table_options {
              column_names_visibility   = "VISIBLE"
              single_metric_visibility  = "HIDDEN"
              toggle_buttons_visibility = "VISIBLE"

              cell_style {
                text_wrap = "WRAP"
                height    = 40
              }

              column_header_style {
                text_wrap = "WRAP"
                height    = 40
              }

              row_header_style {
                # BUG NOTE: With AWS provider v5.29.0
                #   After a TF apply,
                #     plan will show:
                #       text_wrap -> "WRAP"
                #       height = 0 -> 40
                text_wrap                 = "WRAP"
                height                    = 40
                horizontal_text_alignment = "CENTER"
                vertical_text_alignment   = "TOP"

                border {
                  uniform_border {
                    style     = "SOLID"
                    thickness = 1
                  }
                }
              }
            }
            total_options {
              column_subtotal_options {
                totals_visibility = "HIDDEN"
              }
              column_total_options {
                totals_visibility = "HIDDEN"
              }
              row_subtotal_options {
                totals_visibility = "HIDDEN"
              }
              row_total_options {
                custom_label      = "Total"
                scroll_status     = "PINNED"
                totals_visibility = "VISIBLE"
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">PROD Apps Main</block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">Use the controls to change metric or filter for date range.</inline>\n  </block>\n  <br/>\n  <block align=\"center\">\n    <inline font-size=\"12px\">Total can include duplicates across apps.</inline>\n  </block>\n</visual-title>"
            }
          }
        }
      }
    }

  }

}
