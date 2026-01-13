/* Report metrics to main table entry for Quicksight Dashboard use */
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
      'fhir_v1_call_real_count',
      'fhir_v1_call_synthetic_count',
      'fhir_v1_eob_call_real_count',
      'fhir_v1_eob_call_synthetic_count',
      'fhir_v1_coverage_call_real_count',
      'fhir_v1_coverage_call_synthetic_count',
      'fhir_v1_patient_call_real_count',
      'fhir_v1_patient_call_synthetic_count',
      'fhir_v1_metadata_call_count',
      'fhir_v1_eob_since_call_real_count',
      'fhir_v1_eob_since_call_synthetic_count',
      'fhir_v1_coverage_since_call_real_count',
      'fhir_v1_coverage_since_call_synthetic_count',
      'fhir_v2_call_real_count',
      'fhir_v2_call_synthetic_count',
      'fhir_v2_eob_call_real_count',
      'fhir_v2_eob_call_synthetic_count',
      'fhir_v2_coverage_call_real_count',
      'fhir_v2_coverage_call_synthetic_count',
      'fhir_v2_patient_call_real_count',
      'fhir_v2_patient_call_synthetic_count',
      'fhir_v2_metadata_call_count',
      'fhir_v2_eob_since_call_real_count',
      'fhir_v2_eob_since_call_synthetic_count',
      'fhir_v2_coverage_since_call_real_count',
      'fhir_v2_coverage_since_call_synthetic_count',
      'auth_ok_real_bene_count',
      'auth_ok_synthetic_bene_count',
      'auth_fail_or_deny_real_bene_count',
      'auth_fail_or_deny_synthetic_bene_count',
      'auth_demoscope_required_choice_sharing_real_bene_count',
      'auth_demoscope_required_choice_sharing_synthetic_bene_count',
      'auth_demoscope_required_choice_not_sharing_real_bene_count',
      'auth_demoscope_required_choice_not_sharing_synthetic_bene_count',
      'auth_demoscope_required_choice_deny_real_bene_count',
      'auth_demoscope_required_choice_deny_synthetic_bene_count',
      'auth_demoscope_not_required_not_sharing_real_bene_count',
      'auth_demoscope_not_required_not_sharing_synthetic_bene_count',
      'auth_demoscope_not_required_deny_real_bene_count',
      'auth_demoscope_not_required_deny_synthetic_bene_count',
      'sdk_requests_python_count',
      'sdk_requests_node_count'
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
   nightly global state events
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
    type = 'global_state_metrics'
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
    json_extract(user, '$$.crosswalk.fhir_id') as crosswalk_fhir_id
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
          OR path LIKE '/v%/o/token%/'
        )
    )
),

api_audit_events AS (
  select
    *,
    json_extract(user, '$$.crosswalk.fhir_id') as crosswalk_fhir_id
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
    '${ENV}' as vpc,
    CAST('${START_DATE}' as Date) as start_date, 
    CAST('${END_DATE}' as Date) as end_date, 
    CAST('${REPORT_DATE}' as Date) as report_date, 
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
),

/* Select metrics already generated for report_date group_timestamp
   from PER_APP table: ENV_global_state_per_app
*/
global_state_metrics_per_app_for_max_group_timestamp AS (
    SELECT
      vpc,
      start_date,
      end_date,
      report_date,
      max_group_timestamp,
      /*
       NOTE: Metrics in this section prefixed by "total_" come from the 
       type = "global_state_metrics", 
       where counts are performed at time of logging.
       */
      max_crosswalk_real_bene_count total_crosswalk_real_bene,
      max_crosswalk_synthetic_bene_count total_crosswalk_synthetic_bene,
      max_crosswalk_table_count total_crosswalk_table_count,
      max_crosswalk_archived_table_count total_crosswalk_archived_table_count,
      max_grant_real_bene_count total_grant_real_bene_count,
      max_grant_synthetic_bene_count total_grant_synthetic_bene_count,
      max_grant_table_count total_grant_table_count,
      max_grant_archived_table_count total_grant_archived_table_count,
      max_grant_real_bene_deduped_count total_grant_real_bene_deduped_count,
      max_grant_synthetic_bene_deduped_count total_grant_synthetic_bene_deduped_count,
      max_grantarchived_real_bene_deduped_count total_grantarchived_real_bene_deduped_count,
      max_grantarchived_synthetic_bene_deduped_count total_grantarchived_synthetic_bene_deduped_count,
      max_grant_and_archived_real_bene_deduped_count total_grant_and_archived_real_bene_deduped_count,
      max_grant_and_archived_synthetic_bene_deduped_count total_grant_and_archived_synthetic_bene_deduped_count,
      max_token_real_bene_deduped_count total_token_real_bene_deduped_count,
      max_token_synthetic_bene_deduped_count total_token_synthetic_bene_deduped_count,
      max_token_table_count total_token_table_count,
      max_token_archived_table_count total_token_archived_table_count,
      max_global_apps_active_cnt total_apps_in_system,
      max_global_apps_inactive_cnt total_inactive_apps_in_system,
      max_global_apps_require_demographic_scopes_cnt total_apps_require_demo_scopes_cnt,
      max_global_developer_count total_developer_count,
      max_global_developer_distinct_organization_name_count total_developer_distinct_organization_name_count,
      max_global_developer_with_first_api_call_count total_developer_with_first_api_call_count,
      max_global_developer_with_registered_app_count total_developer_with_registered_app_count,
      /*
       NOTE: Metrics in this section prefixed by "app_" come from the 
       type = "global_state_metrics_per_app",
       per the vw_${ENV}_global_state_per_app view
       where the counts/sums are performed in SQL below.
       */
      "count"(
        (
          CASE
            WHEN (app_active = true) THEN 1
          END
        )
      ) app_total_active,
      "count"(
        (
          CASE
            WHEN (
              (app_real_bene_cnt > 25)
              AND (app_active = true)
            ) THEN 1
          END
        )
      ) app_active_bene_cnt_gt25,
      "count"(
        (
          CASE
            WHEN (
              (app_real_bene_cnt <= 25)
              AND (app_active = true)
            ) THEN 1
          END
        )
      ) app_active_bene_cnt_le25,
      "count"(
        (
          CASE
            WHEN (
              (app_created IS NOT NULL)
              AND (app_active = true)
            ) THEN 1
          END
        )
      ) app_active_registered,
      "count"(
        (
          CASE
            WHEN (
              (app_first_active IS NOT NULL)
              AND (app_active = true)
            ) THEN 1
          END
        )
      ) app_active_first_api,
      "count"(
        (
          CASE
            WHEN (
              (
                app_require_demographic_scopes = true
              )
              AND (app_active = true)
            ) THEN 1
          END
        )
      ) app_active_require_demographic,
      "sum"(
        (
          CASE
            WHEN (app_active = true) THEN app_real_bene_cnt
          END
        )
      ) app_grant_active_real_bene,
      "sum"(
        (
          CASE
            WHEN (app_active = true) THEN app_synth_bene_cnt
          END
        )
      ) app_grant_active_synthetic_bene,
      "count"(*) app_all,
      "count"(
        (
          CASE
            WHEN (app_real_bene_cnt > 25) THEN 1
          END
        )
      ) app_all_real_bene_gt25,
      "count"(
        (
          CASE
            WHEN (app_real_bene_cnt <= 25) THEN 1
          END
        )
      ) app_all_real_bene_le25,
      "count"(
        (
          CASE
            WHEN (app_created IS NOT NULL) THEN 1
          END
        )
      ) app_all_registered,
      "count"(
        (
          CASE
            WHEN (app_first_active IS NOT NULL) THEN 1
          END
        )
      ) app_all_first_api,
      "count"(
        (
          CASE
            WHEN (
              app_require_demographic_scopes = true
            ) THEN 1
          END
        )
      ) app_all_require_demographic,
      "sum"(app_grant_and_archived_real_bene_deduped_count) app_all_grant_real_bene,
      "sum"(
        app_grant_and_archived_synthetic_bene_deduped_count
      ) app_all_grant_synthetic_bene,
      "sum"(
        app_fhir_v1_call_real_count
      ) app_all_fhir_v1_call_real_count,
      "sum"(
        app_fhir_v1_call_synthetic_count
      ) app_all_fhir_v1_call_synthetic_count,
      "sum"(
        app_fhir_v1_eob_call_real_count
      ) app_all_fhir_v1_eob_call_real_count,
      "sum"(
        app_fhir_v1_eob_call_synthetic_count
      ) app_all_fhir_v1_eob_call_synthetic_count,
      "sum"(
        app_fhir_v1_coverage_call_real_count
      ) app_all_fhir_v1_coverage_call_real_count,
      "sum"(
        app_fhir_v1_coverage_call_synthetic_count
      ) app_all_fhir_v1_coverage_call_synthetic_count,
      "sum"(
        app_fhir_v1_patient_call_real_count
      ) app_all_fhir_v1_patient_call_real_count,
      "sum"(
        app_fhir_v1_patient_call_synthetic_count
      ) app_all_fhir_v1_patient_call_synthetic_count,
      "sum"(
        app_fhir_v1_metadata_call_count
      ) app_all_fhir_v1_metadata_call_count,
      "sum"(
        app_fhir_v1_eob_since_call_real_count
      ) app_all_fhir_v1_eob_since_call_real_count,
      "sum"(
        app_fhir_v1_eob_since_call_synthetic_count
      ) app_all_fhir_v1_eob_since_call_synthetic_count,
      "sum"(
        app_fhir_v1_coverage_since_call_real_count
      ) app_all_fhir_v1_coverage_since_call_real_count,
      "sum"(
        app_fhir_v1_coverage_since_call_synthetic_count
      ) app_all_fhir_v1_coverage_since_call_synthetic_count,
      "sum"(
        app_fhir_v2_call_real_count
      ) app_all_fhir_v2_call_real_count,
      "sum"(
        app_fhir_v2_call_synthetic_count
      ) app_all_fhir_v2_call_synthetic_count,
      "sum"(
        app_fhir_v2_eob_call_real_count
      ) app_all_fhir_v2_eob_call_real_count,
      "sum"(
        app_fhir_v2_eob_call_synthetic_count
      ) app_all_fhir_v2_eob_call_synthetic_count,
      "sum"(
        app_fhir_v2_coverage_call_real_count
      ) app_all_fhir_v2_coverage_call_real_count,
      "sum"(
        app_fhir_v2_coverage_call_synthetic_count
      ) app_all_fhir_v2_coverage_call_synthetic_count,
      "sum"(
        app_fhir_v2_patient_call_real_count
      ) app_all_fhir_v2_patient_call_real_count,
      "sum"(
        app_fhir_v2_patient_call_synthetic_count
      ) app_all_fhir_v2_patient_call_synthetic_count,
      "sum"(
        app_fhir_v2_metadata_call_count
      ) app_all_fhir_v2_metadata_call_count,
      "sum"(
        app_fhir_v2_eob_since_call_real_count
      ) app_all_fhir_v2_eob_since_call_real_count,
      "sum"(
        app_fhir_v2_eob_since_call_synthetic_count
      ) app_all_fhir_v2_eob_since_call_synthetic_count,
      "sum"(
        app_fhir_v2_coverage_since_call_real_count
      ) app_all_fhir_v2_coverage_since_call_real_count,
      "sum"(
        app_fhir_v2_coverage_since_call_synthetic_count
      ) app_all_fhir_v2_coverage_since_call_synthetic_count,
      "sum"(
        app_auth_ok_real_bene_count
      ) app_all_auth_ok_real_bene_count,
      "sum"(
        app_auth_ok_synthetic_bene_count
      ) app_all_auth_ok_synthetic_bene_count,
      "sum"(
        app_auth_fail_or_deny_real_bene_count
      ) app_all_auth_fail_or_deny_real_bene_count,
      "sum"(
        app_auth_fail_or_deny_synthetic_bene_count
      ) app_all_auth_fail_or_deny_synthetic_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_sharing_real_bene_count
      ) app_all_auth_demoscope_required_choice_sharing_real_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_sharing_synthetic_bene_count
      ) app_all_auth_demoscope_required_choice_sharing_synthetic_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_not_sharing_real_bene_count
      ) app_all_auth_demoscope_required_choice_not_sharing_real_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_not_sharing_synthetic_bene_count
      ) app_all_auth_demoscope_required_choice_not_sharing_synthetic_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_deny_real_bene_count
      ) app_all_auth_demoscope_required_choice_deny_real_bene_count,
      "sum"(
        app_auth_demoscope_required_choice_deny_synthetic_bene_count
      ) app_all_auth_demoscope_required_choice_deny_synthetic_bene_count,
      "sum"(
        app_auth_demoscope_not_required_not_sharing_real_bene_count
      ) app_all_auth_demoscope_not_required_not_sharing_real_bene_count,
      "sum"(
        app_auth_demoscope_not_required_not_sharing_synthetic_bene_count
      ) app_all_auth_demoscope_not_required_not_sharing_synthetic_bene_count,
      "sum"(
        app_auth_demoscope_not_required_deny_real_bene_count
      ) app_all_auth_demoscope_not_required_deny_real_bene_count,
      "sum"(
        app_auth_demoscope_not_required_deny_synthetic_bene_count
      ) app_all_auth_demoscope_not_required_deny_synthetic_bene_count,
      "sum"(
        app_token_authorization_code_2xx_count
      ) app_all_token_authorization_code_2xx_count,
      "sum"(
        app_token_authorization_code_4xx_count
      ) app_all_token_authorization_code_4xx_count,
      "sum"(
        app_token_authorization_code_5xx_count
      ) app_all_token_authorization_code_5xx_count,
      "sum"(
        app_token_authorization_code_for_real_bene_count
      ) app_all_token_authorization_code_for_real_bene_count,
      "sum"(
        app_token_authorization_code_for_synthetic_bene_count
      ) app_all_token_authorization_code_for_synthetic_bene_count,
      "sum"(
        app_token_refresh_for_real_bene_count
      ) app_all_token_refresh_for_real_bene_count,
      "sum"(
        app_token_refresh_for_synthetic_bene_count
      ) app_all_token_refresh_for_synthetic_bene_count,
      "sum"(
        app_token_refresh_response_2xx_count
      ) app_all_token_refresh_response_2xx_count,
      "sum"(
        app_token_refresh_response_4xx_count
      ) app_all_token_refresh_response_4xx_count,
      "sum"(
        app_token_refresh_response_5xx_count
      ) app_all_token_refresh_response_5xx_count,
      "sum"(
       app_authorize_initial_count
      ) app_all_authorize_initial_count,
      "sum"(
       app_medicare_login_redirect_OK_count
      ) app_all_medicare_login_redirect_OK_count,
      "sum"(
       app_medicare_login_redirect_FAIL_count
      ) app_all_medicare_login_redirect_FAIL_count,
      "sum"(
       app_authentication_start_ok_count
      ) app_all_authentication_start_ok_count,
      "sum"(
       app_authentication_start_fail_count
      ) app_all_authentication_start_fail_count,
      "sum"(
       app_authentication_matched_new_bene_real_count
      ) app_all_authentication_matched_new_bene_real_count,
      "sum"(
       app_authentication_matched_new_bene_synthetic_count
      ) app_all_authentication_matched_new_bene_synthetic_count,
      "sum"(
       app_authentication_matched_returning_bene_real_count
      ) app_all_authentication_matched_returning_bene_real_count,
      "sum"(
       app_authentication_matched_returning_bene_synthetic_count
      ) app_all_authentication_matched_returning_bene_synthetic_count,
      "sum"(
       app_sls_callback_ok_real_count
      ) app_all_sls_callback_ok_real_count,
      "sum"(
       app_sls_callback_ok_synthetic_count
      ) app_all_sls_callback_ok_synthetic_count,
      "sum"(
       app_sls_callback_fail_count
      ) app_all_sls_callback_fail_count,
      "sum"(
       app_approval_view_get_ok_real_count
      ) app_all_approval_view_get_ok_real_count,
      "sum"(
       app_approval_view_get_ok_synthetic_count
      ) app_all_approval_view_get_ok_synthetic_count,
      "sum"(
       app_approval_view_get_fail_count
      ) app_all_approval_view_get_fail_count,
      "sum"(
       app_approval_view_post_ok_real_count
      ) app_all_approval_view_post_ok_real_count,
      "sum"(
       app_approval_view_post_ok_synthetic_count
      ) app_all_approval_view_post_ok_synthetic_count,
      "sum"(
       app_approval_view_post_fail_count
      ) app_all_approval_view_post_fail_count,
      "sum"(
       app_sdk_requests_python_count
      ) app_all_sdk_requests_python_count,
      "sum"(
       app_sdk_requests_node_count
      ) app_all_sdk_requests_node_count
    FROM
      ${ENV}_${BASENAME_PER_APP}
    WHERE
      cast(report_date AS date) = (
        select
          report_date
        FROM
          report_params
      )
    GROUP BY
      vpc,
      start_date,
      end_date,
      report_date,
      max_group_timestamp,
      max_real_bene_cnt,
      max_synth_bene_cnt,
      max_crosswalk_real_bene_count,
      max_crosswalk_synthetic_bene_count,
      max_crosswalk_table_count,
      max_crosswalk_archived_table_count,
      max_grant_real_bene_count,
      max_grant_synthetic_bene_count,
      max_grant_table_count,
      max_grant_archived_table_count,
      max_grant_real_bene_deduped_count,
      max_grant_synthetic_bene_deduped_count,
      max_grantarchived_real_bene_deduped_count,
      max_grantarchived_synthetic_bene_deduped_count,
      max_grant_and_archived_real_bene_deduped_count,
      max_grant_and_archived_synthetic_bene_deduped_count,
      max_token_real_bene_deduped_count,
      max_token_synthetic_bene_deduped_count,
      max_token_table_count,
      max_token_archived_table_count,
      max_global_apps_active_cnt,
      max_global_apps_inactive_cnt,
      max_global_apps_require_demographic_scopes_cnt,
      max_global_developer_count,
      max_global_developer_distinct_organization_name_count,
      max_global_developer_with_first_api_call_count,
      max_global_developer_with_registered_app_count
    ORDER BY
      start_date ASC
),

v1_fhir_events AS (
  select
    time_of_event,
    path,
    fhir_id_v2 AS fhir_id,
    req_qparam_lastupdated
  from
    perf_mon_events
  WHERE
    (
      type = 'request_response_middleware'
      and vpc = '${ENV}'
      and request_method = 'GET'
      and path LIKE '/v1/fhir%'
      and response_code = 200
      AND
        app_name NOT IN ('TestApp', 'BlueButton Client (Test - Internal Use Only)',
                     'MyMedicare PROD', 'new-relic')
    )
),

v2_fhir_events AS (
  select
    time_of_event,
    path,
    fhir_id_v2 AS fhir_id,
    req_qparam_lastupdated
  from
    perf_mon_events
  WHERE
    (
      type = 'request_response_middleware'
      and vpc = '${ENV}'
      and request_method = 'GET'
      and path LIKE '/v2/fhir%'
      and response_code = 200
      AND
        app_name NOT IN ('TestApp', 'BlueButton Client (Test - Internal Use Only)',
                     'MyMedicare PROD', 'new-relic')
    )
),

auth_events AS (
  select
    time_of_event,
    auth_require_demographic_scopes,
    auth_crosswalk_action,
    auth_share_demographic_scopes,
    auth_status,
    share_demographic_scopes,
    allow,
    json_extract(user, '$$.crosswalk.fhir_id_v2') as fhir_id
  from
    perf_mon_events
  WHERE
    (
      type = 'Authorization'
      and vpc = '${ENV}'
      AND
        auth_app_name NOT IN ('TestApp', 'BlueButton Client (Test - Internal Use Only)',
                     'MyMedicare PROD', 'new-relic')
    )
)


SELECT 
  t0.*,
  (
    total_crosswalk_real_bene - app_all_grant_real_bene
  ) diff_total_crosswalk_vs_grant_and_archived_real_bene,
  (
    total_crosswalk_synthetic_bene - app_all_grant_synthetic_bene
  ) diff_total_crosswalk_vs_grant_and_archived_synthetic_bene,

  /* V1 FHIR resource stats top level */
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_call_real_count')
        AND
          try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v1_call_real_count,

  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_call_synthetic_count')
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v1_call_synthetic_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_eob_call_real_count')
        and path LIKE '/v1/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v1_eob_call_real_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_eob_call_synthetic_count')
        and path LIKE '/v1/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v1_eob_call_synthetic_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_coverage_call_real_count')
        and path LIKE '/v1/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v1_coverage_call_real_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_coverage_call_synthetic_count')
        and path LIKE '/v1/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v1_coverage_call_synthetic_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_patient_call_real_count')
        and path LIKE '/v1/fhir/Patient%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v1_patient_call_real_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_patient_call_synthetic_count')
        and path LIKE '/v1/fhir/Patient%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v1_patient_call_synthetic_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_metadata_call_count')
        and path LIKE '/v1/fhir/metadata%'
      )
  ) as fhir_v1_metadata_call_count,
  /* V1 since (lastUpdated) stats top level */
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_eob_since_call_real_count')
        and path LIKE '/v1/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) > 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v1_eob_since_call_real_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_eob_since_call_synthetic_count')
        and path LIKE '/v1/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) < 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v1_eob_since_call_synthetic_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_coverage_since_call_real_count')
        and path LIKE '/v1/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) > 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v1_coverage_since_call_real_count,
  (
    select
      count(*)
    from
      v1_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v1_coverage_since_call_synthetic_count')
        and path LIKE '/v1/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) < 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v1_coverage_since_call_synthetic_count,
  /* V2 FHIR resource stats top level */
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_call_real_count')
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v2_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_call_synthetic_count')
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v2_call_synthetic_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_eob_call_real_count')
        and path LIKE '/v2/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v2_eob_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_eob_call_synthetic_count')
        and path LIKE '/v2/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v2_eob_call_synthetic_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_coverage_call_real_count')
        and path LIKE '/v2/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v2_coverage_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_coverage_call_synthetic_count')
        and path LIKE '/v2/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v2_coverage_call_synthetic_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_patient_call_real_count')
        and path LIKE '/v2/fhir/Patient%'
        and try_cast(fhir_id as BIGINT) > 0
      )
  ) as fhir_v2_patient_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_patient_call_synthetic_count')
        and path LIKE '/v2/fhir/Patient%'
        and try_cast(fhir_id as BIGINT) < 0
      )
  ) as fhir_v2_patient_call_synthetic_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_metadata_call_count')
        and path LIKE '/v2/fhir/metadata%'
      )
  ) as fhir_v2_metadata_call_count,
  /* V2 since (lastUpdated) stats top level */
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_eob_since_call_real_count')
        and path LIKE '/v2/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) > 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v2_eob_since_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_eob_since_call_synthetic_count')
        and path LIKE '/v2/fhir/ExplanationOfBenefit%'
        and try_cast(fhir_id as BIGINT) < 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v2_eob_since_call_synthetic_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_coverage_since_call_real_count')
        and path LIKE '/v2/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) > 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v2_coverage_since_call_real_count,
  (
    select
      count(*)
    from
      v2_fhir_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'fhir_v2_coverage_since_call_synthetic_count')
        and path LIKE '/v2/fhir/Coverage%'
        and try_cast(fhir_id as BIGINT) < 0
        and req_qparam_lastupdated != ''
      )
  ) as fhir_v2_coverage_since_call_synthetic_count,
  /* AUTH and demographic scopes stats top level */
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_ok_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
      )
  ) as auth_ok_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_ok_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
      )
  ) as auth_ok_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_fail_or_deny_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and auth_status = 'FAIL'
      )
  ) as auth_fail_or_deny_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_fail_or_deny_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and auth_status = 'FAIL'
      )
  ) as auth_fail_or_deny_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_sharing_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'True'
      )
  ) as auth_demoscope_required_choice_sharing_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_sharing_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'True'
      )
  ) as auth_demoscope_required_choice_sharing_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_not_sharing_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'False'
      )
  ) as auth_demoscope_required_choice_not_sharing_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_not_sharing_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'True'
        and share_demographic_scopes = 'False'
      )
  ) as auth_demoscope_required_choice_not_sharing_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_deny_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and allow = False
        and auth_require_demographic_scopes = 'True'
      )
  ) as auth_demoscope_required_choice_deny_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_required_choice_deny_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and allow = False
        and auth_require_demographic_scopes = 'True'
      )
  ) as auth_demoscope_required_choice_deny_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_not_required_not_sharing_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'False'
      )
  ) as auth_demoscope_not_required_not_sharing_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_not_required_not_sharing_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and auth_status = 'OK'
        and allow = True
        and auth_require_demographic_scopes = 'False'
      )
  ) as auth_demoscope_not_required_not_sharing_synthetic_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_not_required_deny_real_bene_count')
        and try_cast(fhir_id as BIGINT) > 0
        and allow = False
        and auth_require_demographic_scopes = 'False'
      )
  ) as auth_demoscope_not_required_deny_real_bene_count,
  (
    select
      count(*)
    from
      auth_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'auth_demoscope_not_required_deny_synthetic_bene_count')
        and try_cast(fhir_id as BIGINT) < 0
        and allow = False
        and auth_require_demographic_scopes = 'False'
      )
  ) as auth_demoscope_not_required_deny_synthetic_bene_count,
  (
    select
      count(*)
    from
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'sdk_requests_python_count')
        AND req_header_bluebutton_sdk = 'python'
      )
  ) as sdk_requests_python_count,
  (
    select
      count(*)
    from
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'sdk_requests_node_count')
        AND req_header_bluebutton_sdk = 'node'
      )
  ) as sdk_requests_node_count

FROM
  (
    SELECT 
      *
    FROM global_state_metrics_per_app_for_max_group_timestamp
  ) t0

  LEFT JOIN
  (
    SELECT 
      *
    FROM global_state_metrics_for_max_group_timestamp
  ) t1 
  ON t1.max_group_timestamp = t0.max_group_timestamp
