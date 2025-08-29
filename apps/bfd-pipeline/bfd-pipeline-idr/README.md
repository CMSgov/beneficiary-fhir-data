# IDR Pipeline

## Setup

Install packages

```sh
uv sync
```

## Development

Initialize the database with test data. Test data must be generated first.

```sh
./run-db.sh
```

Run the app against a local database

```sh
uv run ./pipeline.py local
```

Run tests

```sh
TESTCONTAINERS_RYUK_DISABLED=true uv run pytest
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
- Add the data to `bfd.sql` (BFD database definition)
- Add the data to `model.py`, queries will be auto-generated using those fields
- If adding a new table, register it in the call to `load_all` in `pipeline.py`
- If adding a new table, register it in the list of CSVs to load in `load_synthetic.py` 

## Loading synthetic data

First, ensure you've generated some synthetic data - see directions in `bfd-model/bfd-model-idr/sample-data/generator`

Load it into the IDR mock database and run the pipeline in synthetic data mode.

```sh
BFD_DB_ENDPOINT=your_db_url BFD_DB_USERNAME=your_user BFD_DB_PASSWORD=your_password uv run load_synthetic.py
BFD_DB_ENDPOINT=your_db_url BFD_DB_USERNAME=your_user BFD_DB_PASSWORD=your_password uv run pipeline.py synthetic
```
