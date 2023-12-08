# AWS Cloudwatch Alerts Audit

This script is set to grab all Cloudwatch alerts based on the Action Prefix of `arn:aws:sns:us-east-1:577373831711:bfd-prod-cloudwatch-alarms`. This includes all production based Cloudwatch alerts and then writes to an HTML table. This document will allow us to regenerate any changes that may occur in the prod environment and allow us to update the wiki page.

## Quickstart

```sh
cd ops/ccs-ops-misc/alert-audit
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
chmod +x alert-audit.py
./alert-audit.py
```
