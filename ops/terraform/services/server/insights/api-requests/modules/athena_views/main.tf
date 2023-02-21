locals {
  manage_views_command = <<-EOF
chmod +x ${path.module}/manage_athena_view.sh
${path.module}/manage_athena_view.sh
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
        env = var.env
      }
    )
  }
  view_to_triggers = {
    for view in keys(local.view_to_filepath) :
    view => {
      view = view
      md5  = md5(local.view_to_templated_sql[view])

      # External references from destroy provisioners are not allowed -
      # they may only reference attributes of the related resource.
      command       = local.manage_views_command
      database_name = var.database_name
      region        = var.region
    }
  }
}

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

resource "null_resource" "athena_view_api_requests_by_bene" {
  depends_on = [null_resource.athena_view_api_requests]
  triggers   = local.view_to_triggers.api_requests_by_bene

  provisioner "local-exec" {
    command = self.triggers.command

    environment = {
      REGION         = var.region
      DATABASE_NAME  = var.database_name
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

resource "null_resource" "athena_view_new_benes_by_day" {
  depends_on = [
    null_resource.athena_view_api_requests,
    null_resource.athena_view_api_requests_by_bene
  ]
  triggers = local.view_to_triggers.new_benes_by_day

  provisioner "local-exec" {
    command = self.triggers.command

    environment = {
      REGION         = var.region
      DATABASE_NAME  = var.database_name
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
