/* Report metrics to applications table entry for Quicksight Dashboard use */
WITH report_params AS (
  /* Report parameters
     Generates metrics between start/end dates */
  SELECT
    '${ENV}' as vpc,
    CAST('${START_DATE}' as Date) as start_date,
    CAST('${END_DATE}' as Date) as end_date,
    CAST('${REPORT_DATE}' as Date) as report_date,
    /* List of metrics to enable.
       NOTE: To greatly speed up development and testing of new metrics,
       this list can be used to select only metrics
       being worked on.
    */
    ARRAY [
      'app_fhir_v1_call_real_count',
      'app_fhir_v1_call_synthetic_count',
      'app_fhir_v1_eob_call_real_count',
      'app_fhir_v1_eob_call_synthetic_count',
      'app_fhir_v1_coverage_call_real_count',
      'app_fhir_v1_coverage_call_synthetic_count',
      'app_fhir_v1_patient_call_real_count',
      'app_fhir_v1_patient_call_synthetic_count',
      'app_fhir_v1_metadata_call_count',
      'app_fhir_v1_eob_since_call_real_count',
      'app_fhir_v1_eob_since_call_synthetic_count',
      'app_fhir_v1_coverage_since_call_real_count',
      'app_fhir_v1_coverage_since_call_synthetic_count',
      'app_fhir_v2_call_real_count',
      'app_fhir_v2_call_synthetic_count',
      'app_fhir_v2_eob_call_real_count',
      'app_fhir_v2_eob_call_synthetic_count',
      'app_fhir_v2_coverage_call_real_count',
      'app_fhir_v2_coverage_call_synthetic_count',
      'app_fhir_v2_patient_call_real_count',
      'app_fhir_v2_patient_call_synthetic_count',
      'app_fhir_v2_metadata_call_count',
      'app_fhir_v2_eob_since_call_real_count',
      'app_fhir_v2_eob_since_call_synthetic_count',
      'app_fhir_v2_coverage_since_call_real_count',
      'app_fhir_v2_coverage_since_call_synthetic_count',
      'app_fhir_v3_call_real_count',
      'app_fhir_v3_call_synthetic_count',
      'app_fhir_v3_eob_call_real_count',
      'app_fhir_v3_eob_call_synthetic_count',
      'app_fhir_v3_coverage_call_real_count',
      'app_fhir_v3_coverage_call_synthetic_count',
      'app_fhir_v3_patient_call_real_count',
      'app_fhir_v3_patient_call_synthetic_count',
      'app_fhir_v3_metadata_call_count',
      'app_fhir_v3_eob_since_call_real_count',
      'app_fhir_v3_eob_since_call_synthetic_count',
      'app_fhir_v3_coverage_since_call_real_count',
      'app_fhir_v3_coverage_since_call_synthetic_count',
      'app_fhir_v3_generate_insurance_card_call_real_count',
      'app_fhir_v3_generate_insurance_card_call_synthetic_count',
      'app_auth_ok_real_bene_count',
      'app_auth_ok_synthetic_bene_count',
      'app_auth_ok_real_bene_distinct_count',
      'app_auth_ok_synthetic_bene_distinct_count',
      'app_auth_fail_or_deny_real_bene_count',
      'app_auth_fail_or_deny_synthetic_bene_count',
      'app_auth_fail_or_deny_real_bene_distinct_count',
      'app_auth_fail_or_deny_synthetic_bene_distinct_count',
      'app_auth_demoscope_required_choice_sharing_real_bene_count',
      'app_auth_demoscope_required_choice_sharing_synthetic_bene_count',
      'app_auth_demoscope_required_choice_not_sharing_real_bene_count',
      'app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count',
      'app_auth_demoscope_required_choice_deny_real_bene_count',
      'app_auth_demoscope_required_choice_deny_synthetic_bene_count',
      'app_auth_demoscope_not_required_not_sharing_real_bene_count',
      'app_auth_demoscope_not_required_not_sharing_synthetic_bene_count',
      'app_auth_demoscope_not_required_deny_real_bene_count',
      'app_auth_demoscope_not_required_deny_synthetic_bene_count',
      'app_token_refresh_for_real_bene_count',
      'app_token_refresh_for_synthetic_bene_count',
      'app_token_authorization_code_for_real_bene_count',
      'app_token_authorization_code_for_synthetic_bene_count',
      'app_token_refresh_response_2xx_count',
      'app_token_refresh_response_4xx_count',
      'app_token_refresh_response_5xx_count',
      'app_token_authorization_code_2xx_count',
      'app_token_authorization_code_4xx_count',
      'app_token_authorization_code_5xx_count',
      'app_authorize_initial_count',
      'app_medicare_login_redirect_ok_count',
      'app_medicare_login_redirect_fail_count',
      'app_authentication_start_ok_count',
      'app_authentication_start_fail_count',
      'app_authentication_matched_new_bene_real_count',
      'app_authentication_matched_new_bene_synthetic_count',
      'app_authentication_matched_returning_bene_real_count',
      'app_authentication_matched_returning_bene_synthetic_count',
      'app_sls_callback_ok_real_count',
      'app_sls_callback_ok_synthetic_count',
      'app_sls_callback_fail_count',
      'app_approval_view_get_ok_real_count',
      'app_approval_view_get_ok_synthetic_count',
      'app_approval_view_get_fail_count',
      'app_approval_view_post_ok_real_count',
      'app_approval_view_post_ok_synthetic_count',
      'app_approval_view_post_fail_count',
      'app_sdk_requests_python_count',
      'app_sdk_requests_node_count',
      'app_access_grant_enabled',
      'app_access_grant_category',
      'app_internal_application_labels'
    ] as enabled_metrics_list 
),

/* All perf_mon application log events. Is a base for other sub-queries
   NOTE: This includes the report_date for getting at the max(group_timestamp) */
perf_mon_events_all AS (
  select
    *
  from
    "bb2"."events_${ENV}_perf_mon"
  WHERE
    (
      vpc = (
        select
          vpc
        FROM
          report_params
      )
      and cast("from_iso8601_timestamp"(time_of_event) AS date) >= (
        select
          start_date
        FROM
          report_params
      )
      and cast("from_iso8601_timestamp"(time_of_event) AS date) <= (
        select
          report_date
        FROM
          report_params
      )
      /* 
        Restricting select by partitions.
         NOTE: This significantly speeds up the SQL!
      */
      AND ${PARTITION_LIMIT_SQL}
    )
),

/* Find the max(group_timestamp) from 
   nightly global state per application events

   NOTE: We are wanting the entries that get logged
   on the actual report_date vs. other metrics to
   just include <= end_date.
*/
max_group_timestamp AS (
  SELECT 
    max(group_timestamp) as max_group_timestamp
  FROM 
    perf_mon_events_all
  WHERE
    type = 'global_state_metrics_per_app'
),
perf_mon_events AS (
  select
    *
  from
    perf_mon_events_all
  WHERE
    cast("from_iso8601_timestamp"(time_of_event) AS date) <= (
      select
        end_date
      FROM
        report_params
    )
),

request_response_middleware_events AS (
  select
    *,
    json_extract(user, '$$.crosswalk.fhir_id_v2') as crosswalk_fhir_id
  from
    perf_mon_events
  WHERE
    (
      type = 'request_response_middleware'
      AND 
        ( path LIKE '/v%/o/authorize%'
          OR path = '/mymedicare/login'
          OR path = '/mymedicare/sls-callback'
          OR path LIKE '/v1/fhir%'
          OR path LIKE '/v2/fhir%'
          OR path LIKE '/v3/fhir%'
          OR path LIKE '/v%/o/token%/'
        )
    )
),

api_audit_events AS (
  select
    *,
    json_extract(user, '$$.crosswalk.fhir_id_v2') as crosswalk_fhir_id
  from
    perf_mon_events
  WHERE
    (
      type = 'Authentication:start'
      OR type = 'Authentication:success'
      OR type = 'Authorization'
      OR type = 'AccessToken'
    )
),

/* Select all application names and metrics from 
   nightly global state per application events
*/
applications_state_metrics AS (
  SELECT 
    *
  /*
    DISTINCT name app_name,
    group_timestamp max_group_timestamp,
  */
  FROM 
    perf_mon_events_all
  WHERE
    type = 'global_state_metrics_per_app'
    AND
      group_timestamp = (
        select
          max_group_timestamp
        FROM
          max_group_timestamp
      )
    AND
      name NOT IN ('TestApp', 'BlueButton Client (Test - Internal Use Only)',
                   'MyMedicare PROD', 'new-relic')
),
/* Select all top level global state metrics from 
   nightly global state event
*/
global_state_metrics_for_max_group_timestamp AS (
  SELECT 
    group_timestamp max_group_timestamp,
    real_bene_cnt max_real_bene_cnt,
    synth_bene_cnt max_synth_bene_cnt,
    crosswalk_real_bene_count max_crosswalk_real_bene_count,
    crosswalk_synthetic_bene_count max_crosswalk_synthetic_bene_count,
    crosswalk_table_count max_crosswalk_table_count,
    crosswalk_archived_table_count max_crosswalk_archived_table_count,
    grant_real_bene_count max_grant_real_bene_count,
    grant_synthetic_bene_count max_grant_synthetic_bene_count,
    grant_table_count max_grant_table_count,
    grant_archived_table_count max_grant_archived_table_count,
    grant_real_bene_deduped_count max_grant_real_bene_deduped_count,
    grant_synthetic_bene_deduped_count max_grant_synthetic_bene_deduped_count,
    grantarchived_real_bene_deduped_count max_grantarchived_real_bene_deduped_count,
    grantarchived_synthetic_bene_deduped_count max_grantarchived_synthetic_bene_deduped_count,
    grant_and_archived_real_bene_deduped_count max_grant_and_archived_real_bene_deduped_count,
    grant_and_archived_synthetic_bene_deduped_count max_grant_and_archived_synthetic_bene_deduped_count,
    token_real_bene_deduped_count max_token_real_bene_deduped_count,
    token_synthetic_bene_deduped_count max_token_synthetic_bene_deduped_count,
    token_table_count max_token_table_count,
    token_archived_table_count max_token_archived_table_count,
    global_apps_active_cnt max_global_apps_active_cnt,
    global_apps_inactive_cnt max_global_apps_inactive_cnt,
    global_apps_require_demographic_scopes_cnt max_global_apps_require_demographic_scopes_cnt,
    global_developer_count max_global_developer_count,
    global_developer_distinct_organization_name_count max_global_developer_distinct_organization_name_count,
    global_developer_with_first_api_call_count max_global_developer_with_first_api_call_count,
    global_developer_with_registered_app_count max_global_developer_with_registered_app_count
  FROM 
    perf_mon_events_all
  WHERE
    type = 'global_state_metrics'
    AND
      group_timestamp = (
        select
          max_group_timestamp
        FROM
          max_group_timestamp
      )
)

SELECT 
  '${ENV}' as vpc,
  CAST('${START_DATE}' as Date) as start_date, 
  CAST('${END_DATE}' as Date) as end_date, 
  CAST('${REPORT_DATE}' as Date) as report_date, 
  t0.group_timestamp max_group_timestamp,

  t9999.max_real_bene_cnt,
  t9999.max_synth_bene_cnt,
  t9999.max_crosswalk_real_bene_count,
  t9999.max_crosswalk_synthetic_bene_count,
  t9999.max_crosswalk_table_count,
  t9999.max_crosswalk_archived_table_count,
  t9999.max_grant_real_bene_count,
  t9999.max_grant_synthetic_bene_count,
  t9999.max_grant_table_count,
  t9999.max_grant_archived_table_count,
  t9999.max_grant_real_bene_deduped_count,
  t9999.max_grant_synthetic_bene_deduped_count,
  t9999.max_grantarchived_real_bene_deduped_count,
  t9999.max_grantarchived_synthetic_bene_deduped_count,
  t9999.max_grant_and_archived_real_bene_deduped_count,
  t9999.max_grant_and_archived_synthetic_bene_deduped_count,
  t9999.max_token_real_bene_deduped_count,
  t9999.max_token_synthetic_bene_deduped_count,
  t9999.max_token_table_count,
  t9999.max_token_archived_table_count,
  t9999.max_global_apps_active_cnt,
  t9999.max_global_apps_inactive_cnt,
  t9999.max_global_apps_require_demographic_scopes_cnt,
  t9999.max_global_developer_count,
  t9999.max_global_developer_distinct_organization_name_count,
  t9999.max_global_developer_with_first_api_call_count,
  t9999.max_global_developer_with_registered_app_count,

  t0.name app_name,
  t0.id app_id,
  t0.created app_created,
  t0.updated app_updated,
  t0.active app_active,
  t0.first_active app_first_active,
  t0.last_active app_last_active,
  t0.user_limit_data_access app_access_grant_enabled,
  t0.data_access_type app_access_grant_category,
  t0.internal_application_labels app_internal_application_labels,
  t0.require_demographic_scopes app_require_demographic_scopes,
  t0.user_organization app_user_organization,
  t0.user_id app_user_id,
  t0.user_username app_user_username,
  t0.user_date_joined app_user_date_joined,
  t0.user_last_login app_user_last_login,
  t0.real_bene_cnt app_real_bene_cnt,
  t0.synth_bene_cnt app_synth_bene_cnt,
  t0.grant_real_bene_count app_grant_real_bene_count,
  t0.grant_synthetic_bene_count app_grant_synthetic_bene_count,
  t0.grant_table_count app_grant_table_count,
  t0.grant_archived_table_count app_grant_archived_table_count,
  t0.grantarchived_real_bene_deduped_count app_grantarchived_real_bene_deduped_count,
  t0.grantarchived_synthetic_bene_deduped_count app_grantarchived_synthetic_bene_deduped_count,
  t0.grant_and_archived_real_bene_deduped_count app_grant_and_archived_real_bene_deduped_count,
  t0.grant_and_archived_synthetic_bene_deduped_count app_grant_and_archived_synthetic_bene_deduped_count,
  t0.token_real_bene_count app_token_real_bene_count,
  t0.token_synthetic_bene_count app_token_synthetic_bene_count,
  t0.token_table_count app_token_table_count,
  t0.token_archived_table_count app_token_archived_table_count,

  /* FHIR V1 per application */
  COALESCE(t1.app_fhir_v1_call_real_count, 0)
    app_fhir_v1_call_real_count,
  COALESCE(t2.app_fhir_v1_call_synthetic_count, 0)
    app_fhir_v1_call_synthetic_count,
  COALESCE(t3.app_fhir_v1_eob_call_real_count, 0)
    app_fhir_v1_eob_call_real_count,
  COALESCE(t4.app_fhir_v1_eob_call_synthetic_count, 0)
    app_fhir_v1_eob_call_synthetic_count,
  COALESCE(t5.app_fhir_v1_coverage_call_real_count, 0)
    app_fhir_v1_coverage_call_real_count,
  COALESCE(t6.app_fhir_v1_coverage_call_synthetic_count, 0)
    app_fhir_v1_coverage_call_synthetic_count,
  COALESCE(t7.app_fhir_v1_patient_call_real_count, 0)
    app_fhir_v1_patient_call_real_count,
  COALESCE(t8.app_fhir_v1_patient_call_synthetic_count, 0)
    app_fhir_v1_patient_call_synthetic_count,
  COALESCE(t9.app_fhir_v1_metadata_call_count, 0)
    app_fhir_v1_metadata_call_count,
  COALESCE(t10.app_fhir_v1_eob_since_call_real_count, 0)
    app_fhir_v1_eob_since_call_real_count,
  COALESCE(t11.app_fhir_v1_eob_since_call_synthetic_count, 0)
    app_fhir_v1_eob_since_call_synthetic_count,
  COALESCE(t12.app_fhir_v1_coverage_since_call_real_count, 0)
    app_fhir_v1_coverage_since_call_real_count,
  COALESCE(t13.app_fhir_v1_coverage_since_call_synthetic_count, 0)
    app_fhir_v1_coverage_since_call_synthetic_count,

  /* FHIR V2 per application */
  COALESCE(t21.app_fhir_v2_call_real_count, 0)
    app_fhir_v2_call_real_count,
  COALESCE(t22.app_fhir_v2_call_synthetic_count, 0)
    app_fhir_v2_call_synthetic_count,
  COALESCE(t23.app_fhir_v2_eob_call_real_count, 0)
    app_fhir_v2_eob_call_real_count,
  COALESCE(t24.app_fhir_v2_eob_call_synthetic_count, 0)
    app_fhir_v2_eob_call_synthetic_count,
  COALESCE(t25.app_fhir_v2_coverage_call_real_count, 0)
    app_fhir_v2_coverage_call_real_count,
  COALESCE(t26.app_fhir_v2_coverage_call_synthetic_count, 0)
    app_fhir_v2_coverage_call_synthetic_count,
  COALESCE(t27.app_fhir_v2_patient_call_real_count, 0)
    app_fhir_v2_patient_call_real_count,
  COALESCE(t28.app_fhir_v2_patient_call_synthetic_count, 0)
    app_fhir_v2_patient_call_synthetic_count,
  COALESCE(t29.app_fhir_v2_metadata_call_count, 0)
    app_fhir_v2_metadata_call_count,
  COALESCE(t30.app_fhir_v2_eob_since_call_real_count, 0)
    app_fhir_v2_eob_since_call_real_count,
  COALESCE(t31.app_fhir_v2_eob_since_call_synthetic_count, 0)
    app_fhir_v2_eob_since_call_synthetic_count,
  COALESCE(t32.app_fhir_v2_coverage_since_call_real_count, 0)
    app_fhir_v2_coverage_since_call_real_count,
  COALESCE(t33.app_fhir_v2_coverage_since_call_synthetic_count, 0)
    app_fhir_v2_coverage_since_call_synthetic_count,

  /* FHIR V3 per application */
  COALESCE(t34.app_fhir_v3_call_real_count, 0)
    app_fhir_v3_call_real_count,
  COALESCE(t35.app_fhir_v3_call_synthetic_count, 0)
    app_fhir_v3_call_synthetic_count,
  COALESCE(t36.app_fhir_v3_eob_call_real_count, 0)
    app_fhir_v3_eob_call_real_count,
  COALESCE(t37.app_fhir_v3_eob_call_synthetic_count, 0)
    app_fhir_v3_eob_call_synthetic_count,
  COALESCE(t38.app_fhir_v3_coverage_call_real_count, 0)
    app_fhir_v3_coverage_call_real_count,
  COALESCE(t39.app_fhir_v3_coverage_call_synthetic_count, 0)
    app_fhir_v3_coverage_call_synthetic_count,
  COALESCE(t40.app_fhir_v3_patient_call_real_count, 0)
    app_fhir_v3_patient_call_real_count,
  COALESCE(t41.app_fhir_v3_patient_call_synthetic_count, 0)
    app_fhir_v3_patient_call_synthetic_count,
  COALESCE(t42.app_fhir_v3_metadata_call_count, 0)
    app_fhir_v3_metadata_call_count,
  COALESCE(t43.app_fhir_v3_eob_since_call_real_count, 0)
    app_fhir_v3_eob_since_call_real_count,
  COALESCE(t44.app_fhir_v3_eob_since_call_synthetic_count, 0)
    app_fhir_v3_eob_since_call_synthetic_count,
  COALESCE(t45.app_fhir_v3_coverage_since_call_real_count, 0)
    app_fhir_v3_coverage_since_call_real_count,
  COALESCE(t46.app_fhir_v3_coverage_since_call_synthetic_count, 0)
    app_fhir_v3_coverage_since_call_synthetic_count,
  COALESCE(t47.app_fhir_v3_generate_insurance_card_call_synthetic_count, 0)
    app_fhir_v3_generate_insurance_card_call_synthetic_count,
  COALESCE(t48.app_fhir_v3_generate_insurance_card_call_real_count, 0)
    app_fhir_v3_generate_insurance_card_call_real_count
  /* AUTH per applicaiton */
  COALESCE(t101.app_auth_ok_real_bene_count, 0)
    app_auth_ok_real_bene_count,
  COALESCE(t102.app_auth_ok_synthetic_bene_count, 0)
    app_auth_ok_synthetic_bene_count,
  COALESCE(t103.app_auth_ok_real_bene_distinct_count, 0)
    app_auth_ok_real_bene_distinct_count,
  COALESCE(t104.app_auth_ok_synthetic_bene_distinct_count, 0)
    app_auth_ok_synthetic_bene_distinct_count,

  COALESCE(t105.app_auth_fail_or_deny_real_bene_count, 0)
    app_auth_fail_or_deny_real_bene_count,
  COALESCE(t106.app_auth_fail_or_deny_synthetic_bene_count, 0)
    app_auth_fail_or_deny_synthetic_bene_count,
  COALESCE(t107.app_auth_fail_or_deny_real_bene_distinct_count, 0)
    app_auth_fail_or_deny_real_bene_distinct_count,
  COALESCE(t108.app_auth_fail_or_deny_synthetic_bene_distinct_count, 0)
    app_auth_fail_or_deny_synthetic_bene_distinct_count,

  COALESCE(t109.app_auth_demoscope_required_choice_sharing_real_bene_count, 0)
    app_auth_demoscope_required_choice_sharing_real_bene_count,
  COALESCE(t110.app_auth_demoscope_required_choice_sharing_synthetic_bene_count, 0)
    app_auth_demoscope_required_choice_sharing_synthetic_bene_count,
  COALESCE(t111.app_auth_demoscope_required_choice_not_sharing_real_bene_count, 0)
    app_auth_demoscope_required_choice_not_sharing_real_bene_count,
  COALESCE(t112.app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count, 0)
    app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count,
  COALESCE(t113.app_auth_demoscope_required_choice_deny_real_bene_count, 0)
    app_auth_demoscope_required_choice_deny_real_bene_count,
  COALESCE(t114.app_auth_demoscope_required_choice_deny_synthetic_bene_count, 0)
    app_auth_demoscope_required_choice_deny_synthetic_bene_count,
  COALESCE(t115.app_auth_demoscope_not_required_not_sharing_real_bene_count, 0)
    app_auth_demoscope_not_required_not_sharing_real_bene_count,
  COALESCE(t116.app_auth_demoscope_not_required_not_sharing_synthetic_bene_count, 0)
    app_auth_demoscope_not_required_not_sharing_synthetic_bene_count,
  COALESCE(t117.app_auth_demoscope_not_required_deny_real_bene_count, 0)
    app_auth_demoscope_not_required_deny_real_bene_count,
  COALESCE(t118.app_auth_demoscope_not_required_deny_synthetic_bene_count, 0)
    app_auth_demoscope_not_required_deny_synthetic_bene_count,


  COALESCE(t200.app_token_refresh_for_real_bene_count, 0)
    app_token_refresh_for_real_bene_count,
  COALESCE(t201.app_token_refresh_for_synthetic_bene_count, 0)
    app_token_refresh_for_synthetic_bene_count,
  COALESCE(t202.app_token_authorization_code_for_real_bene_count, 0)
    app_token_authorization_code_for_real_bene_count,
  COALESCE(t203.app_token_authorization_code_for_synthetic_bene_count, 0)
    app_token_authorization_code_for_synthetic_bene_count,
  COALESCE(t204.app_token_refresh_response_2xx_count, 0)
    app_token_refresh_response_2xx_count,
  COALESCE(t205.app_token_refresh_response_4xx_count, 0)
    app_token_refresh_response_4xx_count,
  COALESCE(t206.app_token_refresh_response_5xx_count, 0)
    app_token_refresh_response_5xx_count,
  COALESCE(t207.app_token_authorization_code_2xx_count, 0)
    app_token_authorization_code_2xx_count,
  COALESCE(t208.app_token_authorization_code_4xx_count, 0)
    app_token_authorization_code_4xx_count,
  COALESCE(t209.app_token_authorization_code_5xx_count, 0)
    app_token_authorization_code_5xx_count,
  COALESCE(t210.app_authorize_initial_count, 0)
    app_authorize_initial_count,
  COALESCE(t211.app_medicare_login_redirect_ok_count, 0)
    app_medicare_login_redirect_ok_count,
  COALESCE(t212.app_medicare_login_redirect_fail_count, 0)
    app_medicare_login_redirect_fail_count,
  COALESCE(t213.app_authentication_start_ok_count, 0)
    app_authentication_start_ok_count,
  COALESCE(t214.app_authentication_start_fail_count, 0)
    app_authentication_start_fail_count,
  COALESCE(t215.app_authentication_matched_new_bene_real_count, 0)
    app_authentication_matched_new_bene_real_count,
  COALESCE(t216.app_authentication_matched_new_bene_synthetic_count, 0)
    app_authentication_matched_new_bene_synthetic_count,
  COALESCE(t217.app_authentication_matched_returning_bene_real_count, 0)
    app_authentication_matched_returning_bene_real_count,
  COALESCE(t218.app_authentication_matched_returning_bene_synthetic_count, 0)
    app_authentication_matched_returning_bene_synthetic_count,
  COALESCE(t219.app_sls_callback_ok_real_count, 0)
    app_sls_callback_ok_real_count,
  COALESCE(t220.app_sls_callback_ok_synthetic_count, 0)
    app_sls_callback_ok_synthetic_count,
  COALESCE(t221.app_sls_callback_fail_count, 0)
    app_sls_callback_fail_count,
  COALESCE(t222.app_approval_view_get_ok_real_count, 0)
    app_approval_view_get_ok_real_count,
  COALESCE(t223.app_approval_view_get_ok_synthetic_count, 0)
    app_approval_view_get_ok_synthetic_count,
  COALESCE(t224.app_approval_view_get_fail_count, 0)
    app_approval_view_get_fail_count,
  COALESCE(t225.app_approval_view_post_ok_real_count, 0)
    app_approval_view_post_ok_real_count,
  COALESCE(t226.app_approval_view_post_ok_synthetic_count, 0)
    app_approval_view_post_ok_synthetic_count,
  COALESCE(t227.app_approval_view_post_fail_count, 0)
    app_approval_view_post_fail_count,
  COALESCE(t228.app_sdk_requests_python_count, 0)
    app_sdk_requests_python_count,
  COALESCE(t229.app_sdk_requests_node_count, 0)
    app_sdk_requests_node_count

FROM
  (
    SELECT 
      *
    FROM applications_state_metrics
  ) t0

  LEFT JOIN
  (
    SELECT 
      *
    FROM global_state_metrics_for_max_group_timestamp
  ) t9999 ON t9999.max_group_timestamp = t0.group_timestamp

  /* V1 FHIR resource stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_call_real_count')

        AND path LIKE '/v1/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t1 ON t1.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_call_synthetic_count')

        AND path LIKE '/v1/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t2 ON t2.app_name = t0.name 


  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_eob_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_eob_call_real_count')

        AND path LIKE '/v1/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t3 ON t3.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_eob_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_eob_call_synthetic_count')

        AND path LIKE '/v1/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t4 ON t4.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_coverage_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_coverage_call_real_count')

        AND path LIKE '/v1/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t5 ON t5.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_coverage_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_coverage_call_synthetic_count')

        AND path LIKE '/v1/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t6 ON t6.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_patient_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_patient_call_real_count')

        AND path LIKE '/v1/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t7 ON t7.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_patient_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_patient_call_synthetic_count')

        AND path LIKE '/v1/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t8 ON t8.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_metadata_call_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_metadata_call_count')

        AND path LIKE '/v1/fhir/metadata%'
        AND request_method = 'GET'
        AND response_code = 200
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t9 ON t9.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_eob_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_eob_since_call_real_count')

        AND path LIKE '/v1/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t10 ON t10.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_eob_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_eob_since_call_synthetic_count')

        AND path LIKE '/v1/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t11 ON t11.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_coverage_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_coverage_since_call_real_count')

        AND path LIKE '/v1/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t12 ON t12.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v1_coverage_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v1_coverage_since_call_synthetic_count')

        AND path LIKE '/v1/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t13 ON t13.app_name = t0.name 


  /* V2 FHIR resource stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_call_real_count')

        AND path LIKE '/v2/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t21 ON t21.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_call_synthetic_count')

        AND path LIKE '/v2/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t22 ON t22.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_eob_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_eob_call_real_count')

        AND path LIKE '/v2/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t23 ON t23.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_eob_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_eob_call_synthetic_count')

        AND path LIKE '/v2/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t24 ON t24.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_coverage_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_coverage_call_real_count')

        AND path LIKE '/v2/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t25 ON t25.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_coverage_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_coverage_call_synthetic_count')

        AND path LIKE '/v2/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t26 ON t26.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_patient_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_patient_call_real_count')

        AND path LIKE '/v2/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t27 ON t27.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_patient_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_patient_call_synthetic_count')

        AND path LIKE '/v2/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t28 ON t28.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_metadata_call_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_metadata_call_count')

        AND path LIKE '/v2/fhir/metadata%'
        AND request_method = 'GET'
        AND response_code = 200
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t29 ON t29.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_eob_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_eob_since_call_real_count')

        AND path LIKE '/v2/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t30 ON t30.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_eob_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_eob_since_call_synthetic_count')

        AND path LIKE '/v2/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t31 ON t31.app_name = t0.name 


  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_coverage_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_coverage_since_call_real_count')

        AND path LIKE '/v2/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t32 ON t32.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v2_coverage_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v2_coverage_since_call_synthetic_count')

        AND path LIKE '/v2/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t33 ON t33.app_name = t0.name 

  /* V3 FHIR resource stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_call_real_count')

        AND path LIKE '/v3/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t34 ON t34.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_call_synthetic_count')

        AND path LIKE '/v3/fhir%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t35 ON t35.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_eob_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_eob_call_real_count')

        AND path LIKE '/v3/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t36 ON t36.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_eob_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_eob_call_synthetic_count')

        AND path LIKE '/v3/fhir/ExplanationOfBenefit%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t37 ON t37.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_coverage_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_coverage_call_real_count')

        AND path LIKE '/v3/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t38 ON t38.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_coverage_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_coverage_call_synthetic_count')

        AND path LIKE '/v3/fhir/Coverage%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t39 ON t39.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_patient_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_patient_call_real_count')

        AND path LIKE '/v3/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t40 ON t40.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_patient_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_patient_call_synthetic_count')

        AND path LIKE '/v3/fhir/Patient%'
        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t41 ON t41.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_metadata_call_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_metadata_call_count')

        AND path LIKE '/v3/fhir/metadata%'
        AND request_method = 'GET'
        AND response_code = 200
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t42 ON t42.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_eob_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_eob_since_call_real_count')

        AND path LIKE '/v3/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t43 ON t43.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_eob_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_eob_since_call_synthetic_count')

        AND path LIKE '/v3/fhir/ExplanationOfBenefit%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t44 ON t44.app_name = t0.name 


  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_coverage_since_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_coverage_since_call_real_count')

        AND path LIKE '/v3/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t45 ON t45.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_coverage_since_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_coverage_since_call_synthetic_count')

        AND path LIKE '/v3/fhir/Coverage%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t46 ON t46.app_name = t0.name

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_generate_insurance_card_call_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_generate_insurance_card_call_synthetic_count')

        AND path LIKE '/v3/fhir/Patient/\$generate-insurance-card%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t47 ON t47.app_name = t0.name

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_fhir_v3_generate_insurance_card_call_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_fhir_v3_generate_insurance_card_call_real_count')

        AND path LIKE '/v3/fhir/Patient/\$generate-insurance-card%'
        AND req_qparam_lastupdated != ''

        AND request_method = 'GET'
        AND response_code = 200
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t48 ON t48.app_name = t0.name

  /* AUTH per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_ok_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_ok_real_bene_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t101 ON t101.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_ok_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_ok_synthetic_bene_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t102 ON t102.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(DISTINCT auth_uuid) as app_auth_ok_real_bene_distinct_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_ok_real_bene_distinct_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t103 ON t103.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(DISTINCT auth_uuid) as app_auth_ok_synthetic_bene_distinct_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_ok_synthetic_bene_distinct_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t104 ON t104.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_fail_or_deny_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_fail_or_deny_real_bene_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'FAIL'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t105 ON t105.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_fail_or_deny_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_fail_or_deny_synthetic_bene_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'FAIL'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t106 ON t106.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(DISTINCT auth_uuid) as app_auth_fail_or_deny_real_bene_distinct_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_fail_or_deny_real_bene_distinct_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'FAIL'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t107 ON t107.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(DISTINCT auth_uuid) as app_auth_fail_or_deny_synthetic_bene_distinct_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_fail_or_deny_synthetic_bene_distinct_count')
        AND type = 'Authorization'
        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'FAIL'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t108 ON t108.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_sharing_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_sharing_real_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'True'

      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t109 ON t109.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_sharing_synthetic_bene_count 
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_sharing_synthetic_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'True'

      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t110 ON t110.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_not_sharing_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_not_sharing_real_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t111 ON t111.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t112 ON t112.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_deny_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_deny_real_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and allow = False
        and auth_require_demographic_scopes = 'True'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t113 ON t113.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_required_choice_deny_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_required_choice_deny_synthetic_bene_count')
        AND type = 'Authorization'
        
        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and allow = False
        and auth_require_demographic_scopes = 'True'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t114 ON t114.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_not_required_not_sharing_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_not_required_not_sharing_real_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t115 ON t115.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_not_required_not_sharing_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_not_required_not_sharing_synthetic_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t116 ON t116.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_not_required_deny_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_not_required_deny_real_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        and allow = False
        and auth_require_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t117 ON t117.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_auth_demoscope_not_required_deny_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_auth_demoscope_not_required_deny_synthetic_bene_count')
        AND type = 'Authorization'

        AND try_cast(crosswalk_fhir_id as BIGINT) < 0
        and allow = False
        and auth_require_demographic_scopes = 'False'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t118 ON t118.app_name = t0.name 

  /* Token stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_refresh_for_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_refresh_for_real_bene_count')

        AND type = 'AccessToken'
        AND action = 'authorized'
        AND auth_grant_type = 'refresh_token'
        AND try_cast(crosswalk.fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t200 ON t200.app_name = t0.name 


  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_refresh_for_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_refresh_for_synthetic_bene_count')

        AND type = 'AccessToken'
        AND action = 'authorized'
        AND auth_grant_type = 'refresh_token'
        AND try_cast(crosswalk.fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t201 ON t201.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_authorization_code_for_real_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_authorization_code_for_real_bene_count')

        AND type = 'AccessToken'
        AND auth_grant_type = 'authorization_code'
        AND try_cast(crosswalk.fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t202 ON t202.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_authorization_code_for_synthetic_bene_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_authorization_code_for_synthetic_bene_count')

        AND type = 'AccessToken'
        AND auth_grant_type = 'authorization_code'
        AND try_cast(crosswalk.fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t203 ON t203.app_name = t0.name 

  /* Token request stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_refresh_response_2xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_refresh_response_2xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'refresh_token'
        AND response_code >= 200
        AND response_code < 300
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t204 ON t204.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_refresh_response_4xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_refresh_response_4xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'refresh_token'
        AND response_code >= 400
        AND response_code < 500
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t205 ON t205.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_refresh_response_5xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_refresh_response_5xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'refresh_token'
        AND response_code >= 500
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t206 ON t206.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_authorization_code_2xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_authorization_code_2xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'authorization_code'
        AND response_code >= 200
        AND response_code < 300
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t207 ON t207.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_authorization_code_4xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_authorization_code_4xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'authorization_code'
        AND response_code >= 400
        AND response_code < 500
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t208 ON t208.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_token_authorization_code_5xx_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_token_authorization_code_5xx_count')

        AND request_method = 'POST'
        AND path LIKE '/v%/o/token%/'
        AND auth_grant_type = 'authorization_code'
        AND response_code >= 500
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t209 ON t209.app_name = t0.name 

  /* Auth flow stats per application */
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authorize_initial_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authorize_initial_count')

        AND path LIKE '/v%/o/authorize/'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t210 ON t210.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_medicare_login_redirect_ok_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_medicare_login_redirect_ok_count')

        AND path = '/mymedicare/login'
        AND response_code = 302
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t211 ON t211.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_medicare_login_redirect_fail_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_medicare_login_redirect_fail_count')

        AND path = '/mymedicare/login'
        AND response_code != 302
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t212 ON t212.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_start_ok_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_start_ok_count')

        AND type = 'Authentication:start'
        AND sls_userinfo_status_code = 200
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t213 ON t213.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_start_fail_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_start_fail_count')

        AND type = 'Authentication:start'
        AND sls_userinfo_status_code != 200
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t214 ON t214.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_matched_new_bene_real_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_matched_new_bene_real_count')

        AND type = 'Authentication:success'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        AND auth_crosswalk_action = 'C'

      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t215 ON t215.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_matched_new_bene_synthetic_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_matched_new_bene_synthetic_count')

        AND type = 'Authentication:success'
        and try_cast(crosswalk_fhir_id as BIGINT) < 0
        AND auth_crosswalk_action = 'C'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t216 ON t216.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_matched_returning_bene_real_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_matched_returning_bene_real_count')

        AND type = 'Authentication:success'
        AND try_cast(crosswalk_fhir_id as BIGINT) > 0
        AND auth_crosswalk_action = 'R'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t217 ON t217.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_authentication_matched_returning_bene_synthetic_count
    FROM
      api_audit_events 
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_authentication_matched_returning_bene_synthetic_count')

        AND type = 'Authentication:success'
        and try_cast(crosswalk_fhir_id as BIGINT) < 0
        AND auth_crosswalk_action = 'R'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t218 ON t218.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sls_callback_ok_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sls_callback_ok_real_count')

        AND path = '/mymedicare/sls-callback'
        AND response_code = 302
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t219 ON t219.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sls_callback_ok_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sls_callback_ok_synthetic_count')

        AND path = '/mymedicare/sls-callback'
        AND response_code = 302
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t220 ON t220.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sls_callback_fail_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sls_callback_fail_count')

        AND path = '/mymedicare/sls-callback'
        AND response_code != 302
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t221 ON t221.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_get_ok_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_get_ok_real_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'GET'
        AND response_code IN (200, 302)
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t222 ON t222.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_get_ok_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_get_ok_synthetic_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'GET'
        AND response_code IN (200, 302)
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t223 ON t223.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_get_fail_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_get_fail_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'GET'
        AND response_code NOT IN (200, 302)
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t224 ON t224.app_name = t0.name 


  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_post_ok_real_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_post_ok_real_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'POST'
        AND response_code IN (200, 302)
        AND try_cast(fhir_id_v2 as BIGINT) > 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t225 ON t225.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_post_ok_synthetic_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_post_ok_synthetic_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'POST'
        AND response_code IN (200, 302)
        AND try_cast(fhir_id_v2 as BIGINT) < 0
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t226 ON t226.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_approval_view_post_fail_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_approval_view_post_fail_count')

        AND path LIKE '/v%/o/authorize/%/'
        AND request_method = 'POST'
        AND response_code NOT IN (200, 302)
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t227 ON t227.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sdk_requests_python_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sdk_requests_python_count')

        AND req_header_bluebutton_sdk = 'python'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t228 ON t228.app_name = t0.name 

  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sdk_requests_node_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sdk_requests_node_count')

        AND req_header_bluebutton_sdk = 'node'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t229 ON t229.app_name = t0.name 
