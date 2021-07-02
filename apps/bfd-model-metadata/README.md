# BFD Model Metadata

This is a (hilariously incomplete) proof of concept of a new system for describing and working with BFD's data model.
The idea is to move BFD to more of a declarative data management model,
  which would enable:

* Data documentation to be automatically generated, into multiple formats.
* Data transformation code to be automatically generated.
* Easier comparison between API versions (e.g. v1 versus v2).
* More collaboration with non-developer teammates on the data documentation and transformations.

At the moment, practically none of that is implemented.
As of 2020-03-15, it's barely past the "sketch it on the back of a napkin" design phase.
The following are likely next steps:

* Finish the stubbed-out mapping for RIF --> BFD database.
* Resolve compile errors by stubbing out more classes.
* Add a unit test for the mapping declaration.
* Implement enough things that the unit test compiles.
* Add mappings for BFD DB to BFD FHIR v1.
* Try to abstract the mappings so they don't require separate implementations for each format pair.
* Add a few more fields to flush out more potential problems, such as:
   * FHIR nested fields that aren't arrays. (We must be using some of those, right?)
   * Cross-hierarchy references, like with `EOB.diagnosis`.
* Build a terrible PoC for documentation generation.
* Build a terrible PoC for auto-generated ELT code.
