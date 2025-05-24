Welcome to the README!

This file will be extended, but for this step, we're consolidating multiple steps into a single script. 

Downloading the FHIR validator is necessary to run the following scripts, along with installing sushi

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar

Install sushi
```sh
npm install -g fsh-sushi
```


EOB Institutional Inpatient:
```sh
python compile_resources.py \
    -m maps/EOB-Base.map \
    -i sample-data/EOB-Base-Sample.json \
    -o outputs/EOB.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --test
```
Patient:
```sh
python compile_resources.py \
    -m maps/patient.map \
    -i sample-data/Bene.json \
    -o outputs/Patient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Patient \
    --test
```
Coverage (part A/B):
```sh
python compile_resources.py \
    -m maps/Coverage-Base.map \
    -i sample-data/Coverage-FFS-Sample.json \
    -o outputs/Coverage-FFS.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Coverage-Base \
    --test
```


Pass along map with -m
pass along the sample file with -i
pass along the output file with -o
pass along the resource url with -r
pass along --test to run conformance tests


To generate synthetic claims data, the claims_generator.py script is used. As of 5/22/25, it will only generate inpatient institutional claims and their PAC equivalent claim type codes. 
To utilize it:
```sh
python claims_generator.py \
    --sushi \
    --benes <bene_filename.csv>
```

--sushi is not strictly needed, if you have a local copy of the compiled shorthand files, but recommended to reduce drift. To specify a list of benes, pass in a .csv file containing a column named BENE_SK. 
The files output will be in the outputs folder, there are several files:
SYNTHETIC_CLM_DCMTN.csv
SYNTHETIC_CLM_LINE_INSTNL.csv
SYNTHETIC_CLM_INSTNL.csv
SYNTHETIC_CLM_DT_SGNTR.csv
SYNTHETIC_CLM_PROD.csv
SYNTHETIC_CLM_VAL.csv
SYNTHETIC_CLM_LINE.csv
SYNTHETIC_CLM.csv

These files represent the schema of the tables the information is sourced from, although for tables other than CLM_DT_SGNTR, the CLM_UNIQ_ID is propagated instead of the 5 part unique key from the IDR.
