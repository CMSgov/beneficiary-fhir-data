# RDA API Database Entities

This project builds a JAR file containing the JPA entity classes used to store data collected from the
Replicated Data Access (RDA) API.

The associated migration scripts to manage the database schema used by these entities are maintained in
the `bfd-model-rif` project.

## persistence.xml

Hibernate will only auto-discover entity objects located in the same JAR as the `persistence.xml` file.
The normal `persistence.xml` file for the ETL pipeline is located in the `bfd-model-rif` project
This is not a problem for the ETL process since it is deployed inside of an 'uber-jar' containing classes
from all of the maven projects that it depends on.  Nor is it a problem for the BFD server since it
uses Spring to build up its JPA configuration and Spring does not have the same limitation.

However for unit and integration tests within each project we need a different `persistence.xml` file
to allow the RDA entities to be used with JPA.  The `persistence.xml` file in this project defines a
persistence unit named `gov.cms.bfd.rda` that contains only the RDA entity classes.
