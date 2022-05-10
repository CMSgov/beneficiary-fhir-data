# Generate SQL to Remove Certain Synthetic Data

These scripts implement [BFD-1700](https://jira.cms.gov/browse/BFD-1700) to remove the legacy synthetic data specified in [BFD-1686](https://jira.cms.gov/browse/BFD-1686).

## Running the SQL

There are three directories in this directory: `test`, `prod-sbx`, and `prod`, one for each of the environments in which these scripts are intended to run.

Each `.sql` script can be piped directly into the postgres console. Use `count.sql` to get a count of all records that would be affected and `delete.sql` to perform the delete.

From the command line, the syntax is (replace the Postgres instance URI with whatever is appropriate for the environment you're using):

    cat count.sql | psql 'postgres://bfd:InsecureLocalDev@localhost:5432/bfd'

## Generating the SQL

There shouldn't be a need to generate the `.sql` scripts, unless you make changes in this directory.

The `make_sql.py` script can be run from the command line (with options) and will output the SQL needed to perform one operation in one environment.

The `generate_all_scripts.sh` script will create both `create.sql` and `delete.sql` scripts for each of the `test`, `prod-sbx`, and `prod` environments.

From the command line, simply invoke the script from within this directory:

    ./generate_all_scripts.sh