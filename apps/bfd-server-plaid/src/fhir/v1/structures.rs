//! Contains the FHIR structures used in v1 of this API, which uses FHIR R3.
//!
//! Note: We want to match FHIR naming conventions here, so we aren't using snake case.
#![allow(non_snake_case)]

use serde::Serialize;

/// Just about every FHIR resource and element can contain an `Extension`.
///
/// Note: Rust doesn't allow for struct inheritance; composition is used, instead.
#[derive(Clone, Debug, Eq, PartialEq, Serialize)]
pub struct Extension {
    pub url: String,
    #[serde(flatten)]
    pub value: ExtensionValue,
}

/// Enumerates the types of extension values.
///
/// Note: extensions can contain other extensions, though Rust doesn't allow that directly. If we
/// ever need to do support that, we'll need to wrap it in a `Box` or somesuch.
#[derive(Clone, Debug, Eq, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ExtensionValue {
    ValueIdentifier(Identifier),
}

#[derive(Debug, Serialize)]
#[serde(untagged)]
pub enum Resource {
    ExplanationOfBenefit(explanation_of_benefit::ExplanationOfBenefit),
}

#[derive(Clone, Debug, Default, Eq, PartialEq, Serialize)]
pub struct Reference {
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub extension: Vec<Extension>,
    pub reference: Option<String>,
    pub identifier: Option<Identifier>,
}

#[derive(Clone, Debug, Default, Serialize)]
pub struct CodeableConcept {
    pub coding: Vec<Coding>,
}

#[derive(Clone, Debug, Default, Serialize)]
pub struct Coding {
    pub system: Option<String>,
    pub code: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display: Option<String>,
}

#[derive(Clone, Debug, Default, Eq, PartialEq, Serialize)]
pub struct Identifier {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<String>,
}

#[derive(Clone, Debug, Default, Serialize)]
pub struct Money {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Number>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
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
        #[serde(skip_serializing_if = "Vec::is_empty")]
        pub item: Vec<Item>,
        #[serde(skip_serializing_if = "Vec::is_empty")]
        pub careTeam: Vec<CareTeam>,
        // TODO flesh out the rest of this
    }

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct CareTeam {
        pub sequence: u64,
        pub provider: super::Reference,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub role: Option<super::CodeableConcept>,
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

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct Item {
        pub sequence: u64,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub careTeamLinkId: Option<u64>,
        #[serde(flatten)]
        pub serviced: Option<Serviced>,
        #[serde(skip_serializing_if = "Vec::is_empty")]
        pub adjudication: Vec<Adjudication>,
        #[serde(skip_serializing_if = "Vec::is_empty")]
        pub detail: Vec<Detail>,
    }

    /// Enumerates the possible EOB.serviced[x] field types.
    #[derive(Clone, Debug, Serialize)]
    #[serde(rename_all = "camelCase")]
    pub enum Serviced {
        ServicedDate(NaiveDate),
    }

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct Adjudication {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub category: Option<super::CodeableConcept>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub reason: Option<super::CodeableConcept>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub amount: Option<super::Money>,
    }

    #[derive(Clone, Debug, Default, Serialize)]
    pub struct Detail {
        pub sequence: u64,
        pub r#type: Option<super::CodeableConcept>,
    }

    /// Unit tests for the explanation_of_benefit FHIR stuctures.
    #[cfg(test)]
    mod tests {
        use serde_json::json;

        /// Verifies that FHIR `Extension`s serialize as expected.
        #[test]
        fn serialize_serviced() {
            let expected = json!({
                "sequence": 1,
                "servicedDate": "2019-11-08",
            });
            let expected = serde_json::to_string(&expected).unwrap();

            let actual = super::Item {
                sequence: 1,
                careTeamLinkId: None,
                serviced: Some(super::Serviced::ServicedDate(chrono::NaiveDate::from_ymd(
                    2019, 11, 08,
                ))),
                adjudication: vec![],
                detail: vec![],
            };
            let actual = serde_json::to_string(&actual).unwrap();

            assert_eq!(expected, actual);
        }
    }
}

/// Configures Serde to serialize the specified field via its `Display` trait and deserialize it
/// via its `FromStr` trait.
///
/// Can be used on a field via `#[serde(with = "serde_string")]`.
///
/// Derived from: <https://github.com/serde-rs/serde/issues/1316>.
#[allow(dead_code)]
mod serde_string {
    use std::fmt::Display;
    use std::str::FromStr;

    use serde::{de, Deserialize, Deserializer, Serializer};

    /// Serializes values via their `Display` trait.
    pub fn serialize<T, S>(value: &T, serializer: S) -> Result<S::Ok, S::Error>
    where
        T: Display,
        S: Serializer,
    {
        serializer.collect_str(value)
    }

    /// Deserializes values via their type's `FromStr` trait.
    pub fn deserialize<'de, T, D>(deserializer: D) -> Result<T, D::Error>
    where
        T: FromStr,
        T::Err: Display,
        D: Deserializer<'de>,
    {
        String::deserialize(deserializer)?
            .parse()
            .map_err(de::Error::custom)
    }
}
