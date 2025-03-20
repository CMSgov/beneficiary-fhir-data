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

Run the app

```sh
uv run ./pipeline.py local
```

Run tests

```sh
uv run pytest
```

# Adding columns to the model

- add the column to `mock-idr.sql` (local representation of the IDR schema)
- add the column to `bfd.sql` (BFD database definition)
- add the column to `model.py`, queries will be auto-generated using those fields
