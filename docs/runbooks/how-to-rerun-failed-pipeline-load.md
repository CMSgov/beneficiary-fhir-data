# How to Re-run a Failed BFD Pipeline Load

Follow this runbook to successfully reload data via BFD pipeline after a failed run.

Note: If there are pending deployments or db migrations, make sure those finish before running these steps.

1. SSH into the AWS ETL EC2 instance for a given environment ```bfd-<test/prod/prod-sbx>-etl``` with ```ssh -i <local ssh key> <your ssh username>@<EC2 IP Address>```.

2. Confirm the pipeline has failed to load data. 

    - In AWS S3, the RIF folder (i.e. ```<yyyy>-<MM>-<dd>T<HH>:<mm>:<ss>Z```) containing the data for reloading will still be in 'Incoming' with the file S3 file structure as:
        ```
        <S3 Bucket Name>-<aws-account-id>
        │
        └───Incoming/
        │   │
        │   └───2022-09-23T13:44:55Z/
        │   │    │   *_manifest.xml
        │   │    │   *.rif
        │   │    │   ...
        │   │ 
        │   └───...
        │   
        └───Done/
        │    │   
        │    └───...
        ```
        The AWS S3 bucket name in the file structure above can be found within the ETL EC2 instance by running ```grep S3_BUCKET_NAME /bluebutton-data-pipeline/bfd-pipeline-service.sh | cut -f2 -d=```.

3. Check if the pipeline is running with ```sudo systemctl status bfd-pipeline```, and if so, stop it with ```sudo systemctl stop bfd-pipeline```.

4. In the EC2 instance enable idempotent mode for the pipeline:
    - Open the file ```/bluebutton-data-pipeline/bfd-pipeline-service.sh```.
    - Change the line ```export IDEMPOTENCY_REQUIRED='false'``` to ```export IDEMPOTENCY_REQUIRED='true'```.
    - Save and close the file.

5. Restart the pipeline with ```sudo systemctl start bfd-pipeline```.

6. Confirm restarting the pipleine and loading data in idempotent mode is succesful: 
    - The output of running ```sudo systemctl status bfd-pipeline``` should say "active(running) since …".
    - As data is loading check the logs by running ```tail /bluebutton-data-pipeline/bluebutton-data-pipeline.log -f```. 
    - When data is loaded properly, in AWS S3, the RIF folder containing the data for reloading will have automatically moved from 'Incoming' to 'Done' with the file S3 file structure as:
        ```
        <S3 Bucket Name>-<aws-account-id>
        │
        └───Incoming/
        │   │
        │   └───...
        │   
        └───Done/
        │   │   
        │   └───2022-09-23T13:44:55Z/
        │   │   │   *_manifest.xml
        │   │   │   *.rif
        │   │   │  ...
        │   │ 
        │   └───...
        ```
7. With the data successfully loaded, in the EC2 instance, make sure to disable idempotent mode for the pipeline again:
    - Open the file ```/bluebutton-data-pipeline/bfd-pipeline-service.sh```.
    - Change the line ```export IDEMPOTENCY_REQUIRED='true'``` to ```export IDEMPOTENCY_REQUIRED='false'```.
    - Save and close the file.

8. Restart the pipeline ```sudo systemctl restart bfd-pipeline```.





