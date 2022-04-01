# API Changelog


## BFD-874 Fix diagnosis.sequence in V1 and V2

 * Following FHIR mapping changes were made:
  * Adds diagnosis.sequence if present in EOB.diagnosis, now, when a DRG is reported for V1 and V2, diagnosis.sequence is present in EOB.diagnosis. Previously, diagnosis.sequence was not present

```
  "diagnosis" : [ {
    "sequence" : 1,
    ,
    ,
  }]
 ```
  

## BFD-1424 Fix mtus code

Added a new extension with the correct url/system for MTUS Code and kept the old extension with the MTUS Code value that had the incorrect url/system of MTUS Count:
The old coding:
```
{
      "url" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
      "valueQuantity" : {
        "value" : 1
      }
}, 
{
      "url" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
        "code" : "3"
      }
}
```
The new coding:
```
{
      "url" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
      "valueQuantity" : {
        "value" : 1
      }
},
{
      "url" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
        "code" : "3"
      }
},
{
      "url" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd",
        "code" : "3",
        "display":"Services"
      }
```

## BFD-1446: Added focal field to v2

For V2, set eob.insurance.focal to 'true' for all hard coded eob.insurance.coverage elements

This is a Boolean field and should be set to either true or false. The definition of the field is this: "Coverage to be used for adjudication". My guess is that there will only be one insurance per claim. If this is the case then the focal should always be set to true. If there is more than one, then we need to determine if that insurance/coverage was used for adjudication or not. However, this is only for PDE claims, since it appears this is the only claim type that sets any values within the eob.insurance[N]. This is also ONLY A FIX FOR V2.
````
 "insurance" : [ {
    "focal" : true,
    ...,
 }]
````

## BFD-1383 Update V2 line item allowed charge amount mapping

Previously, the allowed charge amount in the EOB FHIR response was incorrectly being populated by the submitted charge amount data field. The value for allowed charge amount is now being populated correctly by the respective allowed charge amount data field. Note that this is only for v2
For DME the new allowed charge amount looks like:
````
"amount" : {
    "value" : 129.45,
    "currency" : "USD"
}
````

For Carrier:
````
"amount" : {
    "value" : 47.84,
    "currency" : "USD"
}
````

## BFD-1516: Map Hospice Period Count in V2

Added mapping for Hospice Period count
BENE_HOSPC_PRD_CNT => ExplanationOfBenefit.extension

This field was mapped in v1 but missing in v2, so this change is to achieve parity for this field.

The newly added information will look like:

```
"resource" : {
  "resourceType" : "ExplanationOfBenefit",
  ...
  {
    "url" : "https://bluebutton.cms.gov/resources/variables/bene_hospc_prd_cnt",
    "valueQuantity" : {
      "value" : 2
    }
  }
  ...
}
```

## BFD-1517: Map FI Number in V2

Added mapping for Fiscal Intermediary Number
FI_NUM => ExplanationOfBenefit.extension

This field was mapped in v1 but missing in v2, so this change is to achieve parity for this field.

The newly added information will look like:

```
"resource" : {
  "resourceType" : "ExplanationOfBenefit",
  ...
  {
    "url" : "https://bluebutton.cms.gov/resources/variables/fi_num",
    "valueCoding" : {
      "system" : "https://bluebutton.cms.gov/resources/variables/fi_num",
      "code" : "8299"
    }
  }
  ...
}
```

## BFD-1518: Map Revenue Center Status Indicator Code in V2

Added mapping for Revenue Status Code:
REV_CNTR_STUS_IND_CD => ExplanationOfBenefit.item.revenue.extension

This field was mapped in v1 but missing in v2, so this change is to achieve parity for this field.

The newly added extension will look like:

```
"resource" : {
  "resourceType" : "ExplanationOfBenefit",
  ...
  "item" : [ {
    ...
    "revenue" : {
          "extension" : [ {
            "url" : "https://bluebutton.cms.gov/resources/variables/rev_cntr_stus_ind_cd",
            "valueCoding" : {
              "system" : "https://bluebutton.cms.gov/resources/variables/rev_cntr_stus_ind_cd",
              "code" : "A",
              "display" : "Services not paid under OPPS; uses a different fee schedule (e.g., ambulance, PT, mammography)"
            }
          } ],
          ...
    },
    ...
  } ],
  ...
}
```

## BFD-1566: Add Patient.meta.tag entry for Some Patients

Our system has delayed the processing of demographic and enrollment data
  for some persons who had previously been enrolled in Medicare
  but are not enrolled in Medicare for the current year.
This delay is due to errors in
  how that data has been sent to our system for processing
  that only impact such persons.
Only around 0.3% of persons we have records for are impacted by this issue.

Nevertheless, for such impacted persons,
  their `Patient` resources are being tagged,
  as follows:

```
{
  "resourceType": "Patient",
  ...
  "meta": {
    ...
    "tag": [
      {
        "system": "https://bluebutton.cms.gov/resources/codesystem/tags",
        "code": "delayed-backdated-enrollment",
        "display": "Impacted by delayed backdated enrollment data."
      }
    ]
  },
  ...
```

## BFD-1338: Add 2021 CPT Codes for SAMHSA Filtering

Added three new codes to `codes-cpt.csv`:
```
G1028
G2215
G2216
```

These new codes allow for enhanced SAMHSA filtering for the v1 and v2 endpoints to remain compliant with SAMHSA data sharing policy.

## BLUEBUTTON-865: Adding Carrier & DME Tax Numbers to ExplanationOfBenefit resource

A new optional flag has been added that will cause tax numbers from a claim to be included in response `ExplanationOfBenefit` resources.
To enable this, set an "`IncludeTaxNumbers: true`" HTTP header in the `/ExplanationOfBenefit` request.

The added fields will look like:

```
"resource" : {
  "resourceType" : "ExplanationOfBenefit",
  ...
  "careTeam" : [
    ... ,
    {
      "sequence" : 42,
      "provider" : {
        "identifier" : {
          "system" : "http://terminology.hl7.org/CodeSystem/v2-0203",
          "value" : "9994931888"
        }
      },
      "responsible" : true,
      "role" : {
        "coding" : [ {
          "system" : "http://hl7.org/fhir/claimcareteamrole",
          "code" : "other",
          "display" : "Other"
        } ]
      }
    },
    ...
  ],
}
```


## BCDA-3872: Add service-date query parameter on ExplanationOfBenefit resources
A new optional query parameter has been added to ExplanationOfBenefit searches that will filter the returned results by type their respective service dates.

Some examples:

* Query for claims data that fall between 1999-10-27 and 2016-01-27 (inclusive)
  ```
  /v1/fhir/ExplanationOfBenefit?service-date=ge1999-10-27&service-date=le2016-01-27
  ```
* Query for claims data that occurred before the EOY 2020
  ```
  /v1/fhir/ExplanationOfBenefit?service-date=le2020-12-31
  ```

If the new parameter is not included, all EOBs will still be returned:

For more details, please see the associated [RFC].(https://github.com/CMSgov/beneficiary-fhir-data/blob/master/rfcs/0007-service-date-filter.md)

## BFD-8: Add and map coverage fields dual01..dual12 and rfrnc_yr to the coverage resource

The dual_01..dual12 and rfnrc_yr are essentially coverage related fields and need to be included in the coverage resource. Previously, these fields were only available on the patient resource and when beneficiaries choose to redact their demographic data, downstream consumers lose these vital eligibility fields thus the need to add them to the coverage resource. In future versions of the API, we'll want to consider removing these from the patient resource. 

## BLUEBUTTON-1843: HAPI 4.1.0 Upgrade

As part of the upgrade of the HAPI package used by the BFD, the FHIR version was changed from `3.0.1` to `3.0.2`. 
This is a minor FHIR version change. 

The `CapabilitiesStatement` resource returned by the metadata endpoint reflects this change. 
In the resource the `fhirVersion` field changed and the profile path changed from:
```
"reference" : "http://hl7.org/fhir/StructureDefinition/<Resource Type>"
```
to:
```
"reference" : "http://hl7.org/fhir/Profile/<Resource Type>"
```

## BLUEBUTTON-1679: Hashed HICN needs to be removed from Patient Resource

The Hashed HICN identifier is removed from the Patient resource response. This is to ensure that we are in compliance for the HICN rule. This is to leave no traces of the HICN-hash in any external facing data requests to BFD. 

For internal facing requests using the `IncludeIdentifiers` header, the Hashed HICN identifier will still be included in the response for the following values: [ "true", "hicn" ]. 

The following is an example of the identifier that is NO LONGER included in external facing responses:

    <identifier>
       <system value="https://bluebutton.cms.gov/resources/identifier/hicn-hash"></system>
       <value value="96228a57f37efea543f4f370f96f1dbf01c3e3129041dba3ea4367545507c6e7"></value>
    </identifier>

## BLUEBUTTON-1784: Search for patients by ptdcntrct

The Patient resource supports searching a part D Contract Number for a given month:

Requests using this feature should be of the form `v1/fhir/Patient/?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct<month code>|<contract id>`.
For example: `v1/fhir/Patient/?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct02|S0000`

#### valid parameter systems:
```
https://bluebutton.cms.gov/resources/variables/ptdcntrct01
https://bluebutton.cms.gov/resources/variables/ptdcntrct02
https://bluebutton.cms.gov/resources/variables/ptdcntrct03
https://bluebutton.cms.gov/resources/variables/ptdcntrct04
https://bluebutton.cms.gov/resources/variables/ptdcntrct05
https://bluebutton.cms.gov/resources/variables/ptdcntrct06
https://bluebutton.cms.gov/resources/variables/ptdcntrct07
https://bluebutton.cms.gov/resources/variables/ptdcntrct08
https://bluebutton.cms.gov/resources/variables/ptdcntrct09
https://bluebutton.cms.gov/resources/variables/ptdcntrct10
https://bluebutton.cms.gov/resources/variables/ptdcntrct11
https://bluebutton.cms.gov/resources/variables/ptdcntrct12
```

## BLUEBUTTON-1536: Extend `IncludeIdentifiers` header options for the Patient Resource

The `IncludeIdentifiers` header has been extended to handle values of [ "true", "hicn", "mbi' ]. 

To enable this, set an "`IncludeIdentifiers: <value>`" HTTP header in the `/Patient` request.

Multiple values can be seperated by a comma (","). For example, "`IncludeIdentifiers: hicn,mbi`" .

The following table describes the response behaviors:


HEADER: IncludeIdentifier | Response Behavior
--- | ---
\<blank\> | omit HICN and MBI
false | omit HICN and MBI
true | include HICN and MBI
hicn | include HICN
mbi | include MBI
hicn,mbi or mbi,hicn | include HICN and MBI
\<any invalid value\> | Throws an InvalidRequest exception (400 HTTP status code)


## BLUEBUTTON-1506: Support for _Since Parameter

Support for `_lastUpdated` as a search parameter is added for resources. RFC-0004 explains the details of the changes.


## BLUEBUTTON-1516: Support searching the Patient resource by hashed MBI

The Patient resource supports searching by a hashed MBI identifier:

```
https://bluebutton.cms.gov/resources/identifier/mbi-hash
```

## BLUEBUTTON-1191: Allow Filtering of EOB Searches by type

A new optional query parameter has been added to `ExplanationOfBenefit` searches that will filter the returned results by `type`.
At this time, only the `https://bluebutton.cms.gov/resources/codesystem/eob-type` codes are supported.

Some examples:

* If the new parameter is not included, all EOBs will still be returned:
    
    ```
    /v1/fhir/ExplanationOfBenefit?patient=123
    ```
    
* If only the code system is specified, all EOBs will also still be returned:
    
    ```
    /v1/fhir/ExplanationOfBenefit?patient=123&type=https://bluebutton.cms.gov/resources/codesystem/eob-type|
    ```
    
* If just a single code is specified, only EOBs matching that claim type will be returned:
    
    ```
    /v1/fhir/ExplanationOfBenefit?patient=123&type=https://bluebutton.cms.gov/resources/codesystem/eob-type|pde
    ```
    
* If just a single code is specified and no system or `|` separator is included, EOBs matching that single claim type will still be returned:
    
    ```
    /v1/fhir/ExplanationOfBenefit?patient=123&type=pde
    ```
    
* If multiple codes are specified, EOBs matching all of those claim type will be returned:
    
    ```
    /v1/fhir/ExplanationOfBenefit?patient=123&type=carrier,dme,hha,hospice,inpatient,outpatient,snf
    ```
    

## BLUEBUTTON-865: Adding plaintext HICN/MBI to Patient resource

A new optional flag has been added that will return all of a beneficiary's known HICNs and MBIs (in plaintext, both current and historical).
This feature is primarily intended for BCDA, whose ACO users need this data for patient matching purposes.
To enable this, set an "`IncludeIdentifiers: true`" HTTP header in the `/Patient` request.

The added fields will look like:

```
"resource" : {
  "resourceType" : "Patient",
  ...
  "identifier" : [
  ... ,
  {
    "extension" : [ {
      "url" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
        "code" : "current",
        "display" : "Current"
      }
    } ],
    "system" : "http://hl7.org/fhir/sid/us-medicare",
    "value" : "543217066U"
  }, {
    "extension" : [ {
      "url" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
        "code" : "current",
        "display" : "Current"
      }
    } ],
    "system" : "http://hl7.org/fhir/sid/us-mbi",
    "value" : "3456789"
  }, {
    "extension" : [ {
      "url" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
      "valueCoding" : {
        "system" : "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
        "code" : "historic",
        "display" : "Historic"
      }
    } ],
    "system" : "http://hl7.org/fhir/sid/us-medicare",
    "value" : "543217066T"
  },
  ...
  ],
  ...
}
```

## BLUEBUTTON-926: Exposing additional beneficiary coverage fields

A number of additional data fields have been added, mostly related to coverage and enrollment:

* The beneficiary's effective/coverage-start date.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/covstart>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part [AB]$/)].period.start`
* The "reference year" for beneficiary enrollment data, which identifies the calendar year that most `Patient` and `Coverage` data is from. Will always be the latest year that we have data for.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/rfrnc_yr>
    * Found at: `Patient.extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/rfrnc_yr/].valueDate`
* Monthly Part C Contract Number: The Medicare Part C contract number for the beneficiary’s Medicare Advantage (MA) plan for a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/ptc_cntrct_id_01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part C$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptc_cntrct_id_\d\d/].valueCoding.code`
* Monthly Part C PBP Number: The Medicare Part C plan benefit package (PBP) for the beneficiary’s Medicare Advantage (MA) plan for a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/ptc_pbp_id_01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part C$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptc_pbp_id_\d\d/].valueCoding.code`
* Monthly Part C Plan Type Code: The type of Medicare Part C plan for the beneficiary for a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/ptc_plan_type_cd_01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part C$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptc_plan_type_cd_\d\d/].valueCoding.code`
* Monthly Part D Contract Number: The Part D contract number for the beneficiary’s Part D plan for a given month. CMS assigns an identifier to each contract that a Part D plan has with CMS.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/ptdcntrct01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part D$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptdcntrct\d\d/].valueCoding.code`
* Monthly Part D Plan Benefit Package Number: The Part D plan benefit package (PBP) for the beneficiary’s Part D plan for a given month. CMS assigns an identifier to each PBP within a contract that a Part D plan sponsor has with CMS.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/ptdpbpid01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part D$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptdpbpid\d\d/].valueCoding.code`
* Monthly Part D Market Segment Identifier: This variable is the segment number that CMS assigns to identify a geographic market segment or subdivision of a Part D plan; the segment number allows you to determine the market area covered by the plan. The variable describes the market segment for a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/sgmtid01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part D$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/sgmtid\d\d/].valueCoding.code`
* Monthly Medicare Entitlement/Buy-In Indicators: Whether the beneficiary was entitled to Part A, Part B, or both for a given month. Also indicates whether the beneficiary’s state of residence paid his/her monthly premium for Part B coverage (and Part A if necessary). State Medicaid programs can pay those premiums for certain dual eligibles; this action is called "buying in" and so this variable is the "buy-in code."
    * Documentation: <https://bluebutton.cms.gov/resources/variables/buyin01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part [AB]$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/buyin\d\d/].valueCoding.code`
* Monthly Medicare Advantage (MA) enrollment indicators: Whether the beneficiary was enrolled in a Medicare Advantage (MA) plan during a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/hmo_ind_01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part C$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/hmo_ind_\d\d/].valueCoding.code`
* Monthly Medicare-Medicaid dual eligibility codes: Whether the beneficiary was eligible for both Medicare and Medicaid in a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/dual_01>
    * Found at: `Patient.extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/dual_\d\d/].valueCoding.code`
* Monthly cost sharing group under Part D low-income subsidy: The beneficiary’s Part D low-income subsidy cost sharing group for a given month. The Part D benefit requires enrollees to pay both premiums and cost-sharing, but the program also has a low-income subsidy (LIS) that covers some or all of those costs for certain low-income individuals, including deductibles and cost-sharing during the coverage gap.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/cstshr01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part D$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/cstshr\d\d/].valueCoding.code`
* Monthly Part D Retiree Drug Subsidy Indicators: Indicates if the beneficiary was enrolled in an employer-sponsored prescription drug plan that qualified for Part D’s retiree drug subsidy (RDS) for a given month.
    * Documentation: <https://bluebutton.cms.gov/resources/variables/rdsind01>
    * Found at: `Coverage[?(grouping.subPlan =~ /^Part D$/)].extension[?url =~ /https:\/\/bluebutton.cms.gov\/resources\/variables\/rdsind\d\d/].valueCoding.code`

## BLUEBUTTON-898: Correct `Patient.gender` codings

Fixed the `Patient.gender` codings to be correct. Previously, all beneficiaries had reported a value of `unknown` in this field.

## BLUEBUTTON-150: Display NPI code displays in EOB

Several changes have been made to these entries:

* The `Identifier.display` to the EOB has been added for NPI type fields in the appropriate claim types.

* A practitioner's/organization's name will be displayed in the appropriate `Identifier.display` for NPI fields.

* An example of what a practitioner's name will look like is as follows:

<provider>
       <identifier>
            <system value="http://hl7.org/fhir/sid/us-npi" />
            <value value="9999333999" />
       </identifier>
       <display value="DR. JOHN E DOE MD" />
</provider>

## BLUEBUTTON-266: Implement Data Server Paging

Adding paging to the backend to lessen the load on the frontend. Changes have been made to ExplanationOfBenefitResourceProvider.findByPatient to now return a resulting bundle containing the resources matching the beneficiaryId. The bundle is created from resources pulled from a new EoBBundleProvider class, returning a small sublist of the resources for each page. Links are added to the bundle for the previous and next pages as appropriate.

This implementation allows the frontend to utilize the links we've added to the bundle instead of having to filter through the data and create their own. This is not a final solution as other issues will need to be addressed regarding result integrity in the future.

## BLUEBUTTON-480: Trim Leading and Trailing Whitespace from Codes

Some of our source data erroneously included leading/trailing whitespace, which was being passed through to the `Coding` entries that it ended up being used in.

All `Coding`s returned by the API should now have leading and trailing whitespace trimmed.

## BLUEBUTTON-147: Display ICD and Procedure code displays in EOB

Several changes have been made to these entries:

* The `Coding.display` to the EOB has been added for Diagnostic and Procedure codes in the appropriate claim types.

* A descriptive name will be displayed in the `Coding.display` for Diagnosis/Procedure fields.

## BLUEBUTTON-306: Remove baseDstu3 mapping

Removing the old mapping (baseDstu3) from the servletRegistration as it was only kept to ensure backwards compatibility until the front end team completed changes. The mapping for the servletRegistration had previously be changed to "v1/fhir" as part of BLUEBUTTON-130.


## BLUEBUTTON-146: Display NDC (National Drug Code) Substance Names in EOB

Several changes have been made to these entries:

* The `Coding.display` to the EOB has been added for NDC fields in Part D, Carrier and DME.

* The Substance Name will be displayed in the `Coding.display` for NDC fields.

* The FDA NDC product file we use is downloaded from (https://www.accessdata.fda.gov/cder/ndctext.zip).
   
## BLUEBUTTON-200: Fix duplicate `ExplanationOfBenefit`s bug

A bug was fixed that had been causing duplicate `ExplanationOfBenefit` resources to be returned for most beneficiaries. (It had been generating one exact-duplicate EOB per each claim line in each claim.)

## BLUEBUTTON-185: Include HIC history data in `Patient` lookups

Beneficiaries' HIC history is now considered for patient lookup requests. This should improve our patient matching success rate to around 99%.

## CBBF-167: Removed date search parameter for EOB searches

This functionality had not been supported/surfaced by the frontend, but was still appearing in the application's capability statement (i.e. `/metadata`). Since it isn't needed or supported at this time, it was removed to correct the overall capability statement.


## CBBF-175: Fixed `ExplanationOfBenefit.diagnosis.type` Entries

Several changes have been made to these entries:

* The `Coding.system` has been changed.
    * Previously: ``
    * Corrected/current: `https://bluebutton.cms.gov/resources/codesystem/diagnosis-type`
* The `Coding.code` values have been fixed.
    * Previous format: `[CODE1]`
    * Corrected/current format: `code1`
        * In rare cases where there's more than one code, these will be captured in additional `ExplanationOfBenefit.diagnosis.type` entries.
* The `Coding.display` values are included.

## CBBF-169: Fixed Money Codings

[Money](http://hl7.org/fhir/STU3/datatypes.html#Money) values returned by the API were previously structured incorrectly per the FHIR specification and have now been corrected:

1. The `Money.system` was incorrect:
    * Previously: `USD`
    * Corrected/Current: `urn:iso:std:iso:4217`
2. The `Money.code` was missing.
    * Previously: (missing)
    * Corrected/Current: `USD`

## CBBF-97 (again): More fixes to code systems, etc.  (Sprint 47, 2018-03)

### Change `Patient.identifier` Systems

The `Identifier.system` values used here were incorrect and have been fixed:

* Beneficiary ID
    * Previously: `http://bluebutton.cms.hhs.gov/identifier#bene_id`
    * Corrected/Current: `https://bluebutton.cms.gov/resources/variables/bene_id`
* HICN Hash
    * Previously: `http://bluebutton.cms.hhs.gov/identifier#hicnHash`
    * Corrected/Current: `https://bluebutton.cms.gov/resources/identifier/hicn-hash`

Documentation for these fields is now available at the corrected URLs.

### Change `ExplanationOfBenefit.information` Entries

Several changes have been made to these entries:

1. The `ExplanationOfBenefit.information.category` codings have changed:
    a. The `Coding.system` for all of these is now `https://bluebutton.cms.gov/resources/codesystem/information`.
    b. The `Coding.code` have all been switched to reference URLs. Those URLs uniquely identify the `information` fields and can also be accessed for documentation on those fields. This is a bit unusual for a FHIR `Coding`, but should be useful in this case.
    c. The `Coding.display` values are included.
2. For `ExplanationOfBenefit.information` entries that are just used to convey a coding (which is most of them), the coded data has been moved to the `ExplanationOfBenefit.information.code` field.

### Switch Most `ExplanationOfBenefit.benefitBalance` Entries to "Adjudication Total" Extensions

Most of the former `ExplanationOfBenefit.benefitBalance` entries were not actually _benefit balances_, and so were improperly mapped. These values are instead better thought of as overall-claim-level analogues of the `ExplanationOfBenefit.item.adjudication` field.

The upcoming STU4 release of FHIR will likely include a new `ExplanationOfBenefit.total` field to accommodate this kind of information. Until then, we are representing those fields as extensions on the `ExplanationOfBenefit` resources.

The <https://bluebutton.cms.gov/resources/codesystem/adjudication-total> reference page provides a list of all these fields.

As part of this change, one of the former `ExplanationOfBenefit.benefitBalance` entries, <https://bluebutton.cms.gov/resources/variables/nch_blood_pnts_frnshd_qty>, was actually changed to an `ExplanationOfBenefit.information` entry, as that was most appropriate.

### Change `ExplanationOfBenefit.benefitBalance` Entries

Several changes have been made to these entries:

1. The `Coding.display` values for `ExplanationOfBenefit.benefitBalance.category` are now included.
2. The `Coding.system` values used for `ExplanationOfBenefit.benefitBalance.financial.type` were incorrect and have been fixed:
    * Previously: `http://bluebutton.cms.hhs.gov/coding#benefitBalanceType`
    * Corrected/Current: `https://bluebutton.cms.gov/resources/codesystem/benefit-balance`
3. The `Coding.code` values used for `ExplanationOfBenefit.benefitBalance.financial.type` were incorrect and have been fixed:
    * Previously, these had been set to what should have been the `Coding.display` values.
    * Now, they've all been switched to reference URLs. Those URLs uniquely identify the `benefitBalance` financial type fields and can also be accessed for documentation on those fields. This is a bit unusual for a FHIR `Coding`, but should be useful in this case.
4. The `Coding.display` values for `ExplanationOfBenefit.benefitBalance.financial.type` are now included.

### Change `ExplanationOfBenefit.item.adjudication` Entries

Several changes have been made to these entries:

1. The `Coding.system` values used for `ExplanationOfBenefit.item.adjudication.category` were incorrect and have been fixed:
    * Previously: "`CMS Adjudications`"
    * Corrected/Current: `https://bluebutton.cms.gov/resources/codesystem/adjudication`
2. The `Coding.code` values used for `ExplanationOfBenefit.item.adjudication.category` were incorrect and have been fixed:
    * Previously, these had been set to what should have been the `Coding.display` values.
    * Now, they've all been switched to reference URLs. Those URLs uniquely identify the `adjudication` fields and can also be accessed for documentation on those fields. This is a bit unusual for a FHIR `Coding`, but should be useful.
3. The `Coding.display` values for `ExplanationOfBenefit.item.adjudication.category` are now included.

### Include `ExplanationOfBenefit.careTeam.role` `Coding.display` Values

The `Coding.display` values for this field are now included in responses, for convenience.

### Fix `ExplanationOfBenefit.type` "FHIR Claim Type" Coding

This fix only applies to `ExplanationOfBenefit.type` `Coding`s where the `Coding.system` is `http://hl7.org/fhir/ex-claimtype`.

The `Coding.code` values used here were incorrectly uppercased and have been fixed (to lowercase). In addition, `Coding.display` values are now included for this `Coding`.

### Fix `ExplanationOfBenefit.identifier` "Prescription Reference Number" System

The `Identifier.system` values used here were incorrect and have been fixed:

* Previously: `CCW.RX_SRVC_RFRNC_NUM`
* Corrected/Current: `https://bluebutton.cms.gov/resources/variables/rx_srvc_rfrnc_num`

Documentation for this field is now available at its corrected URL.

### Change NDC Code System

The NDC `Coding.system` values used have been changed to the ones recommended by the FHIR community:

* Previously: `https://www.accessdata.fda.gov/scripts/cder/ndc`
* Improved/Current: `http://hl7.org/fhir/sid/ndc`

### Change `ExplanationOfBenefit.item.service` and `ExplanationOfBenefit.item.modifier` Code System

The HCPCS `Coding.system` values used here have been changed to point to better documentation:

* Previously: `https://www.cms.gov/Medicare/Coding/MedHCPCSGenInfo/index.html`
* Improved/Current: `https://bluebutton.cms.gov/resources/codesystem/hcpcs`

### Fix `ExplanationOfBenefit.type` "Blue Button EOB Type" Code System

The `Coding.system` values used here were incorrect and have been fixed:

* Previously: `https://bluebutton.cms.gov/developer/docs/reference/some-thing`
* Corrected/Current: `https://bluebutton.cms.gov/resources/codesystem/eob-type`

Documentation for this field is now available at its corrected URL.

### Fix `ExplanationOfBenefit.identifier` "Claim Group ID" System

The `Identifier.system` values used here were incorrect and have been fixed:

* Previously: `http://bluebutton.cms.hhs.gov/identifier#claimGroup`
* Corrected/Current: `https://bluebutton.cms.gov/resources/identifier/claim-group`

Documentation for this field is now available at its corrected URL.

### Include `ExplanationOfBenefit.item.detail.type` `Coding.display` Values

The `Coding.display` values for this field are now included in responses, for convenience. (Note: This field is only included for Part D Events.)

### Remove `http://hl7.org/fhir/ValueSet/v3-ActInvoiceGroupCode` Extension

This extension was included in all `ExplanationOfBenefit` responses (except for Part D Events), with a static/constant value. This wasn't providing any value and has accordingly been removed.

### Remove `ExplanationOfBenefit.disposition`

This field was included in all `ExplanationOfBenefit` responses, with a static/constant value. Since it's not a required field, this wasn't providing any value and has accordingly been removed.

## CBBF-97: Update URIs from "`ccwdata.org`" to "`bluebutton.cms.gov`"

The API's responses included many once-working-but-now-invalid URLs for the `ccwdata.org` domain, e.g. "`https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt`". Most of these URLs have now been updated to instead point to the "`bluebutton.cms.gov`" domain, e.g. "`https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd/`" (note that the path suffix has also changed for many fields to a longer, more expressive field name). These new URLs should all resolve to HTML pages containing the documentation that had previously only been available in the [Data Dictionary PDF codebooks](https://www.ccwdata.org/web/guest/data-dictionaries). In making this documentation more accessible, we hope the API is now easier to use.

Please note some caveats:

* This first big update fixed most of the old, broken URLs used in the API—but not all. We hope to complete the transition for the remaining URLs shortly.
* The new `bluebutton.cms.gov` pages were automatically parsed from the PDF codebooks and we're not quite done stamping out all of the bugs in that parsing. Our apologies for any oddities you encounter while we're working on that.
* The new pages haven't yet received any design love and are looking a bit rough, though the content is there. We hope to have them all shined up for you shortly.

## CBBF-139/CBBF-140: `Coding.display` Values (Sprint 46, 2018-02)

Many fields in the API now include `Coding.display` values: brief, descriptive English text that explains the meaning of the coded value. For example, see the new "`display`" value in the following sample `Patient` resource:

```
{
  "resourceType": "Patient",
  "id": "567834",
  "extension": [
    {
      "url": "https://bluebutton.cms.gov/resources/variables/race",
      "valueCoding": {
        "system": "https://bluebutton.cms.gov/resources/variables/race",
        "code": "1",
        "display": "White"
      }
    }
  ],
  ...
}
```

Please note that these values have been automatically parsed out of the [Data Dictionary PDF codebooks](https://www.ccwdata.org/web/guest/data-dictionaries) and not yet fully QA'd, so some of them will have parsing problems. That QA work is ongoing at the moment, so the problems should be resolved in the future. 

Future updates may add `Coding.display` values for additional fields.

## CBBF-138 (Sprint 45, 2018-02)
* Mapped CARR_CLM_PRMRY_PYR_PD_AMT for Carrier claims to EOB.benefitbalance.financial as "Primary Payer Paid Amount"
* Mapped IME_OP_CLM_VAL_AMT & DSH_OP_CLM_VAL_AMT for Inpatient claims to EOB.benefitbalance.financial as "Indirect Medical Education Amount" and "Disproportionate Share Amount" respectively.
* Mapped BENE_HOSPC_PRD_CNT for Hospice claims as an extension to EOB.hospitalization as https://bluebutton.cms.gov/resources/hospcprd

## CBBF-123 (Sprint 45, 20128-02)
* Added coverage extension codings for part A & B termination codes.
	1. TransformerConstant URLs have been added for both extensions respectively: https://www.ccwdata.org/cs/groups/public/documents/datadictionary/a_trm_cd.txt and https://www.ccwdata.org/cs/groups/public/documents/datadictionary/b_trm_cd.txt
* The status for part D coverage transforms now defaults to active.

## CBBF-126 (Sprint 44, 2018-02)
* Added an extension coding for DME provider billing number at the item level
	1. A temporary URL has been added https://bluebutton.cms.gov/resources/suplrnum

## CBBD-385 (Sprint 43, 2018-01)

* Standardized the [ExplanationOfBenefit.type](http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.type) field across all 8 claim types. This field's `CodeableConcept` will now have some of these possible `Coding`s:
    1. `{ "system": "https://bluebutton.cms.gov/developer/docs/reference/some-thing", "code": "<carrier,dme,hhs,hospice,inpatient,outpatient,pde,snf>" }`
        * This entry will be present for all EOBs.
        * Only one of the listed `code` values will be present on each EOB, designating the CMS claim type.
        * The "`some-thing`" system suffix value there is temporary, pending other in-progress work.
    2. `{ "system": "http://hl7.org/fhir/ex-claimtype", "code": "<institutional,pharmacy,professional>" }`
       * This entry will only be present for carrier, outpatient, inpatient, hospice, SNF, and Part D claims:
           * carrier, outpatient: `professional`
           * inpatient, hospice, SNF: `institutional`
           * Part D: `pharmacy`
           * HHA, DME: not mapped, as there are no equivalent FHIR [Claimtype](http://hl7.org/fhir/codesystem-claim-type.html) codes at the moment.
    3. `{ "system": "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt", "code": "<coded-value>" }`
        * Please note that this `Coding` had previously been mapped to an extension.
        * This entry will not be present for all claim types. See the `NCH_CLM_TYPE_CD` variable in the [Medicare Fee-For-Service Claims codebook](https://www.ccwdata.org/documents/10280/19022436/codebook-ffs-claims.pdf) for details.
    4. `{ "system": "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt", "code": "<coded-value>" }`
        * This entry will not be present for all claim types. See the `NCH_NEAR_LINE_REC_IDENT_CD` variable in the [Medicare Fee-For-Service Claims codebook](https://www.ccwdata.org/documents/10280/19022436/codebook-ffs-claims.pdf) for details.

## CBBF-92 (Sprint 42, 2018-01)

* A number of coding system URIs have been fixed:
    * The care team role coding system is now `http://hl7.org/fhir/claimcareteamrole`, where it had previously used `http://build.fhir.org/valueset-claim-careteamrole.html`.
    * The HCPCS coding system is now `https://www.cms.gov/Medicare/Coding/MedHCPCSGenInfo/index.html`, where it had previously used these:
        * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt`
        * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd1.txt`
        * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd2.txt`
        * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd3.txt`
        * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd4.txt`
    * The benefit balance coding system is now `http://hl7.org/fhir/benefit-category`, where it had previously used `http://build.fhir.org/explanationofbenefit-definitions.html#ExplanationOfBenefit.benefitBalance.category`.
        * The case of many of the values coded in this system has now been corrected to lowercase, as well.	
      
## CBBD-386 Map NDC code to FHIR for Part D

* Following FHIR Mapping changes were made:
    * The NDC (National Drug Code) for Part D claims wasn't being mapped to FHIR.  Now it is mapped to ExplanatonOfBenefit.item.service (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.service`).
    * The `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_orgn_cd.txt` RIF Part D field was being mapped to ExplanationOfBenefit.item.service. Changed to now be mapped to ExplanationOfBenefit.information.
    
## CBBF-111 FHIR Mapping Change (Outpatient)

* Following FHIR Mapping changes were made:

		*  Changes to the diagnosis section (The following was done for ALL claim types that contain diagnosis codes)
			◦ Tie diagnosis.type “PRINCIPAL” to PRNCPAL_DGNS_CD or ICD_DGNS_CD1
			◦ Include PRNCPAL_DGNS_CD or ICD_DGNS_CD1, but not both since both variables store the same value and is considered primary/principal
			◦ For FST_DGNS_E_CD or ICD_DGNS_E_CD1, and ICD_DGNS_E_CD2-12, make diagnosis.type to be “FIRSTEXTERNAL”
			◦ For ICD_DGNS_E_CD2-12, make diagnosis.type to be “EXTERNAL”
			◦ Include FST_DGNS_E_CD or ICD_DGNS_E_CD1, but not both since both variables store the same value
			◦ For RSN_VISIT_CD1-3, make diagnosis.type to be “REASONFORVISIT”

		* The REV_CNTR_DT (Revenue Center Date) for Outpatient, Hospice, and HHA was not being mapped. Now it is mapped to ExplanationOfBenefit.item.serviced.date (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.serviced.date`).
		* The REV_CNTR_PMT_AMT_AMT (Revenue Center Payment Amount Amount) was the same for Outpatient, Hospice, and HHA and has been abstracted to a method in the TransformerUtils.java class.
		* Map REV_UNIT (Revenue Center Unit Count) for Outpatient, Hospice, Inpatient, SNF, and HHA to EOB.item.quantity (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.quantity`).
		* Map REV_CNTR_NDC_QTY (Revenue Center NDC Quantity) for Outpatient, Hospice, Inpatient, SNF, and HHA as an extension of the item.modifier REV_CNTR_NDC_QTY_QLFR_CD.
		* Map HCPCS_CD (Revenue Center Healthcare Common Procedure Coding System) to ExplanationOfBenefit.item.service
		* Map REV_CNTR_IDE_NDC_UPC_NUM (Revenue Center IDE, NDC, UPC Number) to an extension of ExplanationOfBenefit.item.service
		* Change code value from NCH Payment Amount to Revenue Payment Amount - description change
		* Map REV_CNTR_STUS_IND_CD (Revenue Center Status Indicator Code) to an extension of ExplanationOfBenefit.item.revenue
		
	
## CBBF-112 FHIR Mapping Change (Inpatient)

* Following FHIR Mapping changes were made:
	
		*  Changes to the diagnosis section
			◦ Tie diagnosis.type “ADMITTING” to ADMTG_DGNS_CD
			◦ Tie diagnosis.type “PRINCIPAL” to PRNCPAL_DGNS_CD or ICD_DGNS_CD1
			◦ Include PRNCPAL_DGNS_CD or ICD_DGNS_CD1, but not both since both variables store the same value and is considered primary/principal
			◦ For CLM_POA_IND_SW1-25, make extension to diagnosis
			◦ For FST_DGNS_E_CD or ICD_DGNS_E_CD1, and ICD_DGNS_E_CD2-12, make diagnosis.type to be “FIRSTEXTERNAL”
			◦ For ICD_DGNS_E_CD2-12, make diagnosis.type to be “EXTERNAL”
			◦ Include FST_DGNS_E_CD or ICD_DGNS_E_CD1, but not both since both variables store the same value
		
	* Map REV_UNIT (Revenue Center Unit Count) for Outpatient, Hospice, Inpatient, SNF, and HHA to EOB.item.quantity (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.quantity`).
	* Map REV_CNTR_NDC_QTY (Revenue Center NDC Quantity) for Outpatient, Hospice, Inpatient, SNF, and HHA as an extension of the item.modifier REV_CNTR_NDC_QTY_QLFR_CD.
	* Map CLM_DRG_CD (Claim Diagnosis Related Group Code) to ExplanationOfBenefit.diagnosis.packageCode
	* Map PRVDR_NUM (Provider Number) to ExplanationOfBenefit.provider
    

## CBBF-128 Add FILL_NUM to PDE data

* Following FHIR Mapping changes were made:
	
	* The FILL_NUM (Fill Number) for Part D claims wasn't being mapped to FHIR. Now it is mapped to ExplanationOfBenefit.item.quantity (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.quantity`).
	* The DAYS_SUPLY_NUM (Days Supply) for Part D claims was re-mapped as an extension of ExplanationOfBenefit.item.quantity instead of item.modifier.

## CBBF-110 FHIR Mapping Change (DME)

* Following FHIR Mapping changes were made:
	
	* The FI_NUM (Fiscal Intermediary Number) for Inp, Out, HHA, Hospice, and SNF was not being mapped. Now it is mapped to ExplanationOfBenefit.extension (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.extension`).
	* A second occurrence of CCLTRNUM (Clinical Trial Number) has been removed.
	* The SUPLRNUM (DMERC Line Supplier Provider Number) for DME was not being mapped. Now it is mapped to ExplanationOfBenefit.item.extension (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.extension`).
	* System and URL for MTUS_CNT now points to DME_UNIT link instead.
	* System and URL for MTUS_IND now points to UNIT_IND link instead.
	
## CBBF-109 FHIR Mapping Change (HHA)

* Following FHIR Mapping changes were made:

	* The REV_CNTR_DT (Revenue Center Date) for Outpatient, Hospice, and HHA was not being mapped. Now it is mapped to ExplanationOfBenefit.item.serviced.date (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.serviced.date`).
	* The REV_CNTR_PMT_AMT_AMT (Revenue Center Payment Amount Amount) was the same for Outpatient, Hospice, and HHA and has been abstracted to a method in the TransformerUtils.java class.
	* Updated System and URL for DME_UNIT in CarrierClaim to point to MTUS_CNT (undoing the change for Carrier from CBBF-110).
	* Updated System and URL for UNIT_IND in CarrierClaim to point to MTUS_IND (undoing the change for Carrier from CBBF-110).
	* Map REV_UNIT (Revenue Center Unit Count) for Outpatient, Hospice, Inpatient, SNF, and HHA to EOB.item.quantity (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.quantity`).
	* Map REV_CNTR_NDC_QTY (Revenue Center NDC Quantity) for Outpatient, Hospice, Inpatient, SNF, and HHA as an extension of the item.modifier REV_CNTR_NDC_QTY_QLFR_CD.
	
## CBBF-108 FHIR Mapping Change (Hospice)

* Following FHIR Mapping changes were made:

	* The REV_CNTR_PMT_AMT_AMT code value was changed to read "Revenue Center Payment Amount" in the XML for Hospice/HHA/DME/Outpatient.
	
## CBBF-106 FHIR Mapping Change (Carrier)

* Following FHIR Mapping changes were made:

	* Carrier LPRPAYCD (Line Beneficiary Primary Payer Code) had extension URL and system value changed to point to LPRPAYCD.txt.

## CBBF-135 Map Carrier CARR_LINE_CLIA_LAB_NUM

* Following FHIR mapping changes were made:

	* The field CARR_LINE_CLIA_LAB_NUM for Carrier was remapped as a valueIdentifier from a valueCodeableConcept.

## CBBF-142 ICD codes have invalid Coding.system

* Following changes have been made:

	* IcdCode.getFhirSystem() had a condition comparing a string "" to a character '' resulting in the incorrect Coding.system. This was changed to compare a character '' to a character ''.
	* Test classes were created for Diagnosis and CCWProcedure, both of which extend IcdCode, to ensure that the two classes are functioning properly.
	
## CBBF-134 Map Carrier CARR_LINE_ANSTHSA_UNIT_CNT

* Following FHIR mapping changes were made:

	* The field CARR_ANSTHSA_UNIT_CNT (Carrier Line Anesthesia Unit Count) has been mapped to item.service.extension (`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.service.extension`) only when the value is greater than zero.
	
## CBBF-146 Address FIXME's in Transformer like classes

* Following FHIR mapping changes were made:

	* The "FIXME this should be mapped as a valueQuantity, not a valueCoding" issues were addressed by creating a new common method for adding quantities to an extension instead of codeable concepts for these fields. The new method is called addExtensionValueQuantity in TransformerUtils.
	* The "FIXME this should be mapped as an extension valueIdentifier instead of as a valueCodeableConcept" issues were addressed by creating a new common method for adding identifiers to an extension instead of a codeable concept for these fields. The new method is called addExtensionValueIdentifier in TransformerUtils.
	* The "FIXME: check if this field is non-nullable and if not remove the 'if' check" issues were addressed by comparing the fields to their definition in the rif-layout-and-fhir-mapping.xlsx file. Most fields were found to be non-nullable and so the "if" check was removed.

