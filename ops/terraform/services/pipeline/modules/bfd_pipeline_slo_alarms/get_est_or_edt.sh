#!/bin/bash

set -eou pipefail

TZ="America/New_York"

echo "{\"timezone\":\"$(date +%Z)\"}"
