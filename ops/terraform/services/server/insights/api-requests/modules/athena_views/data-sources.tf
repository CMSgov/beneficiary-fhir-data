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
    aws glue get-table --name ${var.source_table_name} \
      --database-name ${var.database_name} |
      yq -o=j '{"version": .Table.VersionId // 0 | tostring}'
    EOF
  ]
}
