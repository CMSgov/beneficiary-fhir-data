# RDA API Utility Apps

This project produces zip file containing standalone console applications for simulating or interacting
with the RDA API.
These applications are not intended for deployment to production servers but rather to be executed
on developer computers to assist in local development and testing.

## Installation

To install or use these applications you need to expand
the `bfd-pipeline/bfd-pipeline-rda-grpc-apps/target/bfd-pipeline-rda-grpc-apps-1.0.0-SNAPSHOT.zip`
file into a directory. Then set your CLASSPATH to include the application jar and all jars in the lib directory.
Then invoke the desired applications using the java CLI.

## RdaServerApp

This application starts a mock RDA API server and listens for client connections.
The server can serve several types of data:

- Randomly generated claims with nonsense content for load testing.
- Pre-generated claims stored in NDJSON files on a local drive.
- Pre-generated claims stored in NDJSON files in an S3 bucket.

Command line options are specified using the format `name:value` where name is an option name from the table below.

Valid command line options supported by the server include:

| Option             | Default   | Description                                                                                      |
|--------------------|-----------|--------------------------------------------------------------------------------------------------|
| port               | 5003      | Optional TCP port for the server to use for incoming connections.                                |
| random.seed        | time      | Seed used to initialize PRNG when generating random claims data.                                 |
| random.verbose     | false     | If true causes random claims to include ALL optional fields in every claim.                      |
| random.errorRate   | 0         | Number of claims to transmit before returning an invalid one. Useful for testing error handling. |
| random.max.mbi     | unlimited | Maximum number of unique MBIs to use in random claims.                                           | 
| random.max.claimId | unlimited | Maximum number of unique claim ids to use in random claims.                                      | 
| maxToSend          | unlimited | Maximum number of claims to send in response to a single API call.                               |
| file.fiss          | none      | Path to an NDJSON file to use as source of data for clients requesting FISS claims.              |
| file.mcs           | none      | Path to an NDJSON file to use as source of data for clients requesting MCS claims.               |
| s3.bucket          | none      | URI for S3 bucket in which to search for NDJSON files containing claims.                         |
| s3.region          | us-east-1 | Name of AWS region containing S3 bucket.                                                         |
| s3.directory       | none      | Directory prefix to use when searching S3 bucket for NDJSON files.                               |
| s3.cacheDirectory  | temp dir  | Local directory to use for caching files downloaded from S3 bucket.                              |

Order of selection of source for claims is:

- If a file name has been provided that file will be used.
- If an S3 bucket URI has been provided that S3 bucket will be used.
- Otherwise randomly generated claims will be used.

## DirectRdaLoadApp

This application calls an RDA API server and stores all returned claims in a database.
Each run of the application will store either FISS or MCS claims (depending on configuration) but not both.
The application uses the actual ETL pipeline jobs internally so it can be used to test those jobs easily from the
command line.

The application reads its configuration from system properties.  
System properties are loaded from a combination of those from the VM and those loaded from a configuration file
specified on the command line.
System properties from the VM take precedence over those from the configuration file.

Command arguments (both required) are:

| Position | Description                                                     |
|----------|-----------------------------------------------------------------|
| 1        | Path to java properties file containing configuration settings. |
| 2        | Either `fiss` or `mcs` to indicate which job to execute.        |

Configuration settings and their associated properties are:

| Option                  | Default         | Description                                                         |
|-------------------------|-----------------|---------------------------------------------------------------------|
| database.url            | none            | JDBC URL to connect to database.                                    |
| database.user           | none            | User ID to use when authenticating to database.                     |
| database.password       | none            | Password to use when authenticating to database.                    |
| database.maxConnections | 5 * threads     | Connection pool size limit.                                         |
| hash.iterations         | 100             | Number of iterations to use when hashing MBI values.                |
| hash.pepper             | notarealpepper  | Pepper to use when hashing MBI values.                              |
| job.batchSize           | 1               | Number of claims per batch when writing to database.                |
| job.writeThreads        | 1               | Number of writer threads to use when writing to the database.       |
| job.startingFissSeqNum  | 0               | Starting sequence number in call to fetch FISS claims from RDA API. |
| job.startingMcsSeqNum   | 0               | Starting sequence number in call to fetch MCS claims from RDA API.  |
| api.host                | localhost       | Host name for connection to RDA API server.                         |
| api.port                | 5003            | TCP port for connection to RDA API server.                          |
| job.idleSeconds         | unlimited       | Maximum idle time before closing connection to RDA API server.      |
| rda.version             | current version | Expected RDA API server version string.                             |

## LoadRdaJsonApp

This application reads FISS and/or MCS claims from NDJSON files and writes them to a database.
The application uses the actual ETL pipeline jobs internally so it can be used to test those jobs easily from the
command line.

The application reads its configuration from system properties.
System properties are loaded from a combination of those from the VM and those loaded from a configuration file
specified on the command line.
System properties from the VM take precedence over those from the configuration file.

The only command line argument is an optional path to a java properties file containing configuration settings.

Configuration settings and their associated properties are:

| Option                  | Default        | Description                                                                           |
|-------------------------|----------------|---------------------------------------------------------------------------------------|
| database.url            | none           | JDBC URL to connect to database.                                                      |
| database.user           | none           | User ID to use when authenticating to database.                                       |
| database.password       | none           | Password to use when authenticating to database.                                      |
| hash.iterations         | 100            | Number of iterations to use when hashing MBI values.                                  |
| hash.pepper             | notarealpepper | Pepper to use when hashing MBI values.                                                |
| job.batchSize           | 1              | Number of claims per batch when writing to database.                                  |
| job.writeThreads        | 1              | Number of writer threads to use when writing to the database.                         |
| job.migration           | false          | If true the program will perform a schema migration before running the pipeline jobs. |
| file.fiss               | none           | Path to a NDJSON file containing FISS claims data.                                    |
| file.mcs                | none           | Path to a NDJSON file containing MCS claims data.                                     |

## StoreRdaJsonApp

This application reads FISS or MCS claims from an RDA API server and writes them to a NDJSON file.

The application reads its configuration from system properties.
System properties are loaded from a combination of those from the VM and those loaded from a configuration file
specified on the command line.
System properties from the VM take precedence over those from the configuration file.

The only command line argument is a required path to a java properties file containing configuration settings.

Configuration settings and their associated properties are:

| Option          | Default   | Description                                                                |
|-----------------|-----------|----------------------------------------------------------------------------|
| output.type     | none      | Specifies type of claims to download. Either `FISS` or `MCS`.              |
| output.maxCount | unlimited | Specifies maximum number of claims to download.                            |
| output.file     | none      | Path to file to store the claims. The file will be created or overwritten. |
| output.seq      | 0         | Starting sequence number for call to RDA API server.                       |

## Sample Run Script

```bash
#!/bin/bash

run_dir=`dirname $0`
cd $run_dir

zip_name=bfd-pipeline-rda-grpc-apps-1.0.0-SNAPSHOT.zip
bin_dir=`basename $zip_name .zip`
echo $bin_dir
ls -l $bin_dir

binaries_base=$HOME/projects/beneficiary-fhir-data
binaries_zip=$binaries_base/apps/bfd-pipeline/bfd-pipeline-rda-grpc-apps/target/$zip_name

rm -rf $bin_dir
unzip $binaries_zip
ls -l $bin_dir

jar=$bin_dir/bfd-pipeline-rda-grpc-apps-1.0.0-SNAPSHOT.jar

ls -l $bin_dir/lib

cp=$jar
for i in $bin_dir/lib/*.jar ; do
  cp=$cp:$i
done
export CLASSPATH=$cp

java \
  -Ds3.local=true \
  gov.cms.bfd.pipeline.rda.grpc.apps.RdaServerApp \
  seed:42 \
  maxToSend:50000 \
#  s3Bucket:rda \
#  s3Directory:test \
  "$@"
```
