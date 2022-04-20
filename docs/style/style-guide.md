# BFD Code Style Guide

This document details the specific code and documentation styles recommended and enforced within the BFD codebase.

## Enforcement
All style rules will be enforced via checkstyle during the build (as possible). To ease development, you may wish
to install a checkstyle checker plugin to your IDE, and point it at the checkstyle.xml located in the apps directory.
This will allow instant checking of checkstyle rules within the IDE, so you don't need to wait until build time.

Enforcement will only be automated within projects that have been cleaned up to meet the documentation standards; until
a project has been cleaned up it will be excluded from build enforcement. Old code that is an excluded project does not need
to be updated if the class is touched, unless the new code adds new contextual information that would be useful to capture.

Newly written classes/fields/methods in all projects within BFD should follow these conventions, 
regardless of automatic enforcement.

Test classes and any related test code (such as test utils) are included in code that is expected to follow these style guidelines.
Generated code is excluded, as it can be tricky (and of questionable value) to properly style/document generated code.

## Javadoc Style

The goal of the BFD Javadoc style is to allow for easier maintainability and understanding of classes, methods, interfaces, constructors and
fields. Documentation should be as concise as possible while still encapsulating any contextual or historical information
that will help future maintainers make informed code changes. Basically, ensure someone with no context of the code
can come in and quickly understand and safely make adjustments without needing a ton of research or system knowledge.

Meeting this goal will require your careful consideration about when, where, and what to document; stay vigilant!

Javadocs within BFD will follow these conventions:

- All methods and constructors of all scopes must have a javadoc
  - Must have a description that explains what the method does, and any useful contextual/historical information that may help maintainability
  - All method parameters should be documented with the ```@param``` tag, and a description of what the parameter is
    - If ```null``` is explicitly disallowed for a parameter, it should be documented
  - Method returns should be documented with the ```@returns``` tag and a description of what is returned (and how if multiple returns under various conditions)
    - If the method can return ```null``` it should be noted and under what condition this may happen
  - Method overrides or interface implementations should have a javadoc that is simply ```{@inheritDoc}``` unless the overridden method's functionality diverges enough to need additional documentation
    - This includes ```toString```, ```hashCode```, etc.
- All classes and interfaces of all scopes must have a javadoc 
- All class-level fields of all scopes must have a javadoc
    - Information about the field should be captured in the field javadoc, which can be referenced by the getter/setter to reduce duplication
    - Reference other classes when it makes sense to reduce duplication (for instance, a class level field that represents a CcwCodebookVariable can just link to the CcwCodebookVariable in the documentation)

