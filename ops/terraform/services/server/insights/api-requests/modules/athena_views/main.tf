locals {
  sql_views_dir = "${path.module}/sql_views"
  view_to_filepath = {
    for filename in fileset("${local.sql_views_dir}/", "**/*.sql") :
    replace(replace(filename, "/", "_"), ".sql", "") => "${local.sql_views_dir}/${filename}"
  }
}

resource "null_resource" "athena_views" {
  for_each = local.view_to_filepath

  triggers = {
    md5 = filemd5(each.value)

    # External references from destroy provisioners are not allowed -
    # they may only reference attributes of the related resource.
    database_name = var.database_name
    region        = var.region
  }

  provisioner "local-exec" {
    command = <<-EOF
aws athena start-query-execution \
  --region "${var.region}" \
  --output json \
  --query-string file://${each.value} \
  --query-execution-context "Database=${var.database_name}" \
  --work-group "bfd"
EOF
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOF
aws athena start-query-execution \
  --region "${self.triggers.region}" \
  --output json \
  --query-string 'DROP VIEW IF EXISTS ${each.key}' \
  --query-execution-context "Database=${self.triggers.database_name}" \
  --work-group "bfd"
EOF
  }
}
