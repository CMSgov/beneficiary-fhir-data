data "aws_region" "current" {}

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

# We generate the timestamp externally as using Terraform's timestamp() will force a re-apply of all
# resources that depend upon it.
data "external" "current_time_utc" {
  program = [
    "bash",
    "-c",
    # heredoc is used to make quote escaping a little clearer
    <<-EOF
    echo "{\"rfc3339_timestamp\":\"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\"}"
    EOF
  ]
}
