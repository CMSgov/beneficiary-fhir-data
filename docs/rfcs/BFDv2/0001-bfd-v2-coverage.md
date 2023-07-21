# RFC Proposal

[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-coverage-fhir-r4-carin`
* Start Date: TBD
* RFC PR: TBD
* JIRA Ticket(s):
* [BFD-265](https://jira.cms.gov/browse/BFD-265)


This RFC proposes that our team migrate from BFD v1 STU3 Coverage FHIR resource to the FHIR release, R4 and implement CARIN Coverage profile.

## Status
[Status]: #status

* Status: Implemented
* Implementation JIRA Ticket(s):
    * [BFD-344](https://jira.cms.gov/browse/BFD-344)

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

This change will make BFD's Coverage resource increase the system's conformance with FHIR R4 and industry alliance ecosystems like CARIN.
This will also make integration and interoperability with all our peer partners seamless by enabling to provide FHIR-R4 validated and conformant Coverage resources (Beneficaries Coverage and Eligibility data).


## Proposed Solution
[Proposed Solution]: #proposed-solution


BFD proposes to provide a new v2 endpoint to support Coverage FHRI-R4 and CARIN conformant resource. 

https://localhost:1337/v2/fhir/Coverage?beneficiary=-19990000000001&_format=json


Coverage FHIR R4 (US-Core, Normative release):
http://hl7.org/fhir/R4/Coverage.html


CARIN Coverage Profile:
http://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-CARIN-BB-Coverage.html

### Detailed Design

[Detailed Design]: #detailed-design

**HAPI Version to be used:** HAPI FHIR 4.1.0

**HAPI 4.1.0 Coverage Data Model class to be used:** org.hl7.fhir.r4.model.Coverage

**POC Code snippet:** 

      // Slicing implementation for Coverage Class
      coverageSlcing = new Coverage();

      coverageSlcing.addClass_().setValue("Medicare").getType().addCoding().setCode("subgroup")
          .setSystem("http://terminology.hl7.org/CodeSystem/coverage-class").setDisplay("SubGroup");
      coverageSlcing.addClass_().setValue("Part C").getType().addCoding().setCode("subplan")
          .setSystem("http://terminology.hl7.org/CodeSystem/coverage-class").setDisplay("SubPlan");


**Below are the payload changes being proposed in Coverage resource:**


#### Changes:

##### Slicing:

**Current State:** No Slicing.

**Future State:** The element Coverage.class is sliced based on the values of value:type.

Slicing Implementation: POC code above. 

Slicing codes are used from the below list:

http://terminology.hl7.org/CodeSystem/coverage-class version 4.0.1  

**Code** &emsp;  Definition  
**subgroup:** &nbsp; &nbsp;    A sub-group of an employee group    
**subplan:** &nbsp; &nbsp;     Coverage Medicaid number    


Slicing Rules:
https://www.hl7.org/fhir/valueset-resource-slicing-rules.html

 

##### Extensions/Code System Changes:

Coverage.status  
	•	Change value set 
	 From:  
		http://hl7.org/fhir/ValueSet/fm-status  
	 To:   
		http://hl7.org/fhir/ValueSet/fm-status|4.0.1  

Coverage.relationship  
	•	http://terminology.hl7.org/CodeSystem/subscriber-relationship  

Coverage.type  
	•	http://hl7.org/fhir/valueset-coverage-type.html  

Coverage.class.type  
	•	http://terminology.hl7.org/CodeSystem/coverage-class  
	


##### Coverage.status field 

Type boolean

##### MANDATORY New Fields to be Added:

Coverage.class.type  

Type of class such as 'group' or 'plan' (CodeableConcept). E.g. Medicare.  

Coverage.class.value  

Value associated with the type (String). E.g “Part D”.  

##### Additional CARIN MANDATORY New Fields to be Added:

**Subscriber Id:**

ID assigned to the subscriber.
 

**Relationship:**

Beneficiary relationship to the subscriber. The relationship between the Subscriber and the Beneficiary.
http://hl7.org/fhir/ValueSet/subscriber-relationship



##### Cardinality Rule Changes (From STU3 to R4)

**Coverage.status**

Min Cardinality changed from 0 to 1

**Coverage.beneficiary**

Min Cardinality changed from 0 to 1

**Coverage.payor**

Min Cardinality changed from 0 to 1.

**Coverage.class**

Max Cardinality changed from 1 to *


##### Renamed Items:

Coverage.class (Renamed from grouping to class)

##### Deleted Elements:

Coverage.grouping


##### R4 Code Systems Changes:


Coverage.status  
	•	Change value set from:  
		http://hl7.org/fhir/ValueSet/fm-status  
	 To:  
		http://hl7.org/fhir/ValueSet/fm-status|4.0.1  

Coverage.relationship  
	•	http://terminology.hl7.org/CodeSystem/subscriber-relationship  

Coverage.type  
	•	http://hl7.org/fhir/valueset-coverage-type.html  

Coverage.class.type  
	•	http://terminology.hl7.org/CodeSystem/coverage-class  




#### R4 Non-Conformance Errors: 

**FHIR-R4 Coverage Resource Validation errors:**

Next issue ERROR - null - cvc-complex-type.2.4.b: The content of element 'Coverage' is not complete. One of '{"http://hl7.org/fhir":id, "http://hl7.org/fhir":meta,  
 "http://hl7.org/fhir":implicitRules,  
 "http://hl7.org/fhir":language,  
 "http://hl7.org/fhir":text,  
 "http://hl7.org/fhir":contained,  
 "http://hl7.org/fhir":extension,  
 "http://hl7.org/fhir":modifierExtension,  
 "http://hl7.org/fhir":identifier,  
 "http://hl7.org/fhir":status}' is expected.  

**CARIN Validation errors:**

Next issue ERROR - Coverage - Unable to locate profile
http://hl7.org/fhir/us/carin/StructureDefinition/carin-bb-coverage
Next issue ERROR - Coverage - Unable to locate profile
 http://hl7.org/fhir/us/carin/StructureDefinition/carin-bb-Coverage
Next issue INFORMATION - Coverage.extension[0] - Unknown extension

http://hl7.org/fhir/us/core/StructureDefinition/us-core-race

Next issue INFORMATION - Coverage.extension[1] - Unknown extension
http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity

 Next issue INFORMATION - Coverage.extension[2] - Unknown extension
 http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex


#### CARIN IG:


https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-CARIN-BB-Coverage.html  

**Summary**  
Mandatory: 2 elements  
Must-Support: 1 element  

**Slices**  
This structure defines the following Slices:  
The element Coverage.identifier is sliced based on the values of value:type, value:system


### Current State
[Current State]: #current-state

[BFD v1: Current State Coverage Resource](./BFD_v1_Coverage-Current_State.txt)

### Future State
[Future State]: #future-state

[BFD v2: Future State Coverage Resource](./BFD_v2_Coverage-Future_State.txt)

### Differential View
[Differential View]: #differential-view

[Differential_view_1](./Differential_view_Coveragev2_1.PNG)

[Differential_view_2](./Differential_view_Coveragev2_2.PNG)

[Differential_view_3](./Differential_view_Coveragev2_3.PNG)


### Unresolved Questions
[Unresolved Questions]: #unresolved-questions


The following questions need to be resolved prior to merging this RFC:

1. Can raw un-hashed MBI be used a Subscriber Id? YES

**RESOLVED**


2. What code for Coverage “type” can be used  from this Code System:

http://terminology.hl7.org/CodeSystem/v3-ActCode

A code from above code system below and if this can be used? YES

**SUBSIDIZ**:	subsidized health program  

**Definition:** A government health program that provides coverage for health services to persons meeting eligibility criteria such as income, location of residence, access to other coverages, health condition, and age, the cost of which is to some extent subsidized by public funds.

**RESOLVED** 		

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