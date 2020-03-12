# Beneficiary FHIR Data (BFD) Server: Plaid

The BFD Server is an API that serves FHIR-Compliant
  `Patient`, `Coverage`, and `ExplanationOfBenefit` resources representing
  [CMS](https://www.cms.gov/) Medicare beneficiary demographic, enrollment, and claims data (respectively).
The BFD Server is not intended for direct external/public use.
Instead, it's designed as a "backend" data source for other CMS APIs, including:

* [Blue Button 2.0](https://bluebutton.cms.gov/)
* [Beneficiary Claims Data API](https://bcda.cms.gov/)
* [https://dpc.cms.gov/](https://dpc.cms.gov/)

The "Plaid" application is an experimental rewrite of (some of) the BFD Server.
The goal of this experiment is to demonstrate that the BFD Server could be much more performant
  and also that re-writing it is actually a reasonable endeavor.
CMS staff can see the following page for details on the experiment and its results:
  [BFD Server Performance, Part 3: Plaid](TODO).

## Architecture

Plaid is written in the [Rust programming language](https://www.rust-lang.org/),
  a new popular language backed by Mozilla (the Firefox people).
Rust is a functional language with a strong type system,
  focused on safety, performance, and developer productivity.
It's proven to be a great fit for the BFD Server's usecase.

Plaid is structured as a [multi-package workspace](https://doc.rust-lang.org/book/ch14-03-cargo-workspaces.html)
  with the following packages/crates:

* [`bfd-server-plaid-app`](./bfd-server-plaid-app/):
    The Plaid application itself, which produces a single executable binary file
      containing all of the application's dependencies.
    See the crate comment at the top of
      [`bfd-server-plaid-app/src/main.rs`](./bfd-server-plaid-app/src/main.rs)
      for a more detailed description, including instructions on how to build and run it.
* [`bfd-server-plaid-lookups`](./bfd-server-plaid-lookups/):
    A library used by Plaid, it provides details on "lookups" data such as National Drug Codes.
    See the crate comment at the top of
      [`bfd-server-plaid-lookups/src/lib.rs`](./bfd-server-plaid-lookups/src/lib.rs)
      for a more detailed description, including instructions on how to use it.
* [`bfd-server-plaid-lookups-codegen`](./bfd-server-plaid-lookups-codegen/):
    A library used by `bfd-server-plaid-lookups`' [`build.rs` script](./bfd-server-plaid-lookups/build.rs)
      to produce the underlying lookups data structures that it needs.
    See the crate comment at the top of
      [`bfd-server-plaid-lookups-codegen/src/lib.rs`](./bfd-server-plaid-lookups-codegen/src/lib.rs)
      for a more detailed description, including instructions on how to use it.

For more details on Plaid's structure and design, see the links above.

## Development Environment Setup

For the time being, Plaid still requires pieces of the original BFD Server in order to build and run.
Please complete the development environment setup detailed here before proceeding:
  [`beneficiary-fhir-data/dev/devenv-readme.md`](../../dev/devenv-readme.md).

Beyond that, all that's needed is a working Rust toolchain, installed per the instructions here:
  <https://www.rust-lang.org/tools/install>.

Once Rust, Cargo, etc. are installed and working, follow the instructions in
  [`bfd-server-plaid-app/src/main.rs`](./bfd-server-plaid-app/src/main.rs) to build and run Plaid.