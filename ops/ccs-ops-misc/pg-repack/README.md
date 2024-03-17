# Reclaiming Disk Space in FHIRDB Aurora Clusters using pg_repack

Over time our databases can become bloated with unused billable storage space, leading to performance issues and wasteful spending. Unlike `VACUUM FULL`, which requires exclusive locks on tables while it runs, we can use the `pg_repack` extension to reclaim this space without downtime and minimal performance impact to the FHIR API.

## Overview

### How it works

`pg_repack` essentially creates a new table with the same schema as the original table, and then copies all the rows from the original table to the new table. It then builds indexes on the new table and applies all changes which have accrued in the log table to the new table. Finally, it swaps the tables, including indexes and toast tables, using the system catalogs, and drops the original table. You can read more about the process in the [pg_repack documentation](https://reorg.github.io/pg_repack/1.4/).

It should be noted that `pg_repack` does take ACCESS EXCLUSIVE locks, but only for very short durations (milliseconds) before and after processing a table. For the rest of its time, pg_repack only needs to hold an ACCESS SHARE lock on the original table, so INSERT's, UPDATE's, and DELETE's may proceed as usual. Only DDL commands that would require an ACCESS EXCLUSIVE lock on the original table will be blocked. So all schema changes and flyway migrations should be avoided while repack is running.

### How often, when, and how long does it take?

**How often** will depend on the amount of churn in a table. The more churn, the more bloat. The more bloat, the more often we should run `pg_repack` (and some tables may need to be repacked more often than others). Also, BFD's data tables are huge, so repacking is an IO intensive process that can a long time to run and can cause read latency issues. You should repack all tables only as minimally as needed to keep the database costs down, and target tables with lots of UPDATEs and DELETEs more frequently. The `repack_all.sh generate` command will build a list of tables sorted by size to a `repack_pick_list.txt` file, which you can then edit to remove tables you don't want to repack. The script will see this file and prompt you to use it before running.

At the time of this writing, we have only ran the repack_all.sh on all tables once, and it reclaimed several terrabytes of bloat. But without a good understanding of how long it took our tables to get so bloated initially, a good starting point would be to run it once a quarter and adjust as needed.

**When best to run or not run** is a bit more complicated. While the performance impacts are negligble they still exist. So it is _safest_ to run this when the database is not under heavy load and between CCW data loads. While this may be hard to predict, we can use the following as a general rule of thumb for all database maintenance activities:

 - Avoid overlapping a CCW data load
 - Avoid running database migrations
 - Keep an eye on the database metrics and look for signs of trouble

Pay particular attention to the Aurora Replication Lag metric. If a reader node gets too far behind the writer node, Aurora may disconnect it and any requests in flight will fail.

TODO: Update the repack_all.sh script to check for replication lag and other key metrics while running. Adding a sleep statement to allow the readers to catch up (similar to the [bulk_delete_aged_claims.sql](../pg-bulk-delete-aged-claims/bulk_delete_aged_claims.sql) script) would be a good start.

Either way, if you are running this on a prod cluster, you should be prepared to monitor the database and be ready to stop the process.

**How long** it takes to repack all the tables will depend on how large and bloated the tables are. For prod, running on 4x `db.r6i.12xlarge` instances, our largest tables took over 5 hours to repack each.

To give you an idea for a baseline, the first time we ran repack on all tables live (March 2024), some tables took a few minutes and others took 8 hours. Expect 2-3 days to repack all tables in the fhirdb database.

## Installing and running

There are two components to the `pg_repack` process:

- The `pg_repack` database extension (installed on the database with `CREATE EXTENSION pg_repack;`)
- And the `pg_repack` command line application, which an operator runs to repack the tables

The former is dictated by AWS Aurora and what Engine version we are on. The latter must be built from source and the version must match the extension version installed.

### Installing Overview

The steps to install and run `pg_repack` are as follows:

1. Install the `pg_repack` extension on the database cluster and note the version that AWS installed
2. Setup a dedicated ec2 instance and install pg_repack requirements
3. Download and build the `pg_repack` binary on the instance
4. Configure and source your .env file
4. Run the `repack_all.sh` script or the `pg_repack` binary to repack the tables
5. Repeat for each database cluster as needed (tip: you can build it once and copy the pg_repack binary over to other instances, but you still need to install the requirements)

TODO: if you find yourself running this often, consider creating a repack AMI or container image using docker, ansible, et al.

### Install `pg_repack` database extension

`pg_repack` is a supported extension in Aurora PostgreSQL, so we can install it using the following command:

```sql
-- install the extension
CREATE EXTENSION pg_repack;
```

We need to get the pg_repack extension version so we know which version of the binary to build. This is important because the extension and binary versions must match and we do not control which version of the extension AWS installs.

To get `pg_repack` extension version, run the following query in the database cluster you want to repack:

```sql
-- get the version
SELECT extversion FROM pg_extension where extname = 'pg_repack';
```

Now we download and build the source for the matching binary.

_Note: we will use version `1.4.7` for all examples below_

### Setup a dedicated ec2 instance and install requirements

The repack process **should not be run from your workstation** as it takes a long time to run and requires superuser access to the database. The easiest method to run it is by using a migrator instance as a host, as they are ephemeral in nature anyway and we will terminate it when done. Ie,

```sh
cd ops/terraform # tfswitch or tfenv
cd services/migrator
terraform workspace select test # prod-sbx, prod
terraform apply -var=create_migrator_instance=true -var=migrator_monitor_enabled_override=false
...scp, ssh, repack, etc...
terraform destroy -var=create_migrator_instance=false -var=migrator_monitor_enabled_override=true
```
### Install system requirements
The following instructions assumes we are on a fresh migrator instance running Amazon Linux 2 or similar. Once the instance is up and running:

1. Connect to the instance
2. Disable the migrator service
3. Install the required packages

Note: there was a bit of trial and error getting things to build on AL2 and requirements may change over time, so use the following as a general guide but be prepared to figure things out as you go:

```bash
sudo systemctl stop bfd-migrator
sudo systemctl disable bfd-migrator
sudo yum groupinstall "Developer Tools"
sudo yum install \
  screen curl unzip \
  postgresql-devel postgresql-server-devel postgresql-common postgresql-static \
  readline-devel openssl-devel lz4-devel zlib1g-devel libzstd-devel
sudo amazon-linux-extras install postgresql14
```

### Download and build the `pg_repack` binary

Note: The versions of the `pg_repack` extension and the `pg_repack` binary must match, and the requirements may change between versions, so use the following as a general guide but be prepared to figure things out as you go.

Using the version of the installed extension noted above:

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

You will be prompted for the superuser password when you run the script if you do not provide it in the `.env` file.

### Running `pg_repack` on all tables using the provided script

The following describes how to install and run the repack_all.sh script. This script will build a list of all tables sorted by size ascending, and will repack each in the same order. We are processing smaller tables first to help ensure we have enough free space to handle our larger tables.

Once a table is successfully repacked, it will be removed from the repack pick list and the script will continue to the next table. If a table fails to repack, the script will exit and the offending table will be the first in the list, which you should remove before running the script again. Everything is logged to a file called `repack.log`. The script will also log the time it took to repack each table, so you can get an idea of how long it will take to run.

We run the script in a `screen` session to ensure it continues running if we disconnect from the instance. Do not run this from your workstation.

1. Copy the repack_all.sh script to the instance
2. Make the file executable with `chmod +x repack_all.sh`
3. Run the script in a `screen` session with `screen -S repack` and then `./repack_all.sh`
4. Detach from the screen session with `Ctrl-a d`.
5. Monitor the script by tailing the repack.log file with `tail -f repack.log`
6. Reattach to the screen session with `screen -r repack` to check on the progress


Note:

- If repack_all.sh fails to repack a table, you will need to remove the table from the pick list and run the script again.
- If you stop or repack fails for any reason, you will need to clean up manually as pg_repack will leave behind temp tables, triggers, etc.

You can do this by running `./repack_all.sh reset` or by executing the following sql (repack_all.sh will do this automatically after a successful run):

```sql
DROP EXTENSION pg_repack CASCADE;
CREATE EXTENSION pg_repack;
```

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

