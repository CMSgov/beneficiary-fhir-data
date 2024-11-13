---
layout: page
title: FAQ
permalink: /faq.html
---

#### Table of Contents

- [BFD Basics](#bfd-basics)
  - [What is FHIR?](#what-is-fhir)
  - [What does BFD stand for?](#what-does-bfd-stand-for)
  - [What data is available via BFD?](#what-data-is-available-via-bfd)
  - [How frequently is BFD data updated?](#how-frequently-is-bfd-data-updated)
  - [What is the difference between Version 1 (v1) and Version 2 (v2) of the BFD API?](#what-is-the-difference-between-v1-version-1-and-v2-version-2-of-the-bfd-api)
  - [Where can I find the BFD data dictionary?](#where-can-i-find-the-bfd-data-dictionary)
  - [What are some important resources I can use while onboarding to the DASG ecosystem?](#what-are-some-important-resources-i-can-use-while-onboarding-to-the-dasg-ecosystem)
  - [Where can I find the definitions of commonly used terminology related to BFD?](#where-can-i-find-the-definitions-of-commonly-used-terminology-related-to-bfd)
  - [I have a question or feature request for the BFD team, where should I ask or begin the conversation?](#i-have-a-question-for-the-bfd-team-where-should-i-ask-or-begin-the-conversation)
  - [Does the API return coverage resources with future start dates?](#does-the-api-return-coverage-resources-with-future-start-dates)
  - [How do I request synthetic beneficiary data?](#how-do-i-request-synthetic-beneficiary-data)
  - [What is the difference between the 'Claim Number' in the CMS Portal, and the 'Claim ID' or 'Claim Group' in the FHIR payloads provided by BFD?](#what-is-the-difference-between-the-claim-number-in-the-cms-portal-and-the-claim-id-or-claim-group-in-the-fhir-payloads-provided-by-bfd)
  - [Why is the Provider NPI (National Provider Identifier) sometimes an invalid number, such as 9999999992, 9999999993. Similarly, why is the Tax Number sometimes 11111111111?](#why-is-the-provider-npi-national-provider-identifier-sometimes-an-invalid-number-such-as-9999999992-9999999993-similarly-why-is-the-tax-number-sometimes-11111111111)
  - [Are pending or partially adjudicated claims available? How long does it take for those claims to be available?](#are-pending-or-partially-adjudicated-claims-available-how-long-does-it-take-for-those-claims-to-be-available)
  - [What Substance Abuse and Mental Health Services Administration (SAMHSA) filtering takes place in BFD or upstream?](#what-substance-abuse-and-mental-health-services-administration-samhsa-filtering-takes-place-in-bfd-or-upstream)
  - [What are the payment-related monetary fields in BFD on the Explanation of Benefits (EOB) endpoint?](#what-are-the-payment-related-monetary-fields-in-bfd-on-the-explanation-of-benefits-eob-endpoint)
  - [What adjudicated claim types are supported by BFD?](#what-adjudicated-claim-types-are-supported-by-bfd)
  - [What values for the 'gender' field are supported by Medicare?](#what-values-for-the-gender-field-are-supported-by-medicare)
  - [I have a question for CCW, how do I ask it?](#i-have-a-question-for-ccw-how-do-i-ask-it)
  - [What PAC (Partially Adjudicated Claim) data does BFD contain?](#what-pac-partially-adjudicated-claim-data-does-bfd-contain)
- [Data Retention/Lifecycle](#data-retentionlifecycle)
  - [What is the retention policy for PAC (Partially Adjudicated Claims) data?](#what-is-the-retention-policy-for-pac-partially-adjudicated-claims-data)
  - [When a claim is updated, will a new claim with a different claim ID be created and the previous claim marked as cancelled? OR will the existing claim be updated and the status remain unchanged?](#when-a-claim-is-updated-will-a-new-claim-with-a-different-claim-id-be-created-and-the-previous-claim-marked-as-cancelled-or-will-the-existing-claim-be-updated-and-the-status-remain-unchanged)
- [FHIR Mapping](#fhir-mapping)
  - [Does the BFD FHIR API send Part A claims data for care received in Critical Access Hospitals and teaching hospitals for Part C enrollees?](#does-the-bfd-fhir-api-send-part-a-claims-data-for-care-received-in-critical-access-hospitals-and-teaching-hospitals-for-part-c-enrollees)
  - [Does the BFD FHIR API provide information on benefit details such as deductibles, copays, coinsurance, etc.?](#does-the-bfd-fhir-api-provide-information-on-benefit-details-such-as-deductibles-copays-coinsurance-etc)
  - [How does United States Core Data for Interoperability (USCDI) relate to FHIR and its implementation within the BFD API?](#how-does-united-states-core-data-for-interoperability-uscdi-relate-to-fhir-and-its-implementation-within-the-bfd-api)
  - [What other additional resources that are valuable to review in learning about BFD and its ecosystem?](#what-other-additional-resources-that-are-valuable-to-review-in-learning-about-bfd-and-its-ecosystem)
<br/>

### BFD Basics

#### What data is available via BFD?


FHIR, or Fast Healthcare Interoperability Resources, is a standard for exchanging healthcare information electronically. Developed by HL7 International, FHIR aims to simplify and streamline the interoperability of health data across different systems.

Key features of FHIR include:
- Resource-based Structure: FHIR organizes healthcare data into "resources," which represent discrete clinical or administrative concepts, such as patients, medications, and lab results.
- RESTful APIs: It utilizes modern web technologies and principles, including RESTful APIs, allowing for easy integration and access to health information.
- Interoperability: FHIR promotes interoperability between disparate health systems, enabling them to communicate and share data effectively.
- Flexibility: It is designed to be adaptable, allowing developers to implement it in various environments and applications.

Overall, FHIR aims to improve the accessibility, usability, and integration of healthcare information to enhance patient care.

#### What does BFD stand for?

Beneficiary FHIR Data Server

#### What data is available via BFD?

BFD provides different types of data on/about persons who are eligible for Medicare via various [FHIR](http://hl7.org/fhir) resources:
- Patient resources that contain demographic data, such as name, address, etc.
- Coverage resources that contain enrollment data, such as Part A status, Part D status, etc.
- ExplanationOfBenefit resources that contain Part A, B, and D Medicare claims data, such as date of service, diagnosis codes, etc. An EOB shows the status of a claim and how it was processed. The EOB includes the amounts paid by a plan and amounts one is responsible to pay, if any. 

#### How frequently is BFD data updated?

BFD receives data from two different data source maintainers (CCW- Chronic Conditions Warehouse & RDA- Replicated Data Access API). 

Weekly, on Friday afternoons, BFD receives beneficiary data and adjudicated claims data supporting the FHIR Coverage, ExplanationOfBenefit, and Patient resources via weekly RIF files from CCW. The data is loaded into BFD no later than 9:00 AM ET the next business day (typically Monday).

Partially adjudicated claims data supporting the FHIR Claim and ClaimResponse resources is updated nightly (M-F). This data is provided via RDA.

Please note that The amount of time it takes a claim to progress through the data ecosystem depends on a variety of factors from the data source maintainers. A CMS [whitepaper](https://confluence.cms.gov/display/BFD/FHIR+and+CMS+Documentation+Links?preview=/415007408/455427981/medicare-claims-maturity%20(1).pdf) documents some additional details on the claim lifecycle.

#### What is the difference between Version 1 (v1) and Version 2 (v2) of the BFD API?

BFD's v1 of the API stems from the STU3 FHIR specification, while v2 of the BFD API stems from the R4 FHIR Specification

Additional details about more technical differences can be found on this [page](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Migrating-to-V2-FAQ) which also highlights how to upgrade from v1 to v2. While there is no published deadline to deprecate v1 new features are not being added to v1.

Currently, all of the Peering Partner APIs still utilize v1 for some clients with DPC solely using v1. All Peering Partners are highly recommended to make plans to move to v2. 

#### Where can I find the BFD data dictionary?
The data dictionary can be accessed [here](https://d2tcxctwdgn0ff.cloudfront.net/index.html).
The data dictionary will be updated with each release, as needed.

#### What are some important resources I can use while onboarding to the DASG ecosystem?
While the type of resources worth reviewing when onboarding to the DASG ecosystem may vary depending on your role, here are a few that provide helpful context to BFD:

- **[DASG Data Flow Mural](https://app.mural.co/t/ccs9461/m/ccs9461/1675200515492/67e3c7997317cc0295f4ca40c04e499dbfb7b029?sender=u51efffd3f7e7739d86f36174)**
- **[BFD Overview](https://confluence.cms.gov/display/BFD/Beneficiary+FHIR+Data+Server+Home)** - Confluence
- **[BFD Ecosystem](https://app.mural.co/t/ccs9461/m/ccs9461/1611953148570/5552f75ea27c08b1aff84efb68aaaa506bbeec98?sender=ufb3f27f98e0d54a395347002)** - Mural
- **[DASG Ecosystem](https://app.mural.co/t/ccs9461/m/ccs9461/1675200515492/67e3c7997317cc0295f4ca40c04e499dbfb7b029?sender=fa91755a-fdef-4729-9396-9fb7e81c3619)** - Mural
- Key Slack Channels: **[#bfd](https://cmsgov.enterprise.slack.com/archives/C010WDXAZFZ)**, **[#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY)**, **[#bfd-Incident](https://cmsgov.enterprise.slack.com/archives/CH766L1GR)**

#### Where can I find the definitions of commonly-used terminology related to BFD?

You can find the definitions of common terms and acronyms related to BFD in the BFD [terminology dictionary](https://confluence.cms.gov/display/BFD/BFD+Terminology).

#### I have a question or feature request for the BFD team, where should I ask or begin the conversation?

As a DASG or contracting team member, start a conversation with the BFD team by submitting your question or request through our workflow in slack. It can be found by clicking the 'Submit a Question/Request' button bookmarked in the [#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY) channel. When completing the form for a feature request, select 'feature request' as the category. You can also reach out directly to the team PO or PM, also found as members of the team on the BFD channel in slack.

#### Does the API return coverage resources with future start dates?

The API does supply Coverage resources with start dates in the near future, relating to the initial enrollment period, as described [here](https://www.medicare.gov/basics/get-started-with-medicare/sign-up/when-does-medicare-coverage-start).

### Data Elements

#### How do I request synthetic beneficiary data?

The [BFD Synthetic Data Guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide) is the best starting point for obtaining more information about requesting, using, and understanding synthetic data.

Scroll down to the section titled 'Available Synthetic Beneficiaries' to find links to beneficiary characteristics files. Characteristics files include all synthetic beneficiaries available in each dataset in CSV format. After reviewing this page, if there is additional information needed or a request for more synthetic data required please reach out via the slack feature request workflow. 

#### What is the difference between the 'Claim Number' in the CMS Portal, and the 'Claim ID' or 'Claim Group' in the FHIR payloads provided by BFD?

The [Claim Number](https://resdac.org/cms-data/variables/claim-number) found in the [CMS Portal](MyMedicare.gov) ("check my claims") is the IDR (Integrated Data Repository) Claim Unique ID and is derived from data in the National Claims History (NCH) file process. 
The [Claim ID](https://resdac.org/cms-data/variables/claim-id) (CLM_ID) and [Claim Group ID](https://bluebutton.cms.gov/resources/identifier/claim-group/) is specific to the CCW and is not applicable to any other identification system or data source. 

The 'Claim Group ID' value is derived within the process from loading CCW data into the BFD database. 

#### Why is the Provider NPI (National Provider Identifier) sometimes an invalid number, such as 9999999992, 9999999993. Similarly, why is the Tax Number sometimes 11111111111?

The upstream processing systems, MCS (Multi-Carrier System) or FISS (Fiscal Intermediary Shared System), sometimes have default values that get passed on to CCW, and subsequently, to BFD.

#### Are pending or partially adjudicated claims available? How long does it take for those claims to be available?

Yes,  PAC data comes from FISS (Fiscal Intermediary Shared Systems) and MCS (Multi Carrier Systems)  via the MPSM (Medicare Payment System Modernization) RDA (Replicated Data Access) API and is made available in BFD  through the Claim and ClaimResponse resources.


PAC data is received from the MPSM RDA API  as it becomes available, typically nightly. <ins>However, it is important to note that PAC data availability varies by data source.</ins> This shared CMS [whitepaper](https://confluence.cms.gov/display/BFD/FHIR+and+CMS+Documentation+Links?preview=/415007408/455427981/medicare-claims-maturity%20(1).pdf) documents some additional details on the claim lifecycle.

#### What Substance Abuse and Mental Health Services Administration (SAMHSA) filtering takes place in BFD or upstream?

For each endpoint a SAMHSA filter parameter can be queried to include or exclude related SAMHSA data. SAMHSA filtering is done solely in BFD. In general, ICD9/ICD10 procedure and diagnosis codes, Healthcare Common Procedure Coding System (HCPCS) procedure codes, and drug codes, associated with mental health or substance use disorder(s) will trigger the filter. For more detailed information see the [BFD SAMHSA wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki/BFD-SAMHSA-Filtering).

BFD currently uses CMS/OIT's Substance Use Disorder (SUD) list to comply with the applicable regulations Title 42 USC:
- [mainly Part 2](https://www.ecfr.gov/current/title-42/chapter-I/subchapter-A/part-2)
- [partially Part 51](https://www.ecfr.gov/current/title-42/chapter-I/subchapter-A/part-51)

Please note there will be an updated list that BFD will incorporate into the SAMHSA filtering for a January 1, 2025 effective date. 


#### What are the payment-related monetary fields in BFD on the Explanation of Benefits (EOB) endpoint?

All adjudicated claims except PDE: 

- **payment.amount**
- **total.amount**
- **benefitBalance.financial.usedMoney**
- **item.adjudication** (adj. codes: submitted, noncovered, deductible, coinsurance, priorpayerpaid, paidtoprovider, paidtopatient, paidbypatient)

PDE: 

- **payment.date**
- **total.amount**
- **item.adjudication** (adj. codes: discount, drugcost, priorpayerpaid, paidbypatient, coinsurance, benefit)
- PAC: **total**

#### What adjudicated claim types are supported by BFD?

- Carrier
- Durable Medical Equipment (DME)
- Home Health Aid (HHA)
- Hospice
- Inpatient
- Outpatient
- Skilled Nursing Facility (SNF)
- Part D

#### What values for the 'gender' field are supported by Medicare?

The 'gender' value can be: 'male', 'female', or 'unknown'.

#### I have a question for CCW. How do I ask it?

You can submit questions about the CCW by clicking the ‘Submit a question/request’ bookmark in the [#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY) channel.

The BFD team will work with CCW to answer your questions. In some situations, you may be put in direct contact with the CCW team.

#### What PAC (Partially Adjudicated Claim) data does BFD contain?

There is a detailed Confluence page that contains a more in-depth overview of the PAC (Partially Adjudicated Claim) data and its mapping. It can be found [here](https://confluence.cms.gov/pages/viewpage.action?pageId=1012446673).

### Data Retention/Lifecycle

#### What is the retention policy for PAC (Partially Adjudicated Claims) data?

60 days

#### When a claim is updated, will a new claim with a different claim ID be created and the previous claim marked as cancelled? Or will the existing claim be updated and the status remain unchanged?

It's more likely the claim will get updated and the status remains the same. The status will only change to cancelled if the final action field we receive from CCW changes.

### FHIR Mapping

#### Does the BFD FHIR API send Part A claims data for care received in Critical Access Hospitals and teaching hospitals for Part C enrollees?

No. This information is not provided as part of the CCW-based BFD FHIR API. While we do have Part C enrollee data, our APIs focus is on Fee for Service Medicare (A, B, & D) and not Medicare Advantage (Part C).

#### Does the BFD FHIR API provide information on benefit details such as deductibles, copays, coinsurance, etc.?

In general, BFD has somewhat limited information available for deductibles. For Part A claims, BFD has deductible related data elements for Inpatient and Skilled Nursing Facility (SNF) claims:

- **NCH_IP_TOT_DDCTN_AMT**
- **NCH_BENE_IP_DDCTBL_AMT**

BFD does not provide Medicare Advantage claims so there is no deductible information relating to Part C. In terms of Part D claims, unfortunately BFD currently does not provide any deductible-related data elements.

BFD does include deductible information under the adjudication fields, please read over the FAQ question on monetary fields (What are the payment-related monetary fields in BFD on the Explanation of Benefits (EOB) endpoint?) for more information.

#### How does United States Core Data for Interoperability (USCDI) relate to FHIR and its implementation within the BFD API?

The USCDI defines a general set of core data classes/elements for US based health information exchange (e.g. allergies, labs, meds, problems). The FHIR community put together the US Core Implementation Guide based on the USCDI definition. This is the basic (or minimum) set of data that a vendor/payer/provider/etc needs to supply via their Patient Access APIs. 

This is relevant to BFD because BFD mostly aligns to the C4BB IG, version 1.1.0. Efforts are ongoing to improve conformance with IGs such as C4BB and US Core for applicable resources, and is an ongoing process. Of note, there is now a US Core profile for Coverage, which is referenced in C4BB versions 2.1+. For more information on the nuance of USCDI see the confluence page [here](https://confluence.cms.gov/display/BFD/questions/906270000/what-is-uscdi-and-how-does-it-relate-to-bfd).

#### What other additional resources that are valuable to review in learning about BFD and its ecosystem?

Additional resources for learning about BFD's data ecosystem can be found by clicking the links below:

- **[RESDAC](https://resdac.org/search-data-variables)** is a reputable and reliable source that outlines definitions of key medicare data variables. 
- **[FHIR CARIN](https://build.fhir.org/ig/HL7/carin-bb/)** is a good resource for FHIR standards and the CARIN implementation guild
- **[Bulk FHIR IG](https://build.fhir.org/ig/HL7/bulk-data/)** is a good resource for more information on the ecosystem BFD makes up

