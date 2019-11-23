//! This crate contains the code generation logic that will be called by other projects to produce
//! the lookups data they need.
//!
//! # Setup
//!
//! Add this to your `Cargo.toml`:
//!
//! ```toml
//! [package]
//! build = "build.rs"
//!
//! [build-dependencies]
//! bfd-server-plaid-lookups-codegen = { path = "../bfd-server-plaid-lookups-codegen" }
//! ```
//!
//! Then, create the `build.rs` script as a sibling (not child) of the project's `src/` directory
//! and edit its `main` function as follows:
//!
//! ```rust,no_run
//! fn main() -> std::result::Result<(), bfd_server_plaid_lookups_codegen::error::Error> {
//!     bfd_server_plaid_lookups_codegen::generate_lookups()?;
//!
//!     Ok(())
//! }
//! ```

pub mod error;
mod ndc_codegen;
mod npi_codegen;

/// Generate all of the lookups data:
///
/// * `${OUT_DIR}/ndc_descriptions.rs`: A PHF map the the NDC codes and their descriptions.
/// * `${OUT_DIR}/npi_descriptions.rs`: A PHF map the the NPI numbers and their descriptions.
pub fn generate_lookups() -> error::Result<()> {
    ndc_codegen::generate_ndc_descriptions()?;
    npi_codegen::generate_npi_descriptions()?;

    Ok(())
}
