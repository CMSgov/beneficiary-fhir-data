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

To load from `apps/bfd-model-idr/out`, just run:

```sh
./run-db.sh
```

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

## Settings

The pipeline has many settings that can be tweaked for different kinds of loads.
These are all done using environment variables starting with `IDR_`.
See `settings.py` for the current list of settings.

## Loading synthetic data into a live environment

Test data must be generated first (see details in `bfd-model-idr`)
(replace the value of `BFD_ENV` with the environment name you want to target).

```sh
BFD_ENV=1234-test ./load-synthetic-env.sh
```

## Running against Snowflake data

Set up credentials

```sh
source ./load-credentials.sh
```

Run the app (optionally specify a minimum transaction date)

```sh
PIPELINE_MIN_TRANSACTION_DATE=2024-01-01 uv run ./pipeline.py
```

## Adding data to the model

- Add the data to `mock-idr.sql` (local representation of the IDR schema)
- Update migrations
- Add the data to `model.py`, queries will be auto-generated using those fields
- Add the data to `generator_util.py`, for synthetic data generation
- If adding a new table, register it in `main` for the corresponding states (initial load vs incremental load and bene only vs claims only vs all claims load-in) in `pipeline.py`
- If adding a new table, register it in the list of CSVs to load in `load_synthetic.py`

## Loading synthetic data

First, ensure you've generated some synthetic data - see directions in `bfd-model-idr`

Load it into the IDR mock database and run the pipeline in synthetic data mode.

```sh
BFD_DB_ENDPOINT=your_db_url BFD_DB_USERNAME=your_user BFD_DB_PASSWORD=your_password uv run load_synthetic.py
BFD_DB_ENDPOINT=your_db_url BFD_DB_USERNAME=your_user BFD_DB_PASSWORD=your_password uv run pipeline.py synthetic
```
