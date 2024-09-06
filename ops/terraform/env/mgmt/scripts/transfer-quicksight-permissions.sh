#!/usr/bin/env bash
set -euo pipefail

##############################################################################
# This script uses the AWS CLI to transfer a user's permissions for assets
# where they are the sole owner. The permissions are transferred to an admin
# or admin group as defined by `$principal_admin_arn`.
##############################################################################

principal_admin_arn="${PRINCIPAL_ADMIN_ARN}"
readonly principal_admin_arn

aws_account_id="${AWS_ACCOUNT_ID}"
readonly aws_account_id

sole_owner_arn="${SOLE_OWNER_ARN}"
readonly sole_owner_arn

check_user_exists() {
  users_found=$(aws quicksight list-users --aws-account-id "$aws_account_id" --namespace default \
    --query "UserList[?Arn=='$sole_owner_arn']" | jq length)
  if [ "$users_found" != "1" ]; then
    # User was already deleted through some other means. 
    # If we try to search for their assets, it will fail due to a lookup on a nonexistent ARN
    exit 0;
  fi
}

format_asset_permissions() {
  permissions=$1
  echo "[{\"Principal\": \"$principal_admin_arn\",\"Actions\": $permissions}]"
}

search_asset_sole_owner() {
  asset=$1
  query_id=$2
  # it thinks this is an array expansion, but it's just part of the query syntax
  # shellcheck disable=SC1087
  aws quicksight "search-$asset" --aws-account-id "$aws_account_id" \
    --filters "Operator=StringEquals,Name=DIRECT_QUICKSIGHT_SOLE_OWNER,Value=$sole_owner_arn" \
    --query "$query_id[] | [?Status != 'DELETED']"
}

transfer_sole_ownership_of_assets() {
  check_user_exists

  data_sources=$(search_asset_sole_owner "data-sources" "DataSourceSummaries")
  data_sources=$(echo "$data_sources" | jq -r '[.[].DataSourceId] | join(",")')

  data_sets=$(search_asset_sole_owner "data-sets" "DataSetSummaries")
  data_sets=$(echo "$data_sets" | jq -r '[.[].DataSetId] | join(",")')

  analyses=$(search_asset_sole_owner "analyses" "AnalysisSummaryList")
  analyses=$(echo "$analyses" | jq -r '[.[].AnalysisId] | join(",")')

  dashboards=$(search_asset_sole_owner "dashboards" "DashboardSummaryList")
  dashboards=$(echo "$dashboards" | jq -r '[.[].DashboardId] | join(",")')

  folders=$(search_asset_sole_owner "folders" "FolderSummaryList")
  folders=$(echo "$folders" | jq -r '[.[].FolderId] | join(",")')

  data_source_perms=$(format_asset_permissions '["quicksight:UpdateDataSourcePermissions", "quicksight:DescribeDataSourcePermissions", "quicksight:PassDataSource", "quicksight:DescribeDataSource", "quicksight:DeleteDataSource", "quicksight:UpdateDataSource"]')
  data_set_perms=$(format_asset_permissions '["quicksight:DescribeDataSet", "quicksight:DescribeDataSetPermissions", "quicksight:PassDataSet", "quicksight:DescribeIngestion", "quicksight:ListIngestions", "quicksight:UpdateDataSet", "quicksight:DeleteDataSet", "quicksight:CreateIngestion", "quicksight:CancelIngestion", "quicksight:UpdateDataSetPermissions", "quicksight:PutDataSetRefreshProperties", "quicksight:UpdateRefreshSchedule", "quicksight:DeleteRefreshSchedule", "quicksight:DeleteDataSetRefreshProperties", "quicksight:CreateRefreshSchedule", "quicksight:DescribeRefreshSchedule", "quicksight:ListRefreshSchedules", "quicksight:DescribeDataSetRefreshProperties"]')
  analysis_perms=$(format_asset_permissions '["quicksight:RestoreAnalysis", "quicksight:UpdateAnalysisPermissions", "quicksight:DeleteAnalysis", "quicksight:QueryAnalysis", "quicksight:DescribeAnalysisPermissions", "quicksight:DescribeAnalysis", "quicksight:UpdateAnalysis"]')
  dashboard_perms=$(format_asset_permissions '["quicksight:DescribeDashboard", "quicksight:ListDashboardVersions", "quicksight:UpdateDashboardPermissions", "quicksight:QueryDashboard", "quicksight:UpdateDashboard", "quicksight:DeleteDashboard", "quicksight:UpdateDashboardPublishedVersion", "quicksight:DescribeDashboardPermissions"]')
  folder_perms=$(format_asset_permissions '["quicksight:CreateFolder", "quicksight:DescribeFolder", "quicksight:UpdateFolder", "quicksight:DeleteFolder", "quicksight:CreateFolderMembership", "quicksight:DeleteFolderMembership", "quicksight:DescribeFolderPermissions", "quicksight:UpdateFolderPermissions"]')

  IFS=',' read -ra data_sources <<<"$data_sources"
  IFS=',' read -ra data_sets <<<"$data_sets"
  IFS=',' read -ra analyses <<<"$analyses"
  IFS=',' read -ra dashboards <<<"$dashboards"
  IFS=',' read -ra folders <<<"$folders"

  default_args="--no-cli-pager --aws-account-id $aws_account_id"
  for i in "${!data_sources[@]}"; do
    id="${data_sources[i]}"
    aws quicksight update-data-source-permissions $default_args --data-source-id "$id" --grant-permissions "$data_source_perms"
  done
  for i in "${!data_sets[@]}"; do
    id="${data_sets[i]}"
    aws quicksight update-data-set-permissions $default_args --data-set-id "$id" --grant-permissions "$data_set_perms"
  done
  for i in "${!analyses[@]}"; do
    id="${analyses[i]}"
    aws quicksight update-analysis-permissions $default_args --analysis-id "$id" --grant-permissions "$analysis_perms"
  done
  for i in "${!dashboards[@]}"; do
    id="${dashboards[i]}"
    aws quicksight update-dashboard-permissions $default_args --dashboard-id "$id" --grant-permissions "$dashboard_perms"
  done
  for i in "${!folders[@]}"; do
    id="${folders[i]}"
    aws quicksight update-folder-permissions $default_args --folder-id "$id" --grant-permissions "$folder_perms"
  done
}

transfer_sole_ownership_of_assets
