# Reclaiming Disk Space in FHIRDB Aurora Clusters using pg_repack

Over time our fhirdb Aurora clusters can become bloated with unused disk space, leading to performance issues and increased storage costs. `VACUUM FULL` requires exclusive locks on tables while it runs, so it is not something we run on a production db. Instead, we can use the `pg_repack` extension to reclaim this space without downtime and with minimal performance impact.

## How it works

Essentially, `pg_repack` works by creating a new table with the same structure as the original table, and then copying the data from the original table to the new table. Once the data is copied, the old table is dropped and the new table is renamed to the original table's name. This process is done in a way that is transparent to the application, so it can be run on a live database without downtime (sub-second locks on each table).

There is two components to `pg_repack`:

- The `pg_repack` extension, which is installed on the database cluster
- The `pg_repack` binary, which is used to run the repack process

The latter must be built from source, and the version of the binary must match the version of the extension installed on the cluster. These instructions will guide you through the process of building the binary and running the repack process.

_NOTE: pg_repack needs to run using our database superuser!_

## Process Overview

1. Install `pg_repack` extension on the database cluster and note the version that was installed
2. Setup a dedicated ec2 instance and install pg_repack requirements
3. Download and build the `pg_repack` binary
4. Run `pg_repack` on the desired cluster, either directly or via the provided script

## Install `pg_repack` extension

Installing the extension is as simple as running `CREATE EXTENSION pg_repack;` on each database cluster we want to run repack on. This can be done using the `psql` command line tool or any other tool that can execute SQL commands on the database.

```sql
-- install the extension
CREATE EXTENSION pg_repack;

-- get the version
SELECT extversion FROM pg_extension where extname = 'pg_repack';
```

We will use `1.4.7` in all examples going forward.

## Setup a dedicated ec2 instance and install requirements

The repack process can take a long time to run against all tables. How long will laregly depend on the size of the tables and how bloated they are. Some tables may only take a few minutes, while others may take hours.

**Either way, you should not run this from your local workstation!** Instead, run it from a dedicated ec2 instance. The only requirement is the instance needs to be in the same VPC as the cluster. The easiest way to get this going is to simply deploy an ephemeral migrator. See [TODO](link-to-ephemeral-instructions) for instructions on how to do this.

The following instructions assumes we are on a fresh migrator instance running Amazon Linux 2 or similar. Once the instance is up and running:

1. Connect to the instance
2. Disable the migrator service
3. Install the required packages

```bash
sudo systemctl stop bfd-migrator
sudo systemctl disable bfd-migrator
sudo yum groupinstall "Developer Tools"
sudo yum install \
  screen curl unzip \
  postgresql-devel postgresql-server-devel postgresql-common postgresql-static \
  readline-devel openssl-devel lz4-devel zlib1g-devel libzstd-devel
```

## Download and build the `pg_repack` binary

The `pg_repack` binary must be built from source, and the version of the binary must match the version of the extension installed on the cluster. The following instructions will guide you through the process of building the binary and running the repack process using the version of the extension we identified in step 1 (1.4.7 in this example).


1. Visit https://reorg.github.io/pg_repack
2. At the top, select the desired version ([1.4.7](https://reorg.github.io/pg_repack/1.4/) in this example)
3. Find the "download pg_repack" link under the "Download" section and click it
4. Use the dropdown to select the desired version and right click on the green download arrow to copy the link address
5. On the ec2 instance, use `curl` to download the zip file
6. Unzip the file and cd into the directory
7. Run `make` and `make install` to build and install the binary

```bash
# on the migrator instance
curl --output pg_repack-1.4.7.zip --location https://api.pgxn.org/dist/pg_repack/1.4.7/pg_repack-1.4.7.zip
unzip pg_repack-1.4.7.zip
cd pg_repack-1.4.7
make
make install
```

Verify the installation by running the following command:
```bash
pg_repack --version
```
## Running `pg_repack`

The `pg_repack` binary can be run from the command line to repack a single table or an entire database. Run `pg_repack --help` for usage, but use caution exploring. The following assumes you will be repacking all tables in the `fhirdb` database using the supplied shell script.

`pg_repack` needs to be run using the database superuser, so we will need to provide the `PGUSER` and `PGPASSWORD` environment variables. We will also need to provide the `PGHOST` environment variable to specify the hostname of the database cluster. The easiest way to do this is to create a `.env` file with the following contents:

```bash
PGHOST=your_db_cluster_host
PGUSER=your_db_super_username
PGPASSWORD=your_db_super_password
PGPORT=5432
PGDATABASE=fhirdb
```

Then, source the file to set the environment variables:

```bash
. .env
```

You will be prompted for the suepruser password when you run the script if you do not provide it in the `.env` file.

### Running `pg_repack` on all tables using the provided script

The following describes how to install and run the repack_all.sh script. This script will build a list of all tables sorted by size ascending, and then run `pg_repack` on each table in the list. We process smaller tables first to help ensure we have enough free space to handle processing our largest tables.

We run the script in a `screen` session to ensure it continues running if we disconnect from the instance.

1. Copy the repack_all.sh script to the instance
2. Make the file executable with `chmod +x repack_all.sh`
3. Run the script in a `screen` session with `screen -S repack` and then `./repack_all.sh`
4. Detach from the screen session with `Ctrl-a d`.
5. Monitor the script by tailing the repack.log file with `tail -f repack.log`
6. Reattach to the screen session with `screen -r repack` to check on the progress

## References

- https://jira.cms.gov/browse/BFD-3302
- https://aws.amazon.com/blogs/database/a-case-study-of-tuning-autovacuum-in-amazon-rds-for-postgresql/
- https://aws.amazon.com/blogs/database/understanding-autovacuum-in-amazon-rds-for-postgresql-environments/
- https://aws.amazon.com/blogs/database/remove-bloat-from-amazon-aurora-and-rds-for-postgresql-with-pg_repack
- https://reorg.github.io/pg_repack/1.4/
- https://aws.amazon.com/about-aws/whats-new/2020/10/amazon-aurora-enables-dynamic-resizing-database-storage-space
- https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.Overview.StorageReliability.html
- https://www.timescale.com/blog/how-to-reduce-your-postgresql-database-size/
- https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Appendix.PostgreSQL.CommonDBATasks.Autovacuum.html#
- https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/compile-software.html

