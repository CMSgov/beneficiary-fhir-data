# RFC Proposal

[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0001-bfd-v2-patient`
* Start Date: 2020-08-03
* RFC PR: [BFD-264-v2_Patient_RFC #313](https://github.com/CMSgov/beneficiary-fhir-data/pull/313)
* JIRA Ticket(s): [BFD-264](https://jira.cms.gov/browse/BFD-264)


This RFC proposes that our team migrate from BFD v1 STU3 Patient FHIR resource to the first normative FHIR release, R4 and implement CARIN Patient profile.

## Status
[Status]: #status

* Status: Implemented
* Implementation JIRA Ticket(s):
    * [BFD-293](https://jira.cms.gov/browse/BFD-293)
    
## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
	* [Detailed Design](#detailed-design)
	* [Current State](#current-state)
	* [Future State](#future-state)
	* [Differential View](#differential-view)
	* [Unresolved Questions](#unresolved-questions)
	* [Drawbacks](#proposed-solution-drawbacks)
	* [Notable Alternatives](#proposed-solution-notable-alternatives)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

This change will make BFD's Patient FHIR resource increase the system's conformance with FHIR R4 and industry alliance like CARIN.
This will help BFD move its platform to use the first normative release of FHIR R4. This will also make integration and 
interoperability with all our peer partners seamless by enabling to provide FHIR R4 validated and conformant Patient resources
(Beneficiaries demographic data).


## Proposed Solution
[Proposed Solution]: #proposed-solution


BFD proposes to provide a new v2 endpoint based on Patient FHRI-R4 and CARIN BB Profile. 

Sample indicative Patient v2 endpoint:  
https://localhost:1337/v2/fhir/Patient?_id=-19990000000001&_format=json


Patient FHIR R4 (US-Core, Normative release):  
http://hl7.org/fhir/R4/patient.html


CARIN Patient Profile:  
http://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-CARIN-BB-Patient.html

### Detailed Design

[Detailed Design]: #detailed-design

**HAPI Version to be used:** HAPI FHIR 4.1.0

**HAPI 4.1.0 Patient Data Model class to be used:** org.hl7.fhir.r4.model.Patient

**POC Code snippet:** 

	// Slicing for Patient Identifier
      Patient patientSlicing = new Patient();

      patientSlicing.addIdentifier().setValue("-20140000010000")
          .setSystem("https://bluebutton.cms.gov/resources/variables/bene_id").getType().addCoding()
          .setCode(
          "PI")
          .setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/IdentifierTypeCS")
          .setDisplay("Patient Internal Identifier");

      patientSlicing.addIdentifier()
          .setValue("-2b034220943953861f7b17963091ea962c13548f4b1d5f4c1013ee1779d621f4")
          .setSystem("https://bluebutton.cms.gov/resources/identifier/mbi-hash").getType()
          .addCoding().setCode("MC").setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/IdentifierTypeCS")
          .setDisplay("Patient's Medicare Number");

**Below are the payload changes being proposed in Patient resource:**


#### Changes:

##### Slicing:

Current State: No Slicing.

Future State: The element Patient.identifier is sliced based on the values of value:type, value:system.

Slicing Implementation: POC code above. Slicing codes are used from the below list:

|Code|Display|
|-|-|
|MR|Medical record number|
|MA|Patient Medicaid number|
|PI|Patient internal identifier|
|PT|Patient external identifier|
|SN|Subscriber Number|
 

##### Extensions/Code System Changes:

Race:
 
CMS/CCW Race Codes will continue to be provided. In addition US-Core OMB Race extension will also be provided. CCW Race codes will be mapped as UNK (Unknown) Race code for ALL races to support US-Core Race Code System (this may undergo change based on FINAL decision). CCW Race codes will continue to be provided to accurately reflect Race in the system.

CMS BFD Code System URL:

https://bluebutton.cms.gov/resources/variables/race/

US Core Race Code Systems URL:

http://hl7.org/fhir/us/core/StructureDefinition/us-core-race

CCW Race Codes:

|Code|Code value|
|-|-|
|0|UNKNOWN|
|1|NON-HISPANIC WHITE|
|2|BLACK (OR AFRICAN-AMERICAN)|
|3|OTHER|
|4|ASIAN/PACIFIC ISLANDER|
|5|HISPANIC|
|6|AMERICAN INDIAN / ALASKA NATIVE|

##### Patient.Active field 

Type boolean

If the patient is not deceased and has any coverage mark them active, else mark them false. We will use Death_Dt to populate this field. If deceased date is not present set Active flag to *true* else *false*.

##### MANDATORY New Fields to be Added:
None as part of FHIR-R4 Spec. But CARIN mandates some mandatory Patient fields:  

•	Identifier  
•	Name  
•	Gender  

##### Cardinality Rule Changes (From STU3 to R4)
NONE.

##### Renamed Items:
NONE.

##### Deleted Elements:
N/A (Patient.Animal)


##### R4 Code Systems Changes:


Patient.identifier:

•	https://bluebutton.cms.gov/resources/variables/bene_id/  
•	http://hl7.org/fhir/us/carin/ValueSet/carin-bb-identifier-type  
•	http://terminology.hl7.org/CodeSystem/v2-0203  

Patient.gender:

	* Change value set from http://hl7.org/fhir/ValueSet/administrative-gender to http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1
	


#### Not Supported:

•	Telecom field - we don’t send, not in CCW.  
•	Email field – we don’t send, not in CCW.  
•	Ethnicity - we don’t send, not in CCW.  
•	Communication/Language - we don’t send, not in CCW.  
•	Contact/Next of Kin - we don’t send, not in CCW.  

#### R4 Non-Conformance Errors: 

Next issue ERROR - Patient - Unable to locate profile
 http://hl7.org/fhir/us/carin/StructureDefinition/carin-bb-patient
Next issue INFORMATION - Patient.extension[0] - Unknown extension

http://hl7.org/fhir/us/core/StructureDefinition/us-core-race

Next issue INFORMATION - Patient.extension[1] - Unknown extension
http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity

Next issue INFORMATION - Patient.extension[2] - Unknown extension
http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex

#### CARIN IG:
https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-CARIN-BB-Patient.html

Summary:  
•	Mandatory: 2 elements  
•	Must-Support: 1 element  

Slices:
This structure defines the following Slices:  
•	The element Patient.identifier is sliced based on the values of value:type, value:system

### Current State
[Current State]: #current-state

[BFD v1: Current State Patient Resource](./patientReadCurrentState.json)

### Future State
[Future State]: #future-state

[BFD v2: Future State Patient Resource](./patientReadFutureState.json)

### Differential View
[Differential View]: #differential-view


[Differential_view](./patient-v1-to-v2.diff)


### Unresolved Questions
[Unresolved Questions]: #unresolved-questions


The following questions need to be resolved prior to merging this RFC:

1. Hash Identifiers in Patient resource:

Do we need to return hash identifiers? Use the identifier flag in the header to determine what to send – MBI. HICN, Hashes? We do send out BENE_ID as local patient (beneficiary) identifier (medical record number).

**RESOLVED:** Hashed MBI’s will not be provided.

2. Race code mapping for Hispanic:

What is the best approach in CMS-Blue Button to map Hispanic race to US-Core Race code (see below):

	Code			Display  
•	1002-5			American Indian or Alaska Native  
•	2028-9			Asian  
•	2054-5			Black or African American  
•	2076-8			Native Hawaiian or Other Pacific Islander  
•	2106-3			White  
•	UNK			Unknown  
•	ASKU			Asked but no answer  

**RESOLVED:**  CCW Race codes will be mapped as UNK race code for ALL races to support US-Core race Code System. CCW/SSA Race codes will continue to be provided to accurately reflect race in the system. 		

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Supporting both v1 and v2 in parallel may cause slight performance issues.


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives


NA.

## Prior Art
[Prior Art]: #prior-art

BFD project already has a reasonable amount of experience with FHIR DSTU3/R3, those experiences have been very positive.


## Future Possibilities
[Future Possibilities]: #future-possibilities

No future possibilities are being seriously considered at this time.


## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal: None