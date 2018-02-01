# API Changelog

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

## CBBF-128 Add FILL_NUM to PDE data

* Following FHIR Mapping changes were made:
	* The FILL_NUM (Fill Number) for Part D claims wasn't being mapped to FHIR. Now it is mapped to ExplanationOfBenefit.item.quantity
(`http://hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item.quantity`).
	* The DAYS_SUPLY_NUM (Days Supply) for Part D claims was re-mapped as an extension of ExplanationOfBenefit.item.quantity instead of item.modifier.
	