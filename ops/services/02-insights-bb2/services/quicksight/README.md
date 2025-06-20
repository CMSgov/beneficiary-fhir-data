# QuickSight Terraform

This README contains information to help with managing the BB2 BFD-Insights AWS Quicksight resources.

See the [README](../../README.md) for general Terraform service and workspace usage info. This README is specific to the Quicksight setup.

## Summary

The Quicksight DataSet and Analysis resources are configured under this directory. These are used to publish related dashboards via the web UI/console.

Prior to setting up Quicksight terraform code for new metrics, the underlying Athena tables must be updated to contain the new fields first. An exception to this is when using existing fields to create a new calculated field type metric. For example, you may have a new metric that is based on a difference or sum of existing metrics.

Instruction for adding new metrics to the Athena tables and related documentation can be found [HERE](../analytics/modules/lambda/update_athena_metric_tables/README.md).


## Naming and Resource Layout

The namings are appended with the workspace used: `test` or `prod` (default is `prod` if not appended).

The namings are prepended with the API enviornment the metrics are for: `test`, `impl` or `prod`

The main datasets and tables contain a row of metrics for each report date. These are for global type metrics across the API for the week. The datasets containing `_per_app_` in the naming have rows of metrics for each one of the 3rd party applications---for each report date. This provides a break down of metrics per application.

Two Terraform workspaces are used:
- Workspace: `test`
  - Is a workspace for development versions of resources.
- Workspace: `prod`
  - Is a workspace for production versions of resources.

The following table shows how the resources are laid out and connect with each other:

| Workspace | Analysis | DataSet | AthenaTable |
| --- | --- | --- | --- |
| test | BB2-DASG-METRICS-test | prod_global_state_test | prod_global_state_test |
| test | BB2-DASG-METRICS-test | impl_global_state_test | impl_global_state_test |
| test | BB2-DASG-METRICS-test | prod_global_state_per_app_test | prod_global_state_per_app_test |
| prod | BB2-DASG-METRICS-prod | prod_global_state_prod | prod_global_state |
| prod | BB2-DASG-METRICS-prod | impl_global_state_prod | impl_global_state |
| prod | BB2-DASG-METRICS-prod | prod_global_state_per_app_prod | prod_global_state_per_app |
| --- | --- | --- | --- |
| test | BB2-PROD-APPLICATIONS-test | prod_global_state_per_app_test | prod_global_state_per_app_test |
| prod | BB2-PROD-APPLICATIONS-prod | prod_global_state_per_app_prod | prod_global_state_per_app |


## Terraform Expected Plan Changes

The Terraform for Quicksight components were originally created manually via the web UI/console. These are now setup via Terraform in this location. During this setup, several issues were worked around or resolved related to Analysis terraform import, plan and apply operations. As a result, some parts of the terrform are not completely working at this time. This requires some final editing via the web UI before publishing a dashboard. This will be outlined in a next section of this README.

Normally, after performing a `terraform apply` there will be a "No changes" status shown when immediately performing a `terraform plan` afterward.

Possible AWS provider bugs (as of version 5.34.0) are encountered and the terraform plan is always showing some changes. However, the resulting Analyses are working OK after a terraform apply. New Terraform AWS provider version releases should be periodically reviewed for resolutions and when corresponding Terraform code can be updated to match.

The expected plan changes are documented below and can be considered the same as if "No changes" was the result.

The following are expected changes for the `quicksight_analysis_dasg_metrics` module resource:

```
      ~ definition {
          ~ sheets {
                name         = "Engagement - Production"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              ~ cell_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              ~ column_header_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              ~ cell_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              ~ column_header_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (1 unchanged block hidden)
            }
          ~ sheets {
                name         = "Engagement - Sandbox"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              ~ cell_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              ~ column_header_style {
                                  - text_wrap = "WRAP" -> null
                                    # (1 unchanged attribute hidden)
                                }

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (1 unchanged block hidden)
            }

            # (23 unchanged blocks hidden)
        }

        # (2 unchanged blocks hidden)
    }
```

The following are expected changes for the `quicksight_analysis_dasg_metrics` module resource:

```
      ~ definition {
          ~ column_configurations {
              ~ role = "DIMENSION" -> "MEASURE"

              ~ column {
                  ~ column_name         = "calc_app_last_active" -> "Dynamic Field"
                    # (1 unchanged attribute hidden)
                }
            }
          ~ column_configurations {
              ~ role = "MEASURE" -> "DIMENSION"

              ~ column {
                  ~ column_name         = "Dynamic Field" -> "calc_app_last_active"
                    # (1 unchanged attribute hidden)
                }
            }
          ~ sheets {
                name         = "PROD Applications"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (3 unchanged attributes hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                    # (2 unchanged attributes hidden)

                                    # (1 unchanged block hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }
                          ~ total_options {
                              ~ row_subtotal_options {
                                    # (1 unchanged attribute hidden)

                                  - total_cell_style {
                                    }
                                }

                                # (3 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (6 unchanged blocks hidden)
            }
          ~ sheets {
                name         = "PROD Single Application"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }
              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (1 unchanged attribute hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (1 unchanged attribute hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (4 unchanged blocks hidden)
            }
          ~ sheets {
                name         = "PROD Applications Enabled/Disabled"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (3 unchanged attributes hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (2 unchanged attributes hidden)

                                    # (1 unchanged block hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }
                          ~ total_options {
                              ~ row_subtotal_options {
                                    # (1 unchanged attribute hidden)

                                  - total_cell_style {
                                    }
                                }

                                # (3 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (3 unchanged blocks hidden)
            }
          ~ sheets {
                name         = "PROD Applications Token Activity"
                # (2 unchanged attributes hidden)

              ~ visuals {
                  ~ pivot_table_visual {
                        # (1 unchanged attribute hidden)

                      ~ chart_configuration {
                          ~ table_options {
                                # (3 unchanged attributes hidden)

                              - row_field_names_style {
                                  - height    = 40 -> null
                                  - text_wrap = "WRAP" -> null
                                }

                              ~ row_header_style {
                                  ~ height                    = 0 -> 40
                                  + text_wrap                 = "WRAP"
                                    # (2 unchanged attributes hidden)

                                    # (1 unchanged block hidden)
                                }

                                # (2 unchanged blocks hidden)
                            }
                          ~ total_options {
                              ~ row_subtotal_options {
                                    # (1 unchanged attribute hidden)

                                  - total_cell_style {
                                    }
                                }

                                # (3 unchanged blocks hidden)
                            }

                            # (3 unchanged blocks hidden)
                        }

                        # (2 unchanged blocks hidden)
                    }
                }

                # (5 unchanged blocks hidden)
            }

            # (86 unchanged blocks hidden)
        }

        # (2 unchanged blocks hidden)
    }
```

## Summary of Analysis Terraform Issues

The following is a description of some issues related to the plan changes **that always show** in both analyses plan outputs. Even after a successful terraform APPLY.

- Some types of `height` values are set to 0 internally; however, setting this in the TF code produces an error during the apply, with a message that the value must be greater than 0.
- Some types of `height` values do not change (Ex. height=40 for row_header_style).
- Some `text_wrap = "WRAP"` values do not change.
- The `column_configurations` shown will flip positions, when trying to matching them in the TF code and after a new apply.
  - For example, if you reorder the TF code for these `column_configurations`, they will flip positions after a new apply. The cause is currently unknown, but the resulting analysis works OK:
  ```
  ~ column_configurations {
    ~ role = "DIMENSION" -> "MEASURE"

      ~ column {
        ~ column_name         = "calc_app_last_active" -> "Dynamic Field"
        # (1 unchanged attribute hidden)
      }
    }
   ~ column_configurations {
       ~ role = "MEASURE" -> "DIMENSION"

       ~ column {
           ~ column_name         = "Dynamic Field" -> "calc_app_last_active"
             # (1 unchanged attribute hidden)
         }
     }
  ```
- The theme setting does not work, but can be easily set up before publishing.

**NOTE**: If you have the analysis open in the web UI after a TF apply, additional changes may show up in the TF plan afterward due to that activity. 

## Publishing Dashboards Using Analyses

The dashboards are published using the corresponding analysis and manually edited via the Quicksight web UI/console. 

After a terraform apply, the following edits may be needed:
- For each sheet and pivot table visual, adjust the row header widths so that they are fully visable.
- Update to the desired THEME via Edit -> Themes.
- When finished, publish to a dashboard using the `PUBLISH` button in the top-right.
  - Choose to replace the target dashboard.
    - Dashboards will have the same name as the analysis.
  - Under "Advanced publish options", enable the "Enable ad hoc filtering" setting. This will allow dashboard users to change date range and other filters of interest to them. This is useful for looking back in the metrics history.


## Pre-Terraform Versions

There are pre-terraform versions of the analyses and dashboards in Quicksight. These have "-nonTF-original-20240311" appended to the resource names. These were previously developed using the Quicksight UI and AWS Console only. So Terraform was not involved in their original development.

These are kept for short term reference and comparison with the new TF versions. For example, there are some sheets that could not be imported and recreated, but it may be possible to import those in the future, if desired.