#!/usr/bin/env bash

# Set the SCRIPT_PATH

if [ "$BFD_PATH" = "" ]; then
    SCRIPT_PATH=`pwd`
else
    SCRIPT_PATH="$BFD_PATH/apps/bfd-model/bfd-model-rif/src/main/resources/db/scripts/BFD-1700-remove-synthetic"
fi

# Make sure the directory and file exist

if [ ! -d "$SCRIPT_PATH" ]; then
    echo "Could not find directory: $SCRIPT_PATH" 1>&2
    exit 1
fi

if [ ! -f "$SCRIPT_PATH/make_sql.py" ]; then
    echo "Could not find file: $SCRIPT_PATH/make_sql.py" 1>&2
    exit 1
fi

# Generate SQL Scripts

mkdir -p $SCRIPT_PATH/sql/test

python3 $SCRIPT_PATH/make_sql.py count test > $SCRIPT_PATH/sql/test/count.sql
python3 $SCRIPT_PATH/make_sql.py delete test > $SCRIPT_PATH/sql/test/delete.sql

mkdir -p $SCRIPT_PATH/sql/prod-sbx

python3 $SCRIPT_PATH/make_sql.py count test > $SCRIPT_PATH/sql/prod-sbx/count.sql
python3 $SCRIPT_PATH/make_sql.py delete test > $SCRIPT_PATH/sql/prod-sbx/delete.sql

mkdir -p $SCRIPT_PATH/sql/prod

python3 $SCRIPT_PATH/make_sql.py count test > $SCRIPT_PATH/sql/prod/count.sql
python3 $SCRIPT_PATH/make_sql.py delete test > $SCRIPT_PATH/sql/prod/delete.sql
