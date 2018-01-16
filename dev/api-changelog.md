# API Changelog

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
           * HHA, DME: not mapped, as there are no equivalent FHIR [Claimtype]http://hl7.org/fhir/codesystem-claim-type.html() codes at the moment.
    3. TODO: NCH claim type
        * Please note that this `Coding` had previously been mapped to an extension.
    4. TODO: near line record ID

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