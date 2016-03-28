CMS BlueButton Sample Data Provenance
=====================================

For the April 2016 Codeathon (and also future development), the CMS Blue Button on FHIR team has assembled sample data. This sample data is largely derived from the [CMS 2008-2010 Data Entrepreneurs’ Synthetic Public Use File (DE-SynPUF)](https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/DE_Syn_PUF.html) data sets. This was also been combined with some additional source of randomized data, in order to make the sample data more complete, in terms of types of data available.

This document is a brief summary of the additions made to that data set, to allow for its evaluation in terms of beneficiary privacy.

## DE-SynPUF

By far the largest source of data in the Blue Button sample data is the [DE-SynPUF](https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/DE_Syn_PUF.html). This data set is documented in extensive detail at that link, including all of the steps taken to ensure that it does not violate beneficiary privacy.

## Beneficiary Names

The DE-SynPUF data does not contain beneficiary names. For our purposes, it was however important that synthetic beneficiary records have data in the name fields. These names **do not** correspond to the names of actual de-anonymized beneficiaries. To accomplish this, the following was done for these fields:

1. Beneficiary: First Name
    * A random number between `0` (inclusive) and `10^7` (exclusive) was generated. This number was prefixed with "`f`" and assigned as the field value. For example: "`f12345`".
1. Beneficiary: Last Name
    * The already-anonymized DE-SynPUF `DESYNPUF_ID` field's value was inserted here.

## National Plan and Provider Enumeration System (NPPES)

The DE-SynPUF data contained a number of claim types that are often associated with a healthcare provider. For example, Part D events typically have an associated pharmacy, as well as an associated prescriber. Another example: Part B claims typically have a doctor's office or somesuch. For all of those cases, however, the DE-SynPUF does not provide a realistic-looking NPI. If such a field is provided in the DE-SynPUF, it's just a completely random alphanumeric string. This is documented in the [DE-SynPUF Codebook](https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/SynPUF_Codebook.pdf). For example, the inpatient claim "`PRVDR_NUM`" field is documented as follows: "... these fields are random numbers/characters, with no association to any known id number."

For our purposes, it is important that any National Provider Identifiers (NPIs) be valid. This means that a given synthetic claim has to have an actual NPI, though that NPI can be selected entirely at random. So that's what was done:

1. The 2016-03-13 version of the NPPES database was downloaded from <http://download.cms.gov/nppes/NPI_Files.html>.
1. A subset of roughly 10,000 records from that database was randomly chosen, to make the data size more manageable.
1. Rows from that 10K subset were randomly selected for each DE-SynPUF record-column tuple where such data was needed.

This was done for the following specific fields:

1. Inpatient Claims: Provider NPI at Time of Claim
1. Inpatient Claims: Attending Physician NPI
1. Inpatient Claims: Operating Physician NPI
1. Inpatient Claims: Other Physician NPI
1. Outpatient Claims: Provider NPI at Time of Claim
1. Outpatient Claims: Attending Physician NPI
1. Outpatient Claims: Operating Physician NPI
1. Outpatient Claims: Other Physician NPI

## CCW-Derived Random Subset: Beneficiary Addresses (from Prescriber Addresses)

The DE-SynPUF data does not contain valid beneficiary addresses. For our purposes, it was however important that synthetic beneficiary records have an address that was "mappable", i.e. an address that could be looked up on Google Maps, or other similar services. This address **does not** correspond to the address of an actual de-anonymized beneficiary; it just needs to (likely) be a valid actual address that can be mapped. To accomplish this, the following was done:

1. The CCW was searched for all of the prescribers that were used in Part D events for more than 1,000 beneficiaries.
1. For each such prescriber, their address and NPI were selected and saved to a CSV file by themselves.
1. Rows from that CSV were randomly selected for each DE-SynPUF record-column tuple where such data was needed.

This was done for the following specific fields:

1. Beneficiary: Contact Address (Except Zip)
1. Beneficiary: Contact Address Zip

## CCW-Derived Random Subset: Fulfillment Pharmacy NPI

As mentioned above, the DE-SynPUF data does not contain valid NPIs. For our purposes, it was however important that synthetic Part D events be associated with valid NPIs for their fulfillment/servicing pharmacies. These NPIs **do not** correspond to the pharmacy for the de-anonymized event. To accomplish this, the following was done:

1. The CCW was searched for all of the pharmacies that were used in Part D events for more than 1,000 beneficiaries.
1. For each such pharmacy, their name and NPI were selected and saved to a CSV file by themselves.
1. Rows from that CSV were randomly selected for each DE-SynPUF record-column tuple where such data was needed.

This was done for the following specific fields:

1. Part D Events: Fulfillment/Servicing Pharmacy NPI

## CCW-Derived Random Subset: Prescriber NPI

As mentioned above, the DE-SynPUF data does not contain valid NPIs. For our purposes, it was however important that synthetic Part D events be associated with valid NPIs for their prescriber. These NPIs **do not** correspond to the prescriber for the de-anonymized event. To accomplish this, the following was done:

1. The CCW was searched for all of the prescribers that were used in Part D events for more than 1,000 beneficiaries.
1. For each such prescriber, their name and NPI were selected and saved to a CSV file by themselves.
1. Rows from that CSV were randomly selected for each DE-SynPUF record-column tuple where such data was needed.

This was done for the following specific fields:

1. Part D Events: Prescriber NPI

## Data Cleanup

There were some instances where the DE-SynPUF data appeared to violate CMS' business rules. When such cases were found and determined to not be representative of data that might appear in the CCW, the data was manipulated such that the business rules in question were no longer being violated.

This was done in the following cases:

1. Part D Events: Total Cost < Beneficiary Pay Amount
    * The beneficiary would never pay more than the total cost of the prescription. When such cases were encountered, the Total Cost amount was adjusted upwards to match the Patient Pay Amount.
