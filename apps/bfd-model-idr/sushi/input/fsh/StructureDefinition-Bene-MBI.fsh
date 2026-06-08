Logical: Bene-MBI
Id: Bene-MBI
* ^url = "http://hl7.org/fhir/StructureDefinition/Bene-MBI"
* ^status = #draft
* ^type = "Bene-MBI"
* . ^label = "IDR MBI Base Resource. Ignore in DD Generation"
* BENE_MBI_ID 1..1 string "Medicare Beneficiary Identifier" "An identifier that uniquely identifies a beneficiary and replaces the Health Insurance Claim Number (HICN) on medicare cards. The format of the Medicare Beneficiary Identifier (MBI) is 11-positions in length and contains the following values:  position 1 numeric values: 1 - 9  position 2 alphabetic values: A - Z (minus S, L, O, I, B, Z)  position 3 alphanumeric values: 0 - 9 and A - Z (minus S, L, O, I, B, Z)  position 4 numeric values: 0 - 9  position 5 alphabetic values: A - Z (minus S, L, O, I, B, Z)  position 6 alphanumeric values: 0 - 9 and a - z (minus S, L, O, I, B, Z)  position 7 numeric values: 0 - 9  position 8 alphabetic values: A - Z (minus S, L, O, I, B, Z)  position 9 alphabetic values: A - Z (minus S, L, O, I, B, Z)  position 10 numeric values: 0 - 9  position 11 numeric values: 0 - 9"
* BENE_MBI_EFCTV_DT 1..1 dateTime "MBI Effective Date" "The effective date when an MBI is assigned to a beneficiary."
* BENE_MBI_OBSLT_DT 0..1 dateTime "MBI Obsolete Date" "The end date when an MBI period is closed due to being compromised or involved in a merge cross reference action. (replaced by a new MBI)."
