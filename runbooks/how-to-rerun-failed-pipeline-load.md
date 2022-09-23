# How to Re-run a Failed BFD Pipeline Load

Follow this runbook to successfully reload data via BFD pipeline after a failed run.

Note: If there are pending deployments or db migrations, make sure those finish before running these steps.

1. SSH into the AWS ETL EC2 instance for a given environment ```bfd-<test/prod/prod-sbx>-etl``` with ```ssh -i <local ssh key> <aws username>@<EC2 IP Address>```. The entry point is the specific AWS user home directory.

2. Confirm the pipeline has failed to load data. 

    - In AWS S3, the RIF folder containing the data for reloading will still be in 'Incoming' with the file S3 file structure as:
        ```
        bfd-<test/prod/prod-sbx>-etl-<aws-account-id>
        │
        └───Incoming/
        │   │
        │   └───<year>-<month>-<day>T<hh>:<mm>:<ss>Z/
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
3. Check if the pipeline is running with ```sudo systemctl status bfd-pipeline```, and if so, stop it with ```sudo systemctl stop bfd-pipeline```.

4. In the EC2 instance enable idempotent mode for the pipeline:
    - Open the file ```../../../bluebutton-data-pipeline/bfd-pipeline-service.sh```.
    - Change the line ```export IDEMPOTENCY_REQUIRED='false'``` to ```export IDEMPOTENCY_REQUIRED='true'```.
    - Save and close the file.

5. Restart the pipeline with ```sudo systemctl start bfd-pipeline```.

6. Confirm restarting the pipleine and loading data in idempotent mode is succesful: 
    - The output of running ```sudo systemctl status bfd-pipeline``` should say "active(running) since …".
    - As data is loading check the logs by running ```tail ../../../bluebutton-data-pipeline/bluebutton-data-pipeline.log -f```. 
    - When data is loaded properly, in AWS S3, the RIF folder containing the data for reloading will have automatically moved from 'Incoming' to 'Done' with the file S3 file structure as:
        ```
        bfd-<test/prod/prod-sbx>-etl-<aws-account-id>
        │
        └───Incoming/
        │   │
        │   └───...
        │   
        └───Done/
        │   │   
        │   └───<year>-<month>-<day>T<hh>:<mm>:<ss>Z/
        │   │   │   *_manifest.xml
        │   │   │   *.rif
        │   │   │  ...
        │   │ 
        │   └───...
        ```





