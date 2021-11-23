# RDA Bridge
Utility project for converting data to RDA data (such as rif files into RDA ndjson files) to be used with the
bfd-pipeline-rda-grpc-server to provide synthetic RDA data for FISS and MCS claims.


## Prerequisite
Before the rda bridge can be executed, you need data files to read from.  For converting RIF files, they can have any
origin, but the expected approach is to generate them from the Synthea project (using the bfd specific branch) to first
create the required RIF files, and then use the root directory of those files as the basis for the bridge execution.

The bridge project will generate FISS claims from the supplied FISS sources, as well as additional MCS claims from the
supplied MCS sources.

## Build
```shell
mvn clean package
```

## Run
Execute the shell script
```shell
./run_bridge <Path to Rif Dir> [options]
```

## Optional Parameters
```
usage: run_bridge sourceDir [-b <arg>] [-e <arg>] [-f <arg>] [-g <arg>] [-m
       <arg>] [-n <arg>] [-o <arg>]
    -b <arg>    Benefit History file to read from
    -e <arg>    Path to yaml file containing run configs
    -f <arg>    FISS file to read from
    -g <arg>    FISS RDA output file
    -m <arg>    MCS file to read from
    -n <arg>    MCS RDA output file
    -o <arg>    The directory where the output files will be written to.
```

## Example execution commands
### CLI Based
```shell
./run_bridge path/to/rif/ \
    -o output/ \
    -g rda-fiss-out.ndjson \
    -n rda-mcs-out.ndjson \
    -f inpatient.csv \
    -f outpatie.csv \
    -m carrier.csv \
    -b beneficiary_history.csv
```

### YAML Config Based
```shell
./run_bridge -e path/to/config.yml
```

## Sample Output

```
Wrote 43214 inpatient claims
Wrote 391498 outpatient claims
Wrote 54940 home claims
Wrote 140635 hospice claims
Wrote 3784 snf claims
Wrote 326592 carrier claims
```