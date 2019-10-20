/// Contains FHIR utiltity functions useful for all supported versions of FHIR.
use lazy_static::lazy_static;
use regex::Regex;

/// Parses the specified FHIR relative resource `Reference` value into its components. For example,
/// `Patient/123` will be parsed into `("Patient", "123")`. Invalid relative resource `Reference`s
/// will return `None`.
pub fn parse_relative_reference(reference: &str) -> Option<(&str, &str)> {
    lazy_static! {
        static ref RE: Regex = Regex::new(
            r"(?x)
            ^
            # The resource type.
            (?P<resource_type>[[:alpha:]]+)
            /
            # The resource ID.
            (?P<resource_id>[A-Za-z0-9\-\.]{1,64})
            $
            "
        )
        .unwrap();
    }
    RE.captures(reference).and_then(|cap| {
        Some((
            cap.name("resource_type").unwrap().as_str(),
            cap.name("resource_id").unwrap().as_str(),
        ))
    })
}

/// Parses the ID from the specified FHIR relative resource `Reference` value. For example,
/// `Patient/123` will be parsed into `"123"`, assuming that `resource_type_expected` is specified
/// as `"Patient"`. If the elative resource `Reference` is invalid, or if `resource_type_expected`
/// does not match, this will return `None`.
pub fn parse_relative_reference_expected(
    reference: &str,
    resource_type_expected: &str,
) -> Option<String> {
    match parse_relative_reference(reference) {
        Some((resource_type, resource_id)) => {
            if resource_type == resource_type_expected {
                Some(String::from(resource_id))
            } else {
                None
            }
        }
        None => None,
    }
}
