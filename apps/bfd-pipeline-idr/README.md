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
> - Prior to loading data into your local database, you _may_ need to generate data using the synthetic data generators in `apps/bfd-model-idr`. If you just loading patient data, this synthetic data already exists in `apps/bfd-model-idr/synthetic-data`. Consult the `README.md` in that directory for further detail.

To load from `apps/bfd-model-idr/out`, run:

```sh
./run-db.sh
```

(see the contents of the script for examples on how to run each phase separately)

Or, you can pass the directory to load from as the first positional argument to `run-db.sh`:

```sh
./run-db.sh <directory_path>
```

This is useful for loading our synthetic data stored in our repository, or the test data, e.g.:

- `./run-db.sh ../bfd-model-idr/synthetic-data`
- `./run-db.sh ./test_samples1`
- `./run-db.sh ./test_samples2`

### Run tests

```sh
uv run pytest
```

### Debugging tests

The tests work with the VS Code testing integration (the beaker icon). By default, the pipeline spawns multiple processes
in order to load data concurrently, but this does not play nicely with the debugger. We detect when a debugger is attached
and run using threads instead of processes. This incurs a performance hit due to blocking IO, but is necessary for breakpoints
to work seamlessly.

To run a specific test: 

```sh
 uv run test/test_pipeline.py::{your_test_name}
```

### Debugging generated queries

The queries used here are heavily dynamic and sometimes it's useful to inspect the generated result.

To inspect a single query, run `IDR_SQL_LOG=1 IDR_LOG_LEVEL=warning IDR_TABLES="idr.<your_table_name>" ./run-db.sh ./test_samples1`

This will enable debug logging and only run against a single table to prevent dozens of queries from spamming the logs.
Setting `IDR_LOG_LEVEL=warning` will prevent additional logs from making it hard to find the query.

## Settings

The pipeline has many settings that can be tweaked for different kinds of loads.
These are all done using environment variables starting with `IDR_`.
See `settings.py` for the current list of settings.

## Loading synthetic data into a live environment

Data is loaded into a live environment from our Snowflake dev instance
(replace the value of `BFD_ENV` with the environment name you want to target).

This will load the current contents of Snowflake into the environment.

> [!NOTE]
>
> By default, loading synthetic data does not truncate existing tables before loading. This allows additional synthetic data to be appended.
> To perform a fresh load, pass the '--truncate' flag to the pipeline or in 'load_synthetic.py'

```sh
BFD_ENV=1234-test ./load-synthetic-env.sh
```

This will first _replace_ the contents in Snowflake with the given CSV data and then load it into the environment.

```sh
BFD_ENV=1234-test ./load-synthetic-env.sh ../bfd-model-idr/synthetic-data
```

## Loading synthetic data into your local database

The steps above also apply, but run `./load-synthetic-local.sh` instead.

## Running against production data

Set up credentials

```sh
source ./load-credentials.sh
```

Run the app (optionally specify a minimum transaction date)

```sh
PIPELINE_MIN_TRANSACTION_DATE=2024-01-01 uv run idr-pipeline
```

## Adding data to the model

- Add the data to `mock-idr.sql` (local representation of the IDR schema)
- Update migrations, both for our DB (`bfd-db-migrator-ng` project) and the IDR synthetic environment (`bfd-db-migrator-synthetic` project)
- Add the data to `model.py`, queries will be auto-generated using those fields
- Add the data to `generator_util.py`, for synthetic data generation
- If adding a new table, register it in `main` for the corresponding states (initial load vs incremental load and bene only vs claims only vs all claims load-in) in `pipeline.py`
- If adding a new table, register it in the list of CSVs to load in `load_synthetic.py`
