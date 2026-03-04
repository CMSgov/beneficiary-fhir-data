Logical: Bene-MBI
Id: Bene-MBI
* ^url = "http://hl7.org/fhir/StructureDefinition/Bene-MBI"
* ^status = #draft
* ^type = "Bene-MBI"
* . ^label = "IDR MBI Base Resource. Ignore in DD Generation"
* BENE_MBI_ID 1..1 string "Medicare Beneficiary Identifier"
* BENE_MBI_ID ^label = "Medicare Beneficiary Identifier"
* BENE_MBI_EFCTV_DT 1..1 dateTime "MBI Effective Date"
* BENE_MBI_EFCTV_DT ^label = "MBI Effective Date"
* BENE_MBI_OBSLT_DT 0..1 dateTime "MBI Obsolete Date"
* BENE_MBI_OBSLT_DT ^label = "MBI Obsolete Date"
