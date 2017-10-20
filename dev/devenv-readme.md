Development Environment Setup
=============================

Thinking of contributing to this project or some of the other Java-based Blue Button projects? Great! This document provides some help on getting a development environment setup for that work.

## Automation FTW!

First off, if you're on one of the following platforms, we provide the [devenv-install.py](./devenv-install.py) script, which automates most of the work for you. It supports:

* Windows, with [Cygwin](https://www.cygwin.com/)

It can be run as follows:

1. Install Python 3.

  On Windows, this can be done using [apt-cyg](https://github.com/transcode-open/apt-cyg), as follows:
    
    ```
    $ apt-cyg install python3 python3-setuptools
    ```

    __Note:__ If apt-cyg is having problems connecting to your cygwin mirror this may be due to an incorrect HOSTTYPE setting.  Verify your HOSTTYPE does not have additional decoration(i.e. x86_64-cygwin) and only contains the your system architecture(i.e. x86_64) that you are attempting to install on.
    ```
    # Example incorrect setting
    $ echo $HOSTTYPE
    x86_64-cygwin

    # Example correct setting
    $ echo $HOSTTYPE
    x86_64
    ```
    HOSTTYPE can be overridden on the command line or set in your shell configuration(i.e. .bashrc, .cshrc, etc).  To override HOSTTYPE on the command line use the following construct:
    ```
    # for csh/tcsh
    > setenv HOSTTYPE x86_64 && apt-cyg <command>

    # for bash
    $ HOSTTYPE=x86_64 apt-cyg <command>
    ```
1. Run the script:

    ```
    $ ./devenv-install.py
    ```

What does it do for you? Great question! It will create a `~/workspaces/tools/` directory and then download and install (as a user) the following into there for you:

* An [Oracle Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven](https://maven.apache.org/)
* [Latest Eclipse Version](https://www.eclipse.org/downloads/)
* Eclipse __m2e-apt__ Plugin
    * The m2e-apt plugin allows Maven projects in Eclipse to easily leverage [the JDK's Annotation Processing framework](http://docs.oracle.com/javase/7/docs/technotes/guides/apt/).
    * Install m2e-apt using the [Drag To Install](http://marketplace.eclipse.org/content/m2e-apt) option or install from within the Eclipse IDE by opening **Help > Eclipse Marketplace...** and searching for **m2e-apt** using the **Eclipse Marketplace** find dialog box.  Click the **Install** button for the plugin and restart Eclipse when prompted. 
    * [Usage Instructions for m2e-apt](https://developer.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out?_sscc=t)

If you're not using one of those supported platforms, or would prefer to setup things manually, you'll want to download and install the items listed above yourself.

## Cygwin Configuration

### Associating .sh files with Cygwin
Shell scripts that end in .sh should be associated with the Cygwin shell you have chosen to use.  Follow these steps to change the association for your installation:

  1. Open a Command Prompt by pressing the Windows Key+R, type "cmd" and press return.
  1. In the newly opened command prompt type:

    ``` 
    # Everything after .sh= is the ftype name
    > assoc .sh
    .sh=sh_auto_file
    ``` 
  1. Using the ftype name discovered in the previous step check the current association:

    ```
    # Example of sh_auto_file set to use git-bash
    > ftype sh_auto_file
    sh_auto_file="C:\Git\git-bash.exe" --no-cd "%L" %*
    ```
  1. If necessary, change the .sh association to use your Cygwin shell:

    ```
    # Example setting Cygwin bash to be associated with .sh files 
    > ftype sh_auto_file="C:\cygwin64\bin\bash.exe" %1 %*
    ```

## AWS Configuration

### AWS Credentials

Many of the automated tests associated with the Blue Button framework use AWS resources.  Before running a build using Maven or importing projects into your Eclipse IDE, which will run a build automatically, please ensure the appropriate accounts and credentials are configured within your environment.  **This is necessary to prevent incurring unwanted charges on the wrong AWS account**.

Below are links to detailed instructions on configuring your AWS credentials for your environment:

  * [Configuring the AWS CLI](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html)
  * [Configuration and Credential Files](http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html)

## Github Configuration

### Github SSH

You will need to configure an SSH credential in order to clone the Blue Button repositories.  Instructions are thoroughly documented on Github but for convenience here are the relevant links:

  * [Connecting to Github with SSH](https://help.github.com/articles/connecting-to-github-with-ssh/)
  * [Generating a new SSH key and adding it to the ssh-agent](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/)
  * [Adding a new SSH key to your GitHub account](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)
  * [Testing your SSH connection](https://help.github.com/articles/testing-your-ssh-connection/)

## Maven Configuration

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
      <!-- Change the id value to match the jdk version on your system --> 
      <id>jdk-8u31-linux-x64</id>
    </provides>
    <configuration>
      <!-- Change the jdkHome value to point to the jdk location on your system --> 
      <jdkHome>/home/myusername/workspaces/tools/jdk-8u31-linux-x64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

### Maven GPG Signing

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

```
$ mvn clean verify -Dgpg.skip=true
```

But please note that the `deploy` goal/phase will still fail if builds are not signed by an authorized user, as that's a requirement imposed by the repository itself.

### Proper HAPI-FHIR branch

Currently the Blue Button repositories require a specific version of the HAPI-FHIR libraries in order to build successfully.  The following commands should be run in order to configure the required libraries properly for the build:

```
$ git clone git@github.com:HHSIDEAlab/hapi-fhir.git
$ cd hapi-fhir
$ git checkout -b fix-race-condition-in-if-none-exists-2.4 origin/fix-race-condition-in-if-none-exists-2.4
$ mvn clean install
```

### Skipping Tests

The default install goal for the maven build will run the integration tests.  If you do not want to run them as some do use AWS resources use the following command line when executing the build:

```
$ mvn clean install -DskipITs
```

## Git Large File Storage

You'll also need to manually (boo!) download and install [Git Large File Storage](https://git-lfs.github.com/), which is used by some of the projects to store the large amounts of sample data needed in tests. Once installed, you need to run the following command once on your system:

    $ git lfs install

If you've already cloned any of our repos, you'll also want to run the following command in each of them to download and checkout any LFS files (future clones should do this automagically for you, now that it's installed):

    $ git lfs pull

## Eclipse Configuration

The following instructions are to be executed from within the Eclipse IDE application to ensure proper configuration.

### Eclipse JDK

Verify Eclipse is using the correct JDK.

1. Open **Window > Preferences**.
1. Select **Java > Installed JREs**.
1. If your JDK does not appear in the **Installed JREs** table add it by clicking the **Add** button, select **Standard VM** and locate your installation using the **Directory...** button.
1. Ensure your JDK is selected in the **Installed JREs** table by checking the checkbox next to the JDK you wish to use.

### Eclipse Preferences

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

### Importing Maven Projects into Eclipse

If you have already cloned Blue Button repositories to your system they can easily be added to your Eclipse workspace using the **Import** feature.

1. Open **File > Import...**.
1. Select **Existing Maven Projects**.
1. Specify a **Root Directory** using the **Browse...** button or by typing in a path.
1. Select the pom files you want to import from the **Projects** table.
1. Click **Finish**.
1. The projects and packages you selected will now appear in the **Project Explorer** window.

### Enable Auto-generated Code

Some of the projects use the m2e-apt plugin to generate source code that is compiled into some of the jars.  This is not enabled by default but can be by easily following these steps:

1. In the **Project Explorer** right-click on the **bluebutton-data-model-rif** project and select **Properties**.
1. In the **Properties** dialog, on the left-hand side select **Java Compiler > Annotation Processing**.
1. Check the checkbox labeled **Enable project specific settings".
1. In the **Generated source directory** editbox enter the following:
```
target/generated-sources/annotations
```
1. Click the **Apply and Close** button.
1. When prompted to rebuild the project select **Yes**.
  
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
