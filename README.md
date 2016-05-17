MyMedicare.gov BlueButton Parent POM
====================================

This repo just contains a Maven parent POM and some other build/dev resources that are shared by the CMS/MyMedicare.gov Java projects.

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
