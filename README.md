MyMedicare.gov BlueButton Data Pipeline
=======================================

Provides an Extract-Transform-Load (ETL) data pipeline, which moves data from CMS' Chronic Conditions data warehouse (CCW) into the CMS Blue Button FHIR Server.

## Development Environment

Going to work on this project? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Running Locally (with Sample Data)

This project produces a `bluebutton-data-pipeline-fhir-sampledata/target/bluebutton-data-pipeline-fhir-sampledata-0.1.0-SNAPSHOT-capsule-fat.x` executable/binary. It's a self-contained/"fat" JAR built using [Capsule](http://www.capsule.io/); all it needs is a Java 8 JVM. When run, that will start generating sample beneficiary and claims data (using the code in the `bluebutton-data-pipeline-sampledata` module) and pushing it into the specified FHIR server. This can be built and run, as follows (assuming you have a FHIR server running locally at <http://localhost:8080/baseDstu2>):

    $ mvn clean verify
    $ ./bluebutton-data-pipeline-fhir-sampledata/target/bluebutton-data-pipeline-fhir-sampledata-0.1.0-SNAPSHOT-capsule-fat.x --server http://localhost:8080/baseDstu2

Please note that the ETL is currently very slow, and will take **days** to finish, even on fast hardware. Until that situation is improved, it's recommended you monitor how much sample data has been loaded into the FHIR server while the ETL is running. Once enough has been loaded for your purposes, the remaining ETL can be stopped by pressing `ctrl+c` at the console.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
