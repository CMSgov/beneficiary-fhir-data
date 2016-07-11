Development Environment Setup
=============================

Thinking of contributing to this project or some of the other Java-based Blue Button projects? Great! This document provides some help on getting a development environment setup for that work.

## Automation FTW!

First off, if you're on one of the following platforms, we provide the [devenv-install.py](./devenv-install.py) script, which automates most of the work for you. It supports:

* Windows, with [Cygwin](https://www.cygwin.com/)

It can be run as follows:

1. Install Python 3.
    * On Windows, this can be done using [apt-cyg](https://github.com/transcode-open/apt-cyg), as follows:
    
        ```
        $ apt-cyg install python3 python3-setuptools
        ```
    
1. Run the script:

    ````
    $ ./devenv-install.py
    ````

What does it do for you? Great question! It will create a `~/workspaces/tools/` directory and then download and install (as a user) the following into there for you:

* An [Oracle Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Eclipse Mars](https://eclipse.org/mars/)
    * These Eclipse Plugins:
        * [m2e-apt](https://developer.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out?_sscc=t): Allows Maven projects in Eclipse to easily leverage [the JDK's Annotation Processing framework](http://docs.oracle.com/javase/7/docs/technotes/guides/apt/).
* [Apache Maven](https://maven.apache.org/)

If you're not using one of those supported platforms, or would prefer to setup things manually, you'll want to download and install the items listed above yourself.

### Maven `toolchains.xml`

Right now, the script does not create the Maven `toolchains.xml` file (though it can and should). As a workaround, create it manually yourself, as `~/.m2/toolchains.xml`, with content similar to the following (adjust the paths to match your system):

```
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
	<toolchain>
		<type>jdk</type>
		<provides>
			<version>1.8</version>
			<vendor>oracle</vendor>
			<id>jdk-8u31-linux-x64</id>
		</provides>
		<configuration>
			<jdkHome>/home/karl/workspaces/tools/jdk-8u31-linux-x64</jdkHome>
		</configuration>
	</toolchain>
</toolchains>
```

## Git Large File Storage

You'll also need to manually (boo!) download and install [Git Large File Storage](https://git-lfs.github.com/), which is used by some of the projects to store the large amounts of sample data needed in tests. Once installed, you need to run the following command once on your system:

    $ git lfs install

If you've already cloned any of our repos, you'll also want to run the following command in each of them to download and checkout any LFS files (future clones should do this automagically for you, now that it's installed):

    $ git lfs pull

## Eclipse Preferences

If you're using Eclipse for development, you'll want to configure its preferences, as follows:

1. Open **Window > Preferences**.
1. Select **Java > Code Style > Code Templates**.
    1. Click **Import...** and select this project's [eclipse-codetemplates.xml](./eclipse-codetemplates.xml) file.
        * This configures the file, class, method, etc. comments on new items such that they match the existing style used in these projects.
    1. Enable the **Automatically add comments for new methods and types** option.
1. Select **Java > Code Style > Formatter**.
    1. Click **Import...** and select this project's [eclipse-formatter.xml](./eclipse-formatter.xml) file.
        * This configures the Eclipse autoformatter (`ctrl+shift+f`) to match the existing style used in these projects.
1. Select **Java > Editor > Save Actions**.
    1. Enable the **Perform the selected actions on save** option.
    1. Enable the **Format source code** option.
1. Click **OK**.

## Maven GPG Signing

Given that most of this project's builds go through Maven Central, all builds for the project have signing turned on by default. If you do not have a GPG key configured on your system, you will receive errors like the following:

```
[INFO] --- maven-gpg-plugin:1.5:sign (sign-artifacts) @ bluebutton-parent ---
gpg: directory `/home/karl/.gnupg' created
gpg: new configuration file `/home/karl/.gnupg/gpg.conf' created
gpg: WARNING: options in `/home/karl/.gnupg/gpg.conf' are not yet active during this run
gpg: keyring `/home/karl/.gnupg/secring.gpg' created
gpg: keyring `/home/karl/.gnupg/pubring.gpg' created
gpg: no default secret key: secret key not available
gpg: signing failed: secret key not available
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 1.505 s
[INFO] Finished at: 2016-06-13T09:06:37-04:00
[INFO] Final Memory: 18M/481M
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-gpg-plugin:1.5:sign (sign-artifacts) on project bluebutton-parent: Exit code: 2 -> [Help 1]
```

To fix these errors, the simplest thing to do is just install GPG and create keys for yourself, per the instructions here: [Central Repository: Working with PGP Signatures](http://central.sonatype.org/pages/working-with-pgp-signatures.html).

GPG signing can also be disabled by adding `-Dgpg.skip=true` to your Maven builds, e.g.:

    $ mvn clean verify -Dgpg.skip=true

But please note that the `deploy` goal/phase will still fail if builds are not signed by an authorized user, as that's a requirement imposed by the repository itself.

## OSSRH Hosting

Even with a GPG key, you will be unable to deploy to OSSRH/Maven Central, unless your account has been given permissions to do so. This will result in errors like the following:

```
[ERROR] Failed to execute goal org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:deploy (injected-nexus-deploy) on project bluebutton-parent: Failed to deploy artifacts: Could not transfer artifact gov.hhs.cms.bluebutton:bluebutton-parent:pom.asc:1.1.1-20160614.223219-1 from/to ossrh (https://oss.sonatype.org/content/repositories/snapshots): Access denied to https://oss.sonatype.org/content/repositories/snapshots/gov/hhs/cms/bluebutton/bluebutton-parent/1.1.1-SNAPSHOT/bluebutton-parent-1.1.1-20160614.223219-1.pom.asc. Error code 401, Unauthorized -> [Help 1]
```

Follow the following procedure to obtain those permissions and resolve this problem:

1. [Create a Sonatype JIRA account](https://issues.sonatype.org/secure/Signup!default.jspa).
1. Ensure you've published your GPG key to a public key server, per [OSSRH: Working With PGP Signatures](http://central.sonatype.org/pages/working-with-pgp-signatures.html#distributing-your-public-key).
1. [File a new OSSRH issue](https://issues.sonatype.org/secure/CreateIssue.jspa) requesting authorization for the `gov.hhs.cms.bluebutton` repo in OSSRH. See [OSSRH-23379: Authorize Shaun Brockhoff to deploy to gov.hhs.cms.bluebutton repo](https://issues.sonatype.org/browse/OSSRH-23379) for an example.
1. Follow the instructions on [OSSRH: Apache Maven: Distribution Management and Authentication](http://central.sonatype.org/pages/apache-maven.html#distribution-management-and-authentication) to ensure that your `~/.m2/settings.xml` file has a `<server/>` entry for `<id>ossrh</id>`, with your JIRA login and password.
