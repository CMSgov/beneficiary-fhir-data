# Synthea Test Plan

This document details various tests/procedures to ensure that each batch of synthetic data generated will not collide with previously generated data. 
Synthetic data is generated with the external Synthea codebase. More information on this process can be found at: `apps/bfd-model/bfd-model-rif-samples/dev/design-sample-data-sets.md`.

## Before Generating Data

1. Each time you generate synthea data, you are also generating ranges of values for several beneficiary and claim data properties. Furthermore, this creates database constraints for subsequent generated synthea datasets, which will cause collisions if not set properly. The input is the set of start values that the data can have, i.e the lower bound of these constraints. Once the data is generated, there is an output file representing the end values of the data, or the upper bound. Before generating data, go to `apps/bfd-model/bfd-model-rif-samples/dev/synthea_releases/XX-XX-20XX/end_state.properties` for the latest end-state properties. See example below: 

        exporter.bfd.clm_grp_id_start=-104410371
        exporter.bfd.pde_id_start=-10000464592
        exporter.bfd.carr_clm_cntl_num_start=-100518629
        exporter.bfd.fi_doc_cntl_num_start=-100251672
        exporter.bfd.hicn_start=T01030000A
        exporter.bfd.bene_id_start=-10000000020000
        exporter.bfd.clm_id_start=-10000003945779
        exporter.bfd.mbi_start=1S00E00NA00

2. With the end-state properties in mind, you need to check for two things before setting the next synthea property start values. First, if you increment each end-state property by 1, will the amount of data you are trying to generate e.g. 10,000, allow for a new range of values that has no overlap with previously generated synthea datasets? To answer this you need to run queries in PROD SBX. Run `psql -h {prod sbx db url} -d fhirdb -U bfduser -f apps/bfd-model/bfd-model-rif/src/main/resources/db/scripts/Query_synthetic_id_ranges.sql | tee Query_synthetic_id_rangesTest.out` to generate a series of outputs for various end state properties. An example for beneficiary ID is shown below:

        grouping_start  |  grouping_end
        -----------------+-----------------
        -88888888888888 | -88888888888888
        -20140000010000 | -20140000000001
        -20000000010000 | -20000000000001
        -19990000010000 | -19990000000001
                   -400 |            -400
                   -223 |            -207
                   -204 |            -201
                      1 |        60000000
        (8 rows)


3. Second, is the value you are incrementing creating a range of values that are FHIR compliant. For example the previous MBI ID end is `1S00-E00-FY99`, and you set the next start value to `1S00-E00-FZ01`, you are generating data that does not create database collisions, but is not FHIR compliant. For each property make sure to check resources like [https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf](https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf) for MBI ID. 

4. If all checks out, set the synthea property start values and generate new data.


## Loading Data

1. Test CCWRIFLoader functionality locally (i.e. verify BFD Pipeline can ingest records). Copy the files that were generated from the steps above and place them in the rif-synthea folder located at: `apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea`
2. Go to `/apps/bfd-model/bfd-model-rif-samples/src/main/java/gov/cms/bfd/model/rif/samples/StaticRifResource.java`. Go to and navigate to the SAMPLE_SYNTHEA entries and add or delete the entries that are currently there to correspond to the files you have copied from step 1.  You will also need to know how many records are in each file and place that in the entry as well. To find the counts run `wc -l apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea/*.csv`. Each value will need to be subtracted by 1 to remove the count for headers.

Sample Below: 

    Synthea-Rif-Static:


        SAMPLE_SYNTHEA_BENE(   
        resourceUrl("rif-synthea/beneficiary.csv"), RifFileType.BENEFICIARY, 10000),
        SAMPLE_SYNTHEA_BENEINTERIM(
        resourceUrl("rif-synthea/beneficiary_interim.csv"), RifFileType.BENEFICIARY, 10000),
        SAMPLE_SYNTHEA_BENEFINAL(
        resourceUrl("rif-synthea/beneficiary_final.csv"), RifFileType.BENEFICIARY, 10000),
        SAMPLE_SYNTHEA_CARRIER(resourceUrl("rif-synthea/carrier.csv"), RifFileType.CARRIER, 279900),
        SAMPLE_SYNTHEA_INPATIENT(resourceUrl("rif-synthea/inpatient.csv"), RifFileType.INPATIENT, 36606),
        SAMPLE_SYNTHEA_OUTPATIENT(
        resourceUrl("rif-synthea/outpatient.csv"), RifFileType.OUTPATIENT, 328420),
        SAMPLE_SYNTHEA_SNF(resourceUrl("rif-synthea/snf.csv"), RifFileType.SNF, 2797),
        SAMPLE_SYNTHEA_HOSPICE(resourceUrl("rif-synthea/hospice.csv"), RifFileType.HOSPICE, 1396),
        SAMPLE_SYNTHEA_HHA(resourceUrl("rif-synthea/hha.csv"), RifFileType.HHA, 14377),
        SAMPLE_SYNTHEA_DME(resourceUrl("rif-synthea/dme.csv"), RifFileType.DME, 8727),
        SAMPLE_SYNTHEA_PDE(resourceUrl("rif-synthea/pde.csv"), RifFileType.PDE, 214157),
        SAMPLE_SYNTHEA_BENEHISTORY(
        resourceUrl("rif-synthea/beneficiary_history.csv"), RifFileType.BENEFICIARY_HISTORY, 10000),

3. Go to `/apps/bfd-model/bfd-model-rif-samples/src/main/java/gov/cms/bfd/model/rif/samples/StaticRifResourceGroup.java` and navigate to where the SYNTHEA_DATA type is and make sure the entries from Step 2 are represented underneath the type

StaticResourceGroup:

    SYNTHEA_DATA(
        StaticRifResource.SAMPLE_SYNTHEA_BENES2011,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2012,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2013,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2014,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2015,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2016,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2017,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2018,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2019,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2020,
        StaticRifResource.SAMPLE_SYNTHEA_BENES2021,
        StaticRifResource.SAMPLE_SYNTHEA_CARRIER,
        StaticRifResource.SAMPLE_SYNTHEA_INPATIENT,
        StaticRifResource.SAMPLE_SYNTHEA_OUTPATIENT,
        StaticRifResource.SAMPLE_SYNTHEA_SNF,
        StaticRifResource.SAMPLE_SYNTHEA_HOSPICE,
        StaticRifResource.SAMPLE_SYNTHEA_HHA,
        StaticRifResource.SAMPLE_SYNTHEA_DME,
        StaticRifResource.SAMPLE_SYNTHEA_PDE,
        StaticRifResource.SAMPLE_SYNTHEA_BENEHISTORY),

4. Go to `/apps/bfd-pipeline/bfd-pipeline-ccw-rif/src/test/java/gov/cms/bfd/pipeline/ccw/rif/load/RifLoaderIT.java` and comment out the @Ignore from the loadSyntheaData() test.

5.  Run the following in a terminal window: 
`Docker
docker run \
     -d \
     --name 'bfd-db' \
     -e 'POSTGRES_DB=bfd' \
     -e 'POSTGRES_USER=bfd' \
     -e 'POSTGRES_PASSWORD=InsecureLocalDev' \
     -p '5432:5432' \
     -v 'bfd_pgdata:/var/lib/postgresql/data' \
     postgres:12`

6. Run the following in a terminal window:
`mvn -Dits.db.url="jdbc:postgresql://localhost:5432/bfd" -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dit.test=RifLoaderIT#loadSyntheaData clean install`

7. A sanity check to see if any collisions are possible with previously generated data locally requires loading both the new and old datasets into the local DB. Download the most recent synthea RIF files located in S3, and move them into `apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea`. Then run steps 2, 3, and 6 with counts and entries for both data sets Synthea-Rif-Static and StaticResourceGroup in mind. If the RIF Loader IT does not fail, the data likely does not create collisions, but the queries in the `Checking for collisions in TEST, SBX or PROD` section below should also be run. 

## Data Compliance
Test FHIR API payload (data) for FHIR and CARIN compliance. 
Test removal of data. 

Delete Script: 

    DELETE FROM public.carrier_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.carrier_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.dme_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.dme_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.hha_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.hha_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.hospice_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.hospice_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.inpatient_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.inpatient_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.outpatient_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.outpatient_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
    
    DELETE FROM public.snf_claim_lines
        WHERE "clm_id" in (SELECT "clm_id" FROM public.snf_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999'
    );
        
        
    DELETE FROM public.carrier_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.dme_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.hha_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    
    DELETE FROM public.hospice_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.inpatient_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.outpatient_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.snf_claims
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    
    DELETE FROM public.partd_events
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.beneficiary_monthly
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.beneficiaries_history
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';
        
    DELETE FROM public.beneficiaries
    WHERE "bene_id" >='-10000000000000'
        AND "bene_id" <='-10000000009999';`


## Load Synthetic Data in Hosted Environments

1. Before loading data, we need to make sure in TEST, SBX or PROD there are no collisions for the main properties in synthetic data i.e. bene_id, mbi_num, claim_id, etc. Each of the queries should return zero records. Aggregate RIF file data from all claim types, beneficiaries, and Part D Events located in `apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea/` to generate the following queries for the various synthea end-state properties: 

        -- For Beneficiary
        Select count(*) from public.beneficiaries where mbi_num in (‘XXX’,’YYY’,…);

        Select count(*) from public.beneficiaries where bene_id in (‘XXX’,’YYY’,…);

        Select count(*) from public.beneficiaries where hicn_unhashed in (‘XXX’,’YYY’,…);

        
        -- For PDE
        Select count(*) from public.partd_events where pde_id in (‘XXX’,’YYY’,…);


        -- For all claim types

        Select count(*) from public.carrier_claims where claim_id in (‘XXX’,’YYY’,…);

        Select count(*) from public.carrier_claims where clm_grp_id BETWEEN ‘XXX’ AND ’YYY’;


        -- For DME and Carrier
        Select count(*) from public.carrier_claims where carr_clm_cntl_num in (‘XXX’,’YYY’,…);


        -- For HHA, Hospice, Inpatient, Outpatient, SNF
        Select count(*) from public.hha_claims where fi_doc_clm_cntl_num in (‘XXX’,’YYY’,…);



2. Load data. Begin with TEST (Approval required for SBX and PROD)
3. Create dated Pipeline folder (same format as CCW) in the 'Incoming' folder of the environments ETL bucket. 
4. Upload generated synthetic data to dated folder.  Watch for Data Discovered and Data Loaded slack messages. Note: Also watch logs (at first)
5. Repeat for each approved environment. 
6. Upload synthetic data to shared space (TBD) example: test-synthetic-data S3 bucket (public)

## Checking for collisions in TEST, SBX or PROD

1. Run queries to find any possible duplicate parameters

        SELECT mbi_hash, count(*) 
        FROM public.beneficiaries 
        GROUP BY mbi_hash
        HAVING count(*)>1

        SELECT mbi_num, count(*) 
        FROM public.beneficiaries 
        GROUP BY mbi_num
        HAVING count(*)>1;

        SELECT hicn_unhashed, count(*) 
        FROM public.beneficiaries 
        GROUP BY hicn_unhashed
        HAVING count(*)>1;

        SELECT bene_crnt_hic_num, count(*) 
        FROM public.beneficiaries 
        GROUP BY bene_crnt_hic_num
        HAVING count(*)>1;

        SELECT DISTINCT
            dups.mbi_hash as hash, b.mbi_num as num, b.bene_id as bene
        FROM
            (	SELECT mbi_hash
                FROM beneficiaries
                GROUP BY mbi_hash
                HAVING COUNT(*) > 1
            ) dups
        LEFT JOIN beneficiaries b ON dups.mbi_hash = b.mbi_hash
        GROUP BY hash, num, bene
        HAVING count(*) < 2