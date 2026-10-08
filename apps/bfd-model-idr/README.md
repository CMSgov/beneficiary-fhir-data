# IDR Model

## `synthetic-data`

The `../bfd-pipeline-idr/test_samples1` directory contains some synthetic-data, along with `../bfd-pipeline-idr/test_samples2`.

**When adding new fields, take care to pass _every_ CSV into the `patient_generator.py` (see below) or else the resulting data may be invalid.**

## Generating data

### Compile FSH Resources

To compile the .fsh files from this folder

```sh
just sushi
```

This will generate the StructureDefinition and CodeSystem resources necessary for synthetic data generation. Running compile_resources.py is not necessary to generate synthetic data.

### Matchbox

To reduce dependencies on tx.fhir.org as well as improve the speed of validation, we use matchbox to run a local FHIR server. Read more about matchbox at <https://ahdis.github.io/matchbox/>

### Referencing New/Updated Dependencies for Matchbox

As new versions of IGs are released, they may have multiple nested dependencies. This takes up a significant amount of memory if loaded directly into Matchbox. To eliminate heap errors while still being able to accurately validate profiles and terminology, we download the FHIR Packages locally, untar them, and upload relevant resources directly to Matchbox. The list of resources + packages are in matchbox_profiles.txt. To add a new IG reference, follow the syntax in that file. Packages are only uploaded using docker compose up (by calling setup_matchbox.py), so restart the composition if adding more dependencies.

### Generating Sample JSON from Synthetic CSVs

To generate or update sample data files from the generated synthetic CSVs (located in `out/`), you can use the sample generation scripts. This only exists for prior auth data for now - it will be expanded to other use cases gradually.

#### Prior Authorization Sample Generator

To generate a prior authorization JSON sample from the synthetic CSVs based on tracking number.

```sh
just generate-prior-auth-sample --utn=<utn-here>
```

This will search for the specified UTN in `out/SYNTHETIC_PRAUC.csv`, collect the segments, and get it into the format that we map using FML.

#### EOB Sample Generator

To generate a EOB sample that is not prior authorization.

```sh
just generate-eob-sample --clm-uniq-id <clm_uniq_id_here>
```
This will search for the clm_uniq_id in out/SYNTHETIC_CLM.csv, collect the appropriate data fields and format it to be able to be mapped to fml.

This will currently only work for Pharmacy and Basic types but will be continued to work on other types.

There are two optional parameters .
- --source-directory  Directory where the source csv files are located.  Default: ./out
- --output-directory  Directory where to output the files.  Default: ./sample-data

### Bene Sample Generator

```sh
just generate-bene-sample --bene-sk <bene_sk>
```
This will search for the bene_sk in out/SYNTHETIC_BENE_HIST.csv, collect the appropriate data fields and format it to be able to be mapped to fml.


There are two optional parameters .
- --source-directory  Directory where the source csv files are located.  Default: ./out
- --output-directory  Directory where to output the files.  Default: ./sample-data


### Create FHIR files with synthetic data

To easily compile all resources:

```sh
just generate-structure-map --all
just fhir-transform --all
```

These commands also allow you to provide a specific resource or choose one interactively when no arguments are supplied.

Note: Matchbox uses a good bit of memory. Allocating at least 8GB of RAM is recommended. When new version dependencies are added, the heap allocated required should be re-evaluated. The compose setup is optimized to GC aggressively to reduce the impact.

Note, it takes several minutes and requires a good bit of RAM. Once the uploads from matchbox-setup are completed, matchbox should be ready to use. Additionally, matchbox has a health check available at:

```sh
curl -X GET "http://localhost:18080/matchboxv3/actuator/health"
```

#### Patient Data - `patient_generator.py`

##### Generating patient data

To generate synthetic patient data, the `patient_generator` script is used.
To utilize it to generate an entirely _new_ set of data from nothing:

```sh
just patient-generator --patients <num_patients>
```

Or, to load the v3 synthetic data (to add new fields):

```sh
just patient-generator ../bfd-pipeline-idr/test_samples1/*.csv
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
- `SYNTHETIC_BENE_LIS_CMBND.csv`
- `SYNTHETIC_BENE_MAPD_ENRLMT_RX.csv`
- `SYNTHETIC_BENE_MAPD_ENRLMT.csv`

The patient generator creates synthetic beneficiary data with realistic but _synthetic_ MBIs, coverage information, and historical records. It can generate multiple MBI versions per beneficiary and handles beneficiary cross-references with kill credit switches.

#### Claims data - `claims_generator.py`

> [!WARNING]
> Either `SYNTHETIC_CLM.csv` or `SYNTHETIC_BENE_HSTRY.csv` **must** be provided as claims data generation requires an existing `BENE_SK` or `CLM` to generate/regenerate data. It is recommended that `SYNTHETIC_CNTRCT_PBP_NUM.csv` also be provided so new claims use the same generated contracts. If not provided, newly generated claims will instead use randomly selected contract nums and pbp nums. This may result in claims referencing contracts that do not exist in the generated contract tables.

To generate synthetic claims data, the `claims-generator` script is used.

The synthetic claims data generated will be written to the `./out` folder in the form of CSVs, one per-table:

- `SYNTHETIC_CLM.csv`
- `SYNTHETIC_CLM_RLT_COND_SGNTR_MBR.csv`
  - This file contains an extra column, `CLM_UNIQ_ID`, that is purely metadata used by the synthetic claims generator and is not consumed by the IDR Pipeline
- `SYNTHETIC_CLM_LINE.csv`
- `SYNTHETIC_CLM_LINE_RX.csv`
- `SYNTHETIC_CLM_VAL.csv`
- `SYNTHETIC_CLM_DT_SGNTR.csv`
- `SYNTHETIC_CLM_PROD.csv`
- `SYNTHETIC_CLM_INSTNL.csv`
- `SYNTHETIC_CLM_LINE_INSTNL.csv`
- `SYNTHETIC_CLM_DCMTN.csv`
- `SYNTHETIC_CLM_FISS.csv`
- `SYNTHETIC_CLM_PRFNL.csv`
- `SYNTHETIC_CLM_LINE_PRFNL.csv`
- `SYNTHETIC_CLM_ANSI_SGNTR.csv`
- `SYNTHETIC_PRVDR_HSTRY.csv`
- `SYNTHETIC_CNTRCT_PBP_NUM.csv`
- `SYNTHETIC_CNTRCT_PBP_CNTCT.csv`

These files represent the schema of the tables the information is sourced from, although for tables other than `CLM_DT_SGNTR`, the `CLM_UNIQ_ID` is propagated instead of the 5 part unique key from the IDR.

##### Using `SYNTHETIC_BENE_HSTRY.csv` and `SYNTHETIC_CNTRCT_PBP_NUM.csv`

The below will generate _entirely new claims_ for the given `BENE_SK`s in the provided file:

```sh
just claims-generator out/SYNTHETIC_BENE_HSTRY.csv out/SYNTHETIC_CNTRCT_PBP_NUM.csv
```

##### Regenerating existing claims data

The below will _re-generate_ **existing claims data** (assume `<PATH_TO_CLAIMS_DATA>` is a local directory containing synthetic claims data):

```sh
just claims-generator ./out
```

If _any_ claims-related tables have had columns added to their respective generation functions, those new columns will be populated with values without impacting existing values in other columns.

> [!CAUTION]
> If an **existing column value** must be updated, that column value **MUST BE DELETED** from the respective table CSV first so that the values can be regenerated.

## Testing Mapping Changes

### Verifying FML Map Changes

To test updates to your FML map files, run `conformance-test` to generate a resource and verify the output:

```sh
just conformance-test
```

### Updating Structure Definitions

FHIR Mapping Language is used to go from source to target. The source data structure must be defined, and we define them using FHIR Shorthand (https://hl7.org/fhir/uv/shorthand/N2/). When a new source field is added, add it to the relevant .fsh file (or create a new one) in the sushi/input folder. StructureDefinitions for input data structures should follow the naming convention of StructureDefinition-<Resource>.fsh

### Resource Augmentation

Due to FML limitations, some complex mappings that require more than simple lookups are handled via augment_sample_resources.py.
For example, CLM_AUDT_TRL_STUS_CD on an EOB is derived by combining the status code, location code (CLM_AUDT_TRL_LCTN_CD), and source (META_SRC_SK). The augmentation script resolves these fields into the claim status code.
To test augmentation logic independently:

```sh
uv run augment_sample_resources.py {your_sample_file}.json [Basis|Regular|CMS]
```

> [!NOTE]
> The `just fhir-transform` command mentioned above also executes this script. You can inspect the output at out/temporary-sample.json and the compiled resource.

## Data Dictionary

Generally, the data dictionary will source definitions from the IDR's table definitions. There are instances where this may not be the definition we wish to publish. To overwrite the definition from the IDR, or populate a definition not available from the IDR, populate the "definition" key for the relevant concept in the relevant StructureDefinition.

Sometimes a field may be condensed at the IDR level, and fanned into multiple discrete components at the BFD / FHIR layer. An example is BENE_MDCR_STUS_CD. This code can indicate several interesting characteristics, such as ESRD status and disability status. A field, nameOverride, is available to directly populate names in the BFD DD for these fields that do not surface through a StructureDefinition.
To generate the data dictionary:

```sh
just generate-data-dictionary
```

If the data dictionary script produces warnings about missing tables or columns, run the following query to retrieve the latest updates for the affected table from IDR.
Run:

```sql
DESCRIBE VIEW CMS_VDM_VIEW_MDCR_PRD.{TABLE_NAME}
```

Export the results as a CSV named {TABLE_NAME}.csv and save it under ReferenceTables.
