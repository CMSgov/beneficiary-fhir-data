Instance: explanationofbenefit-outcome
InstanceOf: SearchParameter
Description: "The outcome search parameter is meant to make it simple to query for ExplanationOfBenefit resources on the basis of their processing status."
Usage: #definition
* url = "https://bluebutton.cms.gov/fhir/SearchParameter/explanationofbenefit-outcome"
* name = "ExplanationOfBenefit_Outcome"
* status = #active
* description = "Search by ExplanationOfBenefit outcome"
* code = #outcome
* base[0] = #ExplanationOfBenefit
* type = #token
* expression = "ExplanationOfBenefit.outcome"
* comparator[0] = #eq