# IDR Pipeline

## Setup

Install packages

```sh
uv sync
```

## Development

### Loading local synthetic data

> [!IMPORTANT]
>
> - Make sure you do not have Postgres running locally on your computer as this starts Postgres in a container.
> - Prior to loading data into your local database, you _may_ need to generate data using the synthetic data generators in `apps/bfd-model-idr`. If you just loading patient data, some synthetic data already exists in `./test_samples1` and `./test_samples2`. Consult the `README.md` in `../bfd-model-idr` for further detail generating/regenerating.

To load from `apps/bfd-model-idr/out`, run:

```sh
just pipeline --seed-from ../bfd-model-idr/out
```

(see the dependencies in the Justfile for examples on how to run each phase separately)

This is useful for loading our synthetic data stored in our repository, or the test data, e.g.:

- `just pipeline --seed-from ./test_samples1`
- `just pipeline --seed-from ./test_samples2`

### Run tests

```sh
just pipeline-tests
```

### Debugging tests

The tests work with the VS Code testing integration (the beaker icon). By default, the pipeline spawns multiple processes
in order to load data concurrently, but this does not play nicely with the debugger. We detect when a debugger is attached
and run using threads instead of processes. This incurs a performance hit due to blocking IO, but is necessary for breakpoints
to work seamlessly.

To run a specific test:

```sh
just pipeline-tests --test-name {your_test_name}
```

### Debugging generated queries

The queries used here are heavily dynamic and sometimes it's useful to inspect the generated result.

To inspect a single query, run `just pipeline --sql-log --log-level warning --tables "idr.<your_table_name>"`

This will enable debug logging and only run against a single table to prevent dozens of queries from spamming the logs.
Setting `IDR_LOG_LEVEL=warning` will prevent additional logs from making it hard to find the query.

## Settings

The pipeline has many settings that can be tweaked for different kinds of loads.
These are all done using environment variables starting with `IDR_`.
See `settings.py` for the current list of settings.

## Loading synthetic data into a live environment

Data is loaded into a live environment from our Snowflake dev instance
(replace the value of `--env` with the environment name you want to target).

This will load the current contents of Snowflake into the environment.

```sh
just pipeline --env 1234-test
```

To first ingest new data into Snowflake before loading, supply a folder containing the CSV files you wish to load as a positional argument.

```sh
just pipeline --env 1234-test --seed-from ../bfd-model-idr/out
```

> [!NOTE]
>
> By default, loading synthetic data does not truncate existing tables before loading. This allows additional synthetic data to be appended.
> To perform a fresh load, pass the '--truncate' flag to the pipeline or in 'load_synthetic.py'


This will first _replace_ the contents in Snowflake with the given CSV data and then load it into the environment.
Only the tables matching the files given will be truncated.

```sh
just pipeline --env 1234-test --seed-from ../bfd-model-idr/out --truncate
```

## Loading synthetic data into your local database

```sh
just pipeline --env local --source-env 1234-test
```

## Adding data to the model

- Add the data to `mock-idr.sql` (local representation of the IDR schema)
- Update migrations, both for our DB (`bfd-db-migrator-ng` project) and the IDR synthetic environment (`bfd-db-migrator-synthetic` project)
- Add the data to `model.py`, queries will be auto-generated using those fields
- Add the data to `generator_util.py`, for synthetic data generation
- If adding a new table, register it in `main` for the corresponding states (initial load vs incremental load and bene only vs claims only vs all claims load-in) in `pipeline.py`
- If adding a new table, register it in the list of CSVs to load in `load_synthetic.py`

## Export from Test Snowflake

We have a test snowflake environment. This process will take all of the tables that we use for Synthetic Data and will pull them to .csv of the approprate name
that can be uploaded via the idr_pipeline in bfd-pipeline-idr.

```bash
just extract-snowflake --env 1234-test
```

## Running the IDR Pipeline in ECS

Refer to the documentation on the `run-idr-pipeline` Lambda in the [`idr-pipeline` Tofuservice README](../../ops/services/04-idr-pipeline/README.md)
