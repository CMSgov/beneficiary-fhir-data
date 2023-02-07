# BFD Code Style Guide

This document details the specific code and documentation styles recommended and enforced within the BFD codebase.

## Enforcement
All style rules will be enforced via checkstyle during the build (as possible). To ease development, you may wish
to install a checkstyle checker plugin to your IDE, and point it at the checkstyle.xml located in the apps directory.
This will allow instant checking of checkstyle rules within the IDE, so you don't need to wait until build time.

Test classes and any related test code (such as test utils) are included in code that is expected to follow these style guidelines.
Generated code is excluded, as it can be tricky (and of questionable value) to properly style/document generated code. New generated code
that needs to be excluded from checkstyle can be excluded in the checkstyle.xml file at the repo root.

## Javadoc Style

The goal of the BFD Javadoc style is to allow for easier maintainability and understanding of classes, methods, interfaces, constructors and
fields. Documentation should be as concise as possible while still encapsulating any contextual or historical information
that will help future maintainers make informed code changes. Basically, ensure someone with no context of the code
can come in and quickly understand and safely make adjustments without needing a ton of research or system knowledge.

Meeting this goal will require your careful consideration about when, where, and what to document; stay vigilant!

Javadocs within BFD will follow these conventions:

**Classes**

Classes must have a description; ideally this should be a high-level description of the class responsibility and/or purpose that is at least one full sentence ending in a period.

```java
/** Orchestrates and manages the execution of {@link PipelineJob}s. */
public final class PipelineManager implements AutoCloseable {
...
```

**Class Fields** 

Fields must have a full-sentence description which should describe what the purpose of the field is, and contain useful contextual information if applicable.
- Information about the field should be captured in the field javadoc, which can be referenced by the getter/setter to reduce duplication

```java
/** Holds the records of completed jobs. */
private final PipelineJobRecordStore jobRecordsStore;
```

**Methods**

Methods must have at least a one sentence description of what the method does, and must end in a period. It should avoid being too descriptive of the implementation
details, but still describe what the method does.
 - All method parameters should be documented with the `@param` tag, and a description of what the parameter is
 - Method parameter descriptions don't need to be full sentences and should not end in punctuation, they can be short descriptions
 - If `null` is explicitly disallowed for a parameter, it should be documented
 - Method overrides or interface implementations should have a javadoc that is simply `{@inheritDoc}` unless the overridden method's functionality diverges enough to need additional documentation
   - This includes `toString`, `hashCode`, etc.
 - Method returns should be documented with the `@return` tag and a description of what is returned (and how if multiple return conditions under various conditions)
   - If the method can return `null` it should be noted and under what condition this may happen
   - If the method returns a boolean, you can just list the `true` case (the false case should an obvious inverse, but if not document it)
 - Getters and setters can use a template for linking to the documented class field, to reduce doc duplication: 
 - Exceptions are not required to be documented, but it may be helpful to note when an exception can be thrown for the caller

```java
 /**
   * Adds support for the FHIR "read" operation, for {@link ExplanationOfBenefit}s. The {@link Read}
   * annotation indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param eobId The read operation takes one parameter, which must be of type {@link IdType} and
   *     must be annotated with the {@link IdParam} annotation.
   * @param requestDetails the request details for the read
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Read(version = false)
  @Trace
  public ExplanationOfBenefit read(@IdParam IdType eobId, RequestDetails requestDetails) {
...
```

Getters/Setters example:

```java
/**
  * Gets the {@link #rifColumnName}.
  *
  * @return the name of the RIF column in RIF export data files
  */
  public String getRifColumnName() {
     return rifColumnName;
  }
```

**Misc**

- Static final loggers don't need docs as long as they are named LOGGER (specifically excluded in checkstyle)
```java
private static final Logger LOGGER = LoggerFactory.getLogger(VolunteerJob.class);
```
- Class, method, or field docs can be condensed to one line in the event the doc only has one line with no params or returns
```java
/** The orchestration object for the pipeline. */
  private final PipelineManager pipelineManager;
```
- Enums must be documented with a description of what the enum is for, along with the values and what those values represent
```java
/** Enumerates the codebooks that are expected to be converted. */
public enum SupportedCodebook {
  /** The Beneficiary summary codebook which includes Medicare parts A/B/D. */
  BENEFICIARY_SUMMARY(
      "codebook-mbsf-abd.pdf",
      "Master Beneficiary Summary File - Base With Medicare Part A/B/D",
      "May 2017, Version 1.0"),
...
```
- Tests should also be documented and the test description should describe what the intention of the test is; someone reading the test should get an idea of why the test was written and what the test passing/failing means for the application.
  - Parameterized tests which have data methods can use the data method as an opportunity to describe the test

```java
 /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a version supplied with the coverageId parameter, as our read requests do not support
   * versioned requests.
   */
  @Test
  public void testCoverageReadWhereVersionedIdExpectException() {
```