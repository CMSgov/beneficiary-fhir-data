Instance: CMSConsentQuestionnaire
InstanceOf: Questionnaire
Title: "CMS Network Scope Consent Questionnaire"
Description: "A questionnaire designed to capture granular patient consent for SMART-on-FHIR scopes and data categories, serving as a source for FAST Consent resources. Descriptions and actual mappings subject to change."

* url = "http://hte.cms.gov/fhir/Questionnaire/Consent-Questionnaire"
* version = "1.0.0"
* status = #active
* subjectType = #Patient

// --- High Level Data Categories ---
* item[+]
  * linkId = "high-level-categories"
  * text = "High-Level Data Categories"
  * type = #group
  
  * item[+]
    * linkId = "all-clinical"
    * text = "Authorize ALL Clinical Data Types"
    * type = #boolean
    
  * item[+]
    * linkId = "all-claims"
    * text = "Authorize ALL Claims and Financial Data"
    * type = #boolean
    
  * item[+]
    * linkId = "sensitive-restricted"
    * text = "Authorize Sensitive / Restricted Data within selected data categories"
    * type = #boolean

// --- Clinical Data Scopes ---
* item[+]
  * linkId = "clinical-scopes"
  * text = "Clinical Data Access Scopes"
  * type = #group
  * enableWhen
    * question = "all-clinical"
    * operator = #!=
    * answerBoolean = true
  
  * item[+]
    * linkId = "scope-allergies"
    * text = "Allergies and Intolerances (AllergyIntolerance)"
    * type = #boolean
  * item[+]
    * linkId = "scope-careplan"
    * text = "Care Plans (CarePlan)"
    * type = #boolean
  * item[+]
    * linkId = "scope-careteam"
    * text = "Care Teams (CareTeam)"
    * type = #boolean
  * item[+]
    * linkId = "scope-device"
    * text = "Medical Devices (Device)"
    * type = #boolean
  * item[+]
    * linkId = "scope-diagnosticreport"
    * text = "Diagnostic Reports (DiagnosticReport)"
    * type = #boolean
  * item[+]
    * linkId = "scope-encounter"
    * text = "Encounters and Visits (Encounter)"
    * type = #boolean
  * item[+]
    * linkId = "scope-familymemberhistory"
    * text = "Family Member History (FamilyMemberHistory)"
    * type = #boolean
  * item[+]
    * linkId = "scope-goal"
    * text = "Care Goals (Goal)"
    * type = #boolean
  * item[+]
    * linkId = "scope-immunization"
    * text = "Immunizations (Immunization)"
    * type = #boolean
  * item[+]
    * linkId = "scope-medicationdispense"
    * text = "Medications Dispensed (MedicationDispense)"
    * type = #boolean
  * item[+]
    * linkId = "scope-medicationrequest"
    * text = "Medication Requests (MedicationRequest)"
    * type = #boolean
  * item[+]
    * linkId = "scope-procedure"
    * text = "Procedures (Procedure)"
    * type = #boolean
  * item[+]
    * linkId = "scope-servicerequest"
    * text = "Service Requests (ServiceRequest)"
    * type = #boolean
  * item[+]
    * linkId = "scope-specimen"
    * text = "Specimens (Specimen)"
    * type = #boolean
  * item[+]
    * linkId = "scope-medication"
    * text = "Medication Definitions (Medication - May be supported)"
    * type = #boolean

// --- Granular Clinical Elements ---
* item[+]
  * linkId = "granular-clinical"
  * text = "Granular Clinical Filtering"
  * type = #group
  * enableWhen
    * question = "all-clinical"
    * operator = #!=
    * answerBoolean = true

  // Conditions Breakdown
  * item[+]
    * linkId = "scope-condition-health-concern"
    * text = "Health Concerns (Condition?category=health-concern)"
    * type = #boolean
  * item[+]
    * linkId = "scope-condition-encounter-diagnosis"
    * text = "Encounter Diagnoses (Condition?category=encounter-diagnosis)"
    * type = #boolean
  * item[+]
    * linkId = "scope-condition-problem-list"
    * text = "Problem List Items (Condition?category=problem-list-item)"
    * type = #boolean

  // Observations Breakdown
  * item[+]
    * linkId = "scope-observation-sdoh"
    * text = "Social Determinants of Health (Observation?category=sdoh)"
    * type = #boolean
  * item[+]
    * linkId = "scope-observation-social-history"
    * text = "Social History (Observation?category=social-history)"
    * type = #boolean
  * item[+]
    * linkId = "scope-observation-laboratory"
    * text = "Laboratory Results (Observation?category=laboratory)"
    * type = #boolean
  * item[+]
    * linkId = "scope-observation-survey"
    * text = "Surveys/Assessments (Observation?category=survey)"
    * type = #boolean
  * item[+]
    * linkId = "scope-observation-vital-signs"
    * text = "Vital Signs (Observation?category=vital-signs)"
    * type = #boolean

  // Documents Breakdown
  * item[+]
    * linkId = "scope-document-clinical-note"
    * text = "Clinical Notes (DocumentReference?category=clinical-note)"
    * type = #boolean

// --- Claims / Financial Data Scopes ---
* item[+]
  * linkId = "claims-scopes"
  * text = "Claims & Insurance Scopes"
  * type = #group
  * enableWhen
    * question = "all-claims"
    * operator = #!=
    * answerBoolean = true

      * item[+]
    * linkId = "scope-coverage"
    * text = "Insurance Coverage (Coverage)"
    * type = #boolean
  * item[+]
    * linkId = "scope-claim"
    * text = "Insurance Claims (Claim)"
    * type = #boolean
  * item[+]
    * linkId = "scope-claimresponse"
    * text = "Claim Responses (ClaimResponse)"
    * type = #boolean
  * item[+]
    * linkId = "scope-eob"
    * text = "Explanation of Benefits (ExplanationOfBenefit)"
    * type = #boolean

// --- Mixed / Infrastructure Scopes (Both) ---
* item[+]
  * linkId = "infrastructure-scopes"
  * text = "Shared & Network Shared Context (Both / CMS Aligned)"
  * type = #group

  * item[+]
    * linkId = "scope-coverage"
    * text = "Insurance Coverage (Coverage)"
    * type = #boolean
  * item[+]
    * linkId = "scope-patient"
    * text = "Patient Demographics (Patient)"
    * type = #boolean
  * item[+]
    * linkId = "scope-questionnaireresponse"
    * text = "Saved Forms/Responses (QuestionnaireResponse)"
    * type = #boolean
  * item[+]
    * linkId = "scope-relatedperson"
    * text = "Associated Caregivers/Guarantors (RelatedPerson)"
    * type = #boolean
