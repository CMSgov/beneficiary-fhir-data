//! Contains the FHIR structures used in v1 of this API, which uses FHIR R3.
//!
//! Note: We want to match FHIR naming conventions here, so we aren't using snake case.
#![allow(non_snake_case)]

use serde::Serialize;

/// Just about every FHIR resource and element can contain an `Extension`.
///
/// Note: Rust doesn't allow for struct inheritance; composition is used, instead.
#[derive(Clone, Debug, Serialize)]
pub struct Extension {
    pub url: String,
    #[serde(flatten)]
    pub value: ExtensionValue,
}

/// Enumerates the types of extension values.
///
/// Note: extensions can contain other extensions, though Rust doesn't allow that directly. If we
/// ever need to do support that, we'll need to wrap it in a `Box` or somesuch.
#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ExtensionValue {
    ValueIdentifier(Identifier),
}

#[derive(Debug, Serialize)]
#[serde(untagged)]
pub enum Resource {
    ExplanationOfBenefit(explanation_of_benefit::ExplanationOfBenefit),
}

#[derive(Clone, Debug, Default, Serialize)]
pub struct Reference {
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub extension: Vec<Extension>,
    pub reference: Option<String>,
}

#[derive(Debug, Default, Serialize)]
pub struct CodeableConcept {
    pub coding: Vec<Coding>,
}

#[derive(Debug, Default, Serialize)]
pub struct Coding {
    pub system: Option<String>,
    pub code: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display: Option<String>,
}

#[derive(Clone, Debug, Default, Serialize)]
pub struct Identifier {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<String>,
}

/// Unit tests for the base/shared FHIR stuctures.
#[cfg(test)]
mod tests {
    use serde::Serialize;
    use serde_json::json;

    /// Verifies that FHIR `Extension`s serialize as expected.
    #[test]
    fn serialize_extension() {
        let expected = json!({
            "extension": [{
                "url": "http://example.com/foo",
                "valueIdentifier": {
                    "value": "bar"
                }
                }],
            "other_stuff": "fizz",
        });
        let expected = serde_json::to_string(&expected).unwrap();

        let actual = ExtensionTester {
            extension: vec![super::Extension {
                url: "http://example.com/foo".to_string(),
                value: super::ExtensionValue::ValueIdentifier(super::Identifier {
                    system: None,
                    value: Some("bar".to_string()),
                }),
            }],
            other_stuff: "fizz".to_string(),
        };
        let actual = serde_json::to_string(&actual).unwrap();

        assert_eq!(expected, actual);
    }

    /// Fake FHIR structure for testing serialization of `Extension`s.
    #[derive(Debug, Serialize)]
    struct ExtensionTester {
        #[serde(skip_serializing_if = "Vec::is_empty")]
        extension: Vec<super::Extension>,
        other_stuff: String,
    }
}

/// Contains structs specific to the FHIR Bundle resource.
pub mod bundle {
    use chrono::{DateTime, Utc};
    use serde::Serialize;

    #[derive(Debug, Serialize)]
    #[serde(tag = "resourceType")]
    pub struct Bundle {
        pub id: String,
        pub meta: ResourceMeta,
        pub r#type: String,
        // FYI: FHIR has a max of 2,147,483,647, while Rust's u32 has a max of 4,294,967,295.
        pub total: u32,
        pub link: Vec<BundleLink>,
        pub entry: Vec<BundleEntry>,
    }

    #[derive(Debug, Serialize)]
    pub struct ResourceMeta {
        pub lastUpdated: DateTime<Utc>,
    }

    #[derive(Debug, Serialize)]
    pub struct BundleLink {
        pub relation: String,
        pub url: String,
    }

    #[derive(Debug, Serialize)]
    pub struct BundleEntry {
        pub resource: super::Resource,
    }
}

/// Contains structs specific to the FHIR Bundle resource.
pub mod explanation_of_benefit {
    use chrono::NaiveDate;
    use serde::Serialize;

    #[derive(Debug, Default, Serialize)]
    pub struct ExplanationOfBenefit {
        pub resourceType: String,
        pub id: String,
        pub status: Option<String>,
        pub patient: Option<super::Reference>,
        pub r#type: Option<super::CodeableConcept>,
        #[serde(skip_serializing_if = "Vec::is_empty")]
        pub identifier: Vec<super::Identifier>,
        pub insurance: Option<Insurance>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub payment: Option<Payment>,
        // TODO flesh out the rest of this
    }

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct Insurance {
        pub coverage: Option<super::Reference>,
    }

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct Payment {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub date: Option<NaiveDate>,
    }
}
