MyMedicare.gov BlueButton Parent POM
====================================

This repo just contains a Maven parent POM and some other build/dev resources that are shared by the CMS/MyMedicare.gov Java projects.

## About

The Beneficiary FHIR Data Server (BFD) provides FHIR-formatted beneficiary data to CMS Peering Partner APIs.

### DASG Mission
Drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare. 

### BFD Mission
Enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare. 

### BFD Vision
Provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data. 

## Development Environment

Going to work on this project or one of the other Blue Button Java projects? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Releases

This project uses Maven's [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/) for releases, and must be manually released by a developer with permissions to [its GitHub repo](https://github.com/HHSIDEAlab/bluebutton-parent-pom) and to [OSSRH](http://central.sonatype.org/pages/ossrh-guide.html) (which is used to ensure its releases land in Maven Central).

Run the following commands to perform a release:

    $ mvn release:prepare release:perform
    $ git push --all && git push --tags

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
