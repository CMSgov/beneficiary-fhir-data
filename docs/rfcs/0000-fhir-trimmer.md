# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0023-fhir-trimming`
* Start Date: 2026-06-11
* RFC PR: [rust-lang/rfcs#0000](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [BFD-4768](https://jira.cms.gov/browse/BFD-4768)

To implement CARIN Basis Profiles in v3 in an effort to speed up bulk queries from the APIs (AB2D), BFD v3 needs to be sending trimmed FHIR resources. To trim these resources, BFD needs a framework for applying these profiles, either to queries into our internal database or by applying a profile to a FHIR resource and trimming unnecessary elements. This document serves as a record of research done in the interest of implementing this solution, the findings from that research, and a proposed solution to support it going forward.
git 
## Status
[Status]: #status

* Status: Proposed
* Implementation JIRA Ticket(s):
    * [BFD-4768](https://jira.cms.gov/browse/BFD-4768)

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

We need a way to implement CARIN Basis Profiles to limit the data we're sending to optimize AB2D, specifically. Because some APIs are legally unable to share all of the data available to IDR (or sometimes just dont' want it all), they have to implement their own resource trimming logic which is decentralized and error prone. Generally, this framework will (hopefully) allow us to implement any number of resource trimming tactics in the future.

## Proposed Solution
[Proposed Solution]: #proposed-solution

* FHIR Trimmer

FHIR trimming happens after the toFhir() call on a resource is made during the creation of FHIR resources. This happens after all data is pulled up from the database, and would involve running some quick pruning of resources via a yet-to-be-fully-realized process. I do know that it needs to skip validation, it needs to not use many HAPI library calls, and it needs to take a FHIRPath and process some sort of whitelist or blacklist for performance resource trimming.

To support this implementation, we need to copy the YAML map files from the model projects into the bfd-server-ng project. If there is a way to replicate or reference across projects cleanly, I am unsure, so a manual copy (or scipted copy) needs to occur. Then, on server startup, it needs to load it into memory so that it is easily accessible by the resource handlers when they are creating the FHIR resources.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

There are two portions of this code; reading and parsing the YAML files into a useable framework for trimming, and then doing the trimming.

* Mapping Framework

On bfd-server-ng startup, the yaml files need to be parsed and translated into usable lists (shape to be determined) and stored in memory (or in a cache). These will be read into either a blacklist or whitelist and used to trim FHIR resources after generation. This will interact with server startup, and be instantiated as a singleton that is used across all FHIR resource handlers. There are some implementation questions that are outstanding, but I will suggest two possible implementations.

Copy Existing Cache Protocol

Right now, we read in SecurityLabels (in a similiarish process) in EobHandler and cache them in memory via a static object. We do not want to have to reread these files during run, and they will only be used in specific locations.

```  // Cache the security labels map to avoid repeated I/O and parsing
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();
```
This is the simplest solution, but may not be the best solution.

Spring Configuration

Configuration.java (in bfd-server-ng) already uses @Getter(lazy = true) for clientCertsToAliases and samhsaAllowedCertificateAliases which are computed once on first access and cached in the field. A new nested class (e.g., BasisProfileMapping) could be added under this config, populated at startup via @ConfigurationProperties to parse the YAMLs. This is consistent with how clientCertificates and samhsaAllowedCertificateAliasesJson are handled in the code and has the added benefit that the mapping can be injected and overridden in tests.

The Spring config approach gets us better maintainability and testability, but still has unanswered questions.

* Resource Trimming

After toFhir(), the resource exists as a full resource. Before sending, some control flow needs to determine if a profile was passed into the request, and if so, we need to send the resource to the barber shop. The FhirTrimmer should be lightweight and simple, requiring no resource validation to occur and utilizing as little of HAPI FHIR as possible (because their operations tend to be very safe and slow, and we need to handle minimumum 100k/second resources).

Because our mapping has valid FhirPaths, we will either need to implement logic that walks a FhirPath (FhirTerser proved to be too slow in testing) and removes elements via a blacklist, or verify evey element in the resultant resource matches a whitelist and remove those that don't. I think there is some wiggle room on converting FhirPath logic into something more similar for directly accessing values in a FhirResource.

FhirPath

```Coverage.extension.where(url='https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD').value.code```

HAPI Java to Remove

```coverage.getExtension().removeIf(ext -> "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD".equals(ext.getUrl()));```

Work still needs to be done to make this universal, but I believe if we can translate the FhirPaths into HAPI Java fhir code 1:1, it will speed up removal and makes the structure of the BasisProfileMapping a lot easier to manage (whether or not we go with the blacklist or whitelist).

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Items to be discussed:

* Is there a way to access files across modules?
  
    Answer: Either by using symlinks or by moving the files into the shared-utils module
* Spring Config vs Static?

    Answer: 
* Is there a way to modify the bundles without using HAPI Fhir? Would that be faster?

    Answer: There is, but it is much more dangerous and should only be considered if other options aren't feasible

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

* We are spending resources querying the database and building a full FHIR resource before throwing some of it away.
* It is tied to the YAML dictionary map files that we maintain, so the logic of the mapping may be less transferrable to new structures of data

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

* Query Level

In the JPA (Jakarta Persistence) layer, we define entities that map to the database via @Embedded and @Column for building FHIR resources. The entities contain fields that map to columns which map to FhirPaths. When an object is requested (say a Claim) by a request, we build it as fully as possible (with some exceptions, like SAMHSA) and send it out. We already short-circuit on concepts like system type.

To achieve our objective at this layer, it would require us to modify how we handle @Embedded and @Column with some sort of Hibernate interception. Given the complicated nature of implementing this and possible caching pitfalls + rewriting our entire JQL generating code, I have opted not to go with this approach.

The primary impact of not doing this is that the pain of doing it now may be necessary depending on performance of the trimming process.

## Prior Art
[Prior Art]: #prior-art

Blue Button currently does very minimal resource trimming, and it is very manual. There is no framework in place to handle something generic, and I am unsure any other API has impelemented a generic solution to this problem.

It's possible to use StructureDefinitions and HAPI FHIR validation to remove elements, but this proved to be very slow (100 resources/second)

## Future Possibilities
[Future Possibilities]: #future-possibilities

There should be the ability to add new profiles (or some similar concept) to the trimmer, maybe without requesting it to the dictionary support map YAML files. Possibly a StructureDefinition resource that's broken down into blacklist/whitelist? We can't use the HAPI parser because it's too slow, but we could theoretically convert a StructureDefinition into a list of FhirPaths.

## Addendums
[Addendums]: #addendums

None at this time
