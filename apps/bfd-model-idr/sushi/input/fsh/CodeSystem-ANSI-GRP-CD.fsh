CodeSystem: ANSI_GRP_CD
Title: "ANSI Group Code - Claim Adjustment Group Code"
Id: ANSI-GRP-CD
Description: "The Group Code is combined with the ANSI reason code to demonstrate who has financial responsibility for the amount."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/ANSI-GRP-CODE"
* ^status = #active
* ^content = #complete
* #CO "Contractual Obligations -- this group code should be used when a contractual agreement between the payer and payee, or a regulatory requirement, resulted in an adjustment. Generally, these adjustments are considered a write-off for the provider and are not billed to the patient."
* #CR "Corrections and Reversals -- this group code should be used for correcting a prior claim. It applies when there is a change to a previously adjudicated claim."
* #OA "Other Adjustments -- this group code should be used when no other group code applies to the adjustment."
* #PI "Payer Initiated Reductions -- this group code should be used when, in the opinion of the payer, the adjustment is not the responsibility of the patient, but there is no supporting contract between the provider and the payer (i.e., medical review or professional review organization adjustments)."
* #PR "Patient Responsibility -- this group should be used when the adjustment represents an amount that should be billed to the patient or insured. This group would typically be used for deductible and copay adjustments."
