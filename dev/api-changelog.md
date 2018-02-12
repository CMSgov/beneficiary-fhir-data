# API Changelog

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
