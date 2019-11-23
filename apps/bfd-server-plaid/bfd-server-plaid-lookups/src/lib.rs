//! This crate contains data and functions for the BFD Server's lookups data.
//!
//! # National Drug Code (NDC) Lookups
//!
//! These use the FDA's "products" file from <https://www.accessdata.fda.gov/cder/ndctext.zip> to
//! return a pre-computed description for NDC codes, identifying the drug in question.
//!
//! Use it, as follows:
//!
//! ```rust
//! let ndc_code = "00777-3107";
//! let ndc_description = bfd_server_plaid_lookups::ndc::lookup_ndc_description(ndc_code);
//!
//! assert_eq!(Some("Prozac - FLUOXETINE HYDROCHLORIDE"), ndc_description);
//! ```
//!
//! # National Provider Identifier (NPI) Lookups
//!
//! These use a "NPI_Coded_Display_Values_Tab.txt" file from the CCW to return a pre-computed
//! description for NPIs, identifying the provder in question.
//!
//! Use it, as follows:
//!
//! ```rust
//! let npi = "1679576722";
//! let npi_description = bfd_server_plaid_lookups::npi::lookup_npi_description(npi);
//!
//! assert_eq!(Some("DAVID A WIEBE M.D."), npi_description);
//! ```

pub mod ndc;
pub mod npi;
