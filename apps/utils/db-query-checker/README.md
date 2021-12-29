# BFD Database Query Checker

A simple application that checks whether or not all possible variations of
  some BFD Server database queries complete within a given time limit.
Why?
Because query planners are inscrutable
  and we keep running into edge cases that blow past our time budgets
  (usually because the DB decides that a table scan is the best move).


## Running the Application

First, ensure you have installed the following prerequisites:

* [Rust](https://www.rust-lang.org/) >= v1.51.0, as installed via [rustup](https://www.rust-lang.org/learn/get-started).
* If you're looking to build the application on RHEL 7,
    you will also need to install the "Development Tools", e.g.:

    ```shell
    $ sudo yum groupinstall "Development Tools"
    ```

Then, run these commands to build and run the application:

```shell
$ cd beneficiary-fhir-data.git/apps/utils/db-query-checker/
$ DB_QUERIES_URL=postgres://localuser:insecurelocalpw@localhost:5432/bfd \
    DB_QUERIES_CONNECTIONS=10 \
    DB_QUERIES_OUTPUT=results/db_query_checker_$(date +"%Y-%m-%d-%H-%M").csv \
    cargo run --release
```

The default behavior for the query checker is to use a year starting at 2020 and continue
on thru 2021; lib.rs dynamically builds a year-month date within its loop processing which
is then added to an array of psql formatted date(s), i.e., '2020-01-01'. If you are running
the checker vs. a db that contains synthetic data, you will need to pass in the start year
since the default start year of 2020 will not result in any meaningful tests since no records
will be found. To run vs. synthetic data (or to not use the default start date of 2020) pass
in the DB_QUERIES_START_YEAR; for example:

```shell
$ cd beneficiary-fhir-data.git/apps/utils/db-query-checker/
$ DB_QUERIES_URL=postgres://localuser:insecurelocalpw@localhost:5432/bfd \
    DB_QUERIES_CONNECTIONS=10 \
    DB_QUERIES_START_YEAR=0003 \
    DB_QUERIES_OUTPUT=results/db_query_checker_$(date +"%Y-%m-%d-%H-%M").csv \
    cargo run --release
```

Alternatively,
  you can instead configure the environment variables in a `.env` file,
  and run it as follows:

```
$ cargo run --release
```

Some additional notes:

* You can copy a sample `.env` file from `env.sample`, e.g.:

    ```
    $ cp env.sample .env
    ```

* The database password will need to be percent encoded if it contains special characters, e.g.:

    ```
    $ python -c "import urllib, sys; print urllib.quote(sys.argv[1])" very%cool@password
    ```

* The application will write/overwrite its output data to the specified `DB_QUERIES_OUTPUT` CSV file.
* The application's tracing/logging output will go out to the console on `STDERR`.


### Building and Copying Around a Binary

The `cargo run --release` command builds the application,
  saves the application as a single binary/executable file in `target/release/db-query-checker`,
  and then runs that binary locally.

You can also copy that binary to another computer and run it there.
However, it's important to remember that executables generally aren't cross-platform;
  a binary built for Mac OS can't be copied and run on a Linux system.


### Cross-Compiling

<em>
Note:
The instructions in this section, should work, but don't.
If you need to run the application on RHEL 7,
  your best bet is to copy the source over to a RHEL 7 box and build it there.
See <https://github.com/rust-embedded/cross/issues/455#issuecomment-883514537>
  for details on how/why cross-compiling from MacOS to RHEL 7 is broken.
</em>

Fortunately, Rust has excellent support for cross-compilation,
  where a binary for a different target architecture and OS can be built locally.
There are actually many mechanisms for cross-compiling Rust,
  but the one recommended for this project is
  <https://github.com/rust-embedded/cross>.

To cross-compile the application for a RHEL 7 system,
  first install and configure [Docker](https://www.docker.com).

Then, run the following commands:

```
$ cargo install cross
$ cross build --target x86_64-unknown-linux-gnu --release
```

This will produce a single application binary
  in `target/x86_64-unknown-linux-gnu/release/db-query-checker`.
That binary can then be copied to whatever RHEL 7 server you wish to run it from
  and can then executed, as follows:

```shell
$ DB_QUERIES_URL=postgres://localuser:insecurelocalpw@localhost:5432/bfd \
    DB_QUERIES_CONNECTIONS=10 \
    DB_QUERIES_OUTPUT=results/db_query_checker_2021-07-07-T14-31.csv \
    db-query-checker
```

Please note that cross-compiling is slower, mostly due to its reliance on Docker:
  clean builds may take around 15 minutes.


## Engineering Design

For the time being, this application is simple and mostly hardcoded:
  it's designed to do one thing and do it well.
However, if we find future need to expand on this type of check,
  it should be quite simple to refactor and extend.

Why Rust?
Because this problem is **perfect** for Rust's `async` model:
  we need to run many iterations of slightly different test scripts,
  which all will spend most of their time waiting around for I/O to complete.
With Rust, even a single small test client system should be easily able to saturate a DB server.
(We don't actually _want_ to saturate a DB server,
  but we definitely want to keep one quite busy,
  so that this doesn't take weeks to complete.)