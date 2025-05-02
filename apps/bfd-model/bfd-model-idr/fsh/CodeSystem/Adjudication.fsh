CodeSystem: Adjudication
Title: Supporting
Id: Adjudication
Description: "This code system includes a selection of Adjudication Value Codes which convey the payers assessment of the item provided in the claim under the terms of the patient’s insurance coverage."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication"
* ^status = #active
* ^content = #complete

* #CLM_LINE_NCVRD_CHRG_AMT "Revenue Center Non-Covered Charge Amount" "The charge amount related to a revenue center code for services that are not covered by Medicare."
* #CLM_LINE_ALOWD_CHRG_AMT "Line Allowed Charge Amount" "The amount of allowed charges for the line-item service on the claim."
* #CLM_LINE_SBMT_CHRG_AMT "Line Submitted Charge Amount" "The amount of submitted charges for the line-item service on the claim."
* #CLM_LINE_PRVDR_PMT_AMT "Line Provider Payment Amount" "The amount Medicare paid for the services reported on the revenue center record."
* #CLM_LINE_BENE_PMT_AMT "Revenue Center Patient Responsibility Payment Amount" "The amount paid by the beneficiary to the provider for the line-item service."
* #CLM_LINE_BENE_PD_AMT "Revenue Center Payment Amount to Beneficiary" "The amount paid to the beneficiary for the services reported on the line item."
* #CLM_LINE_CVRD_PD_AMT	"Revenue Center Payment Amount" "Amount of payment made from the Medicare trust fund (after deductible and coinsurance amounts have been paid) for the line-item service on the claim."
* #CLM_LINE_BLOOD_DDCTBL_AMT "Revenue Center Blood Deductible Amount" "This variable is the dollar amount the beneficiary is responsible for related to the deductible for blood products that appear on the revenue center record."
* #CLM_LINE_MDCR_DDCTBL_AMT	"Revenue Center Cash Deductible Amount" "The amount of the cash deductible as submitted on the claim."
* #CLM_LINE_INSTNL_ADJSTD_AMT "Revenue Center Coinsurance/Wage Adjusted Coinsurance Amount" "This variable is the beneficiary’s liability for coinsurance for the revenue center record."
* #CLM_LINE_INSTNL_RDCD_AMT	"Revenue Center Reduced Coinsurance Amount" "For all services subject to Outpatient prospective payment system (PPS or OPPS), the amount of coinsurance applicable to the line for a particular service (as indicated by the HCPCS code) for which the provider has elected to reduce the coinsurance amount."
* #CLM_LINE_INSTNL_MSP1_PD_AMT "Revenue Center 1st MSP Paid Amount" "The amount paid by the primary payer when the payer is primary to Medicare (Medicare is a secondary)."
* #CLM_LINE_INSTNL_MSP2_PD_AMT "Revenue Center 2nd MSP Paid Amount" "The amount paid by the secondary payer when two payers are primary to Medicare (Medicare is the tertiary payer)."
* #CLM_LINE_INSTNL_RATE_AMT	"Revenue Center Rate Amount" "Charges relating to unit cost associated with the revenue center code."
* #CLM_SBMT_CHRG_AMT "Total Charge Amount" "The total charges for all services included on the claim."
