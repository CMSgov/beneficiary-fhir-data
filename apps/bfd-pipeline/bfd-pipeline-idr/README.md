# Setup

Install packages

```sh
uv sync
```

# Development

Initialize the database with test data

```sh
./run-db.sh
```

Run the app against a local database

```sh
uv run ./pipeline.py local
```

Run tests

```sh
uv run pytest
```

# Running against live data

Set up credentials

```sh
source ./load-credentials.sh
```

Run the app (optionally specify a minimum transaction date)

```sh
PIPELINE_MIN_TRANSACTION_DATE=2024-01-01 uv run ./pipeline.py
```

# Adding columns to the model

- add the column to `mock-idr.sql` (local representation of the IDR schema)
- add the column to `bfd.sql` (BFD database definition)
- add the column to `model.py`, queries will be auto-generated using those fields
