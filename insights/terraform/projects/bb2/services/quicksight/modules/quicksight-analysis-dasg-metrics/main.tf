data "aws_caller_identity" "current" {}

locals {
  account_id       = data.aws_caller_identity.current.account_id
  current_user_arn = data.aws_caller_identity.current.arn
}


resource "aws_quicksight_analysis" "quicksight_analysis_dasg_metrics" {
  analysis_id = var.id
  name        = var.name

  permissions {
    principal = "arn:aws:quicksight:us-east-1:${local.account_id}:${var.quicksight_groupname_owners}"
    actions   = [
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
    actions   = [
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

    data_set_identifiers_declarations {
      data_set_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:dataset/${var.data_set_prod_per_app_id}"
      identifier   = "prod_global_state_per_app"
    }
    data_set_identifiers_declarations {
      data_set_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:dataset/${var.data_set_impl_id}"
      identifier   = "impl_global_state"
    }
    data_set_identifiers_declarations {
      data_set_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:dataset/${var.data_set_perf_mon_id}"
      identifier   = "events_prod_perf_mon"
    }
    data_set_identifiers_declarations {
      data_set_arn = "arn:aws:quicksight:us-east-1:${local.account_id}:dataset/${var.data_set_prod_id}"
      identifier   = "prod_global_state"
    }

    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(({total_grant_and_archived_real_bene_deduped_count} - 468158) / (615000 - 468158), 3)"
      name                = "bene_served_progress_to_target"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "Difference(sum({app_active_registered}), [{report_date} ASC], -1)"
      name                = "third_party_apps_served_wow"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "difference(sum({total_grant_and_archived_real_bene_deduped_count}), [{report_date} ASC], -1)"
      name                = "bene_served_wow"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(({fhir_v1_coverage_since_call_real_count} + {fhir_v1_eob_since_call_real_count} + {fhir_v2_coverage_since_call_real_count} + {fhir_v2_eob_since_call_real_count}) / ({fhir_v1_coverage_call_real_count} + {fhir_v1_eob_call_real_count} + {fhir_v1_coverage_call_real_count} + {fhir_v2_eob_call_real_count}) , 3)"
      name                = "total_v1_v2_calls_using_since_param_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v1_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v1_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v1_coverage_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v1_coverage_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v1_eob_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v1_eob_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v1_patient_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v1_patient_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v2_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v2_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v2_coverage_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v2_coverage_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v2_eob_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v2_eob_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v2_patient_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v2_patient_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v3_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v3_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v3_coverage_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v3_coverage_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v3_eob_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v3_eob_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v3_patient_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v3_patient_call_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({fhir_v3_generate_insurance_card_call_real_count}), [{report_date} ASC], -1), 3)"
      name                = "fhir_v3_generate_insurance_card_call_real_count_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round(percentDifference(sum({total_grant_and_archived_real_bene_deduped_count}), [{report_date} ASC], -1), 3)"
      name                = "bene_served_wow_percent"
    }
    calculated_fields {
      data_set_identifier = "prod_global_state"
      expression          = "round({auth_demoscope_required_choice_not_sharing_real_bene_count} / ({auth_demoscope_required_choice_sharing_real_bene_count} + {auth_demoscope_required_choice_not_sharing_real_bene_count}) , 3)"
      name                = "auth_bene_chose_not_to_share_demographic_scopes_percent"
    }

    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "b7405b1c-98c4-4d19-b212-085d6773c75e"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "e6b27a89-279d-4d80-a718-c334e0688a18"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 2
          time_granularity    = "MONTH"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope      = "SELECTED_VISUALS"
            sheet_id   = "afaae03e-b8f7-47c2-a7eb-e1935ed05ee0"
            visual_ids = [
              "7a018458-8312-47f0-9cb9-6d340ff29d7d",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "ee1c03da-a3e8-4cde-beb6-5aac9763208b"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "ee16ace7-ea55-41c2-981d-b48a650f98ff"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 2
          time_granularity    = "MONTH"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "prod_global_state"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope      = "SELECTED_VISUALS"
            sheet_id   = "afaae03e-b8f7-47c2-a7eb-e1935ed05ee0"
            visual_ids = [
              "0948cc12-cc79-4213-95cb-3a1f77599152",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "143560cc-d28c-4984-a4d7-62fdedac30e4"
      status          = "ENABLED"

      filters {
        relative_dates_filter {
          filter_id           = "99770d4c-db83-4ab6-a114-0d9944d17718"
          minimum_granularity = "DAY"
          null_option         = "NON_NULLS_ONLY"
          relative_date_type  = "LAST"
          relative_date_value = 2
          time_granularity    = "MONTH"

          anchor_date_configuration {
            anchor_option = "NOW"
          }

          column {
            column_name         = "report_date"
            data_set_identifier = "impl_global_state"
          }
        }
      }

      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope      = "SELECTED_VISUALS"
            sheet_id   = "42c76446-8b34-4221-860b-9e38a2f164c6"
            visual_ids = [
              "a32bb923-757d-42a5-8e2e-f9a35dafb81c",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "1aa6fdf9-e9f2-40e5-bb80-e73d0dd4a3e5"
      status          = "ENABLED"

      filters {

        relative_dates_filter {
          filter_id           = "eeadfc95-76f2-4e43-9d9a-d43a35ac97a8"
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
            scope      = "SELECTED_VISUALS"
            sheet_id   = "8ce46ef8-3288-4045-a12e-836dea601e0b"
            visual_ids = [
              "9c24246d-c46a-41b1-a0ef-ed9892d0d80f",
            ]
          }
        }
      }
    }
    filter_groups {
      cross_dataset   = "SINGLE_DATASET"
      filter_group_id = "bee9241c-3ad2-42ac-b7a8-7a80b7b8ccbc"
      status          = "ENABLED"

      filters {
        category_filter {
          filter_id = "89db5a12-7e42-4407-a82c-b1de5cb14643"

          configuration {
            filter_list_configuration {
              category_values = [
                "en-us",
                "es-mx"
              ]
              match_operator = "CONTAINS"
            }
          }

          column {
            column_name         = "req_qparam_lang"
            data_set_identifier = "events_prod_perf_mon"
          }
        }
      }
      scope_configuration {
        selected_sheets {
          sheet_visual_scoping_configurations {
            scope      = "SELECTED_VISUALS"
            sheet_id   = "4595cab3-1c2d-4544-b922-29b846c5f2eb"
            visual_ids = [
              "bd76b260-b4ac-4912-8512-7e7217ad2fa0",
            ]
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Engagement - Production"
      sheet_id     = "afaae03e-b8f7-47c2-a7eb-e1935ed05ee0"

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 36
              element_id   = "7a018458-8312-47f0-9cb9-6d340ff29d7d"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 11
            }
            elements {
              column_index = "0"
              column_span  = 36
              element_id   = "0948cc12-cc79-4213-95cb-3a1f77599152"
              element_type = "VISUAL"
              row_index    = "11"
              row_span     = 16
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "0948cc12-cc79-4213-95cb-3a1f77599152"

          chart_configuration {
            field_options {
              data_path_options {
                width = "298px"

                data_path_list {
                  field_id    = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 FHIR Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_call_real_count.9.1642010221063"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 FHIR Calls Made WoW %"
                field_id     = "7aae197f-153d-4fc8-aee0-38cfd0ce0ba8.9.1641593134978"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 EOB Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_eob_call_real_count.10.1642010261765"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 EOB Calls Made WoW %"
                field_id     = "4c7cf68b-9e10-4319-997a-fbbe4c219ae7.11.1641910995279"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 COVERAGE Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_coverage_call_real_count.11.1642010283003"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 COVERAGE Calls Made WoW %"
                field_id     = "7c91d4cf-eab9-437e-91e2-ed038e3169ff.13.1641911013980"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 PATIENT Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_patient_call_real_count.12.1642010288803"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 PATIENT Calls Made WoW %"
                field_id     = "dde3b7a1-cb5b-42eb-badc-20ec2b76208d.15.1641911024282"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 FHIR Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_call_real_count.13.1642010306327"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 FHIR Calls Made WoW %"
                field_id     = "cd5cc33b-3cc1-4c3d-bf62-3ee0bb6bbec0.17.1641911037918"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 EOB Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_eob_call_real_count.14.1642010322212"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 EOB Calls Made WoW %"
                field_id     = "850e0507-340a-444b-ba88-503db0a66613.19.1641911044118"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 COVERAGE Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_coverage_call_real_count.15.1642010329771"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 COVERAGE Calls Made WoW %"
                field_id     = "ae06d4ea-be61-42d8-a6c1-95f63afaf228.14.1641912222931"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 PATIENT Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_patient_call_real_count.16.1642010344299"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V2 PATIENT Calls Made WoW %"
                field_id     = "d410f8bf-842b-4885-aadf-05a365ea4353.16.1641912230066"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V1 & V2 Calls Using SINCE Parameter %"
                field_id     = "94e747da-b933-4faf-9883-46d2c43caa87.17.1642013219568"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 FHIR Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_call_real_count.18.1770146014844"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 FHIR Calls Made WoW %"
                field_id     = "cd5cc33b-3cc1-4c3d-bf62-3ee0bb6bbec0.17.1641911037918"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 EOB Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_eob_call_real_count.19.1770146626075"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 EOB Calls Made WoW %"
                field_id     = "850e0507-340a-444b-ba88-503db0a66613.19.1641911044118"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 COVERAGE Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_coverage_call_real_count.20.1770146636223"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 COVERAGE Calls Made WoW %"
                field_id     = "ae06d4ea-be61-42d8-a6c1-95f63afaf228.14.1641912222931"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 PATIENT Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_patient_call_real_count.21.1770146641503"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 PATIENT Calls Made WoW %"
                field_id     = "d410f8bf-842b-4885-aadf-05a365ea4353.16.1641912230066"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 Generate Insurance Card Calls Made"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_generate_insurance_card_call_real_count.26.1772213941067"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "TOTAL V3 Generate Insurance Card Calls Made WoW %"
                field_id     = "db26ea0b-c755-4881-8152-ee1ec448f042.27.1772214152630"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    date_granularity = "DAY"
                    field_id         = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_call_real_count.9.1642010221063"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v1_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "7aae197f-153d-4fc8-aee0-38cfd0ce0ba8.9.1641593134978"

                    column {
                      column_name         = "fhir_v1_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_eob_call_real_count.10.1642010261765"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v1_eob_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "4c7cf68b-9e10-4319-997a-fbbe4c219ae7.11.1641910995279"

                    column {
                      column_name         = "fhir_v1_eob_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_coverage_call_real_count.11.1642010283003"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v1_coverage_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "7c91d4cf-eab9-437e-91e2-ed038e3169ff.13.1641911013980"

                    column {
                      column_name         = "fhir_v1_coverage_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v1_patient_call_real_count.12.1642010288803"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v1_patient_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "dde3b7a1-cb5b-42eb-badc-20ec2b76208d.15.1641911024282"

                    column {
                      column_name         = "fhir_v1_patient_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_call_real_count.13.1642010306327"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v2_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "cd5cc33b-3cc1-4c3d-bf62-3ee0bb6bbec0.17.1641911037918"

                    column {
                      column_name         = "fhir_v2_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_eob_call_real_count.14.1642010322212"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v2_eob_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "850e0507-340a-444b-ba88-503db0a66613.19.1641911044118"

                    column {
                      column_name         = "fhir_v2_eob_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_coverage_call_real_count.15.1642010329771"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v2_coverage_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ae06d4ea-be61-42d8-a6c1-95f63afaf228.14.1641912222931"

                    column {
                      column_name         = "fhir_v2_coverage_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v2_patient_call_real_count.16.1642010344299"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v2_patient_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "d410f8bf-842b-4885-aadf-05a365ea4353.16.1641912230066"

                    column {
                      column_name         = "fhir_v2_patient_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "94e747da-b933-4faf-9883-46d2c43caa87.17.1642013219568"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "total_v1_v2_calls_using_since_param_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_call_real_count.18.1770146014844"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v3_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "cd5cc33b-3cc1-4c3d-bf62-3ee0bb6bbec0.17.1641911037918"

                    column {
                      column_name         = "fhir_v3_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_eob_call_real_count.19.1770146626075"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v3_eob_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "850e0507-340a-444b-ba88-503db0a66613.19.1641911044118"

                    column {
                      column_name         = "fhir_v3_eob_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_coverage_call_real_count.20.1770146636223"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v3_coverage_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "ae06d4ea-be61-42d8-a6c1-95f63afaf228.14.1641912222931"

                    column {
                      column_name         = "fhir_v3_coverage_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.fhir_v3_patient_call_real_count.21.1770146641503"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "fhir_v3_patient_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "d410f8bf-842b-4885-aadf-05a365ea4353.16.1641912230066"

                    column {
                      column_name         = "fhir_v3_patient_call_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
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
                      column_name         = "fhir_v3_generate_insurance_card_call_real_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "db26ea0b-c755-4881-8152-ee1ec448f042.27.1772214152630"

                    column {
                      column_name         = "fhir_v3_generate_insurance_card_call_real_count_wow_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                height = 40
              }

              column_header_style {
                height = 40
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-subtitle>\n  <inline font-size=\"10px\">Metrics Legend: https://confluence.cms.gov/display/BB2/BFD-Insights#BFDInsights-metrics-legend</inline>\n</visual-subtitle>"
            }
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <inline font-size=\"16px\">Production FHIR Resources V1 &amp; V2</inline>\n  <br/>\n  <inline font-size=\"12px\">Past 2-months. Filter on report_date to see more/less.</inline>\n</visual-title>"
            }
          }
        }
      }
      visuals {
        pivot_table_visual {
          visual_id = "7a018458-8312-47f0-9cb9-6d340ff29d7d"

          chart_configuration {
            field_options {
              data_path_options {
                # BUG NOTE: With AWS provider v5.29.0
                #   This setting does not currently work to set.
                width = "288px"

                data_path_list {
                  field_id    = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Enrollees served (life to date cumulative)"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.total_grant_and_archived_real_bene_deduped_count.8.1641487524533"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Enrollees served-WoW Gains/Losses"
                field_id     = "2ef3e5ec-fd0d-49c7-9c62-9f2ed827f260.11.1641418258046"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Enrollees served-Progress to FY2025 Target 615K"
                field_id     = "0581b3bf-d5ad-47a1-ad31-c6cc9c1408ec.4.1641486129331"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Third party apps served (currently active)"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_registered.6.1641486857245"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Third party apps served-with >25 beneficiaries (currently active)"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_bene_cnt_gt25.8.1641486863054"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Third party apps served-With 25 or fewer beneficiaries (currently active)"
                field_id     = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_bene_cnt_le25.7.1641486861801"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Third party apps served (life to date cumulative)"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.app_all.11.1691759858662"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Percent of enrollees who chose NOT to share demographic information"
                field_id     = "81f529e4-ddf9-4076-9713-d4bef77a4659.8.1642012374409"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Enrollees NOT sharing demographic"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.auth_demoscope_required_choice_not_sharing_real_bene_count.9.1658786337009"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Enrollees  sharing demographic"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.auth_demoscope_required_choice_sharing_real_bene_count.10.1658786343538"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "NODE SDK Requests"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.sdk_requests_node_count.14.1691762512237"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "PYTHON SDK Requests"
                field_id     = "395e9e0d-ae34-480e-8f58-5fe90f34425e.sdk_requests_python_count.15.1691762513768"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.total_grant_and_archived_real_bene_deduped_count.8.1641487524533"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "total_grant_and_archived_real_bene_deduped_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "2ef3e5ec-fd0d-49c7-9c62-9f2ed827f260.11.1641418258046"

                    column {
                      column_name         = "bene_served_wow"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "0581b3bf-d5ad-47a1-ad31-c6cc9c1408ec.4.1641486129331"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "bene_served_progress_to_target"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          suffix = "%"
                          negative_value_configuration {
                            display_mode = "NEGATIVE"
                          }
                          separator_configuration {
                            decimal_separator = "DOT"
                            thousands_separator {
                              symbol     = "COMMA"
                              visibility = "VISIBLE"
                            }
                          }
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_registered.6.1641486857245"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_active_registered"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_bene_cnt_gt25.8.1641486863054"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_active_bene_cnt_gt25"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.app_active_bene_cnt_le25.7.1641486861801"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_active_bene_cnt_le25"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.app_all.11.1691759858662"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_all"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "81f529e4-ddf9-4076-9713-d4bef77a4659.8.1642012374409"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "auth_bene_chose_not_to_share_demographic_scopes_percent"
                      data_set_identifier = "prod_global_state"
                    }

                    format_configuration {
                      numeric_format_configuration {
                        percentage_display_format_configuration {
                          null_value_format_configuration {
                            null_string = "null"
                          }
                        }
                      }
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.auth_demoscope_required_choice_not_sharing_real_bene_count.9.1658786337009"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "auth_demoscope_required_choice_not_sharing_real_bene_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.auth_demoscope_required_choice_sharing_real_bene_count.10.1658786343538"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "auth_demoscope_required_choice_sharing_real_bene_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.sdk_requests_node_count.14.1691762512237"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "sdk_requests_node_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "395e9e0d-ae34-480e-8f58-5fe90f34425e.sdk_requests_python_count.15.1691762513768"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "sdk_requests_python_count"
                      data_set_identifier = "prod_global_state"
                    }
                  }
                }
              }
            }
            sort_configuration {
              field_sort_options {
                field_id = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "83e6f763-d91d-4aab-9aae-8186a0985ed9.report_date.0.1641411826990"
                  }
                }
              }
            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                height = 40
              }

              column_header_style {
                height = 40
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-subtitle>\n  <inline font-size=\"10px\">Metrics Legend: https://confluence.cms.gov/display/BB2/BFD-Insights#BFDInsights-metrics-legend</inline>\n</visual-subtitle>"
            }
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <inline font-size=\"16px\">Production Enrollees and Applications</inline>\n  <br/>\n  <inline font-size=\"12px\">Past 2-months. Filter on report_date to see more/less.</inline>\n</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Engagement - Sandbox"
      sheet_id     = "42c76446-8b34-4221-860b-9e38a2f164c6"

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 36
              element_id   = "a32bb923-757d-42a5-8e2e-f9a35dafb81c"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 12
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "a32bb923-757d-42a5-8e2e-f9a35dafb81c"

          chart_configuration {
            field_options {
              data_path_options {
                width = "253px"

                data_path_list {
                  field_id    = "8ff7582f-723f-4213-8e76-325f06d8986d.report_date.4.1706288115763"
                  field_value = "Report Date"
                }
              }
              selected_field_options {
                custom_label = "Report Date"
                field_id     = "8ff7582f-723f-4213-8e76-325f06d8986d.report_date.4.1706288115763"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Developers w/ Registered Application"
                field_id     = "f310934e-c316-4c0e-96ea-f61276d89bab.total_developer_with_registered_app_count.4.1662127374754"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Developers with First API Call"
                field_id     = "f310934e-c316-4c0e-96ea-f61276d89bab.total_developer_with_first_api_call_count.5.1656688567231"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Developers Total V1 FHIR Calls"
                field_id     = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v1_call_synthetic_count.7.1701907139093"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Developers Total V2 FHIR Calls"
                field_id     = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v2_call_synthetic_count.3.1701908769910"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Developers Total V3 FHIR Calls"
                field_id     = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v3_call_synthetic_count.5.1770145756562"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {

                columns {
                  date_dimension_field {
                    field_id = "8ff7582f-723f-4213-8e76-325f06d8986d.report_date.4.1706288115763"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }


                values {
                  numerical_measure_field {
                    field_id = "f310934e-c316-4c0e-96ea-f61276d89bab.total_developer_with_registered_app_count.4.1662127374754"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "total_developer_with_registered_app_count"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "f310934e-c316-4c0e-96ea-f61276d89bab.total_developer_with_first_api_call_count.5.1656688567231"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "total_developer_with_first_api_call_count"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v1_call_synthetic_count.7.1701907139093"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_all_fhir_v1_call_synthetic_count"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v2_call_synthetic_count.3.1701908769910"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_all_fhir_v2_call_synthetic_count"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "8ff7582f-723f-4213-8e76-325f06d8986d.app_all_fhir_v3_call_synthetic_count.5.1770145756562"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_all_fhir_v3_call_synthetic_count"
                      data_set_identifier = "impl_global_state"
                    }
                  }
                }
              }
            }
            sort_configuration {

              field_sort_options {
                field_id = "8ff7582f-723f-4213-8e76-325f06d8986d.report_date.4.1706288115763"

                sort_by {
                  field {
                    direction = "ASC"
                    field_id  = "8ff7582f-723f-4213-8e76-325f06d8986d.report_date.4.1706288115763"
                  }
                }
              }

            }
            table_options {
              metric_placement = "ROW"

              cell_style {
                height = 40
              }

              column_header_style {
                height = 40
              }

            }
          }

          subtitle {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-subtitle>\n  <inline font-size=\"10px\">Metrics Legend: https://confluence.cms.gov/display/BB2/BFD-Insights#BFDInsights-metrics-legend</inline>\n</visual-subtitle>"
            }
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <inline font-size=\"14px\">Engagement - Sandbox</inline>\n  <br/>\n  <inline font-size=\"12px\">Past 2-months. Filter on report_date to see more/less.</inline>\n</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Top Organizations by Enrollee Count"
      sheet_id     = "8ce46ef8-3288-4045-a12e-836dea601e0b"

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 31
              element_id   = "9c24246d-c46a-41b1-a0ef-ed9892d0d80f"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 12
            }
          }
        }
      }

      visuals {
        table_visual {
          visual_id = "9c24246d-c46a-41b1-a0ef-ed9892d0d80f"

          chart_configuration {
            field_options {
              #order = []

              selected_field_options {
                custom_label = "Organization (can have multiple apps)"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.0.1667239330557"
                width        = "300px"
              }
              selected_field_options {
                custom_label = " Enrollees served (life to date cumulative)"
                field_id     = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_real_bene_deduped_count.2.1667239330557"
                width        = "300px"
              }
            }
            field_wells {
              table_aggregated_field_wells {
                group_by {
                  categorical_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_user_organization.0.1667239330557"

                    column {
                      column_name         = "app_user_organization"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                group_by {
                  date_dimension_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.report_date.2.1706289461891"

                    column {
                      column_name         = "report_date"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_real_bene_deduped_count.2.1667239330557"

                    aggregation_function {
                      simple_numerical_aggregation = "SUM"
                    }

                    column {
                      column_name         = "app_grant_and_archived_real_bene_deduped_count"
                      data_set_identifier = "prod_global_state_per_app"
                    }
                  }
                }
              }
            }
            sort_configuration {
              row_sort {
                field_sort {
                  direction = "DESC"
                  field_id  = "73bc51bc-79d0-43d6-8757-eae5d7dea826.app_grant_and_archived_real_bene_deduped_count.2.1667239330557"
                }
              }
            }
            table_options {

              cell_style {
                height = 25
              }
            }
          }

          subtitle {
            visibility = "VISIBLE"
          }

          title {
            visibility = "VISIBLE"

            format_text {
              rich_text = "<visual-title>\n  <block align=\"center\">\n    Top Organizations by Enrollees\n    <b> served (life to date cumulative)</b>\n  </block>\n  <br/>\n  <block align=\"center\">\n    <b>For last Report Date</b>\n  </block>\n</visual-title>"
            }
          }
        }
      }
    }
    sheets {
      content_type = "INTERACTIVE"
      name         = "Permission Screen Language"
      sheet_id     = "4595cab3-1c2d-4544-b922-29b846c5f2eb"

      layouts {
        configuration {
          grid_layout {
            elements {
              column_index = "0"
              column_span  = 20
              element_id   = "bd76b260-b4ac-4912-8512-7e7217ad2fa0"
              element_type = "VISUAL"
              row_index    = "0"
              row_span     = 11
            }
          }
        }
      }

      visuals {
        pivot_table_visual {
          visual_id = "bd76b260-b4ac-4912-8512-7e7217ad2fa0"

          chart_configuration {
            field_options {
              data_path_options {
                width = "244px"

                data_path_list {
                  field_id    = "87e818d6-910f-43fc-a8e5-16483bbce956.time_of_event.0.1718642543721"
                  field_value = "2024-07-08 00:00:00.000"
                }
                data_path_list {
                  field_id    = "87e818d6-910f-43fc-a8e5-16483bbce956.req_user_id.2.1718642578144"
                  field_value = "req_user_id"
                }
              }
              selected_field_options {
                custom_label = "Language"
                field_id     = "87e818d6-910f-43fc-a8e5-16483bbce956.req_qparam_lang.1.1718642562990"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Date"
                field_id     = "87e818d6-910f-43fc-a8e5-16483bbce956.time_of_event.0.1718642543721"
                visibility   = "VISIBLE"
              }
              selected_field_options {
                custom_label = "Total Unique Enrollees"
                field_id     = "87e818d6-910f-43fc-a8e5-16483bbce956.req_user_id.2.1718642578144"
                visibility   = "VISIBLE"
              }
            }
            field_wells {
              pivot_table_aggregated_field_wells {
                columns {
                  date_dimension_field {
                    date_granularity = "WEEK"
                    field_id         = "87e818d6-910f-43fc-a8e5-16483bbce956.time_of_event.0.1718642543721"

                    column {
                      column_name         = "time_of_event"
                      data_set_identifier = "events_prod_perf_mon"
                    }
                  }
                }
                rows {
                  categorical_dimension_field {
                    field_id = "87e818d6-910f-43fc-a8e5-16483bbce956.req_qparam_lang.1.1718642562990"

                    column {
                      column_name         = "req_qparam_lang"
                      data_set_identifier = "events_prod_perf_mon"
                    }
                  }
                }
                values {
                  numerical_measure_field {
                    field_id = "87e818d6-910f-43fc-a8e5-16483bbce956.req_user_id.2.1718642578144"

                    aggregation_function {
                      simple_numerical_aggregation = "DISTINCT_COUNT"
                    }

                    column {
                      column_name         = "req_user_id"
                      data_set_identifier = "events_prod_perf_mon"
                    }
                  }
                }
              }
            }
            sort_configuration {
            }

            table_options {
              column_names_visibility = "VISIBLE"
            }
            total_options {
              row_subtotal_options {
                custom_label = "<<$aws:subtotalDimension>> Subtotal"
                totals_visibility = "VISIBLE"
                metric_header_cell_style {
                }
                total_cell_style {
                }
                value_cell_style {
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
              rich_text = <<-EOT
                <visual-title>
                  <inline color="#21a0d7" font-size="15px">Permission Screen Language</inline>
                </visual-title>
              EOT
            }
          }
        }
      }
    }
  }
}
