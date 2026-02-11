# IDR Model

## `synthetic-data`

The `synthetic-data` directory contains the synthetic data loaded into each of our environment's databases.

**When adding new fields, take care to pass _every_ CSV into the `patient_generator.py` (see below) or else the resulting data may be invalid.**

## Generating data

Downloading the FHIR validator is necessary to run the following scripts, along with installing sushi

To download the FHIR Validator:
<https://github.com/hapifhir/org.hl7.fhir.core/releases/download/6.7.10/validator_cli.jar>

```sh
curl -L https://github.com/hapifhir/org.hl7.fhir.core/releases/download/6.7.10/validator_cli.jar > validator_cli.jar
```

### Install sushi + fhirpath.js

```sh
# Check if npm is installed
npm --version

# If not then install
brew install npm
```

```sh
npm install -g fsh-sushi fhirpath
```

### Install packages  (via uv)

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

### Compile FSH Resources

To compile the .fsh files from this folder
```sh
cd sushi && sushi build && cd ..
```

This will generate the StructureDefinition and CodeSystem resources necessary for synthetic data generation. Running compile_resources.py is not necessary to generate synthetic data. 

### Get Matchbox up and running
To reduce dependencies on tx.fhir.org as well as improve the speed of validation, we use matchbox to run a local FHIR server. Read more about matchbox at https://ahdis.github.io/matchbox/

Note: Matchbox uses a significant amount of memory. Allocating at least 8GB of RAM is recommended, and more may be necessary in the future.

To start matchbox, run 

```sh
docker compose up -d
```
Note, it takes several minutes and requires a good bit of RAM. It'll be ready once it says that packages have been loaded and some obviously untrue amount of RAM (generally half of what it actually used) was used. Additionally, one can check the logs for "Finished engines during startup" or running a health check using
```sh
curl -X GET "http://localhost:8080/matchboxv3/actuator/health"
```

### Create FHIR files with synthetic data

Requires Matchbox to be active.

To easily compile all resources:

```sh
./compile-all-resources.sh
```

To compile a specific resource:

Pass along map with -m
pass along the sample file with -i
pass along the output file with -o
pass along the resource url with -r
pass along --test to run conformance tests
pass along --skip-structure-map-generation to skip generating the structure map. Only use this in the context of sequential transformations that re-use a structure map.

Example (Patient):

```sh
uv run compile_resources.py \
    -m maps/patient.map \
    -i sample-data/Beneficiary-Sample.json \
    -o outputs/Patient.json \
    -r https://bfd.cms.gov/MappingLanguage/Maps/Patient \
    --test
```

#### Patient Data - `patient_generator.py`

##### Usage

```text
usage: patient_generator.py [-h] [--patients PATIENTS] [--claims]
                            [--exclude-empty | --no-exclude-empty]
                            [--force-ztm-static-rows | --no-force-ztm-static-rows]
                            [files ...]

Generate synthetic patient data

positional arguments:
  files                 CSVs that will be regenerated/updated with new
                        columns. Updates are idempotent, meaning that passing
                        in an existing table/CSV without any new columns being
                        added to the synthetic data generation will result in
                        a byte-identical output file. Take care to avoid
                        providing a partial set of tables with foreign key
                        constraints (e.g. BENE_SK) without providing the root
                        table as this could result in broken output data

options:
  -h, --help            show this help message and exit
  --patients PATIENTS   Number of patients to generate. Ignored if
                        SYNTHETIC_BENE_HSTRY is provided via 'files'
  --claims              Automatically generate claims after patient generation
                        using the generated SYNTHETIC_BENE_HSTRY.csv file
  --exclude-empty, --no-exclude-empty
                        Treat empty column values as non-existant and allow
                        the generator to generate new values
  --force-ztm-static-rows, --no-force-ztm-static-rows
                        Allow "zero-to-many" rows (e.g. BENE_ENTLMT, c/d data,
                        etc.) for a patient loaded from a file to be
                        generated. This will introduce new rows for patients
                        that previously had none. Useful if not all tables for
                        a patient have been generated yet.

```

##### Generating Data

To generate synthetic patient data, the patient_generator.py script is used.
To utilize it to generate an entirely _new_ set of data from nothing:

```sh
uv run patient_generator.py
```

Or, to load the v3 synthetic data (to add new fields):

```sh
uv run patient_generator.py synthetic-data/*.csv
```

_**NOTE**: the `bene_id` column in `SYNTHETIC_BENE_HSTRY.csv` is to reference the `bene_id` field used in V1/V2. It's not used for sample data generation here._

The files output will be in the `out` folder:

- `SYNTHETIC_BENE_HSTRY.csv`
- `SYNTHETIC_BENE_MBI_ID.csv`
- `SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv`
- `SYNTHETIC_BENE_MDCR_ENTLMT.csv`
- `SYNTHETIC_BENE_MDCR_STUS.csv`
- `SYNTHETIC_BENE_TP.csv`
- `SYNTHETIC_BENE_XREF.csv`
- `SYNTHETIC_BENE_CMBND_DUAL_MDCR.csv`
- `SYNTHETIC_BENE_LIS.csv`
- `SYNTHETIC_BENE_MAPD_ENRLMT_RX.csv`
- `SYNTHETIC_BENE_MAPD_ENRLMT.csv`

The patient generator creates synthetic beneficiary data with realistic but _synthetic_ MBIs, coverage information, and historical records. It can generate multiple MBI versions per beneficiary and handles beneficiary cross-references with kill credit switches.

#### Claims data

To generate synthetic claims data, the claims_generator.py script is used.
To utilize it:

```sh
uv run claims_generator.py \
    --sushi \
    out/SYNTHETIC_BENE_HSTRY.csv
```

--sushi is not strictly needed, if you have a local copy of the compiled shorthand files, but recommended to reduce drift. To specify a list of benes, pass in a .csv file containing a column named BENE_SK.
The files output will be in the out folder, there are several files:
SYNTHETIC_CLM.csv
SYNTHETIC_CLM_LINE.csv
SYNTHETIC_CLM_VAL.csv
SYNTHETIC_CLM_DT_SGNTR.csv
SYNTHETIC_CLM_PROD.csv
SYNTHETIC_CLM_INSTNL.csv
SYNTHETIC_CLM_LINE_INSTNL.csv
SYNTHETIC_CLM_DCMTN.csv
SYNTHETIC_CLM_FISS.csv
SYNTHETIC_CLM_PRFNL.csv
SYNTHETIC_CLM_LINE_PRFNL.csv
SYNTHETIC_CLM_ANSI_SGNTR.csv

These files represent the schema of the tables the information is sourced from, although for tables other than CLM_DT_SGNTR, the CLM_UNIQ_ID is propagated instead of the 5 part unique key from the IDR.

## Data Dictionary

Generally, the data dictionary will source definitions from the IDR's table definitions. There are instances where this may not be the definition we wish to publish. To overwrite the definition from the IDR, or populate a definition not available from the IDR, populate the "definition" key for the relevant concept in the relevant StructureDefinition.

Sometimes a field may be condensed at the IDR level, and fanned into multiple discrete components at the BFD / FHIR layer. An example is BENE_MDCR_STUS_CD. This code can indicate several interesting characteristics, such as ESRD status and disability status. A field, nameOverride, is available to directly populate names in the BFD DD for these fields that do not surface through a StructureDefinition.
To generate the data dictionary:

```sh
./compile-all-resources.sh
uv run gen_dd.py
```

If the gen_dd.py script produces warnings about missing tables or columns, run the following query to retrieve the latest updates for the affected table from IDR.
Run:

```sql
DESCRIBE VIEW CMS_VDM_VIEW_MDCR_PRD.{TABLE_NAME}
```

Export the results as a CSV named {TABLE_NAME}.csv and save it under ReferenceTables.
