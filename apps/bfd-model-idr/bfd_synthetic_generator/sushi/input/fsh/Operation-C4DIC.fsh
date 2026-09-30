Logical: Operation-C4DIC
Id: Operation-C4DIC
* ^url = "https://bfd.cms.gov/fhir/StructureDefinition/Operation-C4DIC"
* ^status = #draft
* ^type = "Operation-C4DIC"
* . ^label = "IDR Operation-C4DIC Base. Ignore in DD Generation"
* beneficiary 1..1 Beneficiary "The beneficiary information for the C4DIC bundle" "Beneficiary Information"
  * ^type.targetProfile = "https://bfd.cms.gov/fhir/StructureDefinition/Beneficiary"
* coverageArray 1..* Coverage-Base "Coverage Array" "Array of Coverage-Base elements for different Medicare parts (A, B, C, D, DUAL)"
  * ^type.targetProfile = "https://bfd.cms.gov/fhir/StructureDefinition/Coverage-Base"
* lastUpdated 1..1 dateTime "The time the C4DIC bundle was last updated" "Last Updated Time"
