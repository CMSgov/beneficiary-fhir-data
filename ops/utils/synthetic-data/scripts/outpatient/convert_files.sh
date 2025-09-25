#!/usr/bin/bash

# Dropping the outpclm2000_encyp_s2000 and optclm2001_encyp_s2000 files because they contain duplicate claim ids. 

for file in outpclm1999_encyp_s1999.csv outpclm2000_encyp_s1999.csv outpclm2001_encyp_s1999.csv outpclm2002_encyp_s2000.csv outpclm2014_encyp_s2014.csv outpclm2015_encyp_s2014.csv outpclm2016_encyp_s2014.csv
#for file in outpclm1999_encyp_s1999.csv
do
  fbase="${file%.*}"

  echo CONVERTING FILE:  ${fbase}.csv to ${fbase}.txt

  python3 convert_outpatient_claims_csv_file_to_rif.py ${fbase}.csv >${fbase}.txt

  if [ $? -eq 0 ]
  then
     echo Converted successfully!
  else
     echo FAILED TO CONVERT!!!! Exiting
     exit 1
  fi
done
