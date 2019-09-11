# Sample Data Sets

This document details the various sample/test data sets available for use with the Blue Button Data/backend systems.

## Sample Data Design Constraints

Some notes:

1. All sample data sets are provided/stored in RIF format, so that they can be used to test the full set of Data systems.
2. Ideally, all sample data beneficiary and claim identifiers will "look" very distinct from actual PII/PHI, to help engineers and operators avoid mistakes in handling sensitive data.
    * Sample/test values for the following fields should be negative (i.e. prefixed with a "`-`"): `BENE_ID`, `CLM_ID`, `PDE_ID`, `CLM_GRP_ID`.
    * Sample/test values for `BENE_CRNT_HIC_NUM` (i.e. HICNs) should follow the following pattern, which is used by CMS' Next Generation Desktop (NGD) system for sample/test data: `MBPnnnnnnA` (where "`n`" is a numeric digit).
        * At this time, NGD is known to have exactly the following sample/test HICNs loaded in (at least) some of its lower environments: `MBP000201A`, `MBP000202A`, `MBP000203A`, `MBP000204A`, `MBP000207A`, `MBP000208A`, `MBP000209A`, `MBP000210A`.
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


The synthetic data set was statistically validated and certified as fit for public use by CMS' Data Governance Board. The documentation from that is published here: [CMSgov/beneficiary-fhir-data:bfd-model-rif-samples/dev](./).

It's important to note that the synthetic data set does not yet cover all supported claim types: it has carrier claims, inpatient claims, and Part D events but does not have DME claims, HHA claims, hospice claims, outpatient claims, or SNF claims. The synthetic data set has the following counts, by record type:

|Record Type|Count|
|---|---|
|Beneficiary|30000|
|Carrier CLaims|1744920|
|Inpatient Claims|70212|
|Part D Events|413347|

### `SAMPLE_MCT`: MCT-Compatible Test Data (Derived from Synthetic)

**Location**: [CMSgov/bluebutton-data-model:bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples/](https://github.com/CMSgov/bluebutton-data-model/tree/master/bluebutton-data-model-rif-samples/src/main/resources/rif-static-samples)

The `SAMPLE_MCT` data set is a small data set created specifically to support the Medicare Coverage Tools (MCT) project (also known as the new "Medicare Plan Finder").

It was created via the following process:

1. Start with the synthetic data set.
2. Randomly select 8 beneficiaries from the synthetic data set. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://confluence.cms.gov/pages/viewpage.action?pageId=172097602).
3. For each of those beneficiaries, randomly select Part D events from the synthetic data set to associate with them. Modify their field values (only) as needed to correspond to the values required by MCT, as detailed on [NGD/MBP/BB/MCT Test Case Data](https://confluence.cms.gov/pages/viewpage.action?pageId=172097602).
    * It so happens that MCT wants each beneficiary to have exactly 5 Part D events, but this appears to have been a mostly arbitrary decision.
4. Adjust all other identifiers (`BENE_ID`, `CLM_ID`, `PDE_ID`, and `CLM_GRP_ID`) so that they don't conflict/collide with any other data sets.
