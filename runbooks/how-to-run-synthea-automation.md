# How to Run Synthea Automation

1. Go to the [Synthea Automation Jenkins Job](https://jenkins-east.cloud.cms.gov/bfd/job/bfd-run-synthea-generation/)
2. Click "Build with Parameters" on the left side
3. The parameters here control how the Synthea automation will generate and load data:
    - NUM_BENES: The number of beneficiaries to generate data for
        - Jenkins has an upper limit that it can handle creating in one batch, so if you need more than the specified number, please split the load into multiple batches or manually generate the load on a detached instance
    - NUM_FUTURE_MONTHS: If greater than 0, Synthea will generate some beneficiaries which have claim creation dates up to that many months into the future, and those future claims will be automatically portioned into weekly loads and placed in the output as separate load folders. When placed in an environment's ETL Synthea/Incoming folder, these future claim folders will be loaded and updated when their load date comes to term every week for the number of months specified. If this is 0, no claims will have dates beyond the current date.
    - UPDATE_END_STATE_S3 - In order to avoid collisions in fields which have unique database constraints, we keep a file called "end state" in S3. This file keeps track of the current last generated value of all synthetic constrained fields so we can continue incrementing them in the next load without conflicting with previous values. If this checkbox is checked, it will update the file with the load's new latest state.
        - Basically this should always be checked if you intend to load the generated data into any environment. The only time this should not be checked is if you're _reloading_ data that exists (using idempotent mode in the pipeline) or creating a "test batch" to verify something but do not intend to load the data into BFD.
4. Once the parameters are as you'd like then, click Build to begin the process
5. The Jenkins pipeline will begin going through the steps to generate the data
6. Once the data is generated, the output will be automatically uploaded to S3 in the [Output Directory](https://s3.console.aws.amazon.com/s3/buckets/bfd-mgmt-synthea?region=us-east-1&prefix=generated/&showversions=false)
7. To load this data into an environment, some manual steps are currently required (everything can be done from the AWS console)
    - Find your generated data in the output directory in S3. It should have the date and time it was created on the S3 'folder'
    - Within your generated data, follow the directory down into output/bfd/
    - Create a folder in the environment's ETL Synthetic/Incoming with the timestamp found in the manifest.xml file for this load
    - Copy the csv files and manifest (ignore folders if any for now) into the timestamped folder(s) you just created
      - You can ignore the following when copying, as these files are not needed: end state properties, export summary, npi.tsv
    - Lastly, once all the files are copied to the timestamped folder in each environment you wish you load, edit the names of the manifest files in the timestamped folders to be "0_manifest.xml" instead of "manifest.xml". This signals to the pipeline to try loading the files.
      - This a safety mechanism to ensure that all the files have been copied before attempting to load, to ensure loading does not begin while file transfer is still occurring
    - If you generated any future data, you'll see a number of folders at the top of output/bfd/ with timestamps
      - If you wish to have your synthetic data "update" weekly, move these folders into the appropriate environment's ETL Synthetic/Incoming folder in AWS
      - Note: the update folders may generate with underscores; these underscores should be replaced with colons when you move them or else they will not be picked up by the pipeline (basically the folder name should be a proper timestamp)

## Prod Load Additional Steps

For prod/prod-sbx loads, we need to publish the results to our consumers, so some additional steps are required.

1. A characteristics file needs to be generated and made available in order to let our partners know what beneficiary ids and claims will be available to use. A script exists for generating the characteristics file at '''beneficiary-fhir-data/ops/ccs-ops-misc/synthetic-data/scripts/synthea-automation/generate-characteristics-file.py'''
2. Ensure you have Python3 installed, and also psycopg2 and boto3 (Python libraries) installed, as the script will need them
3. Ensure you're connected to the VPN, as you'll need access to the database to run the script
4. Run the script locally. It takes three parameters:
        - bene_id_start: this is the bene id the generation started at, which will be printed in the Jenkins log when you run the job
        - bene_id_end: this is the bene id the generation ended at, which will be printed in the Jenkins log when you run the job
        - output location: the local directory the characteristics file should be written to
5. Once the script runs, a file should be output called characteristics.csv at the location you specified in parameter 3
6. Upload this file to the AWS public folder [bfd-public-test-data/characteristics](https://s3.console.aws.amazon.com/s3/buckets/bfd-public-test-data?region=us-east-1&prefix=characteristics/)
7. Next we need to update the github wiki page: [Synthetic Data Guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide)
8. On this page there are two spots we need to update: 
    - The Available Synthetic Beneficiaries table, which you should add a row for the date, bene ranges, and link to the characteristics file in AWS above. Additionally, if there are future updates with this batch, an additional column should specify for how many months the batch will update
    - The Release History table, which should describe the purpose of the synthetic batch along with any other relevant information
9. Lastly, our partners should be made aware of the new data load; post a message in the bfd-users chat informing them of the newly available data and a link to the wiki page with additional information (update the parts in brackets as needed/if there is future data) Note the default update time is Wednesday at 7am, so just remove the brackets if the update was done with future data.
> BFD Synthetic data in prod-sbx and prod has been updated with <10,000> new beneficiaries<, which will update every Wednesday at 7am EST>. Information about the beneficiary ranges added <and update schedule> can be found in the Synthetic Data Guide: https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide

## Troubleshooting (High Level)
- Many steps of the automation process can fail during data generation or loading, which will fail the Jenkins job. If this occurs, you will need to investigate the failure to determine the next steps. If errors occur during generation, you may need to check the database or end-state.properties file parameters. If errors occur during the pipeline load, you may need to move the *new* load files generated in Synthetic/Incoming out to Synthetic/Failed (just a holding ground) and restart the pipeline, as well as investigate the failure.
- ETL Pipeline loading of Synthea data to a BFD database is subject to a pre-validation step that checks data ranges based on pre-validation elements in the manifest file. For example, the manifest includes two elements: _bene_id_start_ and _bene_id_end_; those values represent a lo-hi bene_id range that can be checked vs. bene_id(s) currently stored in the target database. If pre-validation detects an overlap (i.e., a _bene_id_ that falls within the lo-hi range that is already stored in the database), then pre-validation fails ***unless the ETL is running in idempotent mode***. When Synthea pre-validation fails, the ETL does the following:
    - Logs an error message specific to the pre-validation failure and terminates further processing.
    - moves the mainfest and associated RIF files out of the S3 bucket folder _/Incoming_ to the S3 bucket folder _/Failed_. This is done to preclude the ETL process from attempting to process the same mainfest and files which would occur if they were left in the _/Incoming_ folder.
- During the pipeline data load, the automation will automatically pass the end-state.properties data used to create the load and pre-validate the target database(s) do not contain those values before loading any data. This is intended to ensure there are no unique column collisions with the data before loading. If this pre-validation fails, no data cleanup will be required in the database since the load will be stopped; however you will need to check the end-state.properties in s3 and determine which fields are problematic, and fix them in the file. Additionally, you may need to move/delete the files from Synthetic/Incoming in the target environments' ETL boxes in AWS (see bullet above).
- In the unlikely event of issues in the prod environments after successful test, keep in mind you may need investigation and manual re-run of the data to keep consistency between environments, or cleanup/rollback of the Test database.
