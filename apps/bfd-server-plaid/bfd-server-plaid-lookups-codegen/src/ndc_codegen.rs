use phf_codegen;

use crate::error;
use csv;
use serde;
use serde::Deserialize;
use std::env;
use std::fs::File;
use std::io::{BufWriter, Write};
use std::path::Path;

/// Generate the `ndc_descriptions.rs` file.
pub(crate) fn generate_ndc_descriptions() -> error::Result<()> {
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
    let result = writeln!(
        &mut file,
        "static NDC_DESCRIPTIONS: phf::Map<&'static str, &'static str> = \n{};",
        builder.build()
    )?;

    Ok(result)
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
fn parse_ndc_data() -> error::Result<Vec<NdcRecord>> {
    let ndc_data = download_ndc_data()?;
    let ndc_data: &[u8] = &ndc_data;
    let mut ndc_reader = csv::ReaderBuilder::new()
        .delimiter(b'\t')
        .from_reader(ndc_data);
    let mut ndc_records = vec![];
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
        let (manufacturer_id, drug_id) = ndc_parsed.ok_or(
            error::Error::InvalidLookupsDataError("Unexpected NDC format.".to_string()),
        )?;
        let ndc = format!("{:0>5}-{:0>4}", manufacturer_id, drug_id);
        ndc_record.ndc = ndc;

        ndc_records.push(ndc_record);
    }

    Ok(ndc_records)
}

/// Retrieves the FDA's "products" data file from their site and returns the data as UTF-8 bytes.
fn download_ndc_data() -> error::Result<Vec<u8>> {
    use encoding::types::Encoding;
    use std::io::Read;

    // Download the ZIP file from the FDA.
    let zip_url = "https://www.accessdata.fda.gov/cder/ndctext.zip";
    let mut zip_response = reqwest::get(zip_url)?;
    let mut zip_dest = tempfile::tempfile()?;
    std::io::copy(&mut zip_response, &mut zip_dest)?;

    // Extract the products file from the ZIP.
    // (Note: the exact name of the file seems to change from time to time. Yay.)
    let mut zip_archive = zip::ZipArchive::new(zip_dest)?;
    let mut products_file = zip_archive.by_name("product.txt")?;

    // Convert the products character set from CP-1252 to UTF-8.
    let mut products_cp1252_buffer = Vec::new();
    let mut products_utf8_chars = String::new();
    products_file.read_to_end(&mut products_cp1252_buffer)?;
    encoding::all::WINDOWS_1252
        .decode_to(
            &products_cp1252_buffer,
            encoding::DecoderTrap::Strict,
            &mut products_utf8_chars,
        )
        .map_err(|_| {
            error::Error::InvalidLookupsDataError("Unable to decode NDC data.".to_string())
        })?;

    Ok(products_utf8_chars.as_bytes().to_owned())
}
