---
layout: page
title: FAQ
permalink: /faq.html
---

#### Table of Contents

- [BFD Basics](#bfd-basics)
  - [What data is available via BFD?](#what-data-is-available-via-bfd)
  - [What does BFD stand for?](#what-does-bfd-stand-for)
  - [How frequently is BFD data updated?](#how-frequently-is-bfd-data-updated)
  - [What is the difference between V1 (Version 1) and V2 (Version 2) of the BFD API?](#what-is-the-difference-between-v1-version-1-and-v2-version-2-of-the-bfd-api)
  - [Where can I find the BFD data dictionary?](#where-can-i-find-the-bfd-data-dictionary)
  - [What are some important resources I can use while onboarding to the DASG ecosystem?](#what-are-some-important-resources-i-can-use-while-onboarding-to-the-dasg-ecosystem)
  - [Where can I find the definitions of commonly-used terminology related to BFD?](#where-can-i-find-the-definitions-of-commonly-used-terminology-related-to-bfd)
  - [I have a question for the BFD team. Where should I ask or begin the conversation?](#i-have-a-question-for-the-bfd-team-where-should-i-ask-or-begin-the-conversation)
  - [I have a feature request, where should I ask for it to be considered or prioritized?](#i-have-a-feature-request-where-should-i-ask-for-it-to-be-considered-or-prioritized)
  - [What other additional resources that are valuable to review in learning about BFD and its ecosystem?](#what-other-additional-resources-that-are-valuable-to-review-in-learning-about-bfd-and-its-ecosystem)
- [Data Elements](#data-elements)
  - [Does the API return coverage resources with future start dates?](#does-the-api-return-coverage-resources-with-future-start-dates)
  - [How do I request synthetic beneficiary data?](#how-do-i-request-synthetic-beneficiary-data)
  - [What is the difference between the 'Claim Number' in the CMS Portal, and the 'Claim ID' or 'Claim Group' in the FHIR payloads provided by BFD?](#what-is-the-difference-between-the-claim-number-in-the-cms-portal-and-the-claim-id-or-claim-group-in-the-fhir-payloads-provided-by-bfd)
  - [Why is the Provider NPI (National Provider Identifier) sometimes an invalid number, such as 9999999992, 9999999993. Similarly, why is the Tax Number sometimes 11111111111?](#why-is-the-provider-npi-national-provider-identifier-sometimes-an-invalid-number-such-as-9999999992-9999999993-similarly-why-is-the-tax-number-sometimes-11111111111)
  - [Are pending or partially adjudicated claims available? How long does it take for those claims to be available?](#are-pending-or-partially-adjudicated-claims-available-how-long-does-it-take-for-those-claims-to-be-available)
  - [What Substance Abuse and Mental Health Services Administration (SAMHSA) filtering takes place in BFD or upstream?](#what-substance-abuse-and-mental-health-services-administration-samhsa-filtering-takes-place-in-bfd-or-upstream)
  - [What are the payment-related monetary fields in BFD on the Explanation of Benefits (EOB) endpoint?](#what-are-the-payment-related-monetary-fields-in-bfd-on-the-explanation-of-benefits-eob-endpoint)
  - [What adjudicated claim types are supported by BFD?](#what-adjudicated-claim-types-are-supported-by-bfd)
  - [What values for the 'gender' field are supported by Medicare?](#what-values-for-the-gender-field-are-supported-by-medicare)
  - [I have a question for CCW. How do I ask it?](#i-have-a-question-for-ccw-how-do-i-ask-it)
  - [What PAC (Partially Adjudicated Claim) data does BFD contain?](#what-pac-partially-adjudicated-claim-data-does-bfd-contain)
- [Data Retention/Lifecycle](#data-retentionlifecycle)
  - [What is the retention policy for PAC (Partially Adjudicated Claims) data?](#what-is-the-retention-policy-for-pac-partially-adjudicated-claims-data)
  - [When a claim is updated, will a new claim with a different claim ID be created and the previous claim marked as cancelled? OR will the existing claim be updated and the status remain unchanged?](#when-a-claim-is-updated-will-a-new-claim-with-a-different-claim-id-be-created-and-the-previous-claim-marked-as-cancelled-or-will-the-existing-claim-be-updated-and-the-status-remain-unchanged)
- [FHIR Mapping](#fhir-mapping)
  - [Does the BFD FHIR API send Part A claims data for care received in Critical Access Hospitals and teaching hospitals for Part C enrollees?](#does-the-bfd-fhir-api-send-part-a-claims-data-for-care-received-in-critical-access-hospitals-and-teaching-hospitals-for-part-c-enrollees)
  - [Does the BFD FHIR API provide information on benefit details such as deductibles, copays, coinsurance, etc.?](#does-the-bfd-fhir-api-provide-information-on-benefit-details-such-as-deductibles-copays-coinsurance-etc)
  - [How does United States Core Data for Interoperability (USCDI) relate to FHIR and its implementation within the BFD API?](#how-does-united-states-core-data-for-interoperability-uscdi-relate-to-fhir-and-its-implementation-within-the-bfd-api)

<br/>

### BFD Basics

#### What data is available via BFD?

BFD provides different types of data on/about persons who are eligible for Medicare via various [FHIR](http://hl7.org/fhir) resources:
- Patient resources that contain demographic data such as name, address, etc.
- Coverage resources that contain enrollment data such as Part A status, Part D status, etc.
- ExplanationOfBenefit resources that contain Part A, B, and D Medicare claims data, such as date of service, diagnosis codes, etc.

#### What does BFD stand for?

Beneficiary FHIR Data Server

#### How frequently is BFD data updated?

Data for people with Medicare and adjudicated claims data supporting Coverage, ExplanationOfBenefit, and Patient resources is updated weekly.
Partially adjudicated claims data supporting Claim and ClaimResponse resources is updated nightly. This data is provided via RDA (Replicated Data Access API).

BFD also receives beneficiary data and adjudicated claims data weekly via RIF files from CCW on Friday afternoons.
The data is loaded into BFD no later than 9:00 AM ET the next business day (typically Monday). If there is any delay past this time frame, BFD will declare an incident and notify its partners.

#### What is the difference between V1 (Version 1) and V2 (Version 2) of the BFD API?

V1 of the BFD API works towards conformance to [STU3](https://www.hl7.org/fhir/STU3/), while V2 of the BFD API is conformant to FHIR [R4](https://hl7.org/fhir/R4/). 

Another key difference between V1 and V2 is that partially adjudicated claim data is only available in V2. 

Additional details about more technical differences can be found on this [page](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Migrating-to-V2-FAQ) which also highlights how to upgrade from V1 to V2.
Lastly, there are is no deadline to deprecate V1. However, there is no active feature development.

#### Where can I find the BFD data dictionary?
The data dictionary can be accessed [here](/data-dictionary-v1) for V1 and [here](/data-dictionary-v2) for V2.
The data dictionary is updated in each release if there are changes.

#### What are some important resources I can use while onboarding to the DASG ecosystem?
While the type of resources worth reviewing when onboarding to the DASG ecosystem may vary depending on your role, here are a few that provide helpful context to BFD:

- **[BFD Overview](https://confluence.cms.gov/display/BFD/Beneficiary+FHIR+Data+Server+Home)** - Confluence
- **[BFD Ecosystem](https://app.mural.co/t/ccs9461/m/ccs9461/1611953148570/5552f75ea27c08b1aff84efb68aaaa506bbeec98?sender=ufb3f27f98e0d54a395347002)** - Mural
- **[DASG Ecosystem](https://app.mural.co/t/ccs9461/m/ccs9461/1675200515492/67e3c7997317cc0295f4ca40c04e499dbfb7b029?sender=fa91755a-fdef-4729-9396-9fb7e81c3619)** - Mural
- Key Slack Channels: **[#bfd](https://cmsgov.enterprise.slack.com/archives/C010WDXAZFZ)**, **[#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY)**, **[#bfd-Incident](https://cmsgov.enterprise.slack.com/archives/CH766L1GR)**

#### Where can I find the definitions of commonly-used terminology related to BFD?

You can find the definitions of common terms and acronyms related to BFD in the BFD [terminology dictionary](https://confluence.cms.gov/display/BFD/BFD+Terminology).

#### I have a question for the BFD team. Where should I ask or begin the conversation?

To start a conversation with the BFD team, please submit your question or request through our workflow in slack.
It can be found by clicking the 'Submit a Question/Request' button bookmarked in the [#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY) channel. 

#### I have a feature request, where should I ask for it to be considered or prioritized?

To start a conversation on a feature request, please submit your request through our workflow in Slack. It can be found here or by clicking the button bookmarked in the [#bfd-users](https://cmsgov.enterprise.slack.com/archives/CMT1YS2KY) channel. When completing the form, select 'feature request' as the category.

#### What other additional resources that are valuable to review in learning about BFD and its ecosystem?

Here are some additional resources for learning about BFD's data ecosystem. It will be updated periodically.

- **[RESDAC](https://resdac.org/search-data-variables)** is a reputable and reliable source that outlines definitions of key medicare data variables. 
- **[FHIR CARIN](https://build.fhir.org/ig/HL7/carin-bb/)** is a good resource for FHIR standards and the CARIN implementation guild
- **[Bulk FHIR IG](https://build.fhir.org/ig/HL7/bulk-data/)** is a good resource for more information on the ecosystem BFD makes up


### Data Elements

#### Does the API return coverage resources with future start dates?

The API does supply Coverage resources with start dates in the near future, relating to the initial enrollment period, as described [here](https://www.medicare.gov/basics/get-started-with-medicare/sign-up/when-does-medicare-coverage-start).

#### How do I request synthetic beneficiary data?

The [BFD Synthetic Data Guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide) is the best starting point for obtaining more information about requesting, using, and understanding synthetic data. 

Scroll down to the section titled [Available Synthetic Beneficiaries](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide#available-synthetic-beneficiaries) to find links to beneficiary characteristics files.
Characteristics files include all synthetic beneficiaries available in each dataset in CSV format.


#### What is the difference between the 'Claim Number' in the CMS Portal, and the 'Claim ID' or 'Claim Group' in the FHIR payloads provided by BFD?

The [Claim Number](https://resdac.org/cms-data/variables/claim-number) found in the CMS Portal ("check my claims") is the IDR (Integrated Data Repository) Claim Unique ID and is derived from data in the National Claims History (NCH) file process. 
The [Claim ID](https://resdac.org/cms-data/variables/claim-id) (CLM_ID) and [Claim Group ID](https://bluebutton.cms.gov/resources/identifier/claim-group/) are assigned by the CCW.
They are specific to the CCW and are not applicable to any other identification system or data source. 

#### Why is the Provider NPI (National Provider Identifier) sometimes an invalid number, such as 9999999992, 9999999993. Similarly, why is the Tax Number sometimes 11111111111?

The upstream processing systems (MCS (Multi-Carrier System) or FISS (Fiscal Intermediate Shared System)) sometimes have strange default values like this that get passed on to CCW, and then subsequently to BFD.

BFD, in most cases, does not correct this type of data passed to it from upstream providers. BFD seeks to enrich the data that is provided.

#### Are pending or partially adjudicated claims available? How long does it take for those claims to be available?

Yes, this data is provided by RDA (Replicated Data Access API) and available within BFD from the Claim and ClaimResponse resources.

The amount of time it takes a claim to progress through the data ecosystem depends on a variety of factors.
A shared CMS [whitepaper](https://confluence.cms.gov/display/BFD/FHIR+and+CMS+Documentation+Links?preview=/415007408/455427981/medicare-claims-maturity%20(1).pdf) documents some additional details on the claim lifecycle.

#### What Substance Abuse and Mental Health Services Administration (SAMHSA) filtering takes place in BFD or upstream?

For each endpoint, the SAMHSA filter parameter can be supplied to include or exclude related SAMHSA data. SAMHSA filtering is done solely in BFD. In general, ICD9/ICD10 procedure and diagnosis codes, Healthcare Common Procedure Coding System (HCPCS) procedure codes, and drug codes, associated with mental health or substance use disorder(s) will trigger the filter.

BFD currently uses CMS/OIT's Substance Use Disorder (SUD) list to comply with the applicable regulations Title 42 USC:

- [Mainly Part 2](https://www.ecfr.gov/current/title-42/chapter-I/subchapter-A/part-2) 
- [Partially Part 51](https://www.ecfr.gov/current/title-42/chapter-I/subchapter-A/part-51)

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

Carrier
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

#### When a claim is updated, will a new claim with a different claim ID be created and the previous claim marked as cancelled? OR will the existing claim be updated and the status remain unchanged?

It's more likely the claim will get updated and the status remains the same. The status will only change to cancelled if the final action field we receive from CCW changes.

### FHIR Mapping

#### Does the BFD FHIR API send Part A claims data for care received in Critical Access Hospitals and teaching hospitals for Part C enrollees?

No. This information is not provided as part of the CCW-based BFD FHIR API.

#### Does the BFD FHIR API provide information on benefit details such as deductibles, copays, coinsurance, etc.?

In general, we have somewhat limited information available for deductibles. For Part A claims, we have deductible-related data elements for Inpatient and Skilled Nursing Facility (SNF) claims: 

- **NCH_IP_TOT_DDCTN_AMT**
- **NCH_BENE_IP_DDCTBL_AMT**

BFD does not provide Medicare Advantage claims so there is no deductible information relating to Part C. In terms of Part D claims, unfortunately we currently do not provide any deductible-related data elements.

BFD does include deductible information under the adjudication fields, please read over the FAQ question on monetary fields for more information.

#### How does United States Core Data for Interoperability (USCDI) relate to FHIR and its implementation within the BFD API?

The USCDI defines a general set of core data classes/elements for US based health information exchange (e.g. allergies, labs, meds, problems). The FHIR community put together US Core Implementation Guide based on the USCDI definition. This is the basic (or minimum) set of data that a vendor/payer/provider/etc needs to supply via their Patient Access APIs. For instance, looking at the API docs for EPIC, the largest EMR vendor in the country aligns with USCDI.

This is relevant to BFD because we conform to some CARIN IG which is based on the US Core IG (which is based on USCDI). However, the CARIN IG is only based on USCDI for patient, practitioner, and org resources. EOB and Coverage are not part of US Core. More information on this nuance can be found here. [[Source]](https://confluence.cms.gov/display/BFD/questions/906270000/what-is-uscdi-and-how-does-it-relate-to-bfd)

