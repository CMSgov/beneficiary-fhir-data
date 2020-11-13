# Sample Data Sets

This document details the various sample/test data sets available for use with the Blue Button Data/backend systems.

## Sample Data Design Constraints

Some notes:

1. All sample data sets are provided/stored in RIF format, so that they can be used to test the full set of Data systems.
2. Ideally, all sample data beneficiary and claim identifiers will "look" very distinct from actual PII/PHI, to help engineers and operators avoid mistakes in handling sensitive data.
    * Sample/test values for the following fields should be negative (i.e. prefixed with a "`-`"): `BENE_ID`, `CLM_ID`, `PDE_ID`, `CLM_GRP_ID`.
    * Sample/test values for `BENE_CRNT_HIC_NUM` (i.e. HICNs) should follow the following pattern, which is used by CMS' Next Generation Desktop (NGD) system for sample/test data: `MBPnnnnnnA` (where "`n`" is a numeric digit).
        * At this time, NGD is known to have exactly the following sample/test HICNs loaded in (at least) some of its lower environments: `MBP000201A`, `MBP000202A`, `MBP000203A`, `MBP000204A`, `MBP000207A`, `MBP000208A`, `MBP000209A`, `MBP000210A`.
    * Synthetic MBIs use an `S` in the second character. This is a forbidden value for a real MBI. 
    * Note that most of our sample/test data sets do not yet conform to this best practice.
3. Ideally, all sample data beneficiary and claim identifiers will be unique when compared to each other and also when compared to production data. This allows for arbitrary subsets of sample data to be deployed into development, test, and production environments.
    * It's very important to have "safe" sample/test data in production! This enables all sorts of best practices around testing code in its actual habitat.
    * Note that most of our sample/test data sets do not yet conform to this best practice.

### Identifier Ranges

The following table details the key ranges for all known sample and production data sets.

|Name|`BENE_CRNT_HIC_NUM` Range|`BENE_ID` Range|`CLM_ID` Range|`PDE_ID` Range|`CLM_GRP_ID` Range|Notes|
|---|---|---|---|---|---|---|
|Production|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|
|`SAMPLE_A`|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|
|`SAMPLE_U`|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|
|Random|TODO: `BENE_CRNT_HIC_NUM`s|`1` - `9999999`|`0000004484` - `00000002201` - `99999992536`|`00000002715` - `99999993651`|TODO: `CLM_GRP_ID`s|TODO: notes|
|`SAMPLE_C`|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|
|`SAMPLE_B`|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|
|Synthetic|TODO: `BENE_CRNT_HIC_NUM`s|`1` - `9999999`|Carrier: `00000002211` - `99999992534`, Inpatient: `-1132194757` - `5024534892`|`-1000004332` - `-999962460`|`68` - `99999991267`|TODO: Notes|
|`SAMPLE_MCT`|TODO: `BENE_CRNT_HIC_NUM`s|TODO: `BENE_ID`s|TODO: `CLM_ID`s|TODO: `PDE_ID`s|TODO: `CLM_GRP_ID`s|TODO: notes|

This similar table details what those key ranges _should_ be going forwards, to avoid conflicts:


The following table details the ideal/desired key ranges for all known sample and production data sets.

|Name        |`BENE_CRNT_HIC_NUM` Range                                             |`BENE_ID` Range          |`CLM_ID` and `PDE_ID` Range    |`CLM_GRP_ID` Range             |
|------------|----------------------------------------------------------------------|-------------------------|-------------------------------|-------------------------------|
|Production  |HICNs: `000000000A` - `999999999Z`, RRBs: TODO                        |     >= `0`              |       >= `0`                  |       >= `0`                  |
|`SAMPLE_A`  |`T00000001A` - `T00000004A`                                           |       `-1` -        `-2`|         `-1` -           `-50`|         `-1` -           `-50`|
|`SAMPLE_U`  |`T00000005A` - `T00000009A`                                           |       `-1` -        `-2`|        `-51` -           `-99`|        `-51` -           `-99`|
|Random      |`T10000000A` - `T99999999A`                                           |`-10000000` - `-99999999`|`-1000000000` - `-999999999999`|`-1000000000` - `-999999999999`|
|Synthetic   |`T01000000A` - `T01999999A`                                           | `-1000000` -  `-1999999`| `-100000000` -    `-199999999`| `-100000000` -    `-199999999`|
|`SAMPLE_MCT`|HICNs: `MBP000201A` - `MBP000210A`, RRBs: `{0099190316`, `B3499290814`|     `-201` -      `-212`|       `-240` -          `-285`|       `-240` -          `-285`|

## Procedures to Load Data 

### Local Dev

The synthetic data set can be loaded to a local Postgres database. An HSQL database
**cannot** be used. The `RifLoaderIT#loadSyntheticData` test will load the synthetic data set. 
Normally, this test is marked with `@Ignore` because it will take as much as 20 minutes to run. 

```$xslt
mvn -Dits.db.url=jdbc:postgresql://localhost:5432/fhir -Dit.test=RifLoaderIT#loadSyntheticData clean install
```

When adding test new synthetic data, developers should create new integration tests for the new data. 
The `OutpatientClaimTransformerTest.transformSyntheticData` is a good model to follow for this type of test. 

### TEST, PROD-SBX, and PROD

Before loading data into an environment, ensure that the data loads and works in your local environment. 
Experience from outages has shown that engineers should test every record that is being loaded, not a sample of records. 

To manually load data into an environment, one essentially needs to duplicate the process that the CCW does for its weekly drop of RIF files. 
Every ETL service monitors an S3 bucket for incoming RIF files. 
The buckets used in the CCS account are:

| Environment | Bucket         |
|-------------|-------------------------|
| Test        | bfd-test-etl-577373831711 |
| Prod-Sbx    | bfd-prod-sbx-etl-577373831711 |
| Prod        | bfd-prod-etl-577373831711 |

In each of these buckets there are two special directories: `Done` and `Incoming`. 
The `Incoming` directory further contains a subdirectory that holds the data for a single data load. 
The name of this subdirectory must match the timestamp in the manifest file. 

In the subdirectory, there are two types of files: RIF files which hold the data records to be loaded and manifest files which point the RIF files.
Each manifest file follows the naming convention of `<sequenceId>_manifest.xml` where `sequenceId` is the must match the `sequenceId` found in the manifest file. 
After the ETL service processes a manifest file in the `Incoming` directory, it writes the RIF file and the manifest file into the `Done` directory.
After all incoming files are processed, the `Incoming` directory is deleted. 

The `bfd-prod-etl-577373831711` bucket is a good place to find examples of manifest files and data load subdirectories. 

## Sample Data Set Details

### `SAMPLE_A`: Lovingly Hand-Crafted, Bespoke, Hipster-Approved Data

**Location**: [CMSgov/bluebutton-data-model:bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples/](https://github.com/CMSgov/bluebutton-data-model/tree/master/bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples)

The `SAMPLE_A` data set is our smallest data set: it aims to have exactly one beneficiary, with one claim of each claim type. It is used in almost all of our unit tests and in many of our integration tests.

It was created via the following process:

1. The [RIF layouts](../../bfd-model-rif/src/main/java/gov/cms/bfd/model/rif/rif-layout-and-fhir-mapping.xlsx) were used to determine each column's type and size, and an arbitrary matching value was chosen.
    * For example, the `Beneficiary.BENE_BIRTH_DT` column is listed as a `DATE` column of length `8`. The `SAMPLE_A` record has an arbitrary value of `17-MAR-1981` for this column.
2. In some cases, a value was needed that conformed to the CCW coding/codebook for that field, so an arbitrary but valid entry was selected from the codebook.
    * For example, the `Beneficiary.BENE_SEX_IDENT_CD` column is listed as having a value of `0` ("Unknown"), `1` ("Male"), or `2` ("Female") in the [Master Beneficiary Summary - Base (A/B/C/D) codebook](https://www.ccwdata.org/documents/10280/19022436/codebook-mbsf-abcd.pdf). The `SAMPLE_A` record has an arbitrary value of `1` for this column.
3. In some cases, a particular usage scenario or test case mandated a certain value be present in these records. When this could be accomodated without risking or appearing to risk the security of PII/PHI, `SAMPLE_A` was updated to accommodate these needs.

### Synthetic: Statisically Verified, Semi-Realistic

**Location**: The synthetic data is large enough that it needs to be stored in S3. Because it's certified as not containing PII or PHI, it's stored in a *public* S3 bucket. There have been multiple iterations of this data set:

1. The original data set can be downloaded using the following command:

    ```
    $ wget \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/0_manifest.xml \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z/synthetic-pde-2016.rif
    ```

2. A "fixed" version was produced via the [SyntheticDataFixer](../../../bfd-pipeline/bfd-pipeline-rif-extract/src/test/java/gov/cms/bfd/pipeline/rif/extract/synthetic/SyntheticDataFixer.java) utility. That data set can be downloaded using the following command:

    ```
    $ wget \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/0_manifest.xml \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed/synthetic-pde-2016.rif
    ```

3. A "fixed with negative IDs" version was produced via the [SyntheticDataFixer2](../../../bfd-pipeline/bfd-pipeline-rif-extract/src/test/java/gov/cms/bfd/pipeline/rif/extract/synthetic/SyntheticDataFixer2.java) utility. That data set can be downloaded using the following command:

    ```
    $ wget \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/0_manifest.xml \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/gov-hhs-cms-bluebutton-sandbox-etl-test-data/data-synthetic/2017-11-27T00%3A00%3A00.000Z-fixed-with-negative-ids/synthetic-pde-2016.rif
    ```

4. A random MBIs were generated for each synthetic beneficiary. 
These MBI's have a `S` in as their second character to distinguish them from real MBIs. 
With a change in AWS accounts for the BFD project, the synthetic data set was moved to a different S3 bucket: `bfd-public-test-data` 

   ```
    $ wget \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/0_manifest.xml \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-1-25-mbi-with-negative-ids/synthetic-pde-2016.rif
   ```
    
5. A set of Outpatient claims were generated and added to the synthetic data set. 
The source data is kept in a team Keybase folder. 
All the fixups on the source data that was performed are documented in the `ops/ccs-ops-misc/synthetic-data/scripts/outpatient/convert_outpatient_claims_csv_file_to_rif.py` script.

   ``` 
     $ wget \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/0_manifest.xml \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-pde-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2000-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2001-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2001-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2002-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2015-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-02-27-add-outpatient/synthetic-outpatient-2016-2014.rif
   ```
6. Synthetic part D contract IDs were generated for each synthetic beneficiary.
The contract ID fields `PTD_CNTRCT_JAN_ID`...`PTD_CNTRCT_DEC_ID` (or `PTDCNTRCT01`...`PTDCNTRCT12` CCW Codebook) were set for all months using the following rules:
  * Assigned Z0000 to 100 BENE_ID's: -19990000000001 thru -19990000000100
  * Assigned Z0001 to 1000 BENE_ID's: -19990000000101 thru -19990000001100
  * Assigned Z0002 to 2000 BENE_ID's: -19990000001101 thru -19990000003100
  * Assigned Z0005 to 5000 BENE_ID's: -19990000003101 thru -19990000008100
  * Assigned Z0010 to 10000 BENE_ID's: -19990000008101 thru -19990000010000 and -20000000000001 thru -20000000008100
  * Assigned Z0012 to 11900 BENE_ID's: -20000000008101 thru -20000000010000 and -20140000000001 thru -20140000010000

  The source data was from the previous iteration #5 synthetic-beneficiary RIF files.
  All the changes on the source data that was performed are documented in the `ops/ccs-ops-misc/synthetic-data/scripts/beneficiary/update_beneficiary_rif_files_for_bb1896.py` script. That data set can be downloaded using the following command:

   ```
     $ wget \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/0_manifest.xml \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-beneficiary-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-beneficiary-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-beneficiary-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-carrier-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-1999-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-1999-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2000-2001.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2000-2002.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2014-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-inpatient-2014-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-pde-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-pde-2015.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-pde-2016.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-1999-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2000-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2001-1999.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2000-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2001-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2002-2000.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2014-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2015-2014.rif \
        https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-04-10-part-d-enrollment/synthetic-outpatient-2016-2014.rif
   ```
   
7. The following is a summary of the changes related to update  2020-05-21-synthetic-bene-updated-mbi:
To denote that an MBI is synthetic in the BFD database, we are using the 2nd position character = "S". We are breaking the MBI standard format for just the 2nd character position for this purpose:
https://www.cms.gov/Medicare/New-Medicare-Card/Understanding-the-MBI-with-Format.pdf
However, we ran in to validation issues with 999 of the 30K records when updating the values in SLS and also accounting for that difference in the 2nd character position.
These records had an "S" in the 3rd character position and were failing the validation.
To resolve, the 3rd character position of those 999 MBI values was updated from "S" => "T".
For example, the MBI value "8SS0A00AA00" was updated to "8ST0A00AA00"
All MBI values are now updated to conform to the MBI standard format, with the exception of our special case to denote them as synthetic vs. real.

  ``` 
  $ wget \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/0_manifest.xml \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-beneficiary-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-beneficiary-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-beneficiary-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-1999-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-1999-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2000-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2000-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2000-2002.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2014-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-carrier-2014-2016.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-1999-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-1999-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2000-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2000-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2000-2002.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2014-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-inpatient-2014-2016.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2000-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2001-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2000-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2001-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2002-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2015-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-outpatient-2016-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-pde-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-pde-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-05-21-synthetic-bene-updated-mbi/synthetic-pde-2016.rif 
  ``` 

8. The following is a summary of the changes related to update  2020-11-02-New-Data-Fields:
   Added new fields that we are sending over to CCW

  ``` 
  $ wget \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/0_manifest.xml \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-beneficiary-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-beneficiary-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-beneficiary-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-1999-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-1999-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2000-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2000-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2000-2002.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2014-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-carrier-2014-2016.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-1999-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-1999-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2000-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2000-2001.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2000-2002.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2014-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-inpatient-2014-2016.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-1999-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2000-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2001-1999.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2002-2000.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2014-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2015-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-outpatient-2016-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-pde-2014.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-pde-2015.rif \
    https://s3.amazonaws.com/bfd-public-test-data/data-synthetic/2020-11-02-New-Data-Fields/synthetic-pde-2016.rif 
  ``` 


The synthetic data set was statistically validated and certified as fit for public use by CMS' Data Governance Board. The documentation from that is published here: [CMSgov/beneficiary-fhir-data:bfd-model-rif-samples/dev](./).

It's important to note that the synthetic data set does not yet cover all supported claim types: it has carrier claims, inpatient claims, outpatient claims, and Part D events but does not have DME claims, HHA claims, hospice claims, or SNF claims. The synthetic data set has the following counts, by record type:

|Record Type|Count|
|---|---|
|Beneficiary|30000|
|Carrier CLaims|1744920|
|Inpatient Claims|70212|
|Part D Events|413347|
|Outpatient Claims|171144|

### `SAMPLE_MCT`: MCT-Compatible Test Data (Derived from Synthetic)

**Location**: [CMSgov/bluebutton-data-model:bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples/](https://github.com/CMSgov/bluebutton-data-model/tree/master/bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples)

The `SAMPLE_MCT` data set is a small data set created specifically to support the Medicare Coverage Tools (MCT) project (also known as the new "Medicare Plan Finder").

It was created via the following process:

1. Start with the synthetic data set.
2. Randomly select 8 beneficiaries from the synthetic data set. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://confluence.cms.gov/pages/viewpage.action?pageId=172097602).
3. For each of those beneficiaries, randomly select Part D events from the synthetic data set to associate with them. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://confluence.cms.gov/pages/viewpage.action?pageId=172097602).
    * It so happens that MCT wants each beneficiary to have exactly 5 Part D events, but this appears to have been a mostly arbitrary decision.
4. Adjust all other identifiers (`BENE_ID`, `CLM_ID`, `PDE_ID`, and `CLM_GRP_ID`) so that they don't conflict/collide with any other data sets.

### `SAMPLE_MCT`: MCT-Compatible Test Data (Derived from Synthetic) Added 11 new beneficiaries and associated data with pde's

**Location**: [CMSgov/bluebutton-data-model:bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples/](https://github.com/CMSgov/bluebutton-data-model/tree/master/bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples)

The `SAMPLE_MCT` data set is a small data set created specifically to support the Medicare Coverage Tools (MCT) project (also known as the new "Medicare Plan Finder").

It was created via the following process:

1. Start with the synthetic data set from sample-mct-update-4-beneficiaries.txt.
2. Randomly select 11 beneficiaries from the synthetic data set. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://jira.cms.gov/browse/BFD-326).  Adjust identifiers `BENE_ID` so that they don't conflict/collide with any other data sets.
3. For each of those beneficiaries, randomly select Part D events from the synthetic data sample-mct-update-5-pde.txt set to associate with them. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://jira.cms.gov/browse/BFD-326).
4. Adjust all other identifiers (`BENE_ID`, `PDE_ID`, and `CLM_GRP_ID`) so that they don't conflict/collide with any other data sets.

### `SYNTHEA`: Synthetic Data Generated by Synthea

[Synthea](https://synthetichealth.github.io/synthea/) is an open source tool
  for generating large volumes of realistic, but synthetic, health data.
It was created and is maintained by [MITRE](https://www.mitre.org/),
  a not-for-profit organization,
  which operates federally funded research and development centers (FFRDCs).
The best way to think of Synthea is like this:
  "what if we paid for a metric ton of academic papers on disease prevalence, progression, etc.
  and then paid a bunch of scientist-engineers to turn those papers into code
  to generate realistic health data based on models derived from the papers?"
That's Synthea: sure, it generates data,
  but it's _how_ it generates the data that's the interesting, and tricky, part.

Synthea models a wide range of populations, health conditions, etc.
  and generates a similarly wide range of FHIR data,
  including `Patient`s, and `ExplanationOfBenfit` resources.
That's great, but not super useful for BFD: BFD _produces_ FHIR;
  it doesn't need or _consume_ FHIR data as input.

In 2020, CMS engaged Synthea to add an output mode
  that produced data in BFD's RIF input file formats.
The initial engagement ended with these accomplishments:

* Synthea added a `--exporter.bfd.export=true` option to produce RIF.
* It produces beneficiary RIF records:
    * These are valid records with all 192 columns and convert to FHIR without errors.
    * Most of those columns are optional. Synthea currently populates 21 of them.
    * Of those populated columns, 16 appear to have useful, realistic data.
* It produces inpatient claim RIF records:
    * These are valid records with all 272 columns and convert to FHIR without errors.
    * Most of those columns are optional. Synthea currently populates 52 of them.
    * Of those populated columns, 25 appear to have useful, realistic data.
* _Note: All column counts above are estimates produced by eyeballing the data;
    we need to get more accurate counts from the Synthea folks._
