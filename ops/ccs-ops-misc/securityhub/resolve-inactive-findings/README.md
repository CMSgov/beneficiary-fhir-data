
## Quickstart

```sh
cd ops/ccs-ops-misc/securityhub/resolve-inactive-findings
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
chmod +x resolve-inactive-findings.py
./resolve-inactive-findings.py --help
```

Example resolving Findings referencing invalid or terminated ec2 instances
```sh
# remove the --dry-run flag to actually resolve the findings
./resolve-inactive-findings.py --ec2-instances --dry-run
```

Example resolving all supported types with no continue prompt (for automation)
```sh
./resolve-inactive-findings.py --all --yes
```

Help and usage
```sh
./resolve-inactive-findings.py --help
```
