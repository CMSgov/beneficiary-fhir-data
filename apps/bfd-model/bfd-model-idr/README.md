### Welcome to the README!

This file will be extended, but for this step, we're consolidating multiple steps into a single script. 

Downloading the FHIR validator is necessary to run the following scripts, along with installing sushi

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar
  - place the jar file in the `beneficiary-fhir-data/apps/bfd-model/bfd-model-idr` directory
  - ensure the name file name is `validator_cli.jar`

#### Install sushi + fhirpath.js + yaml
```sh
# Check if npm is installed
npm --version

# If not then install
brew install npm
```

```sh
npm install -g fsh-sushi yaml fhirpath
```

#### Install packages  (via uv)
```sh
# Check if uv is installed
uv --version

# If not then install
curl -LsSf https://astral.sh/uv/install.sh | sh
```

```sh
# Install dependencies 
uv sync
```

### Create FHIR files with synthetic data

EOB Institutional Inpatient:
```sh
uv run compile_resources.py \
    -m maps/EOB-Base.map \
    -i sample-data/EOB-Base-Sample.json \
    -o outputs/ExplanationOfBenefit.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/ExplanationOfBenefit-Base \
    --test
```
Patient:
```sh
uv run compile_resources.py \
    -m maps/patient.map \
    -i sample-data/Beneficiary-Sample.json \
    -o outputs/Patient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Patient \
    --test
```
Coverage (part A/B):
```sh
uv run compile_resources.py \
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
uv run claims_generator.py \
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

To generate synthetic patient data, the patient_generator.py script is used.
To utilize it:
```sh
uv run patient_generator.py
```
The files output will be in the outputs folder:
SYNTHETIC_BENE_HSTRY.csv
SYNTHETIC_BENE_MBI_ID.csv
SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv
SYNTHETIC_BENE_MDCR_ENTLMT.csv
SYNTHETIC_BENE_MDCR_STUS.csv
SYNTHETIC_BENE.csv

Data Dictionary Notes:
Generally, the data dictionary will source definitions from the IDR's table definitions. There are instances where this may not be the definition we wish to publish. To overwrite the definition from the IDR, or populate a definition not available from the IDR, populate the "definition" key for the relevant concept in the relevant StructureDefinition. 

Sometimes a field may be condensed at the IDR level, and fanned into multiple discrete components at the BFD / FHIR layer. An example is BENE_MDCR_STUS_CD. This code can indicate several interesting characteristics, such as ESRD status and disability status. A field, nameOverride, is available to directly populate names in the BFD DD for these fields that do not surface through a StructureDefinition. 
To generate the data dictionary:
python gen_dd.py

