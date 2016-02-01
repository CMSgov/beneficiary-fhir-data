MyMedicare.gov BlueButton Text-to-FHIR Utility
==============================================

Provides a simple utility for parsing CMS/MyMedicare.gov BlueButton files, converting them, and pushing them to a (DSTU 2.1 compliant) FHIR server.

## Building and Running

To run this project, it must be built locally (the project's builds are not currently published anywhere).

    $ mvn clean verify
    $ java -jar ./bluebutton-text-to-fhir-app/target/bluebutton-text-to-fhir-app-1.0.0-SNAPSHOT-capsule-fat.jar --server http://example.com/baseDstu2 input-bb-file.txt
