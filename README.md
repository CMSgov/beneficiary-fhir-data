MyMedicare.gov BlueButton Parent POM
====================================

This repo just contains a Maven parent POM and some other build/dev resources that are shared by the CMS/MyMedicare.gov Java projects.

## Development Environment

Going to work on this project or one of the other Blue Button Java projects? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Releases

This project uses Maven's [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/) for releases, and must be manually released by a developer with permissions to [its GitHub repo](https://github.com/HHSIDEAlab/bluebutton-parent-pom) and to [OSSRH](http://central.sonatype.org/pages/ossrh-guide.html) (which is used to ensure its releases land in Maven Central).

Run the following commands to perform a release:

    $ mvn release:prepare release:perform
    $ git push --all && git push --tags

## Parameter Store

Parameters:

FILE_ID: The Jenkins ID for the config file managed by Jenkins.
Steps:

Fill in the job parameters with appropriate values.
Click "build" and wait for the job to finish.

<h2>Preparing the config for the running the Pipeline</h2>

<h3>Parameter naming</h3>

The parameter names are made from the following:
* Project Name
* Environment (dev/prod/sandbox/etc)
* Date that the value was added in YYYYMMDD format
* Parameter key
Example: /weapon-x/dev/20190130/subject

<h3>Updating the exisiting config.json</h3>

Go to "JsonConfig" in "Manage Jenkins" > "Managed files"
* Click the edit button next to the JsonConfig, it looks like a piece of paper and pencil
* Edit your parameters/secrets, if updating an existing parameter you will need to specify true for Overwrite
* Click Submit

<h2>Running the Pipeline</h2>

Click the Jenkins Pipeline named "parameters"
* Click "Build Now"
* Once the build is complete, click the most recent build number in the "Build History"
* Click "Console Output" to make sure the build ran successfully


## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
