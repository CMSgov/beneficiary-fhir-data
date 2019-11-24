use phf;
use lazy_static::lazy_static;
use regex::Regex;

include!(concat!(env!("OUT_DIR"), "/ndc_descriptions.rs"));

/// Returns the National Drug Code (NDC) description for the specified NDC code.
pub fn lookup_ndc_description(ndc_code: &str) -> Option<&str> {
    // Parse the requested NDC (which will not have any dashes) to work with the format used in our
    // Map.
    lazy_static! {
        static ref RE: Regex = Regex::new(
            r"(?x)
            ^
            # The manufacturer ID.
            (?P<manufacturer_id>.{5})
            # The drug ID.
            (?P<drug_id>.{4})
            # Optional suffix, which we ignore.
            .*
            $
            "
        )
        .unwrap();
    }
    let ndc_parsed = RE.captures(ndc_code).and_then(|cap| {
        Some((
            cap.name("manufacturer_id").unwrap().as_str(),
            cap.name("drug_id").unwrap().as_str(),
        ))
    });

    // If parsing succeeded, look up a match.
    match ndc_parsed {
        Some((manufacturer_id, drug_id)) => {
            let ndc_code = format!("{}-{}", manufacturer_id, drug_id);
            match NDC_DESCRIPTIONS.get(ndc_code.as_str()) {
                Some(ndc_description) => Some(ndc_description),
                None => None,
            }
        }
        None =>{
            None
        }
    }
}
