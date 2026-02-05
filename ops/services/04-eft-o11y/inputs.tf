locals {
  eft_outputs = nonsensitive(zipmap(
    [for _, v in data.aws_ssm_parameters_by_path.eft_outputs.names : replace(v, "/.*/(.*)$/", "$1")],
    [for _, v in data.aws_ssm_parameters_by_path.eft_outputs.values : jsondecode(v)]
  ))
}

data "aws_ssm_parameters_by_path" "eft_outputs" {
  path = "/bfd/${local.env}/eft/tf-outputs/"
}
