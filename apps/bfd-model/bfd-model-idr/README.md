Welcome to the README!

This file will be extended, but for this step, we're consolidating multiple steps into a single script. 

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


