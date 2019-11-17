use phf_codegen;

use csv;
use serde;
use serde::Deserialize;
use std::env;
use std::fs::File;
use std::io::{self, BufWriter, Write};
use std::path::{Path, PathBuf};

/// Entry point for this `build.rs` script, which Cargo will call before compiling the rest of the
/// code (in `src/`).
fn main() -> io::Result<()> {
    generate_ndc_descriptions()
}

/// Generate the `ndc_descriptions.rs` file.
fn generate_ndc_descriptions() -> io::Result<()> {
    // Parse the NDC data file.
    let ndc_data = parse_ndc_data()?;

    /*
     * The source data has multiple entries for the same NDC for some reason. To match the Java
     * code, we first build a Map containing the last entry for each NDC. HashMap replaces
     * duplicates, while PHF's Map throws an error.
     */
    let mut ndc_map = std::collections::HashMap::new();
    for ndc_record in &ndc_data {
        ndc_map.insert(&ndc_record.ndc, ndc_record);
    }

    // Generate map entries for each NDC code with its description.
    let mut builder = phf_codegen::Map::new();
    for (ndc, ndc_record) in &ndc_map {
        let ndc_description = format!(
            "\"{} - {}\"",
            ndc_record.name_propriatary, ndc_record.name_substance
        );
        builder.entry(ndc.as_str(), ndc_description.as_str());
    }

    // Create the output Rust source code file with the generated map.
    let path = Path::new(&env::var("OUT_DIR").unwrap()).join("ndc_descriptions.rs");
    let mut file = BufWriter::new(File::create(&path).unwrap());
    writeln!(
        &mut file,
        "static NDC_DESCRIPTIONS: phf::Map<&'static str, &'static str> = \n{};",
        builder.build()
    )
}

/// Represents the data we care about for records in the NDC data file.
#[derive(Debug, Deserialize, Eq, PartialEq)]
struct NdcRecord {
    #[serde(rename = "PRODUCTNDC")]
    ndc: String,
    #[serde(rename = "PROPRIETARYNAME")]
    name_propriatary: String,
    #[serde(rename = "SUBSTANCENAME")]
    name_substance: String,
}

/// Parse the NDC CSV file into `NdcRecord`s.
fn parse_ndc_data() -> io::Result<Vec<NdcRecord>> {
    let mut ndc_data = vec![];
    let ndc_path = download_ndc_data();
    let mut ndc_reader = csv::ReaderBuilder::new()
        .delimiter(b'\t')
        .from_path(ndc_path)?;
    for ndc_record in ndc_reader.deserialize() {
        let mut ndc_record: NdcRecord = ndc_record?;

        /*
         * Reformat the NDC.
         */
        use lazy_static::lazy_static;
        use regex::Regex;
        lazy_static! {
            static ref RE: Regex = Regex::new(
                r"(?x)
                ^
                # The manufacturer ID.
                (?P<manufacturer_id>[^-]+)
                -
                # The drug ID.
                (?P<drug_id>.+)
                $
                "
            )
            .unwrap();
        }
        let ndc_parsed = RE.captures(&ndc_record.ndc).and_then(|cap| {
            Some((
                cap.name("manufacturer_id").unwrap().as_str(),
                cap.name("drug_id").unwrap().as_str(),
            ))
        });
        let (manufacturer_id, drug_id) = ndc_parsed.unwrap(); // FIXME handle error
        let ndc = format!("{:0>5}-{:0>4}", manufacturer_id, drug_id);
        ndc_record.ndc = ndc;

        ndc_data.push(ndc_record);
    }

    Ok(ndc_data)
}

/// Retrieves the FDA's "products" data file from their site and returns a `PathBuf` to its
/// location on disk.
fn download_ndc_data() -> PathBuf {
    /*
     * FIXME The file at https://www.accessdata.fda.gov/cder/ndctext.zip was downloaded and
     * unpacked by the Java app. We'll need to copy that same logic here, to avoid a circular
     * dependency between that project and this one.
     */
    Path::new(&env::var("CARGO_MANIFEST_DIR").unwrap())
        .join("../../bfd-server/bfd-server-war/target/classes/fda_products_cp1252.tsv")
}
