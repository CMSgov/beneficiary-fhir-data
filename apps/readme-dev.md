Docker Based Local Development
==============================

Alternative to the local setup and debugging and testing as described in BFD wiki, there is docker based set up for local bfd server development.

## Setup

Install docker desktop or other alternatives, that's it, no need to install java, maven etc. on native OS

## Development

### Check out BFD repository

```bash
git clone https://github.com/CMSgov/beneficiary-fhir-data.git
```
### Start BFD

Start the process of build and database migration and server start up:

```bash
cd ./beneficiary-fhir-data/apps
docker compose up bfd -d
```

Monitoring and checking the logging:

```bash
docker compose logs -f
```

It will do a full build of modules for the first time, and will run db migrator, and will start bfd server on port 6500, and java remote debugging enabled on port 5005, this will take a while first time.

Check the services up and running:

```bash
docker compose ps
```

Will see:

```
NAME         IMAGE         COMMAND                  SERVICE   CREATED        STATUS                  PORTS
apps-bfd-1   apps-bfd      "/usr/local/bin/mvn-…"   bfd       21 hours ago   Up About an hour        0.0.0.0:5005->5005/tcp, 0.0.0.0:6500->6500/tcp
apps-db-1    postgres:14   "docker-entrypoint.s…"   db        21 hours ago   Up 21 hours (healthy)   0.0.0.0:5432->5432/tcp
```

Now start your IDE and attach to 0.0.0.0:5005, and set break points in source code, you're all set to debug your code.

### Populate with sample data

You can populate the database with sample data, e.g. by running integration test 'loadSampleA'.
Note several handy scripts are provisioned in the bfd container, one of them is to run IT 'loadSampleA'.

```bash
docker exec -it apps-bfd-1 bash loadSampleA.sh
```

This will populate the database with resources (ExplanationOfBenefit, Coverage, Patient) for a sample patient with FHIR_ID = '567834'.

### Query FHIR resources

In addition to loadSampleA.sh, the docker compose also provisioned below scripts in the container to help generate FHIR queries:
(they are just bash wrapper of curl GETs on specific BFD end points of V1, V2)
```
getClaimsV2.sh
getPatientV2.sh
getCoverageV2.sh
getClaimsV1.sh
getPatientV1.sh
getCoverageV1.sh
```

To generate requests on V1 or V2 claims:

```bash
docker exec -it apps-bfd-1 bash getClaimsV1.sh
```

```bash
docker exec -it apps-bfd-1 bash getClaimsV2.sh
```

You will see FHIR bundles of claims responses in json.
And similarly, you can query that particular patient, and / or the patient's coverage.

### Docker exec other commands in the container

As with native OS development setup, you can run commands in the container as below examples:

Run codebook data tests:

```bash
docker exec -it apps-bfd-1 bash -c "cd /apps/bfd-model/bfd-model-codebook-data;mvn clean -Dmaven.build.cache.enabled=false test"
```

### Other tasks

Doing other tasks e.g. debug an integration test, is the same as on native OS:

```bash
docker exec -it apps-bfd-1 bash -c 'mvnDebug -Dits.db.url=jdbc:postgresql://db:5432/fhirdb -Dits.db.username=bfd -Dits.db.password=InsecureLocalDev -Dmaven.build.cache.enabled=false -Dit.test=RifLoaderIT#loadSampleA clean verify'
```

Maven will wait in port 8000, create a java remote debug profile in your IDE and attach.

### Customize the docker container

Look at the docker file and docker compose yaml, you can customize it to have your own container



