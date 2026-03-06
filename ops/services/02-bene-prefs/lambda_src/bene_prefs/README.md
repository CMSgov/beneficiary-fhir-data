# README

## Overview
`bene-prefs` is a DASG system that fetches Medicare Beneficiary Preferences data directly from IDR-C and provides this data in a familiar format to BFD Partners: AB2D, BCDA, and DPC.

The [templates directory](./app/templates) stores [jinja templates](https://jinja.palletsprojects.com/en/stable/) where there is a template for each partner's:
- snowflake sql query `<partner>.sql.j2`
- fixed-width preferences file format `<partner>.prefs.j2`
- the resultant name of the preferences file `<partner>.file-name.j2`.

Below are the steps taken to generate the partner's preferences files:
1. `last_execution` is stored from the DynmoDB table that tracks execution timestamps
1. `query` is stored from the rendered query template using `last_execution` from the previous step and the current time
1. `results` are stored from the execution of the previous step's `query`
1. `preferences_data` is stored from the rendered preferences template using the `results` from the previous step
1. `file_name` is rendered to provide a name to the resultant preferences file
1. `preferences_data` is written to partner-specific AWS S3 Bucket under the `file_name` object.
1. `last_execution` is updated in the DynamoDB table that tracks execution timestamps.

### Quirks and Features

- Scheduled (daily) execution to fetch timely beneficiary preference data
- Queries, response files, and file names are supported by maintainable templates
- Supports _historical_ cross-reference relationships: a beneficiary's preference today are automatically applied to their previous identifiers

### Future Work
As of this writing, this **does not** support prospective/forward-looking cross-references automatically.
That is, if a beneficiary is issued a new MBI today, the beneficary's preferences associated with their _previous_ (historical) MBI (or MBIs) may not follow.
We're working to a solution for this in the near term as soon as the initial version supports parity with the existing EFT process is in production.
