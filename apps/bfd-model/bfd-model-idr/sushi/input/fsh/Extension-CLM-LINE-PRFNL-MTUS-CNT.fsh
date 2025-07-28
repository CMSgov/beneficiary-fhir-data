Extension: CLM_LINE_PRFNL_MTUS_CNT
Title: "The count of the total units associated with services needing unit reporting such as transportation, miles, anesthesia time units, number of services, volume of oxygen or blood units."
Id: CLM-LINE-PRFNL-MTUS-CNT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-PRFNL-MTUS-CNT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only decimal
