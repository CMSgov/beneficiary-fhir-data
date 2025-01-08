# Synthetic Load Converter

This script can be used to convert a synthetic data load in S3 to the non-synthetic format.
Namely, this involves creating a `ManifestList.done` file and a separate manifest for each incoming file with the slightly different format that the non-synthetic data uses.

Data will be uploaded to `Incoming/{timestamp}` in the destination bucket using the timestamp from the source bucket.

## Example Usage
```sh
poetry install

poetry run ./convert-load.py --source-bucket bft-test-etl0000 --destination-bucket bfd-1000-test-etl0000 --source-prefix Synthetic/Done/2025-01-01T12:00:00Z/
```

## Parameters:
- **--source-bucket**: S3 bucket to pull data from
- **--source-prefix**: Prefix to pull data from
- **--destination-bucket**: S3 bucket to upload modified data to

