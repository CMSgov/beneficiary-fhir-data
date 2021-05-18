#!/bin/bash

set -e
set -o pipefail
year=""

if [ -z "$1" ]
then
    year="2020"
else
    year="$1"
fi


if [[ "$year" != "2019" && "$year" != "2020" ]]; then
  echo "Invalid year specified...must be either 2019 or 2020!"
  exit ;
fi

echo "Begin processing year: $year at: $(date +'%T.%31')"

let tot_rcds=0
while :; do
  echo "Starting 20k transaction at: $(date +'%T.%31')"

  cnt=$(psql -h 127.0.0.1 -U bfd -d fihr --quiet -c '\t' -c "select public.update_bene_monthly_with_delete('$year');")
  expr $cnt + 0

  if (( $cnt > 0 )); then
      tot_rcds=$(( tot_rcds + $cnt ))
      echo "Current record count: $tot_rcds at: $(date +'%T.%31')"
  else
      echo "Completed shell loop processing at:  $(date +'%T.%31')"
      break;
  fi
done

echo "All DONE at: $(date +'%T.%31')"
echo "Records processed: $tot_rcds"
exit 0;
