data "external" "edt_or_est" {
  program = ["bash", "${path.module}/get_est_or_edt.sh"]
}
