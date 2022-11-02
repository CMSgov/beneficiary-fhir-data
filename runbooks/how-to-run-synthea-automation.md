# How to Run Synthea Automation

1. Go to the [Synthea Automation Jenkins Job](https://jenkins-east.cloud.cms.gov/bfd/) - TODO: Add jenkins job link for automation
2. Click "Build with Parameters" on the left side
3. The parameters here control how the Synthea automation will generate and load data:
    - Number of Beneficiaries: the number of beneficiaries to generate data for
        - Jenkins has an upper limit that it can handle creating in one batch, so if you need more than the specified number, please split the load into multiple batches or manually generate the load on a detached instance
    - Target environment: the environment(s) that this run's data will be loaded into
        - There are two options, a load into only test, or a full load into test, prod-sbx, and then prod sequentially
    - Number of Months to generate into the future: If greater than 0, Synthea will generate some beneficiaries which have claims up to that many months into the future, and those future claims will be automatically portioned into weekly loads and placed in the environments' Synthea/Incoming ETL bucket to be loaded in the future. If this is 0, no claims will have dates beyond the current date.
4. Once the parameters are as you'd like then, click Build to begin the process
5. The Jenkins pipeline will begin going through the steps to generate and then load the Synthea data into the target databases
6. Once the data is successfully loaded into Test, if the goal is a full load it will be loaded into prod-sbx and prod as well. Assuming success in Test, the other envionments _should_ have no issues.
7. Once all data is loaded, if the data is only for Test, you are done. For prod-sbx/prod loads, continue the steps below.

## Prod Load Additional Steps

For prod/prod-sbx loads, we need to publish the results to our consumers, so some additional steps are required.

1. A characteristics file needs to be generated and made available in order to let our partners know what benes and claims will be available to use. A script exists for generating the characteristics file at '''beneficiary-fhir-data/ops/ccs-ops-misc/synthetic-data/scripts/synthea-automation/generate-characteristics-file.py'''
2. Ensure you have Python3 installed, and also psycopg2 and boto3 (Python libraries) installed, as the script will need them
3. Ensure you're connected to the VPN, as you'll need access to the database to run the script
4. Run the script. It takes three params:
        - bene_id_start: this is the bene id the generation started at, which will be printed in the Jenkins log when you run the job or found in the manifest xml of the batch (this can be found in AWS under <environment-etl-bucket>/Synthetic/Done after the data is loaded)
        - bene_id_end: this is the bene id the generation ended at, which will be printed in the Jenkins log when you run the job or found in the manifest xml of the batch (this can be found in AWS under <environment-etl-bucket>/Synthetic/Done after the data is loaded)
        - output location: the directory the characteristics file should be written to
5. Once the script runs, a file should be output called characteristics.csv at the location you specified
6. Move this file to an accessible location
7. Next we need to update the github wiki page: https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide
8. On this page there are two spots we need to update: 
    - The Available Synthetic Beneficiaries table, which you should add a row for the date, bene ranges, and link to the characteristics file generated above. Additionally, if there are future updates with this batch, an additional entry should specify how many months the batch will update
    - The Release History table, which should describe the purpose of the synthetic batch along with any other relevant information
9. Lastly, our partners should be made aware of the new data load; post a message in the bfd-users chat informing them of the newly available data, with a link to the wiki page with additional information


## Troubleshooting (High Level)
- Many steps of the automation process can fail during the generation or loading, which will fail the Jenkins job. If this occurs, you will need to investigate the failure to determine the next steps. If errors occur during generation, you may need to check the database or end-state.properties file parameters. If errors occur during the pipeline load, you may need to move the *new* load files generated in Synthetic/Incoming out and restart the pipeline, as well as investigate the failure.
- During the pipeline data load, the automation will automatically pass the end-state.properties data used to create the load and pre-validate the target database(s) do not contain those values before loading any data. This is intended to ensure there are no unique column collisions with the data before loading. If this pre-validation fails, no data cleanup will be required in the database since the load will be stopped; however you will need to check the end-state.properties in s3 and determine which fields are problematic, and fix them in the file. Additionally, you may need to move/delete the files from Synthetic/Incoming in the target environments' ETL boxes in AWS (see bullet above).
- In the unlikely event of issues in the prod environments after successful test, keep in mind you may need investigation and manual re-run of the data to keep consistency between environments, or cleanup/rollback of the Test database.
