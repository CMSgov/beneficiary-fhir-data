# API Changelog

## CBBD-385

* Created the TransformerUtils method mapEobType to map certain CCW codings into a corresponding FHIR coding where appropriate.  This moved the configuration of the EOB type, near line record id and claim type code into a single location in TransformerUtils.  It also implements a default CCW claim type code encoding in this new method.  In this way, any further changes that may be  needed in relation to these specific encodings are now consolidated into the mapEobType method.  For more information on using this new mapping mechanism please refer to the javadocs located in the TransformerUtils class source file.

* CCW claim types now map to FHIR types as follows:
	* CARRIER and OUTPATIENT -> PROFESSIONAL
	* INPATIENT, HOSPICE and SNF -> INSTITUTIONAL
	* PDE -> PHARMACY
	* HHA and DME -> not mapped(no equivalent FHIR mapping at the moment).
* The constant CODING_CCW_CLAIM_TYPE was change to a placeholder(https://bluebutton.cms.gov/developer/docs/reference/some-thing) while its old value(https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt) is now used by a new constant, CODING_NCH_CLAIM_TYPE, for the mapping of the CCW claim type code into FHIR.  	
	        

## CBBF-92

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