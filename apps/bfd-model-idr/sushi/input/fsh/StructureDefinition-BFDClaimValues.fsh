Logical: BFDClaimValues
Id: BFDClaimValues
Title: "IDR BFDClaimValues Base. Ignore in DD Generation"
Description: "IDR BFDClaimValues Base. Ignore in DD Generation"
* ^url = "http://hl7.org/fhir/StructureDefinition/BFDClaimValues"
* ^name = "BFDClaimValues"
* ^status = #draft
* ^abstract = false
* ^type = "BFDClaimValues"
* ^baseDefinition = "http://hl7.org/fhir/StructureDefinition/Base"
* . ^label = "IDR BFDClaimValues Base. Ignore in DD Generation"

* CLM_VAL_CD 0..1 string "Claim Value Code" "The code representing the claim amount type."
* CLM_VAL_AMT 0..1 string "Claim Value Amount" "The claim amount."
