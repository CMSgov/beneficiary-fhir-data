use phf_codegen;

use crate::error;
use csv;
use serde;
use serde::Deserialize;
use std::env;
use std::fs::File;
use std::io::{BufWriter, Write};
use std::path::Path;

/// Generate the `npi_descriptions.rs` file.
pub(crate) fn generate_npi_descriptions() -> error::Result<()> {
    // Parse the NPI data file.
    let npi_data = parse_npi_data()?;

    // Generate map entries for each NPI with its description.
    let mut builder = phf_codegen::Map::new();
    for npi_record in &npi_data {
        let npi_description: String = match npi_record.org_name.as_str() {
            "" => {
                // NPI represents a person, so construct their display name.
                let npi_description = format!(
                    "{prefix} {first} {middle} {last} {suffix} {credential}",
                    prefix = npi_record.name_prefix,
                    first = npi_record.name_first,
                    middle = npi_record.name_middle,
                    last = npi_record.name_last,
                    suffix = npi_record.name_suffix,
                    credential = npi_record.name_credential
                );
                npi_description
            }
            _ => {
                // NPI represents an organization, so just use the org name.
                let npi_description = format!("{org}", org = npi_record.org_name);
                npi_description
            }
        };
        let npi_description = npi_description.replace("  ", " ");
        let npi_description = npi_description.trim();
        let npi_description = format!("\"{}\"", npi_description);

        builder.entry(npi_record.npi.as_str(), npi_description.as_str());
    }

    // Create the output Rust source code file with the generated map.
    let path = Path::new(&env::var("OUT_DIR").unwrap()).join("npi_descriptions.rs");
    let mut file = BufWriter::new(File::create(&path).unwrap());
    let result = writeln!(
        &mut file,
        "static NPI_DESCRIPTIONS: phf::Map<&'static str, &'static str> = \n{};",
        builder.build()
    )?;

    Ok(result)
}

/// Represents the data we care about for records in the NPI data file.
#[derive(Debug, Deserialize, Eq, PartialEq)]
struct NpiRecord {
    #[serde(rename = "NPI")] // column 0
    npi: String,
    #[serde(rename = "Provider Organization Name (Legal Business Name)")] // column 4
    org_name: String,
    #[serde(rename = "Provider Last Name (Legal Name)")] // column 5
    name_last: String,
    #[serde(rename = "Provider First Name")] // column 6
    name_first: String,
    #[serde(rename = "Provider Middle Name")] // column 7
    name_middle: String,
    #[serde(rename = "Provider Name Prefix Text")] // column 8
    name_prefix: String,
    #[serde(rename = "Provider Name Suffix Text")] // column 9
    name_suffix: String,
    #[serde(rename = "Provider Credential Text")] // column 10
    name_credential: String,
}

/// Parse the NPI CSV file into `NpiRecord`s.
fn parse_npi_data() -> error::Result<Vec<NpiRecord>> {
    let npi_data = Path::new(&env::var("CARGO_MANIFEST_DIR").unwrap())
        .join("..")
        .join("..")
        .join("bfd-server")
        .join("bfd-server-war")
        .join("src")
        .join("main")
        .join("resources")
        .join("NPI_Coded_Display_Values_Tab.txt");
    let mut npi_reader = csv::ReaderBuilder::new()
        .delimiter(b'\t')
        .double_quote(true)
        .from_path(npi_data)?;
    let mut npi_records = vec![];
    for npi_record in npi_reader.deserialize() {
        let npi_record: NpiRecord = npi_record?;
        npi_records.push(npi_record);
    }

    Ok(npi_records)
}
