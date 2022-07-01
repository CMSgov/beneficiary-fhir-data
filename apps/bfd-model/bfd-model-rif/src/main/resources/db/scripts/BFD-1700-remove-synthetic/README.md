# Generate SQL to Remove Certain Synthetic Data

These scripts implement [BFD-1700](https://jira.cms.gov/browse/BFD-1700) to remove the legacy synthetic data specified in [BFD-1686](https://jira.cms.gov/browse/BFD-1686).

## Important After-Thoughts

We ran these scripts on `prod-sbx` beginning 05/13/2022. It took around 55 hours to complete.

In order to speed things up on `test`, we ran a different set of scripts to copy the database tables from `prod-sbx` to `test`, since we wanted `test` to end up with the same data as `prod-sbx`. Those scripts are now here under the `sql` directory.

1. Run `copy_from_prod_sbx.sql` from a detached instance that can reach `prod-sbx`. This will copy the database tables into *local* CSV files.

2. SCP those local CSV files onto a detached instance that can reach `test`. (*Tip: Use compression*)

3. Run `insert_into_test.sql` from the detached instance for `test` in the directory with the uncompressed CSV files.


## Running the SQL

There are three directories in this directory: `test`, `prod-sbx`, and `prod`, one for each of the environments in which these scripts are intended to run.

Each `.sql` script can be piped directly into the postgres console or copied and pasted. Use `count.sql` to get a count of all records that would be affected and `delete.sql` to perform the delete.

1. Begin in the directory of the proper environment:

```bash
cd sql/<environment>
```

2. Create a record of the number of rows from each table that we will be deleting (be sure to replace the example with the target database information):

```bash
cat count.sql | psql 'postgres://bfd:InsecureLocalDev@localhost:5432/fhirdb' | tee deleted_rows_$(date +%Y-%m-%d--%H-%M-%S).log
```

3. Open a terminal for deleting the records (be sure to replace the example with the target database information):

```sql
psql 'postgres://bfd:InsecureLocalDev@localhost:5432/fhirdb'
```

4. Start a transaction:

```sql
BEGIN TRANSACTION;
```

5. Copy / paste the contents of delete.sql into the terminal. Verify from the output that the correct number of rows from each table were deleted.

6. Copy / paste the contents of count.sql into the terminal to re-count the number of the targeted rows. There should not be any rows left in any table.

7. Commit the transaction if you are confident that everything ran according to plan, or else rollback to abort:

```sql
COMMIT TRANSACTION;
```

OR:

```sql
ROLLBACK;
```

8. Exit the terminal.

## Generating the SQL

There shouldn't be a need to generate the `.sql` scripts, unless you make changes in this directory.

The `make_sql.py` script can be run from the command line (with options) and will output the SQL needed to perform one operation in one environment.

The `generate_all_scripts.sh` script will create both `create.sql` and `delete.sql` scripts for each of the `test`, `prod-sbx`, and `prod` environments.

From the command line, simply invoke the script from within this directory:

```bash
./generate_all_scripts.sh
```
