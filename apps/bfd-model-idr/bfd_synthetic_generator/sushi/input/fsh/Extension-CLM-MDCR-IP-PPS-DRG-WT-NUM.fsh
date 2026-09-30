Extension: CLM_MDCR_IP_PPS_DRG_WT_NUM
Title: "Claim PPS Capital DRG Weight Number"
Description: "Deprecated 2026-01-06. The number used to determine a transfer adjusted case mix index for capital, under the prospective payment system (PPS). The number is determined by multiplying the Diagnosis Related Group Code (DRG) weight times the discharge fraction. Medicare assigns a weight to each DRG to reflect the average cost of caring for patients with the DRG compared to the average of all types of Medicare cases. This variable reflects the weight that is applied to the base payment amount. The DRG weights in this variable reflect adjustments due to patient characteristics and factors related to the stay. For example, payments are reduced for certain short stay transfers or where patients are discharged to post-acute care. Therefore, for a given DRG, the weight in this field may vary."
Id: CLM-MDCR-IP-PPS-DRG-WT-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-IP-PPS-DRG-WT-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.adjudication"
* valueDecimal 0..1
* value[x] only decimal
