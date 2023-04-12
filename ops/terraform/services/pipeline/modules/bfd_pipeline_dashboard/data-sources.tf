data "external" "edt_or_est" {
  program = [
    "bash",
    "-c",
    # heredoc is used to make quote escaping a little clearer
    <<-EOF
    echo "{\"timezone\":\"$(TZ=America/New_York date +%Z)\"}"
    EOF
  ]
}
