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


