# Rust?

## Goals

Need a working proof of concept that:

1. Is reasonably maintainable.
2. Reduces max latency by at least 30%.
    * A smashing success would cut it in half or better.
3. Can be deployed side-by-side with the existing code.
4. Can be activated via a feature flag.
5. Could gradually replace the entire BFD Server.

Additional constraints:

* Can't talk about this with anyone else until I can demonstrate that it's a **solid** win.
* Can't deploy it anywhere other than `test` until I've talked about it.

## Integration

How do I accomplish the integration goals above:
  a gradual, gentle replacement, bit-by-bit.

Go one query type at a time.
  Don't bother with intermediate steps like interprocess communication or Java interop;
    build a webapp that can support an entire query type end-to-end.

## Development and Test

### Problem: Test Data

In development, how am I going to feed this thing a DB that I can actually test against?

### Problem: Correctness Verification

How am I going to verify that this works as expected?
I don't think I want to reimplement our Integration Tests in Rust yet --
  it'd take too much time right now, and should be deferred until I've proven the concept.

What if I rig up the BFD Server app to passthrough queries to the Plaid app?
It'd definitely incur a performance penalty,
  but that doesn't matter for local development and testing purposes.
The penalty _might_ even be small enough that I could eventually use this mechanism
  as part of a release and testing strategy in `prod`.

The main challenge with the passthrough approach is that both apps
  would need to talk to the same DB, which doesn't work with HSQL DB.
Instead, I'll have to update the tests to use, or at least support, PostgreSQL.

#### How Will I Update the Existing Build to Use PostgreSQL?

Not as simple as it first appears: if I rig it up to launch PostgreSQL via Docker,
  there's no good way to clean up the container & DB afterwards.

If I start PostgreSQL out-of-band and pass the DB URL in, there are a number of problems:

1. We don't currently support passing in the DB username & password.
    * Solution: These can be passed in the JDBC URL, e.g.
      `jdbc:postgresql://localhost/test?user=fred&password=secret`.
2. We don't create the DB schema.

It'd also, in general, be good to add PostgreSQL support to the other projects.

### Problem: Performance Verification

How am I going to measure what performance impact all of this has?
Improving performance is the primary goal of this whole effort.
Short answer: I can't, right away. First, I'll have to either:

* Implement query passthrough, and hope that it still shows a major improvement.
* Update the performance tests (only) to directly hit the Plaid app.

Regardless, both of those options will require me to implement
  all of the deployment changes necessary to standup the Plaid app
  alongside the existing BFD Server.
That's a decent chunk of work, but I don't see how it can be avoided.

## Tooling Notes

### Installs

Installing Diesel command line tools:

```
$ brew install mysql-client
$ RUSTFLAGS="-L/usr/local/opt/mysql-client/lib" cargo install diesel_cli
```

### PostgreSQL

Need a DB server running locally to test against:

```
$ docker-compose --file dev/docker-compose.yml up --detach
```

Created a Diesel migration for the V20 Flyway schema, as follows:

```
$ diesel migration generate flyway_import_V20
```

Dumped the actual schema from the `test` environment's database, as follows:

```
      local $ ssh username@test.bfd-pipeline.example.com
bfd-pipline $ pg_dump --host=bfd-test-master.clyryngdhnko.us-east-1.rds.amazonaws.com --username=some_user --schema-only --no-owner --no-acl --no-tablespaces --clean --if-exists fhirdb > fhirdb_schema_2019-10-14.sql
bfd-pipline $ exit
      local $ scp username@test.bfd-pipeline.example.com:fhirdb_schema_2019-10-14.sql beneficiary-fhir-data.git/apps/bfd-server-plaid/migrations/2019-10-14-203330_flyway_import_V20/up.sql
```

Then I manually:

1. Cut all of the `DROP ...` statements from the top of the `up.sql` script and pasted them into the `down.sql` script.
2. Removed all of the stuff at the top, including the `SET`s and creating the schema.
    * For some unknown reason, if I left this, running `diesel ...` operations would fail with this error: "`Failed with: relation "__diesel_schema_migrations" does not exist`".

### Diesel Notes

Diesel is great! But it's also **very** opinionated and may not work for our schema.

Limitations:

1. Only support tables with a primary key: <https://github.com/diesel-rs/diesel/issues/1661>.
2. Only supports tables with up to 128 columns:
    * <https://github.com/diesel-rs/diesel/issues/359>
    * <https://github.com/diesel-rs/diesel/blob/v1.4.2/diesel/Cargo.toml>
3. Only fully supports tables; view support requires manual hackery: <https://github.com/diesel-rs/diesel/issues/1482>.
    * Workaround: <https://deterministic.space/diesel-view-table-trick.html>.

Right now, I'm very tempted to give up and go with
  <https://github.com/diesel-rs/diesel/blob/v1.4.2/diesel/Cargo.toml>, instead.
But... long-term, if Rust works out, we likely want to move everything to it.
Why not hack around the limitations now for my proof of concept and say,
  "if we want to do this we'll need to refactor our DB schema, first"?
Those refactorings aren't _bad_ ideas at all, to begin with.

A counterpoint: our schema is dumb and flat on purpose, and buys us several advantages:

1. Faster ETL... probably -- we haven't tested that hypothesis.
2. A simple input format for the CCW to produce.
3. Allows the use of auto-generated code for parsing and ORM'ing our records.

I **like** those advantages!

Maybe a better long-term strategy would be to contribute an
  absurdly-large-tables feature to Diesel.
They've already said they're open to it!
We could still do some DB schema refactorings if we really wanted to, e.g.:

* Pulling out the monthly enrollment fields,
    which would also allow us to support multiple reference years for each beneficiary.
* Fixing our table and column names, which will be the worst migration ever
    but would pay long-term dividends.
