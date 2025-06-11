locals {
  manage_views_command = <<-EOF
chmod +x ${path.module}/scripts/manage_athena_view.sh
${path.module}/scripts/manage_athena_view.sh
EOF

  sql_views_dir = "${path.module}/sql_views"
  view_to_filepath = {
    for filename in fileset("${local.sql_views_dir}/", "**/*.sql.tfpl") :
    replace(replace(filename, "/", "_"), ".sql.tfpl", "") => "${local.sql_views_dir}/${filename}"
  }
  view_to_templated_sql = {
    for view, filepath in local.view_to_filepath :
    view => templatefile(
      filepath,
      {
        # replace '-' with '_' in `prod-sbx` to avoid athena errors
        env = replace(local.env, "-", "_")
      }
    )
  }
  view_to_triggers = {
    for view in keys(local.view_to_filepath) :
    view => {
      view = view
      md5  = md5(local.view_to_templated_sql[view])

      # We want this to trigger if the source table's schema changes so that the view is updated
      src_table_version = data.external.src_table_version.result.version

      # External references from destroy provisioners are not allowed - they may only reference
      # attributes of the related resource. This means that in order for destroy local-exec
      # provisioners to reference external data that data needs to be encoded within its resource's
      # triggers, hence why "command", "database_name", and "region" are all included in triggers
      command       = local.manage_views_command
      database_name = module.database.name
      region        = var.region
    }
  }
}

# We need to use an external AWS CLI command as the "aws_glue_catalog_table" data resource does not
# expose the "VersionId" attribute of the table, for no apparent reason. It does expose a
# "schema_version_number", but it is not obvious whether that version number is the same as the
# table's version number which increments whenever the table changes at all. The data resource also
# returns a _list_ of schemas, rather than the current schema, so we would also need to extract the
# latest schema in order to use it as a trigger. Altogether, it's much simpler to simply query the
# version using the CLI
data "external" "src_table_version" {
  program = [
    "bash", "-c",
    <<-EOF
    aws glue get-table --name ${module.glue_table_api_requests.name} \
      --database-name ${module.database.name} |
      yq -o=j '{"version": .Table.VersionId // 0 | tostring}'
    EOF
  ]
}

# Do _not_ remove without first following the instructions for safely removing
# "destroy-time provisioners": https://developer.hashicorp.com/terraform/language/resources/provisioners/syntax#destroy-time-provisioners
# Otherwise, the null_resources that encode the various Athena Views will not run their destroy
# provisioners and therefore those Views will remain indefinitely
resource "null_resource" "athena_view_api_requests" {
  triggers = local.view_to_triggers.api_requests

  provisioner "local-exec" {
    command = self.triggers.command
    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      VIEW_SQL       = local.view_to_templated_sql[self.triggers.view]
      OPERATION_TYPE = "CREATE_VIEW"
    }
  }

  provisioner "local-exec" {
    when    = destroy
    command = self.triggers.command

    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      OPERATION_TYPE = "DESTROY_VIEW"
    }
  }
}

# Do _not_ remove without first following the instructions for safely removing
# "destroy-time provisioners": https://developer.hashicorp.com/terraform/language/resources/provisioners/syntax#destroy-time-provisioners
# Otherwise, the null_resources that encode the various Athena Views will not run their destroy
# provisioners and therefore those Views will remain indefinitely
resource "null_resource" "athena_view_api_requests_by_bene" {
  depends_on = [null_resource.athena_view_api_requests]
  triggers   = local.view_to_triggers.api_requests_by_bene

  provisioner "local-exec" {
    command = self.triggers.command

    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      VIEW_SQL       = local.view_to_templated_sql[self.triggers.view]
      OPERATION_TYPE = "CREATE_VIEW"
    }
  }

  provisioner "local-exec" {
    when    = destroy
    command = self.triggers.command

    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      OPERATION_TYPE = "DESTROY_VIEW"
    }
  }
}

# Do _not_ remove without first following the instructions for safely removing
# "destroy-time provisioners": https://developer.hashicorp.com/terraform/language/resources/provisioners/syntax#destroy-time-provisioners
# Otherwise, the null_resources that encode the various Athena Views will not run their destroy
# provisioners and therefore those Views will remain indefinitely
resource "null_resource" "athena_view_new_benes_by_day" {
  depends_on = [
    null_resource.athena_view_api_requests,
    null_resource.athena_view_api_requests_by_bene
  ]
  triggers = local.view_to_triggers.new_benes_by_day

  provisioner "local-exec" {
    command = self.triggers.command

    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      VIEW_SQL       = local.view_to_templated_sql[self.triggers.view]
      OPERATION_TYPE = "CREATE_VIEW"
    }
  }

  provisioner "local-exec" {
    when    = destroy
    command = self.triggers.command

    environment = {
      REGION         = self.triggers.region
      DATABASE_NAME  = self.triggers.database_name
      VIEW_NAME      = self.triggers.view
      OPERATION_TYPE = "DESTROY_VIEW"
    }
  }
}
